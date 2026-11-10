"""Register a palette on the existing Violet layout without changing its behaviors."""
from pathlib import Path
p=Path('app/src/main/assets/index.html');s=p.read_text()
def replace(old,new):
 global s
 assert old in s, 'Aurora registration marker missing: '+old[:80]
 s=s.replace(old,new,1)
replace("bg=variant==='violet'?'#171126'", "bg=appearance?.colorTheme==='aurora'&&variant==='violet'?'#000000':variant==='violet'?'#171126'")
replace("const violet=p.variant==='violet',rhythm=", "const aurora=p.variant==='violet'&&p.colorTheme==='aurora',violet=p.variant==='violet',rhythm=")
replace("t=violet?['Фиолетовый неон'", "t=aurora?['Северное сияние','#000000','#ffffff','#090c12','#65707d','#9faaca','#21e4d9',true]:violet?['Фиолетовый неон'")
replace("l=violet?['#171126'", "l=aurora?['#000000','#ffffff','#f4fbff','#090c12','#a2bbd0','#edf8fc','#a2bbd0']:violet?['#171126'")
replace("document.body.dataset.variant=p.variant;", "document.body.dataset.variant=p.variant;document.body.dataset.colorTheme=aurora?'aurora':'';")
replace("JSON.stringify({design:p.design,variant:p.variant})", "JSON.stringify({design:p.design,variant:p.variant,colorTheme:p.colorTheme})")
replace("$('theme-current').textContent=violet?", "$('theme-current').textContent=aurora?'Северное сияние':violet?")
replace("['violet','Фиолетовый неон']].map", "['violet','Фиолетовый неон'],['aurora','Северное сияние']].map")
start=s.index('function renderThemeVariants()');end=s.index('\n',start)
line=s[start:end].replace("state.prefs.variant===id", "(state.prefs.colorTheme==='aurora'&&state.prefs.variant==='violet'?'aurora':state.prefs.variant)===id").replace("['ice','rhythm','violet'].includes(id)","['ice','rhythm','violet','aurora'].includes(id)")
s=s[:start]+line+s[end:]
replace("state.prefs.variant=['graphite','blueberry','rhythm','violet'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice';", "if(b.dataset.themeVariant==='aurora'){state.prefs.variant='violet';state.prefs.colorTheme='aurora'}else{delete state.prefs.colorTheme;state.prefs.variant=['graphite','blueberry','rhythm','violet'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice'};")
s=s.replace('</head>','<link rel="stylesheet" href="aurora.css"></head>',1)
p.write_text(s)
