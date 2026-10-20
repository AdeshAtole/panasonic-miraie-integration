#!/usr/bin/env python3
"""Build/test native macOS app. No installation, account access or appliance writes."""
from pathlib import Path
import argparse
import os
import subprocess

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['build', 'test'])
    args = parser.parse_args()
    env = os.environ.copy()
    developer = Path('/Applications/Xcode.app/Contents/Developer')
    if 'DEVELOPER_DIR' not in env and developer.is_dir():
        env['DEVELOPER_DIR'] = str(developer)
    testing = args.action == 'test'
    command = ['xcodebuild', '-project', 'OpenMirAIe.xcodeproj', '-scheme', 'OpenMirAIeMac',
               '-configuration', 'Debug', '-destination', 'platform=macOS',
               '-derivedDataPath', 'build/Tests' if testing else 'build/DerivedData',
               'CODE_SIGN_IDENTITY=-', 'CODE_SIGN_STYLE=Manual']
    if testing:
        env['OPENMIRAIE_NO_NETWORK'] = '1'
        # Keep synthetic PNG review output in ignored build/, outside an app container.
        # Production builds retain their sandbox/network-client entitlements.
        command += ['ENABLE_APP_SANDBOX=NO', 'CODE_SIGN_ENTITLEMENTS=', 'test']
    else:
        command += ['build']
    subprocess.run(command, cwd=ROOT, env=env, check=True)


if __name__ == '__main__':
    main()
