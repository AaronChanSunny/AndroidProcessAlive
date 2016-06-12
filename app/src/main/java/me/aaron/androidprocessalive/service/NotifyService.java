package me.aaron.androidprocessalive.service;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

public class NotifyService extends Service {

    private static final String TAG = NotifyService.class.getSimpleName();
    private static final int NOTIFY_SERVICE_ID = 4567;

    private boolean mIsRunning;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, NotifyService.class));
    }

    public NotifyService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");

        mIsRunning = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            InnerService.actionStart(this);
            startForeground(NOTIFY_SERVICE_ID, new Notification());
        } else {
            startForeground(NOTIFY_SERVICE_ID, new Notification());
        }

        if (!mIsRunning) {
            mIsRunning = true;

            doSomething();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void doSomething() {
        Log.d(TAG, "doSomething...");

        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    SystemClock.sleep(3000);
                    Log.d(TAG, "doing...");
                }
            }
        }).start();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
    }

    public static class InnerService extends Service {

        public static void actionStart(Context context) {
            context.startService(new Intent(context, InnerService.class));
        }

        public InnerService() {
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            startForeground(NOTIFY_SERVICE_ID, new Notification());
            stopSelf();
            return super.onStartCommand(intent, flags, startId);
        }

        @Override
        public IBinder onBind(Intent intent) {
            // TODO: Return the communication channel to the service.
            throw new UnsupportedOperationException("Not yet implemented");
        }
    }

}
