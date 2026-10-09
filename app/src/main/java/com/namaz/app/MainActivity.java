package com.namaz.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import org.json.JSONObject;
import org.json.JSONArray;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String[] NAMES = {"ফজর", "সূর্যোদয়", "যোহর", "আসর", "মাগরিব", "এশা"};
    private LinearLayout root, list;
    private TextView status;
    private double lat = 23.433, lon = 91.813; // Hathazari-area fallback; GPS overrides when allowed
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("namaz", MODE_PRIVATE);
        makeChannel();
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        loadLocationAndTimes();
    }

    private void buildUi() {
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(24, 28, 24, 24); root.setBackgroundColor(0xFFF4F8F5);
        TextView title = new TextView(this); title.setText("☾  নামাজ"); title.setTextSize(30); title.setTextColor(0xFF064E3B); title.setGravity(Gravity.CENTER); title.setPadding(0,8,0,8); root.addView(title);
        TextView subtitle = new TextView(this); subtitle.setText("পাঁচ ওয়াক্ত নামাজের সময়সূচি"); subtitle.setTextSize(16); subtitle.setTextColor(0xFF3C6254); subtitle.setGravity(Gravity.CENTER); root.addView(subtitle);
        status = new TextView(this); status.setText("সময়সূচি লোড হচ্ছে…"); status.setTextSize(14); status.setPadding(0,20,0,12); status.setGravity(Gravity.CENTER); root.addView(status);
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); root.addView(list, new LinearLayout.LayoutParams(-1,0,1));
        Button refresh = new Button(this); refresh.setText("সময়সূচি আপডেট করুন"); refresh.setOnClickListener(v -> loadLocationAndTimes()); root.addView(refresh);
        TextView note = new TextView(this); note.setText("আজানের নোটিফিকেশন পেতে নোটিফিকেশন ও অ্যালার্মের অনুমতি দিন। ফোনের ব্যাটারি সেটিংস কিছু অ্যালার্ম আটকে দিতে পারে।"); note.setTextSize(12); note.setTextColor(0xFF64748B); note.setPadding(4,12,4,0); root.addView(note);
        setContentView(root);
    }

    private void loadLocationAndTimes() {
        try {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationManager lm = (LocationManager)getSystemService(LOCATION_SERVICE);
                Location loc = null;
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (loc == null && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) loc = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (loc != null) { lat = loc.getLatitude(); lon = loc.getLongitude(); }
            }
        } catch (Exception ignored) {}
        status.setText("আপনার এলাকার নামাজের সময় আনা হচ্ছে…");
        final double qlat = lat, qlon = lon;
        new Thread(() -> {
            try {
                String day = new SimpleDateFormat("dd-MM-yyyy", Locale.US).format(new Date());
                URL url = new URL("https://api.aladhan.com/v1/timings/" + day + "?latitude=" + qlat + "&longitude=" + qlon + "&method=1&school=1");
                HttpURLConnection c = (HttpURLConnection)url.openConnection(); c.setConnectTimeout(12000); c.setReadTimeout(12000);
                JSONObject json = new JSONObject(new String(c.getInputStream().readAllBytes(), "UTF-8"));
                JSONObject timings = json.getJSONObject("data").getJSONObject("timings");
                String[] keys = {"Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha"};
                String[] times = new String[keys.length]; for (int i=0;i<keys.length;i++) times[i] = timings.getString(keys[i]).split(" ")[0];
                runOnUiThread(() -> showTimes(times, day));
                schedulePrayerAlarms(times);
            } catch (Exception e) { runOnUiThread(() -> status.setText("সময়সূচি আনতে ইন্টারনেট চালু করুন, তারপর আবার চেষ্টা করুন।")); }
        }).start();
    }

    private void showTimes(String[] times, String day) {
        status.setText("আজকের সময়সূচি  •  " + new SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(new Date()) + "\nঅবস্থান: " + String.format(Locale.US,"%.3f, %.3f",lat,lon));
        list.removeAllViews();
        for (int i=0;i<times.length;i++) {
            LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(18,14,18,14); row.setBackgroundColor(i%2==0?0xFFFFFFFF:0xFFE8F3EC);
            TextView n = new TextView(this); n.setText(NAMES[i]); n.setTextSize(18); n.setTextColor(0xFF064E3B); row.addView(n,new LinearLayout.LayoutParams(0,-2,1));
            TextView t = new TextView(this); t.setText(toBanglaDigits(times[i])); t.setTextSize(20); t.setTextColor(0xFF064E3B); row.addView(t); list.addView(row);
        }
    }
    private String toBanglaDigits(String s) { char[] b={'০','১','২','৩','৪','৫','৬','৭','৮','৯'}; StringBuilder o=new StringBuilder(); for(char ch:s.toCharArray()) o.append(ch>='0'&&ch<='9'?b[ch-'0']:ch); return o.toString(); }

    private void schedulePrayerAlarms(String[] times) {
        String[] names={"ফজরের নামাজের সময় হয়েছে", "সূর্যোদয়", "যোহরের নামাজের সময় হয়েছে", "আসরের নামাজের সময় হয়েছে", "মাগরিবের নামাজের সময় হয়েছে", "এশার নামাজের সময় হয়েছে"};
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE); Calendar now=Calendar.getInstance();
        for(int i=0;i<times.length;i++) {
            if(i==1) continue; // Sunrise is displayed but is not a prayer reminder
            try {
                String[] hm=times[i].split(":"); Calendar when=Calendar.getInstance(); when.set(Calendar.HOUR_OF_DAY,Integer.parseInt(hm[0])); when.set(Calendar.MINUTE,Integer.parseInt(hm[1])); when.set(Calendar.SECOND,0); when.set(Calendar.MILLISECOND,0);
                if(!when.after(now)) when.add(Calendar.DATE,1);
                Intent intent=new Intent(this,PrayerAlarmReceiver.class); intent.putExtra("message",names[i]); intent.putExtra("prayerIndex",i);
                PendingIntent pi=PendingIntent.getBroadcast(this,100+i,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
                if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi); }
                else if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi); else am.setExact(AlarmManager.RTC_WAKEUP,when.getTimeInMillis(),pi);
            } catch(Exception ignored) {}
        }
    }
    private void makeChannel() { if(Build.VERSION.SDK_INT>=26) { NotificationChannel ch=new NotificationChannel("namaz_adhan","নামাজের সময়",NotificationManager.IMPORTANCE_HIGH); ch.setDescription("নামাজের সময় হলে সতর্কবার্তা"); ch.enableVibration(true); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch); } }
}
