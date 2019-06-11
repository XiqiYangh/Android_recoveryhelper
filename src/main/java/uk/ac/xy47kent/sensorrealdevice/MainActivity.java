package uk.ac.xy47kent.sensorrealdevice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.w3c.dom.Text;

import java.lang.reflect.Method;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView sensorNumber;
    private TextView sensorView;
    private SensorManager sensorManager;
    private Button buttondata;

    ImageView welcomePage;
    int welcomeFlag = 1;

    public int counterAnimation = 3;//calculate coundown timer for 3s

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sensorNumber = (TextView) findViewById(R.id.sensorNumber);
        sensorView = (TextView) findViewById(R.id.sensorView);
        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        List<Sensor> list = sensorManager.getSensorList(Sensor.TYPE_ALL);
        sensorNumber.append("Your smartphone has " + list.size() + " supported sensors");
        for (Sensor sen : list) {
            String message = sen.getName() + "\n";
            sensorView.append(message);

        }

        buttondata = (Button) findViewById(R.id.buttondata);
        buttondata.setOnClickListener(datacollected);

        welcomePage = (ImageView)findViewById(R.id.welcomePage);
        welcomePage.setOnClickListener(welcomeclicked);

        //CountDownTimer: for initialization and determinePosition
        new CountDownTimer(3000, 1000) { //count 3s ,time interval 1s
            @Override
            public void onTick(long millisUntilFinished) {
                //Set the welcome page visible
                welcomePage.setVisibility(View.VISIBLE);
                sensorNumber.setVisibility(View.INVISIBLE);
                sensorView.setVisibility(View.INVISIBLE);
                buttondata.setVisibility(View.INVISIBLE);
                getSupportActionBar().hide();
                counterAnimation--;//3s minus to 0s
            }

            @Override
            public void onFinish() {
                //Set the welcome page invisible
                welcomePage.setVisibility(View.INVISIBLE);
                sensorNumber.setVisibility(View.VISIBLE);
                sensorView.setVisibility(View.VISIBLE);
                buttondata.setVisibility(View.VISIBLE);
                getSupportActionBar().show();
            }
        }.start();

    }

    private View.OnClickListener datacollected = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(v.getContext(), SensorButtons.class);
            startActivity(intent);
        }
    };

    @Override
    public void onClick(View v) {
        final int welcomeFlag = 0;
    }

    private ImageView.OnClickListener welcomeclicked = new ImageView.OnClickListener(){
        @Override
        public void onClick(View v) {
            int welcomeFlag = 0;
            welcomePage.setVisibility(View.INVISIBLE);
            sensorNumber.setVisibility(View.VISIBLE);
            sensorView.setVisibility(View.VISIBLE);
            buttondata.setVisibility(View.VISIBLE);
            getSupportActionBar().show();
        }
    };

}