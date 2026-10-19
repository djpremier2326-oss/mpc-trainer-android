package com.bytbeat.mpctrainer

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView

/** Компактный Learn Mode: уроки подсвечивают нужный аппаратный контрол, экзамен — без подсказок. */
object LearnSurfaceStyler {
    private var lastTarget: Button? = null
    private var styledCard: TextView? = null

    fun update(a: Activity, root: ViewGroup) {
        val card = findText(root) { v ->
            v.visibility == View.VISIBLE && (v.text?.toString()?.contains("ЗАДАНИЕ •") == true || v.text?.toString()?.contains("ЭКЗАМЕН •") == true)
        }
        if (card == null) { clearTarget(); return }
        if (styledCard !== card) {
            styledCard = card
            card.textSize = 9.2f
            card.typeface = Typeface.DEFAULT_BOLD
            card.setTextColor(Color.rgb(236, 218, 154))
            card.setPadding(dp(a, 9), dp(a, 4), dp(a, 9), dp(a, 4))
            card.maxLines = 3
            card.background = GradientDrawable().apply { shape=GradientDrawable.RECTANGLE;setColor(Color.rgb(48,45,39));setStroke(dp(a,1),Color.rgb(112,101,73));cornerRadius=dp(a,3).toFloat() }
        }
        val text=card.text?.toString().orEmpty()
        if ("ЭКЗАМЕН •" in text) { clearTarget(); return }
        val target=targetLabel(text)?.let { findButton(root,it) }
        if(target!==lastTarget){clearTarget();lastTarget=target;target?.apply{scaleX=1.035f;scaleY=1.06f;elevation=dp(a,7).toFloat()}}
    }

    private fun targetLabel(t:String):String?=when{
        "Нажми MAIN" in t -> "MAIN"
        "BROWSE" in t -> "BROWSE"
        "REC →" in t -> "REC"
        "OVERDUB" in t -> "OVERDUB"
        "Измени TC" in t -> "TC"
        "Измени Swing" in t -> "SWING"
        "NOTE REPEAT" in t -> "NOTE REPEAT"
        "FULL LEVEL" in t -> "FULL LEVEL"
        "16 LEVELS" in t -> "16 LEVELS"
        "STEP SEQ" in t -> "STEP SEQ"
        "другой TRACK" in t || "Переключись на другой TRACK" in t -> "TRACK +"
        "TRACK MUTE" in t -> "TRACK MUTE"
        "SAMPLE EDIT" in t -> "SAMPLE EDIT"
        else -> null
    }

    private fun clearTarget(){lastTarget?.apply{scaleX=1f;scaleY=1f;elevation=0f};lastTarget=null}
    private fun findButton(root:View,label:String):Button?=find(root,Button::class.java){it.text?.toString()==label}
    private fun findText(root:View,p:(TextView)->Boolean):TextView?=find(root,TextView::class.java,p)
    private fun <T:View> find(root:View,type:Class<T>,p:(T)->Boolean):T?{if(type.isInstance(root))type.cast(root)?.let{if(p(it))return it};if(root is ViewGroup)for(i in 0 until root.childCount)find(root.getChildAt(i),type,p)?.let{return it};return null}
    private fun dp(a:Activity,v:Int)=(v*a.resources.displayMetrics.density+.5f).toInt()
}
