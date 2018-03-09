package com.dexter.bluetoothp2p.testing;

import android.database.Cursor;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.dexter.bluetoothp2p.InstanceProviderAPI;
import com.dexter.bluetoothp2p.InstancesDao;
import com.dexter.bluetoothp2p.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Activity2 extends AppCompatActivity {

    EditText editTextAddress;
    Button buttonConnect;
    TextView textPort;

    static final int SocketServerPORT = 8080;
    private static final String TAG = "Activity2";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_2);

        editTextAddress = (EditText) findViewById(R.id.address);
        textPort = (TextView) findViewById(R.id.port);
        textPort.setText("port: " + SocketServerPORT);
        buttonConnect = (Button) findViewById(R.id.connect);

        buttonConnect.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                ClientRxThread clientRxThread =
                        new ClientRxThread(
                                editTextAddress.getText().toString(),
                                SocketServerPORT);

                clientRxThread.start();
            }});
    }

    private class ClientRxThread extends Thread {
        String dstAddress;
        int dstPort;
        Socket socket;

        ClientRxThread(String address, int port) {
            dstAddress = address;
            dstPort = port;
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
            Cursor c = null;
            try {
                c = new InstancesDao().getInstancesCursor(selection, selectionArgs);

                if (c != null && c.getCount() > 0) {
                    OutputStream os = socket.getOutputStream();
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.writeInt(c.getCount());
                    c.moveToPosition(-1);
                    while (c.moveToNext()) {

                        //publishProgress(c.getPosition() + 1, ids.length);
                        String instance = c.getString(
                                c.getColumnIndex(InstanceProviderAPI.InstanceColumns.INSTANCE_FILE_PATH));
                        String id = c.getString(c.getColumnIndex(InstanceProviderAPI.InstanceColumns._ID));
                        if(!sendInstance(instance)){
                            return false;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
                if (c != null) {
                    c.close();
                }
                return false;
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            return true;
        }

        private String getFileExtension(String fileName) {
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex == -1) {
                return "";
            }
            return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
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
        boolean uploadOneFile(List<File> files) {
            byte[] bytes = new byte[4096];
            try {
                int read = 0;
                OutputStream os = socket.getOutputStream();
                DataOutputStream dos = new DataOutputStream(os);
                dos.writeInt(files.size());

                for(int i = 0; i < files.size(); i++){
                    File file = files.get(i);
                    //publishProgress(i+1, files.size());
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
                    DataInputStream dis = new DataInputStream(bis);
                    dos.writeUTF(file.getName());
                    dos.writeLong(file.length());
                    while( ( read = dis.read( bytes) ) > 0 ){
                        Log.d("READ" , read + "");
                        dos.write(bytes, 0, read);
                    }
                    final String sentMsg = "File sent to: " + socket;
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

        @Override
        public void run() {
            socket = null;

            try {
                socket = new Socket(dstAddress, dstPort);

                File file = new File(
                        Environment.getExternalStorageDirectory(),
                        "test.txt");

                byte[] bytes = new byte[1024];
                InputStream is = socket.getInputStream();
                FileOutputStream fos = new FileOutputStream(file);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                int bytesRead = is.read(bytes, 0, bytes.length);
                bos.write(bytes, 0, bytesRead);
                bos.close();
                socket.close();

                Activity2.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Activity2.this,
                                "Finished",
                                Toast.LENGTH_LONG).show();
                    }});

            } catch (IOException e) {

                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                Activity2.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(Activity2.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            } finally {
                if(socket != null){
                    try {
                        socket.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
