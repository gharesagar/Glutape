package com.example.administrator.glutape;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import connectivity.ConnectivityReceiver;
import services.ApiConstants;
import services.AppController;
import services.SessionManager;

import static com.google.android.gms.common.internal.safeparcel.SafeParcelable.NULL;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    Button btLogin;
    EditText edtEmpId, edtPassword;
    String empId, password;
    private boolean isConnected;
    ProgressDialog progress;
    SessionManager sessionManager;

    private void checkConnection() {
        isConnected = ConnectivityReceiver.isConnected(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        btLogin = findViewById(R.id.btLogin);
        edtEmpId = findViewById(R.id.edtEmpId);
        edtPassword = findViewById(R.id.edtPassword);

        sessionManager = new SessionManager(LoginActivity.this);

        btLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                empId = edtEmpId.getText().toString().trim();
                password = edtPassword.getText().toString().trim();

                if (empId.isEmpty() && empId.matches("")) {
                    edtEmpId.setError("Please enter emp id");
                } else if (password.isEmpty() && password.matches("")) {
                    edtPassword.setError("Please enter password");
                } else {
                    checkConnection();
                    if (isConnected) {

                        InputMethodManager inputManager = (InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(),
                                InputMethodManager.HIDE_NOT_ALWAYS);
                        login();
                    } else {

                   /*     Snackbar snackbar = Snackbar.make(constraintLayout, "No internet connection", Snackbar.LENGTH_LONG);
                        View snackbarView = snackbar.getView();
                        TextView tv =snackbarView.findViewById(android.support.design.R.id.snackbar_text);
                        tv.setTextColor(Color.RED);
                        tv.setTextSize( 16 );
                        Typeface tp = Typeface.createFromAsset(getAssets(), "cooperhewittmediumd.ttf");
                        tv.setTypeface(tp);
                        tv.setTextColor(Color.RED);
                        snackbar.show();*/
                        Toast.makeText(LoginActivity.this, "No internet!!", Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });
    }

    private void login() {
        final StringRequest stringRequest = new StringRequest(Request.Method.POST, ApiConstants.LOGIN, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {

                    JSONObject jsonObject = new JSONObject(response);
                    Boolean error = jsonObject.getBoolean("Error");
                    String msg = jsonObject.getString("Message");

                    if (error) {
                        progress.dismiss();
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();

                    } else {
                        progress.dismiss();
                        //json response
                        String userName = jsonObject.getString("UserName");
                        String password = jsonObject.getString("Password");
                        int empId = jsonObject.getInt("EmpID");

                        sessionManager.createLoginSession(userName, password, empId);
                        Toast.makeText(LoginActivity.this, msg, Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, CheckInCheckOutActivity.class);
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
                // parseVolleyError(error);
                if (error.getMessage() == NULL) {
                    Log.e(TAG, "Failed to retrieve data");
                } else {
                    Log.e(TAG, error.getMessage());
                }
                progress.dismiss();

                AlertDialog.Builder al = new AlertDialog.Builder(LoginActivity.this);
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
                loginParams.put("UserName", empId);
                loginParams.put("Password", password);
                return loginParams;
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

    public void parseVolleyError(VolleyError error) {
        try {
            // final String statusCode = String.valueOf(error.networkResponse.statusCode);
            String responseBody = new String(error.networkResponse.data, "utf-8");
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);
            String message = jsonMessage.getString("message");
            // Log.e(TAG,"statusCode ="+ statusCode+"  "+message);
            Log.e(TAG, message);
        } catch (JSONException e) {
        } catch (UnsupportedEncodingException errorr) {
        }
    }
}
