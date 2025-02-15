package com.cns.encom;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.hardware.camera2.CameraManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.SurfaceHolder;

import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import net.majorkernelpanic.streaming.gl.SurfaceView;
import net.majorkernelpanic.streaming.SessionBuilder;
import net.majorkernelpanic.streaming.rtsp.RtspServer;

import java.net.InetAddress;
import java.io.*;
import java.net.*;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback{

    boolean switchCam = false; //switch3
//    boolean switchMic = false; //switch2
    WifiManager wifiMgr;
    CameraManager camMgr;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private connectionThread mConnThread = null;
    boolean connectionUp = false;
    boolean rtspServerStarted = false;
    String[] ips = {"0","0","0","0"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        Context context = getApplicationContext();
        wifiMgr = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
        camMgr = (CameraManager) context.getSystemService(context.CAMERA_SERVICE);

        TextView term = findViewById(R.id.textView);
        term.setMovementMethod(new ScrollingMovementMethod());

        mSurfaceView = findViewById(R.id.surfaceView);
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(this);

        // ***************************Display device IP***************************
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(ip);
        InetAddress inetAddressRestored;
        String strip=null;
        try{
            inetAddressRestored = InetAddress.getByAddress(buffer.array());
            strip = inetAddressRestored.getHostAddress();
        }catch (UnknownHostException e){
            strip = "error";
        }
        try {
            //Only works on ipv4!
            ips = strip.split("\\.");
        }catch(Exception e){
            Log.d("Start up", "onCreate: "+e.getMessage());
        }
        String tip = ips[3]+"."+ips[2]+"."+ips[1]+"."+ips[0];
        TextView myip = findViewById(R.id.Indicator_myip);
        myip.setText("Device IP: "+tip);

        TextView logsWindows = findViewById(R.id.textView);
        try {
            logsWindows.setText("Online cameras: "+Arrays.toString(camMgr.getCameraIdList())+"\n");
        }catch (Exception e){
            logsWindows.append(e.getMessage());
        }

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 1);
        }
    }


    public void clear_onclick(View view){
        Toast.makeText(view.getContext(), "Console cleared", Toast.LENGTH_SHORT).show();
        TextView logsWindows = findViewById(R.id.textView);
        logsWindows.setText("");
    }

    public void scanLocal(View view){
        WifiInfo wifiInfo = wifiMgr.getConnectionInfo();
        int ip = wifiInfo.getIpAddress();
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putInt(ip);
        InetAddress inetAddressRestored;
        String strip=null;
        try{
            inetAddressRestored = InetAddress.getByAddress(buffer.array());
            strip = inetAddressRestored.getHostAddress();
        }catch (UnknownHostException e){
            strip = "error";
        }
        TextView logsWindows = findViewById(R.id.textView);
        Log.d("ScanNet", strip);

        String[] ips={"0","0","0","0"};
        try {
            //Only works on ipv4!
            ips = strip.split("\\.");
        }catch(Exception e){
            Toast.makeText(view.getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
            int sub = Integer.parseInt(ips[3]);
            Log.d("Sub", String.valueOf(sub));
            logsWindows.append("\nTry scanning on subnet\n");

            new Thread(new scannerRunner(ips, logsWindows)).start();
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    public class scannerRunner implements Runnable
    {
        String[] ips;
        TextView view;
        public scannerRunner(String[] ips, TextView view)
        {
            this.ips = ips;
            this.view = view;
        }

        public scannerRunner(String tarip, int i, View view) {

        }

        @Override
        public void run() {

            ExecutorService pool = Executors.newFixedThreadPool(10);
            for (int i = 0; i <= 255; i+=10){


                for(int j = 0; j < 10; j++)
                {
                    final String target = ips[3]+"."+ips[2]+"."+ips[1]+"."+String.valueOf(i+j);
                    final int ij = i + j;
                    pool.submit(new Runnable() {
                        @Override
                        public void run() {
                            boolean alive=false;
                            try {
                                final InetAddress tarmachine = InetAddress.getByName(target);
                                alive=tarmachine.isReachable(400);

                            }catch(Exception e) {
                                Log.d("ScanNet", "run: " + ij + " " + e.getMessage());
                                alive=false;
                            }
                            final String targetCopy = target;
                            final boolean aliveCopy = alive;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (aliveCopy) view.append(targetCopy + " Alive \n");
                                }
                            });
                        }
                    });
                }


            }
        }
    }

    public void connect_onclick(View view){
        EditText editText=findViewById(R.id.ip_form);
        String tarip=editText.getText().toString();
        TextView logsWindows = findViewById(R.id.textView);
        logsWindows.append("Target machine IP: "+tarip+"\n");
        TextView indicator_host = findViewById(R.id.Indicator_hostip);
        indicator_host.setText("Host: "+tarip);
        switchCam = true;
        if(mConnThread != null && mConnThread.isAlive())
            logsWindows.append("You have already connected\n");
        else{
            mConnThread = new connectionThread(tarip, this);
            mConnThread.start();
        }
    }

    public void disconnect_onclick(View view) {
        TextView logsWindows = findViewById(R.id.textView);

        try{
            if (mConnThread != null) {
                try {
                    mConnThread.join();
                } catch (InterruptedException e) {
                    mConnThread.interrupt();
                }
            }
            logsWindows.append("You have disconnected\n");
            TextView indicator_host = findViewById(R.id.Indicator_hostip);
            indicator_host.setText("Host: 0.0.0.0");
            mConnThread = null;
            switchCam = false;
            connectionUp = false;
        } catch(Exception err){
            Log.d("Disconnect", "disconnect_onclick: "+err.getMessage());
            logsWindows.append(err.getMessage()+"\n");
        }

    }

    public class connectionThread extends Thread{
        String ipaddress;
        MainActivity mainActivity;
        Intent service;

        public connectionThread(String ipaddress, MainActivity mainActivity){
            this.ipaddress = ipaddress;
            this.mainActivity = mainActivity;
            service = new Intent(mainActivity, RtspServer.class);
        }

        @Override
        public void run(){
            Socket socket;
            try {
                socket = new Socket(ipaddress, 65500);
            }catch(IOException e){return;}

            Log.d("connectionThread", "run: socket connected");

            SessionBuilder.getInstance()
                    .setSurfaceView(mSurfaceView)
                    .setPreviewOrientation(90)
                    .setContext(getApplicationContext())
                    .setAudioEncoder(SessionBuilder.AUDIO_NONE)
                    .setVideoEncoder(SessionBuilder.VIDEO_H264);

            int newPort = getRandomNumber(1000, 2000);
            // Sets the port of the RTSP server to random number
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mainActivity).edit();
            editor.putString(RtspServer.KEY_PORT, String.valueOf(newPort));
            editor.apply();

            if(rtspServerStarted)
                mainActivity.stopService(service);
            connectionUp = true;

            mainActivity.startService(service);
            rtspServerStarted = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView logsWindows = findViewById(R.id.textView);
                    logsWindows.append("RTSP Server started\n");
                    CheckBox cameraCheckbox = findViewById(R.id.checkBox);
                    cameraCheckbox.setChecked(true);
                }
            });

            DataOutputStream out;
            try{
                out = new DataOutputStream(socket.getOutputStream());
                for(int i = 3; i >= 0; i--)
                    out.writeInt(Integer.parseInt(ips[i]));
                out.writeInt(newPort);
            }
            catch(IOException e){return;}

            Log.d("connectionThread", "newPort = " + newPort + " sent");

            while(connectionUp){
                try{out.writeChar(1);}
                catch(IOException e){break;}
                try{Thread.sleep(50);}
                catch(InterruptedException e){break;}
            }

            mainActivity.stopService(service);
            rtspServerStarted = false;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView logsWindows = findViewById(R.id.textView);
                    logsWindows.append("RTSP Server stopped\n");
                }
            });

            try{
                socket.shutdownOutput();
                socket.close();
            }
            catch(IOException e){}

            while(isRTSPServiceAlive()){
                try{Thread.sleep(50);}
                catch(InterruptedException e){break;}
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mSurfaceHolder.setFormat(PixelFormat.TRANSPARENT);
                    mSurfaceHolder.setFormat(PixelFormat.OPAQUE);
                    CheckBox cameraCheckbox = findViewById(R.id.checkBox);
                    cameraCheckbox.setChecked(false);
                }
            });
        }

        int getRandomNumber(int min, int max){
            return (int)((Math.random() * (max - min)) + min);
        }
        boolean isRTSPServiceAlive(){
            ActivityManager manager = (ActivityManager)getSystemService(ACTIVITY_SERVICE);
            for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
                if("RtspServer".equals(service.service.getClassName()))
                    return true;
            }
            return false;
        }
    }
}


