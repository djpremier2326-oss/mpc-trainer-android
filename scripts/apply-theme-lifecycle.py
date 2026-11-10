"""Keep the first visible frame in the selected palette and retire Blueberry safely."""
from pathlib import Path
import re
p=Path('app/src/main/assets/index.html');s=p.read_text()
s=s.replace("function applyTheme(){const p=state.prefs;", "function applyTheme(){const p=state.prefs;if(p.variant==='blueberry'){p.variant='ice';delete p.colorTheme;}",1)
s=s.replace("if(!appearance&&!window.AndroidFiles?.friendSchedule?.())", "if(appearance?.variant==='blueberry')appearance={...appearance,variant:'ice',colorTheme:''};\nif(!appearance&&!window.AndroidFiles?.friendSchedule?.())",1)
s=s.replace('window.__plannerStartupVariant=variant;',"window.__plannerStartupVariant=variant;window.__plannerStartupColorTheme=variant==='violet'&&['aurora','indigo','plum'].includes(appearance?.colorTheme)?appearance.colorTheme:'';",1)
s=s.replace("document.body.dataset.variant=window.__plannerStartupVariant||'ice';", "document.body.dataset.variant=window.__plannerStartupVariant||'ice';document.body.dataset.colorTheme=window.__plannerStartupColorTheme||'';",1)
s=s.replace("['blueberry','Черничный туман'],",'')
start=s.index('function renderThemeVariants()');end=s.index('\n',start)
line=s[start:end];line=re.sub(r'<img src="[^"]+"', '<img src="theme-previews/${id}-today.jpg"',line);s=s[:start]+line+s[end:]
s=s.replace("if(!b)return;if(['aurora'", "if(!b||b.dataset.themeVariant==='blueberry')return;if(['aurora'",1)
s=s.replace('<style>html:not([data-app-ready]) body', '<style>html:not([data-app-ready]) main{visibility:hidden!important}html:not([data-app-ready]) body',1)
s=s.replace('</head>','<link rel="stylesheet" href="theme-gallery.css"></head>',1)
p.write_text(s)
# Exclude the retired theme's stylesheet, artwork and old preview from the APK.
import shutil
s=p.read_text().replace('<link rel="stylesheet" href="blueberry/theme.css">','').replace('<link rel="stylesheet" href="blueberry/targeted-fixes.css">','')
p.write_text(s)
shutil.rmtree(p.parent/'blueberry',ignore_errors=True)
(p.parent/'theme-previews/blueberry-day.webp').unlink(missing_ok=True)
