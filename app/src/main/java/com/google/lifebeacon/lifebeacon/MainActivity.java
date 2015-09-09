package com.google.lifebeacon.lifebeacon;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.estimote.sdk.Beacon;
import com.estimote.sdk.Nearable;
import com.estimote.sdk.Region;
import com.estimote.sdk.eddystone.Eddystone;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.estimote.sdk.BeaconManager;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final int REQUEST_ENABLE_BT = 1;
    TextView indicatorView;
    GoogleApiClient mGoogleApiClient;
    Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private boolean mScanning = false;
    private BeaconManager mBeaconManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBeaconManager = new BeaconManager(this);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }


        mHandler = new Handler(Looper.getMainLooper());
        final TextView ssidView = (TextView) findViewById(R.id.ssid);
        final TextView passwordView = (TextView) findViewById(R.id.password);
        buildGoogleApiClient();
        indicatorView = (TextView) findViewById(R.id.indicator);
        findViewById(R.id.connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect(ssidView.getText().toString(), passwordView.getText().toString());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendLocation();
                    }
                }, 3000L);
            }
        });

        findViewById(R.id.scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });

        mBeaconManager.setEddystoneListener(new BeaconManager.EddystoneListener() {
            @Override
            public void onEddystonesFound(List<Eddystone> eddystones) {
                for (Eddystone eddyStone : eddystones) {
                    Log.i("E", "url is "  + eddyStone.url);
                    if (eddyStone.url != null && eddyStone.url.startsWith("http://lb")) {
                        Pattern pattern = Pattern.compile("http://lb/([^/]+)/([^/]+)");
                        Matcher matcher = pattern.matcher(eddyStone.url);
                        if (matcher.matches()) {
                            String ssid = matcher.group(1);
                            String pwd = matcher.group(2);
                            connect(ssid, pwd);
                            sendLocation();
                            Log.i("E", "Success");
                            mBeaconManager.stopEddystoneScanning(scanId);
                            break;
                        }
                    }
                }
            }
        });


    }

    private void startScan() {
        connectToService();
    }

    private String scanId;
    private void connectToService() {

        mBeaconManager.connect(new BeaconManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {
                scanId = mBeaconManager.startEddystoneScanning();
            }
        });
    }

    private void connect(String ssid, String key) {
        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);


        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    private void sendLocation() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this, new HurlStack() {
            @Override
            protected HttpURLConnection createConnection(URL url) throws IOException {
                HttpURLConnection connection = super.createConnection(url);
                connection.setInstanceFollowRedirects(true);
                return connection;
            }
        });
        String url ="https://www.google.com/startTracking";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                                mGoogleApiClient);
                        if (mLastLocation != null) {
                            indicatorView.setText("Latitude: " + String.valueOf(mLastLocation.getLatitude()
                                    + "\nLongtitude: " + String.valueOf(mLastLocation.getLongitude())));
                        }
                        indicatorView.setText(indicatorView.getText() + "\nLocation sent! Saved!");
                        Log.i("main", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("main", "error" + error.toString());
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        sendLocation();
                    }
                }, 3000L);
            }
        });
        queue.add(stringRequest);
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.e("main", "GoogleApi connected");
        LocationRequest mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds

        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e("main", "GoogleApi connection failed" + connectionResult.toString());
    }

    @Override
    public void onLocationChanged(Location location) {
        indicatorView.setText("Latitude: " + String.valueOf(location.getLatitude()
                + "\nLongtitude: " + String.valueOf(location.getLongitude())));
    }
}
