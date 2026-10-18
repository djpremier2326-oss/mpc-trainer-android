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
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {
 data class PadSample(val soundId:Int,val name:String,val uri:Uri)
 data class NoteEvent(val bank:Int,val pad:Int,var tick:Long,val track:Int)
 private lateinit var soundPool:SoundPool
 private val pads=Array(4){arrayOfNulls<PadSample>(16)}; private val events=mutableListOf<NoteEvent>(); private val padButtons=mutableListOf<Button>(); private val muted=BooleanArray(4)
 private val handler=Handler(Looper.getMainLooper()); private var bank=0;private var selectedPad=0;private var track=0;private var bpm=90;private var bars=2;private var quantize=16;private var swing=50
 private var recording=false;private var overdub=false;private var playing=false;private var fullLevel=false;private var noteRepeat=false;private var playStartMs=0L;private var lastTick=-1L
 private lateinit var status:TextView;private lateinit var bankLabel:TextView;private lateinit var transportLabel:TextView
 override fun onCreate(s:Bundle?){super.onCreate(s);soundPool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();setContentView(ui());refresh();screen()}
 private fun btn(t:String,f:(View)->Unit)=Button(this).apply{text=t;textSize=9f;setOnClickListener(f)}
 private fun lcd(sz:Float,b:Boolean)=TextView(this).apply{textSize=sz;setTextColor(Color.rgb(206,231,192));if(b)setTypeface(typeface,1)}
 private fun ui():View{val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,10,12,10);setBackgroundColor(Color.rgb(215,209,195))}
  root.addView(TextView(this).apply{text="MPC TRAINER • RETRO";textSize=18f;setTypeface(typeface,1);setTextColor(Color.rgb(35,35,32))},LinearLayout.LayoutParams(-1,38))
  val display=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(14,8,14,8);setBackgroundColor(Color.rgb(55,69,62))};bankLabel=lcd(16f,true);transportLabel=lcd(12f,true);status=lcd(11f,false);display.addView(bankLabel);display.addView(transportLabel);display.addView(status);root.addView(display,LinearLayout.LayoutParams(-1,105))
  fun row(vararg bs:Button)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;bs.forEach{addView(it,LinearLayout.LayoutParams(0,48,1f))}}
  val tc=btn("TC 1/$quantize"){quantize=when(quantize){8->16;16->32;else->8};(it as Button).text="TC 1/$quantize";screen()};val sw=btn("SW $swing%"){swing=when(swing){50->54;54->58;58->62;else->50};(it as Button).text="SW $swing%";screen()}
  root.addView(row(btn("BPM −"){bpm=(bpm-1).coerceAtLeast(40);screen()},btn("BPM +"){bpm=(bpm+1).coerceAtMost(300);screen()},tc,sw))
  root.addView(row(*listOf("A","B","C","D").mapIndexed{i,n->btn("BANK $n"){bank=i;refresh();screen()}}.toTypedArray()))
  val grid=GridLayout(this).apply{columnCount=4;rowCount=4;setPadding(2,3,2,3)};repeat(16){i->val b=Button(this).apply{textSize=9f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(62,61,57));setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{selectedPad=i;hit(bank,i,true);if(noteRepeat)repeatStart()};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->repeatStop()};refresh();true}};padButtons+=b;grid.addView(b,GridLayout.LayoutParams().apply{width=0;height=82;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(3,3,3,3)})};root.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
  root.addView(row(*Array(4){i->btn("TRK ${i+1}"){track=i;screen()}}))
  val full=btn("FULL LEVEL: OFF"){fullLevel=!fullLevel;(it as Button).text="FULL LEVEL: ${if(fullLevel)"ON" else "OFF"}"};val nr=btn("NOTE REPEAT: OFF"){noteRepeat=!noteRepeat;(it as Button).text="NOTE REPEAT: ${if(noteRepeat)"ON" else "OFF"}"};root.addView(row(full,nr,btn("TRACK MUTE"){muted[track]=!muted[track];status.text="TRACK ${track+1} ${if(muted[track])"MUTED" else "ON"}"},btn("STEP GRID"){stepGrid()}))
  root.addView(btn("LOAD SAMPLE TO SELECTED PAD"){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},900)},LinearLayout.LayoutParams(-1,46))
  root.addView(row(btn("REC"){record(false)},btn("OVERDUB"){record(true)},btn("STOP"){stop()},btn("PLAY"){play()}));return root}
 private fun ppq()=96L;private fun loop()=bars*4*ppq();private fun mpt()=60000.0/bpm/ppq();private fun tick()=((SystemClock.elapsedRealtime()-playStartMs)/mpt()).toLong()%loop()
 private fun q(raw:Long):Long{val step=(ppq()*4/quantize).coerceAtLeast(1);var x=(raw.toDouble()/step).roundToLong()*step;if((x/step)%2L==1L&&swing>50)x+=((step*(swing-50))/50.0).roundToLong();return x%loop()}
 private fun hit(b:Int,p:Int,user:Boolean){if(!muted[track])pads[b][p]?.let{soundPool.play(it.soundId,1f,1f,1,0,1f)};if(user&&recording&&playing){val t=q(tick());if(!overdub)events.removeAll{it.track==track&&it.tick==t&&it.pad==p&&it.bank==b};events+=NoteEvent(b,p,t,track);status.text="REC • T${track+1} • PAD ${p+1}"}else if(user)status.text="PAD ${p+1} • ${pads[b][p]?.name?:"EMPTY"}"}
 private val player=object:Runnable{override fun run(){if(!playing)return;val t=tick();if(lastTick>=0&&t<lastTick)status.text=if(recording)"RECORDING • LOOP" else "PLAYING • LOOP";events.filter{!muted[it.track]&&near(it.tick,t,loop())<=1}.forEach{e->if(e.tick!=lastTick)pads[e.bank][e.pad]?.let{soundPool.play(it.soundId,1f,1f,1,0,1f)}};lastTick=t;screen();handler.postDelayed(this,2)}}
 private fun near(a:Long,b:Long,l:Long):Long{val d=kotlin.math.abs(a-b);return minOf(d,l-d)}
 private fun play(){if(playing)return;playing=true;recording=false;playStartMs=SystemClock.elapsedRealtime();lastTick=-1;handler.post(player);status.text="PLAYING"}
 private fun record(od:Boolean){if(!od)events.removeAll{it.track==track};playing=true;recording=true;overdub=od;playStartMs=SystemClock.elapsedRealtime();lastTick=-1;handler.removeCallbacks(player);handler.post(player);status.text=if(od)"OVERDUB" else "RECORD"}
 private fun stop(){playing=false;recording=false;overdub=false;handler.removeCallbacks(player);repeatStop();lastTick=-1;status.text="STOPPED • ${events.size} EVENTS";screen()}
 private val repeater=object:Runnable{override fun run(){if(noteRepeat){hit(bank,selectedPad,true);val ms=(60000.0/bpm*(4.0/quantize)).toLong().coerceAtLeast(20);handler.postDelayed(this,ms)}}};private fun repeatStart(){handler.removeCallbacks(repeater);handler.postDelayed(repeater,(60000.0/bpm*(4.0/quantize)).toLong())};private fun repeatStop(){handler.removeCallbacks(repeater)}
 private fun stepGrid(){val step=ppq()/4;val base=(selectedPad%16)*step;val existing=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==selectedPad&&it.tick==base};if(existing>=0){events.removeAt(existing);status.text="STEP OFF • PAD ${selectedPad+1}"}else{events+=NoteEvent(bank,selectedPad,base,track);status.text="STEP ON • PAD ${selectedPad+1} • STEP ${selectedPad+1}"}}
 private fun screen(){val t=if(playing)tick() else 0;val beat=t/ppq()+1;val sub=t%ppq();bankLabel.text="BANK ${'A'+bank} • PAD ${selectedPad+1} • TRACK ${track+1}${if(muted[track])" MUTE" else ""}";transportLabel.text="$bpm BPM • $bars BARS • 001:${beat.toString().padStart(2,'0')}:${sub.toString().padStart(2,'0')} • ${events.size} EVT"}
 private fun refresh(){padButtons.forEachIndexed{i,b->val s=pads[bank][i];b.text=if(s==null)"${i+1}\nEMPTY" else "${i+1}\n${s.name.take(8)}";b.alpha=if(i==selectedPad)1f else .84f}}
 @Deprecated("Deprecated in Java") override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==900&&c==RESULT_OK){val u=d?.data?:return;try{contentResolver.takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){};val fd=contentResolver.openFileDescriptor(u,"r")?:return;val id=soundPool.load(fd.fileDescriptor,0,fd.statSize,1);fd.close();val n=name(u);pads[bank][selectedPad]=PadSample(id,n,u);status.text="LOADED • $n → PAD ${selectedPad+1}";refresh()}}
 private fun name(u:Uri):String{contentResolver.query(u,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i).substringBeforeLast('.')} ;return "SAMPLE"}
 override fun onDestroy(){handler.removeCallbacksAndMessages(null);soundPool.release();super.onDestroy()}
}
