package com.khilman.www.learngoogleapi;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.libraries.places.widget.AutocompleteSupportFragment;
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;
import com.khilman.www.learngoogleapi.directionhelpers.FetchURL;
import com.khilman.www.learngoogleapi.directionhelpers.TaskLoadedCallback;
import com.khilman.www.learngoogleapi.network.ApiServices;
import com.khilman.www.learngoogleapi.network.InitLibrary;
import com.khilman.www.learngoogleapi.placehelpers.FieldSelector;
import com.khilman.www.learngoogleapi.response.Distance;
import com.khilman.www.learngoogleapi.response.Duration;
import com.khilman.www.learngoogleapi.response.LegsItem;
import com.khilman.www.learngoogleapi.response.ResponseRoute;

import java.text.DecimalFormat;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OjekActivity extends AppCompatActivity implements OnMapReadyCallback, TaskLoadedCallback {
    private GoogleMap mMap;

    private String API_KEY = "AIzaSyBuyVE7jwezt37fHERMHckv8EtRMXaUeqs";

    public LatLng pickUpLatLng = null;
    public LatLng locationLatLng = null;

    private TextView tvStartAddress, tvEndAddress;
    private TextView tvPrice, tvDistance;
    private Button btnNext;
    // Deklarasi variable
    private TextView tvPickUpFrom, tvDestLocation;

    public static final int PICK_UP = 0;
    public static final int DEST_LOC = 1;
    private static int REQUEST_CODE = 0;

    private FieldSelector fieldSelector;
    private PlacesClient placesClient;
    TextView customfields;
    LatLng coord1, coord2;
    private Polyline currentPolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ojek);
        //getSupportActionBar().setTitle("Ojek Hampir Online");

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), API_KEY);
        }

        placesClient = Places.createClient(this);

        fieldSelector =
                new FieldSelector(
                        findViewById(R.id.custom_fields_list));

        customfields = findViewById(R.id.custom_fields_list);
        customfields.setVisibility(View.GONE);

        // Inisialisasi Widget
        wigetInit();
        setupAutocompleteSupportFragment();

    }

    // Method untuk Inisilisasi Widget agar lebih rapih
    private void wigetInit() {
        // Maps
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapNearBy);
        mapFragment.getMapAsync(this);

    }

    private void setupAutocompleteSupportFragment() {
        final AutocompleteSupportFragment autocompleteSupportFragment =
                (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.autocomplete_support_fragment);
        autocompleteSupportFragment.setPlaceFields(getPlaceFields());
        autocompleteSupportFragment.setOnPlaceSelectedListener(getPlaceSelectionListener("1"));

        final AutocompleteSupportFragment lokasi2 =
                (AutocompleteSupportFragment)
                        getSupportFragmentManager().findFragmentById(R.id.lokasi2);
        lokasi2.setPlaceFields(getPlaceFields());
        lokasi2.setOnPlaceSelectedListener(getPlaceSelectionListener("2"));

    }

    private List<Place.Field> getPlaceFields() {
        return fieldSelector.getAllFields();
    }

    @NonNull
    private PlaceSelectionListener getPlaceSelectionListener(String lokasi) {
        return new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                if (lokasi.equalsIgnoreCase("1")){
                    coord1 = place.getLatLng();
                }

                else{
                    coord2 = place.getLatLng();

                    mMap.addMarker(new MarkerOptions().position(coord1).title("Location 1"));
                    mMap.addMarker(new MarkerOptions().position(coord2).title("Location 2"));

                    new FetchURL(OjekActivity.this).execute(getUrl(coord1, coord2, "driving"), "driving");

                    set_camera(coord1,coord2);

                    Double x = SphericalUtil.computeDistanceBetween(coord1, coord2);

                    TextView tv_jarak = findViewById(R.id.tv_jarak);
                    tv_jarak.setText(String.format("%.2f", x*0.001)+" km");

                }
//                Toast.makeText(MainActivity.this, StringUtil.stringifyAutocompleteWidget(place), Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Status status) {
//                responseView.setText(status.getStatusMessage());
                Toast.makeText(OjekActivity.this, status.getStatusMessage(), Toast.LENGTH_SHORT).show();
            }
        };
    }

    void set_camera(LatLng latlng1, LatLng latlng2){
        LatLngBounds.Builder latLongBuilder = new LatLngBounds.Builder();
        latLongBuilder.include(latlng1);
        latLongBuilder.include(latlng2);

        // Bounds Coordinata
        LatLngBounds bounds = latLongBuilder.build();

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int paddingMap = (int) (width * 0.2); //jarak dari
        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, paddingMap);
        mMap.animateCamera(cu);
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
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + API_KEY;
        return url;
    }

    @Override
    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mMap.addPolyline((PolylineOptions) values[0]);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setPadding(10, 180, 10, 10);
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }

    public double CalculationByDistance(LatLng StartP, LatLng EndP) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = StartP.latitude;
        double lat2 = EndP.latitude;
        double lon1 = StartP.longitude;
        double lon2 = EndP.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
    }

}
