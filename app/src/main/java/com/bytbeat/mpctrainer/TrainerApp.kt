package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Phone-only presentation layer. Audio and trainer semantics stay in TrainerActivity. */
class TrainerApp : Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is TrainerActivity) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.tag == "phone_layout_fixed_v2") return
        val root = content.getChildAt(0) as? LinearLayout ?: return

        tunePads(activity, root)
        tuneRows(activity, root)
        tuneHeader(activity, root)

        content.removeView(root)
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
            isVerticalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            addView(root, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ))
        }
        content.addView(scroll, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))
        content.tag = "phone_layout_fixed_v2"
    }

    private fun tunePads(activity: Activity, root: ViewGroup) {
        findFirst(root, GridLayout::class.java)?.let { grid ->
            (grid.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                lp.topMargin = activity.dp(4)
                lp.bottomMargin = activity.dp(5)
                grid.layoutParams = lp
            }
            for (i in 0 until grid.childCount) {
                (grid.getChildAt(i) as? Button)?.apply {
                    layoutParams = layoutParams.apply { height = activity.dp(64) }
                    minHeight = 0
                    minimumHeight = 0
                    setPadding(activity.dp(2), activity.dp(2), activity.dp(2), activity.dp(2))
                    textSize = 10f
                    setTextColor(Color.rgb(244, 241, 232))
                    setBackgroundColor(Color.rgb(58, 57, 53))
                }
            }
        }
    }

    private fun tuneRows(activity: Activity, root: ViewGroup) {
        for (i in 0 until root.childCount) {
            val row = root.getChildAt(i) as? LinearLayout ?: continue
            if (row.orientation != LinearLayout.HORIZONTAL) continue
            row.layoutParams = row.layoutParams.apply { height = activity.dp(44) }
            for (j in 0 until row.childCount) {
                (row.getChildAt(j) as? Button)?.apply {
                    minHeight = 0
                    minimumHeight = 0
                    gravity = Gravity.CENTER
                    isAllCaps = false
                    setPadding(activity.dp(2), 0, activity.dp(2), 0)
                    textSize = 9.5f
                    setTextColor(Color.rgb(39, 38, 34))
                    setBackgroundColor(buttonColor(text.toString()))
                }
            }
        }
    }

    private fun tuneHeader(activity: Activity, root: ViewGroup) {
        findFirst(root, TextView::class.java) {
            it.text?.toString()?.startsWith("MPC TRAINER") == true
        }?.apply {
            text = "MPC TRAINER  •  LIVE III BASICS"
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(activity.dp(2), activity.dp(5), 0, activity.dp(5))
        }
    }

    private fun buttonColor(label: String): Int = when {
        label == "REC" -> Color.rgb(196, 79, 58)
        label == "OVERDUB" -> Color.rgb(218, 137, 62)
        label == "PLAY" -> Color.rgb(171, 184, 151)
        label == "STOP" -> Color.rgb(190, 188, 177)
        label.startsWith("BANK") -> Color.rgb(201, 194, 174)
        label == "LEARN" -> Color.rgb(210, 185, 105)
        label == "SAMPLE EDIT" || label == "STEP SEQ" -> Color.rgb(194, 190, 177)
        else -> Color.rgb(211, 211, 204)
    }

    private fun <T : View> findFirst(
        root: View,
        type: Class<T>,
        predicate: (T) -> Boolean = { true }
    ): T? {
        if (type.isInstance(root)) {
            val value = type.cast(root)
            if (value != null && predicate(value)) return value
        }
        if (root is ViewGroup) {
            for (i in 0 until root.childCount) {
                val found = findFirst(root.getChildAt(i), type, predicate)
                if (found != null) return found
            }
        }
        return null
    }

    private fun Activity.dp(value: Int): Int =
        (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onActivityCreated(a: Activity, b: Bundle?) {}
    override fun onActivityStarted(a: Activity) {}
    override fun onActivityPaused(a: Activity) {}
    override fun onActivityStopped(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}
