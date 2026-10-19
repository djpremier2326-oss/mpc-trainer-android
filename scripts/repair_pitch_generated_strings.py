from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

# integrate_pitch_quantize.py builds Kotlin from a Python triple-quoted block. The Learn
# explanation intentionally needs a visible line break, but an actual LF inside a normal
# Kotlin quoted string breaks parsing and makes the rest of the Activity look out of scope.
bad='громкости.' + chr(10) + 'TUNE:'
good='громкости.\\nTUNE:'
if bad in s:
    s=s.replace(bad,good,1)
elif good not in s:
    raise SystemExit('16 Levels explanation string anchor missing')

p.write_text(s)
print('Pitch-generated Kotlin string literals repaired')