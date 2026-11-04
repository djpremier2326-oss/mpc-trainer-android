from pathlib import Path
p=Path("app/src/main/assets/index.html")
s=p.read_text(encoding="utf-8")
a="function renderIceDay(){if(state.prefs.variant==='violet')return renderVioletTimelineDay();"
assert s.count(a)==1
s=s.replace(a,"function renderIceDay(){return renderVioletTimelineDay();",1)
a="if(state.prefs?.variant!=='violet'||tab!=='day')return;"
assert s.count(a)==2
s=s.replace(a,"if(state.prefs?.design!=='ice'||tab!=='day')return;")
a="if(state.prefs?.variant==='violet'&&tab==='day')render()"
assert s.count(a)==1
s=s.replace(a,"if(state.prefs?.design==='ice'&&tab==='day')render()")
a='<div class="ice-tupac-photo"><img src="ice/new-york-city.jpg" alt="" decoding="async"></div>'
b='<div class="ice-tupac-photo"><img src="'+chr(36)+'{state.prefs.variant==="blueberry"?"blueberry/palms-sunset.png":state.prefs.variant==="graphite"?"graphite/tupac.jpg":"ice/new-york-city.jpg"}" alt="" decoding="async"></div>'
assert s.count(a)==1
s=s.replace(a,b,1)
selector="body[data-design=ice][data-variant=violet]"
other="body[data-design=ice]:not([data-variant=violet])"
css=Path("app/src/main/assets/violet/timeline.css").read_text(encoding="utf-8").replace(selector,other)
for a,b in [
 ("#121214!important","var(--u-paper)!important"),
 ("#171719","var(--u-track)"),
 ("#37373c","var(--u-line)"),
 ("#343439","var(--u-line)"),
 ("#a8a8af","var(--u-ink)"),
 ("background:#0b0b0c","background:var(--u-actions)"),
 ("border-left:1px solid #2e2e33","border-left:1px solid var(--u-line)"),
 ("background:#c59af2!important","background:var(--u-accent)!important"),
 ("color:#000!important","color:var(--u-on-accent)!important"),
 ("background:#000!important","background:var(--u-actions)!important"),
 ("border:2px solid #c59af2!important","border:2px solid var(--u-accent)!important"),
 ("linear-gradient(#c59af2,#c59af2)","linear-gradient(var(--u-accent),var(--u-accent))")]:
 css=css.replace(a,b)
extras=[]
for id in ("violet-day-tools-fix","violet-axis-label-fix","violet-timeline-sizing-fix","violet-task-auto-icons"):
 start=s.index('<style id="'+id+'">')
 end=s.index("</style>",start)+len("</style>")
 extras.append(s[start:end].replace(selector,other).replace(' id="'+id+'"',''))
extra="\n".join(extras).replace("background:#000!important","background:var(--u-tools)!important").replace("color:#fff!important","color:var(--u-tools-ink)!important")
vars="""
body[data-design=ice][data-variant=ice]{--u-paper:#061c31;--u-track:#08263e;--u-line:#36536b;--u-ink:#d5e9f9;--u-actions:#12334d;--u-accent:#51bff4;--u-on-accent:#052a40;--u-tools:#041c31;--u-tools-ink:#fff}
body[data-design=ice][data-variant=graphite]{--u-paper:#777;--u-track:#868686;--u-line:#aaa;--u-ink:#101010;--u-actions:#303030;--u-accent:#222;--u-on-accent:#fff;--u-tools:#8d8d8d;--u-tools-ink:#101010}
body[data-design=ice][data-variant=blueberry]{--u-paper:#9cabb9;--u-track:#acb9c7;--u-line:#788a9c;--u-ink:#383b47;--u-actions:#424049;--u-accent:#424049;--u-on-accent:#fff;--u-tools:#a5afbb;--u-tools-ink:#424049}
body[data-design=ice][data-variant=rhythm]{--u-paper:#e8e7e1;--u-track:#f2f1eb;--u-line:#c5c4be;--u-ink:#1c1c1a;--u-actions:#171716;--u-accent:#f3f700;--u-on-accent:#111;--u-tools:#f7f6f1;--u-tools-ink:#111}
"""
extra+="""
body[data-design=ice]:not([data-variant=violet]) .violet-event-axis{background:var(--u-track)!important}
body[data-design=ice]:not([data-variant=violet]) .violet-event-paper{background:var(--u-paper)!important;color:var(--u-ink)!important}
body[data-design=ice]:not([data-variant=violet]) .violet-day-tools button,body[data-design=ice]:not([data-variant=violet]) .violet-day-tools button svg{color:var(--u-tools-ink)!important}
body[data-design=ice]:not([data-variant=violet]) .violet-event-card .violet-task-auto-icon{color:inherit!important}
body[data-design=ice]:not([data-variant=violet]) .violet-add-task{background:var(--u-accent)!important;color:var(--u-on-accent)!important;border-color:var(--u-on-accent)!important}
body[data-design=ice]:not([data-variant=violet]) .tabs>.ice-compose{background:var(--u-actions)!important;border-color:var(--u-accent)!important}
body[data-design=ice]:not([data-variant=violet]) .tabs>.ice-compose::before{background:linear-gradient(var(--u-accent),var(--u-accent)) top center/29px 3px no-repeat,linear-gradient(var(--u-accent),var(--u-accent)) center/29px 3px no-repeat,linear-gradient(var(--u-accent),var(--u-accent)) bottom center/29px 3px no-repeat!important}
"""
Path("app/src/main/assets/unified-timeline.css").write_text(vars+css+extra,encoding="utf-8")
s=s.replace("</head>",'<link rel="stylesheet" href="unified-timeline.css"></head>',1)
p.write_text(s,encoding="utf-8")
print("Shared day view enabled across 5 themes")
