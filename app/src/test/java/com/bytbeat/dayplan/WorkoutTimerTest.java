package com.bytbeat.dayplan;

import android.app.Application;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Looper;
import android.os.Vibrator;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowToneGenerator;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=Application.class)
public class WorkoutTimerTest {
    private void verifySignals(boolean foreground){
        Context c=RuntimeEnvironment.getApplication();
        WorkoutTimer.foreground=foreground;
        c.getSharedPreferences("workout_rest",0).edit().putString("token","test-rest").putLong("end",System.currentTimeMillis()-1).commit();
        WorkoutTimer receiver=new WorkoutTimer();
        receiver.onReceive(c,new Intent());
        receiver.onReceive(c,new Intent()); // A repeated alarm must not repeat the signal sequence.
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(1600));
        assertEquals(3,ShadowToneGenerator.getPlayedTones().size());
        assertArrayEquals(new long[]{0,200,350,200,350,200},Shadows.shadowOf((Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE)).getPattern());
        assertEquals(-1,Shadows.shadowOf((Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE)).getRepeat());
        if(!foreground){
            var channel=c.getSystemService(NotificationManager.class).getNotificationChannel("workout_rest_v2");
            assertNotNull(channel);assertNull(channel.getSound());assertFalse(channel.shouldVibrate());
        }
        WorkoutTimer.foreground=false;
    }
    @Test public void threeSoundsAndThreeVibrationsInForeground(){verifySignals(true);}
    @Test public void backgroundNotificationDoesNotAddAFourthSignal(){verifySignals(false);}
    @Test public void anEarlyAlarmDoesNotSignal(){
        Context c=RuntimeEnvironment.getApplication();
        c.getSharedPreferences("workout_rest",0).edit().putString("token","test-rest").putLong("end",System.currentTimeMillis()+60000).commit();
        new WorkoutTimer().onReceive(c,new Intent());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(2));
        assertTrue(ShadowToneGenerator.getPlayedTones().isEmpty());
    }
}
