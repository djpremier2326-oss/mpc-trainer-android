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
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToLong

class MainActivity : AppCompatActivity() {
    data class PadSample(val soundId: Int, val name: String, val uri: Uri)
    data class NoteEvent(val bank: Int, val pad: Int, var tick: Long, val track: Int)

    private lateinit var soundPool: SoundPool
    private val pads = Array(4) { arrayOfNulls<PadSample>(16) }
    private val events = mutableListOf<NoteEvent>()
    private val padButtons = mutableListOf<Button>()
    private val handler = Handler(Looper.getMainLooper())

    private var bank = 0
    private var selectedPad = 0
    private var track = 0
    private var bpm = 90
    private var bars = 2
    private var quantize = 16
    private var swing = 50
    private var recording = false
    private var overdub = false
    private var playing = false
    private var playStartMs = 0L
    private var lastLoopTick = -1L

    private lateinit var status: TextView
    private lateinit var bankLabel: TextView
    private lateinit var transportLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        soundPool = SoundPool.Builder().setMaxStreams(16)
            .setAudioAttributes(AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            .build()
        setContentView(buildUi())
        refreshPads()
        updateScreen()
    }

    private fun buildUi(): View {
        val shell = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 14)
            setBackgroundColor(Color.rgb(215, 209, 195))
        }
        shell.addView(TextView(this).apply {
            text = "MPC TRAINER  •  RETRO"
            textSize = 19f
            setTextColor(Color.rgb(35,35,32)); setTypeface(typeface,1)
        }, LinearLayout.LayoutParams(-1, 42))

        val screen = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(16,10,16,10)
            setBackgroundColor(Color.rgb(55,69,62))
        }
        bankLabel = lcd(17f, true)
        transportLabel = lcd(13f, true)
        status = lcd(12f, false)
        screen.addView(bankLabel); screen.addView(transportLabel); screen.addView(status)
        shell.addView(screen, LinearLayout.LayoutParams(-1, 116))

        val controls = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val bpmDown = smallButton("BPM −") { bpm = (bpm - 1).coerceAtLeast(40); updateScreen() }
        val bpmUp = smallButton("BPM +") { bpm = (bpm + 1).coerceAtMost(300); updateScreen() }
        val tc = smallButton("TC 1/$quantize") {
            quantize = when(quantize){8->16;16->32;else->8}; (it as Button).text="TC 1/$quantize"; updateScreen()
        }
        val sw = smallButton("SW $swing%") {
            swing = when(swing){50->54;54->58;58->62;else->50}; (it as Button).text="SW $swing%"; updateScreen()
        }
        listOf(bpmDown,bpmUp,tc,sw).forEach { controls.addView(it, LinearLayout.LayoutParams(0,52,1f)) }
        shell.addView(controls)

        val bankRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("A","B","C","D").forEachIndexed { i,n -> bankRow.addView(smallButton("BANK $n") { bank=i; refreshPads(); updateScreen() }, LinearLayout.LayoutParams(0,50,1f)) }
        shell.addView(bankRow)

        val grid = GridLayout(this).apply { columnCount=4; rowCount=4; setPadding(3,5,3,5) }
        repeat(16) { i ->
            val b=Button(this).apply {
                textSize=10f; gravity=Gravity.CENTER; setTextColor(Color.WHITE); setBackgroundColor(Color.rgb(62,61,57))
                setOnTouchListener { _,e -> if(e.action==MotionEvent.ACTION_DOWN){ selectedPad=i; triggerPad(bank,i,true); refreshPads() }; true }
            }
            padButtons += b
            grid.addView(b, GridLayout.LayoutParams().apply { width=0;height=96;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);setMargins(4,4,4,4) })
        }
        shell.addView(grid, LinearLayout.LayoutParams(-1,0,1f))

        val trackRow=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        repeat(4){i->trackRow.addView(smallButton("TRK ${i+1}"){track=i;updateScreen()},LinearLayout.LayoutParams(0,48,1f))}
        shell.addView(trackRow)

        shell.addView(smallButton("LOAD SAMPLE TO SELECTED PAD") {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="audio/*"},900)
        }, LinearLayout.LayoutParams(-1,52))

        val transport=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL}
        transport.addView(smallButton("REC"){ startRecord(false) },LinearLayout.LayoutParams(0,54,1f))
        transport.addView(smallButton("OVERDUB"){ startRecord(true) },LinearLayout.LayoutParams(0,54,1f))
        transport.addView(smallButton("STOP"){ stopTransport() },LinearLayout.LayoutParams(0,54,1f))
        transport.addView(smallButton("PLAY"){ startPlay() },LinearLayout.LayoutParams(0,54,1f))
        shell.addView(transport)
        return shell
    }

    private fun lcd(size:Float,bold:Boolean)=TextView(this).apply{textSize=size;setTextColor(Color.rgb(206,231,192));if(bold)setTypeface(typeface,1)}
    private fun smallButton(label:String, action:(View)->Unit)=Button(this).apply{text=label;textSize=10f;setOnClickListener(action)}

    private fun ticksPerBeat()=96L
    private fun loopTicks()=bars*4*ticksPerBeat()
    private fun msPerTick()=60000.0/bpm/ticksPerBeat()
    private fun currentTick():Long=((SystemClock.elapsedRealtime()-playStartMs)/msPerTick()).toLong()%loopTicks()

    private fun quantizedTick(raw:Long):Long {
        val step = (ticksPerBeat()*4/quantize).coerceAtLeast(1)
        var q=(raw.toDouble()/step).roundToLong()*step
        val stepIndex=q/step
        if(stepIndex%2L==1L && swing>50) q += ((step*(swing-50))/50.0).roundToLong()
        return q%loopTicks()
    }

    private fun triggerPad(b:Int,p:Int,user:Boolean){
        pads[b][p]?.let{soundPool.play(it.soundId,1f,1f,1,0,1f)}
        if(user && recording && playing){
            val tick=quantizedTick(currentTick())
            if(!overdub) events.removeAll{it.track==track && kotlin.math.abs(it.tick-tick)<3 && it.bank==b && it.pad==p}
            events += NoteEvent(b,p,tick,track)
            status.text="REC • T${track+1} • PAD ${p+1} • $tick"
        } else if(user) status.text="PAD ${p+1} • ${pads[b][p]?.name ?: "EMPTY"}"
    }

    private val player=object:Runnable{
        override fun run(){
            if(!playing)return
            val tick=currentTick()
            if(lastLoopTick>=0 && tick<lastLoopTick) status.text=if(recording) "RECORDING • LOOP" else "PLAYING • LOOP"
            val window=(2.0/msPerTick()).toLong().coerceAtLeast(1)
            events.filter{it.track==track && circularDistance(it.tick,tick,loopTicks())<=window}.forEach{e->
                if(e.tick!=lastLoopTick) triggerPad(e.bank,e.pad,false)
            }
            lastLoopTick=tick
            updateScreen()
            handler.postDelayed(this,2)
        }
    }

    private fun circularDistance(a:Long,b:Long,len:Long):Long{val d=kotlin.math.abs(a-b);return minOf(d,len-d)}

    private fun startPlay(){if(playing)return;playing=true;recording=false;overdub=false;playStartMs=SystemClock.elapsedRealtime();lastLoopTick=-1;handler.post(player);status.text="PLAYING";updateScreen()}
    private fun startRecord(isOverdub:Boolean){
        if(!isOverdub) events.removeAll{it.track==track}
        playing=true;recording=true;overdub=isOverdub;playStartMs=SystemClock.elapsedRealtime();lastLoopTick=-1;handler.removeCallbacks(player);handler.post(player)
        status.text=if(isOverdub)"OVERDUB • T${track+1}" else "REC • T${track+1}"
        updateScreen()
    }
    private fun stopTransport(){playing=false;recording=false;overdub=false;handler.removeCallbacks(player);lastLoopTick=-1;status.text="STOPPED • ${events.size} EVENTS";updateScreen()}

    private fun updateScreen(){
        val tick=if(playing)currentTick() else 0L
        val beat=tick/ticksPerBeat()+1
        val sub=tick%ticksPerBeat()
        bankLabel.text="BANK ${'A'+bank}  PAD ${selectedPad+1}  •  TRACK ${track+1}"
        transportLabel.text="${bpm} BPM   ${bars} BARS   001:${beat.toString().padStart(2,'0')}:${sub.toString().padStart(2,'0')}"
    }

    private fun refreshPads(){padButtons.forEachIndexed{i,b->val s=pads[bank][i];b.text=if(s==null)"${i+1}\nEMPTY" else "${i+1}\n${s.name.take(9)}";b.alpha=if(i==selectedPad)1f else .84f}}

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==900&&resultCode==RESULT_OK){
            val uri=data?.data?:return
            try{contentResolver.takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION)}catch(_:Exception){}
            val fd=contentResolver.openFileDescriptor(uri,"r")?:return
            val id=soundPool.load(fd.fileDescriptor,0,fd.statSize,1);fd.close()
            val name=displayName(uri);pads[bank][selectedPad]=PadSample(id,name,uri);status.text="LOADED • $name → PAD ${selectedPad+1}";refreshPads()
        }
    }
    private fun displayName(uri:Uri):String{contentResolver.query(uri,null,null,null,null)?.use{c->val i=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(i>=0&&c.moveToFirst())return c.getString(i).substringBeforeLast('.')} ;return "SAMPLE"}
    override fun onDestroy(){handler.removeCallbacks(player);soundPool.release();super.onDestroy()}
}
