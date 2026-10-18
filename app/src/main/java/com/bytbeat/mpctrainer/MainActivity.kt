package com.bytbeat.mpctrainer

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.*
import android.provider.OpenableColumns
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {
 data class PadSample(val soundId:Int,val name:String,val uri:Uri)
 data class NoteEvent(val bank:Int,val pad:Int,var tick:Long,val track:Int)
 private lateinit var pool:SoundPool;private val pads=Array(4){arrayOfNulls<PadSample>(16)};private val events=mutableListOf<NoteEvent>();private val padButtons=mutableListOf<Button>();private val mute=BooleanArray(4);private val h=Handler(Looper.getMainLooper())
 private var bank=0;private var pad=0;private var track=0;private var bpm=90;private var bars=2;private var tc=16;private var swing=50;private var rec=false;private var overdub=false;private var playing=false;private var noteRepeat=false;private var fullLevel=false;private var levels16=false;private var start=0L;private var last=-1L
 private lateinit var top:TextView;private lateinit var pos:TextView;private lateinit var info:TextView
 override fun onCreate(b:Bundle?){super.onCreate(b);pool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();setContentView(build());refresh();screen()}
 private fun button(s:String,f:(View)->Unit)=Button(this).apply{text=s;textSize=9f;setOnClickListener(f)}
 private fun row(vararg b:Button)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;b.forEach{addView(it,LinearLayout.LayoutParams(0,48,1f))}}
 private fun lcd(size:Float,bold:Boolean)=TextView(this).apply{textSize=size;setTextColor(Color.rgb(207,232,193));if(bold)setTypeface(typeface,1)}
 private fun build():View{val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,10);setBackgroundColor(Color.rgb(216,210,195))}
  root.addView(TextView(this).apply{text="MPC TRAINER  •  LIVE III WORKFLOW";textSize=17f;setTypeface(typeface,1);setTextColor(Color.rgb(38,37,33))},LinearLayout.LayoutParams(-1,38))
  val display=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,8,14,8);setBackgroundColor(Color.rgb(53,68,60))};top=lcd(15f,true);pos=lcd(12f,true);info=lcd(11f,false);display.addView(top);display.addView(pos);display.addView(info);root.addView(display,LinearLayout.LayoutParams(-1,105))
  root.addView(row(button("MAIN"){info.text="MAIN • SEQUENCE / TRACK / PROGRAM"},button("BROWSE"){openSample()},button("SAMPLE EDIT"){info.text="SAMPLE EDIT • selected PAD ${pad+1}"},button("MENU"){info.text="MENU • essential training build"}))
  root.addView(row(*listOf("A","B","C","D").mapIndexed{i,n->button("BANK $n"){bank=i;refresh();screen()}}.toTypedArray()))
  val grid=GridLayout(this).apply{columnCount=4;rowCount=4;setPadding(2,3,2,3)};repeat(16){i->val b=Button(this).apply{textSize=9f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(58,57,53));setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{pad=i;hit(bank,i,true);if(noteRepeat)repeatStart()};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->repeatStop()};refresh();true}};padButtons+=b;grid.addView(b,GridLayout.LayoutParams().apply{width=0;height=88;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(3,3,3,3)})};root.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
  val nr=button("NOTE REPEAT"){noteRepeat=!noteRepeat;info.text="NOTE REPEAT ${if(noteRepeat)"ON • hold a pad" else "OFF"}"};val fl=button("FULL LEVEL"){fullLevel=!fullLevel;info.text="FULL LEVEL ${if(fullLevel)"ON" else "OFF"}"};val lv=button("16 LEVELS"){levels16=!levels16;info.text="16 LEVELS ${if(levels16)"ON • velocity training" else "OFF"}"};root.addView(row(nr,fl,lv,button("STEP SEQ"){showSteps()}))
  root.addView(row(button("TRACK −"){track=(track+3)%4;screen()},button("TRACK +"){track=(track+1)%4;screen()},button("TRACK MUTE"){mute[track]=!mute[track];screen()},button("TC / SWING"){tc=when(tc){8->16;16->32;else->8};swing=when(swing){50->54;54->58;else->50};info.text="TIMING CORRECT 1/$tc • SWING $swing%"}))
  root.addView(row(button("REC"){record(false)},button("OVERDUB"){record(true)},button("STOP"){stop()},button("PLAY"){play()}));return root}
 private fun openSample(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},900)}
 private fun ppq()=96L;private fun loop()=bars*4*ppq();private fun mpt()=60000.0/bpm/ppq();private fun tick()=((SystemClock.elapsedRealtime()-start)/mpt()).toLong()%loop()
 private fun quant(raw:Long):Long{val step=(ppq()*4/tc).coerceAtLeast(1);var x=(raw.toDouble()/step).roundToLong()*step;if((x/step)%2L==1L&&swing>50)x+=((step*(swing-50))/50.0).roundToLong();return x%loop()}
 private fun hit(b:Int,p:Int,user:Boolean){if(!mute[track])pads[b][p]?.let{pool.play(it.soundId,1f,1f,1,0,1f)};if(user&&rec&&playing){val t=quant(tick());events+=NoteEvent(b,p,t,track);info.text="REC • TRACK ${track+1} • PAD ${p+1}"}else if(user)info.text="PAD ${p+1} • ${pads[b][p]?.name?:"EMPTY"}"}
 private val runner=object:Runnable{override fun run(){if(!playing)return;val t=tick();events.filter{!mute[it.track]&&dist(it.tick,t,loop())<=1}.forEach{e->if(e.tick!=last)pads[e.bank][e.pad]?.let{pool.play(it.soundId,1f,1f,1,0,1f)}};last=t;screen();h.postDelayed(this,2)}}
 private fun dist(a:Long,b:Long,l:Long):Long{val d=kotlin.math.abs(a-b);return minOf(d,l-d)}
 private fun play(){if(playing)return;playing=true;rec=false;start=SystemClock.elapsedRealtime();last=-1;h.post(runner);info.text="PLAY"}
 private fun record(od:Boolean){if(!od)events.removeAll{it.track==track};playing=true;rec=true;overdub=od;start=SystemClock.elapsedRealtime();last=-1;h.removeCallbacks(runner);h.post(runner);info.text=if(od)"OVERDUB • TRACK ${track+1}" else "REC • TRACK ${track+1}"}
 private fun stop(){playing=false;rec=false;overdub=false;h.removeCallbacks(runner);repeatStop();last=-1;info.text="STOP • ${events.size} EVENTS";screen()}
 private val repeater=object:Runnable{override fun run(){if(noteRepeat){hit(bank,pad,true);h.postDelayed(this,(60000.0/bpm*(4.0/tc)).toLong().coerceAtLeast(20))}}};private fun repeatStart(){h.removeCallbacks(repeater);h.postDelayed(repeater,(60000.0/bpm*(4.0/tc)).toLong())};private fun repeatStop(){h.removeCallbacks(repeater)}
 private fun showSteps(){val box=GridLayout(this).apply{columnCount=4;setPadding(16,16,16,16)};val step=ppq()/4;repeat(16){i->val t=i*step;val on=events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};box.addView(Button(this).apply{text="${i+1}\n${if(on)"●" else "○"}";setOnClickListener{val at=events.indexOfFirst{e->e.track==track&&e.bank==bank&&e.pad==pad&&e.tick==t};if(at>=0)events.removeAt(at)else events+=NoteEvent(bank,pad,t,track);showSteps()}},GridLayout.LayoutParams().apply{width=150;height=110})};AlertDialog.Builder(this).setTitle("STEP SEQUENCER • PAD ${pad+1}").setView(box).setPositiveButton("DONE",null).show()}
 private fun screen(){val t=if(playing)tick()else 0;val beat=t/ppq()+1;val sub=t%ppq();top.text="SEQ 01 • TRACK ${track+1} • BANK ${'A'+bank} • PAD ${pad+1}${if(mute[track])" • MUTE" else ""}";pos.text="$bpm BPM • $bars BARS • 001:${beat.toString().padStart(2,'0')}:${sub.toString().padStart(2,'0')}"}
 private fun refresh(){padButtons.forEachIndexed{i,b->val s=pads[bank][i];b.text=if(s==null)"${i+1}\nEMPTY" else "${i+1}\n${s.name.take(8)}";b.alpha=if(i==pad)1f else .84f}}
 @Deprecated("Deprecated in Java") override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==900&&c==RESULT_OK){val u=d?.data?:return;try{contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};val fd=contentResolver.openFileDescriptor(u,"r")?:return;val id=pool.load(fd.fileDescriptor,0,fd.statSize,1);fd.close();val n=name(u);pads[bank][pad]=PadSample(id,n,u);info.text="BROWSER • $n → BANK ${'A'+bank} PAD ${pad+1}";refresh()}}
 private fun name(u:Uri):String{contentResolver.query(u,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i).substringBeforeLast('.')} ;return "SAMPLE"}
 override fun onDestroy(){h.removeCallbacksAndMessages(null);pool.release();super.onDestroy()}
}
