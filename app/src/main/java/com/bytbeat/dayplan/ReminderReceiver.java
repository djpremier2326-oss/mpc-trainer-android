package com.bytbeat.dayplan;
import android.content.*;
public class ReminderReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i){if("com.bytbeat.dayplan.REMIND".equals(i.getAction()))ReminderScheduler.fire(c,i.getStringExtra("id"),i.getLongExtra("event",System.currentTimeMillis()));else ReminderScheduler.restore(c);}
}
