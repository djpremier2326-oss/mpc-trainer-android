package com.bytbeat.mpctrainer

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Adapts the trainer surface to real phone screens without touching audio logic. */
class TrainerApp : Application(), Application.ActivityLifecycleCallbacks {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityResumed(activity: Activity) {
        if (activity !is TrainerActivity) return
        val content = activity.findViewById<FrameLayout>(android.R.id.content) ?: return
        if (content.tag == "phone_layout_fixed") return
        val root = content.getChildAt(0) as? LinearLayout ?: return

        findFirst(root, GridLayout::class.java)?.let { grid ->
            (grid.layoutParams as? LinearLayout.LayoutParams)?.let { lp ->
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                lp.weight = 0f
                grid.layoutParams = lp
            }
            for (i in 0 until grid.childCount) {
                grid.getChildAt(i).layoutParams = grid.getChildAt(i).layoutParams.apply {
                    height = activity.dp(66)
                }
            }
        }

        tuneRows(activity, root)

        content.removeView(root)
        val scroll = ScrollView(activity).apply {
            isFillViewport = true
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
        content.tag = "phone_layout_fixed"
    }

    private fun tuneRows(activity: Activity, root: ViewGroup) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            if (child is LinearLayout && child.orientation == LinearLayout.HORIZONTAL) {
                child.layoutParams = child.layoutParams.apply { height = activity.dp(48) }
                for (j in 0 until child.childCount) {
                    (child.getChildAt(j) as? Button)?.apply {
                        minHeight = 0
                        minimumHeight = 0
                        setPadding(activity.dp(3), 0, activity.dp(3), 0)
                        textSize = 9f
                    }
                }
            }
        }
        findFirst(root, TextView::class.java) {
            it.text?.toString()?.startsWith("MPC TRAINER") == true
        }?.setPadding(0, activity.dp(4), 0, activity.dp(4))
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
