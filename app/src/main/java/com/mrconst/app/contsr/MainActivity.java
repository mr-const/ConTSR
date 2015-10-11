package com.mrconst.app.contsr;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

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
    private static final int VM_NORMAL = 0;
    private static final int VM_HSV = 1;

    private CameraBridgeViewBase mOpenCvCameraView;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.setMaxFrameSize(960, 540);
                    mOpenCvCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
    private int mVMode = VM_NORMAL;
    private int mHLowerMax = 14;
    private int mHUpperMin = 160;
    private int mSMin = 80;
    private int mVMin = 80;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.video_mode, menu);
        inflater.inflate(R.menu.settings, menu);

        int id;
        switch(mVMode) {
            case VM_NORMAL:
                id = R.id.menu_vm_normal;
                break;
            case VM_HSV:
                id = R.id.menu_vm_hsv;
                break;
            default:
                throw new AssertionError("Invalid video mode: " + mVMode);
        }
        menu.findItem(id).setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_vm_normal:
                mVMode = VM_NORMAL;
                item.setChecked(true);
                return true;
            case R.id.menu_vm_hsv:
                mVMode = VM_HSV;
                item.setChecked(true);
                return true;
            case R.id.menu_settings_hsv:
                _popHsvDialog();
                return true;
            default:
                return false;
        }
    }

    private void _popHsvDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.settings_hsv);
        View v = getLayoutInflater().inflate(R.layout.dialog_hsv_settings, null);
        SeekBar hLowerMax = (SeekBar)v.findViewById(R.id.h_lower_max);
        SeekBar hUpperMin = (SeekBar)v.findViewById(R.id.h_upper_min);
        SeekBar sMin = (SeekBar)v.findViewById(R.id.s_min);
        SeekBar vMin = (SeekBar)v.findViewById(R.id.v_min);

        hLowerMax.setProgress(mHLowerMax);
        hUpperMin.setProgress(mHUpperMin);
        sMin.setProgress(mSMin);
        vMin.setProgress(mVMin);

        HsvSeekBarListener l = new HsvSeekBarListener((TextView)v.findViewById(R.id.h_lower_max_str),
                (TextView)v.findViewById(R.id.h_upper_min_str),
                (TextView)v.findViewById(R.id.s_min_str),
                (TextView)v.findViewById(R.id.v_min_str));
        hLowerMax.setOnSeekBarChangeListener(l);
        hUpperMin.setOnSeekBarChangeListener(l);
        sMin.setOnSeekBarChangeListener(l);
        vMin.setOnSeekBarChangeListener(l);

        builder.setView(v);

        AlertDialog d = builder.create();
        d.show();
    }


    public void onCameraViewStarted(int width, int height) {
    }

    public void onCameraViewStopped() {
    }

    private void _redFilter(Mat hsv, Mat dst) {
        Mat lower = new Mat();
        Mat upper = new Mat();
        Core.inRange(hsv, new Scalar(0, mSMin, mVMin), new Scalar(mHLowerMax, 255, 255), lower);
        Core.inRange(hsv, new Scalar(mHUpperMin, mSMin, mVMin), new Scalar(179, 255, 255), upper);

        Core.addWeighted(lower, 1.0, upper, 1.0, 0.0, dst);
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat rgba = inputFrame.rgba();
        Mat orig = new Mat();
        rgba.copyTo(orig);
        Imgproc.medianBlur(rgba, rgba, 5);
        Imgproc.cvtColor(rgba, rgba, Imgproc.COLOR_RGB2HLS, 3);
        _redFilter(rgba, rgba);
        Imgproc.medianBlur(rgba, rgba, 5);

        Mat circles = new Mat();
        Imgproc.HoughCircles(rgba, circles, Imgproc.HOUGH_GRADIENT, 1.0, 20.0, 50, 30, 10, 100);

        Mat dst;
        if (mVMode == VM_NORMAL) {
            dst = orig;
            rgba.release();
        }
        else if (mVMode == VM_HSV) {
            orig.release();
            dst = rgba;
        }
        else {
            throw new AssertionError("Invalid VideoMode: " + mVMode);
        }

        for(int i = 0; i < circles.cols(); i++) {
            double[] circ = circles.get(0, i);
            if (circ == null)
                break;
            Point pt = new Point(Math.round(circ[0]), Math.round(circ[1]));
            int r = (int) Math.round(circ[2]);

            Imgproc.rectangle(dst, new Point(pt.x - r - 5, pt.y - r - 5), new Point(pt.x + r + 5, pt.y + r + 5), new Scalar(255, 0, 255), 3);
        }
        circles.release();

        return dst;
    }

    private class HsvSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private final TextView mHLowerMaxText;
        private final TextView mHUpperMinText;
        private final TextView mSMinText;
        private final TextView mVMinText;

        public HsvSeekBarListener(TextView hLowerMax, TextView hUpperMin, TextView sMin, TextView vMin) {
            mHLowerMaxText = hLowerMax;
            mHUpperMinText = hUpperMin;
            mSMinText = sMin;
            mVMinText = vMin;
            mHLowerMaxText.setText(getString(R.string.h_lower_max, mHLowerMax));
            mHUpperMinText.setText(getString(R.string.h_upper_min, mHUpperMin));
            mSMinText.setText(getString(R.string.s_min, mSMin));
            mVMinText.setText(getString(R.string.v_min, mVMin));
        }
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            switch(seekBar.getId()) {
                case R.id.h_lower_max:
                    mHLowerMax = progress;
                    mHLowerMaxText.setText(getString(R.string.h_lower_max, progress));
                    break;
                case R.id.h_upper_min:
                    mHUpperMin = progress;
                    mHUpperMinText.setText(getString(R.string.h_upper_min, progress));
                    break;
                case R.id.s_min:
                    mSMin = progress;
                    mSMinText.setText(getString(R.string.s_min, progress));
                    break;
                case R.id.v_min:
                    mVMin = progress;
                    mVMinText.setText(getString(R.string.v_min, progress));
                    break;
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    }
}
