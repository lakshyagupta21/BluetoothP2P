package com.dexter.bluetoothp2p.wifi;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.dexter.bluetoothp2p.MainActivity;
import com.dexter.bluetoothp2p.ProgressListener;
import com.dexter.bluetoothp2p.R;
import com.dexter.bluetoothp2p.SendActivity;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class HotspotActivity extends AppCompatActivity  implements ProgressListener  {


    private final String TAG = this.getClass().getName();
    HotspotController controller;
    boolean settingsCanWrite;
    static final int SocketServerPORT = 8080;
    ServerSocket serverSocket;
    Socket socket;
    private static final int PROGRESS_DIALOG = 1;
    private ProgressDialog progressDialog;
    private String alertMsg;


    ServerSocketThread serverSocketThread;
    Button startSend;


    private Long[] instancesToSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hotspot);

        long ids[] = getIntent().getLongArrayExtra("data");
        instancesToSend = new Long[ids.length];
        for (int i = 0; i < ids.length; i++) {
            instancesToSend[i] = ids[i];
        }

        startSend = (Button) findViewById(R.id.startSend);
        startSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(HotspotActivity.this, " Started Listening for connections",
                        Toast.LENGTH_LONG).show();
                serverSocketThread = new ServerSocketThread();
                serverSocketThread.start();
//                try {
//                    serverSocket = new ServerSocket(SocketServerPORT);
//                    while (true) {
//
//                        socket = serverSocket.accept();
//                        HotspotActivity.this.runOnUiThread(new Runnable() {
//
//                            @Override
//                            public void run() {
//                                Toast.makeText(HotspotActivity.this,
//                                        "Connection Accepted",
//                                        Toast.LENGTH_LONG).show();
//                            }});
//                        // start task
//                        WifiSendTask sendTask = new WifiSendTask(socket);
//                        sendTask.execute(instancesToSend);
//                    }
//                } catch (IOException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } finally {
//                    if (socket != null) {
//                        try {
//                            socket.close();
//                        } catch (IOException e) {
//                            // TODO Auto-generated catch block
//                            e.printStackTrace();
//                        }
//                    }
//                }

            }
        });
        Log.e(TAG, String.valueOf(HotspotConnect.isHotspotOn(this)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        boolean settingsCanWrite = false;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            settingsCanWrite = Settings.System.canWrite(this);
            if (!settingsCanWrite) {
                // If do not have write settings permission then open the Can modify system settings panel.
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);
                finish();
            } else {
                controller = new HotspotController(this);
                Log.d(TAG, "=== onResume " + controller.enableHotspot());
            }
        } else {

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (controller != null) {
            Log.d(TAG, "===onStop " + controller.disableNet());
        }
    }

    public class ServerSocketThread extends Thread {
        @Override
        public void run() {
            Socket socket = null;

            try {
                serverSocket = new ServerSocket(SocketServerPORT);
                HotspotActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Log.e(TAG, "Port : " + serverSocket.getLocalPort());
                    }
                });
                Log.e("PORT ", serverSocket.getLocalPort() + " " + serverSocket.getLocalSocketAddress() + " " + serverSocket.getInetAddress());
                while (true) {

                    socket = serverSocket.accept();
                    HotspotActivity.this.runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            Toast.makeText(HotspotActivity.this,
                                    "Connection Accepted",
                                    Toast.LENGTH_LONG).show();
                            showDialog(PROGRESS_DIALOG);
                        }
                    });
//                    FileTxThread fileTxThread = new FileTxThread(socket);
//                    fileTxThread.start();
                    WifiSendTask sendTask = new WifiSendTask(socket);
                    sendTask.setUploaderListener(HotspotActivity.this);
                    sendTask.execute(instancesToSend);
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                if (socket != null) {
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

    public class FileTxThread extends Thread {
        Socket socket;

        FileTxThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            File file = new File(
                    Environment.getExternalStorageDirectory(),
                    "test.txt");

            byte[] bytes = new byte[(int) file.length()];
            BufferedInputStream bis;
            try {
                bis = new BufferedInputStream(new FileInputStream(file));
                bis.read(bytes, 0, bytes.length);
                OutputStream os = socket.getOutputStream();
                os.write(bytes, 0, bytes.length);
                os.flush();
                socket.close();

                final String sentMsg = "File sent to: " + socket.getInetAddress();
                HotspotActivity.this.runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(HotspotActivity.this,
                                sentMsg,
                                Toast.LENGTH_LONG).show();
                        dismissDialog(PROGRESS_DIALOG);
                    }
                });

            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

        }
    }

    @Override
    public void uploadingComplete(String result) {
        try {
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }
        Toast.makeText(this, "Files sent", Toast.LENGTH_LONG).show();
    }

    @Override
    public void progressUpdate(int progress, int total) {
        alertMsg = "Sending form " + String.valueOf(progress) + " of " + String.valueOf(total);
        progressDialog.setMessage(alertMsg);
    }

    @Override
    public void onCancel() {
        try {
            dismissDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }
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
                return progressDialog;
        }

        return null;
    }

}
