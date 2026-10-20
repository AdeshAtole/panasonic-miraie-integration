"""Build routing/lifecycle tests. No SDK, network, credentials or actual builds."""
from pathlib import Path
import importlib.util
import os
import tempfile
import unittest
from unittest import mock

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('project', ROOT / 'scripts/project.py')
project = importlib.util.module_from_spec(spec)
spec.loader.exec_module(project)
spec2 = importlib.util.spec_from_file_location('android_builder', ROOT / 'tools/android/build.py')
builder = importlib.util.module_from_spec(spec2)
spec2.loader.exec_module(builder)


class ProjectTests(unittest.TestCase):
    def test_production_targets_are_explicitly_unbuildable_and_never_fall_back(self):
        for name in ['android', 'ios']:
            with self.subTest(target=name), mock.patch.object(project, 'run') as run:
                with self.assertRaisesRegex(ValueError, 'No PoC fallback'):
                    project.operate(project.target(name), 'build')
                run.assert_not_called()

    def test_macos_routes_native_build_and_offline_tests(self):
        entry = project.target('macos')
        self.assertEqual(entry['status'], 'active')
        self.assertEqual(entry['toolchain'], 'xcode')
        self.assertIn('CoreTests/MenuBarTests.swift', entry['requiredFiles'])
        for operation in ['build', 'test']:
            with mock.patch.object(project, 'inspect', return_value=[]), mock.patch.object(project, 'run') as run, mock.patch.object(Path, 'exists', return_value=True):
                project.operate(entry, operation)
                run.assert_called_once_with(['{python}', 'manage.py', operation], ROOT / entry['path'])

    def test_mcp_is_active_and_uses_its_own_python_environment(self):
        entry = project.target('miraie-mcp')
        self.assertEqual(entry['status'], 'active')
        self.assertEqual(entry['toolchain'], 'python-uv')
        self.assertEqual(entry['path'], 'servers/miraie-mcp')
        self.assertIn('uv.lock', entry['requiredFiles'])
        for operation in ['build', 'test']:
            with mock.patch.object(project, 'inspect', return_value=[]), mock.patch.object(project, 'run') as run, mock.patch.object(Path, 'exists', return_value=True):
                project.operate(entry, operation)
                run.assert_called_once_with(entry[operation], ROOT / entry['path'])

    def test_poc_build_and_tests_delegate_from_project_directory(self):
        entry = project.target('android-poc')
        with mock.patch.object(project, 'inspect', return_value=[]), mock.patch.object(project, 'run') as run, mock.patch.object(Path, 'exists', return_value=True):
            project.operate(entry, 'build')
            run.assert_called_once_with(['{python}', 'build.py'], ROOT / 'prototypes/room-ac-android')
        with mock.patch.object(project, 'inspect', return_value=[]), mock.patch.object(project, 'run') as run:
            project.operate(entry, 'test')
            run.assert_called_once_with(['{python}', 'tests/run.py'], ROOT / 'prototypes/room-ac-android')

    def test_missing_sdk_fails_without_starting_build(self):
        with mock.patch.object(project, 'inspect', return_value=['android.jar']), mock.patch.object(project, 'run') as run:
            with self.assertRaisesRegex(ValueError, 'android.jar'):
                project.operate(project.target('android-poc'), 'build')
            run.assert_not_called()

    def test_future_active_native_targets_use_their_own_commands(self):
        for name in ['android', 'ios', 'macos']:
            entry = dict(project.target(name), status='active')
            with self.subTest(target=name), mock.patch.object(project, 'inspect', return_value=[]), mock.patch.object(project, 'run') as run, mock.patch.object(Path, 'exists', return_value=True):
                project.operate(entry, 'build')
                run.assert_called_once_with(entry['build'], ROOT / entry['path'])

    def test_apple_tests_are_not_invented(self):
        for name in ['ios']:
            with self.assertRaisesRegex(ValueError, 'No test task'):
                project.operate(dict(project.target(name), status='active'), 'test')

    def test_unknown_target_fails_clearly(self):
        with self.assertRaisesRegex(ValueError, 'Unknown target'):
            project.target('nonexistent')

    def test_environment_controls_poc_sdk_jdk_and_pins(self):
        with tempfile.TemporaryDirectory() as directory:
            env = {'ANDROID_SDK_ROOT': directory + '/sdk', 'JAVA_HOME': directory + '/jdk', 'POC_ANDROID_API': '36', 'POC_ANDROID_BUILD_TOOLS': '36.0.0'}
            with mock.patch.dict(os.environ, env, clear=True):
                sdk, jdk, bt, android = builder.configuration()
                self.assertEqual(sdk, Path(directory) / 'sdk')
                self.assertEqual(jdk, Path(directory) / 'jdk/bin')
                self.assertEqual(bt, sdk / 'build-tools/36.0.0')
                self.assertEqual(android, sdk / 'platforms/android-36/android.jar')
                self.assertIn(str(android), builder.prerequisites())

    def test_commands_use_argument_arrays_with_no_shell(self):
        with mock.patch.object(project.subprocess, 'run') as run:
            project.run(['{python}', 'build.py', 'a value with spaces'], ROOT)
            run.assert_called_once_with([project.sys.executable, 'build.py', 'a value with spaces'], cwd=ROOT, check=True)


if __name__ == '__main__':
    unittest.main()
