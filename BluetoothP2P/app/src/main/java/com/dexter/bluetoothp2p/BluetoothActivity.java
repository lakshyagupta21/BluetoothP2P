package com.dexter.bluetoothp2p;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Set;

public class BluetoothActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int DIALOG_DOWNLOAD_PROGRESS = 1;

    ListView detectedDevices;
    Button buttonSearch, buttonOn, listenStart, discover;
    ArrayAdapter<String> detectedAdapter;
    BluetoothDevice bdDevice;
    BluetoothAdapter bluetoothAdapter = null;
    ArrayList<BluetoothDevice> arrayListBluetoothDevices = null;
    boolean isBluetoothOn;
    private static final String TAG = "BluetoothActivity";

    ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        isBluetoothOn = false;
        detectedDevices = (ListView) findViewById(R.id.detectedList);
        buttonSearch = (Button) findViewById(R.id.search);
        buttonOn = (Button) findViewById(R.id.bluetoothToggle);
        listenStart = (Button) findViewById(R.id.listen);
        discover = (Button) findViewById(R.id.discover);

        buttonOn.setOnClickListener(this);
        buttonSearch.setOnClickListener(this);
        listenStart.setOnClickListener(this);
        discover.setOnClickListener(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        arrayListBluetoothDevices = new ArrayList<BluetoothDevice>();
        detectedAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
        detectedDevices.setAdapter(detectedAdapter);
        detectedAdapter.notifyDataSetChanged();
        if (!bluetoothAdapter.isEnabled()) {
            buttonOn.setText("On");
        } else {
            isBluetoothOn = false;
            buttonOn.setText("Off");
        }

        detectedDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bdDevice = arrayListBluetoothDevices.get(i);
                Set<BluetoothDevice> pairedDevice = bluetoothAdapter.getBondedDevices();
                if (pairedDevice.size() > 0) {
                    for (BluetoothDevice device : pairedDevice) {
                        if (device.equals(bdDevice)) {
                            Log.d(TAG, "Already Paired");
                            BluetoothDevice bluetoothDevice = bdDevice;
                            break;
                        }
                    }
                }
            }
        });

    }

    private void onBluetooth() {
        if (!bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.enable();
        }
    }

    private void offBluetooth() {
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
    }

    private void makeDiscoverable() {
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);
        Log.i(TAG, "Discoverable Started");
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
                    isBluetoothOn = false;
                    offBluetooth();
                    buttonOn.setText("On");
                } else {
                    isBluetoothOn = true;
                    onBluetooth();
                    buttonOn.setText("Off");
                }
                break;
            case R.id.search:
                if(bluetoothAdapter.isEnabled()) {
                    arrayListBluetoothDevices.clear();
                    detectedAdapter.notifyDataSetChanged();
                    startSearch();
                } else {
                    Toast.makeText(this,"Turn on the Bluetooth",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.listen:
                if(bluetoothAdapter.isEnabled()) {
                    // bluetooth is already on
                    ListeningThread t = new ListeningThread();
                    t.start();
                } else {
                    // bluetooth is off, ask to start
                    Toast.makeText(BluetoothActivity.this,"Turn on the Bluetooth",Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.discover:
                makeDiscoverable();

            default:
                    break;
        }
    }

    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                boolean flag = true;    // flag to indicate that particular device is already in the arlist or not
                for (int i = 0; i < arrayListBluetoothDevices.size(); i++) {
                    if (device.getAddress().equals(arrayListBluetoothDevices.get(i).getAddress())) {
                        flag = false;
                    }
                }
                if (flag == true) {
                    detectedAdapter.add(device.getName() + "\n" + device.getAddress());
                    arrayListBluetoothDevices.add(device);
                    detectedAdapter.notifyDataSetChanged();
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {

            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {

            }
        }
    };

    private class ListeningThread extends Thread {
        private final BluetoothServerSocket bluetoothServerSocket;

        public ListeningThread() {
            BluetoothServerSocket temp = null;
            try {
                temp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(getString(R.string.app_name), Constants.uuid);

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

    public class ClientRxThread extends Thread {

        BluetoothSocket socket;

        ClientRxThread(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                int num = dis.readInt();
                Log.e("Number of forms" , num + " ");
                while (num-- > 0) {
                    int file_no = dis.readInt();
                    Log.e("FILE ", file_no+"");
                    while(file_no-- > 0) {
                        String filename = dis.readUTF();
                        long fileSize = dis.readLong();
                        File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
                        newFile.createNewFile();
                        FileOutputStream fos = new FileOutputStream(newFile);
                        int n;
                        byte buf[] = new byte[4096];
                        while (fileSize > 0 && (n = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
                            fos.write(buf, 0, n);
                            fileSize -= n;
                        }
                        fos.close();
                        Log.d("File created", filename);
                    }
                }
                BluetoothActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(BluetoothActivity.this,
                                "Finished",
                                Toast.LENGTH_LONG).show();
                    }
                });
                runOnUiThread(new Runnable() {
                    public void run() {
                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
                            mProgressDialog.dismiss();
                        }
                    }
                });


            } catch (IOException e) {
                dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                BluetoothActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(BluetoothActivity.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
            }finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
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

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(myReceiver);
        }catch(Exception e){

        }
    }
}
