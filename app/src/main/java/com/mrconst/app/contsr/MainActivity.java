package com.mrconst.app.contsr;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity  implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = MainActivity.class.getName();
    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private void _redFilter(Mat hsv, Mat dst) {
        Mat lower = new Mat();
        Mat upper = new Mat();
        Core.inRange(hsv, new Scalar(0, 60, 60), new Scalar(20, 255, 255), lower);
        Core.inRange(hsv, new Scalar(160, 60, 60), new Scalar(179, 255, 255), upper);

        Core.addWeighted(lower, 1.0, upper, 1.0, 0.0, dst);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Mat orig = new Mat();
        rgba.copyTo(orig);
        Imgproc.medianBlur(rgba, rgba, 5);
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2HLS);
        _redFilter(rgba, rgba);
        Imgproc.medianBlur(rgba, rgba, 5);

        Mat circles = new Mat();
        Imgproc.HoughCircles(rgba, circles, Imgproc.HOUGH_GRADIENT, 1.0, 20.0, 50, 30, 10, 100);

        for(int i = 0; i < circles.cols(); i++) {
            double[] circ = circles.get(0, i);
            if (circ == null)
                break;
            Point pt = new Point(Math.round(circ[0]), Math.round(circ[1]));
            int r = (int) Math.round(circ[2]);

            Imgproc.rectangle(orig, new Point(pt.x - r - 5, pt.y - r - 5), new Point(pt.x + r + 5, pt.y + r + 5), new Scalar(255, 0, 255), 3);
        }

        return orig;
    }
}
