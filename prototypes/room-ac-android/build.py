"""Build the retained Android proof of concept, not the production app."""
from pathlib import Path
import runpy
root = Path(__file__).resolve().parent
builder = runpy.run_path(str(root.parents[1] / 'tools/android/build.py'))
if __name__ == '__main__':
    builder['build'](root, 'room-ac.apk')
