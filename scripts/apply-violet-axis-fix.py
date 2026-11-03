from pathlib import Path

path = Path('app/src/main/assets/index.html')
text = path.read_text(encoding='utf-8')
old = "function violetTimelineAxis(layout){return Array.from({length:25},(_,h)=>`<time style=\"top:${(layout.tops[h]??layout.total).toFixed(2)}px\">${String(h).padStart(2,'0')}:00</time>`).join('')}\nfunction violetTimelineLines(layout){return Array.from({length:25},(_,h)=>`<i class=\"violet-hour-line\" style=\"top:${(layout.tops[h]??layout.total).toFixed(2)}px\"></i>`).join('')}"
new = "function violetTimelineAxis(layout){return Array.from({length:24},(_,h)=>`<time style=\"top:${(layout.tops[h]+Math.min(7,Math.max(3,layout.heights[h]*0.16))).toFixed(2)}px\">${String(h).padStart(2,'0')}:00</time>`).join('')}\nfunction violetTimelineLines(layout){return Array.from({length:24},(_,h)=>`<i class=\"violet-hour-line\" style=\"top:${layout.tops[h+1].toFixed(2)}px\"></i>`).join('')}"
if old not in text:
    raise SystemExit('Violet axis fix failed: timeline functions not found')
text = text.replace(old, new, 1)
style = '''<style id="violet-axis-label-fix">
body[data-design=ice][data-variant=violet] .violet-event-axis time{transform:none!important;line-height:16px!important}
body[data-design=ice][data-variant=violet] .violet-event-axis time:first-child,
body[data-design=ice][data-variant=violet] .violet-event-axis time:last-child{transform:none!important}
</style>'''
if style not in text:
    text = text.replace('</head>', style + '</head>', 1)
path.write_text(text, encoding='utf-8')
print('Violet hour labels aligned inside their rows')
