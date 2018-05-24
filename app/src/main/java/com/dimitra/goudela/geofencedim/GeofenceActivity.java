package com.dimitra.goudela.geofencedim;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import com.google.android.gms.location.LocationListener;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class GeofenceActivity extends AppCompatActivity
        implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback,
//        GoogleMap.OnMapClickListener,
        GoogleMap.OnMarkerClickListener, ResultCallback<Status> {

    private static final String TAG = GeofenceActivity.class.getSimpleName();

    private TextView textLat, textLong;
    private MapFragment mapFragment;
    private GoogleMap map;

    private GoogleApiClient googleApiClient;

    private Location lastLocation;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_geofence);

        //TextView για τις συντεταγμένες
        textLat = (TextView)findViewById(R.id.lat);
        textLong = (TextView)findViewById(R.id.lon);

        //Αρχικοποίηση GoogleMaps
        initGMaps();

        //Δημιουργία GoogleApiClient
        createGoogleApi();

        Button btn_geo = (Button) findViewById(R.id.btn_geo);


        btn_geo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGeofence();
            }
        });


    }

    //Δημιουργία GoogleApiClient για πρόσβαση στα Location Services
    private void createGoogleApi()    {
        Log.d(TAG,"createGoogleApi()");
        if(googleApiClient == null){
            googleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi( LocationServices.API )
                    .build();
        }
    }

    //Αρχικοποίηση GoogleMaps
    private void initGMaps(){
        //map fragment sto xml
        mapFragment = (MapFragment)getFragmentManager().findFragmentById(R.id.fragment);
        mapFragment.getMapAsync(this);
    }

/*    //Καλείται όταν κλικάρουμε στο χάρτη
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG,"onMapClick("+latLng+")");
        markerFofGeofence(latLng);
    }*/

    //Καλείται όταν κλικάρουμε έναν marker
    @Override
    public boolean onMarkerClick(Marker marker) {
        Log.d(TAG, "onMarkerClickListener: " + marker.getPosition() );
        return false;
    }

    //Καλείται όταν ο χάρτης είναι έτοιμος για χρήση
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.d(TAG,"onMapReady()");
        map = googleMap;
//        map.setOnMapClickListener(this);
        map.setOnMarkerClickListener(this);

    }

    @Override
    protected void onStart() {
        super.onStart();

        //Σύνδεση στον GoogleApiClient με την έναρξη του activity
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();

        //Αποσύνδεση του GoogleApiClient στο τέλος του activity
        googleApiClient.disconnect();
    }

    //GoogleApiClient.ConnectionCallbacks cconnected
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "onConnected()");
        getLastKnownLocation();
    }
//Λήψη της τελευταίας γνωστής θέσης (last known location)
    private void getLastKnownLocation() {
        Log.d(TAG,"getLastKnownLocation()");
        if(checkPermission()) {
            lastLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
            if (lastLocation != null) {
                Log.i(TAG, "Lastknown location" +
                        "lon:" + lastLocation.getLongitude() +
                        " lat: " + lastLocation.getLatitude());
                //Εμφανιση συνταταγμένων στα Textviews
                writeLastLocation();
                //
                startLocationUpdates();
            } else {
                Log.w(TAG, "no location retrieved yet");
                startLocationUpdates();
            }
        }
        else askPermission();
                
    }

    private LocationRequest locationRequest;


    //define in mili seconds
    //this number is extremely low, and should only be used only for debug
    private final int UPADATE_INTERVAL = 1000;
    private final int FASTEST_INTERVAL = 900;
    private final int REQ_PERMISSION = 1;

    //start location updates
    private void startLocationUpdates(){
        Log.i(TAG,"startLocationUpdates()");
        locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPADATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);

        if( checkPermission()) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }

    }


    //GoogleApiClient.ConnectionCallbacks suspended
    @Override
    public void onConnectionSuspended(int i) {
        Log.w(TAG,"onConnectionSuspended");

    }

    //GoogleAPiClient.onConnectionFailedListener fail
    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.w(TAG,"onConnectioFailed()");

    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG,"onLocationChanged ["+location+"]");
        lastLocation = location;
        writeActualLocation(location);

    }
    //Εμφάνιση των συντεταγμένων στο UI, στα Textviews Που ορίσαμε για τις συντεταγμένες
    private void writeActualLocation(Location location){
        textLat.setText("Lat: " + location.getLatitude());
        textLong.setText("Long: " + location.getLongitude());
        //markerLocation(new LatLng(55.9422796,-3.2240093));
        markerLocation(new LatLng(location.getLatitude(),location.getLongitude()));
    }

    private Marker locationMarker;

    //marker με την θέση του χρήστη
    private void markerLocation(LatLng latLng) {
        Log.i(TAG,"markerLocation("+latLng+")");
        String title = latLng.latitude + "," + latLng.longitude;
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if(map!=null){
            //remove the anterior marker
            if(locationMarker != null)
                locationMarker.remove();
            locationMarker = map.addMarker(markerOptions);
            float zoom = 14f;
            CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoom);
            map.animateCamera(cameraUpdate);
        }

    }

/*    private Marker geoFenceMarker;
    //create a marker for the geofence creation
    private void markerFofGeofence(LatLng latLng){
        Log.i(TAG,"makerForGeofence(" +latLng+")");
        String title = "Geo" + latLng.latitude + "," +latLng.longitude;
        //define marker options
        MarkerOptions markerOptions = new MarkerOptions()
                .position(latLng)
                .title(title);
        if (map!=null){
            //remove last geofenceMarker
            if(geoFenceMarker !=null)
                geoFenceMarker.remove();

            geoFenceMarker = map.addMarker(markerOptions);
        }
    }*/

    //Διάρκεια του Geofence σε miliseconds
    private static final long GEOFENCE_DURATION = 4* 60 * 60 *1000;//4 hours
    //Geofence id
    private static final String GEOFENCE_REQ_ID = "Stathmos Neos Kosmos";
    //Ακτίνα Geofence
    private static final float GEOFENCE_RADIUS = 500.0f; // in meters
    //Συντεταγμένες Geofence
    private static final double GEOFENCE_LAT = 55.911;//37.957705; //sintetagmenes gia stathmos metro neos kosmos
    private static final double GEOFENCE_LON = -3.2788;//23.728587;



    //Δημιουργία Geofence
    private Geofence createGeofence(LatLng latLng, float radius){
        Log.d(TAG,"createGeofence");
        return new Geofence.Builder()
                .setRequestId(GEOFENCE_REQ_ID)
                //.setCircularRegion( latLng.latitude, latLng.longitude, radius)
                .setCircularRegion(GEOFENCE_LAT, GEOFENCE_LON, GEOFENCE_RADIUS)
                .setExpirationDuration(GEOFENCE_DURATION)
                .setTransitionTypes( Geofence.GEOFENCE_TRANSITION_ENTER
                        | Geofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    //create a Geofence Request
    private GeofencingRequest createGeofenceRequest(Geofence geofence){
        Log.d(TAG,"createGeofencingReauest()");
        return new GeofencingRequest.Builder()
                .setInitialTrigger( GeofencingRequest.INITIAL_TRIGGER_ENTER )
                .addGeofence( geofence )
                .build();
    }

    private PendingIntent geoFencePendingIntent;
    private final int GEOFENCE_REQ_CODE = 0;
    private PendingIntent createGeofencePendingIntent(){
        Log.d(TAG,"createGeofencePendingIntent");
        // Reuse the PendingIntent if we already have it
        if(geoFencePendingIntent != null)
            return geoFencePendingIntent;

        Intent intent = new Intent(this, GeofenceTrasitionService.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        return PendingIntent.getService(
                this, GEOFENCE_REQ_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Προσθήκη GeofenceRequest στη λιστα παρακολούθησης
    private void addGeofence(GeofencingRequest request){
        Log.d(TAG,"addGeofence");
        if(checkPermission())
            LocationServices.GeofencingApi.addGeofences(
                    googleApiClient,
                    request,
                    createGeofencePendingIntent()
            ).setResultCallback(this);
    }

    //Υπολογισμός της απόστασης της τρέχουσας θέσης από το κέντρο του Geofence
    public float distance(){
        float[] distance = new float[2];

        Location.distanceBetween( lastLocation.getLatitude(), lastLocation.getLongitude(),
                geofenceCircle.getCenter().latitude, geofenceCircle.getCenter().longitude, distance);
        return distance[0];

    }

    public void onResult(@NonNull Status status){
        Log.i(TAG,"onResult: " + status);
        if (status.isSuccess()){
            //Δημιουργία Geofence
            //Ζωγραφίζω το Geofence με διάφανο χρώμα και το κάνω αόρατο
            drawGeofence();

            //Αν η τρέχουσα θέση δεν είναι μέσα στο Geofence
            if( distance() > geofenceCircle.getRadius() ){
                //Εμφανίζεται το αντίστοιχο μήνυμα με Toast
                Toast.makeText(this, "Outside Geofence. Distance from centre: " + distance() , Toast.LENGTH_LONG).show();

            } else {
                //Αν είναι μέσα στο Geofence εμφανίζεται το Geofence
                showCircle();
                Toast.makeText(this, "Inside Geofence. Distance from centre:   " + distance()  , Toast.LENGTH_LONG).show();
            }

        }else{
            Toast.makeText(this, "Fail ", Toast.LENGTH_LONG).show();
        }

    }
    //draw Geofence circle on GoogleMap
    private Circle geofenceCircle;

    //Δημιουργία Geofence
    //Το ζωγραφίζω διάφανο ώστε να μην είναι ορατό
    private void drawGeofence() {
        Log.d(TAG,"drawGeofence()");

        if( geofenceCircle !=null)
            geofenceCircle.remove();

        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(GEOFENCE_LAT, GEOFENCE_LON))
                .strokeColor(0x00000000)//διάφανο
                .fillColor(0x00000000)//διάφανο
                .radius(GEOFENCE_RADIUS);
        geofenceCircle = map.addCircle( circleOptions);
    }
    private void showCircle(){
        CircleOptions circleOptions = new CircleOptions()
                .center(new LatLng(GEOFENCE_LAT, GEOFENCE_LON))
                .strokeColor(Color.argb(50,70,70,70))
                .fillColor(Color.argb(100,150,150,150))
                .radius(GEOFENCE_RADIUS);
        geofenceCircle = map.addCircle( circleOptions);
    }


    //Start Geofence creation process
    private void startGeofence(){
        Log.i(TAG,"startGeofence()");

            Geofence geofence = createGeofence( new LatLng(GEOFENCE_LAT, GEOFENCE_LON), GEOFENCE_RADIUS );
            GeofencingRequest geofenceRequest = createGeofenceRequest(geofence);
            addGeofence( geofenceRequest);
     }


    private void writeLastLocation(){
        writeActualLocation(lastLocation);
    }

    //Ελέγχει αν έχει permission για προσβαση σε δεδομένα τοποθεσίας
    private boolean checkPermission(){
        Log.d(TAG,"checkPermission()");
        //ask for permission if it wasnt granted yet
        return (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED);
    }

    //Ζητάει αν έχει permission για προσβαση σε δεδομένα τοποθεσίας
    private void askPermission() {
        Log.d(TAG, "askPermission()");
        ActivityCompat.requestPermissions(
                this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQ_PERMISSION
        );
    }

    // Επιβεβαιώνει την έγκριση του χρήστη για πρόσβαση σε δεδομένα τοποθεσίας
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult()");
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch ( requestCode ) {
            case REQ_PERMISSION: {
                if ( grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED ){
                    // Εγκριση Permission
                    getLastKnownLocation();

                } else {
                    // Άρνηση Permission
                    permissionsDenied();
                }
                break;
            }
        }
    }

    //app cannot work without the permissions
    private void permissionsDenied(){
        Log.w(TAG,"permissionsDenied()");
    }


      public static Intent makeNotificationIntent(Context applicationContext, String msg) {
        Log.d(TAG,msg);
        return new Intent(applicationContext,GeofenceActivity.class);

    }
}
