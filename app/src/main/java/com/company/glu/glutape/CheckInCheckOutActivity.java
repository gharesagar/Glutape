package com.company.glu.glutape;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import connectivity.ConnectivityReceiver;
import services.ApiConstants;
import services.AppController;
import services.SessionManager;

public class CheckInCheckOutActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {

    Button btCheckIn, btCheckOut;
    SessionManager sessionManager;
    HashMap<String, String> checkInData, empData;
    String status;
    ProgressDialog progress;
    String remarks, empId, reason;
    private Spinner spReason;
    private String[] reasonList = {"Select reason", "Order taken", "Complaint", "Collection", "Unproductive"};
    private View view;
    private EditText edtRemarks;
    private boolean isConnected;

    Button btLogout;


    private void checkConnection() {
        isConnected = ConnectivityReceiver.isConnected(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_check_out);

        initViews();

        sessionManager = new SessionManager(CheckInCheckOutActivity.this);
        checkInData = new HashMap<>();
        empData = new HashMap<>();
        checkInData = sessionManager.getCheckIndetails();
        status = checkInData.get(SessionManager.KEY_STATUS);

        empData = sessionManager.getEmpdetails();
        empId = empData.get(SessionManager.KEY_EMPID);

        if (status.equalsIgnoreCase("CheckIN")) {
            btCheckIn.setVisibility(View.GONE);
            btCheckOut.setVisibility(View.VISIBLE);
        } else {
            //not check In
            btCheckIn.setVisibility(View.VISIBLE);
            btCheckOut.setVisibility(View.GONE);
        }

        loadResonList();

        btCheckIn.setOnClickListener(this);
        btCheckOut.setOnClickListener(this);
        spReason.setOnItemSelectedListener(this);

        btLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sessionManager.logoutUser();
                Intent i = new Intent(CheckInCheckOutActivity.this, LoginActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
            }
        });
    }

    private void loadResonList() {
        ArrayAdapter<String> aa = new ArrayAdapter<String>(CheckInCheckOutActivity.this, android.R.layout.simple_spinner_item, reasonList);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spReason.setAdapter(aa);
    }

    private void initViews() {
        btCheckIn = findViewById(R.id.btCheckIn);
        btCheckOut = findViewById(R.id.btCheckOut);
        spReason = findViewById(R.id.spReason);
        view = findViewById(R.id.view);
        edtRemarks = findViewById(R.id.edtRemarks);
        btLogout=findViewById(R.id.btLogout);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btCheckIn:
                Intent intent = new Intent(CheckInCheckOutActivity.this, SelectDisributerDealerActivity.class);
                //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;

            case R.id.btCheckOut:

                openCheckoutDialog();
                break;
        }
    }

    private void openCheckoutDialog() {

        if(view.getVisibility()==View.GONE){
            view.setVisibility(View.VISIBLE);
        }else {

            reason = spReason.getSelectedItem().toString().trim();
            remarks = edtRemarks.getText().toString().trim();

            if (edtRemarks.getVisibility() == View.GONE) {
                remarks = "N.A";
            }else if(remarks.isEmpty()){
                Toast.makeText(this, "add remarks", Toast.LENGTH_SHORT).show();
            }

            if (reason.equalsIgnoreCase("Select reason")) {
                Toast.makeText(this, "Select reason", Toast.LENGTH_SHORT).show();
            } else {

                checkConnection();
                if (isConnected) {

                    checkOut();
                } else {

                    Toast.makeText(CheckInCheckOutActivity.this, "No internet!!", Toast.LENGTH_SHORT).show();

                }
            }

        }

    /*    final View view = LayoutInflater.from(this).inflate(R.layout.remark_layout, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(CheckInCheckOutActivity.this).create();
        alertDialog.setCancelable(false);
        final EditText edtRemarks =  view.findViewById(R.id.edtRemarks);
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                remarks=edtRemarks.getText().toString().trim();

                if(remarks.isEmpty()&& remarks.matches("")){
                    Toast.makeText(CheckInCheckOutActivity.this, "Enter remarks", Toast.LENGTH_SHORT).show();
                }else {

                    checkOut();
                }
            }
        });


        alertDialog.setView(view);
        alertDialog.show();*/
    }

    private void checkOut() {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.CHECKOUT_DISTRIBUTOR, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    boolean error = jsonObject.getBoolean("Error");
                    String msg = jsonObject.getString("Message");

                    if (error) {
                        progress.dismiss();
                        Toast.makeText(CheckInCheckOutActivity.this, msg, Toast.LENGTH_SHORT).show();

                    } else {
                        progress.dismiss();
                        //json response
                       /* String checkInId = jsonObject.getString("Date");
                        String time=jsonObject.getString("Time");
                        String remark = jsonObject.getString("Remark");
                        int empUsername=jsonObject.getInt("EmpID");
                        String date=jsonObject.getString("Date");*/
                        String status = jsonObject.getString("Status");

                        sessionManager.checkInDistributorSession("", "", "", "", status);
                        btCheckIn.setVisibility(View.VISIBLE);
                        btCheckOut.setVisibility(View.GONE);
                        view.setVisibility(View.GONE);

                        Toast.makeText(CheckInCheckOutActivity.this, msg, Toast.LENGTH_SHORT).show();
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                progress.dismiss();

                AlertDialog.Builder al = new AlertDialog.Builder(CheckInCheckOutActivity.this);
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
                params.put("CheckOutDate", "03/29/2019");
                params.put("CheckoutTime", "12:00 PM");
                params.put("Remark", remarks);
                params.put("EmpID", empId);
                params.put("Reason", reason);
                return params;
            }
        };


        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
        progress = ProgressDialog.show(this, "CHECK OUT",
                "Please wait..", true);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        if (reasonList[1].equalsIgnoreCase(spReason.getSelectedItem().toString())) {
            edtRemarks.setVisibility(View.VISIBLE);
        } else {
            edtRemarks.setVisibility(View.GONE);
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        view.setVisibility(View.GONE);
        edtRemarks.setVisibility(View.GONE);
    }
}
