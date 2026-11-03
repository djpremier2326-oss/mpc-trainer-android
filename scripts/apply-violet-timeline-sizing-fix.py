from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

old = "const VIOLET_EMPTY_HOUR=24;\nconst VIOLET_BUSY_HOUR=84;"
new = "const VIOLET_EMPTY_HOUR=40;\nconst VIOLET_BUSY_HOUR=40;"
if old not in text:
    raise SystemExit('Violet sizing fix failed: hour constants not found')
text = text.replace(old, new, 1)

style = '''<style id="violet-timeline-sizing-fix">
body[data-design=ice][data-variant=violet] .violet-event-card{
  display:block!important;
  align-items:flex-start!important;
  justify-content:flex-start!important;
  padding-top:6px!important;
  padding-bottom:5px!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card .violet-event-range{
  margin-top:0!important;
  margin-bottom:2px!important;
}
body[data-design=ice][data-variant=violet] .violet-event-card strong{
  margin:0!important;
}
</style>'''
if 'id="violet-timeline-sizing-fix"' not in text:
    if '</head>' not in text:
        raise SystemExit('Violet sizing fix failed: head not found')
    text = text.replace('</head>', style + '</head>', 1)

if text == original:
    raise SystemExit('Violet sizing fix made no changes')
path.write_text(text, encoding='utf-8')
print('Violet timeline hour sizing normalized and task text top-aligned')
