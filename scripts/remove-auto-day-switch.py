from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
original = text

start = "let dayFlowTouch=null,dayFlowLast=-Infinity;"
end = "function scrollScheduleToNow(){"

if start not in text or end not in text:
    raise SystemExit('Auto day switch removal failed: markers not found')

before, rest = text.split(start, 1)
_, after = rest.split(end, 1)
replacement = "// Automatic day switching at the top/bottom of the daily schedule is disabled.\nfunction turnScheduleDay(){return false}\n\n" + end
text = before + replacement + after

style = '''<style id="no-auto-day-switch-style">
.ice-day-boundary{display:none!important}
</style>'''
if 'id="no-auto-day-switch-style"' not in text:
    if '</head>' not in text:
        raise SystemExit('Auto day switch removal failed: head marker not found')
    text = text.replace('</head>', style + '</head>', 1)

if text == original:
    raise SystemExit('Auto day switch removal made no changes')

path.write_text(text, encoding='utf-8')
print('Automatic day switching disabled for daily schedule')
