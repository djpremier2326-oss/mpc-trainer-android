from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

# Keep every hour compact. Tasks still span their real time range,
# but a 2.5 hour task no longer takes half the phone screen.
old = "const VIOLET_EMPTY_HOUR=24;\nconst VIOLET_BUSY_HOUR=84;"
new = "const VIOLET_EMPTY_HOUR=28;\nconst VIOLET_BUSY_HOUR=28;"
if old not in text:
    raise SystemExit('Violet sizing fix failed: hour constants not found')
text = text.replace(old, new, 1)

# Short events need enough room for readable text without making the whole hour tall.
old_pos = "positions[r.i]={top:y(s),height:Math.max(30,y(e)-y(s))}"
new_pos = "positions[r.i]={top:y(s),height:Math.max(28,y(e)-y(s))}"
if old_pos not in text:
    raise SystemExit('Violet sizing fix failed: event position formula not found')
text = text.replace(old_pos, new_pos, 1)

style = '''<style id="violet-timeline-sizing-fix">
body[data-design=ice][data-variant=violet] .violet-event-card{
  display:flex!important;
  flex-direction:column!important;
  align-items:flex-start!important;
  justify-content:flex-start!important;
  box-sizing:border-box!important;
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
print('Violet timeline compacted; event content pinned to top')
