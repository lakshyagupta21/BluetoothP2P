package com.dexter.bluetoothp2p.wifi;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Created by laksh on 3/8/2018.
 */

public class HotspotController {
    private final String TAG = this.getClass().getName();

    private WifiConfiguration existingConfig;
    private HotspotConnect hotspotConnect;
    Context context;
    Method getWifiApConfiguration, setWifiApEnabled, setWifiApConfiguration, isWifiApEnabled, getWifiApState;

    HotspotController(Context context) {
        this.context = context;
        hotspotConnect = new HotspotConnect(context);
        for (Method method : WifiManager.class.getDeclaredMethods()) {
            switch (method.getName()) {
                case "getWifiApConfiguration":
                    getWifiApConfiguration = method;
                    break;
                case "getWifiApState":
                    getWifiApState = method;
                    break;
                case "isWifiApEnabled":
                    isWifiApEnabled = method;
                    break;
                case "setWifiApEnabled":
                    setWifiApEnabled = method;
                    break;
                case "setWifiApConfiguration":
                    setWifiApConfiguration = method;
                    break;
            }
        }
    }



    private WifiConfiguration createNewWifi(String ssid){
        //Create new Open Wifi Configuration
        WifiConfiguration wifiConf = new WifiConfiguration();
        wifiConf.SSID = ssid;
        wifiConf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        hotspotConnect.getWifiManager().addNetwork(wifiConf);
        hotspotConnect.getWifiManager().saveConfiguration();
        return wifiConf;
    }

    public Object invokeMethods(Method method, Object manager, Object ...args) {
        try {
            return method.invoke(manager,args);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean disableNet() {
        // check for previous config
        Object obj;
        if (existingConfig != null) {
            invokeMethods(setWifiApConfiguration,hotspotConnect.getWifiManager(),existingConfig);
        }
        return setHotspotEnabled(existingConfig,false);
    }

    public boolean enableHotspot() {
        hotspotConnect.getWifiManager().setWifiEnabled(false);
        Object obj = null;
        obj = invokeMethods(getWifiApConfiguration,hotspotConnect.getWifiManager());

        if(obj != null) {
            existingConfig = (WifiConfiguration)obj;
            Log.d(TAG, " === Config details " + existingConfig.toString() );
        }

        String ssid = "NewSSID" ;
        WifiConfiguration newConfig = createNewWifi(ssid);
        Log.d(TAG, "===enableHotspot " + " New added " + newConfig.toString());
        // enable hotspot

        return setHotspotEnabled(newConfig,true);
        // existing config stored so that if user doesn't have to configure hostpot for personal use
    }
    public boolean setHotspotEnabled(WifiConfiguration config, boolean enabled) {
        Object result = invokeMethods(setWifiApEnabled, hotspotConnect.getWifiManager(), config, enabled);
        if (result == null) {
            return false;
        }
        return (Boolean) result;
    }
    public WifiConfiguration getConfig() {
        Object obj = null;
        obj = invokeMethods(getWifiApConfiguration,hotspotConnect.getWifiManager());
        if(obj != null) {
            return (WifiConfiguration) obj;
        }
        return null;
    }
}
