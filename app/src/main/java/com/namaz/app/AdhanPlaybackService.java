package com.namaz.app;

import android.app.*;
import android.content.*;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.IBinder;

public class AdhanPlaybackService extends Service {
 private MediaPlayer player;
 @Override public int onStartCommand(Intent intent, int flags, int startId) {
   String msg = intent == null ? "নামাজের সময় হয়েছে" : intent.getStringExtra("message");
   Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this,"namaz_adhan") : new Notification.Builder(this);
   b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("নামাজ — আজান").setContentText(msg == null ? "নামাজের সময় হয়েছে" : msg).setOngoing(true);
   startForeground(401, b.build());
   try {
     if (player != null) { player.release(); player = null; }
     player = new MediaPlayer();
     android.content.res.AssetFileDescriptor afd = getAssets().openFd("adhan.ogg");
     player.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
     afd.close();
     player.setOnCompletionListener(mp -> stopSelf());
     player.setOnErrorListener((mp, what, extra) -> { stopSelf(); return true; });
     player.prepare(); player.start();
   } catch (Exception e) { stopSelf(); }
   return START_NOT_STICKY;
 }
 @Override public void onDestroy() { if(player!=null){ try{player.stop();}catch(Exception ignored){} player.release(); player=null; } super.onDestroy(); }
 @Override public IBinder onBind(Intent intent) { return null; }
}
