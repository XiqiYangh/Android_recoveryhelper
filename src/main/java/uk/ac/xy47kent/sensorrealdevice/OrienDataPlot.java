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
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class OrienDataPlot extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor, gravitySensor,magneticdfieldSensor;
    private TextView xOriVal,yOriVal,zOriVal;
    private float[] accValues = new float[3];//store the accValues of 3-axes
    private float[] magValues = new float[3];
    private float[] rotationMatrix = new float[9];//hold the desired rotation matrix
    private float[] oriValues = new float[3];

    private List<SensorClass> orientationDataList;
    private SensorClass orientationData;

    private GraphView orientationGraph;
    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;

    int index=0;
    long curTime;
    long diffTime;
    long lastUpdate= System.currentTimeMillis();

    Button doSave,doLog;
    private boolean doLogging = false;

    private Display display;
    float[] rotationMatrixAdjusted = new float[9];//new

    /*----------------------------filter--------------------------------------*/
    private SwitchCompat filterOriSwitch;
    private TextView xFilteredOriVal,yFilteredOriVal,zFilteredOriVal;

    private float[] oriFilteredValues = new float[3];

    private List<SensorClass> orientationFilteredDataList;
    private SensorClass orientationFilteredData;
    private GraphView orientationFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredOriX;
    private LineGraphSeries<DataPoint> seriesFilteredOriY;
    private LineGraphSeries<DataPoint> seriesFilteredOriZ;

    int indexFilteredOri = 0;

    int filterEachOn;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_orien_data_plot);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WindowManager windowManager = (WindowManager)getSystemService(WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();//new

        xOriVal = (TextView) findViewById(R.id.xOriVal);
        yOriVal = (TextView) findViewById(R.id.yOriVal);
        zOriVal = (TextView) findViewById(R.id.zOriVal);

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null){
            sensorManager.registerListener(OrienDataPlot.this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(OrienDataPlot.this, "Accelerometer Unavailable", Toast.LENGTH_SHORT).show();
        }

        this.magneticdfieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticdfieldSensor != null){
            sensorManager.registerListener(OrienDataPlot.this, magneticdfieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(OrienDataPlot.this, "Magnetometer Unavailable", Toast.LENGTH_SHORT).show();
        }

        this.orientationDataList = new ArrayList<SensorClass>();
        this.orientationDataList.add(new SensorClass(0,0,0,0,0));
        initializeOrientationGraph();

        /*----------------------filtered orientation---------------------*/
        this.orientationFilteredDataList = new ArrayList<SensorClass>();
        this.orientationFilteredDataList.add(new SensorClass(0, 0, 0, 0, 0));
        initializeOrientationFilteredGraph();

        doSave = (Button)findViewById(R.id.btsaveOri);
        doSave.setOnClickListener(btsaveClicked);

        doLog = (Button)findViewById(R.id.btlogOri);
        doLog.setOnClickListener(btlogClicked);

        /*------------------------filter switch--------------------*/
        xFilteredOriVal = (TextView)findViewById(R.id.xFilteredOriVal);
        yFilteredOriVal = (TextView)findViewById(R.id.yFilteredOriVal);
        zFilteredOriVal = (TextView)findViewById(R.id.zFilteredOriVal);

        filterOriSwitch = (SwitchCompat) findViewById(R.id.filterOriSwitch);
        filterOriSwitch.setChecked(false);
        filterOriSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //filtered Acc Data
                    orientationGraph.setVisibility(View.INVISIBLE);
                    orientationFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    xOriVal.setVisibility(View.INVISIBLE);
                    xFilteredOriVal.setVisibility(View.VISIBLE);//filtered
                    yOriVal.setVisibility(View.INVISIBLE);
                    yFilteredOriVal.setVisibility(View.VISIBLE);//filtered
                    zOriVal.setVisibility(View.INVISIBLE);
                    zFilteredOriVal.setVisibility(View.VISIBLE);//filtered
                    filterEachOn = 1;

                } else{
                    //raw Acc data
                    orientationGraph.setVisibility(View.VISIBLE);//raw
                    orientationFilteredGraph.setVisibility(View.INVISIBLE);
                    xOriVal.setVisibility(View.VISIBLE);//raw
                    xFilteredOriVal.setVisibility(View.INVISIBLE);
                    yOriVal.setVisibility(View.VISIBLE);//raw
                    yFilteredOriVal.setVisibility(View.INVISIBLE);
                    zOriVal.setVisibility(View.VISIBLE);//raw
                    zFilteredOriVal.setVisibility(View.INVISIBLE);
                    filterEachOn = 0;
                }
            }
        });

    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            accValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            magValues = event.values.clone();
        }

        switch (display.getRotation()){
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;//new
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y,SensorManager.AXIS_MINUS_X ,rotationMatrixAdjusted );
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y,rotationMatrixAdjusted );
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rotationMatrixAdjusted);
                break;
        }

        SensorManager.getRotationMatrix(rotationMatrix, null, accValues, magValues);
        //populate rotationMatrix

        //SensorManager.getOrientation(rotationMatrixAdjusted, oriValues);
        SensorManager.getOrientation(rotationMatrix, oriValues);
        //get azimuth, pitch and roll

        //sensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector);
        //getRotationMatrix: rotate the global coordinate system to the device coordinate system

        oriValues[1] = (float)Math.toDegrees(oriValues[1]);
        oriValues[2] = (float)Math.toDegrees(oriValues[2]);
        oriValues[0] = (float)Math.toDegrees(oriValues[0]);

        xOriVal.setText("Pitch:" + oriValues[1]);
        yOriVal.setText("Roll:" + oriValues[2]);
        zOriVal.setText("Azimuth:" + oriValues[0]);

        this.orientationData = new SensorClass();
        this.orientationData.setxAxisValue(oriValues[1]);
        this.orientationData.setyAxisValue(oriValues[2]);
        this.orientationData.setzAxisValue(oriValues[0]);
        this.orientationData.setAccuracy(event.accuracy);

        /*this.curTime = System.currentTimeMillis();// return the current time in milliseconds since 1,Jan,1970
        diffTime = (curTime - this.lastUpdate); //calculate the differential time
        this.lastUpdate = curTime;

        System.out.println("difftime:" + diffTime);
        this.orientationData.setTimestamp(diffTime);*/

        this.orientationDataList.add(orientationData);

        updateOrientationGraph();


        /*--------------------------filtered orientation-------------------------*/
        oriFilteredValues[1] = lowPass(oriValues[1], oriFilteredValues[1]);
        oriFilteredValues[2] = lowPass(oriValues[2], oriFilteredValues[2]);
        oriFilteredValues[0] = lowPass(oriValues[0], oriFilteredValues[0]);

        xFilteredOriVal.setText("Pitch:" + oriFilteredValues[1]);
        yFilteredOriVal.setText("Roll:" + oriFilteredValues[2]);
        zFilteredOriVal.setText("Azimuth:" + oriFilteredValues[0]);

        this.orientationFilteredData = new SensorClass();
        this.orientationFilteredData.setxAxisValue(oriFilteredValues[1]);
        this.orientationFilteredData.setyAxisValue(oriFilteredValues[2]);
        this.orientationFilteredData.setzAxisValue(oriFilteredValues[0]);
        this.orientationFilteredData.setAccuracy(event.accuracy);
        this.orientationFilteredDataList.add(orientationFilteredData);
        updateOrientationFilteredGraph();

        if (doLogging){
            try {
                writeFile();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() { //start interacting
        super.onResume();
        sensorManager.registerListener(this, this.accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, this.magneticdfieldSensor, SensorManager.SENSOR_DELAY_NORMAL);

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


    public void initializeOrientationGraph(){

        this.orientationGraph = (GraphView) findViewById(R.id.orientationGraph);
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
        this.seriesX.setTitle("Pitch");
        //this.seriesX.setDrawDataPoints(true);
        //this.seriesX.setDataPointsRadius(2);
        this.seriesY.setTitle("Roll");
        this.seriesZ.setTitle("Azimuth");

        this.orientationGraph.getLegendRenderer().setVisible(true);
        this.orientationGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.orientationGraph.getViewport().setXAxisBoundsManual(true);
        this.orientationGraph.getViewport().setMinX(0);
        this.orientationGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.orientationGraph.getViewport().setYAxisBoundsManual(true);
        this.orientationGraph.getViewport().setMinY(-360);
        this.orientationGraph.getViewport().setMaxY(360);
        this.orientationGraph.addSeries(this.seriesX);
        this.orientationGraph.addSeries(this.seriesY);
        this.orientationGraph.addSeries(this.seriesZ);

        //scrolling and scaling setting
        this.orientationGraph.getViewport().setScalable(true);
        this.orientationGraph.getViewport().setScrollable(true);
        this.orientationGraph.getViewport().scrollToEnd();

        this.orientationGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        this.orientationGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    public void updateOrientationGraph(){

        this.index=this.index + 1;
        System.out.println("update");
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");
        System.out.println("date:" + sdf.format(date));

        this.seriesX.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.orientationDataList.get(this.index).getxAxisValue()),
                true, 20);

        this.seriesY.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.orientationDataList.get(this.index).getyAxisValue()),
                true, 20);

        this.seriesZ.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.orientationDataList.get(this.index).getzAxisValue()),
                true, 20);
    }


    public void writeFile() throws InterruptedException { //write data into file

        if (filterEachOn == 0) { //record raw data
            FileOutputStream outputStream = null;
            String fileName = "orientationRawData.csv";
            String fileContents = oriValues[1] + "," + oriValues[2] + "," + oriValues[0] + "\n";

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
        } else if (filterEachOn == 1){
            FileOutputStream outputStream = null;
            String fileName = "orientationFilteredData.csv";
            String fileContents = oriFilteredValues[1] + "," + oriFilteredValues[2] + "," + oriFilteredValues[0] + "\n";

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
            if (filterEachOn == 0) {
                deleteFile("orientationData.csv");
                Toast.makeText(OrienDataPlot.this, "logging raw data", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                deleteFile("orientationFilteredData.csv");
                Toast.makeText(OrienDataPlot.this,"logging filtered data", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private View.OnClickListener btsaveClicked = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            //saving data onto a file
            doLogging = false;
            if (filterEachOn == 0) {
                Toast.makeText(OrienDataPlot.this, "raw data saved", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                Toast.makeText(OrienDataPlot.this, "filtered data saved", Toast.LENGTH_SHORT).show();
            }
        }
    };


    /*---------------------------lowpass filter----------------------------*/
    public float lowPass(float current, float last){
        float alpha = 0.12f;
        return last * (1.0f - alpha) + current * alpha;
    }

    //initialize filtered orientation:
    public void initializeOrientationFilteredGraph() {

        this.orientationFilteredGraph = (GraphView) findViewById(R.id.orientationFilteredGraph);
        //creating series for x axis plot
        this.seriesFilteredOriX = new LineGraphSeries<DataPoint>();
        this.seriesFilteredOriX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesFilteredOriY = new LineGraphSeries<DataPoint>();
        this.seriesFilteredOriY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesFilteredOriZ = new LineGraphSeries<DataPoint>();
        this.seriesFilteredOriZ.setColor(Color.GREEN);

        // legend
        this.seriesFilteredOriX.setTitle("Pitch");
        this.seriesFilteredOriY.setTitle("Roll");
        this.seriesFilteredOriZ.setTitle("Azimuth");

        this.orientationFilteredGraph.getLegendRenderer().setVisible(true);
        this.orientationFilteredGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.orientationFilteredGraph.getViewport().setXAxisBoundsManual(true);
        this.orientationFilteredGraph.getViewport().setMinX(0);
        this.orientationFilteredGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.orientationFilteredGraph.getViewport().setYAxisBoundsManual(true);
        this.orientationFilteredGraph.getViewport().setMinY(-360);
        this.orientationFilteredGraph.getViewport().setMaxY(360);
        this.orientationFilteredGraph.addSeries(this.seriesFilteredOriX);
        this.orientationFilteredGraph.addSeries(this.seriesFilteredOriY);
        this.orientationFilteredGraph.addSeries(this.seriesFilteredOriZ);

        //scrolling and scaling setting
        this.orientationFilteredGraph.getViewport().setScalable(true);
        this.orientationFilteredGraph.getViewport().setScrollable(true);
        this.orientationFilteredGraph.getViewport().scrollToEnd();

        this.orientationFilteredGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        this.orientationFilteredGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update filtered orientation
    public void updateOrientationFilteredGraph() {

        this.indexFilteredOri = this.indexFilteredOri + 1;

        this.seriesFilteredOriX.appendData(new DataPoint(indexFilteredOri, this.orientationFilteredDataList.get(this.indexFilteredOri).getxAxisValue()),
                true, 20);

        this.seriesFilteredOriY.appendData(new DataPoint(indexFilteredOri, this.orientationFilteredDataList.get(this.indexFilteredOri).getyAxisValue()),
                true, 20);

        this.seriesFilteredOriZ.appendData(new DataPoint(indexFilteredOri, this.orientationFilteredDataList.get(this.indexFilteredOri).getzAxisValue()),
                true, 20);
    }

}
