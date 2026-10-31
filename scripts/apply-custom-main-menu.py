from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

def replace_once(old, new, label):
    global text
    if old not in text:
        raise SystemExit(f'Custom main menu patch failed: {label} not found')
    text = text.replace(old, new, 1)

replace_once(
    "const MAIN_SECTIONS={day:'День',week:'Неделя',calendar:'Календарь',notes:'Заметки',savings:'Накопления',events:'Напоминания'};",
    "const MAIN_SECTIONS={day:'Сегодня',week:'Неделя',calendar:'Календарь',notes:'Заметки',savings:'Накопления',events:'Напоминания',wishes:'Желания',workout:'Тренировка'};",
    'main section labels'
)

replace_once(
    "function validMainMenu(menu){return Array.isArray(menu)&&menu.length===3&&new Set(menu).size===3&&menu.every(x=>['day','week','calendar','notes','savings','events'].includes(x))}",
    "function validMainMenu(menu){return Array.isArray(menu)&&menu.length===3&&new Set(menu).size===3&&menu.every(x=>['day','week','calendar','notes','savings','events','wishes','workout'].includes(x))}",
    'main menu validation'
)

replace_once(
    "function mainMenuIcon(id){return ['day','calendar'].includes(id)?referenceIcon(id):id==='savings'?referenceIcon('goals'):uiIcon(id==='events'?'bell':id)}",
    "function mainMenuIcon(id){if(id==='wishes')return iceIcon('speaker');if(id==='workout')return iceIcon('workout');return ['day','calendar'].includes(id)?referenceIcon(id):id==='savings'?referenceIcon('goals'):uiIcon(id==='events'?'bell':id)}",
    'main menu icons'
)

old_sync = "function syncDesignNavigation(){if(state.prefs?.design!=='ice'||tab!=='day')endIceEditing(false);const nav=document.querySelector('.tabs');if(state.prefs?.design!=='ice'){const goal=$('ice-goal');if(goal)goal.hidden=true;syncLegacyDesignNavigation();return}const gear=$('settings-open'),home=document.querySelector('.header-actions');if(gear&&home&&gear.parentElement!==home)home.appendChild(gear);if(!nav)return;nav.dataset.unifiedMenu='ice';nav.dataset.menuKey='';if(!nav.querySelector('#ice-add'))nav.innerHTML=`<button class=\"tab\" data-ice-action=\"today\" aria-label=\"Сегодня\">${iceIcon('today')}<span>Сегодня</span></button><button class=\"tab\" data-tab=\"week\">${iceIcon('week')}<span>Неделя</span></button><button class=\"tab\" data-tab=\"calendar\">${iceIcon('calendar')}<span>Календарь</span></button><button class=\"ice-compose\" id=\"ice-add\" aria-label=\"Открыть инструменты\">${iceIcon('plus')}</button>`;const today=nav.querySelector('[data-ice-action=today]');today.classList.toggle('active',tab==='day'&&selected===iso());today.setAttribute('aria-current',tab==='day'&&selected===iso()?'page':'false');const oldGoal=$('reference-goal');if(oldGoal)oldGoal.hidden=true;const goal=$('ice-goal');if(goal)goal.remove()}"
new_sync = "function syncDesignNavigation(){if(state.prefs?.design!=='ice'||tab!=='day')endIceEditing(false);const nav=document.querySelector('.tabs');if(state.prefs?.design!=='ice'){const goal=$('ice-goal');if(goal)goal.hidden=true;syncLegacyDesignNavigation();return}const gear=$('settings-open'),home=document.querySelector('.header-actions');if(gear&&home&&gear.parentElement!==home)home.appendChild(gear);if(!nav)return;const menu=validMainMenu(state.prefs?.mainMenu)?state.prefs.mainMenu:['day','week','calendar'],key=menu.join('|');nav.dataset.unifiedMenu='ice';if(nav.dataset.menuKey!==key){const iconFor=id=>id==='day'?'today':id==='week'?'week':id==='calendar'?'calendar':id==='notes'?'notes':id==='savings'?'savings':id==='wishes'?'speaker':id==='workout'?'workout':null;nav.innerHTML=menu.map(id=>id==='day'?`<button class=\"tab\" data-ice-action=\"today\" aria-label=\"Сегодня\">${iceIcon('today')}<span>Сегодня</span></button>`:`<button class=\"tab\" data-tab=\"${id}\" aria-label=\"${MAIN_SECTIONS[id]}\">${id==='events'?uiIcon('bell'):iceIcon(iconFor(id))}<span>${MAIN_SECTIONS[id]}</span></button>`).join('')+`<button class=\"ice-compose\" id=\"ice-add\" aria-label=\"Открыть инструменты\">${iceIcon('plus')}</button>`;nav.dataset.menuKey=key}const today=nav.querySelector('[data-ice-action=today]');if(today){today.classList.toggle('active',tab==='day'&&selected===iso());today.setAttribute('aria-current',tab==='day'&&selected===iso()?'page':'false')}nav.querySelectorAll('[data-tab]').forEach(b=>{b.classList.toggle('active',b.dataset.tab===tab);b.setAttribute('aria-current',b.dataset.tab===tab?'page':'false')});const oldGoal=$('reference-goal');if(oldGoal)oldGoal.hidden=true;const goal=$('ice-goal');if(goal)goal.remove()}"
replace_once(old_sync, new_sync, 'ice bottom navigation')

replace_once(
    "<p class=\"dialog-note\">Выбери три раздела для нижнего меню. Плюс остаётся третьей кнопкой. Выбор и порядок одинаковы во всех темах.</p>",
    "<p class=\"dialog-note\">Выбери три раздела для нижней панели. Кнопка «+» всегда остаётся справа. Можно поставить, например, Заметки вместо Календаря.</p>",
    'dialog explanation'
)
replace_once(
    "<label class=\"field\">Кнопка справа от плюса<select name=\"menuThird\"></select></label>",
    "<label class=\"field\">Третья кнопка<select name=\"menuThird\"></select></label>",
    'third slot label'
)
replace_once(
    "const rows=[menu[0],menu[1],'plus',menu[2]];",
    "const rows=[menu[0],menu[1],menu[2],'plus'];",
    'menu preview order'
)

# The current Ice CSS intentionally hid this older setting. Re-enable it now that Ice honors the setting.
replace_once(
    "body[data-design=ice] #main-menu-open{display:none}",
    "body[data-design=ice] #main-menu-open{display:block}",
    'show setting in Ice'
)

if text == original:
    raise SystemExit('Custom main menu patch made no changes')
path.write_text(text, encoding='utf-8')
print('Custom main menu enabled for Ice UI')
