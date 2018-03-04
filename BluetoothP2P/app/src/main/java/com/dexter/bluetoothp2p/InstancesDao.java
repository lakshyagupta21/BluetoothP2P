package com.dexter.bluetoothp2p;

import android.database.Cursor;

/**
 * Created by laksh on 3/4/2018.
 */

public class InstancesDao {
    public Cursor getInstancesCursor(String selection, String[] selectionArgs) {
        return getInstancesCursor(null, selection, selectionArgs, null);
    }

    public Cursor getInstancesCursor(String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return Supervisor.getSingleton().getContentResolver()
                .query(InstanceProviderAPI.InstanceColumns.CONTENT_URI, projection, selection, selectionArgs, sortOrder);
    }
}
