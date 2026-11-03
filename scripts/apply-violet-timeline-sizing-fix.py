from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

# Keep the timeline compact, but not cramped. Empty and occupied hours use the
# same height so every event stays locked to the same time grid.
old = "const VIOLET_EMPTY_HOUR=24;\nconst VIOLET_BUSY_HOUR=84;"
new = "const VIOLET_EMPTY_HOUR=42;\nconst VIOLET_BUSY_HOUR=42;"
if old not in text:
    raise SystemExit('Violet sizing fix failed: hour constants not found')
text = text.replace(old, new, 1)

# Never enlarge a short event beyond its actual time range.
old_pos = "positions[r.i]={top:y(s),height:Math.max(30,y(e)-y(s))}"
new_pos = "positions[r.i]={top:y(s),height:Math.max(1,y(e)-y(s))}"
if old_pos not in text:
    raise SystemExit('Violet sizing fix failed: event position formula not found')
text = text.replace(old_pos, new_pos, 1)

# Mark very short events so the title remains readable inside the exact slot.
old_fn = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'';return `"
new_fn = "function violetEventMarkup(r,layout){const m=layout.positions[r.i],duration=Math.max(1,violetClockMinutes(violetTaskEnd(r))-violetClockMinutes(r.time)),short=duration<45,black=r.color==='black',rem=linkedReminder(selected,r.i),bell=rem?.enabled?'●':'';return `"
if old_fn not in text:
    raise SystemExit('Violet sizing fix failed: event markup function not found')
text = text.replace(old_fn, new_fn, 1)

old_class = "class=\"violet-event-card ${black?'is-black':''}\""
new_class = "class=\"violet-event-card ${black?'is-black':''}${short?' is-short':''}\""
if old_class not in text:
    raise SystemExit('Violet sizing fix failed: event card class not found')
text = text.replace(old_class, new_class, 1)

style = '''<style id="violet-timeline-sizing-fix">
body[data-design=ice][data-variant=violet] .violet-event-wrap{
  min-height:0!important;
  overflow:hidden!important;
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
  line-height:19px!important;
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
print('Violet timeline rows set to 42px; legacy and new events align to one grid')
