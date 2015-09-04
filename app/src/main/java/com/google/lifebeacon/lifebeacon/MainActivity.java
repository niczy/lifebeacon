package com.google.lifebeacon.lifebeacon;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.messages.Message;
import com.google.android.gms.nearby.messages.MessageFilter;
import com.google.android.gms.nearby.messages.MessageListener;
import com.google.android.gms.nearby.messages.Strategy;

public class MainActivity extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private static final String TAG = "LifeBeacon";

    private final MessageListener messageListener = new MessageListener() {
        // Called each time a new message is discovered nearby.
        @Override
        public void onFound(Message message) {
            String nearbyMessageNamespace = message.getNamespace();
            String nearbyMessageType = message.getType();
            String nearbyMessageString = new String(message.getContent());

            Log.i(TAG, "Message string: " + nearbyMessageString);
            Log.i(TAG, "Message namespaced type: " + nearbyMessageNamespace +
                    "/" + nearbyMessageType);
        }

        // Called when a message is no longer nearby.
        @Override
        public void onLost(Message message) {
            Log.i(TAG, "Lost message: " + message);
        }
    };

    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Nearby.MESSAGES_API)
                        // These callbacks happen on the main thread.
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();


        connect("zzz", "531531531");
        sendLocation();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Connecting GoogleApiClient.");
        mGoogleApiClient.connect();
    }

    private void subscribeBeacon() {

        // Subscribe to receive nearby messages created by this Developer Console project.
        // Use Strategy.BLE_ONLY because we are only interested in messages that we
        // attached to BLE beacons.
        Nearby.Messages.subscribe(mGoogleApiClient, messageListener, Strategy.BLE_ONLY)
                .setResultCallback(new ErrorCheckingCallback("subscribe()"));
    }

    private void connect(String ssid, String key) {
        WifiConfiguration wifiConfig = new WifiConfiguration();

        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", key);

        WifiManager wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        //remember id
        int netId = wifiManager.addNetwork(wifiConfig);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }

    private void sendLocation() {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url ="http://www.google.com";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("main", response);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("main", error.getStackTrace().toString());
            }
        });
        queue.add(stringRequest);
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

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
