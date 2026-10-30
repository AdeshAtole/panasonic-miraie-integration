#!/usr/bin/env python3
"""Offline test/build only; no AWS deployment, login or appliance commands."""
import argparse
import ast
from pathlib import Path
import subprocess
import sys
import zipfile

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=('build', 'test'))
    args = parser.parse_args()
    if args.action == 'test':
        subprocess.run([sys.executable, '-m', 'unittest', 'discover', '-s', 'tests', '-v'], cwd=ROOT, check=True)
    else:
        sources = sorted((ROOT / 'src').glob('*.py'))
        for path in sources:
            ast.parse(path.read_text(), filename=path.name)
        output = ROOT / 'build/display-skill.zip'
        output.parent.mkdir(exist_ok=True)
        with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as archive:
            for path in sources:
                archive.write(path, path.name)
        print('Built', output)
        print('Uses AWS Lambda Python 3.12 runtime boto3. No account access or deployment performed.')


if __name__ == '__main__':
    main()
