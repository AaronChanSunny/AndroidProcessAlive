package me.aaron.androidprocessalive.service;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import me.aaron.androidprocessalive.receiver.WakeReceiver;

public class GrayService extends Service {

    private static final String TAG = GrayService.class.getSimpleName();
    private static final int GRAY_SERVICE_ID = -11111;
    private static final int WAKE_REQUEST_CODE = 1123123;
    private static final long INTERVAL = 5 * 1000;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, GrayService.class));
    }

    public GrayService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            InnerService.actionStart(this);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            startForeground(GRAY_SERVICE_ID, new Notification());
        }

        setupAlarm();

        return super.onStartCommand(intent, flags, startId);
    }

    private void setupAlarm() {
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent alarmIntent = new Intent(WakeReceiver.GRAY_WAKE_ACTION);
        PendingIntent operation = PendingIntent.getBroadcast(
                this,
                WAKE_REQUEST_CODE,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        am.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                INTERVAL,
                operation);
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public static class InnerService extends Service {

        public static void actionStart(Context context) {
            context.startService(new Intent(context, InnerService.class));
        }

        public InnerService() {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(GRAY_SERVICE_ID, new Notification());
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            // TODO: Return the communication channel to the service.
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }
}
