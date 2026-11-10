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
    'scripts/separate-photos.py',
    'scripts/apply-aurora-theme.py',
    'scripts/apply-indigo-theme.py',
    'scripts/apply-plum-theme.py',
    'scripts/apply-theme-lifecycle.py',
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
        html=page.read_text()
        html=html.replace("indexedDB.open('dayplan-note-media',1)","indexedDB.open(state.prefs?.mediaDatabase||'dayplan-note-media',1)")
        startup='initializeExtras();\nif(!state.prefs.mainMenu.includes(tab))tab=state.prefs.mainMenu[0];'
        assert html.count(startup)==1, 'Planner startup insertion marker missing'
        # Install section renderers before the very first render, including when
        # Calendar is the first menu item. No legacy controls may reach appReady.
        html=html.replace(startup,'initializeExtras();\n</script><script src="photos-section.js"></script><script src="calendar-tools.js"></script><script src="calendar-icon-library.js"></script><script src="calendar-day-settings.js"></script><script src="more-menu.js"></script><script src="home-date-strip.js"></script><script src="calendar-photos.js"></script><script src="note-albums.js"></script><script src="section-controls.js"></script><script src="header-artworks.js"></script><script src="header-gallery.js"></script><script src="album-gallery.js"></script><script src="hidden-album.js"></script><script src="betting.js"></script><script>\ntab=state.prefs.mainMenu[0];')
        html=html.replace('</head>','<link rel="stylesheet" href="calendar-tools.css"><link rel="stylesheet" href="navigation-overlay.css"><link rel="stylesheet" href="calendar-day-settings.css"><link rel="stylesheet" href="more-menu.css"><link rel="stylesheet" href="home-date-strip.css"><link rel="stylesheet" href="calendar-photos.css"><link rel="stylesheet" href="note-albums.css"><link rel="stylesheet" href="section-controls.css"><link rel="stylesheet" href="header-gallery.css"><link rel="stylesheet" href="theme-consistency.css"><link rel="stylesheet" href="album-gallery.css"><link rel="stylesheet" href="photos-section.css"><link rel="stylesheet" href="betting.css"><script src="betting-core.js"></script></head>')
        html=html.replace('</body>','<script src="navigation-overlay.js"></script></body>')
        html=html.replace('</body>','<script src="data-protection.js"></script></body>')
        page.write_text(html)
        destination.mkdir(parents=True,exist_ok=True)
        for child in destination.iterdir():
            if child.is_dir():shutil.rmtree(child)
            else:child.unlink()
        shutil.copytree(assets,destination,dirs_exist_ok=True)
if __name__=='__main__':prepare()
