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
        // Create a mutable copy of the bitmap
        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        // Calculate dimensions for the semi-transparent box
        int boxWidth = mutableBitmap.getWidth() / 2;  // Half of image width
        int boxHeight = mutableBitmap.getHeight() / 3; // One-third of image height
        int boxY = mutableBitmap.getHeight() - boxHeight;

        // Create semi-transparent gray box
        Paint boxPaint = new Paint();
        boxPaint.setColor(Color.GRAY);
        boxPaint.setAlpha(128); // 50% transparency
        canvas.drawRect(0, boxY, boxWidth, mutableBitmap.getHeight(), boxPaint);

        // Configure text paint with smaller size
        Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(boxHeight / 10); // Reduced text size
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.LEFT);

        // Calculate text positioning
        float lineHeight = textPaint.getTextSize() + 6; // Reduced line spacing
        float textX = 10; // Moved text more to the left
        float textY = boxY + lineHeight; // Start from top of box

        // Get note text before creating text lines
        String noteText = editNote.getText().toString().trim();
        if (noteText.isEmpty()) {
            noteText = ""; // Set empty string if no note is entered
        }

        // Draw each line of text with proper formatting
        String[] textLines = {
                String.format("Latitude: %s", editLatitude.getText().toString()),
                String.format("Longitude: %s", editLongitude.getText().toString()),
                String.format("Altitude: %s m", editAltitude.getText().toString()),
                String.format("Accuracy: %s m", editAccuracy.getText().toString()),
                String.format("Time: %s", new SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(new Date())),
                String.format("Note: %s", noteText)
        };

        // Draw text with proper wrapping to stay within gray box
        for (String line : textLines) {
            if (line.startsWith("Note: ")) {
                // Handle note text wrapping
                String noteContent = line.substring(6); // Remove "Note: " prefix
                String wrappedNote = wrapText(noteContent, textPaint, boxWidth - 20); // 20px padding

                // Draw "Note: " prefix
                canvas.drawText("Note:", textX, textY, textPaint);

                if (!noteContent.isEmpty()) {
                    textY += lineHeight;
                    String[] noteLines = wrappedNote.split("\n");
                    for (String noteLine : noteLines) {
                        canvas.drawText(noteLine, textX + 20, textY, textPaint); // Indent wrapped lines
                        textY += lineHeight;
                    }
                }
            } else {
                // Draw other lines normally
                canvas.drawText(line, textX, textY, textPaint);
                textY += lineHeight;
            }
        }

        return mutableBitmap;
    }

    // Helper method to wrap text within the box width (unchanged)
    private String wrapText(String text, Paint paint, float maxWidth) {
        String[] words = text.split("\\s");
        StringBuilder wrapped = new StringBuilder();
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            float testWidth = paint.measureText(line + " " + word);
            if (testWidth <= maxWidth) {
                if (line.length() > 0) {
                    line.append(" ");
                }
                line.append(word);
            } else {
                if (wrapped.length() > 0) {
                    wrapped.append("\n");
                }
                wrapped.append(line);
                line = new StringBuilder(word);
            }
        }

        // Add the last line
        if (line.length() > 0) {
            if (wrapped.length() > 0) {
                wrapped.append("\n");
            }
            wrapped.append(line);
        }

        return wrapped.toString();
    }

    // Add this helper method to format numbers nicely
    private String formatNumber(double number) {
        return String.format(Locale.US, "%.6f", number);
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