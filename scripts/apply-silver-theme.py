"""Register Silver independently; reuse existing layout with a neutral palette."""
from pathlib import Path
import re
p=Path('app/src/main/assets/index.html');s=p.read_text()
def replace(old,new):
 global s
 assert old in s,old
 s=s.replace(old,new,1)
replace("bg=appearance?.colorTheme==='plum'", "bg=appearance?.colorTheme==='silver'&&variant==='violet'?'#e5e5e5':appearance?.colorTheme==='plum'")
replace("const plum=p.variant==='violet'", "const silver=p.variant==='violet'&&p.colorTheme==='silver',plum=p.variant==='violet'")
replace("t=plum?", "t=silver?['Серебро','#e5e5e5','#fafbfc','#202124','#66696e','#c6c9ce','#d4e3fd',false]:plum?")
replace("l=plum?", "l=silver?['#2b2b2b','#fafbfc','#d4e3fd','#202124','#b6bbc3','#f2f3f5','#c6c9ce']:plum?")
replace("document.body.dataset.colorTheme=plum?", "document.body.dataset.colorTheme=silver?'silver':plum?")
replace("$('theme-current').textContent=plum?", "$('theme-current').textContent=silver?'Серебро':plum?")
replace("['plum','Слива']].map", "['plum','Слива'],['silver','Серебро']].map")
s=s.replace("['aurora','indigo','plum']", "['aurora','indigo','plum','silver']")
s=s.replace('</head>','<link rel="stylesheet" href="silver-base.css"><link rel="stylesheet" href="silver.css"></head>',1)
p.write_text(s)
# Reuse the established palette-only layout rather than forking the large legacy
# stylesheet. Convert only literal colors; manual day/shift colors use variables.
base=p.parent/'plum.css'
def gray(m):
 h=m.group(0)[1:]
 if len(h) in (3,4):h=''.join(c*2 for c in h)
 r,g,b=(int(h[i:i+2],16) for i in (0,2,4));v=round(.2126*r+.7152*g+.0722*b)
 return '#'+f'{v:02x}'*3+h[6:]
css=re.sub(r'#[0-9a-fA-F]{8}\b|#[0-9a-fA-F]{6}\b|#[0-9a-fA-F]{4}\b|#[0-9a-fA-F]{3}\b',gray,base.read_text().replace('data-color-theme=plum','data-color-theme=silver'))
(p.parent/'silver-base.css').write_text('/* Generated neutral layout from plum.css; changes live in silver.css. */\n'+css)
