package com.company.glu.glutape;

import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SelectDisributerDealerActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int PERMISSION_REQUEST_CODE =11 ;
    Button btDistributer,btDealer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_disributer_dealer);

        btDistributer=findViewById(R.id.btDistributer);
        btDealer=findViewById(R.id.btDealer);

        btDistributer.setOnClickListener(this);
        btDealer.setOnClickListener(this);

    }


    @Override
    public void onClick(View v) {

        switch (v.getId()){

            case R.id.btDistributer:

                Intent intent=new Intent(SelectDisributerDealerActivity.this,CheckInDistributerActivity.class);
               // intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;
            case R.id.btDealer:

                Intent intent1=new Intent(SelectDisributerDealerActivity.this,CheckInDealerActivity.class);
              //  intent1.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent1);
                break;
        }
    }
}
