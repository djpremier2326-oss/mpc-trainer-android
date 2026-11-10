"""Register the muted plum palette without changing stored variant values."""
from pathlib import Path
p=Path('app/src/main/assets/index.html');s=p.read_text()
def replace(old,new):
 global s
 assert old in s,'Plum registration marker missing: '+old[:80]
 s=s.replace(old,new,1)
replace("bg=appearance?.colorTheme==='indigo'", "bg=appearance?.colorTheme==='plum'&&variant==='violet'?'#e8e4e5':appearance?.colorTheme==='indigo'")
replace("const indigo=p.variant==='violet'", "const plum=p.variant==='violet'&&p.colorTheme==='plum',indigo=p.variant==='violet'")
replace("t=indigo?", "t=plum?['Слива','#e8e4e5','#f7f4f3','#392f36','#7d7078','#d2c8cd','#624955',false]:indigo?")
replace("l=indigo?", "l=plum?['#624955','#f7f4f3','#fffdfb','#392f36','#d2c8cd','#eee8eb','#d2c8cd']:indigo?")
replace("document.body.dataset.colorTheme=indigo?", "document.body.dataset.colorTheme=plum?'plum':indigo?")
replace("$('theme-current').textContent=indigo?", "$('theme-current').textContent=plum?'Слива':indigo?")
replace("['indigo','Индиго']].map", "['indigo','Индиго'],['plum','Слива']].map")
s=s.replace("['aurora','indigo']", "['aurora','indigo','plum']")
replace("['ice','rhythm','violet','aurora','indigo'].includes(id)", "['ice','rhythm','violet','aurora','indigo','plum'].includes(id)")
s=s.replace('</head>','<link rel="stylesheet" href="plum.css"></head>',1)
p.write_text(s)
