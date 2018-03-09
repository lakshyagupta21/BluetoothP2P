package com.dexter.bluetoothp2p.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.lang.reflect.Method;

/**
 * Created by laksh on 3/8/2018.
 */

public class HotspotConnect {

    WifiManager wm;

    HotspotConnect(Context context) {
        wm = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
    }

    public WifiManager getWifiManager() {
        return wm;
    }

    public static boolean isHotspotOn(Context context) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            Method method = wifimanager.getClass().getDeclaredMethod("isWifiApEnabled");
            method.setAccessible(true);
            return (Boolean) method.invoke(wifimanager);
        } catch (Throwable ignored) {
        }
        return false;
    }

    public boolean toggleHotspot(Context context, WifiConfiguration wifiConfiguration) {
        WifiManager wifimanager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        try {
            if (isHotspotOn(context)) {
                wifimanager.setWifiEnabled(false);
            }
            Method method = wifimanager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method.invoke(wifimanager, wifiConfiguration, !isHotspotOn(context));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}