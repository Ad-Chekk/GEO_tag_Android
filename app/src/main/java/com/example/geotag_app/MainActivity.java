package com.example.geotag_app;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.location.Location;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int CAMERA_REQUEST_CODE = 456;

    private EditText editLatitude, editLongitude, editAltitude, editAccuracy, editNote;
    private ImageView imageViewPhoto;
    private Bitmap photoBitmap;
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        initializeViews();

        // Initialize FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
        findViewById(R.id.btnGetLocation).setOnClickListener(v -> getCurrentLocation());
        findViewById(R.id.btnTakePhoto).setOnClickListener(v -> takePhoto());
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission not granted", Toast.LENGTH_SHORT).show();
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        editLatitude.setText(String.valueOf(location.getLatitude()));
                        editLongitude.setText(String.valueOf(location.getLongitude()));
                        editAltitude.setText(String.valueOf(location.getAltitude()));
                        editAccuracy.setText(String.valueOf(location.getAccuracy()));
                    } else {
                        Toast.makeText(this, "Unable to get location", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void takePhoto() {
        android.content.Intent intent = new android.content.Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == CAMERA_REQUEST_CODE && resultCode == RESULT_OK) {
            photoBitmap = (Bitmap) data.getExtras().get("data");
            Bitmap taggedBitmap = addGeoTagToImage(photoBitmap);
            imageViewPhoto.setImageBitmap(taggedBitmap);
        }
    }

    private Bitmap addGeoTagToImage(Bitmap originalBitmap) {
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);
        Paint grayBoxPaint = new Paint();
        grayBoxPaint.setColor(Color.GRAY);
        grayBoxPaint.setAlpha(200);

        int boxHeight = 150;
        canvas.drawRect(0, mutableBitmap.getHeight() - boxHeight,
                mutableBitmap.getWidth() / 2, mutableBitmap.getHeight(), grayBoxPaint);

        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(30);

        String locationText = String.format(
                "Lat: %s, Lon: %s\nNote: %s\nTime: %s",
                editLatitude.getText().toString(),
                editLongitude.getText().toString(),
                editNote.getText().toString(),
                new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())
        );

        canvas.drawText(locationText, 10, mutableBitmap.getHeight() - boxHeight + 40, textPaint);
        return mutableBitmap;
    }

    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
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
