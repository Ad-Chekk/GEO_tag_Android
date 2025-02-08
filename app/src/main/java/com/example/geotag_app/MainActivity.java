package com.example.geotag_app;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;




public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int CAMERA_REQUEST_CODE = 456;

    private EditText editLatitude, editLongitude, editAltitude,
            editAccuracy, editNote;
    private ImageView imageViewPhoto;
    private Bitmap photoBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        initializeViews();

        // Setup button listeners
        setupButtonListeners();

        // Check and request permissions
        checkPermissions();
    }

    private void initializeViews() {
        editLatitude = findViewById(R.id.editLatitude);
        editLongitude = findViewById(R.id.editLongitude);
        editAltitude = findViewById(R.id.editAltitude);
        editAccuracy = findViewById(R.id.editAccuracy);
        editNote = findViewById(R.id.editNote);
        imageViewPhoto = findViewById(R.id.imageViewPhoto);
    }

    private void setupButtonListeners() {
        // Get Location Button
        findViewById(R.id.btnGetLocation).setOnClickListener(v -> getCurrentLocation());

        // Take Photo Button
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> takePhoto());
    }

    private void getCurrentLocation() {
        // Check location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Location location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        if (location != null) {
            editLatitude.setText(String.valueOf(location.getLatitude()));
            editLongitude.setText(String.valueOf(location.getLongitude()));
            editAltitude.setText(String.valueOf(location.getAltitude()));
            editAccuracy.setText(String.valueOf(location.getAccuracy()));
        } else {
            Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
        }
    }

    private void takePhoto() {
        // Launch camera intent
        android.content.Intent intent = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            // Get the photo
            photoBitmap = (Bitmap) data.getExtras().get("data");

            // Add geotag to photo
            Bitmap taggedBitmap = addGeoTagToImage(photoBitmap);

            // Display the tagged image
            imageViewPhoto.setImageBitmap(taggedBitmap);
        }
    }

    private Bitmap addGeoTagToImage(Bitmap originalBitmap) {
        // Create a mutable copy of the bitmap
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);

        // Create canvas to draw on
        Canvas canvas = new Canvas(mutableBitmap);

        // Create paint for gray box
        Paint grayBoxPaint = new Paint();
        grayBoxPaint.setColor(Color.GRAY);
        grayBoxPaint.setAlpha(200);

        // Draw gray box in bottom left
        int boxHeight = 150;
        canvas.drawRect(
                0,
                mutableBitmap.getHeight() - boxHeight,
                mutableBitmap.getWidth() / 2,
                mutableBitmap.getHeight(),
                grayBoxPaint
        );

        // Create paint for text
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);

        // Prepare text with location and note
        String locationText = String.format(
                "Lat: %s, Lon: %s\nNote: %s\nTime: %s",
                editLatitude.getText().toString(),
                editLongitude.getText().toString(),
                editNote.getText().toString(),
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())
        );

        // Draw text
        canvas.drawText(
                locationText,
                10,
                mutableBitmap.getHeight() - boxHeight + 40,
                textPaint
        );

        return mutableBitmap;
    }

    private void checkPermissions() {
        // Check and request permissions
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        if (!hasPermissions(permissions)) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    private boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissions Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}