package com.bytbeat.dayplan;

import android.app.*;
import android.content.*;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.*;

/** One rest deadline, independent of WebView timers and Activity lifecycle. */
public class WorkoutTimer extends BroadcastReceiver {
    private static final String PREF="workout_rest",CHANNEL="workout_rest_v1";
    private static final int ID=700;
    static volatile boolean foreground;
    private static PendingIntent alarm(Context c){
        return PendingIntent.getBroadcast(c,ID,new Intent(c,WorkoutTimer.class).setAction("com.bytbeat.dayplan.REST"),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    public static synchronized void schedule(Context c,String token,long end,String title){
        AlarmManager am=c.getSystemService(AlarmManager.class);
        am.cancel(alarm(c));
        c.getSystemService(NotificationManager.class).cancel(ID);
        android.content.SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        p.edit().putString("token",token).putLong("end",end).putString("title",title).commit();
        if(token.isEmpty()||end<=System.currentTimeMillis())return;
        long when=SystemClock.elapsedRealtime()+end-System.currentTimeMillis();
        if(ReminderScheduler.exactAllowed(c)){
            try{am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,alarm(c));return;}catch(SecurityException ignored){}
        }
        am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,when,alarm(c));
    }
    @Override public void onReceive(Context c,Intent intent){
        android.content.SharedPreferences p=c.getSharedPreferences(PREF,Context.MODE_PRIVATE);
        if(p.getString("token","").isEmpty()||p.getLong("end",0)>System.currentTimeMillis()+500)return;
        p.edit().putString("token","").apply();
        if(foreground){
            try{final ToneGenerator tone=new ToneGenerator(AudioManager.STREAM_NOTIFICATION,80);tone.startTone(ToneGenerator.TONE_PROP_BEEP2,600);new Handler(Looper.getMainLooper()).postDelayed(tone::release,1000);}catch(Exception ignored){}
            try{Vibrator v=(Vibrator)c.getSystemService(Context.VIBRATOR_SERVICE);if(v!=null)v.vibrate(VibrationEffect.createWaveform(new long[]{0,200,100,200},-1));}catch(Exception ignored){}
            return;
        }
        NotificationManager nm=c.getSystemService(NotificationManager.class);
        NotificationChannel ch=new NotificationChannel(CHANNEL,"Отдых между подходами",NotificationManager.IMPORTANCE_HIGH);ch.enableVibration(true);nm.createNotificationChannel(ch);
        if(!ReminderScheduler.notificationsAllowed(c))return;
        Intent open=new Intent(c,PlannerActivity.class).putExtra("openWorkout",true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap=PendingIntent.getActivity(c,ID,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        nm.notify(ID,new Notification.Builder(c,CHANNEL).setSmallIcon(com.bytbeat.dayplan.R.drawable.ic_notification).setContentTitle("Отдых окончен").setContentText(p.getString("title","")+" — можно начать следующий подход").setContentIntent(tap).setAutoCancel(true).setCategory(Notification.CATEGORY_ALARM).setVisibility(Notification.VISIBILITY_PRIVATE).build());
    }
}
