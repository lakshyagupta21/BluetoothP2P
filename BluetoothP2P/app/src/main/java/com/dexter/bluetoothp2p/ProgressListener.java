package com.dexter.bluetoothp2p;

/**
 * Created by laksh on 3/4/2018.
 */

public interface ProgressListener {
    void uploadingComplete(String result);
    void progressUpdate(int progress, int total);
    void onCancel();
}
