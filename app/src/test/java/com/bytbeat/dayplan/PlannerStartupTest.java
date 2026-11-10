package com.bytbeat.dayplan;

import android.app.Application;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.Button;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class PlannerStartupTest {
    private static final int NEUTRAL=0xff121214;
    public static class LaunchOnlyActivity extends PlannerActivity {
        int engineStarts;
        @Override protected void startWebEngine(){engineStarts++;}
    }
    private Object get(Object target,String name)throws Exception{
        Field f=PlannerActivity.class.getDeclaredField(name);f.setAccessible(true);return f.get(target);
    }
    private void set(Object target,String name,Object value)throws Exception{
        Field f=PlannerActivity.class.getDeclaredField(name);f.setAccessible(true);f.set(target,value);
    }
    private void call(Object target,String method)throws Exception{
        Method m=PlannerActivity.class.getDeclaredMethod(method);m.setAccessible(true);m.invoke(target);
    }
    private void theme(int color){RuntimeEnvironment.getApplication().getSharedPreferences("appearance",0).edit().clear().putInt("background",color).putBoolean("dark",false).commit();}
    private void assertLaunchColor(Context app,int style,int attribute){
        Context themed=new ContextThemeWrapper(app,style);
        android.content.res.TypedArray a=themed.obtainStyledAttributes(new int[]{attribute});
        try{assertEquals(NEUTRAL,a.getColor(0,0));}finally{a.recycle();}
    }
    @Test public void oldBluePreferenceCannotPaintAnIntermediateScreen()throws Exception{
        theme(0xff03192e);
        var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            FrameLayout root=(FrameLayout)get(a,"root");
            assertEquals(NEUTRAL,((ColorDrawable)root.getBackground()).getColor());
            assertEquals(NEUTRAL,((ColorDrawable)a.getWindow().getDecorView().getBackground()).getColor());
            assertTrue("Incomplete HTML must not draw",root.getViewTreeObserver().dispatchOnPreDraw());
            assertEquals(1,a.engineStarts);
        }finally{c.pause().stop().destroy();}
    }
    @Test public void savedLightThemeIsAppliedOnlyAfterThePageIsReady()throws Exception{
        theme(0xffa5afbb);
        var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            FrameLayout root=(FrameLayout)get(a,"root");
            a.new BackupBridge().themeColors("#8d8d8d",false);
            assertEquals(NEUTRAL,((ColorDrawable)root.getBackground()).getColor());
            set(a,"pageReady",true);call(a,"applyWindowTheme");
            assertEquals(0xff8d8d8d,((ColorDrawable)root.getBackground()).getColor());
            assertEquals(0xff8d8d8d,a.getWindow().getStatusBarColor());
        }finally{c.pause().stop().destroy();}
    }
    @Test public void hotResumeDoesNotRecreateTheLaunchSurface()throws Exception{
        theme(0xff171126);
        var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            FrameLayout root=(FrameLayout)get(a,"root");set(a,"pageReady",true);
            assertFalse(root.getViewTreeObserver().dispatchOnPreDraw());
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(250));
            c.pause().resume();
            assertSame(root,get(a,"root"));assertEquals(1,a.engineStarts);
            assertNull(get(a,"recoveryButton"));assertEquals(1f,root.getAlpha(),0.001f);
        }finally{c.pause().stop().destroy();}
    }
    @Test public void brokenStartupShowsRetryInsteadOfKeepingSplashForever()throws Exception{
        theme(0xff03192e);var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(11));
            assertTrue(((Button)get(a,"recoveryButton")).getText().toString().contains("Повторить"));
            assertFalse(((FrameLayout)get(a,"root")).getViewTreeObserver().dispatchOnPreDraw());
            assertEquals(NEUTRAL,((ColorDrawable)((FrameLayout)get(a,"root")).getBackground()).getColor());
        }finally{c.pause().stop().destroy();}
    }
    @Test @Config(sdk=35) public void allLegacyCachedSplashThemesAreNeutralOnAndroid15(){
        Context app=RuntimeEnvironment.getApplication();
        for(int style:new int[]{R.style.PlannerLaunchTheme,R.style.AppTheme,R.style.PlannerIceLaunchTheme,R.style.PlannerGraphiteLaunchTheme,R.style.PlannerBlueberryLaunchTheme}){
            assertLaunchColor(app,style,android.R.attr.windowBackground);
            assertLaunchColor(app,style,android.R.attr.windowSplashScreenBackground);
        }
    }
    @Test @Config(sdk=35) public void android15StartsWithTheSameNeutralWindow()throws Exception{oldBluePreferenceCannotPaintAnIntermediateScreen();}
    @Test public void calendarStatusBarMatchesPanelWithoutChangingSavedTheme()throws Exception{
        theme(0xff171126);
        var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            set(a,"pageReady",true);call(a,"applyWindowTheme");
            a.new BackupBridge().statusBarColor("#000000",true);
            assertEquals(0xff000000,a.getWindow().getStatusBarColor());
            assertEquals(0,a.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            assertEquals(0xff171126,((ColorDrawable)((FrameLayout)get(a,"root")).getBackground()).getColor());
            assertEquals(0xff171126,a.getSharedPreferences("appearance",0).getInt("background",0));
            a.new BackupBridge().statusBarColor("#c7c7c7",false);
            assertEquals(0xffc7c7c7,a.getWindow().getStatusBarColor());
            assertTrue((a.getWindow().getDecorView().getSystemUiVisibility()&View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)!=0);
            a.new BackupBridge().statusBarColor("",true);
            assertEquals(0xff171126,a.getWindow().getStatusBarColor());
        }finally{c.pause().stop().destroy();}
    }
    public static class FrameControlledWebView extends android.webkit.WebView {
        final java.util.List<VisualStateCallback> frames=new java.util.ArrayList<>();
        FrameControlledWebView(Context context){super(context);}
        @Override public void evaluateJavascript(String script,android.webkit.ValueCallback<String> callback){if(callback!=null)callback.onReceiveValue("true");}
        @Override public void postVisualStateCallback(long id,VisualStateCallback callback){frames.add(callback);}
    }
    @Test public void statusBarWaitsForCommittedPageAndIgnoresStaleNavigation()throws Exception{
        theme(0xff171126);
        var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            set(a,"pageReady",true);call(a,"applyWindowTheme");
            a.new BackupBridge().statusBarColor("#000000",true);
            FrameControlledWebView view=new FrameControlledWebView(a);set(a,"web",view);
            a.new BackupBridge().statusBarColor("#e9ddf7",false);
            assertEquals(0xff000000,a.getWindow().getStatusBarColor());
            assertEquals(1,view.frames.size());
            a.new BackupBridge().statusBarColor("#000000",true);
            view.frames.get(0).onComplete(0);
            assertEquals(0xff000000,a.getWindow().getStatusBarColor());
            view.frames.get(1).onComplete(1);
            a.new BackupBridge().statusBarColor("#e9ddf7",false);
            assertEquals(0xff000000,a.getWindow().getStatusBarColor());
            view.frames.get(2).onComplete(2);
            assertEquals(0xffe9ddf7,a.getWindow().getStatusBarColor());
        }finally{c.pause().stop().destroy();}
    }
    @Test public void resumeRevealsOnlyACommittedCurrentFrame()throws Exception{
        theme(0xffe8e4e5);var c=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();var a=c.get();
        try{
            FrameControlledWebView view=new FrameControlledWebView(a);set(a,"web",view);set(a,"pageReady",true);
            c.pause().resume();assertEquals(View.INVISIBLE,view.getVisibility());assertEquals(1,view.frames.size());
            c.pause();view.frames.get(0).onComplete(0);assertEquals(View.INVISIBLE,view.getVisibility());
            c.resume();assertEquals(2,view.frames.size());view.frames.get(1).onComplete(1);
            assertEquals(View.VISIBLE,view.getVisibility());assertSame(view,get(a,"web"));
        }finally{c.pause().stop().destroy();}
    }
    @Test public void runtimePlaceSurvivesActivityRecreationButUsesNoPersistentStorage()throws Exception{
        Field memory=PlannerActivity.class.getDeclaredField("runtimePlace");memory.setAccessible(true);memory.set(null,null);
        var first=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();
        try{var bridge=first.get().new BackupBridge();assertEquals("",bridge.runtimePlace());
            bridge.rememberRuntimePlace("{\"tab\":\"workout\",\"exercise\":\"pull\",\"date\":\"2026-11-10\",\"scroll\":120}");
            String saved=bridge.runtimePlace();assertEquals("workout",new org.json.JSONObject(saved).getString("tab"));
            bridge.rememberRuntimePlace("{broken");assertEquals(saved,bridge.runtimePlace());
            first.pause().stop().destroy();var second=Robolectric.buildActivity(LaunchOnlyActivity.class).setup();
            try{assertEquals(saved,second.get().new BackupBridge().runtimePlace());memory.set(null,null);assertEquals("",second.get().new BackupBridge().runtimePlace());}finally{second.pause().stop().destroy();}
        }finally{memory.set(null,null);}
    }
    @Test public void photoPickerSupportsMultipleUrisAndCancellation(){
        android.net.Uri a=android.net.Uri.parse("content://photos/one"),b=android.net.Uri.parse("content://photos/two");
        android.content.Intent data=new android.content.Intent().setData(a);
        android.content.ClipData clips=android.content.ClipData.newRawUri("photos",a);clips.addItem(new android.content.ClipData.Item(b));data.setClipData(clips);
        assertArrayEquals(new android.net.Uri[]{a,b},PlannerActivity.photoUris(android.app.Activity.RESULT_OK,data));
        assertArrayEquals(new android.net.Uri[]{a},PlannerActivity.photoUris(android.app.Activity.RESULT_OK,new android.content.Intent().setData(a)));
        assertNull(PlannerActivity.photoUris(android.app.Activity.RESULT_CANCELED,data));
        assertNull(PlannerActivity.photoUris(android.app.Activity.RESULT_OK,new android.content.Intent()));
        assertNull(PlannerActivity.photoUris(android.app.Activity.RESULT_OK,null));
    }
}
