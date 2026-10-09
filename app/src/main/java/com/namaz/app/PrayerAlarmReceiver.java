package com.namaz.app;

import android.app.*;
import android.content.*;
import android.os.Build;
import android.media.RingtoneManager;
import android.net.Uri;

public class PrayerAlarmReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context, Intent intent) {
   String message=intent.getStringExtra("message"); if(message==null) message="নামাজের সময় হয়েছে";
   NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
   Notification.Builder b = Build.VERSION.SDK_INT>=26 ? new Notification.Builder(context,"namaz_adhan") : new Notification.Builder(context);
   Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM); if(sound==null) sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
   b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("নামাজ").setContentText(message).setAutoCancel(true).setVibrate(new long[]{0,500,250,500});
   // If a licensed adhan.ogg is bundled in app/src/main/assets, play the full adhan.
   // Otherwise, fall back to the phone's normal alarm sound.
   boolean hasAdhan=false; try { context.getAssets().open("adhan.ogg").close(); hasAdhan=true; } catch(Exception ignored) {}
   if (!hasAdhan) b.setSound(sound);
   nm.notify((int)(System.currentTimeMillis()%100000),b.build());
   if (hasAdhan) {
     Intent play=new Intent(context,AdhanPlaybackService.class); play.putExtra("message",message);
     try { if(Build.VERSION.SDK_INT>=26) context.startForegroundService(play); else context.startService(play); } catch(Exception ignored) {}
   }
 }
}
