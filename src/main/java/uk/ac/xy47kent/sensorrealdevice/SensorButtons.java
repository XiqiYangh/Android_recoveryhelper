package uk.ac.xy47kent.sensorrealdevice;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SensorButtons extends AppCompatActivity {
    String TAG = "SensorButtons";

    private Button buttona, buttonm, buttono, selftest, quickview;
    public View v;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor_buttons);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        buttona = (Button)findViewById(R.id.buttona);
        buttona.setOnClickListener(acceclicked);
        //Log.d(TAG, "stimulate accelerometer");

        buttonm = (Button) findViewById(R.id.buttonm);
        buttonm.setOnClickListener(magfclicked);
        //Log.d(TAG, "stimulate magnetic field");

        buttono = (Button) findViewById(R.id.buttono);
        buttono.setOnClickListener(orienclicked);

        selftest = (Button)findViewById(R.id.selftest);
        selftest.setOnClickListener(selftestclicked);

        quickview = (Button)findViewById(R.id.alldata);
        quickview.setOnClickListener(quickviewClicked);

    }

    private View.OnClickListener acceclicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(),AccelDataPlot.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener magfclicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(),MagfieldDataPlot.class);
            startActivity(intent);
        }
    };

    private View.OnClickListener orienclicked = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(),OrienDataPlot.class);
            startActivity(intent);
        }
    };


    private View.OnClickListener selftestclicked = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(),SelfTest.class);
            startActivity(intent);
        }
    };


    private View.OnClickListener quickviewClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent quickviewintent = new Intent(v.getContext(),AllDataPlot.class);
            startActivity(quickviewintent);
        }
    };

}
