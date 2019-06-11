package uk.ac.xy47kent.sensorrealdevice;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import org.w3c.dom.Text;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

//BAD：use GraphView
public class AccelDataPlot extends AppCompatActivity implements SensorEventListener {

    //setting variables
    private SensorManager sensorManager;
    private Sensor accelerometerSensor, gravitySensor;
    private TextView xAccVal,yAccVal,zAccVal;
    float accValues[] = new float[3];

    private List<SensorClass> accelerometerDataList;
    private SensorClass accelerometerData;

    private GraphView accelerometerGraph;
    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;

    int index=0;
    long curTime;
    long diffTime;
    long lastUpdate= System.currentTimeMillis();

    Button doSave,doLog;
    private boolean doLogging = false;

    //TextView currenttime;

    /*----------------------------filter--------------------------------------*/
    private SwitchCompat filterAccSwitch;
    private TextView xFilteredAccVal,yFilteredAccVal,zFilteredAccVal;

    private float[] accFilteredValues = new float[3];

    private List<SensorClass> accelerometerFilteredDataList;
    private SensorClass accelerometerFilteredData;
    private GraphView accelerometerEachFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredAccX;
    private LineGraphSeries<DataPoint> seriesFilteredAccY;
    private LineGraphSeries<DataPoint> seriesFilteredAccZ;

    int indexFilteredAcc = 0;

    public int filterEachOn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accel_data_plot);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        xAccVal = (TextView) findViewById(R.id.xAccVal);
        yAccVal = (TextView) findViewById(R.id.yAccVal);
        zAccVal = (TextView) findViewById(R.id.zAccVal);

        //to identify the sensors available in an android device
        this.sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);

        //to check if accelerometer is present in a device
        if(this.sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER)!=null){
            //Toast.makeText(this, "Accelerometer Supported ", Toast.LENGTH_SHORT).show();
            //setting reference of the accelerometer to the variable accelerometerSensor
            this.accelerometerSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            //initializing arraylist
            this.accelerometerDataList = new ArrayList<SensorClass>();
            //adding accelerometer data list values for the starting
            this.accelerometerDataList.add(new SensorClass(0, 0, 0, 0, 0));
            //initializing accelerometer graph
            initializeAccelerometerGraph();

            /*------------------filtered acceleration-------------------*/
            this.accelerometerFilteredDataList = new ArrayList<SensorClass>();
            this.accelerometerFilteredDataList.add(new SensorClass(0,0,0,0,0));
            initializeAccelerometerFilteredGraph();

        }else{
            Toast.makeText(this, "Accelerometer Unavailable", Toast.LENGTH_SHORT).show();
        }

        /*gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);//specific
        if (gravitySensor != null){
            sensorManager.registerListener(AccelDataPlot.this, gravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            Toast.makeText(AccelDataPlot.this, "Gravity Supported", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(AccelDataPlot.this, "Gravity Unavailable", Toast.LENGTH_SHORT).show();
        }*/

        doSave = (Button)findViewById(R.id.btsaveAcc);
        doSave.setOnClickListener(btsaveClicked);

        doLog = (Button)findViewById(R.id.btlogAcc);
        doLog.setOnClickListener(btlogClicked);

        /*------------------------filter switch--------------------*/
        xFilteredAccVal = (TextView)findViewById(R.id.xFilteredAccVal);
        yFilteredAccVal = (TextView)findViewById(R.id.yFilteredAccVal);
        zFilteredAccVal = (TextView)findViewById(R.id.zFilteredAccVal);

        filterAccSwitch = (SwitchCompat) findViewById(R.id.filterAccSwitch);
        filterAccSwitch.setChecked(false);
        filterAccSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //filtered Acc Data
                    accelerometerGraph.setVisibility(View.INVISIBLE);
                    accelerometerEachFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    xAccVal.setVisibility(View.INVISIBLE);
                    xFilteredAccVal.setVisibility(View.VISIBLE);//filtered
                    yAccVal.setVisibility(View.INVISIBLE);
                    yFilteredAccVal.setVisibility(View.VISIBLE);//filtered
                    zAccVal.setVisibility(View.INVISIBLE);
                    zFilteredAccVal.setVisibility(View.VISIBLE);//filtered
                    filterEachOn = 1;

                } else{
                    //raw Acc data
                    accelerometerGraph.setVisibility(View.VISIBLE);//raw
                    accelerometerEachFilteredGraph.setVisibility(View.INVISIBLE);
                    xAccVal.setVisibility(View.VISIBLE);//raw
                    xFilteredAccVal.setVisibility(View.INVISIBLE);
                    yAccVal.setVisibility(View.VISIBLE);//raw
                    yFilteredAccVal.setVisibility(View.INVISIBLE);
                    zAccVal.setVisibility(View.VISIBLE);//raw
                    zFilteredAccVal.setVisibility(View.INVISIBLE);
                    filterEachOn = 0;
                }
            }
        });

    }


    //this method is implemented as part of SensorEventListener
    //it is called automatically at specific time intervals by the phone to retrieve accelerometer values
    //event object contains sensor values at a timeinstance
    @Override
    public void onSensorChanged(SensorEvent event) { //available to users
        //float eventTimeMillis = (event.timestamp/1000000) + diffTime;
        //Calendar calendar = Calendar.getInstance();
        //calendar.setTimeInMillis(eventTimeMillis);

        //Accx_offset = 0.7060, Accy_offset = -0.1554, Accz_offset = -0.1842
        xAccVal.setText("xAccVal:" + event.values[0]);
        yAccVal.setText("yAccVal:" + event.values[1]);
        zAccVal.setText("zAccVal:" + event.values[2]);

        accValues = event.values.clone();

        //creating a accelerometerclass and filling in all the data
        this.accelerometerData = new SensorClass();
        this.accelerometerData.setxAxisValue(event.values[0]);
        this.accelerometerData.setyAxisValue(event.values[1]);
        this.accelerometerData.setzAxisValue(event.values[2]);
        this.accelerometerData.setAccuracy(event.accuracy);

        //calculating time lapse
        this.curTime = System.currentTimeMillis();
        diffTime = (curTime - this.lastUpdate);
        this.lastUpdate = curTime ;

        //setting time lapse between consecutive datapoints
        this.accelerometerData.setTimestamp(diffTime);

        //adding the class to the list of accelerometer data points
        this.accelerometerDataList.add(accelerometerData);

        //displaying accelerometer values on the console
        String display = String.valueOf(this.accelerometerData.getxAxisValue())+ "; "
                +String.valueOf(this.accelerometerData.getyAxisValue())+"; "
                +String.valueOf(this.accelerometerData.getzAxisValue())+"; "
                +String.valueOf(this.accelerometerData.getTimestamp());
        //Toast.makeText(this, display, Toast.LENGTH_LONG);
        System.out.println(display);

        //updating graph display
        updateAccelerometerGraph();

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            if (doLogging){
                try {
                    writeFile();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /*--------------------filtered acceleration-----------------------*/
        accFilteredValues[0] = lowPass(accValues[0],accFilteredValues[0]);
        accFilteredValues[1] = lowPass(accValues[1],accFilteredValues[1]);
        accFilteredValues[2] = lowPass(accValues[2],accFilteredValues[2]);

        xFilteredAccVal.setText("xAccVal:" + accFilteredValues[0]);
        yFilteredAccVal.setText("yAccVal:" + accFilteredValues[1]);
        zFilteredAccVal.setText("zAccVal:" + accFilteredValues[2]);

        this.accelerometerFilteredData = new SensorClass();
        this.accelerometerFilteredData.setxAxisValue(accFilteredValues[0]);
        this.accelerometerFilteredData.setyAxisValue(accFilteredValues[1]);
        this.accelerometerFilteredData.setzAxisValue(accFilteredValues[2]);
        this.accelerometerFilteredData.setAccuracy(event.accuracy);
        this.accelerometerFilteredDataList.add(accelerometerFilteredData);
        updateAccelerometerFilteredGraph();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //registering the sensor when application is resumed
    //continues retrieving data from sensor
    @Override
    protected void onResume() { //start interacting
        super.onResume();
        sensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }


    //method called when application is on pause
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


    public void initializeAccelerometerGraph(){

        this.accelerometerGraph = (GraphView) findViewById(R.id.accelerometerGraph);
        //creating series for x axis plot
        this.seriesX = new LineGraphSeries<com.jjoe64.graphview.series.DataPoint>();
        this.seriesX.setColor(Color.RED);
        //creating series for y axis plot
        this.seriesY = new LineGraphSeries<com.jjoe64.graphview.series.DataPoint>();
        this.seriesY.setColor(Color.BLUE);
        //creating series for z axis plot
        this.seriesZ = new LineGraphSeries<com.jjoe64.graphview.series.DataPoint>();
        this.seriesZ.setColor(Color.GREEN);

        // legend
        this.seriesX.setTitle("xAxis");
        //this.seriesX.setDrawDataPoints(true);
        //this.seriesX.setDataPointsRadius(2);
        this.seriesY.setTitle("yAxis");
        this.seriesZ.setTitle("zAxis");

        this.accelerometerGraph.getLegendRenderer().setVisible(true);
        this.accelerometerGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.accelerometerGraph.getViewport().setXAxisBoundsManual(true);
        this.accelerometerGraph.getViewport().setMinX(0);
        this.accelerometerGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.accelerometerGraph.getViewport().setYAxisBoundsManual(true);
        this.accelerometerGraph.getViewport().setMinY(-15);
        this.accelerometerGraph.getViewport().setMaxY(15);
        this.accelerometerGraph.addSeries(this.seriesX);
        this.accelerometerGraph.addSeries(this.seriesY);
        this.accelerometerGraph.addSeries(this.seriesZ);

        //scrolling and scaling setting
        this.accelerometerGraph.getViewport().setScalable(true);
        this.accelerometerGraph.getViewport().setScrollable(true);
        this.accelerometerGraph.getViewport().scrollToEnd();

        this.accelerometerGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        //this.accelerometerGraph.getGridLabelRenderer().setVerticalAxisTitle("accelerometer values");
        this.accelerometerGraph.getGridLabelRenderer().setHighlightZeroLines(true);

        //color setting
        //this.accelerometerGraph.setBackgroundColor(Color.WHITE);

    }

    //update accelerometer data
    public void updateAccelerometerGraph(){

        this.index=this.index+1;
        System.out.println("update");

        this.seriesX.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.accelerometerDataList.get(this.index).getxAxisValue()),
                true, 20);

        this.seriesY.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.accelerometerDataList.get(this.index).getyAxisValue()),
                true, 20);

        this.seriesZ.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.accelerometerDataList.get(this.index).getzAxisValue()),
                true, 20);
    }


    public void writeFile() throws InterruptedException { //write data into file

        if (filterEachOn == 0) { //record raw data
            FileOutputStream outputStream = null;
            String fileName = "accelerometerRawData.csv";
            String fileContents = accValues[0] + "," + accValues[1] + "," + accValues[2] + "\n";
            //String fileContents = xAccVal.getText().toString();

            try {
                outputStream = openFileOutput(fileName, Context.MODE_APPEND);
                //byte[] bytes = fileContents.getBytes();
                //outputStream.write(bytes);
                outputStream.write(fileContents.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else if (filterEachOn == 1){ //record filtered data
            FileOutputStream outputStream = null;
            String fileName = "accelerometerFilteredData.csv";
            String fileContents = (accFilteredValues[0] - 0.7060) + ","
                    + (accFilteredValues[1] - (-0.1554)) + ","
                    + (accFilteredValues[2] - (-0.1842)) + "\n";
            //String fileContents = xAccVal.getText().toString();

            try {
                outputStream = openFileOutput(fileName, Context.MODE_APPEND);
                outputStream.write(fileContents.getBytes());
                outputStream.flush();
                outputStream.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    private View.OnClickListener btlogClicked = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            doLogging = true;
            if (filterEachOn == 0){
                deleteFile("accelerometerData.csv");//delete previous data every time to avoid influencing test each time
                Toast.makeText(AccelDataPlot.this, "logging raw data", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                deleteFile("accelerometerFilteredData.csv");
                Toast.makeText(AccelDataPlot.this, "logging filtered data", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private View.OnClickListener btsaveClicked = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //saving data onto a file
            doLogging = false;
            if (filterEachOn == 0){
                Toast.makeText(AccelDataPlot.this, "raw data saved", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                Toast.makeText(AccelDataPlot.this, "filtered data saved", Toast.LENGTH_SHORT).show();
            }
        }
    };


    /*---------------------------lowpass filter----------------------------*/
    public float lowPass(float current, float last){
        float alpha = 0.12f;
        return last * (1.0f - alpha) + current * alpha;
    }

    //initialize filtered acceleration:
    public void initializeAccelerometerFilteredGraph(){

        this.accelerometerEachFilteredGraph = (GraphView) findViewById(R.id.accelerometerFilteredGraph);
        //creating series for x axis plot
        this.seriesFilteredAccX = new LineGraphSeries<DataPoint>();
        this.seriesFilteredAccX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesFilteredAccY = new LineGraphSeries<DataPoint>();
        this.seriesFilteredAccY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesFilteredAccZ = new LineGraphSeries<DataPoint>();
        this.seriesFilteredAccZ.setColor(Color.GREEN);

        // legend
        this.seriesFilteredAccX.setTitle("xAxis");
        this.seriesFilteredAccY.setTitle("yAxis");
        this.seriesFilteredAccZ.setTitle("zAxis");

        this.accelerometerEachFilteredGraph.getLegendRenderer().setVisible(true);
        this.accelerometerEachFilteredGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.accelerometerEachFilteredGraph.getViewport().setXAxisBoundsManual(true);
        this.accelerometerEachFilteredGraph.getViewport().setMinX(0);
        this.accelerometerEachFilteredGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.accelerometerEachFilteredGraph.getViewport().setYAxisBoundsManual(true);
        this.accelerometerEachFilteredGraph.getViewport().setMinY(-15);
        this.accelerometerEachFilteredGraph.getViewport().setMaxY(15);
        this.accelerometerEachFilteredGraph.addSeries(this.seriesFilteredAccX);
        this.accelerometerEachFilteredGraph.addSeries(this.seriesFilteredAccY);
        this.accelerometerEachFilteredGraph.addSeries(this.seriesFilteredAccZ);

        //scrolling and scaling setting
        this.accelerometerEachFilteredGraph.getViewport().setScalable(true);
        this.accelerometerEachFilteredGraph.getViewport().setScrollable(true);
        this.accelerometerEachFilteredGraph.getViewport().scrollToEnd();

        this.accelerometerEachFilteredGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        this.accelerometerEachFilteredGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update filtered accelerometer data
    public void updateAccelerometerFilteredGraph(){

        this.indexFilteredAcc=this.indexFilteredAcc+1;

        this.seriesFilteredAccX.appendData(new DataPoint(indexFilteredAcc, this.accelerometerFilteredDataList.get(this.indexFilteredAcc).getxAxisValue()),
                true, 20);

        this.seriesFilteredAccY.appendData(new DataPoint(indexFilteredAcc, this.accelerometerFilteredDataList.get(this.indexFilteredAcc).getyAxisValue()),
                true, 20);

        this.seriesFilteredAccZ.appendData(new DataPoint(indexFilteredAcc, this.accelerometerFilteredDataList.get(this.indexFilteredAcc).getzAxisValue()),
                true, 20);
    }

}

