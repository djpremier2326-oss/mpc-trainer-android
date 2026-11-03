from pathlib import Path

INDEX = Path('app/src/main/assets/index.html')

index = INDEX.read_text(encoding='utf-8')

# Give the bell SVG two visual states: outline when no reminder is set,
# solid silhouette when a reminder is active.
old_bell = "bell:'<path d=\"M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4\"/>'"
new_bell = "bell:'<path class=\"bell-outline\" d=\"M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4\"/><path class=\"bell-fill\" d=\"M12 22c1.1 0 2-.9 2-2h-4c0 1.1.9 2 2 2zm6-6v-5c0-3.1-1.6-5.6-4.5-6.3V4a1.5 1.5 0 0 0-3 0v.7C7.6 5.4 6 7.9 6 11v5l-2 2v1h16v-1l-2-2z\"/>'"
if old_bell not in index:
    raise SystemExit('Missing expected bell icon definition')
index = index.replace(old_bell, new_bell, 1)

marker = '<style id="unified-reminder-bell-fix">'
if marker not in index:
    css = r'''
<style id="unified-reminder-bell-fix">
/* Reminder bell in every theme:
   off = outline bell;
   on = solid black bell;
   on + black task = solid white bell.
   No active background, border, glow or colored frame. */
.ui-icon .bell-fill{display:none;fill:currentColor;stroke:none}
.ui-icon .bell-outline{display:block;fill:none;stroke:currentColor}

.bell-button.bell-on,
.slot:not([data-color=none]) .bell-button.bell-on{
  background:transparent!important;
  color:#000!important;
  border-color:transparent!important;
  box-shadow:none!important;
}
.bell-button.bell-on .bell-outline{display:none}
.bell-button.bell-on .bell-fill{display:block}
.slot[data-color=black] .bell-button.bell-on{color:#fff!important}

body[data-design=ice] .ice-time-block .ice-entry-reminder,
body[data-design=ice] .ice-time-block .ice-entry-reminder.has-reminder,
body[data-design=ice][data-variant=graphite] .ice-time-block .ice-entry-reminder.has-reminder{
  background:transparent!important;
  border:0!important;
  border-radius:0!important;
  box-shadow:none!important;
}
body[data-design=ice] .ice-time-block .ice-entry-reminder.has-reminder{
  color:#000!important;
}
body[data-design=ice] .ice-entry-reminder.has-reminder .bell-outline{display:none}
body[data-design=ice] .ice-entry-reminder.has-reminder .bell-fill{display:block}
body[data-design=ice] .ice-time-block[data-color=black] .ice-entry-reminder.has-reminder{
  color:#fff!important;
}
</style>
'''
    if '</head>' not in index:
        raise SystemExit('Missing </head> in index.html')
    index = index.replace('</head>', css + '</head>', 1)

INDEX.write_text(index, encoding='utf-8')
print('Unified reminder bell active/inactive styling applied')
