"""Portable development builder for the legacy Android PoC and investigation helper.

Production Android apps use their own Gradle wrapper and configuration instead.
"""
from pathlib import Path
import os
import shutil
import subprocess
import zipfile


def configuration():
    sdk_env = os.environ.get('ANDROID_SDK_ROOT') or os.environ.get('ANDROID_HOME')
    candidates = [Path.home() / 'Library/Android/sdk', Path.home() / 'Android/Sdk']
    sdk = Path(sdk_env).expanduser() if sdk_env else next((p for p in candidates if p.exists()), candidates[0])
    java_home = os.environ.get('JAVA_HOME')
    javac = shutil.which('javac')
    homebrew = Path('/opt/homebrew/opt/openjdk/bin')
    jdk = Path(java_home).expanduser() / 'bin' if java_home else homebrew if homebrew.exists() and (not javac or javac == '/usr/bin/javac') else Path(javac).resolve().parent if javac else homebrew
    # These pins reproduce the existing PoC, not the future production app.
    api = os.environ.get('POC_ANDROID_API', '35')
    version = os.environ.get('POC_ANDROID_BUILD_TOOLS', '35.0.0')
    return sdk, jdk, sdk / 'build-tools' / version, sdk / 'platforms' / ('android-' + api) / 'android.jar'


def executable(path):
    if os.name == 'nt':
        for suffix in ['.exe', '.bat', '.cmd']:
            candidate = Path(str(path) + suffix)
            if candidate.exists():
                return candidate
    return path


def prerequisites():
    sdk, jdk, bt, android = configuration()
    paths = [android] + [executable(jdk / name) for name in ['java', 'javac', 'keytool']] + [executable(bt / name) for name in ['aapt', 'd8', 'zipalign', 'apksigner']]
    return [str(p) for p in paths if not p.exists()]


def build(project, apk_name):
    project = Path(project).resolve()
    missing = prerequisites()
    if missing:
        raise SystemExit('Missing Android prerequisites:\n  ' + '\n  '.join(missing) + '\nSet JAVA_HOME and ANDROID_SDK_ROOT. See docs/development.md.')
    sdk, jdk, bt, android = configuration()
    output = project / 'build'
    output.mkdir(exist_ok=True)
    classes, generated = output / 'classes', output / 'generated'
    # Remove stale compiled sources after moves/deletions; preserve development.p12.
    for directory in [classes, generated]:
        if directory.exists():
            shutil.rmtree(directory)
        directory.mkdir()
    env = dict(os.environ, JAVA_HOME=str(jdk.parent), PATH=str(jdk) + os.pathsep + os.environ.get('PATH', ''))

    def run(tool, *args):
        subprocess.run([str(executable(tool)), *map(str, args)], check=True, cwd=project, env=env)

    run(bt / 'aapt', 'package', '-f', '-m', '-J', generated, '-M', project / 'AndroidManifest.xml', '-S', project / 'res', '-I', android, '-F', output / 'resources.apk')
    run(jdk / 'javac', '--release', '8', '-classpath', android, '-d', classes, *sorted(project.glob('src/**/*.java')), *sorted(generated.glob('**/*.java')))
    run(bt / 'd8', '--lib', android, '--min-api', '26', '--output', output, *sorted(classes.glob('**/*.class')))
    shutil.copy2(output / 'resources.apk', output / 'unsigned.apk')
    with zipfile.ZipFile(output / 'unsigned.apk', 'a') as archive:
        archive.write(output / 'classes.dex', 'classes.dex')
    key = output / 'development.p12'
    if not key.exists():
        run(jdk / 'keytool', '-genkeypair', '-keystore', key, '-storepass', 'android', '-keypass', 'android', '-alias', 'development', '-dname', 'CN=Room Development', '-keyalg', 'RSA', '-validity', '3650')
    run(bt / 'zipalign', '-f', '4', output / 'unsigned.apk', output / 'aligned.apk')
    run(bt / 'apksigner', 'sign', '--ks', key, '--ks-key-alias', 'development', '--ks-pass', 'pass:android', '--out', project / apk_name, output / 'aligned.apk')
    run(bt / 'apksigner', 'verify', project / apk_name)
    print('Development APK:', project / apk_name)
