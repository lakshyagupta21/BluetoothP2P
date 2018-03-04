package com.dexter.bluetoothp2p;

import android.bluetooth.BluetoothSocket;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by laksh on 3/4/2018.
 */

public class InstanceSendTask extends AsyncTask<Long, Integer, String>{
    ProgressListener stateListener;
    private static final String TAG = "InstanceSendTask";

    BluetoothSocket socket;
    public InstanceSendTask(BluetoothSocket socket){
        this.socket = socket;
    }

    public void setUploaderListener(ProgressListener sl) {
        synchronized (this) {
            stateListener = sl;
        }
    }
    @Override
    protected String doInBackground(Long... longs) {
        if(processSelectedFiles(longs)) {
            return "Successfully sent " + longs.length +" forms";
        }
        return "Sending Failed !";
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        synchronized (this) {
            if (stateListener != null) {
                stateListener.progressUpdate(values[0], values[1]);
            }
        }
    }

    @Override
    protected void onPostExecute(String s) {
        Log.e("onPostExecute",s);
        stateListener.uploadingComplete(s);
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean processSelectedFiles(Long ids[]){
        StringBuilder selectionBuf = new StringBuilder(InstanceProviderAPI.InstanceColumns._ID + " IN (");
        String[] selectionArgs = new String[ids.length];
        for (int i = 0; i < ids.length; i++) {
            if (i > 0) {
                selectionBuf.append(",");
            }
            selectionBuf.append("?");
            selectionArgs[i] = ids[i].toString();
        }

        selectionBuf.append(")");
        String selection = selectionBuf.toString();
        Log.e("SELECTION " , selection);
        Cursor c = null;
        try {
            c = new InstancesDao().getInstancesCursor(selection, selectionArgs);

            if (c != null && c.getCount() > 0) {
                Log.e("Cursor",c.getCount()+"");
                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeInt(c.getCount());
                c.moveToPosition(-1);
                while (c.moveToNext()) {

                    publishProgress(c.getPosition() + 1, ids.length);
                    String instance = c.getString(
                            c.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
                    String id = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
                    if(!sendInstance(instance)){
                        Log.e("INSTANCE" , instance);
                       return false;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (c != null) {
                c.close();
            }
        }
        return true;
    }
    private boolean sendInstance(String instanceFilePath) {
        File instanceFile = new File(instanceFilePath);
        File[] allFiles = instanceFile.getParentFile().listFiles();

        // add media files
        List<File> files = new ArrayList<File>();
        files.add(instanceFile);
        if (allFiles != null) {
            for (File f : allFiles) {
                String fileName = f.getName();

                if (fileName.startsWith(".")) {
                    continue; // ignore invisible files
                } else if (fileName.equals(instanceFile.getName())) {
                    continue; // the xml file has already been added
                } else if (fileName.equals(instanceFile.getName())) {
                    continue; // the xml file has already been added
                }

                String extension = getFileExtension(fileName);

                if (extension.equals("jpg")) { // legacy 0.9x
                    files.add(f);
                } else if (extension.equals("3gpp")) { // legacy 0.9x
                    files.add(f);
                } else if (extension.equals("3gp")) { // legacy 0.9x
                    files.add(f);
                } else if (extension.equals("mp4")) { // legacy 0.9x
                    files.add(f);
                } else if (extension.equals("osm")) { // legacy 0.9x
                    files.add(f);
                } else {
                    Log.d("unrecognized file type ", f.getName());
                }
            }
        }

        if(!uploadOneFile(files)) {
            return false;
        }
        return true;
    }
    boolean uploadOneFile(List<File> files){
        Log.e("Files",files.size()+"");
//        Log.d(TAG,"file : " + file.getAbsolutePath() +" " + "server" + " " + file.length());
        byte[] bytes = new byte[4096];
        try {
            int read = 0;
            OutputStream os = socket.getOutputStream();
            DataOutputStream dos = new DataOutputStream(os);
            dos.writeInt(files.size());

            for(int i = 0; i < files.size(); i++){
                File file = files.get(i);
                publishProgress(i+1, files.size());
                BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                DataInputStream dis = new DataInputStream(bis);
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                while( ( read = dis.read( bytes) ) > 0 ){
                    Log.d("READ" , read + "");
                    dos.write(bytes, 0, read);
                }
                final String sentMsg = "File sent to: " + socket.getRemoteDevice().getAddress() +  " " + socket.getRemoteDevice().getName();
                Log.d(TAG,"Sent message " + sentMsg);
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }
    private static String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex == -1) {
            return "";
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }
}
