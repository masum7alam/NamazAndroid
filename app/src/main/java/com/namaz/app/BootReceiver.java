package com.namaz.app;
import android.content.*;
public class BootReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context context, Intent intent) {
   if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
     Intent launch=new Intent(context,MainActivity.class); launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); context.startActivity(launch);
   }
 }
}
