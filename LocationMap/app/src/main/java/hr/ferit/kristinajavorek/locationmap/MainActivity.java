package hr.ferit.kristinajavorek.locationmap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final int CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE = 0;
    public static final String MSG_KEY = "message";
    GoogleMap mGoogleMap;
    MapFragment mMapFragment;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Marker currLocationMarker;
    Geocoder geocoder;
    List<Address> addresses;
    LatLng latLng;
    String address;
    TextView tvAddress;
    SoundPool mSoundPool;
    boolean mLoaded = false;
    HashMap<Integer, Integer> mSoundMap = new HashMap<>();
    Button btn;
    File imagesFolder;
    Uri uriSavedImage;
    private GoogleMap.OnMapClickListener mCustomOnMapClickListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.initialize();
        this.loadSounds();
    }

    private void initialize() {
        this.btn= (Button) findViewById(R.id.btn);
        this.tvAddress = (TextView) findViewById(R.id.tvAddress);
        this.mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.fGoogleMap);
        this.mMapFragment.getMapAsync(this);
        this.mCustomOnMapClickListener = new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                playSound(R.raw.sound);
                MarkerOptions newMarkerOptions = new MarkerOptions();
                newMarkerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE));
                newMarkerOptions.title("Hi!");
                newMarkerOptions.draggable(true);
                newMarkerOptions.position(latLng);
                mGoogleMap.addMarker(newMarkerOptions);
            }
        };
        this.btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent imageIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                imagesFolder = Environment.getExternalStoragePublicDirectory("/HereIAm/Images");
                File image = new File(imagesFolder, address + ".png");
                uriSavedImage = Uri.fromFile(image);
                imageIntent.putExtra(MediaStore.EXTRA_OUTPUT, uriSavedImage);
                startActivityForResult(imageIntent, CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == CAPTURE_IMAGE_ACTIVITY_REQUEST_CODE) {
            if(resultCode == Activity.RESULT_OK) {
                sendNotification();
            }
        }
    }

    private void loadSounds(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = new SoundPool.Builder().setMaxStreams(10).build();
        }else{
            this.mSoundPool = new SoundPool(10, AudioManager.STREAM_MUSIC,0);
        }
        this.mSoundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                mLoaded = true;
            }
        });
        this.mSoundMap.put(R.raw.sound, this.mSoundPool.load(this, R.raw.sound,1));
    }

    void playSound(int selectedSound){
        int soundID = this.mSoundMap.get(selectedSound);
        this.mSoundPool.play(soundID, 1,1,1,0,1f);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    Toast.makeText(MainActivity.this, "Permission denied", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mGoogleMap = googleMap;
        UiSettings uiSettings = this.mGoogleMap.getUiSettings();
        uiSettings.setZoomControlsEnabled(true);
        uiSettings.setMyLocationButtonEnabled(true);
        uiSettings.setZoomGesturesEnabled(true);
        this.mGoogleMap.setOnMapClickListener(this.mCustomOnMapClickListener);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setCancelable(true);
                alertBuilder.setMessage("Location permission is necessary to find you on the map!");
                alertBuilder.setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                    }
                });
            } else {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }

        }
        this.mGoogleMap.setMyLocationEnabled(true);
        buildGoogleApiClient();
        mGoogleApiClient.connect();
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }


    @Override
    public void onConnected(Bundle bundle) {

        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            //mGoogleMap.clear();
        }
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(60000);
        mLocationRequest.setFastestInterval(10000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        mLocationRequest.setSmallestDisplacement(0.1F);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(this, "Connection Suspended", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "Connection Failed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        Location mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (mLastLocation != null) {
            //mGoogleMap.clear();
            geocoder = new Geocoder(this, Locale.getDefault());
            latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            Double latitude = mLastLocation.getLatitude();
            Double longitude = mLastLocation.getLongitude();
            try {
                addresses = geocoder.getFromLocation(latitude, longitude, 1);
                address = addresses.get(0).getAddressLine(0);
                String postalCode = addresses.get(0).getPostalCode();
                String city = addresses.get(0).getLocality();
                String country = addresses.get(0).getCountryName();
                tvAddress.setText("Latitude: " + latitude + ", Longitude: " + longitude + "\n" + address + "\n" + postalCode + " " + city + "\n" + country);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (currLocationMarker != null) {
            currLocationMarker.remove();
        }
    }

    private void sendNotification() {
        String msgText = "Click to see your picture";

        Intent notificationIntent = new Intent(this,MainActivity.class);
        notificationIntent.putExtra(MSG_KEY, msgText);
        notificationIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivity(notificationIntent);

        Intent intent = new Intent();
        //intent.setType("image/*");
        intent.setDataAndType(uriSavedImage, "image/png");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setAutoCancel(true)
                .setContentTitle("Picture successfully saved")
                .setContentText(msgText)
                .setSmallIcon(android.R.drawable.ic_dialog_email)
                .setContentIntent(notificationPendingIntent)
                .setLights(Color.BLUE, 2000, 1000)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        Notification notification = notificationBuilder.build();
        NotificationManager notificationManager =(NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0,notification);
        this.finish();

    }

}

