from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

def replace_once(old, new, label):
    global text
    if old not in text:
        raise SystemExit(f'Violet patch failed: {label} not found')
    text = text.replace(old, new, 1)

replace_once(
    "const design='ice',variant=appearance?.variant==='rhythm'?'rhythm':appearance?.variant==='blueberry'?'blueberry':(appearance?.variant==='graphite'||(!appearance&&window.AndroidFiles?.friendSchedule?.()))?'graphite':'ice',bg=variant==='rhythm'?'#f7f6f1':variant==='blueberry'?'#a5afbb':variant==='graphite'?'#8d8d8d':'#03192e';",
    "const design='ice',variant=appearance?.variant==='violet'?'violet':appearance?.variant==='rhythm'?'rhythm':appearance?.variant==='blueberry'?'blueberry':(appearance?.variant==='graphite'||(!appearance&&window.AndroidFiles?.friendSchedule?.()))?'graphite':'ice',bg=variant==='violet'?'#171126':variant==='rhythm'?'#f7f6f1':variant==='blueberry'?'#a5afbb':variant==='graphite'?'#8d8d8d':'#03192e';",
    'startup variant'
)

replace_once(
    '<link rel="stylesheet" href="blueberry/theme.css"><link rel="stylesheet" href="rhythm/theme.css"><link rel="stylesheet" href="rhythm/fixes.css"><link rel="stylesheet" href="rhythm/fixes2.css"></head>',
    '<link rel="stylesheet" href="blueberry/theme.css"><link rel="stylesheet" href="rhythm/theme.css"><link rel="stylesheet" href="rhythm/fixes.css"><link rel="stylesheet" href="rhythm/fixes2.css"><link rel="stylesheet" href="violet/theme.css"></head>',
    'violet stylesheet'
)

replace_once(
    "['ice','graphite','blueberry','rhythm'].includes(p.variant)",
    "['ice','graphite','blueberry','rhythm','violet'].includes(p.variant)",
    'prefs validation'
)

replace_once(
    "p.variant=['graphite','blueberry','rhythm'].includes(p.variant)?p.variant:'ice';const rhythm=p.variant==='rhythm',berry=p.variant==='blueberry',graphite=p.variant==='graphite',t=rhythm?['Ритм','#f7f6f1','#ffffff','#0b0b0a','#777773','#deddd8','#f3f700',false]:berry?['Черничный туман','#a5afbb','#a5afbb','#424049','#424049','#748290','#424049',false]:graphite?['Графит','#8d8d8d','#c7c7c7','#050505','#3f3f3f','#8d8d8d','#3f3f3f',false]:THEMES[p.theme],l=rhythm?['#0b0b0a','#ffffff','#e9e8e4','#0b0b0a','#c9c8c3','#f7f6f1','#c9c8c3']:berry?['#424049','#a5afbb','#c9b8c3','#424049','#748290','#748290','#424049']:graphite?['#c7c7c7','#c7c7c7','#f3f3f3','#050505','#8d8d8d','#8d8d8d','#8d8d8d']:themeLayers(t,p);",
    "p.variant=['graphite','blueberry','rhythm','violet'].includes(p.variant)?p.variant:'ice';const violet=p.variant==='violet',rhythm=p.variant==='rhythm',berry=p.variant==='blueberry',graphite=p.variant==='graphite',t=violet?['Фиолетовый неон','#171126','#fbf8ff','#171322','#71677f','#b98ad9','#d86cff',true]:rhythm?['Ритм','#f7f6f1','#ffffff','#0b0b0a','#777773','#deddd8','#f3f700',false]:berry?['Черничный туман','#a5afbb','#a5afbb','#424049','#424049','#748290','#424049',false]:graphite?['Графит','#8d8d8d','#c7c7c7','#050505','#3f3f3f','#8d8d8d','#3f3f3f',false]:THEMES[p.theme],l=violet?['#171126','#fbf8ff','#f2e9fb','#171322','#b98ad9','#eee3fb','#b98ad9']:rhythm?['#0b0b0a','#ffffff','#e9e8e4','#0b0b0a','#c9c8c3','#f7f6f1','#c9c8c3']:berry?['#424049','#a5afbb','#c9b8c3','#424049','#748290','#748290','#424049']:graphite?['#c7c7c7','#c7c7c7','#f3f3f3','#050505','#8d8d8d','#8d8d8d','#8d8d8d']:themeLayers(t,p);",
    'applyTheme palette'
)

replace_once(
    "$('theme-current').textContent=rhythm?'Ритм':berry?'Черничный туман':graphite?'Графит':'Ледяной голубой'",
    "$('theme-current').textContent=violet?'Фиолетовый неон':rhythm?'Ритм':berry?'Черничный туман':graphite?'Графит':'Ледяной голубой'",
    'theme current label'
)

replace_once(
    "[['ice','Ледяной голубой'],['graphite','Графит'],['blueberry','Черничный туман'],['rhythm','Ритм']]",
    "[['ice','Ледяной голубой'],['graphite','Графит'],['blueberry','Черничный туман'],['rhythm','Ритм'],['violet','Фиолетовый неон']]",
    'theme variants list'
)

replace_once(
    "${(['ice','rhythm'].includes(id)?'theme-previews/'+id+'-day.svg':'theme-previews/'+id+'-day.webp')}",
    "${(['ice','rhythm','violet'].includes(id)?'theme-previews/'+id+'-day.svg':'theme-previews/'+id+'-day.webp')}",
    'theme preview source'
)

replace_once(
    "state.prefs.variant=['graphite','blueberry','rhythm'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice';",
    "state.prefs.variant=['graphite','blueberry','rhythm','violet'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice';",
    'theme click handler'
)

if text == original:
    raise SystemExit('Violet patch made no changes')
path.write_text(text, encoding='utf-8')
print('Violet Neon theme registered in index.html')
