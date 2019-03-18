package com.example.administrator.glutape;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

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
import model.Distributors;
import services.ApiConstants;
import services.AppController;
import services.LocationAddress;
import services.LocationTrack;
import services.SessionManager;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;

public class CheckInDistributerActivity extends AppCompatActivity implements View.OnClickListener, ConnectivityReceiver.ConnectivityReceiverListener {

    public static final String TAG="CheckInDistributer";
    TextView tvDistributer;
    Button btCheckInDist;
    private SpinnerDialog spinnerDialog;
    private ArrayList<Distributors> distributorsList;
    private ArrayList<String> distributorsNamesList;
    private int distributorId;
    private ImageView imgDrop1;

    String distributorName;
    boolean isConnected;
    ProgressDialog progress;
    SessionManager sessionManager;
    HashMap<String, String> empData;
    String empId;

    private GoogleApiClient googleApiClient;
    final static int REQUEST_LOCATION = 199;
    ConnectivityReceiver connectivityReceiver;
    IntentFilter intentFilter;

    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList();
    private ArrayList<String> permissions = new ArrayList();
    private final static int ALL_PERMISSIONS_RESULT = 101;
    LocationTrack locationTrack;
    String locationAddress;


    private void checkConnection() {
        isConnected = ConnectivityReceiver.isConnected(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_distributer);

        tvDistributer = findViewById(R.id.tvDistributer);
        btCheckInDist = findViewById(R.id.btCheckInDist);
        imgDrop1 = findViewById(R.id.imgDrop1);

        sessionManager = new SessionManager(this);
        empData = new HashMap<>();
        empData = sessionManager.getEmpdetails();
        empId = empData.get(SessionManager.KEY_EMPID);

        permissions.add(ACCESS_FINE_LOCATION);
        permissions.add(ACCESS_COARSE_LOCATION);
        permissionsToRequest = findUnAskedPermissions(permissions);
        //get the permissions we have asked for before but are not granted..
        //we will store this in a global list to access later.

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (permissionsToRequest.size() > 0)
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
        }

        locationTrack = new LocationTrack(CheckInDistributerActivity.this);

        loadDistributors();

        btCheckInDist.setOnClickListener(this);
        imgDrop1.setOnClickListener(this);
        tvDistributer.setOnClickListener(this);


        spinnerDialog = new SpinnerDialog(CheckInDistributerActivity.this, distributorsNamesList, "Select distributor");
        spinnerDialog.bindOnSpinerListener(new OnSpinerItemClick() {
            @Override
            public void onClick(String item, int pos) {
                distributorId = getDistributorId(pos);
                tvDistributer.setText(item);
            }
        });

        setConnectivityBroadcastReceiver();
    }



    private void loadDistributors() {
        distributorsNamesList = new ArrayList<>();
        distributorsList = new ArrayList<>();

        final StringRequest stringRequest = new StringRequest(Request.Method.GET, ApiConstants.FETCH_DISTRIBUTOR, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONArray jsonArray = new JSONArray(response);

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


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

                AlertDialog.Builder al = new AlertDialog.Builder(CheckInDistributerActivity.this);
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
        });

        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);

    }

    private int getDistributorId(int pos) {
        return distributorsList.get(pos).getDistributerId();
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {
            case R.id.tvDistributer:
                spinnerDialog.showSpinerDialog();
                break;

            case R.id.imgDrop1:
                spinnerDialog.showSpinerDialog();
                break;

            case R.id.btCheckInDist:

                checkInDistributor();

                break;

        }
    }

    private void checkInDistributor() {
        distributorName = tvDistributer.getText().toString().trim();

        if (distributorName.equalsIgnoreCase("Select Distributer")) {
            Toast.makeText(this, "Select Distributer", Toast.LENGTH_SHORT).show();
        } else {
            checkConnection();
            if (isConnected) {

                if (locationTrack.canGetLocation()) {
                    double longitude = locationTrack.getLongitude();
                    double latitude = locationTrack.getLatitude();

                    Toast.makeText(CheckInDistributerActivity.this, "Longitude:" + Double.toString(longitude) + "\nLatitude:" + Double.toString(latitude), Toast.LENGTH_SHORT).show();

                    LocationAddress locationAddress = new LocationAddress();
                    locationAddress.getAddressFromLocation(latitude, longitude,
                            CheckInDistributerActivity.this, new GeocoderHandler());
                    checkIn();
                }else {
                    enableLoc();

                }

            } else {
                Toast.makeText(this, "No internet!!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            //tvAddress.setText(locationAddress);
        }
    }

    private void checkIn() {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.CHECKIN_DISTRIBUTOR, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    Boolean error = jsonObject.getBoolean("Error");
                    String msg = jsonObject.getString("Message");

                    if (error) {
                        progress.dismiss();
                        Toast.makeText(CheckInDistributerActivity.this, msg, Toast.LENGTH_SHORT).show();

                    } else {
                        progress.dismiss();
                        //json response
                        String checkInId = jsonObject.getString("CheckINID");
                        String location = jsonObject.getString("Location");
                        int empId = jsonObject.getInt("EmpID");
                        String time = jsonObject.getString("Time");
                        String date = jsonObject.getString("Date");
                        String status = jsonObject.getString("Status");


                        sessionManager.checkInDistributorSession(checkInId, location, time, date, status);
                        Toast.makeText(CheckInDistributerActivity.this, msg, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(CheckInDistributerActivity.this, CheckInCheckOutActivity.class);
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

                AlertDialog.Builder al = new AlertDialog.Builder(CheckInDistributerActivity.this);
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
                params.put("Location", locationAddress);
                params.put("Date", "12-03-2019");
                params.put("Time", "6:00 PM");
                params.put("DistributorID", String.valueOf(distributorId));
                params.put("EmpID", empId);
                params.put("Status", "CheckIn");
                return params;
            }

        };


        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
        progress = ProgressDialog.show(this, "LOGIN",
                "Please wait..", true);
    }

    private void setConnectivityBroadcastReceiver() {
        //create intent filter instance
        intentFilter = new IntentFilter();
        // Add network connectivity change action.
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
        // Set broadcast receiver priority.
        intentFilter.setPriority(100);
        connectivityReceiver = new ConnectivityReceiver();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            registerReceiver(connectivityReceiver, intentFilter);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (this.connectivityReceiver != null) {
            unregisterReceiver(connectivityReceiver);
        }
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

        } else {

        }
    }

    private void enableLoc() {

        if (googleApiClient == null) {
            googleApiClient = new GoogleApiClient.Builder(CheckInDistributerActivity.this)
                    .addApi(LocationServices.API)
                    .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                        @Override
                        public void onConnected(Bundle bundle) {

                            LocationRequest locationRequest = LocationRequest.create();
                            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
                            locationRequest.setInterval(30 * 1000);
                            locationRequest.setFastestInterval(5 * 1000);
                            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                                    .addLocationRequest(locationRequest);

                            builder.setAlwaysShow(true);

                            PendingResult<LocationSettingsResult> result =
                                    LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());

                            result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
                                @Override
                                public void onResult(LocationSettingsResult result) {
                                    final Status status = result.getStatus();
                                    switch (status.getStatusCode()) {

                                        case LocationSettingsStatusCodes.SUCCESS:

                                            // NO need to show the dialog;

                                            break;
                                        case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                                            try {
                                                // Show the dialog by calling startResolutionForResult(),
                                                // and check the result in onActivityResult().
                                                status.startResolutionForResult(CheckInDistributerActivity.this, REQUEST_LOCATION);
                                            } catch (IntentSender.SendIntentException e) {
                                                // Ignore the error.
                                            }
                                            break;

                                        case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                                            // Location settings are unavailable so not possible to show any dialog now
                                            break;
                                    }
                                }
                            });
                        }

                        @Override
                        public void onConnectionSuspended(int i) {
                            googleApiClient.connect();
                        }
                    })
                    .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                        @Override
                        public void onConnectionFailed(ConnectionResult connectionResult) {

                            Log.e(TAG, "Location error " + connectionResult.getErrorCode());
                        }
                    }).build();

            googleApiClient.connect();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {

            if (resultCode == RESULT_OK) {

                Toast.makeText(getApplicationContext(), "GPS enabled", Toast.LENGTH_LONG).show();
            } else {

                Toast.makeText(getApplicationContext(), "GPS is not enabled", Toast.LENGTH_LONG).show();
            }
        }
    }

    private ArrayList findUnAskedPermissions(ArrayList<String> wanted) {
        ArrayList result = new ArrayList();

        for (String perm : wanted) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }
        return result;
    }

    private boolean hasPermission(String permission) {
        if (canMakeSmores()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED);
            }
        }
        return true;
    }

    private boolean canMakeSmores() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }


    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {

            case ALL_PERMISSIONS_RESULT:
                for (String perms : permissionsToRequest) {
                    if (!hasPermission(perms)) {
                        permissionsRejected.add(perms);
                    }
                }

                if (permissionsRejected.size() > 0) {


                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            showMessageOKCancel("These permissions are mandatory for the application. Please allow access.",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    });
                            return;
                        }
                    }

                }
                break;
        }
    }

    private void showMessageOKCancel(String message, DialogInterface.OnClickListener okListener) {
        new android.support.v7.app.AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }

    @Override
    public void onPause() {
        super.onPause();
        locationTrack.stopListener();

       /* if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }*/
    }
}
