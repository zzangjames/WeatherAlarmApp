package com.example.weatheralarmapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.example.weatheralarmapp.alarm.AlarmWakeUpActivity;
import com.example.weatheralarmapp.db_connect.DBConst;
import com.example.weatheralarmapp.db_connect.DBHelper;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

public class AlarmReceiver extends BroadcastReceiver {

    DBHelper dbHelper;
    String memo_arr;
    String day_arr;


    @Override
    public void onReceive(Context context, Intent intent) {
        // throw new UnsupportedOperationException("not yet implemented");

        DBHelper dbHelper = new DBHelper(context.getApplicationContext(), DBConst.MEMO_TABLE_NAME, null, DBConst.DATABASE_VERSION);
        Cursor cursor = dbHelper.readMemoContact(dbHelper.getReadableDatabase());

        Date currentTime = Calendar.getInstance().getTime();
        SimpleDateFormat weekdayFormat = new SimpleDateFormat("EE", Locale.getDefault());

        String weekDay = weekdayFormat.format(currentTime);

        weekDay = dayToForm(weekDay);

        try {
            for(int i = 0; i < cursor.getCount(); i++){
                cursor.moveToNext();
                String day = cursor.getString(1);
                if (weekDay.equals(day)) {
                    String memo_contents = cursor.getString(2);
                    day_arr = day;
                    memo_arr = memo_contents;
                    break;
                }
            }
        }
        catch (Exception e) { }

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent notificationIntent = new Intent(context, AlarmAddActivity.class);

        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pendingI = PendingIntent.getActivity(context, 0,
                notificationIntent, 0);

//        Intent wakeupIntent = new Intent(context, AlarmWakeUpActivity.class);
//
//        PendingIntent pendingWakeUp = PendingIntent.getActivity(context, 1,
//                wakeupIntent, 0);

//        if(day_arr[0] != null){}
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "default");

        //OREO API 26 이상에서는 채널 필요
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {

            builder.setSmallIcon(R.drawable.ic_launcher_foreground); //mipmap 사용시 Oreo 이상에서 시스템 UI 에러남


            String channelName ="매일 알람 채널";
            String description = "매일 정해진 시간에 알람합니다.";
            int importance = NotificationManager.IMPORTANCE_HIGH; //소리와 알림메시지를 같이 보여줌

            NotificationChannel channel = new NotificationChannel("default", channelName, importance);
            channel.setDescription(description);

            if (notificationManager != null) {
                // 노티피케이션 채널을 시스템에 등록
                notificationManager.createNotificationChannel(channel);
            }
        }else builder.setSmallIcon(R.mipmap.ic_launcher); // Oreo 이하에서 mipmap 사용하지 않으면 Couldn't create icon: StatusBarIcon 에러남


        builder.setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())

                .setTicker("{Time to watch some cool stuff!}")
                .setContentTitle(day_arr + ", 오늘의 메모 알람")
                .setContentText(memo_arr)
                .setContentInfo("INFO")
                .setContentIntent(pendingI);

        if (notificationManager != null) {

            //화면 끈 상태에서 켜지기

            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK  |
                    PowerManager.ACQUIRE_CAUSES_WAKEUP |
                    PowerManager.ON_AFTER_RELEASE, "My:Tag");
            wakeLock.acquire(5000);


            // 노티피케이션 동작시킴
            notificationManager.notify(1234, builder.build());


//            startSound(context);
//            추가된 부분 AlarmSoundService
            Intent intent2 = new Intent(context.getApplicationContext(), AlarmSoundService.class);
            context.startService(intent2);

            Calendar nextNotifyTime = Calendar.getInstance();

            Log.d("nextNotifiyTime", nextNotifyTime + "");

            //지워야지 -> 여기서 반복을??
            // 내일 같은 시간으로 알람시간 결정
//            nextNotifyTime.add(Calendar.DATE, 1);

            //지워야지
            //  Preference에 설정한 값 저장
            SharedPreferences.Editor editor = context.getSharedPreferences("daily alarm", MODE_PRIVATE).edit();
            editor.putLong("nextNotifyTime", nextNotifyTime.getTimeInMillis());
            editor.apply();

            Date currentDateTime = nextNotifyTime.getTime();
            String date_text = new SimpleDateFormat("yyyy년 MM월 dd일 EE요일 a hh시 mm분 ", Locale.getDefault()).format(currentDateTime);
            Toast.makeText(context.getApplicationContext(),"다음 알람은 " + date_text + "으로 알람이 설정되었습니다!", Toast.LENGTH_SHORT).show();
            Log.d("currentDateTime", currentDateTime + "");
            Log.d("date_text", date_text);
        }
    }

    public String dayToForm(String weekDay) {

        switch (weekDay) {
            case "Mon":
                weekDay = "월요일";
                break;

            case "Tue":
                weekDay = "화요일";
                break;

            case "Wed":
                weekDay = "수요일";
                break;

            case "Thu":
                weekDay = "목요일";
                break;

            case "Fri":
                weekDay = "금요일";
                break;

            case "Sat":
                weekDay = "토요일";
                break;

            case "Sun":
                weekDay = "일요일";
                break;
        }
        return weekDay;
    }

//    public void startSound(Context context) {
//        //Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//        //Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
//        Uri notification = Uri.parse("android.resource://com.example.alarmexample/raw/gradius");
//        android.media.Ringtone r = RingtoneManager.getRingtone(context, notification);
//        r.play();
//    }

}
