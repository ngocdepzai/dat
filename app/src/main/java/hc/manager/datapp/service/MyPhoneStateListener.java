package hc.manager.datapp.service;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;

import hc.manager.datapp.utils.CheckPhoneStatus;

public class MyPhoneStateListener extends PhoneStateListener {
    private Context context;
    private Integer signalNum = -1; //信号强度

    public MyPhoneStateListener(Context context) {
        this.context = context;
    }

    @Override
    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
        super.onSignalStrengthsChanged(signalStrength);

        try {
            if (CheckPhoneStatus.getDeviceInfo(context).net_type.equals(CheckPhoneStatus.NETWORK_CLASS_4_G)) {
                String ssignal = signalStrength.toString();
                String[] parts = ssignal.split(" ");
                signalNum = Integer.parseInt(parts[11]);
            } else {
                signalNum = (signalStrength.getGsmSignalStrength() * 2) - 113;
            }
        } catch (Exception e) {
            signalNum = -1;
        }
    }


    public Integer getSignalNum() {
        return signalNum;
    }
}