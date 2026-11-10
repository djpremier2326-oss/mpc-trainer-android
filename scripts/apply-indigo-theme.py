"""Add the light indigo palette; keep the existing layout and interaction model."""
from pathlib import Path
p=Path('app/src/main/assets/index.html');s=p.read_text()
def replace(old,new):
 global s
 assert old in s,'Indigo registration marker missing: '+old[:80]
 s=s.replace(old,new,1)
replace("bg=appearance?.colorTheme==='aurora'", "bg=appearance?.colorTheme==='indigo'&&variant==='violet'?'#e7eff7':appearance?.colorTheme==='aurora'")
replace("const aurora=p.variant==='violet'", "const indigo=p.variant==='violet'&&p.colorTheme==='indigo',aurora=p.variant==='violet'")
replace("t=aurora?", "t=indigo?['Индиго','#e7eff7','#f5faff','#102146','#65758b','#b8c8db','#5341c8',false]:aurora?")
replace("l=aurora?", "l=indigo?['#e7eff7','#f6faff','#ffffff','#102146','#b8c8db','#eaf2f9','#b8c8db']:aurora?")
replace("document.body.dataset.colorTheme=aurora?'aurora':'';", "document.body.dataset.colorTheme=indigo?'indigo':aurora?'aurora':'';")
replace("$('theme-current').textContent=aurora?", "$('theme-current').textContent=indigo?'Индиго':aurora?")
replace("['aurora','Северное сияние']].map", "['aurora','Северное сияние'],['indigo','Индиго']].map")
s=s.replace("state.prefs.colorTheme==='aurora'&&state.prefs.variant==='violet'?'aurora':state.prefs.variant", "['aurora','indigo'].includes(state.prefs.colorTheme)&&state.prefs.variant==='violet'?state.prefs.colorTheme:state.prefs.variant")
replace("['ice','rhythm','violet','aurora'].includes(id)", "['ice','rhythm','violet','aurora','indigo'].includes(id)")
replace("if(b.dataset.themeVariant==='aurora'){state.prefs.variant='violet';state.prefs.colorTheme='aurora'}", "if(['aurora','indigo'].includes(b.dataset.themeVariant)){state.prefs.variant='violet';state.prefs.colorTheme=b.dataset.themeVariant}")
s=s.replace('</head>','<link rel="stylesheet" href="indigo.css"></head>',1)
p.write_text(s)
