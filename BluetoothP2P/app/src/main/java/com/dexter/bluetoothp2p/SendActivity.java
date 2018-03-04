package com.dexter.bluetoothp2p;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
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
import java.util.Set;

public class SendActivity extends AppCompatActivity implements ProgressListener, View.OnClickListener {

    private static final int PROGRESS_DIALOG = 1;
    private static final String TAG = "SendActivity";
    private ProgressDialog progressDialog;
    private InstanceSendTask task;

    private String alertMsg;

    ListView detectedDevices;
    Button buttonSearch,buttonOn;
    ArrayAdapter<String> detectedAdapter;
    BluetoothDevice bdDevice;
    BluetoothAdapter bluetoothAdapter = null;
    ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    boolean isBluetoothOn;
    private Long[] instancesToSend;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send);
        long ids[] = getIntent().getLongArrayExtra("data");
        Log.e("SIZE", ids.length+"");
        instancesToSend = new Long[ids.length];
        for(int i = 0; i < ids.length; i++){
            Log.e("ITEM", ids[i]+"");
            instancesToSend[i] = ids[i];
        }

        alertMsg = "Please Wait";

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
                            showDialog(PROGRESS_DIALOG);
                            ConnectingThread t = new ConnectingThread(bluetoothDevice);
                            t.start();
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

    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(myReceiver);
    }

    @Override
    public void uploadingComplete(String result) {
        try {
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }
    }

    @Override
    public void progressUpdate(int progress, int total) {
        alertMsg = "Sending form " + String.valueOf(progress) + " of " + String.valueOf(total);
        progressDialog.setMessage(alertMsg);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(this);
                progressDialog.setTitle("Sending ");
                progressDialog.setMessage(alertMsg);
                progressDialog.setIndeterminate(true);
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.setCancelable(false);
                //progressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return progressDialog;
        }

        return null;
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
    private void startSearch() {
        Log.d(TAG, "Search Started");
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);

        registerReceiver(myReceiver, intentFilter);
        bluetoothAdapter.startDiscovery();
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

    private class ConnectingThread extends Thread {
        private BluetoothSocket bluetoothSocket;
        private final BluetoothDevice bluetoothDevice;

        public ConnectingThread(BluetoothDevice device) {

            BluetoothSocket temp = null;
            bluetoothDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                temp = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.uuid);
            } catch (IOException e) {
                e.printStackTrace();
            }
            bluetoothSocket = temp;
        }

        public void run() {
            // Cancel any discovery as it will slow down the connection
            bluetoothAdapter.cancelDiscovery();

            try {
                // This will block until it succeeds in connecting to the device
                // through the bluetoothSocket or throws an exception
                try {
                    bluetoothSocket.connect();
                }catch(IOException e) {
                    bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(Constants.uuid);
                    bluetoothSocket.connect();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Connection Established",
                                Toast.LENGTH_SHORT).show();
                    }
                });
                if (task == null) {
                    // setup dialog and upload task
                    task = new InstanceSendTask(bluetoothSocket);

                    // register this activity with the new uploader task
                    task.setUploaderListener(SendActivity.this);
                    task.execute(instancesToSend);
                }
            } catch (IOException connectException) {
                connectException.printStackTrace();

                try {
                    bluetoothSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dismissDialog(PROGRESS_DIALOG);
            }

            // Code to manage the connection in a separate thread
        /*
            manageBluetoothConnection(bluetoothSocket);
        */
        }

        // Cancel an open connection and terminate the thread
        public void cancel() {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
