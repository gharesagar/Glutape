package com.company.glu.glutape;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkError;
import com.android.volley.NoConnectionError;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.ServerError;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import connectivity.ConnectivityReceiver;
import in.galaxyofandroid.spinerdialog.OnSpinerItemClick;
import in.galaxyofandroid.spinerdialog.SpinnerDialog;
import model.Dealer;
import model.Distributors;
import services.ApiConstants;
import services.AppController;
import services.GpsUtils;
import services.SessionManager;

public class CheckInDealerActivity extends AppCompatActivity implements View.OnClickListener, ConnectivityReceiver.ConnectivityReceiverListener {

    public static final String TAG = "CheckInDealerActivity";
    TextView tvDistributer, tvDealer;
    Button btCheckInDealer;
    ImageView imgDrop1, imgDrop2;

    private SpinnerDialog spinnerDialog, spinnerDialog1;
    private ArrayList<Distributors> distributorsList;
    private ArrayList<String> distributorsNamesList;

    private ArrayList<Dealer> dealersList;
    private ArrayList<String> dealersNameList;

    int distributorId, dealerId;
    boolean isConnected;
    String dealerName, distributorName;
    ProgressDialog progress;

    SessionManager sessionManager;
    HashMap<String, String> empData;
    String empUsername, empId;

    ConnectivityReceiver connectivityReceiver;

    String locationAddress;


    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private boolean isGPS = false;
    double longitude, latitude;


    private void checkConnection() {
        isConnected = ConnectivityReceiver.isConnected(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_dealer);

        initViews();

        sessionManager = new SessionManager(this);
        empData = new HashMap<>();
        empData = sessionManager.getEmpdetails();
        empUsername = empData.get(SessionManager.KEY_USERNAME);
        empId = empData.get(SessionManager.KEY_EMPID);

        distributorsList = new ArrayList<>();
        distributorsNamesList = new ArrayList<>();
        dealersList = new ArrayList<>();
        dealersNameList = new ArrayList<>();

        connectivityReceiver = new ConnectivityReceiver();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(CheckInDealerActivity.this);
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10 * 1000); // 10 seconds
        locationRequest.setFastestInterval(5 * 1000); // 5 seconds

        new GpsUtils(CheckInDealerActivity.this).turnGPSOn(new GpsUtils.onGpsListener() {
            @Override
            public void gpsStatus(boolean isGPSEnable) {
                // turn on GPS
                isGPS = isGPSEnable;
            }
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        Log.e(TAG, "latitude" + latitude + " longitude" + longitude);

                        Geocoder geocoder = new Geocoder(CheckInDealerActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                            locationAddress = addresses.get(0).getAddressLine(0);

                            checkIn();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };


        tvDistributer.setOnClickListener(this);
        tvDealer.setOnClickListener(this);
        imgDrop1.setOnClickListener(this);
        imgDrop2.setOnClickListener(this);
        btCheckInDealer.setOnClickListener(this);


        spinnerDialog = new SpinnerDialog(CheckInDealerActivity.this, distributorsNamesList, "Select distributor");
        spinnerDialog.bindOnSpinerListener(new OnSpinerItemClick() {
            @Override
            public void onClick(String item, int pos) {
                distributorId = getDistributorId(pos);
                tvDistributer.setText(item);

                //load dealer using distributor Id
                loadDealer(distributorId);
            }
        });

        spinnerDialog1 = new SpinnerDialog(CheckInDealerActivity.this, dealersNameList, "Select dealer");
        spinnerDialog1.bindOnSpinerListener(new OnSpinerItemClick() {
            @Override
            public void onClick(String item, int pos) {
                dealerId = getDealerId(pos);
                tvDealer.setText(item);
            }
        });

    }

    private int getDistributorId(int pos) {
        return distributorsList.get(pos).getDistributerId();
    }

    private int getDealerId(int pos) {
        return dealersList.get(pos).getDealerId();
    }

    private void loadDealer(final int distributorId) {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.FETCH_DEALER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.e(TAG, response);

                    JSONObject object = new JSONObject(response);
                    boolean error = object.getBoolean("Error");

                    if (error) {

                        Toast.makeText(CheckInDealerActivity.this, "No delar found with selected distributor", Toast.LENGTH_SHORT).show();

                    } else {

                        if (dealersNameList.size() > 0) {
                            dealersNameList.clear();
                        }

                        if (dealersList.size() > 0) {
                            dealersList.clear();
                        }

                        JSONArray jsonArray = object.getJSONArray("DFdata");
                        for (int i = 0; i < jsonArray.length(); i++) {

                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            int id = jsonObject.getInt("ID");
                            String dealerName = jsonObject.getString("DealerName");
                            String contactNo = jsonObject.getString("ContactNo");
                            String emailID = jsonObject.getString("EmailID");
                            String address = jsonObject.getString("Address");

                            Dealer dealer = new Dealer(id, dealerName, contactNo, emailID, address);
                            dealersList.add(dealer);
                            dealersNameList.add(dealerName);
                        }

                    }

                } catch (JSONException e) {
                    Log.e(TAG, e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e(TAG, "loadDealer " + error.toString());
                AlertDialog.Builder al = new AlertDialog.Builder(CheckInDealerActivity.this);
                String mesaage = null;
                if (error instanceof NetworkError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof ServerError) {
                    mesaage = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof AuthFailureError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof NoConnectionError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof TimeoutError) {
                    mesaage = "Connection TimeOut! Please check your internet connection.";
                } else if (error instanceof ParseError) {
                    mesaage = "Indicates that the server response could not be parsed.";
                } else {
                    mesaage = "Something went wrong. Try later";
                }

                al.setTitle("Error...");
                al.setMessage(mesaage);
                al.show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> data = new HashMap<>();
                data.put("id", String.valueOf(distributorId));
                return data;
            }
        };

        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
    }

    private void loadDistributors() {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.FETCH_DISTRIBUTOR, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.e(TAG, response);

                    JSONObject object = new JSONObject(response);
                    boolean error = object.getBoolean("Error");

                    if (error) {

                        Toast.makeText(CheckInDealerActivity.this, "No data", Toast.LENGTH_SHORT).show();

                    } else {

                        if (distributorsList.size() > 0) {
                            distributorsList.clear();
                        }
                        if (distributorsNamesList.size() > 0) {
                            distributorsNamesList.clear();
                        }

                        JSONArray jsonArray = object.getJSONArray("DFdata");
                        for (int i = 0; i < jsonArray.length(); i++) {

                            JSONObject jsonObject = jsonArray.getJSONObject(i);

                            int id = jsonObject.getInt("ID");
                            String distributorName = jsonObject.getString("DistributorName");
                            String contactNo = jsonObject.getString("ContactNO");
                            String emailID = jsonObject.getString("EmailID");
                            String address = jsonObject.getString("Address");

                            Distributors distributors = new Distributors(id, distributorName, contactNo, emailID, address);
                            distributorsList.add(distributors);
                            distributorsNamesList.add(distributorName);
                        }
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    Log.e(TAG, e.getMessage());
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                Log.e(TAG, "loadDistributors  " + error.toString());

                AlertDialog.Builder al = new AlertDialog.Builder(CheckInDealerActivity.this);
                String mesaage = null;
                if (error instanceof NetworkError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof ServerError) {
                    mesaage = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof AuthFailureError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof NoConnectionError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof TimeoutError) {
                    mesaage = "Connection TimeOut! Please check your internet connection.";
                } else if (error instanceof ParseError) {
                    mesaage = "Indicates that the server response could not be parsed.";
                } else {
                    mesaage = "Something went wrong. Try later";
                }

                al.setTitle("Error...");
                al.setMessage(mesaage);
                al.show();
            }
        }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                HashMap<String, String> data = new HashMap<>();
                data.put("id", empUsername);
                return data;
            }
        };

        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
    }

    private void initViews() {
        tvDistributer = findViewById(R.id.tvDistributer);
        tvDealer = findViewById(R.id.tvDealer);
        btCheckInDealer = findViewById(R.id.btCheckInDealer);
        imgDrop1 = findViewById(R.id.imgDrop1);
        imgDrop2 = findViewById(R.id.imgDrop2);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {

            case R.id.tvDistributer:
                spinnerDialog.showSpinerDialog();

                break;
            case R.id.tvDealer:
                spinnerDialog1.showSpinerDialog();

                break;
            case R.id.btCheckInDealer:
                checkInDealer();
                break;

            case R.id.imgDrop1:
                spinnerDialog.showSpinerDialog();

                break;
            case R.id.imgDrop2:
                spinnerDialog1.showSpinerDialog();

                break;
        }
    }


    private void checkInDealer() {
        dealerName = tvDealer.getText().toString().trim();
        distributorName = tvDistributer.getText().toString().trim();

        if (distributorName.equalsIgnoreCase("Select Distributer")) {
            Toast.makeText(this, "Select Distributer", Toast.LENGTH_SHORT).show();
        } else if (dealerName.equalsIgnoreCase("Select Dealer")) {
            Toast.makeText(this, "Select dealer", Toast.LENGTH_SHORT).show();
        } else {
            checkConnection();
            if (isConnected) {

                if (!isGPS) {
                    Toast.makeText(CheckInDealerActivity.this, "Please turn on GPS", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ContextCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {


                        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                                ApiConstants.LOCATION_REQUEST);
                    }else {
                        getDeviceLocation();
                    }
                }
                else {
                    getDeviceLocation();
                }

            } else {
                Toast.makeText(this, "No internet!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void getDeviceLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(CheckInDealerActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();

                            Geocoder geocoder = new Geocoder(CheckInDealerActivity.this, Locale.getDefault());
                            try {
                                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                                locationAddress = addresses.get(0).getAddressLine(0);

                                checkIn();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        } else {

                            //Location is null , request location
                            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, e.toString());
                    }
                });
    }


    private void checkIn() {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.CHEKIN_DEALER, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("Error");
                    String msg = jsonObject.getString("Message");

                    if (error) {
                        progress.dismiss();
                        Toast.makeText(CheckInDealerActivity.this, msg, Toast.LENGTH_SHORT).show();

                    } else {
                        progress.dismiss();
                        //json response
                        String checkInId = jsonObject.getString("CheckInID");
                        String dealerID = jsonObject.getString("DealerID");
                        int empId = jsonObject.getInt("EmpID");
                        String location = jsonObject.getString("Location");
                        String time = jsonObject.getString("Time");
                        String date = jsonObject.getString("Date");
                        String status = jsonObject.getString("Status");


                        sessionManager.checkInDealerSession(checkInId, dealerID, location, time, date, status);
                        Toast.makeText(CheckInDealerActivity.this, msg, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CheckInDealerActivity.this, CheckInCheckOutActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();

                AlertDialog.Builder al = new AlertDialog.Builder(CheckInDealerActivity.this);
                String mesaage = null;
                if (error instanceof NetworkError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof ServerError) {
                    mesaage = "The server could not be found. Please try again after some time!!";
                } else if (error instanceof AuthFailureError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof NoConnectionError) {
                    mesaage = "Cannot connect to Internet. Please check your connection!";
                } else if (error instanceof TimeoutError) {
                    mesaage = "Connection TimeOut! Please check your internet connection.";
                } else if (error instanceof ParseError) {
                    mesaage = "Indicates that the server response could not be parsed.";
                } else {
                    mesaage = "Something went wrong. Try later";
                }

                al.setTitle("Error...");
                al.setMessage(mesaage);
                al.show();
            }
        }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("Longitude", String.valueOf(longitude));
                params.put("Latitude", String.valueOf(latitude));
                params.put("Location", locationAddress);
                params.put("Date", "06-12-2019");
                params.put("Time", "10:00 Am");
                params.put("DistributorID", String.valueOf(distributorId));
                params.put("DelarID", String.valueOf(dealerId));
                params.put("EmpID", empId);
                params.put("Status", "CheckIn");

                Log.e(TAG, locationAddress);

                return params;
            }

        };


        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
        progress = ProgressDialog.show(this, "CHECK IN",
                "Please wait..", true);
    }


    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(connectivityReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }

        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onResume() {
        super.onResume();
        // register connection status listener
        AppController.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

        if (isConnected) {
            loadDistributors();
        } else {
            Toast.makeText(this, "NO INTERNET", Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ApiConstants.LOCATION_REQUEST: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ActivityCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
                            .addOnSuccessListener(CheckInDealerActivity.this, new OnSuccessListener<Location>() {
                                @Override
                                public void onSuccess(Location location) {
                                    // Got last known location. In some rare situations this can be null.
                                    if (location != null) {
                                        longitude = location.getLongitude();
                                        latitude = location.getLatitude();

                                        Geocoder geocoder = new Geocoder(CheckInDealerActivity.this, Locale.getDefault());
                                        try {
                                            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

                                            locationAddress = addresses.get(0).getAddressLine(0);

                                            checkIn();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    } else {

                                        //Location is null , request location
                                        if (ActivityCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                                ActivityCompat.checkSelfPermission(CheckInDealerActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                            // TODO: Consider calling
                                            //    ActivityCompat#requestPermissions
                                            // here to request the missing permissions, and then overriding
                                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                            //                                          int[] grantResults)
                                            // to handle the case where the user grants the permission. See the documentation
                                            // for ActivityCompat#requestPermissions for more details.
                                            return;
                                        }
                                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
                                    }
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.e(TAG, e.toString());
                                }
                            });

                } else {
                    Toast.makeText(CheckInDealerActivity.this, "Permission denied. Please allow go to settings", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ApiConstants.GPS_REQUEST) {
                isGPS = true; // flag maintain before get location
            }
        }
    }

}

