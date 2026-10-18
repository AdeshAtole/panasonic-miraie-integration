#!/usr/bin/env python3
"""Repository entry point. No implicit downloads, installs, deployment or live AC calls."""
from pathlib import Path
import argparse
import ast
import json
import os
import platform
import re
import runpy
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def projects():
    data = json.loads((ROOT / 'projects.json').read_text())
    if data.get('schemaVersion') != 1:
        raise ValueError('Unsupported project registry version')
    return data['projects']


def target(name):
    for entry in projects():
        if entry['id'] == name:
            return entry
    raise ValueError('Unknown target: ' + name)


def planned(entry):
    return entry['status'] in ['planned', 'design']


def run(args, cwd=ROOT):
    subprocess.run([sys.executable if arg == '{python}' else arg for arg in args], cwd=cwd, check=True)


def inspect(entry):
    if planned(entry):
        return [entry['reason']]
    if entry['toolchain'] == 'android-poc':
        return runpy.run_path(str(ROOT / 'tools/android/build.py'))['prerequisites']()
    problems = [str(Path(entry['path']) / item) for item in entry.get('requiredFiles', []) if not (ROOT / entry['path'] / item).exists()]
    if entry['toolchain'] == 'python-uv':
        if not shutil.which('uv'):
            problems.append('uv (install explicitly, then uv sync --frozen in the server directory)')
        executable = ROOT / entry['path'] / '.venv' / ('Scripts/python.exe' if os.name == 'nt' else 'bin/python')
        if not executable.exists():
            problems.append('Server virtual environment: run uv sync --frozen in ' + entry['path'])
        else:
            result = subprocess.run([str(executable), '-c', 'import mcp, httpx, keyring, filelock, platformdirs, pytest'], capture_output=True)
            if result.returncode:
                problems.append('Server dependencies: run uv sync --frozen in ' + entry['path'])
    if entry['toolchain'] == 'gradle':
        if not shutil.which('java'):
            problems.append('Java (use the version pinned by the production Gradle project)')
        if not (os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')):
            problems.append('ANDROID_SDK_ROOT or ANDROID_HOME')
    if entry['toolchain'] == 'xcode':
        if platform.system() != 'Darwin' or not shutil.which('xcodebuild'):
            problems.append('macOS with full Xcode selected')
    return problems


def check():
    entries = projects()
    ids = set()
    for entry in entries:
        if entry['id'] in ids:
            raise ValueError('Duplicate target: ' + entry['id'])
        ids.add(entry['id'])
        path = (ROOT / entry['path']).resolve()
        if not path.is_relative_to(ROOT) or not path.is_dir() or not (path / 'README.md').exists():
            raise ValueError('Invalid project path or missing README: ' + entry['path'])
        if entry['status'] not in ['prototype', 'helper', 'design', 'planned', 'active']:
            raise ValueError('Invalid lifecycle: ' + entry['id'])
        if entry['toolchain'] not in ['android-poc', 'gradle', 'xcode', 'python-uv']:
            raise ValueError('Invalid toolchain: ' + entry['id'])
        for operation in ['build', 'test']:
            if operation in entry and (not isinstance(entry[operation], list) or not all(isinstance(arg, str) for arg in entry[operation])):
                raise ValueError('Commands must be argument arrays: ' + entry['id'])
        if not planned(entry):
            for item in entry.get('requiredFiles', []):
                if not (path / item).exists():
                    raise ValueError('Missing active project file: ' + str(path / item))
    # Check maintained source only; never read private captures or synced references.
    python_files = [ROOT / 'scripts/project.py', *ROOT.glob('scripts/tests/*.py'), *ROOT.glob('tools/android/*.py'), *ROOT.glob('tools/stay-awake-android/*.py'), *ROOT.glob('prototypes/room-ac-android/*.py'), *ROOT.glob('prototypes/room-ac-android/tests/*.py')]
    python_files.extend((ROOT / 'servers/miraie-mcp/src').rglob('*.py'))
    python_files.extend((ROOT / 'servers/miraie-mcp/tests').glob('*.py'))
    for path in python_files:
        ast.parse(path.read_text(), filename=str(path.relative_to(ROOT)))
    for path in (ROOT / 'skills').rglob('*.json'):
        json.loads(path.read_text())
    skill = ROOT / 'skills/panasonic-miraie'
    if not (skill / 'SKILL.md').read_text().startswith('---\nname: panasonic-miraie\n'):
        raise ValueError('Portable skill metadata is missing')
    docs = [ROOT / 'README.md', *ROOT.glob('docs/*.md'), *ROOT.glob('apps/**/README.md'), *ROOT.glob('servers/*/README.md'), ROOT / 'packages/README.md', ROOT / 'prototypes/README.md', ROOT / 'prototypes/room-ac-android/README.md', ROOT / 'tools/stay-awake-android/README.md', ROOT / 'design/openmiraie/README.md', *skill.rglob('*.md')]
    for path in docs:
        for ref in re.findall(r'\[[^\]]*\]\(([^)]+)\)', path.read_text()):
            if ref.startswith(('http:', 'https:', '#', 'mailto:', 'app:')):
                continue
            destination = (path.parent / ref.split('#')[0]).resolve()
            if not destination.exists():
                raise ValueError('Broken link in ' + str(path.relative_to(ROOT)) + ': ' + ref)
            if path.is_relative_to(skill) and not destination.is_relative_to(skill):
                raise ValueError('Skill depends on a file outside its portable folder: ' + ref)
    prototype = ROOT / 'design/openmiraie/prototype'
    for path in prototype.glob('*.html'):
        for ref in re.findall(r'(?:src|href)="([^"#]+)"', path.read_text()):
            if not ref.startswith(('http:', 'https:', 'data:')) and not (path.parent / ref.split('#')[0]).exists():
                raise ValueError('Broken prototype asset: ' + ref)
    if not shutil.which('node'):
        raise ValueError('Node.js is required for prototype checks (see docs/development.md)')
    run(['node', '--check', str(prototype / 'app.js')])
    run(['node', str(prototype / 'checks.cjs')])
    print('Repository layout, registry, Python syntax, skill catalogs/portability, maintained links and prototype checks passed.')


def operate(entry, operation):
    if planned(entry):
        raise ValueError(entry['id'] + ' is not buildable yet. ' + entry['reason'] + ' See ' + entry['path'] + '/README.md. No PoC fallback was used.')
    if operation not in entry:
        raise ValueError('No ' + operation + ' task configured for ' + entry['id'])
    missing = inspect(entry)
    if missing:
        raise ValueError('Missing prerequisites:\n  ' + '\n  '.join(missing))
    commands = entry[operation]
    if os.name == 'nt' and commands[0] == './gradlew':
        commands = ['gradlew.bat', *commands[1:]]
    run(commands, ROOT / entry['path'])
    for artifact in entry.get('artifacts', []) if operation == 'build' else []:
        if not (ROOT / entry['path'] / artifact).exists():
            raise ValueError('Expected build output missing: ' + artifact)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    sub.add_parser('list', help='List targets and lifecycle status')
    sub.add_parser('check', help='Offline repository and prototype checks; no SDK required')
    for action in ['doctor', 'build', 'test']:
        item = sub.add_parser(action)
        item.add_argument('target', choices=[entry['id'] for entry in projects()])
    args = parser.parse_args()
    if args.action == 'list':
        for entry in projects():
            print(f"{entry['id']:14} {entry['status']:10} {entry['path']}")
    elif args.action == 'check':
        check()
    elif args.action == 'doctor':
        entry = target(args.target)
        problems = inspect(entry)
        print(('Not ready: ' + '\n  '.join(problems)) if problems else args.target + ': local build prerequisites available')
        return 2 if problems else 0
    else:
        operate(target(args.target), args.action)
    return 0


if __name__ == '__main__':
    try:
        raise SystemExit(main())
    except (ValueError, OSError, KeyError) as exc:
        print(str(exc), file=sys.stderr)
        raise SystemExit(2)
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode)
