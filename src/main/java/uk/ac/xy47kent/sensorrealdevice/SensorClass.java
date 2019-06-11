package uk.ac.xy47kent.sensorrealdevice;


//BAD RELEVANT
public class SensorClass {

    private float xAxisValue;
    private float yAxisValue;
    private float zAxisValue;
    private long timestamp;
    private float accuracy;

    //constructor
    public SensorClass(){
    }

    //initializing constructor
    public SensorClass(float xAxisValue, float yAxisValue, float zAxisValue, long timestamp, float accuracy) {
        this.xAxisValue = xAxisValue;
        this.yAxisValue = yAxisValue;
        this.zAxisValue = zAxisValue;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }

    public float getxAxisValue() {  // xAxis
        return xAxisValue;
    }

    public void setxAxisValue(float xAxisValue) {
        this.xAxisValue = xAxisValue;
    }

    public float getyAxisValue() {  // yAxis
        return yAxisValue;
    }

    public void setyAxisValue(float yAxisValue) {
        this.yAxisValue = yAxisValue;
    }

    public float getzAxisValue() {  // zAxis
        return zAxisValue;
    }

    public void setzAxisValue(float zAxisValue) {
        this.zAxisValue = zAxisValue;
    }

    public long getTimestamp() {  // timestamp
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getAccuracy() {  // accuracy
        return accuracy;
    }

    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
    }
}
