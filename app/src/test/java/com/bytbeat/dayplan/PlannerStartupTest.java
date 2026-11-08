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
}
