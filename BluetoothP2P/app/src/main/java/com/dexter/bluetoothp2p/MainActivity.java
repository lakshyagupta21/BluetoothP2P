package com.dexter.bluetoothp2p;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

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
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    ListView detectedDevices;
    Button buttonSearch,buttonOn;
    ArrayAdapter<String> detectedAdapter;
    BluetoothDevice bdDevice;
    BluetoothAdapter bluetoothAdapter = null;
    ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    boolean isBluetoothOn;
    private static final String TAG = "MainActivity";
    private final static UUID uuid = UUID.fromString("2cc9ec17-8fd3-4e10-a28a-4be8383a9737");
    private static final int DIALOG_DOWNLOAD_PROGRESS = 1;
    ProgressDialog mProgressDialog = null;
    String path ;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        detectedDevices = (ListView) findViewById(R.id.detectedList);
        buttonSearch = (Button) findViewById(R.id.search);
        buttonOn = (Button) findViewById(R.id.bluetoothToggle);

        buttonOn.setOnClickListener(this);
        buttonSearch.setOnClickListener(this);


        detectedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bdDevice = arrayListBluetoothDevices.get(i);
                Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
                if(pairedDevice.size()>0)
                {
                    for(BluetoothDevice device : pairedDevice)
                    {
                        if(device.equals(bdDevice)) {
                            Log.d(TAG,"Already Paired");
                            BluetoothDevice bluetoothDevice = bdDevice;
                            // Initiate a connection request in a separate thread
//                            ConnectingThread t = new ConnectingThread(bluetoothDevice);
//                            t.start();
                            break;
                        }
                    }
                }
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        arrayListBluetoothDevices = new ArrayList<BluetoothDevice>();
        detectedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
        detectedDevices.setAdapter(detectedAdapter);
        detectedAdapter.notifyDataSetChanged();
        if (!bluetoothAdapter.isEnabled()) {
            buttonOn.setText("Bluetooth On");
        } else {
            buttonOn.setText("Bluetooth Off");
        }
        ListeningThread t = new ListeningThread();
        t.start();
    }

    private void onBluetooth() {
        if(!bluetoothAdapter.isEnabled()){
            bluetoothAdapter.enable();
        }
    }
    private void offBluetooth() {
        if(bluetoothAdapter.isEnabled()){
            bluetoothAdapter.disable();
        }
    }

    private void startSearch() {
        Log.d(TAG, "Search Started");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(myReceiver, intentFilter);
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.bluetoothToggle:
                if (isBluetoothOn) {
                    offBluetooth();
                    buttonOn.setText("Bluetooth On");
                }
                else {
                    onBluetooth();
                    buttonOn.setText("Bluetooth Off");
                }
                break;
            case R.id.search:
                arrayListBluetoothDevices.clear();
                detectedAdapter.notifyDataSetChanged();
                startSearch();
                break;
        }
    }
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
                for(int i = 0; i<arrayListBluetoothDevices.size();i++)
                {
                    if(device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress()))
                    {
                        flag = false;
                    }
                }
                if(flag == true)
                {
                    detectedAdapter.add(device.getName()+"\n"+device.getAddress());
                    arrayListBluetoothDevices.add(device);
                    detectedAdapter.notifyDataSetChanged();
                }
            }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){

            }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

            }
        }
    };
    private class ListeningThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), uuid);

            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothServerSocket = temp;
        }

        public void run() {
            BluetoothSocket bluetoothSocket;
            // This will block while listening until a BluetoothSocket is returned
            // or an exception occurs
            while (true) {
                try {
                    bluetoothSocket = bluetoothServerSocket.accept();
                } catch (IOException e) {
                    break;
                }
                // If a connection is accepted
                if (bluetoothSocket != null) {

                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast.makeText(getApplicationContext(), "A connection has been accepted.",
                                    Toast.LENGTH_SHORT).show();
                            showDialog(DIALOG_DOWNLOAD_PROGRESS);

                        }
                    });
                    ClientRxThread clientRxThread = new ClientRxThread(bluetoothSocket);
                    clientRxThread.start();

                    break;
                }
            }
        }

        // Cancel the listening socket and terminate the thread
        public void cancel() {
            try {
                bluetoothServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    //    private class ConnectingThread extends Thread {
//        private final BluetoothSocket bluetoothSocket;
//        private final BluetoothDevice bluetoothDevice;
//
//        public ConnectingThread(BluetoothDevice device) {
//
//            BluetoothSocket temp = null;
//            bluetoothDevice = device;
//
//            // Get a BluetoothSocket to connect with the given BluetoothDevice
//            try {
//                temp = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            bluetoothSocket = temp;
//        }
//
//        public void run() {
//            // Cancel any discovery as it will slow down the connection
//            bluetoothAdapter.cancelDiscovery();
//
//            try {
//                // This will block until it succeeds in connecting to the device
//                // through the bluetoothSocket or throws an exception
//                bluetoothSocket.connect();
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        Toast.makeText(getApplicationContext(), "Connection Established",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//                FileTxThread fileTxThread = new FileTxThread(bluetoothSocket);
//                fileTxThread.start();
//            } catch (IOException connectException) {
//                connectException.printStackTrace();
//                try {
//                    bluetoothSocket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//
//            // Code to manage the connection in a separate thread
//        /*
//            manageBluetoothConnection(bluetoothSocket);
//        */
//        }
//
//        // Cancel an open connection and terminate the thread
//        public void cancel() {
//            try {
//                bluetoothSocket.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//    public class FileTxThread extends Thread {
//        BluetoothSocket socket;
//
//        FileTxThread(BluetoothSocket socket){
//            this.socket= socket;
//        }
//
//        @Override
//        public void run() {
//            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+ "/test.txt");
////            if(file.exists()){
////                Log.d(TAG,"File exists");
////            }else{
////                Log.e(TAG,"PAth : " + path);
////            }
////            Log.d(TAG,"file : " + file.getAbsolutePath() +" " + "server" + " " + file.length());
//            byte[] bytes = new byte[(int) file.length()];
//            BufferedInputStream bis;
//            try {
//                bis = new BufferedInputStream(new FileInputStream(file));
//                Log.d(TAG,"" + bis.read(bytes, 0, bytes.length));
//                OutputStream os = socket.getOutputStream();
//                os.write(bytes, 0, bytes.length);
//                os.flush();
//
//                final String sentMsg = "File sent to: " + socket.getRemoteDevice().getAddress() +  " " + socket.getRemoteDevice().getName();
//                MainActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(MainActivity.this,
//                                sentMsg,
//                                Toast.LENGTH_LONG).show();
//                    }});
//
//
//            } catch (FileNotFoundException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } catch (IOException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//
//        }
//    }
    public class ClientRxThread extends Thread {

        BluetoothSocket socket;
        ClientRxThread(BluetoothSocket socket){
            this.socket= socket;
        }

        @Override
        public void run() {
            try {
//                FileOutputStream fos = new FileOutputStream(file);
//                BufferedOutputStream bos = new BufferedOutputStream(fos);
//                InputStream is = socket.getInputStream();
//                String end = "12345";
//                StringBuilder curMsg = new StringBuilder();
//                while(true) {
//                    int bytesRead = is.read(bytes, 0, bytes.length);
//                    if(bytesRead == -1)
//                        break;
//                    Log.d("IS", is.available() + "");
//                    Log.d("HELLO","available");
//                    Log.d(TAG, "Bytes : " + bytesRead);
//                    Log.d(TAG, curMsg.toString());
//                    bos.write(bytes, 0, bytesRead);
//                    curMsg.append(new String(bytes, 0, bytesRead, Charset.forName("UTF-8")));
////                    int endIdx = curMsg.indexOf(end);
////                    if (endIdx != -1) {
////                        String fullMessage = curMsg.substring(0, endIdx + end.length());
////                        //curMsg.delete(0, endIdx + end.length());
////                        break;
////                        // Now send fullMessage
////                    }
//                }
//                Log.e(TAG,"Message"  + curMsg.toString());
//                bos.close();
//                is.close();
//                fos.close();
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                int num = dis.readInt();
                while(num-- > 0) {
                    String filename = dis.readUTF();
                    long fileSize = dis.readLong();
                    File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/"+filename);
                    newFile.createNewFile();
                    FileOutputStream fos = new FileOutputStream(newFile);
                    int n;
                    byte buf[] = new byte[4096];
                    while (fileSize > 0 && (n = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                        fos.write(buf, 0, n);
                        fileSize -= n;
                    }
                    fos.close();
                    Log.d("File created",filename);
                }
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                "Finished",
                                Toast.LENGTH_LONG).show();
                    }});
                runOnUiThread(new Runnable() {
                    public void run() {
                        if(mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
                });


            } catch (IOException e) {
                dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                MainActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }});

            }
        }
    }
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS:
                mProgressDialog = new ProgressDialog(this);
                mProgressDialog.setMessage("Receiving");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }
}