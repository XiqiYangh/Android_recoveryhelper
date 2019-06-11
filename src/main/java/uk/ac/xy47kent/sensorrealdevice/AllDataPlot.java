package uk.ac.xy47kent.sensorrealdevice;

import android.content.Context;
import android.content.Intent;
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
import java.util.ArrayList;
import java.util.List;

public class AllDataPlot extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometerSensor, magneticdfieldSensor;
    private float[] accValues = new float[3];//store the accValues of 3-axes
    private float[] magValues = new float[3];
    private float[] rotationMatrix = new float[9];//hold the desired rotation matrix
    private float[] oriValues = new float[3];

    TextView accValDisplay, magValDispaly, oriValDispaly;

    private List<SensorClass> accelerometerDataList;
    private SensorClass accelerometerData;
    private GraphView accelerometerGraph;
    private LineGraphSeries<DataPoint> seriesAccX;
    private LineGraphSeries<DataPoint> seriesAccY;
    private LineGraphSeries<DataPoint> seriesAccZ;

    private List<SensorClass> magneticfieldDataList;
    private SensorClass magneticfieldData;
    private GraphView magneticfieldGraph;
    private LineGraphSeries<DataPoint> seriesMagX;
    private LineGraphSeries<DataPoint> seriesMagY;
    private LineGraphSeries<DataPoint> seriesMagZ;

    private List<SensorClass> orientationDataList;
    private SensorClass orientationData;
    private GraphView orientationGraph;
    private LineGraphSeries<DataPoint> seriesOriX;
    private LineGraphSeries<DataPoint> seriesOriY;
    private LineGraphSeries<DataPoint> seriesOriZ;

    int indexAcc = 0; //if use the same index, the application would automatically terminate
    int indexMag = 0;
    int indexOri = 0;

    private Display display;
    float[] rotationMatrixAdjusted = new float[9];//new

    /*------------------------------filter--------------------------------*/
    private SwitchCompat filterSwitch;
    private float[] accFilteredValues = new float[3];
    private float[] magFilteredValues = new float[3];
    private float[] oriFilteredValues = new float[3];

    private List<SensorClass> accelerometerFilteredDataList;
    private SensorClass accelerometerFilteredData;
    private GraphView accelerometerFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredAccX;
    private LineGraphSeries<DataPoint> seriesFilteredAccY;
    private LineGraphSeries<DataPoint> seriesFilteredAccZ;

    private List<SensorClass> magneticfieldFilteredDataList;
    private SensorClass magneticfieldFilteredData;
    private GraphView magneticfieldFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredMagX;
    private LineGraphSeries<DataPoint> seriesFilteredMagY;
    private LineGraphSeries<DataPoint> seriesFilteredMagZ;

    private List<SensorClass> orientationFilteredDataList;
    private SensorClass orientationFilteredData;
    private GraphView orientationFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredOriX;
    private LineGraphSeries<DataPoint> seriesFilteredOriY;
    private LineGraphSeries<DataPoint> seriesFilteredOriZ;

    int indexFilteredAcc = 0;
    int indexFilteredMag = 0;
    int indexFilteredOri = 0;

    Button doSave,doLog;
    int filterEachOn;
    private boolean doLogging = false;


    //int filterOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_all_data_plot);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();//new

        doSave = (Button)findViewById(R.id.btsave);
        doSave.setOnClickListener(btsaveClicked);

        doLog = (Button)findViewById(R.id.btlog);
        doLog.setOnClickListener(btlogClicked);
        //buttons will not get respond if they are put too behind

        this.sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //acceleration:
        this.accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometerSensor != null) {
            sensorManager.registerListener(AllDataPlot.this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);
            this.accelerometerDataList = new ArrayList<SensorClass>();
            this.accelerometerDataList.add(new SensorClass(0, 0, 0, 0, 0));
            initializeAccelerometerGraph();

            /*------------------filtered acceleration-------------------*/
            this.accelerometerFilteredDataList = new ArrayList<SensorClass>();
            this.accelerometerFilteredDataList.add(new SensorClass(0,0,0,0,0));
            initializeAccelerometerFilteredGraph();
        } else {
            Toast.makeText(AllDataPlot.this, "Accelerometer Unavailable", Toast.LENGTH_SHORT).show();
        }


        //magnetic field:
        this.magneticdfieldSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticdfieldSensor != null) {
            sensorManager.registerListener(AllDataPlot.this, magneticdfieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
            this.magneticfieldDataList = new ArrayList<SensorClass>();
            this.magneticfieldDataList.add(new SensorClass(0, 0, 0, 0, 0));
            initializeMagneticFieldGraph();

            /*-------------------filtered magnetic field-----------------*/
            this.magneticfieldFilteredDataList = new ArrayList<SensorClass>();
            this.magneticfieldFilteredDataList.add(new SensorClass(0,0,0,0,0));
            initializeMagneticFieldFilteredGraph();
        } else {
            Toast.makeText(AllDataPlot.this, "Magnetometer Unavailable", Toast.LENGTH_SHORT).show();
        }

        //orientation:
        this.orientationDataList = new ArrayList<SensorClass>();
        this.orientationDataList.add(new SensorClass(0, 0, 0, 0, 0));
        initializeOrientationGraph();

        /*----------------------filtered orientation---------------------*/
        this.orientationFilteredDataList = new ArrayList<SensorClass>();
        this.orientationFilteredDataList.add(new SensorClass(0, 0, 0, 0, 0));
        initializeOrientationFilteredGraph();


        //define TextView:
        accValDisplay = (TextView)findViewById(R.id.accValDisplay);
        magValDispaly = (TextView)findViewById(R.id.magValDisplay);
        oriValDispaly = (TextView)findViewById(R.id.oriValDisplay);

        //trigger events:
        accValDisplay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn into accVal detail
                Intent accintent = new Intent(v.getContext(),AccelDataPlot.class);
                startActivity(accintent);
            }
        });

        magValDispaly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn into magVal detail
                Intent magintent = new Intent(v.getContext(),MagfieldDataPlot.class);
                startActivity(magintent);
            }
        });

        oriValDispaly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //turn into oriVal detail
                Intent oriintent = new Intent(v.getContext(),OrienDataPlot.class);
                startActivity(oriintent);
            }
        });

        /*------------------------filter switch--------------------*/
        filterSwitch = (SwitchCompat) findViewById(R.id.filterSwitch);
        filterSwitch.setChecked(false);
        filterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){ //checked true
                    Toast.makeText(AllDataPlot.this, "Filtered Data", Toast.LENGTH_SHORT).show();
                    //acceleration
                    accelerometerGraph.setVisibility(View.INVISIBLE);
                    accelerometerFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    //magnetic field
                    magneticfieldGraph.setVisibility(View.INVISIBLE);
                    magneticfieldFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    //orientation
                    orientationGraph.setVisibility(View.INVISIBLE);
                    orientationFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    filterEachOn = 1;//filter state on
                } else{
                    Toast.makeText(AllDataPlot.this, "Raw Data", Toast.LENGTH_SHORT).show();
                    //acceleration
                    accelerometerGraph.setVisibility(View.VISIBLE);//raw
                    accelerometerFilteredGraph.setVisibility(View.INVISIBLE);
                    //magnetic field
                    magneticfieldGraph.setVisibility(View.VISIBLE);//raw
                    magneticfieldFilteredGraph.setVisibility(View.INVISIBLE);
                    //orientation
                    orientationGraph.setVisibility(View.VISIBLE);//raw
                    orientationFilteredGraph.setVisibility(View.INVISIBLE);
                    filterEachOn = 0;//off
                }

            }
        });


    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            accValues = event.values.clone();
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magValues = event.values.clone();
        }

        switch (display.getRotation()) {
            case Surface.ROTATION_0:
                rotationMatrixAdjusted = rotationMatrix.clone();
                break;//new
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y, rotationMatrixAdjusted);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X, rotationMatrixAdjusted);
                break;
        }


        //acceleration:
        this.accelerometerData = new SensorClass();
        this.accelerometerData.setxAxisValue(accValues[0]);
        this.accelerometerData.setyAxisValue(accValues[1]);
        this.accelerometerData.setzAxisValue(accValues[2]);
        this.accelerometerData.setAccuracy(event.accuracy);
        this.accelerometerDataList.add(accelerometerData);
        updateAccelerometerGraph();

        //magnetic field:
        this.magneticfieldData = new SensorClass();
        this.magneticfieldData.setxAxisValue(magValues[0]);
        this.magneticfieldData.setyAxisValue(magValues[1]);
        this.magneticfieldData.setzAxisValue(magValues[2]);
        this.magneticfieldData.setAccuracy(event.accuracy);
        this.magneticfieldDataList.add(magneticfieldData);
        updateMagneticFieldGraph();

        //orientation:
        SensorManager.getRotationMatrix(rotationMatrix, null, accValues, magValues);

        SensorManager.getOrientation(rotationMatrix, oriValues);

        oriValues[1] = (float) Math.toDegrees(oriValues[1]);
        oriValues[2] = (float) Math.toDegrees(oriValues[2]);
        oriValues[0] = (float) Math.toDegrees(oriValues[0]);

        this.orientationData = new SensorClass();
        this.orientationData.setxAxisValue(oriValues[1]);
        this.orientationData.setyAxisValue(oriValues[2]);
        this.orientationData.setzAxisValue(oriValues[0]);
        this.orientationData.setAccuracy(event.accuracy);
        this.orientationDataList.add(orientationData);
        updateOrientationGraph();

        /*--------------------filtered acceleration-----------------------*/
        accFilteredValues[0] = lowPass(accValues[0],accFilteredValues[0]);
        accFilteredValues[1] = lowPass(accValues[1],accFilteredValues[1]);
        accFilteredValues[2] = lowPass(accValues[2],accFilteredValues[2]);

        this.accelerometerFilteredData = new SensorClass();
        this.accelerometerFilteredData.setxAxisValue(accFilteredValues[0]);
        this.accelerometerFilteredData.setyAxisValue(accFilteredValues[1]);
        this.accelerometerFilteredData.setzAxisValue(accFilteredValues[2]);
        this.accelerometerFilteredData.setAccuracy(event.accuracy);
        this.accelerometerFilteredDataList.add(accelerometerFilteredData);
        updateAccelerometerFilteredGraph();

        //filtered magnetic field
        magFilteredValues[0] = lowPass(magValues[0],magFilteredValues[0]);
        magFilteredValues[1] = lowPass(magValues[1],magFilteredValues[1]);
        magFilteredValues[2] = lowPass(magValues[2],magFilteredValues[2]);

        this.magneticfieldFilteredData = new SensorClass();
        this.magneticfieldFilteredData.setxAxisValue(magFilteredValues[0]);
        this.magneticfieldFilteredData.setyAxisValue(magFilteredValues[1]);
        this.magneticfieldFilteredData.setzAxisValue(magFilteredValues[2]);
        this.magneticfieldFilteredData.setAccuracy(event.accuracy);
        this.magneticfieldFilteredDataList.add(magneticfieldFilteredData);
        updateMagneticFieldFilteredGraph();

        //filtered orientation
        /*SensorManager.getRotationMatrix(rotationMatrix, null, accFilteredValues, magFilteredValues);
        SensorManager.getOrientation(rotationMatrix, oriFilteredValues);
        oriFilteredValues[1] = (float) Math.toDegrees(oriFilteredValues[1]);
        oriFilteredValues[2] = (float) Math.toDegrees(oriFilteredValues[2]);
        oriFilteredValues[0] = (float) Math.toDegrees(oriFilteredValues[0]);*/

        //oriValues[] here already in degree
        oriFilteredValues[1] = lowPass(oriValues[1], oriFilteredValues[1]);
        oriFilteredValues[2] = lowPass(oriValues[2], oriFilteredValues[2]);
        oriFilteredValues[0] = lowPass(oriValues[0], oriFilteredValues[0]);

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
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }


    //initialize acceleration:
    public void initializeAccelerometerGraph(){

        this.accelerometerGraph = (GraphView) findViewById(R.id.accelerometerGraph);
        //creating series for x axis plot
        this.seriesAccX = new LineGraphSeries<DataPoint>();
        this.seriesAccX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesAccY = new LineGraphSeries<DataPoint>();
        this.seriesAccY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesAccZ = new LineGraphSeries<DataPoint>();
        this.seriesAccZ.setColor(Color.GREEN);

        // legend
        this.seriesAccX.setTitle("xAxis");
        this.seriesAccY.setTitle("yAxis");
        this.seriesAccZ.setTitle("zAxis");

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
        this.accelerometerGraph.addSeries(this.seriesAccX);
        this.accelerometerGraph.addSeries(this.seriesAccY);
        this.accelerometerGraph.addSeries(this.seriesAccZ);

        //scrolling and scaling setting
        this.accelerometerGraph.getViewport().setScalable(true);
        this.accelerometerGraph.getViewport().setScrollable(true);
        this.accelerometerGraph.getViewport().scrollToEnd();

        this.accelerometerGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        //this.accelerometerGraph.getGridLabelRenderer().setVerticalAxisTitle("accelerometer values");
        this.accelerometerGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update accelerometer data
    public void updateAccelerometerGraph(){

        this.indexAcc=this.indexAcc+1;

        this.seriesAccX.appendData(new DataPoint(indexAcc, this.accelerometerDataList.get(this.indexAcc).getxAxisValue()),
                true, 20);

        this.seriesAccY.appendData(new DataPoint(indexAcc, this.accelerometerDataList.get(this.indexAcc).getyAxisValue()),
                true, 20);

        this.seriesAccZ.appendData(new DataPoint(indexAcc, this.accelerometerDataList.get(this.indexAcc).getzAxisValue()),
                true, 20);
    }



    //initialize magnetic field:
    public void initializeMagneticFieldGraph(){

        this.magneticfieldGraph = (GraphView) findViewById(R.id.magfieldGraph);
        //creating series for x axis plot
        this.seriesMagX = new LineGraphSeries<DataPoint>();
        this.seriesMagX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesMagY = new LineGraphSeries<DataPoint>();
        this.seriesMagY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesMagZ = new LineGraphSeries<DataPoint>();
        this.seriesMagZ.setColor(Color.GREEN);

        // legend
        this.seriesMagX.setTitle("xAxis");
        this.seriesMagY.setTitle("yAxis");
        this.seriesMagZ.setTitle("zAxis");

        this.magneticfieldGraph.getLegendRenderer().setVisible(true);
        this.magneticfieldGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.magneticfieldGraph.getViewport().setXAxisBoundsManual(true);
        this.magneticfieldGraph.getViewport().setMinX(0);
        this.magneticfieldGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.magneticfieldGraph.getViewport().setYAxisBoundsManual(true);
        this.magneticfieldGraph.getViewport().setMinY(-120);
        this.magneticfieldGraph.getViewport().setMaxY(120);
        this.magneticfieldGraph.addSeries(this.seriesMagX);
        this.magneticfieldGraph.addSeries(this.seriesMagY);
        this.magneticfieldGraph.addSeries(this.seriesMagZ);

        //scrolling and scaling setting
        this.magneticfieldGraph.getViewport().setScalable(true);
        this.magneticfieldGraph.getViewport().setScrollable(true);
        this.magneticfieldGraph.getViewport().scrollToEnd();

        this.magneticfieldGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");

        this.magneticfieldGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update magneticfield data
    public void updateMagneticFieldGraph(){

        this.indexMag=this.indexMag+1;

        this.seriesMagX.appendData(new DataPoint(indexMag, this.magneticfieldDataList.get(this.indexMag).getxAxisValue()),
                true, 20);

        this.seriesMagY.appendData(new DataPoint(indexMag, this.magneticfieldDataList.get(this.indexMag).getyAxisValue()),
                true, 20);

        this.seriesMagZ.appendData(new DataPoint(indexMag, this.magneticfieldDataList.get(this.indexMag).getzAxisValue()),
                true, 20);
    }


    //initialize orientation:
    public void initializeOrientationGraph() {

        this.orientationGraph = (GraphView) findViewById(R.id.orientationGraph);
        //creating series for x axis plot
        this.seriesOriX = new LineGraphSeries<DataPoint>();
        this.seriesOriX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesOriY = new LineGraphSeries<DataPoint>();
        this.seriesOriY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesOriZ = new LineGraphSeries<DataPoint>();
        this.seriesOriZ.setColor(Color.GREEN);

        // legend
        this.seriesOriX.setTitle("Pitch");
        this.seriesOriY.setTitle("Roll");
        this.seriesOriZ.setTitle("Azimuth");

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
        this.orientationGraph.addSeries(this.seriesOriX);
        this.orientationGraph.addSeries(this.seriesOriY);
        this.orientationGraph.addSeries(this.seriesOriZ);

        //scrolling and scaling setting
        this.orientationGraph.getViewport().setScalable(true);
        this.orientationGraph.getViewport().setScrollable(true);
        this.orientationGraph.getViewport().scrollToEnd();

        this.orientationGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        this.orientationGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    public void updateOrientationGraph() {

        this.indexOri = this.indexOri + 1;

        this.seriesOriX.appendData(new DataPoint(indexOri, this.orientationDataList.get(this.indexOri).getxAxisValue()),
                true, 20);

        this.seriesOriY.appendData(new DataPoint(indexOri, this.orientationDataList.get(this.indexOri).getyAxisValue()),
                true, 20);

        this.seriesOriZ.appendData(new DataPoint(indexOri, this.orientationDataList.get(this.indexOri).getzAxisValue()),
                true, 20);
    }


    /*---------------------------lowpass filter----------------------------*/
    public float lowPass(float current, float last){
        float alpha = 0.12f;
        return last * (1.0f - alpha) + current * alpha;
    }

    //initialize filtered acceleration:
    public void initializeAccelerometerFilteredGraph(){

        this.accelerometerFilteredGraph = (GraphView) findViewById(R.id.accelerometerFilteredGraph);
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

        this.accelerometerFilteredGraph.getLegendRenderer().setVisible(true);
        this.accelerometerFilteredGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.accelerometerFilteredGraph.getViewport().setXAxisBoundsManual(true);
        this.accelerometerFilteredGraph.getViewport().setMinX(0);
        this.accelerometerFilteredGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.accelerometerFilteredGraph.getViewport().setYAxisBoundsManual(true);
        this.accelerometerFilteredGraph.getViewport().setMinY(-15);
        this.accelerometerFilteredGraph.getViewport().setMaxY(15);
        this.accelerometerFilteredGraph.addSeries(this.seriesFilteredAccX);
        this.accelerometerFilteredGraph.addSeries(this.seriesFilteredAccY);
        this.accelerometerFilteredGraph.addSeries(this.seriesFilteredAccZ);

        //scrolling and scaling setting
        this.accelerometerFilteredGraph.getViewport().setScalable(true);
        this.accelerometerFilteredGraph.getViewport().setScrollable(true);
        this.accelerometerFilteredGraph.getViewport().scrollToEnd();

        this.accelerometerFilteredGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");
        this.accelerometerFilteredGraph.getGridLabelRenderer().setHighlightZeroLines(true);

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



    //initialize filtered magnetic field
    public void initializeMagneticFieldFilteredGraph(){

        this.magneticfieldFilteredGraph = (GraphView) findViewById(R.id.magfieldFilteredGraph);
        //creating series for x axis plot
        this.seriesFilteredMagX = new LineGraphSeries<DataPoint>();
        this.seriesFilteredMagX.setColor(Color.RED);

        //creating series for y axis plot
        this.seriesFilteredMagY = new LineGraphSeries<DataPoint>();
        this.seriesFilteredMagY.setColor(Color.BLUE);

        //creating series for z axis plot
        this.seriesFilteredMagZ = new LineGraphSeries<DataPoint>();
        this.seriesFilteredMagZ.setColor(Color.GREEN);

        // legend
        this.seriesFilteredMagX.setTitle("xAxis");
        this.seriesFilteredMagY.setTitle("yAxis");
        this.seriesFilteredMagZ.setTitle("zAxis");

        this.magneticfieldFilteredGraph.getLegendRenderer().setVisible(true);
        this.magneticfieldFilteredGraph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.BOTTOM);

        // set manual X bounds
        this.magneticfieldFilteredGraph.getViewport().setXAxisBoundsManual(true);
        this.magneticfieldFilteredGraph.getViewport().setMinX(0);
        this.magneticfieldFilteredGraph.getViewport().setMaxX(20);

        // set manual Y bounds
        this.magneticfieldFilteredGraph.getViewport().setYAxisBoundsManual(true);
        this.magneticfieldFilteredGraph.getViewport().setMinY(-120);
        this.magneticfieldFilteredGraph.getViewport().setMaxY(120);
        this.magneticfieldFilteredGraph.addSeries(this.seriesFilteredMagX);
        this.magneticfieldFilteredGraph.addSeries(this.seriesFilteredMagY);
        this.magneticfieldFilteredGraph.addSeries(this.seriesFilteredMagZ);

        //scrolling and scaling setting
        this.magneticfieldFilteredGraph.getViewport().setScalable(true);
        this.magneticfieldFilteredGraph.getViewport().setScrollable(true);
        this.magneticfieldFilteredGraph.getViewport().scrollToEnd();

        this.magneticfieldFilteredGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");

        this.magneticfieldFilteredGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update filtered magnetic field
    public void updateMagneticFieldFilteredGraph(){

        this.indexFilteredMag=this.indexFilteredMag+1;

        this.seriesFilteredMagX.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getxAxisValue()),
                true, 20);

        this.seriesFilteredMagY.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getyAxisValue()),
                true, 20);

        this.seriesFilteredMagZ.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getzAxisValue()),
                true, 20);
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

    /*-----------------------Button onClickListener--------------------------*/
    private View.OnClickListener btsaveClicked = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            //saving data onto a file
            doLogging = false;
            if (filterEachOn == 0) { //filter on
                Toast.makeText(AllDataPlot.this, "raw data saved", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){ //filter off
                Toast.makeText(AllDataPlot.this, "filtered data saved", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private View.OnClickListener btlogClicked = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            doLogging = true;
            if (filterEachOn == 0) { //filter on
                deleteFile("AllRawData.csv");
                //delete previous data every time to avoid influencing test each time
                Toast.makeText(AllDataPlot.this, "logging raw data", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){ //filter off
                deleteFile("AllFilteredData.csv");
                Toast.makeText(AllDataPlot.this, "logging filtered data", Toast.LENGTH_SHORT).show();
            }
        }
    };


    public void writeFile() throws InterruptedException { //write data into file

        if (filterEachOn == 0) { //record raw data
            FileOutputStream outputStream = null;
            String fileName = "AllRawData.csv";
            String fileContents = accValues[0] + "," + accValues[1] + "," + accValues[2] + ","
                    + magValues[0] + "," + magValues[1] + "," + magValues[2] + ","
                    + oriValues[1] + "," + oriValues[2] + "," + oriValues[0] + "\n";

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
            String fileName = "AllFilteredData.csv";
            String fileContents = accValues[0] + "," + accFilteredValues[1] + "," + accFilteredValues[2] + ","
                    + magFilteredValues[0] + "," + magFilteredValues[1] + "," + magFilteredValues[2] + ","
                    + oriFilteredValues[1] + "," + oriFilteredValues[2] + "," + oriFilteredValues[0] + "\n";

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

}
