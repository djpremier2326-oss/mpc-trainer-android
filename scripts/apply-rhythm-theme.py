from pathlib import Path
import re

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

def replace_once(old, new, label):
    global text
    if old not in text:
        raise SystemExit(f'Rhythm patch failed: {label} not found')
    text = text.replace(old, new, 1)

replace_once(
    "const design='ice',variant=appearance?.variant==='blueberry'?'blueberry':(appearance?.variant==='graphite'||(!appearance&&window.AndroidFiles?.friendSchedule?.()))?'graphite':'ice',bg=variant==='blueberry'?'#a5afbb':variant==='graphite'?'#8d8d8d':'#03192e';",
    "const design='ice',variant=appearance?.variant==='rhythm'?'rhythm':appearance?.variant==='blueberry'?'blueberry':(appearance?.variant==='graphite'||(!appearance&&window.AndroidFiles?.friendSchedule?.()))?'graphite':'ice',bg=variant==='rhythm'?'#f7f6f1':variant==='blueberry'?'#a5afbb':variant==='graphite'?'#8d8d8d':'#03192e';",
    'startup variant'
)
replace_once(
    "document.body.dataset.dark=String(document.body.dataset.variant!=='graphite');",
    "document.body.dataset.dark=String(!['graphite','rhythm'].includes(document.body.dataset.variant));",
    'startup dark mode'
)
replace_once(
    '<link rel="stylesheet" href="blueberry/theme.css"></head>',
    '<link rel="stylesheet" href="blueberry/theme.css"><link rel="stylesheet" href="rhythm/theme.css"></head>',
    'rhythm stylesheet link'
)
replace_once(
    "['ice','graphite','blueberry'].includes(p.variant)",
    "['ice','graphite','blueberry','rhythm'].includes(p.variant)",
    'prefs validation'
)

new_apply = """function applyTheme(){const p=state.prefs;p.design='ice';p.theme='ice';p.variant=['graphite','blueberry','rhythm'].includes(p.variant)?p.variant:'ice';const rhythm=p.variant==='rhythm',berry=p.variant==='blueberry',graphite=p.variant==='graphite',t=rhythm?['Ритм','#f7f6f1','#ffffff','#0b0b0a','#777773','#deddd8','#f3f700',false]:berry?['Черничный туман','#a5afbb','#a5afbb','#424049','#424049','#748290','#424049',false]:graphite?['Графит','#8d8d8d','#c7c7c7','#050505','#3f3f3f','#8d8d8d','#3f3f3f',false]:THEMES[p.theme],l=rhythm?['#0b0b0a','#ffffff','#e9e8e4','#0b0b0a','#c9c8c3','#f7f6f1','#c9c8c3']:berry?['#424049','#a5afbb','#c9b8c3','#424049','#748290','#748290','#424049']:graphite?['#c7c7c7','#c7c7c7','#f3f3f3','#050505','#8d8d8d','#8d8d8d','#8d8d8d']:themeLayers(t,p);const vars={'--nav-bg':l[0],'--card':l[1],'--control':l[2],'--control-text':l[3],'--outline':l[4],'--inset':l[5],'--card-edge':l[6],'--app-bg':t[1],'--surface':t[2],'--text':t[3],'--muted':t[4],'--border':t[5],'--accent':t[6],'--on-accent':accentText(t[6]),'--plan-font':({small:'13px',normal:'15px',large:'18px'})[p.font]};for(const [k,v]of Object.entries(vars))document.documentElement.style.setProperty(k,v);document.body.dataset.friendSchedule=String(FRIEND_SHIFT);document.body.dataset.variant=p.variant;document.body.dataset.theme=p.theme;document.body.dataset.design=p.design||'classic';document.body.dataset.typeface=p.typeface||'theme';document.body.dataset.dark=String(t[7]);document.body.dataset.density=p.density;document.documentElement.style.backgroundColor=t[1];document.querySelector('meta[name=\"theme-color\"]').content=t[1];try{localStorage.setItem('raspisanie-appearance-v1',JSON.stringify({design:p.design,variant:p.variant}))}catch(e){}if(window.AndroidFiles?.themeColors)AndroidFiles.themeColors(t[1],t[7]);$('theme-current').textContent=rhythm?'Ритм':berry?'Черничный туман':graphite?'Графит':'Ледяной голубой'}"""
text, count = re.subn(r"function applyTheme\(\)\{.*?\}\nfunction renderThemePicker", new_apply + "\nfunction renderThemePicker", text, count=1, flags=re.S)
if count != 1:
    raise SystemExit('Rhythm patch failed: applyTheme not found')

replace_once(
    "function renderThemeVariants(){$('theme-variants').innerHTML=[['ice','Ледяной голубой'],['graphite','Графит'],['blueberry','Черничный туман']].map(([id,name])=>`<button type=\"button\" class=\"theme-variant\" data-theme-variant=\"${id}\" aria-pressed=\"${state.prefs.variant===id}\"><img src=\"theme-previews/${id}-day.webp\" alt=\"${name}\"><span>${name}${state.prefs.variant===id?' · выбрана':''}</span></button>`).join('')}",
    "function renderThemeVariants(){$('theme-variants').innerHTML=[['ice','Ледяной голубой'],['graphite','Графит'],['blueberry','Черничный туман'],['rhythm','Ритм']].map(([id,name])=>`<button type=\"button\" class=\"theme-variant\" data-theme-variant=\"${id}\" aria-pressed=\"${state.prefs.variant===id}\"><img src=\"${id==='rhythm'?'theme-previews/rhythm-day.svg':'theme-previews/'+id+'-day.webp'}\" alt=\"${name}\"><span>${name}${state.prefs.variant===id?' · выбрана':''}</span></button>`).join('')}",
    'theme variants list'
)
replace_once(
    "state.prefs.variant=['graphite','blueberry'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice';",
    "state.prefs.variant=['graphite','blueberry','rhythm'].includes(b.dataset.themeVariant)?b.dataset.themeVariant:'ice';",
    'theme click handler'
)

if text == original:
    raise SystemExit('Rhythm patch made no changes')
path.write_text(text, encoding='utf-8')
print('Rhythm theme registered in index.html')
