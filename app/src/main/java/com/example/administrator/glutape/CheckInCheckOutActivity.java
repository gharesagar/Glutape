package com.example.administrator.glutape;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
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

import services.ApiConstants;
import services.AppController;
import services.SessionManager;

public class CheckInCheckOutActivity extends AppCompatActivity implements View.OnClickListener {

    Button btCheckIn,btCheckOut;
    SessionManager sessionManager;
    HashMap<String,String> checkInData,empData;
    String status;
    ProgressDialog progress;
    String remarks;
    String empId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_in_check_out);

        btCheckIn=findViewById(R.id.btCheckIn);
        btCheckOut=findViewById(R.id.btCheckOut);

        sessionManager=new SessionManager(CheckInCheckOutActivity.this);
        checkInData=new HashMap<>();
        empData=new HashMap<>();
        checkInData=sessionManager.getCheckIndetails();
        status=checkInData.get(SessionManager.KEY_STATUS);

        empData=sessionManager.getEmpdetails();
        empId=empData.get(SessionManager.KEY_EMPID);

        if(status.equalsIgnoreCase("CheckIN")){
            btCheckIn.setVisibility(View.GONE);
            btCheckOut.setVisibility(View.VISIBLE);
        }else {
            btCheckIn.setVisibility(View.VISIBLE);
            btCheckOut.setVisibility(View.VISIBLE);
        }

        btCheckIn.setOnClickListener(this);
        btCheckOut.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btCheckIn:
                Intent intent=new Intent(CheckInCheckOutActivity.this,SelectDisributerDealerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;

            case R.id.btCheckOut:

                openCheckoutDialog();
                break;
        }
    }

    private void openCheckoutDialog() {

        final View view = LayoutInflater.from(this).inflate(R.layout.remark_layout, null);
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
        alertDialog.show();
    }

    private void checkOut() {

        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.CHECKOUT_DISTRIBUTOR, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    Boolean error = jsonObject.getBoolean("Error");
                    String msg = jsonObject.getString("Message");

                    if (error) {
                        progress.dismiss();
                        Toast.makeText(CheckInCheckOutActivity.this,msg,Toast.LENGTH_SHORT).show();

                    } else {
                        progress.dismiss();
                        //json response
                       /* String checkInId = jsonObject.getString("Date");
                        String time=jsonObject.getString("Time");
                        String remark = jsonObject.getString("Remark");
                        int empId=jsonObject.getInt("EmpID");
                        String date=jsonObject.getString("Date");*/
                        String status=jsonObject.getString("Status");


                        sessionManager.checkInDistributorSession("","","","",status);
                        btCheckIn.setVisibility(View.VISIBLE);
                        btCheckOut.setVisibility(View.GONE);
                        Toast.makeText(CheckInCheckOutActivity.this,msg,Toast.LENGTH_SHORT).show();
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
                Map<String, String> loginParams = new HashMap<>();
                loginParams.put("CheckOutDate", "12-03-2019");
                loginParams.put("CheckoutTime", "12:00 PM");
                loginParams.put("Remark", remarks);
                loginParams.put("EmpID", empId);
                return loginParams;
            }

        };


        int MY_SOCKET_TIMEOUT_MS = 30000;
        stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                MY_SOCKET_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        AppController.getInstance().addToRequestQueue(stringRequest);
        progress = ProgressDialog.show(this, "CHECKOUT",
                "Please wait..", true);
    }
}
