package me.aaron.androidprocessalive;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

public class GrayService extends Service {

    private static final int GRAY_SERVICE_ID = -11111;

    public static void actionStart(Context context) {
        context.startService(new Intent(context, GrayService.class));
    }

    public GrayService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            InnerService.actionStart(this);
            startForeground(GRAY_SERVICE_ID, new Notification());
        } else {
            startForeground(GRAY_SERVICE_ID, new Notification());
        }

        return super.onStartCommand(intent, flags, startId);
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

}
