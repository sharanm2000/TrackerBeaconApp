package com.ram.tracker;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.events.Event;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.JsonObject;
import com.ram.tracker.directionHelpers.FetchURL;
import com.ram.tracker.directionHelpers.TaskLoadedCallback;
import com.squareup.okhttp.HttpUrl;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MyLocationActivity extends FragmentActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMyLocationClickListener, GoogleMap.OnMyLocationButtonClickListener, GoogleMap.OnCameraMoveListener, TaskLoadedCallback,
        GoogleMap.OnCameraIdleListener {
    private GoogleMap googleMap;
    ImageView imgLocationPinUp;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private LocationCallback locationCallback;
    boolean requestingLocationUpdates = true;
    private FirebaseFirestore firestoreDB;
    Button captureDestination, showDirections, startDrive;
    String name;
    OkHttpClient client;
    LocationManager locationManager;
    LatLng current, destination;
    Request request;
    private Polyline currentPolyline;
    private static final int REQUEST_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_location);
        imgLocationPinUp = findViewById(R.id.imgLocationPinUp);
        firestoreDB = FirebaseFirestore.getInstance();
        client = new OkHttpClient();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationRequest = new LocationRequest();
        showDirections = findViewById(R.id.showDirections);
        startDrive = findViewById(R.id.startDrive);
        name = getIntent().getStringExtra("name");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        captureDestination = findViewById(R.id.captureDestination);
        startDrive.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    // OnGPS();
                } else {
                    getLocation();
                }
            }
        });
        captureDestination.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                imgLocationPinUp.setVisibility(View.VISIBLE);
                googleMap.setOnCameraMoveListener(MyLocationActivity.this);
                googleMap.setOnCameraIdleListener(MyLocationActivity.this);
            }
        });
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {

                    //Log.v("sdfsdfsd", "" + location.getLatitude() + location.getLongitude());

                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 13));
                    current = new LatLng(location.getLatitude(), location.getLongitude());
                    googleMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Current Location"));
                    Vehicles vehicles = new Vehicles();
                    vehicles.setName(name);
                    vehicles.setLatitude(location.getLatitude());
                    vehicles.setLongitude(location.getLongitude());
                    vehicles.setPath(false);
                    vehicles.setDestination_latitude(0.0);
                    vehicles.setDestination_longitude(0.0);
                    vehicles.setOrigin_latitude(0.0);
                    vehicles.setOrigin_longitude(0.0);

                    firestoreDB.collection("vehicles").document(vehicles.getName()).set(vehicles).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(MyLocationActivity.this, "added", Toast.LENGTH_LONG).show();
                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e("DATA-EXCEPTION", e.toString());
                            e.printStackTrace();
                        }
                    });

                }
            }
        };
        showDirections.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new FetchURL(MyLocationActivity.this).execute(getUrl(current, destination, "driving"), "driving");

                Vehicles vehicles = new Vehicles();
                vehicles.setName(name);
                vehicles.setLatitude(current.latitude);
                vehicles.setLongitude(current.longitude);
                vehicles.setPath(true);
                vehicles.setDestination_latitude(destination.latitude);
                vehicles.setDestination_longitude(destination.longitude);
                vehicles.setOrigin_latitude(current.latitude);
                vehicles.setOrigin_longitude(current.longitude);

                firestoreDB.collection("vehicles").document(vehicles.getName()).set(vehicles).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MyLocationActivity.this, "added", Toast.LENGTH_LONG).show();
                    }

                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e("DATA-EXCEPTION", e.toString());
                        e.printStackTrace();
                    }
                });
                googleMap.setOnCameraMoveListener(null);
                googleMap.setOnCameraIdleListener(null);
            }
        });
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            // Logic to handle location object


                        }
                    }
                });


    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        Log.v("DATA-URL", url);
        return url;
    }

    private void addDocumentToCollection(Event event) {
        firestoreDB.collection("events")
                .add(event)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("TAG", "Event document added - id: "
                                + documentReference.getId());
                        //restUi();
                        Toast.makeText(MyLocationActivity.this,
                                "Event document has been added",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("TAG", "Error adding event document", e);
                        Toast.makeText(MyLocationActivity.this,
                                "Event document could not be added",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        enableMyLocation();
        googleMap.setOnMyLocationClickListener(this);
        googleMap.setOnMyLocationButtonClickListener(this);

    }

    @Override
    public void onClick(View view) {
        //goToMyLocation();
    }

//    private void goToMyLocation()
//	{
//        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
//                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            enableMyLocation();
//        } else {
//            googleMap.setMyLocationEnabled(true);
//            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
//            Criteria criteria = new Criteria();
//            Location location = locationManager.getLastKnownLocation(locationManager.getBestProvider(criteria, false));
//            if (location != null) {
//
//            }
//        }
//    }

    // need for android 6 and above
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE, Manifest.permission.ACCESS_FINE_LOCATION, true);
        } else if (googleMap != null) {
            googleMap.setMyLocationEnabled(true);
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    // need for android 6 and above
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
            enableMyLocation();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (requestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onMyLocationClick(@NonNull Location location) {
        Log.d(MainActivity.TAG, "onMyLocationClick() called with: location = [" + location + "]");
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Log.d(MainActivity.TAG, "onMyLocationButtonClick() called");
        goToMyLocation();
        return true;
    }

    private void goToMyLocation() {


    }

    @Override
    public void onCameraIdle() {
        googleMap.clear();
        imgLocationPinUp.setVisibility(View.INVISIBLE);


        destination = googleMap.getCameraPosition().target;
        Geocoder geocoder = new Geocoder(this);
        googleMap.addMarker(new MarkerOptions().position(new LatLng(destination.latitude, destination.longitude)).title("Destination Location"));
        googleMap.addMarker(new MarkerOptions().position(current).title("Current Location"));
        try {
            List<Address> addressList = geocoder.getFromLocation(destination.latitude, destination.longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                String locality = addressList.get(0).getAddressLine(0);
                String country = addressList.get(0).getCountryName();
                if (!locality.isEmpty() && !country.isEmpty())
                    // resutText.setText(locality + "  " + country);
                    Toast.makeText(getApplicationContext(), locality + "  " + country, Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (locationGPS != null) {
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();
                //latitude = String.valueOf(lat);
                //longitude = String.valueOf(longi);
                MarkerOptions markerOptions = new MarkerOptions().position(new LatLng(locationGPS.getLatitude(), locationGPS.getLongitude())).title("ambulance").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));

                googleMap.addMarker(markerOptions);


                // checkRange();
                new check_range().execute(String.valueOf(locationGPS.getLatitude()) + "@@" + String.valueOf(locationGPS.getLongitude()) + "@@" + name);


                Log.v("ddddd", locationGPS.getLatitude() + "" + locationGPS.getLongitude() + "");
                // showLocation.setText("Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude);
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private String checkRange() {

        return "";


    }

    @Override
    public void onCameraMove() {
        imgLocationPinUp.setVisibility(View.VISIBLE);

    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = googleMap.addPolyline((PolylineOptions) values[0]);
    }


    public class check_range extends AsyncTask<String, String, String> {

        String res = "";

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);


        }

        @Override
        protected String doInBackground(String... strings) {


            //  String lat = strings[0];
            //   String lon = strings[1];

            String[] coord_val = strings[0].split("@@");
            Log.v("dsdsdsd", coord_val[2]);
            request = new Request.Builder()
                    //.url("http://192.168.29.182:8080/getAllBeacons")
                    .url("http://192.168.29.72:8080/getAllBeacons")
                    .addHeader("amb_lat", coord_val[0])
                    .addHeader("amb_lon", coord_val[1])
                    .addHeader("name", coord_val[2])
                    .build();

            try {
                final Response response = client.newCall(request).execute();
                res = response.body().string();


                JSONArray jsonObject = new JSONArray(res);


                for (int k = 0; k < jsonObject.length(); k++) {

                    JSONObject object = jsonObject.getJSONObject(k);
                    String name = object.getString("name");
                    final String lat = object.getString("latitude");
                    final String lon = object.getString("longitude");
                    final String result = object.getString("result");


                    Log.v("asasasas", name);
                    Log.v("asasasas", lat);
                    Log.v("asasasas", lon);
                    Log.v("asasasas", result);


                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            CircleOptions circleOptions = new CircleOptions();
                            circleOptions.center(new LatLng(Double.parseDouble(lat), Double.parseDouble(lon)));
                            circleOptions.radius(700);
                            circleOptions.fillColor(Color.TRANSPARENT);
                            circleOptions.strokeWidth(6);
                            googleMap.addCircle(circleOptions);
                            Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
                        }
                    });
                    //geocoder = new Geocoder(MapsActivity.this, getDefault());
                    ////MarkerOptions markerOptions = new MarkerOptions();
                    //markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                    // mMap.addMarker(markerOptions.title(String.valueOf(location)));

                }


                // System.out.println(response.body().string());

            } catch (IOException | JSONException e) {
                e.printStackTrace();
            }


            return res;
        }
    }
}