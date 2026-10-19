package com.bytbeat.mpctrainer

import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.OpenableColumns
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToLong

class TrainerActivity : AppCompatActivity() {
    data class PadSample(val soundId:Int,val name:String,val path:String)
    data class Event(val bank:Int,val pad:Int,val tick:Long,val track:Int,val velocity:Int)

    private lateinit var pool:SoundPool
    private val pads=Array(4){arrayOfNulls<PadSample>(16)}
    private val events=mutableListOf<Event>()
    private val mute=BooleanArray(4)
    private val padButtons=mutableListOf<Button>()
    private val h=Handler(Looper.getMainLooper())
    private lateinit var status:TextView
    private lateinit var learnCard:TextView
    private lateinit var engine:LearnEngine
    private var bank=0; private var pad=0; private var track=0
    private var bpm=90; private var tc=16; private var swing=50
    private var playing=false; private var recording=false; private var overdub=false
    private var noteRepeat=false; private var fullLevel=false; private var levels16=false
    private var sourceBank=0; private var sourcePad=0; private var startMs=0L
    private var learn=false; private var pending:Uri?=null; private var pendingName=""

    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        pool=SoundPool.Builder().setMaxStreams(24).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build()
        restorePads()
        engine=LearnEngine(getSharedPreferences("mpc",MODE_PRIVATE).getInt("learn_engine_lesson",0))
        engine.begin(snapshot())
        setContentView(buildUi())
        refreshPads(); refreshStatus()
    }

    private fun btn(label:String, click:()->Unit)=Button(this).apply{text=label;textSize=8.5f;setOnClickListener{click()}}
    private fun row(vararg views:View)=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;views.forEach{addView(it,LinearLayout.LayoutParams(0,48,1f))}}

    private fun buildUi():View{
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(10,8,10,8);setBackgroundColor(Color.rgb(216,210,195))}
        root.addView(TextView(this).apply{text="MPC TRAINER • LIVE III BASICS";textSize=16f;setTypeface(typeface,1);setTextColor(Color.rgb(38,37,33))})
        status=TextView(this).apply{setPadding(12,8,12,8);textSize=12f;setTextColor(Color.rgb(207,232,193));setBackgroundColor(Color.rgb(53,68,60))}
        root.addView(status,LinearLayout.LayoutParams(-1,72))
        learnCard=TextView(this).apply{setPadding(10,8,10,8);textSize=11f;setTextColor(Color.rgb(38,37,33));setBackgroundColor(Color.rgb(235,226,201));visibility=View.GONE}
        root.addView(learnCard)
        root.addView(row(btn("MAIN"){report("main");status.text="MAIN • sequence / track / transport"},btn("BROWSE"){report("browse");browse()},btn("SAMPLE EDIT"){status.text="SAMPLE EDIT • basic trim/chop is next module"},btn("LEARN"){learn=!learn;learnCard.visibility=if(learn)View.VISIBLE else View.GONE;if(learn){engine.begin(snapshot());showLesson()}}))
        root.addView(row(*Array(4){i->btn("BANK ${'A'+i}"){bank=i;refreshPads();refreshStatus()}}))
        val grid=GridLayout(this).apply{columnCount=4;rowCount=4}
        repeat(16){i->
            val b=Button(this).apply{
                textSize=8.5f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setBackgroundColor(Color.rgb(58,57,53))
                setOnTouchListener{_,e->when(e.action){MotionEvent.ACTION_DOWN->{pad=i;hit(i,true);report("pad");if(noteRepeat)startRepeat()};MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL->stopRepeat()};refreshPads();true}
            }
            padButtons+=b
            grid.addView(b,GridLayout.LayoutParams().apply{width=0;height=80;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(3,3,3,3)})
        }
        root.addView(grid,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(row(btn("NOTE REPEAT"){noteRepeat=!noteRepeat;report("note_repeat");refreshStatus()},btn("FULL LEVEL"){fullLevel=!fullLevel;report("full_level");refreshStatus()},btn("16 LEVELS"){if(!levels16){sourceBank=bank;sourcePad=pad};levels16=!levels16;report("16_levels");refreshPads();refreshStatus()},btn("STEP SEQ"){steps()}))
        root.addView(row(btn("TC"){tc=when(tc){8->16;16->32;else->8};report("tc");refreshStatus()},btn("SWING"){swing=when(swing){50->54;54->58;else->50};report("swing");refreshStatus()},btn("TRACK −"){track=(track+3)%4;report("track");refreshStatus()},btn("TRACK +"){track=(track+1)%4;report("track");refreshStatus()}))
        root.addView(row(btn("TRACK MUTE"){mute[track]=!mute[track];report("track_mute");refreshStatus()},btn("REC"){record(false);report("record")},btn("OVERDUB"){record(true);report("overdub")},btn("STOP"){stopTransport()}))
        root.addView(row(btn("PLAY"){play()},btn("RESET LESSON"){val r=engine.reset(snapshot());saveLesson();learn=true;learnCard.visibility=View.VISIBLE;learnCard.text=engine.lessonCard()},btn("ABOUT CONTROL"){showControlGuide()},btn("SAVE"){savePads();Toast.makeText(this,"Project state saved",Toast.LENGTH_SHORT).show()}))
        return root
    }

    private fun snapshot()=LearnEngine.Snapshot(pads.sumOf{a->a.count{it!=null}},events.size,track,mute.count{it},tc,swing,noteRepeat,fullLevel,levels16)
    private fun report(action:String){if(!learn)return;val r=engine.report(action,snapshot());if(r.advanced){saveLesson();Toast.makeText(this,"Lesson complete",Toast.LENGTH_SHORT).show()};learnCard.text=if(r.advanced)engine.lessonCard() else "${engine.title()}\n${engine.explanation()}\n\n${r.message}"}
    private fun showLesson(){learnCard.text=engine.lessonCard()}
    private fun saveLesson(){getSharedPreferences("mpc",MODE_PRIVATE).edit().putInt("learn_engine_lesson",engine.lesson).apply()}

    private fun showControlGuide(){
        val names=MpcBasics.controls.map{it.name}.toTypedArray()
        AlertDialog.Builder(this).setTitle("CONTROL GUIDE").setItems(names){_,i->val c=MpcBasics.controls[i];AlertDialog.Builder(this).setTitle(c.name).setMessage("${c.purpose}\n\nPRACTICE • ${c.practice}").setPositiveButton("OK",null).show()}.show()
    }

    private fun velocity(i:Int)=if(levels16)(((i+1)*127f/16f).roundToLong().toInt()).coerceIn(1,127) else if(fullLevel)127 else 100
    private fun hit(i:Int,user:Boolean){val b=if(levels16)sourceBank else bank;val p=if(levels16)sourcePad else i;val v=velocity(i);pads[b][p]?.let{pool.play(it.soundId,v/127f,v/127f,1,0,1f)};if(user&&recording&&playing)events+=Event(b,p,quant(tick()),track,v);refreshStatus()}
    private fun tick()=if(!playing)0L else (((SystemClock.elapsedRealtime()-startMs)/(60000.0/bpm/96.0)).toLong()%(2*4*96))
    private fun quant(raw:Long):Long{val step=(96L*4/tc).coerceAtLeast(1);var q=(raw.toDouble()/step).roundToLong()*step;if((q/step)%2L==1L&&swing>50)q+=((step*(swing-50))/50.0).roundToLong();return q%(2*4*96)}
    private fun record(od:Boolean){if(!od)events.removeAll{it.track==track};overdub=od;recording=true;playing=true;startMs=SystemClock.elapsedRealtime();refreshStatus()}
    private fun play(){recording=false;playing=true;startMs=SystemClock.elapsedRealtime();refreshStatus()}
    private fun stopTransport(){val wasOverdub=overdub;playing=false;recording=false;overdub=false;stopRepeat();report(if(wasOverdub)"overdub_stop" else "stop");refreshStatus()}

    private val repeater=object:Runnable{override fun run(){if(noteRepeat){hit(pad,true);h.postDelayed(this,(60000.0/bpm*(4.0/tc)).toLong().coerceAtLeast(25))}}}
    private fun startRepeat(){h.removeCallbacks(repeater);h.post(repeater)}
    private fun stopRepeat(){h.removeCallbacks(repeater)}

    private fun steps(){
        val box=GridLayout(this).apply{columnCount=4};val buttons=ArrayList<Button>();repeat(16){i->val b=Button(this);buttons+=b;box.addView(b,GridLayout.LayoutParams().apply{width=150;height=105})}
        fun redraw(){buttons.forEachIndexed{i,b->val t=i*24L;val on=events.any{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};b.text="${i+1}\n${if(on)"●" else "○"}";b.setOnClickListener{val ix=events.indexOfFirst{it.track==track&&it.bank==bank&&it.pad==pad&&it.tick==t};if(ix>=0)events.removeAt(ix)else{events+=Event(bank,pad,t,track,127);report("step_added")};redraw()}}}
        redraw();AlertDialog.Builder(this).setTitle("STEP SEQ • BAR 1").setView(box).setPositiveButton("DONE",null).show()
    }

    private fun browse(){startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},901)}
    @Deprecated("Deprecated in Java") override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r!=901||c!=RESULT_OK)return;val u=d?.data?:return;pending=u;pendingName=name(u);AlertDialog.Builder(this).setTitle("BROWSER • $pendingName").setMessage("Load to BANK ${'A'+bank} PAD ${pad+1}?").setNegativeButton("CANCEL",null).setPositiveButton("LOAD TO PAD"){_,_->loadPending()}.show()}
    private fun loadPending(){val u=pending?:return;val dir=File(filesDir,"samples").apply{mkdirs()};val f=File(dir,"${System.currentTimeMillis()}_${pendingName.replace(Regex("[^A-Za-z0-9._-]"),"_")}");contentResolver.openInputStream(u)?.use{input->FileOutputStream(f).use{input.copyTo(it)}};pads[bank][pad]=PadSample(pool.load(f.absolutePath,1),pendingName.substringBeforeLast('.'),f.absolutePath);savePads();refreshPads();report("loaded")}
    private fun name(u:Uri):String{contentResolver.query(u,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i)};return "sample.wav"}

    private fun savePads(){val sp=getSharedPreferences("trainer_v2",MODE_PRIVATE).edit();for(b in 0..3)for(p in 0..15){val s=pads[b][p];if(s!=null){sp.putString("p_${b}_$p",s.path);sp.putString("n_${b}_$p",s.name)}};sp.apply()}
    private fun restorePads(){val sp=getSharedPreferences("trainer_v2",MODE_PRIVATE);for(b in 0..3)for(p in 0..15){val path=sp.getString("p_${b}_$p",null);if(path!=null&&File(path).exists())pads[b][p]=PadSample(pool.load(path,1),sp.getString("n_${b}_$p","sample")?:"sample",path)}}
    private fun refreshPads(){padButtons.forEachIndexed{i,b->val s=if(levels16)pads[sourceBank][sourcePad] else pads[bank][i];b.text=if(levels16)"${i+1}\nVEL ${velocity(i)}" else "${i+1}\n${s?.name?.take(8)?:"EMPTY"}";b.alpha=if(i==pad)1f else .84f}}
    private fun refreshStatus(){status.text="SEQ 01 • TRACK ${track+1}${if(mute[track])" MUTE" else ""} • BANK ${'A'+bank} • PAD ${pad+1}\n$bpm BPM • TC 1/$tc • SWING $swing%${if(recording)if(overdub)" • OVERDUB" else " • REC" else if(playing)" • PLAY" else ""}"}
    override fun onDestroy(){h.removeCallbacksAndMessages(null);pool.release();super.onDestroy()}
}
