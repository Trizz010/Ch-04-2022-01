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

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private TextView  myTxtView;
    private URL url;
    private String filename;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myTxtView = findViewById(R.id.txtView1);

        //Local storage management requires runtime permission granting upon installing the app
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.MANAGE_EXTERNAL_STORAGE, Manifest.permission.INTERNET}, 1);

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
        registerReceiver(new DonwloadCompleteReceiver(), new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    // This broadcast receiver expects a signal when an active downloading status changes.
    // When the file completes the download, it will pop out a toast
    public class DonwloadCompleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)){
                Toast.makeText(context,"Download completed", Toast.LENGTH_LONG).show();

            }
        }
    }

}