package uk.ac.xy47kent.sensorrealdevice;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.telephony.SmsManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;

public class SelfTest extends AppCompatActivity implements SensorEventListener {

    FloatingActionButton plus,message,phone,email,location,capture,moveAngle;
    Animation plus_close,plus_open,rotate_clockwise,rotate_anticlockwise;
    boolean isOpen = false;

    private View selftest;

    private TextView testresult;

    private SensorManager sensorManager;
    private Sensor accelerometerSensor,magneticdfieldSensor;
    private float accValues[] = new float[3];//store the accValues of 3-axes
    private float magValues[] = new float[3];
    private float[] rotationMatrix = new float[9];//hold the desired rotation matrix
    private float[] oriValues = new float[3];
    float[] rotationMatrixAdjusted = new float[9];//new

    float[] accFilteredValues = new float[3];
    float[] oriFilteredValues = new float[3];

    public int counter = 6;//calculate coundown timer for 6s
    int timeCountingFinished = 0; //only true after CountDownTimer

    ImageView goodView, mediumView, littleView, flatView;

    TextView moveAngleDisplay;//display angle
    float movementAngle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_self_test);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        testresult = (TextView) findViewById(R.id.testresult);

        /*goodView = (ImageView)findViewById(R.id.goodView);
        mediumView =(ImageView)findViewById(R.id.mediumView);
        littleView = (ImageView)findViewById(R.id.littleView);*/
        flatView = (ImageView)findViewById(R.id.flatView);

        moveAngleDisplay = (TextView)findViewById(R.id.moveAngleDisplay);//text display

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        this.magneticdfieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        phone = (FloatingActionButton)findViewById(R.id.phone);
        message = (FloatingActionButton)findViewById(R.id.message);
        email = (FloatingActionButton)findViewById(R.id.email);
        plus = (FloatingActionButton)findViewById(R.id.plus);
        location = (FloatingActionButton)findViewById(R.id.location);
        moveAngle = (FloatingActionButton)findViewById(R.id.btmoveAngle); //floating button


        rotate_anticlockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_anticlockwise);
        rotate_clockwise = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.rotate_clockwise);
        plus_close = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.plus_close);
        plus_open = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.plus_open);

        plus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isOpen){ //plus has been unfolded
                    //animation
                    moveAngleDisplay.setVisibility(View.INVISIBLE);
                        //disable moveAngleDisplay visualization
                    phone.startAnimation(plus_close);
                    message.startAnimation(plus_close);
                    email.startAnimation(plus_close);
                    location.startAnimation(plus_close);
                    moveAngle.startAnimation(plus_close);
                    plus.startAnimation(rotate_anticlockwise);
                    //setClickable
                    phone.setClickable(false);
                    message.setClickable(false);
                    email.setClickable(false);
                    location.setClickable(false);
                    moveAngle.setClickable(false);
                    isOpen = false;
                } else{//plus has been folded, tend to open
                    //animation
                    phone.startAnimation(plus_open);
                    message.startAnimation(plus_open);
                    email.startAnimation(plus_open);
                    location.startAnimation(plus_open);
                    moveAngle.startAnimation(plus_open);
                    plus.startAnimation(rotate_clockwise);
                    //setClickable
                    phone.setClickable(true);
                    message.setClickable(true);
                    email.setClickable(true);
                    location.setClickable(true);
                    moveAngle.setClickable(true);
                    isOpen = true;
                }
            }
        });

        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent phoneintent = new Intent(Intent.ACTION_VIEW,Uri.parse("tel:"));
                startActivity(phoneintent);
            }
        });

        message.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SmsManager smsManager = SmsManager.getDefault();
                Intent messageintent = new Intent(Intent.ACTION_VIEW,Uri.parse("smsto:"));
                startActivity(messageintent);
            }
        });

        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailintent = new Intent(Intent.ACTION_VIEW,Uri.parse("mailto:"));
                startActivity(emailintent);
            }
        });

        location.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri gmmIntentUri = Uri.parse("geo:51.306800842285,1.0429999828339");
                // Create an Intent from gmmIntentUri. Set the action to ACTION_VIEW
                Intent mapIntent = new Intent(Intent.ACTION_VIEW,gmmIntentUri);
                // Make the Intent explicit by setting the Google Maps package
                mapIntent.setPackage("com.google.android.apps.maps");
                // Attempt to start an activity that can handle the Intent
                startActivity(mapIntent);
            }
        });

        moveAngle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                moveAngleDisplay.setVisibility(View.VISIBLE);
            }
        });

        selftest = findViewById(R.id.selftest);//selftest layout id--whole view
        /*
        imageView = (ImageView)findViewById(R.id.imageView);
        FloatingActionButton capture = (FloatingActionButton)findViewById(R.id.capture);
        capture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bitmap bitmap = ScreenShot.takescreenshotOfRootView(imageView);
                imageView.setImageBitmap(bitmap);
                selftest.setBackgroundColor(Color.parseColor("#999999"));
            }
        });
        */

        final FloatingActionButton browse = (FloatingActionButton)findViewById(R.id.browse);
        browse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //open screeshots
            }
        });


        //CountDownTimer: for initialization and determinePosition
        new CountDownTimer(6000, 1000) { //count 8s ,time interval 1s
            @Override
            public void onTick(long millisUntilFinished) {
                //testresult.setText(String.valueOf(counter));
                initialization();//call method during the countdown timer, here deciding current position during 10s
                counter--;//6s minus to 0s
            }

            @Override
            public void onFinish() {
                //testresult.setText("Finished");//when it counts down to 0s,things appeared here
                //determinePosition();

                // solution: it should be real-time, however, it cannot.
                // Thus, the determination should be carried in onSensorChanged() method
                timeCountingFinished = 1; //set a flag to tell the CountDownTimer has finished time counting
                Vibrator vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.vibrate(400); //also set vibration to tell finishing initialization

            }
        }.start();


    }//onCreate() ending

    ///data/data/uk.ac.xy47kent.sensorrealdevice
    ///data/data/uk.ac.xy47kent.sensorrealdevice/files/accelerometerFilteredData.csv

    private void initialization() {
        if (Math.abs(accValues[0]) > 3.5 || Math.abs(accValues[1]) > 3.5) {
            Toast.makeText(this, "Please place your phone up and flat", Toast.LENGTH_SHORT).show();
            testresult.setText("");//won't show anything until finishing initialization
            flatView.setVisibility(View.VISIBLE);
        } else {
            testresult.setText("Your device is up and flat" + "\n" + "Please move your forearm");
            flatView.setVisibility(View.INVISIBLE);
        }
    }

    /*private void determinePosition() {
        if ((Math.abs(oriValues[1]) < 25) && (Math.abs(oriValues[1]) > 10)) { //Pitch refers to oriValues[1]
            //Toast.makeText(this, "Little Recovery ", Toast.LENGTH_SHORT).show();
            testresult.setText("Little Recovery" + oriValues[1]);//check if oriValues[1] is in form of degree other than radians
        } else if ((Math.abs(oriValues[1]) < 50) && (Math.abs(oriValues[1]) > 25)) {
            //Toast.makeText(this, "Medium Recovery", Toast.LENGTH_SHORT).show();
            testresult.setText("Medium Recovery" + oriValues[1]);
        } else if ((Math.abs(oriValues[1]) < 90) && (Math.abs(oriValues[1]) > 50)) {
            //Toast.makeText(this, "Good Recovery", Toast.LENGTH_SHORT).show();
            testresult.setText("Good Recovery" + oriValues[1]);
        }
    }*/


    //changes of sensor values
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magValues = event.values.clone();
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accValues, magValues);
        SensorManager.getOrientation(rotationMatrix, oriValues);
        oriValues[1] = (float)Math.toDegrees(oriValues[1]);
        oriValues[2] = (float)Math.toDegrees(oriValues[2]);
        oriValues[0] = (float)Math.toDegrees(oriValues[0]);

        accFilteredValues[0] = lowPass(accValues[0],accFilteredValues[0]);
        accFilteredValues[1] = lowPass(accValues[1],accFilteredValues[1]);
        accFilteredValues[2] = lowPass(accValues[2],accFilteredValues[2]);

        oriFilteredValues[1] = lowPass(oriValues[1], oriFilteredValues[1]);//in degree
        oriFilteredValues[2] = lowPass(oriValues[2], oriFilteredValues[2]);
        oriFilteredValues[0] = lowPass(oriValues[0], oriFilteredValues[0]);

        /*---------------------------movement angle--------------------------------------*/
        if ((accFilteredValues[1] > 0.12f) && (accFilteredValues[1] < 10.0f)){
            if ((accFilteredValues[2] > 0.1f) && (accFilteredValues[2] < 10.0f)){
                //0-90
                movementAngle = Math.abs(oriFilteredValues[1]);
                moveAngleDisplay.setText("Movement Angle: " + movementAngle + "°");
            } else if (accFilteredValues[2] > -10.0f && (accFilteredValues[2] < -0.1f)){
                //90-180
                movementAngle = (180 - Math.abs(oriFilteredValues[1]));
                moveAngleDisplay.setText("Movement Angle: " + movementAngle + "°");
            }
        } else if ((accFilteredValues[1] < 0.0f) && (accFilteredValues[1] > -10.0f)){
            if ((accFilteredValues[2] > -10.0f) && (accFilteredValues[2] < -0.1f)){
                //180-270
                movementAngle = (180 + Math.abs(oriFilteredValues[1]));
                moveAngleDisplay.setText("Movement Angle: " + movementAngle + "°");
            } else if ((accFilteredValues[2] > 0.0f) && (accFilteredValues[2] < 10.0f)){
                //270-360
                movementAngle = (360 - Math.abs(oriFilteredValues[1]));
                moveAngleDisplay.setText("Movement Angle: " + movementAngle + "°");
            }
        }

        float Pitch = oriFilteredValues[1]; //rotation around x axis, which measures the tilt of device

        //after initialization: this method changes with sensor values
        if (timeCountingFinished == 1){ //determinePosition only when finishing initialization
            if (Math.abs(Pitch) < 2){
                testresult.setText("Your device is up and flat ");
                flatView.setVisibility(View.INVISIBLE);
                /*littleView.setVisibility(View.INVISIBLE);
                mediumView.setVisibility(View.INVISIBLE);
                goodView.setVisibility(View.INVISIBLE);*/
            } else if ((movementAngle <30) && (movementAngle > 1)) {
                //Toast.makeText(this, "Little Recovery ", Toast.LENGTH_SHORT).show();
                testresult.setText("Little Recovery");
                flatView.setVisibility(View.INVISIBLE);
                /*littleView.setVisibility(View.VISIBLE); //visible
                mediumView.setVisibility(View.INVISIBLE);
                goodView.setVisibility(View.INVISIBLE);*/
            } else if ((movementAngle < 60) && (movementAngle > 30)) {
                //Toast.makeText(this, "Medium Recovery", Toast.LENGTH_SHORT).show();
                testresult.setText("Medium Recovery");
                flatView.setVisibility(View.INVISIBLE);
                /*littleView.setVisibility(View.INVISIBLE);
                mediumView.setVisibility(View.VISIBLE); //visible
                goodView.setVisibility(View.INVISIBLE);*/
            } else if ((movementAngle < 359 ) && (movementAngle > 60)) {
                //Toast.makeText(this, "Good Recovery", Toast.LENGTH_SHORT).show();
                testresult.setText("Good Recovery");
                flatView.setVisibility(View.INVISIBLE);
                /*littleView.setVisibility(View.INVISIBLE);
                mediumView.setVisibility(View.INVISIBLE);
                goodView.setVisibility(View.VISIBLE); //visible */
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, this.magneticdfieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() { // when the screen turns into sleep
        super.onPause();
        //unregistering sensor when application is on pause to save battery
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }

    //lowPass filter:
    public float lowPass(float current, float last){
        float alpha = 0.5f;
        return (last * (1.0f - alpha) + current * alpha);
    }


    /*
    float a = 0.1f;
    public void onSensorChanged(SensorEvent event {
        x = event.values[0];
        y = event.values[1];
        z = event.values[2];
        mLowPassX = lowPass(x, mLowPassX);
        mLowPassY = lowPass(y, mLowPassY);
        mLowPassZ = lowPass(z, mLowPassZ);
    }

    // simple low-pass filter
    float lowPass(float current, float last) {
        return last * (1.0f - a) + current * a;
    }
    */

}

//layout
/*
    <ImageView
        android:id="@+id/imageView"
        android:layout_width="0dp"
        android:layout_height="match_parent"
        android:layout_below="@id/btfilter"
        android:layout_centerHorizontal="true"
        android:layout_marginStart="8dp"
        android:layout_marginTop="24dp"
        android:layout_marginEnd="8dp"
        android:layout_marginBottom="60dp"
        android:scaleType="center"
        app:layout_constraintBottom_toBottomOf="parent"
        app:layout_constraintEnd_toEndOf="parent"
        app:layout_constraintStart_toStartOf="parent"
        app:layout_constraintTop_toTopOf="parent"
        app:layout_constraintVertical_bias="0.0" />
*/
