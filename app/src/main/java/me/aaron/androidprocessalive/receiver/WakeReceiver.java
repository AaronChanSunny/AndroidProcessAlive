package me.aaron.androidprocessalive.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import me.aaron.androidprocessalive.service.NotifyService;

public class WakeReceiver extends BroadcastReceiver {

    private static final String TAG = WakeReceiver.class.getSimpleName();
    public static final String GRAY_WAKE_ACTION = "me.aaron.androidprocessalive.receiver.WakeReceiver.GRAY_WAKE_ACTION";

    public WakeReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive");
        String action = intent.getAction();
        if (GRAY_WAKE_ACTION.equals(action)) {
            context.startService(new Intent(context, NotifyService.class));
        }
    }
}
