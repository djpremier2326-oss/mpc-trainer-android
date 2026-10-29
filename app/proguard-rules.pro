# Called by the bundled planner JavaScript; preserve the bridge contract in release.
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
