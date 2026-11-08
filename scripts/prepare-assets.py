"""Apply the existing app patches to generated assets, leaving source files untouched."""
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
ROOT = Path(__file__).resolve().parents[1]
PATCHES = [
    'scripts/apply-rhythm-theme.py',
    'scripts/apply-custom-main-menu.py',
    'scripts/apply-theme-preview-fixes.py',
    'scripts/apply-violet-theme.py',
    'scripts/apply-violet-timeline-v3.py',
    'scripts/apply-violet-swipe-gesture-fix.py',
    'scripts/apply-violet-standard-swipe.py',
    'scripts/apply-violet-zero-default-time.py',
    'scripts/apply-violet-tools-autoscroll.py',
    'scripts/apply-violet-axis-fix.py',
    'scripts/apply-violet-timeline-sizing-fix.py',
    'scripts/apply-violet-task-icons.py',
    'scripts/apply-custom-colors.py',
    'scripts/apply-violet-walk-icon.py',
    'scripts/apply-unified-day-all-themes.py',
    'scripts/apply-notes-media-folders.py',
    'scripts/apply-notes-media-polish.py',
    'scripts/apply-wishes-video-limit.py',
    'scripts/apply-favorites-renaming.py',
    'scripts/apply-reminder-bell-fix.py',
    'scripts/remove-auto-day-switch.py',
]

def prepare():
    destination=ROOT / 'app/build/generated/plannerAssets'
    with tempfile.TemporaryDirectory(prefix='planner-assets-') as temporary:
        work=Path(temporary)
        assets=work / 'app/src/main/assets'
        shutil.copytree(ROOT / 'app/src/main/assets',assets)
        for patch in PATCHES:
            subprocess.run([sys.executable,str(ROOT / patch)],cwd=work,check=True)
        page=assets / 'index.html'
        page.write_text(page.read_text().replace('</head>','<link rel="stylesheet" href="calendar-tools.css"></head>').replace('</body>','<script src="calendar-tools.js"></script></body>'))
        destination.mkdir(parents=True,exist_ok=True)
        for child in destination.iterdir():
            if child.is_dir():shutil.rmtree(child)
            else:child.unlink()
        shutil.copytree(assets,destination,dirs_exist_ok=True)
if __name__=='__main__':prepare()
