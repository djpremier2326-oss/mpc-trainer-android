from pathlib import Path

p=Path('app/src/main/java/com/bytbeat/mpctrainer/TrainerActivity.kt')
s=p.read_text()

# Pitch/TC dialogs are generated into TrainerActivity, whose base UI historically uses raw
# pixel sizes and has no dp() helper. Keep the new dialogs density-aware without depending
# on a helper introduced by another integration script.
if 'private fun densityDp(v:Int)=' not in s:
    anchor='private fun btn(label:String,click:()->Unit)=Button(this).apply'
    if anchor not in s: raise SystemExit('button helper anchor missing')
    s=s.replace(anchor,'private fun densityDp(v:Int)=(v*resources.displayMetrics.density).toInt()\n '+anchor,1)

# Only the newly generated pitch/TC dialog padding uses dp(). Replacing these exact calls
# avoids touching any later phone UI helper if one is introduced.
s=s.replace('setPadding(dp(18),dp(8),dp(18),dp(4))','setPadding(densityDp(18),densityDp(8),densityDp(18),densityDp(4))')

# Volume integration deliberately evolves padGain to include selected bank/pad. The pitch
# integration must call the final API, not the pre-volume one-argument helper.
old='private fun padGain(v:Int)=if(levels16)(v.coerceIn(1,127)/127f) else 1f'
new='private fun padGain(v:Int,b:Int,p:Int)=(if(levels16&&levels16Type=="VELOCITY")v.coerceIn(1,127)/127f else 1f)*padVolumes[b][p]*masterVolume'
if old in s:
    s=s.replace(old,new,1)
elif new not in s:
    # The volume script normally already rewrites this helper. Accept its equivalent API.
    if 'private fun padGain(v:Int,b:Int,p:Int)' not in s:
        raise SystemExit('padGain final API anchor missing')

p.write_text(s)
print('Pitch compile API repaired: density padding and final padGain signature')