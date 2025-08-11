import java.util.concurrent.atomic.AtomicBoolean;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class BO3R_Create_A_Real_T extends CameraBridgeViewBase implements SensorEventListener {
    private Mat rgba, grayscale, blurred, canny, contours;
    private SensorManager sensorManager;
    private Sensor accelerometer, gyroscope;
    private AtomicBoolean isTracking = new AtomicBoolean(false);
    private float[] rotationMatrix, orientationValues, accelerometerValues, gyroscopeValues;
    private double x, y, z;
    
    public BO3R_Create_A_Real_T(Context context, AttributeSet attrs) {
        super(context, attrs);
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        
        rgba = new Mat();
        grayscale = new Mat();
        blurred = new Mat();
        canny = new Mat();
        contours = new Mat();
        
        rotationMatrix = new float[16];
        orientationValues = new float[3];
        accelerometerValues = new float[3];
        gyroscopeValues = new float[3];
    }
    
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            System.arraycopy(event.values, 0, accelerometerValues, 0, 3);
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE)
            System.arraycopy(event.values, 0, gyroscopeValues, 0, 3);
        
        SensorManager.getRotationMatrixFromVector(rotationMatrix, gyroscopeValues);
        SensorManager.getOrientation(rotationMatrix, orientationValues);
        
        x = (double) Math.toDegrees(orientationValues[0]);
        y = (double) Math.toDegrees(orientationValues[1]);
        z = (double) Math.toDegrees(orientationValues[2]);
    }
    
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        rgba = inputFrame.rgba();
        
        Imgproc.cvtColor(rgba, grayscale, Imgproc.COLOR_RGBA2GRAY);
        Imgproc.GaussianBlur(grayscale, blurred, new Size(5, 5), 0);
        Imgproc.Canny(blurred, canny, 50, 150);
        
        contours = new Mat();
        Imgproc.findContours(canny, contours, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        
        if (isTracking.compareAndSet(false, true)) {
            for (int i = 0; i < contours.rows(); i++) {
                double area = Imgproc.contourArea(contours, i);
                if (area > 1000) {
                    Imgproc.drawContours(rgba, contours, i, new Scalar(0, 255, 0), 2);
                }
            }
        }
        
        return rgba;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }
    
    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }
}