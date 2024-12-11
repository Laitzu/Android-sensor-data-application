package com.example.sensordataapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String SERVER_URL = "http://xxx.xxx.xxx.xxx/submit_data";
    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;
    private Sensor gravitySensor;
    private LocationManager locationManager;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private TextView accelerometerData;
    private Button btnStartCollection, btnStopCollection, sendButton;

    private List<Data> collectedData;

    private final float[] gravityValues = new float[3];
    private boolean sensorEnabled = false;
    private boolean isCollectingData = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements for showing data in realtime
        accelerometerData = findViewById(R.id.tv_accelerometer_data);
        btnStartCollection = findViewById(R.id.btn_start_collection);
        btnStopCollection = findViewById(R.id.btn_stop_collection);
        sendButton = findViewById(R.id.sendButton);

        // Initialize SensorManager which handles the sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        // Initialize linear acceleration and gravity sensors
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Initialize locationManager for accessing the phones GPS data
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // Start listening for location updates
        requestLocationUpdates();

        // Initialize data storage
        collectedData = new ArrayList<>();

        // Set listeners for the buttons
        btnStartCollection.setOnClickListener(v -> startDataCollection());
        btnStopCollection.setOnClickListener(v -> stopDataCollection());
        sendButton.setOnClickListener(v -> sendDataToServer(collectedData, this));
    }

    // Make sure sensors are only used when the app is in focus
    @Override
    protected void onResume() {
        super.onResume();
        enableSensor();
    }
    @Override
    protected void onPause() {
        super.onPause();
        disableSensor();
    }
    private void enableSensor() {
        // Enable sensors
        if (linearAccelerationSensor != null && gravitySensor != null && !sensorEnabled) {
            sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_FASTEST);
            sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
            sensorEnabled = true;
        }
    }
    private void disableSensor() {
        // Disable sensors
        if (sensorEnabled) {
            sensorManager.unregisterListener(this);
            sensorEnabled = false;
        }
    }

    private void startDataCollection() {
        if (!isCollectingData) {
            isCollectingData = true;
            collectedData.clear();
            Toast.makeText(this, "Data collection started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopDataCollection() {
        if (isCollectingData) {
            isCollectingData = false;
            Toast.makeText(this, "Data collection stopped and saved", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    // Function that gets called when new values arrive from the sensor
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            // Update the gravity vector as received from the sensor
            gravityValues[0] = event.values[0];
            gravityValues[1] = event.values[1];
            gravityValues[2] = event.values[2];
        }

        // Get linear acceleration vector value
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION && sensorEnabled) {
            float accx = event.values[0];
            float accy = event.values[1];
            float accz = event.values[2];

            // Calculate the vertical acceleration with the gravity and linear acceleration vector
            float verticalAcc = calculateVerticalAcceleration(event.values, gravityValues);

            // Display accelerometer data
            String data = String.format("AccX: %.2f\nAccY: %.2f\nAccZ: %.2f\nVertical Acc: %.2f", accx, accy, accz, verticalAcc);
            accelerometerData.setText(data);

            // Collect data if data collection is active
            if (isCollectingData) {
                int timestamp = (int) (System.currentTimeMillis() / 1000) + 7200;
                Data dataObject = new Data(timestamp, accx, accy, accz, (float) currentLatitude, (float) currentLongitude, verticalAcc);
                collectedData.add(dataObject);
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Must be implemented in class even if not used
    }

    private float calculateVerticalAcceleration(float[] linearAcc, float[] gravity) {
        // Calculate the unit vector of the gravity vector
        // Find the magnitude of the gravity vector
        float gravityMagnitude = (float) Math.sqrt(gravity[0] * gravity[0] + gravity[1] * gravity[1] + gravity[2] * gravity[2]);
        // Calculate the unit vector components
        float unitGravityX = gravity[0] / gravityMagnitude;
        float unitGravityY = gravity[1] / gravityMagnitude;
        float unitGravityZ = gravity[2] / gravityMagnitude;

        // Calculate the dot product of the linear acceleration vector and the unit gravity vector
        return linearAcc[0] * unitGravityX + linearAcc[1] * unitGravityY + linearAcc[2] * unitGravityZ;
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }
        // Request for location updates
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // Update latitude and longitude whenever location changes
                currentLatitude = location.getLatitude();
                currentLongitude = location.getLongitude();
            }
            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) { }
            @Override
            public void onProviderEnabled(String provider) { }
            @Override
            public void onProviderDisabled(String provider) { }
        });
    }

    private void sendDataToServer(List<Data> data, Context context) {
        // Get id of the device
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);


        // Execute network stuff on a different thread
        ExecutorService service = Executors.newSingleThreadExecutor();

        service.execute(() -> {
            HttpURLConnection urlConnection = null;

            try {
                URL url = new URL(SERVER_URL);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");

                // Create JSON payload that contains the data in a JSON payload
                JSONObject jsonPayload = new JSONObject();
                // Add deviceId to payload
                jsonPayload.put("deviceId", deviceId);
                JSONArray dataArray = new JSONArray();

                // Populate JSON data array with data from each dataobject
                for (Data obj : data) {
                    JSONObject dataObject = new JSONObject();
                    dataObject.put("time", obj.getTime());
                    dataObject.put("accx", obj.getAccx());
                    dataObject.put("accy", obj.getAccy());
                    dataObject.put("accz", obj.getAccz());
                    dataObject.put("lat", obj.getLat());
                    dataObject.put("lon", obj.getLon());
                    dataObject.put("vertAcc", obj.getVerticalAcc());
                    dataArray.put(dataObject);
                }

                // Add the data array to the payload
                jsonPayload.put("data", dataArray);

                // Send the JSON payload to the server
                OutputStream os = urlConnection.getOutputStream();
                os.write(jsonPayload.toString().getBytes());
                os.flush();
                os.close();

                // Handle server response
                int responseCode = urlConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    runOnUiThread(() -> Toast.makeText(context, "Data sent successfully", Toast.LENGTH_SHORT).show());
                } else {
                    Log.e("NetworkError", "Failed to send data, response code: " + responseCode);
                    runOnUiThread(() -> Toast.makeText(context, "Failed to send data", Toast.LENGTH_SHORT).show());
                }

            } catch (JSONException e) {
                Log.e("NetworkError", "JSON Error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(context, "JSON Error", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e("NetworkError", "Error sending data: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(context, "Error sending data", Toast.LENGTH_SHORT).show());

                // Make sure to disconnect
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        });
    }
}
