package hc.manager.datapp.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import hc.manager.datapp.activity.LoginActivity;

public class ScreenOnOffBroadCastReciever extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            //Take count of the screen off position
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            Intent serviceIntent = new Intent(context, LoginActivity.class);
            context.startActivity(serviceIntent);
        }
    }
}