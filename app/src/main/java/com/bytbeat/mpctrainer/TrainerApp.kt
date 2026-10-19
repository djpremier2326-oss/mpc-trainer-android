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

/**
 * Phone layout adapter kept separate from the trainer/audio logic.
 * It fixes the first real-device issue: the weighted pad grid consumed all
 * remaining height and pushed the MPC controls to the bottom of the screen.
 */
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

        // Remove the 1.0 weight from the pad grid. On tall phones that weight
        // was responsible for the huge empty area visible in the first APK.
        findFirst<GridLayout>(root)?.let { grid ->
            val lp = grid.layoutParams
            if (lp is LinearLayout.LayoutParams) {
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

        // Existing control rows used raw 48 px, which is too short on a
        // high-density Android display. Give controls a readable touch target.
        tuneRows(activity, root)

        // Let compact phones scroll instead of clipping lower MPC controls.
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
                val lp = child.layoutParams
                lp.height = activity.dp(48)
                child.layoutParams = lp
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
        findFirst<TextView>(root) { it.text?.toString()?.startsWith("MPC TRAINER") == true }?.apply {
            setPadding(0, activity.dp(4), 0, activity.dp(4))
        }
    }

    private inline fun <reified T : View> findFirst(
        root: View,
        noinline predicate: (T) -> Boolean = { true }
    ): T? {
        if (root is T && predicate(root)) return root
        if (root is ViewGroup) for (i in 0 until root.childCount) {
            findFirst<T>(root.getChildAt(i), predicate)?.let { return it }
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
