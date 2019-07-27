package services;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;


public class SessionManager {

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    Context context;

    private static final String TAG = "SessionManager";
    public static final String PREF_NAME = "GLUTAPE";
    public static final String IS_LOGIN = "IsLoggedIn";
    public static final String KEY_EMPID = "Emp_Id";
    public static final String KEY_PASSWORD = "Emp_Password";
    public static final String KEY_USERNAME="Username";

    public static final String KEY_CHECK_IN_ID="CheckInId";
    public static final String KEY_STATUS="Status";

    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);

    }


    public void logoutUser() {
        editor = pref.edit();

        editor.putBoolean(IS_LOGIN, false);
        editor.remove(KEY_EMPID);
        editor.remove(KEY_PASSWORD);
        editor.remove(KEY_EMPID);

        boolean isDataRemoved = editor.commit();

        if (isDataRemoved) {
            Log.e(TAG, "User Data removed");
        } else {
            Log.e(TAG, "User Data not removed");
        }
    }


    public void createLoginSession(String userName, String password,int empId) {
        editor = pref.edit();

        editor.putBoolean(IS_LOGIN, true);
        editor.putString(KEY_USERNAME,userName);
        editor.putInt(KEY_EMPID, empId);
        editor.putString(KEY_PASSWORD, password);

        boolean isDataInserted = editor.commit();

        if (isDataInserted) {
            Log.e(TAG, "Data inserted");
        } else {
            Log.e(TAG, "Data not inserted");
        }
    }


    public boolean isLoggedIn() {
        return pref.getBoolean(IS_LOGIN, false);
    }

    public HashMap<String, String> getEmpdetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_EMPID, String.valueOf(pref.getInt(KEY_EMPID,0)));
        user.put(KEY_PASSWORD, pref.getString(KEY_PASSWORD, "default"));
        user.put(KEY_USERNAME, pref.getString(KEY_USERNAME, "default"));

        return user;
    }


    public void checkInDistributorSession(String checkInId, String location, String time, String date, String status) {
        editor = pref.edit();

        editor.putString(KEY_STATUS,status);
        boolean isDataInserted = editor.commit();

        if (isDataInserted) {
            Log.e(TAG, "Data inserted");
        } else {
            Log.e(TAG, "Data not inserted");
        }
    }

    public HashMap<String, String> getCheckIndetails() {
        HashMap<String, String> user = new HashMap<>();
        user.put(KEY_STATUS, pref.getString(KEY_STATUS, "default"));

        return user;
    }


    public void checkInDealerSession(String checkInId, String dealerID, String location, String time, String date, String status) {
        editor = pref.edit();

        editor.putString(KEY_STATUS,status);
        boolean isDataInserted = editor.commit();

        if (isDataInserted) {
            Log.e(TAG, "Data inserted");
        } else {
            Log.e(TAG, "Data not inserted");
        }
    }
}
