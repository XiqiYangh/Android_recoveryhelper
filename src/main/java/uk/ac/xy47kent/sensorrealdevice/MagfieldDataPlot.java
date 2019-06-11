package uk.ac.xy47kent.sensorrealdevice;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
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

public class MagfieldDataPlot extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = "SensorButtons";

    private SensorManager sensorManager;
    Sensor magneticfieldSensor;
    TextView xMagVal, yMagVal, zMagVal;
    float magValues[] = new float[3];

    private List<SensorClass> magneticfieldDataList;
    private SensorClass magneticfieldData;

    private GraphView magneticfieldGraph;
    private LineGraphSeries<DataPoint> seriesX;
    private LineGraphSeries<DataPoint> seriesY;
    private LineGraphSeries<DataPoint> seriesZ;

    int index=0;
    long curTime;
    long diffTime;
    long lastUpdate= System.currentTimeMillis();

    Button doSave,doLog;
    private boolean doLogging = false;

    /*----------------------------filter--------------------------------------*/
    private SwitchCompat filterMagSwitch;
    private TextView xFilteredMagVal,yFilteredMagVal,zFilteredMagVal;

    private float[] magFilteredValues = new float[3];

    private List<SensorClass> magneticfieldFilteredDataList;
    private SensorClass magneticfieldFilteredData;
    private GraphView magneticfieldFilteredGraph;
    private LineGraphSeries<DataPoint> seriesFilteredMagX;
    private LineGraphSeries<DataPoint> seriesFilteredMagY;
    private LineGraphSeries<DataPoint> seriesFilteredMagZ;

    int indexFilteredMag = 0;

    int filterEachOn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_magfield_data_plot);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //cquire magfield data
        xMagVal = (TextView) findViewById(R.id.xMagVal);
        yMagVal = (TextView) findViewById(R.id.yMagVal);
        zMagVal = (TextView) findViewById(R.id.zMagVal);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        this.magneticfieldSensor = this.sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        if (magneticfieldSensor != null){
            this.magneticfieldDataList = new ArrayList<SensorClass>();
            this.magneticfieldDataList.add(new SensorClass(0, 0, 0, 0, 0));
            initializeMagneticFieldGraph();

            /*------------------filtered acceleration-------------------*/
            this.magneticfieldFilteredDataList = new ArrayList<SensorClass>();
            this.magneticfieldFilteredDataList.add(new SensorClass(0,0,0,0,0));
            initializeMagneticFieldFilteredGraph();

        } else {
            Toast.makeText(MagfieldDataPlot.this, "MagneticField Unavailable",Toast.LENGTH_SHORT).show();
        }

        doSave = (Button)findViewById(R.id.btsaveMag);
        doSave.setOnClickListener(btsaveClicked);

        doLog = (Button)findViewById(R.id.btlogMag);
        doLog.setOnClickListener(btlogClicked);

        /*------------------------filter switch--------------------*/
        xFilteredMagVal = (TextView)findViewById(R.id.xFilteredMagVal);
        yFilteredMagVal = (TextView)findViewById(R.id.yFilteredMagVal);
        zFilteredMagVal = (TextView)findViewById(R.id.zFilteredMagVal);

        filterMagSwitch = (SwitchCompat) findViewById(R.id.filterMagSwitch);
        filterMagSwitch.setChecked(false);
        filterMagSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    //filtered Acc Data
                    magneticfieldGraph.setVisibility(View.INVISIBLE);
                    magneticfieldFilteredGraph.setVisibility(View.VISIBLE);//filtered
                    xMagVal.setVisibility(View.INVISIBLE);
                    xFilteredMagVal.setVisibility(View.VISIBLE);//filtered
                    yMagVal.setVisibility(View.INVISIBLE);
                    yFilteredMagVal.setVisibility(View.VISIBLE);//filtered
                    zMagVal.setVisibility(View.INVISIBLE);
                    zFilteredMagVal.setVisibility(View.VISIBLE);//filtered
                    filterEachOn = 1;

                } else{
                    //raw Acc data
                    magneticfieldGraph.setVisibility(View.VISIBLE);//raw
                    magneticfieldFilteredGraph.setVisibility(View.INVISIBLE);
                    xMagVal.setVisibility(View.VISIBLE);//raw
                    xFilteredMagVal.setVisibility(View.INVISIBLE);
                    yMagVal.setVisibility(View.VISIBLE);//raw
                    yFilteredMagVal.setVisibility(View.INVISIBLE);
                    zMagVal.setVisibility(View.VISIBLE);//raw
                    zFilteredMagVal.setVisibility(View.INVISIBLE);
                    filterEachOn = 0;
                }
            }
        });

    }


    @Override
    public void onSensorChanged(SensorEvent event) {

        Log.d(TAG, "onSensorChanged: X:" + event.values[0]+ "\n"+"Y:"+ event.values[1]+"\n" + "Z:" +event.values[2]);

        xMagVal.setText("xMagVal:" + event.values[0]);
        yMagVal.setText("yMagVal:" + event.values[1]);
        zMagVal.setText("zMagVal:" + event.values[2]);

        magValues = event.values.clone();

        this.magneticfieldData = new SensorClass();
        this.magneticfieldData.setxAxisValue(event.values[0]);
        this.magneticfieldData.setyAxisValue(event.values[1]);
        this.magneticfieldData.setzAxisValue(event.values[2]);
        this.magneticfieldData.setAccuracy(event.accuracy);

        //calculating time lapse
        this.curTime = System.currentTimeMillis();
        diffTime = (curTime - this.lastUpdate);
        this.lastUpdate = curTime ;

        //setting time lapse between consecutive datapoints
        this.magneticfieldData.setTimestamp(diffTime);

        //adding the class to the list of magneticfield data points
        this.magneticfieldDataList.add(magneticfieldData);

        //updating graph display
        updateMagneticFieldGraph();

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            if (doLogging){
                try {
                    writeFile();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /*--------------------filtered acceleration-----------------------*/
        magFilteredValues[0] = lowPass(magValues[0],magFilteredValues[0]);
        magFilteredValues[1] = lowPass(magValues[1],magFilteredValues[1]);
        magFilteredValues[2] = lowPass(magValues[2],magFilteredValues[2]);

        xFilteredMagVal.setText("xMagVal:" + magFilteredValues[0]);
        yFilteredMagVal.setText("yMagVal:" + magFilteredValues[1]);
        zFilteredMagVal.setText("zMagVal:" + magFilteredValues[2]);

        this.magneticfieldFilteredData = new SensorClass();
        this.magneticfieldFilteredData.setxAxisValue(magFilteredValues[0]);
        this.magneticfieldFilteredData.setyAxisValue(magFilteredValues[1]);
        this.magneticfieldFilteredData.setzAxisValue(magFilteredValues[2]);
        this.magneticfieldFilteredData.setAccuracy(event.accuracy);
        this.magneticfieldFilteredDataList.add(magneticfieldFilteredData);
        updateMagneticFieldFilteredGraph();

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, magneticfieldSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        sensorManager.unregisterListener(this);
    }


    public void initializeMagneticFieldGraph(){

        this.magneticfieldGraph = (GraphView) findViewById(R.id.magfieldGraph);
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
        this.magneticfieldGraph.addSeries(this.seriesX);
        this.magneticfieldGraph.addSeries(this.seriesY);
        this.magneticfieldGraph.addSeries(this.seriesZ);

        //scrolling and scaling setting
        this.magneticfieldGraph.getViewport().setScalable(true);
        this.magneticfieldGraph.getViewport().setScrollable(true);
        this.magneticfieldGraph.getViewport().scrollToEnd();

        this.magneticfieldGraph.getGridLabelRenderer().setHorizontalAxisTitle("Time/0.2s");

        this.magneticfieldGraph.getGridLabelRenderer().setHighlightZeroLines(true);

    }

    //update magneticfield data
    public void updateMagneticFieldGraph(){

        this.index=this.index+1;
        System.out.println("update");

        this.seriesX.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.magneticfieldDataList.get(this.index).getxAxisValue()),
                true, 20);

        this.seriesY.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.magneticfieldDataList.get(this.index).getyAxisValue()),
                true, 20);

        this.seriesZ.appendData(new com.jjoe64.graphview.series.DataPoint(index, this.magneticfieldDataList.get(this.index).getzAxisValue()),
                true, 20);
    }


    public void writeFile() throws InterruptedException { //write data into file

        if (filterEachOn == 0) { //record raw data
            FileOutputStream outputStream = null;
            String fileName = "magneticfieldRawData.csv";
            String fileContents = magValues[0] + "," + magValues[1] + "," + magValues[2] + "\n";

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
        } else if (filterEachOn == 1){ //record filtered data
            FileOutputStream outputStream = null;
            String fileName = "magneticfieldFilteredData.csv";
            String fileContents = magFilteredValues[0] + "," + magFilteredValues[1] + "," + magFilteredValues[2] + "\n";

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
                deleteFile("magneticfieldData.csv");//delete previous data every time to avoid influencing test each time
                Toast.makeText(MagfieldDataPlot.this, "logging raw data", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                deleteFile("magneticfieldFilteredData.csv");
                Toast.makeText(MagfieldDataPlot.this, "logging filtered data", Toast.LENGTH_SHORT).show();
            }
        }
    };


    private View.OnClickListener btsaveClicked = new View.OnClickListener(){

        @Override
        public void onClick(View v) {
            //saving data onto a file
            doLogging = false;
            if (filterEachOn == 0) {
                Toast.makeText(MagfieldDataPlot.this, "raw data saved", Toast.LENGTH_SHORT).show();
            } else if (filterEachOn == 1){
                Toast.makeText(MagfieldDataPlot.this, "filtered data saved", Toast.LENGTH_SHORT).show();
            }
        }
    };


    /*---------------------------lowpass filter----------------------------*/
    public float lowPass(float current, float last){
        float alpha = 0.12f;
        return last * (1.0f - alpha) + current * alpha;
    }

    //initialize filtered acceleration:
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

    //update filtered accelerometer data
    public void updateMagneticFieldFilteredGraph(){

        this.indexFilteredMag=this.indexFilteredMag+1;

        this.seriesFilteredMagX.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getxAxisValue()),
                true, 20);

        this.seriesFilteredMagY.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getyAxisValue()),
                true, 20);

        this.seriesFilteredMagZ.appendData(new DataPoint(indexFilteredMag, this.magneticfieldFilteredDataList.get(this.indexFilteredMag).getzAxisValue()),
                true, 20);
    }


}
