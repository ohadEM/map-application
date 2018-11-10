package com.example.joel.currentlocation;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private static final String TAG = MapsActivity.class.getSimpleName();
    private GoogleMap mMap;
    private Spinner spinner;
    private ArrayAdapter<String> adapter;
    private List<LatLng> markers;
    private Button searchBtn;
    private EditText searchText;

    enum Url {
        CURRENT,
        NEARBY,
        AUTO_COMPLETE
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        spinner = findViewById(R.id.spinner);
        markers = new ArrayList<>();
        searchBtn = findViewById(R.id.search_btn);
        searchText = findViewById(R.id.search_text);


        Drawable spinnerDrawable = spinner.getBackground().getConstantState().newDrawable();

        spinnerDrawable.setColorFilter(getResources().getColor(android.R.color.black), PorterDuff.Mode.SRC_ATOP);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            spinner.setBackground(spinnerDrawable);
        } else {
            spinner.setBackgroundDrawable(spinnerDrawable);
        }
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setCamera(markers.get(position));
            }

            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        mapFragment.getMapAsync(this);
    }

    private void setCamera(LatLng position) {
        CameraUpdate location = CameraUpdateFactory.newLatLngZoom(
                position, 17);
        mMap.animateCamera(location);
    }

    public void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle(R.string.title_location_permission)
                        .setMessage(R.string.text_location_permission)
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
        } else {
        }
    }


    private void getCurrentLocation() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(final Location location) {
                showLocation(location.getLatitude(), location.getLongitude(), Url.CURRENT);

                searchBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showLocation(location.getLatitude(), location.getLongitude(), Url.AUTO_COMPLETE);
                    }
                });
                showLocation(location.getLatitude(), location.getLongitude(), Url.NEARBY);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
                Log.d(TAG, "onStatusChanged: ");
            }

            @Override
            public void onProviderEnabled(String provider) {
                Log.d(TAG, "onProviderEnabled: ");
            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d(TAG, "onProviderDisabled: ");
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            checkLocationPermission();
            return;
        }
        if (lm != null) {
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 10, locationListener);
        }

    }

    private void showLocation(final double latitude, final double longitude, final Url type) {
        String url;
        switch (type) {
            case CURRENT:
                url = getResources().getString(R.string.get_current_location_url, latitude, longitude, getString(R.string.google_maps_key));
                break;

            case NEARBY:
                url = getResources().getString(R.string.get_nearby_places_url, latitude, longitude, getString(R.string.google_maps_key));
                break;

            default:
                String text = searchText.getText().toString();
                url = getResources().getString(R.string.get_autocomplete_url, latitude, longitude, text.replace(" ", "+"), getString(R.string.google_maps_key));
                break;
        }

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        switch (type) {
                            case CURRENT:
                                String name = getLocationNameFromJson(response);
                                showMarkOnMap(mMap, latitude, longitude, name);
                                viewLocationText(response);
                                break;

                            case NEARBY:
                                getNearbyLocation(mMap, response);
                                break;

                            default:
                                showSelectLocationDialog(response);
                                break;
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, "onErrorResponse: ");
            }
        });
        RequestQueueSingleton.getInstance(MapsActivity.this).getRequestQueue().add(jsonObjectRequest);
    }

    private void showSelectLocationDialog(JSONObject response) {
        String name = "";
        final List<String> placeId = new ArrayList<>();
        AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
        builderSingle.setTitle("Select One Place:-");

        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(MapsActivity.this, android.R.layout.select_dialog_singlechoice);
        try {
            JSONArray results = response.getJSONArray("predictions");
            for (int i = 0; i < results.length(); i++) {

                JSONObject first = results.getJSONObject(i);
                name = first.getString("description");
                arrayAdapter.add(name);

                placeId.add(first.getString("place_id"));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final String finalName = name;
        builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String url = getString(R.string.get_place_url, placeId.get(which), getString(R.string.google_maps_key));
                JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                        new Response.Listener<JSONObject>() {
                            @Override
                            public void onResponse(JSONObject response) {
                                try {
                                    JSONObject result = response.getJSONObject("result");
                                    JSONObject location = result.getJSONObject("geometry").getJSONObject("location");
                                    double lat = location.getDouble("lat");
                                    double lng = location.getDouble("lng");
                                    setCamera(new LatLng(lat, lng));
                                    showMarkOnMap(mMap, lat, lng, finalName);

                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d(TAG, "onErrorResponse: ");
                    }
                });
                RequestQueueSingleton.getInstance(MapsActivity.this).getRequestQueue().add(jsonObjectRequest);
            }
        });
        builderSingle.show();
    }

    public String getLocationNameFromJson(JSONObject location) {
        String name = "";
        try {
            JSONArray results = location.getJSONArray("results");
            JSONObject first = results.getJSONObject(0);
            JSONArray components = first.getJSONArray("address_components");
            JSONObject route = components.getJSONObject(1);
            name = route.getString("long_name");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return name;
    }

    public String getLocationFromJson(JSONObject location) throws JSONException {
        JSONArray results = location.getJSONArray("results");
        JSONObject first = results.getJSONObject(0);

        return first.getString("formatted_address");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        //Request location updates:
                        getCurrentLocation();
                    }
                }
            }
        }
    }

    public void getNearbyLocation(GoogleMap googleMap, JSONObject locationCenter) {
        JSONArray results = null;
        JSONObject location;
        String name = "";
        double latitude = 0;
        double longitude = 0;

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        try {
            results = locationCenter.getJSONArray("results");
            final int MAP_BOUND_PADDING = 180;
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (int i = 0; i < results.length(); i++) {
                location = results.getJSONObject(i);
                name = location.getString("name");
                if (!name.isEmpty()) {
                    location = location.getJSONObject("geometry");
                    location = location.getJSONObject("location");
                    latitude = location.getDouble("lat");
                    longitude = location.getDouble("lng");
                    LatLng latLng = new LatLng(latitude, longitude);
                    builder.include(latLng);

                    showMarkOnMap(googleMap, latitude, longitude, name);
                }
            }

            LatLngBounds bounds = builder.build();
            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, MAP_BOUND_PADDING);
            mMap.animateCamera(cu);
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        getCurrentLocation();
    }

    public void showMarkOnMap(GoogleMap googleMap, double lat, double lon, String name) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng place = new LatLng(lat, lon);
        mMap.addMarker(new MarkerOptions().position(place).title(name));
        showInSpinnerList(name);
        markers.add(new LatLng(lat, lon));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(place));
    }

    private void showInSpinnerList(String name) {
        adapter.add(name);
    }

    public void viewLocationText(JSONObject response) {
        TextView locationText = findViewById(R.id.locationTextView);
        try {
            locationText.setText(getLocationFromJson(response));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
