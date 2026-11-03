from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

# Normal hours stay compact (42px). If an hour contains a task lasting up to
# 30 minutes, only that hour expands to 84px so each half-hour gets a full
# 42px visual slot. This keeps ordinary one-hour tasks exactly as before.
old = "const VIOLET_EMPTY_HOUR=24;\nconst VIOLET_BUSY_HOUR=84;"
new = "const VIOLET_EMPTY_HOUR=42;\nconst VIOLET_BUSY_HOUR=84;"
if old not in text:
    raise SystemExit('Violet sizing fix failed: hour constants not found')
text = text.replace(old, new, 1)

old_layout = '''function violetBuildLayout(rows){
 const busy=Array(24).fill(false);
 rows.forEach(r=>{const s=violetClockMinutes(r.time),e=Math.max(s+1,violetClockMinutes(violetTaskEnd(r)));for(let h=0;h<24;h++){if(s<(h+1)*60&&e>h*60)busy[h]=true}});
 const heights=busy.map(v=>v?VIOLET_BUSY_HOUR:VIOLET_EMPTY_HOUR),tops=[0];
 for(let h=0;h<24;h++)tops[h+1]=tops[h]+heights[h];
 const y=minute=>{const m=Math.max(0,Math.min(1440,minute)),h=Math.min(23,Math.floor(m/60));if(m>=1440)return tops[24];return tops[h]+((m-h*60)/60)*heights[h]};
 const positions={};
 rows.forEach(r=>{const s=violetClockMinutes(r.time),e=Math.max(s+15,violetClockMinutes(violetTaskEnd(r)));positions[r.i]={top:y(s),height:Math.max(30,y(e)-y(s))}});
 return {busy,heights,tops,y,positions,total:tops[24]};
}'''
new_layout = '''function violetBuildLayout(rows){
 const expanded=Array(24).fill(false);
 rows.forEach(r=>{
  const s=violetClockMinutes(r.time),e=Math.max(s+1,violetClockMinutes(violetTaskEnd(r))),duration=e-s;
  if(duration<=30){
   for(let h=0;h<24;h++)if(s<(h+1)*60&&e>h*60)expanded[h]=true;
  }
 });
 const heights=expanded.map(v=>v?VIOLET_BUSY_HOUR:VIOLET_EMPTY_HOUR),tops=[0];
 for(let h=0;h<24;h++)tops[h+1]=tops[h]+heights[h];
 const y=minute=>{const m=Math.max(0,Math.min(1440,minute)),h=Math.min(23,Math.floor(m/60));if(m>=1440)return tops[24];return tops[h]+((m-h*60)/60)*heights[h]};
 const positions={};
 rows.forEach(r=>{const s=violetClockMinutes(r.time),e=Math.max(s+15,violetClockMinutes(violetTaskEnd(r)));positions[r.i]={top:y(s),height:Math.max(1,y(e)-y(s))}});
 return {busy:expanded,heights,tops,y,positions,total:tops[24]};
}'''
if old_layout not in text:
    raise SystemExit('Violet sizing fix failed: layout function not found')
text = text.replace(old_layout, new_layout, 1)

# Only events shorter than 30 minutes use the compact title-only style.
# 30-minute tasks have a full 42px slot, so their time range stays visible.
old_fn = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'';return `"
new_fn = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],duration=Math.max(1,violetClockMinutes(violetTaskEnd(r))-violetClockMinutes(r.time)),short=duration<30,black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'';return `"
if old_fn not in text:
    raise SystemExit('Violet sizing fix failed: event markup function not found')
text = text.replace(old_fn, new_fn, 1)

old_class = "class=\"violet-event-card ${black?'is-black':''}\""
new_class = "class=\"violet-event-card ${black?'is-black':''}${short?' is-short':''}\""
if old_class not in text:
    raise SystemExit('Violet sizing fix failed: event card class not found')
text = text.replace(old_class, new_class, 1)

# uiIcon() has no trash glyph and falls back to the calendar/day icon. Use a
# dedicated trash SVG for the swipe action so Delete always shows a bin.
old_trash = "${uiIcon('trash')}<small>Удалить</small>"
new_trash = '<svg class="ui-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 7h16M9 7V4h6v3M6 7l1 14h10l1-14M10 10v7m4-7v7"/></svg><small>Удалить</small>'
if old_trash not in text:
    raise SystemExit('Violet sizing fix failed: delete icon marker not found')
text = text.replace(old_trash, new_trash, 1)

style = '''<style id="violet-timeline-sizing-fix">
body[data-design=ice][data-variant=violet] .violet-event-wrap{
  min-height:0!important;
  overflow:hidden!important;
  top:calc(var(--vt-top) + 1px)!important;
  height:calc(var(--vt-height) - 2px)!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card{
  display:flex!important;
  flex-direction:column!important;
  align-items:flex-start!important;
  justify-content:flex-start!important;
  box-sizing:border-box!important;
  inset:0!important;
  padding:4px 10px!important;
  overflow:hidden!important;
  text-align:left!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card .violet-event-range{
  position:static!important;
  display:block!important;
  flex:0 0 auto!important;
  margin:0 0 1px 0!important;
  padding:0!important;
  line-height:11px!important;
  font-size:10px!important;
  transform:none!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card strong{
  position:static!important;
  display:block!important;
  flex:0 0 auto!important;
  margin:0!important;
  padding:0!important;
  line-height:14px!important;
  font-size:13px!important;
  transform:none!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short{
  padding:1px 10px!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short .violet-event-range{
  display:none!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card.is-short strong{
  line-height:38px!important;
  white-space:nowrap!important;
  text-overflow:ellipsis!important;
  width:100%!important;
}
body[data-design=ice][data-variant=violet] .violet-reminder-mark{
  top:4px!important;
  right:8px!important;
}
</style>'''

start = text.find('<style id="violet-timeline-sizing-fix">')
if start >= 0:
    end = text.find('</style>', start)
    if end < 0:
        raise SystemExit('Violet sizing fix failed: existing style is malformed')
    text = text[:start] + style + text[end + len('</style>'):]
else:
    if '</head>' not in text:
        raise SystemExit('Violet sizing fix failed: head not found')
    text = text.replace('</head>', style + '</head>', 1)

if text == original:
    raise SystemExit('Violet sizing fix made no changes')
path.write_text(text, encoding='utf-8')
print('Violet timeline: normal hours 42px; <=30-minute hours expand; 30-minute tasks keep time labels')
