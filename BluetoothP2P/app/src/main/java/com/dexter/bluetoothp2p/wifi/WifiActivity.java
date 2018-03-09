package com.dexter.bluetoothp2p.wifi;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.dexter.bluetoothp2p.R;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.sql.ConnectionPoolDataSource;

import static com.dexter.bluetoothp2p.wifi.HotspotActivity.SocketServerPORT;

public class WifiActivity extends AppCompatActivity implements View.OnClickListener {


    private static final int DIALOG_DOWNLOAD_PROGRESS = 1;

    private final String TAG = WifiActivity.class.getName();
    ListView lv;
    Button buttonScan;
    List<ScanResult> results;

    ArrayList<String> wifiList = new ArrayList<>();
    ArrayAdapter adapter;

    WifiManager wifiManager;

    ProgressDialog mProgressDialog = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);
        buttonScan = (Button) findViewById(R.id.scan);
        buttonScan.setOnClickListener(this);
        lv = (ListView) findViewById(R.id.list);

        HotspotConnect connect = new HotspotConnect(this);
        wifiManager = connect.getWifiManager();
        if (wifiManager.isWifiEnabled() == false) {
            wifiManager.setWifiEnabled(true);
        }
        this.adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiList);
        lv.setAdapter(this.adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // Item clicked, Connect network
                ScanResult result = results.get(position);
                String ssid = result.SSID;
                Log.d(TAG, "Clicked on " + ssid + " " + isOpen(result));
                if (ssid.equals("NewSSID") && isOpen(result)) {
                    // hard coded SSID need to change or make something like which remains unique for this app
                    Log.d(TAG, "Connecting to " + ssid);
                    if (connectToSSID(result.SSID)) {
                        String ip = getAccessPointIpAddress(WifiActivity.this);
                        Toast.makeText(WifiActivity.this, "Connected to " + result.SSID + " " + result.BSSID + " " + ip, Toast.LENGTH_LONG).show();
                        // accept it
                        showDialog(DIALOG_DOWNLOAD_PROGRESS);
                        ClientRxThread clientRxThread = new ClientRxThread(ip, SocketServerPORT);
                        clientRxThread.start();
                    }
                }
            }
        });
    }


    public static String getAccessPointIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ;
        DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
        byte[] ipAddress = convert2Bytes(dhcpInfo.serverAddress);
        try {
            String ip = InetAddress.getByAddress(ipAddress).getHostAddress();
            return ip.replace("/", "");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static byte[] convert2Bytes(int hostAddress) {
        byte[] addressBytes = {(byte) (0xff & hostAddress),
                (byte) (0xff & (hostAddress >> 8)),
                (byte) (0xff & (hostAddress >> 16)),
                (byte) (0xff & (hostAddress >> 24))};
        return addressBytes;
    }


    private boolean connectToSSID(String SSID) {
        boolean connected = false;
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        Log.e("SSIDS", wifiInfo.getSSID() + " " + SSID);
        if (wifiInfo.getSSID().equals(SSID) || SSID.equals(wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length() - 1))) {
            // Already Connected
            Toast.makeText(this, "Already connected to SSID " + SSID, Toast.LENGTH_LONG).show();
            connected = true;
        } else {
            Log.d(TAG, "=== connectToSSID Not connected");
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = SSID;   // Please note the quotes. String should contain ssid in quotes
            conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            wifiManager.addNetwork(conf);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration i : list) {
                Log.d(TAG, "===connectToSSID " + i.SSID + " " + SSID);
                if (i.SSID != null && SSID.equals(i.SSID.substring(1, i.SSID.length() - 1))) {
                    wifiManager.disconnect();
                    wifiManager.enableNetwork(i.networkId, true);
                    wifiManager.reconnect();
                    Log.d(TAG, "==connecttoSSID " + "Connecting");
                    Toast.makeText(this, "Connecting to " + SSID, Toast.LENGTH_LONG).show();
                    //will take time to connect add receiver
                    registerReceiver(wifiReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
                    connected = false;
                    return connected;
                }
            }
        }

        return connected;
    }

    private boolean isOpen(ScanResult result) {
        if (result.capabilities.contains("WEP") || result.capabilities.contains("EAP") || result.capabilities.contains("PSK")) {
            return false;
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.scan:
                startScan();
        }
    }

    private void startScan() {
        wifiList.clear();
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

        wifiManager.startScan();
        buttonScan.setText("Scanning .. Please wait");
        buttonScan.setEnabled(false);
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent intent) {
            buttonScan.setEnabled(true);
            buttonScan.setText("Scan");
            Log.d("WifScanner", "onReceive");
            results = wifiManager.getScanResults();
            unregisterReceiver(this);
            for (int i = 0; i < results.size(); i++) {
                wifiList.add(results.get(i).SSID);
            }
            adapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(receiver);
            unregisterReceiver(wifiReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(TAG, "===onStop " + "Receiver not registered");
        }
    }

    private class ClientRxThread extends Thread {
        String dstAddress;
        int dstPort;

        ClientRxThread(String address, int port) {
            Log.e(TAG, "ADD : " + address + " " + port);
            dstAddress = address;
            dstPort = port;
        }

//        @Override
//        public void run() {
//
//            Socket socket = null;
//
//            try {
//                socket = new Socket(dstAddress, dstPort);
//                WifiActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        showDialog(DIALOG_DOWNLOAD_PROGRESS);
//                    }
//                });
//                File file = new File(
//                        Environment.getExternalStorageDirectory(),
//                        "test.txt");
//
//                byte[] bytes = new byte[4096];
//                InputStream is = socket.getInputStream();
//                FileOutputStream fos = new FileOutputStream(file);
//                BufferedOutputStream bos = new BufferedOutputStream(fos);
//                int bytesRead = is.read(bytes, 0, bytes.length);
//                bos.write(bytes, 0, bytesRead);
//                bos.close();
//                socket.close();
//
//                WifiActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(WifiActivity.this,
//                                "Finished",
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//
//            } catch (IOException e) {
//
//                e.printStackTrace();
//
//                final String eMsg = "Something wrong: " + e.getMessage();
//                WifiActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(WifiActivity.this,
//                                eMsg,
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//
//            } finally {
//                if (socket != null) {
//                    try {
//                        socket.close();
//                    } catch (IOException e) {
//                        // TODO Auto-generated catch block
//                        e.printStackTrace();
//                    }
//                }
//            }
//            runOnUiThread(new Runnable() {
//                public void run() {
//                    if (mProgressDialog != null && mProgressDialog.isShowing()) {
//                        mProgressDialog.dismiss();
//                    }
//                }
//            });
//
////            try {
////                Log.e("Data","Reading");
////                String data = downloadDataFromSender("http://"+dstAddress+":"+dstPort);
////                Log.e("Data read", data);
////            } catch (IOException e) {
////                e.printStackTrace();
////            }
//        }

        @Override
        public void run() {
            readData();
//            int file_no;
//            List<File> fileList = new ArrayList<>();
//            Socket socket = null;
//            try {
//                socket = new Socket(dstAddress, dstPort);
//                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
//                int num = dis.readInt();
//                Log.e("Number of forms" , num + " ");
//                ArrayList<String> formsReceived = new ArrayList<>();
//                while (num-- > 0) {
//                    file_no = dis.readInt();
//                    Log.e("FILE ", file_no+"");
//                    fileList.clear();
//                    while(file_no-- > 0) {
//                        String filename = dis.readUTF();
//                        long fileSize = dis.readLong();
//                        File newFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + filename);
//                        newFile.createNewFile();
//                        FileOutputStream fos = new FileOutputStream(newFile);
//                        int n;
//                        byte buf[] = new byte[4096];
//                        while (fileSize > 0 && (n = dis.read(buf, 0, (int) Math.min(buf.length, fileSize))) != -1) {
//                            fos.write(buf, 0, n);
//                            fileSize -= n;
//                        }
//                        fos.close();
//                        fileList.add(newFile);
//                        Log.d("File created", filename);
//                    }
//                }
//                WifiActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(WifiActivity.this,
//                                "Finished",
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//                runOnUiThread(new Runnable() {
//                    public void run() {
//                        if (mProgressDialog != null && mProgressDialog.isShowing()) {
//                            mProgressDialog.dismiss();
//                        }
//                    }
//                });
//
//
//            } catch (IOException e) {
//                dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
//                e.printStackTrace();
//
//                final String eMsg = "Something wrong: " + e.getMessage();
//                WifiActivity.this.runOnUiThread(new Runnable() {
//
//                    @Override
//                    public void run() {
//                        Toast.makeText(WifiActivity.this,
//                                eMsg,
//                                Toast.LENGTH_LONG).show();
//                    }
//                });
//                // Connection Interrupted
//                // Make sure to remove each resource for forms which are received incomplete
//                for(File file : fileList){
//                    Log.d(TAG,"==Delete " + file.getName() + " " + file.delete());
//                }
//            }finally {
//                try {
//                    socket.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
        }

        private void readData() {
            Socket socket = null;
            int file_no;
            List<File> fileList = new ArrayList<>();
            String dialogMessage = null;
            ArrayList<String> filesDownloaded = new ArrayList<>();
            try {
                socket = new Socket(dstAddress, dstPort);
                DataInputStream dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                int num = dis.readInt();
                Log.e("Number of forms" , num + " ");
                while (num-- > 0) {
                    file_no = dis.readInt();
                    Log.e("FILE ", file_no+"");
                    fileList.clear();
                    boolean checkFormName = false;
                    while(file_no-- > 0) {
                        String filename = dis.readUTF();
                        Log.e("File",filename);
                        if(!checkFormName) {
                            checkFormName = true;
                            filesDownloaded.add(filename);
                            Log.e("LIST", filesDownloaded + " " + filesDownloaded.size());
                        }
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
                        fileList.add(newFile);
                        Log.d("File created", filename);
                    }
                }
                WifiActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(WifiActivity.this,
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
                e.printStackTrace();

                final String eMsg = "Something wrong: " + e.getMessage();
                WifiActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(WifiActivity.this,
                                eMsg,
                                Toast.LENGTH_LONG).show();
                    }
                });
                // Connection Interrupted
                // Make sure to remove each resource for forms which are received incomplete
                for(File file : fileList){
                    if(dialogMessage == null) {
                        dialogMessage = "Files Deleted : " + file.getName() + "\n";
                    }else {
                        dialogMessage += file.getName();
                    }
                    Log.d(TAG,"==Delete " + file.getName() + " " + file.delete());
                }
            }finally {
                dismissDialog(DIALOG_DOWNLOAD_PROGRESS);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            boolean messageDownload = false;
            for(String name : filesDownloaded) {
                if(dialogMessage == null) {
                    messageDownload = true;
                    dialogMessage = "Files Downloaded : ";
                }else if(!messageDownload) {
                    messageDownload = true;
                    dialogMessage = "\nFiles Downloaded : ";
                }
                dialogMessage += name + " ";
                Log.e("Message " , dialogMessage);

            }
            final String finalDialogMessage = dialogMessage;
            WifiActivity.this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    showAlertDialog(WifiActivity.this, finalDialogMessage);
                }
            });
        }

    }

    public BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        boolean isConnected = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivity != null) {
                NetworkInfo[] info = connectivity.getAllNetworkInfo();
                if (info != null) {
                    for (int i = 0; i < info.length; i++) {
                        if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                            if (!isConnected) {
                                Log.d("WifiReceiver", "Have Wifi Connection");
                                isConnected = true;
                                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                                if (wifiInfo.getSSID().equals("NewSSID") || ("NewSSID").equals(wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length() - 1))) {
                                    Log.d("Connected to .. ", wifiInfo.getSSID());
                                    String ip = getAccessPointIpAddress(context);
                                    ClientRxThread clientRxThread = new ClientRxThread(ip, SocketServerPORT);
                                    clientRxThread.start();
                                }
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_DOWNLOAD_PROGRESS:
                mProgressDialog = new ProgressDialog(WifiActivity.this);
                mProgressDialog.setMessage("Receiving");
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.show();
                return mProgressDialog;
            default:
                return null;
        }
    }

    private void showAlertDialog(Context context, String message){
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setMessage(message);
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Ok",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert11 = builder1.create();
        alert11.show();
    }

}