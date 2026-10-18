"""Build the optional Android investigation helper."""
from pathlib import Path
import runpy
root = Path(__file__).resolve().parent
builder = runpy.run_path(str(root.parent / 'android/build.py'))
if __name__ == '__main__':
    builder['build'](root, 'stay-awake.apk')
