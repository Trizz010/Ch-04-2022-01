// Code Challenge: Use the following code as baseline to
// create an Android application that in a single activity has a button
// to download a large file (for instance, 1GB). Save that file in the
// local storage. Add the necessary functionality to survey the environment
// via BroadcastReceivers to let the app go ahead with the download only if the
// following conditions are satisfied:
//    1) The device should be at least 95% of battery level OR
//    2) the device is plugged to USB/AC power sources AND
//    3) the device is online via WiFi (not mobile network)

package mx.tec.ch_04_2022_01;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView  myTxtView;
    private URL url;
    private String filename;
    private BroadcastReceiver battery;
    private BroadcastReceiver wifi;
    private boolean isPlugged, hasBattery, hasWifi, isDownloading;
    private Button download;
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        download = findViewById(R.id.downloadButton);

        isPlugged = false;
        hasBattery = false;
        hasWifi = false;

        PowerManager pm = (PowerManager)  getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyApp::MyTag");

        battery = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int plug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
                if(plug == BatteryManager.BATTERY_PLUGGED_AC || plug == BatteryManager.BATTERY_PLUGGED_USB){
                    isPlugged = true;
                } else {
                    isPlugged = false;
                }
                int levelBattery = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
                if(levelBattery > 95){
                    hasBattery = true;
                } else {
                    hasBattery = false;
                }
                // If it's not plugged and battery level is below 95 while downloading
                if((!isPlugged && !hasBattery) && isDownloading){
                    Toast.makeText(context, "ALKDSJGOAWEIJGAIOWEJG", Toast.LENGTH_SHORT).show();
                    // Show the dialog
                    AlertDialog.Builder conf = new AlertDialog.Builder(MainActivity.this);
                    conf.setTitle("Not enough battery level");
                    conf.setMessage("Your phone is not plugged and the battery level is below 95%. Do you want to keep downloading?");
                    conf.setPositiveButton("Yes",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                    conf.setPositiveButton("No, cancel the download",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                }
                            });
                }
            }
        };
        wifi = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
                try {
                    NetworkInfo net = connectivityManager.getActiveNetworkInfo();
                    if(net.isConnectedOrConnecting()){
                        if(net.getType() == ConnectivityManager.TYPE_WIFI){
                            hasWifi = true;
                        } else {
                            hasWifi = false;
                            // If it's not connected with wifi while downloading
                            if(isDownloading) {
                                AlertDialog.Builder conf = new AlertDialog.Builder(MainActivity.this);
                                conf.setTitle("WiFi connection lost");
                                conf.setMessage("You're not connected to a WiFi network anymore. Do you want to keep downloading?");
                                conf.setPositiveButton("Yes",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }
                                        });
                                conf.setPositiveButton("No, cancel the download",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                            }
                                        });
                            }
                        }
                    }
                } catch (Exception e){
                    Toast.makeText(context, "Device is not online", Toast.LENGTH_SHORT).show();

                }
            }
        };

        this.registerReceiver(this.battery, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        this.registerReceiver(this.wifi, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if((hasBattery || isPlugged) && hasWifi){
                    wakeLock.acquire();
                    DownloadFile();
                    isDownloading = true;
                } else {
                    Toast.makeText(MainActivity.this, "The file couldn't be downloaded", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //Local storage management requires runtime permission granting upon installing the app
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);

    }

    private void DownloadFile(){
        // This code uses DownloadManager.Request to prepare a URL request to obtain a file from the internet
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse("https://sabnzbd.org/tests/internetspeed/50MB.bin"));
        request.setDescription("Downloading file " + "testfile.bin");
        request.setTitle("Downloading");
        request.allowScanningByMediaScanner();
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "testfile.bin");

        //Once with the request prepared and configured, we obtain the proper system service and start the download
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);

        // We register a Broadcast Receiver (explained below)
        registerReceiver(new DownloadCompleteReceiver(), new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    // This broadcast receiver expects a signal when an active downloading status changes.
    // When the file completes the download, it will pop out a toast
    public class DownloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
                Toast.makeText(context,"Download completed", Toast.LENGTH_LONG).show();
                wakeLock.release();
            }
        }
    }

}