from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')

index = INDEX.read_text(encoding='utf-8')
marker = '<style id="unified-reminder-bell-fix">'
if marker not in index:
    css = r'''
<style id="unified-reminder-bell-fix">
/* Reminder bell: icon only. No active fill, outline or glow in any theme. */
.bell-button.bell-on,
.slot:not([data-color=none]) .bell-button.bell-on{
  background:transparent!important;
  color:#000!important;
  border-color:transparent!important;
  box-shadow:none!important;
}
.bell-button.bell-on svg{
  fill:none!important;
  stroke:currentColor!important;
}

body[data-design=ice] .ice-time-block .ice-entry-reminder,
body[data-design=ice] .ice-time-block .ice-entry-reminder.has-reminder,
body[data-design=ice][data-variant=graphite] .ice-time-block .ice-entry-reminder.has-reminder{
  background:transparent!important;
  color:#000!important;
  border:0!important;
  border-radius:0!important;
  box-shadow:none!important;
}
body[data-design=ice] .ice-entry-reminder svg,
body[data-design=ice] .ice-entry-reminder.has-reminder svg{
  fill:none!important;
  stroke:currentColor!important;
}
body[data-design=ice] .ice-time-block[data-color=black] .ice-entry-reminder,
body[data-design=ice] .ice-time-block[data-color=black] .ice-entry-reminder.has-reminder{
  color:#fff!important;
}
</style>
'''
    if '</head>' not in index:
        raise SystemExit('Missing </head> in index.html')
    index = index.replace('</head>', css + '</head>', 1)

INDEX.write_text(index, encoding='utf-8')
print('Unified reminder bell styling applied')
