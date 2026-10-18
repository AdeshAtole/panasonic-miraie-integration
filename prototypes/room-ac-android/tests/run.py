#!/usr/bin/env python3
"""Run focused JVM tests with existing local dependencies; no downloads or phone access."""
from pathlib import Path
import os
import subprocess
import tempfile
import runpy

root = Path(__file__).resolve().parents[1]
shared = runpy.run_path(str(root.parents[1] / 'tools/android/build.py'))
_, jdk, _, android_jar = shared['configuration']()
json_jar = Path(os.environ.get('ANDROID_JSON_JAR', str(Path.home() / '.m2/repository/com/vaadin/external/google/android-json/0.0.20131108.vaadin1/android-json-0.0.20131108.vaadin1.jar'))).expanduser()
for required in [jdk / 'javac', jdk / 'java', json_jar, android_jar, root / 'build/classes']:
    if not required.exists():
        raise SystemExit(f'Missing local prerequisite: {required}')
with tempfile.TemporaryDirectory(prefix='room-ac-jvm-tests-') as temporary:
    dependencies = os.pathsep.join(map(str, [json_jar, android_jar, root / 'build/classes']))
    sources = [root / 'src/in/room/ac' / f'{name}.java' for name in ['DeviceProfile', 'MetricStore', 'Telemetry']]
    sources += list((root / 'tests').rglob('*.java'))
    subprocess.run([str(jdk / 'javac'), '-encoding', 'UTF-8', '-cp', dependencies, '-d', temporary, *map(str, sources)], check=True)
    result = subprocess.run([str(jdk / 'java'), '-cp', temporary + os.pathsep + dependencies, 'in.room.ac.ProfileTelemetryTest', str(root / 'tests/fixtures/actual-ac-capabilities.json')])
    raise SystemExit(result.returncode)
