package com.akki.facedetectionpoc.services;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.akki.facedetectionpoc.data.Preferences;
import com.akki.facedetectionpoc.helper.FaceDetectionApp;
import com.akki.facedetectionpoc.utils.Utils;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.rest.ClientException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by SadyAkki on 5/10/2017.
 */

public class DetectionService  extends Service {

    static ResultReceiver resultReceiver;

    private SurfaceHolder sHolder;
    private static Camera mCamera;
    private Camera.Parameters parameters;
    private static boolean inPreview = false;
    private static volatile AtomicBoolean faceProcessing = new AtomicBoolean(false);

    public static final int STATUS_FACE_FOUND = 0;
    public static final int STATUS_FACE_NOT_FOUND = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_NO_FACE_DETECTION_SUPPORT = 3;

    boolean faceDetectionRunning = false;
    private long mReferenceTime = 0;
    private long currTime = 0;
    byte[] currPreviewData = null;

    int countPreviewCall = 0;
    int countFdCall = 0;
    boolean fdSuccess = false;


    private static final String TAG = "DetectionService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand : " +intent);

        if(safeCameraOpen()) {
            return START_STICKY;
        } else {
            return START_NOT_STICKY;
        }

    }

    private boolean safeCameraOpen() {
        boolean camOpened = false;
        float[] distances = new float[3];

        try {
            releaseCameraAndPreview();
            mCamera = Camera.open();
            camOpened = (mCamera != null);

            //mCamera.setDisplayOrientation(90);
            Camera.Parameters params = mCamera.getParameters();
            params.getFocusDistances(distances);
            Log.d(TAG, "Focus Mode: "+params.getFocusMode());
            Log.d(TAG, "focus distance near: "+ Float.toString(distances[0]));
            Log.d(TAG, "focus distance optimum: "+ Float.toString(distances[1]));
            Log.d(TAG, "focus distance far: " +distances.length);



            params.setPreviewSize(640, 480); //TODO: set the required preview size
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            params.setPictureFormat(ImageFormat.JPEG);
            mCamera.setParameters(params);

            if (params.getMaxNumDetectedFaces() <= 0) {
                Toast.makeText(DetectionService.this, "Face Detection Not Supported!",
                        Toast.LENGTH_SHORT).show();
                camOpened = false;
            } else {
                mCamera.startPreview();
                mCamera.setPreviewCallback(previewCallback);
                mCamera.startFaceDetection();
                mCamera.setFaceDetectionListener(faceDetectionListener);
                camOpened = true;
                Log.d(TAG, "Camera opened successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "failed to open Camera");
            e.printStackTrace();
        }
        return camOpened;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            mCamera.stopFaceDetection();
            mCamera.setFaceDetectionListener(null);
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera cam) {
            if (data == null) return;
            currPreviewData = data;
            /*if(fdSuccess) {
                countPreviewCall++;
                Log.d(TAG, "CurCountPreview: " +countPreviewCall + ":: CurTimePreview: " +System.currentTimeMillis());
            }*/
        }
    };

    private Camera.FaceDetectionListener faceDetectionListener = new Camera.FaceDetectionListener() {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {
            if(currPreviewData !=null) {
                if(faces.length >0) {
                    /*fdSuccess = true;
                    countFdCall++;
                    Log.d(TAG, "CurCountFD: " +countFdCall + ":: CurTimeFD: " +System.currentTimeMillis());*/

                    currTime = System.currentTimeMillis();
                    if (currTime > (mReferenceTime + Preferences.FACE_DETECTION_DELAY)) {   //TODO: decide PICTURE_DELAY value
                        mReferenceTime = currTime;
                        if (!faceProcessing.compareAndSet(false, true)) return;
                        Log.d(TAG, "FACECOUNT: " +faces.length);

                        Camera.Size camPreviewSize = camera.getParameters().getPreviewSize();
                        int[] fdImg = Utils.decodeYUV420SPtoRGB(currPreviewData, camPreviewSize.width, camPreviewSize.height);
                        Bitmap mBitmap = Utils.rgbToBitmap(fdImg, camPreviewSize.width, camPreviewSize.height);

                        Log.d(TAG, "Starting detection thread At: " +currTime);
                        //Start new thread to request for face feature
                        FaceFeatureDetectionThread fdThread = new FaceFeatureDetectionThread(mBitmap);
                        fdThread.start();
                    }
                }else {
                    /*fdSuccess = false;
                    Log.d(TAG, "CurCountFD_Else: " +countFdCall + ":: CurTimeFD_else: " +System.currentTimeMillis());*/
                }
            }
        }
    };

    private class FaceFeatureDetectionThread extends Thread {
        private Bitmap bitmap;
        public FaceFeatureDetectionThread(Bitmap bitmap) {
            this.bitmap = bitmap;
        }

        @Override
        public void run() {

            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                Matrix matrix = new Matrix();
                matrix.postRotate(90);
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true).compress(Bitmap.CompressFormat.JPEG, 100, output);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

                //Save current detected face for now to test
                saveDetectedFace(bitmap);

                //Detect face feature
                if(isInternetAvailable()) {
                    detectFaceFeature(inputStream);
                }
            }catch (Exception e) {
                e.printStackTrace();
            } finally {
                //faceProcessing.set(false);
                faceProcessing.compareAndSet(true, false);
                //faceDetectionRunning = false;
            }
        }

        public boolean isInternetAvailable() {
            try {
                InetAddress ipAddr = InetAddress.getByName("google.com");
                return !ipAddr.equals("");
            } catch (Exception e) {
                return false;
            }
        }

        private void detectFaceFeature(InputStream inputStream) throws ClientException, IOException {
            Log.d(TAG, "detectFaceFeature()_called");
            FaceServiceClient faceServiceClient = FaceDetectionApp.getFaceServiceClient();

            boolean mSucceed = true;
            Face[] result = null;
            try {
                result = faceServiceClient.detect(
                        inputStream,  /* Input stream of image to detect */
                        true,       /* Whether to return face ID */
                        true,       /* Whether to return face landmarks */
                        /* Which face attributes to analyze, currently This support:
                           age,gender,headPose,smile,facialHair */
                        new FaceServiceClient.FaceAttributeType[] {
                                FaceServiceClient.FaceAttributeType.Age,
                                FaceServiceClient.FaceAttributeType.Gender
                        });
            }catch (Exception e) {
                mSucceed = false;
                e.printStackTrace();
            }

            if(mSucceed) {
                List<Face> faces = Arrays.asList(result);
                Log.d(TAG, "MSAPI_ReturnFaces: " + faces.size());

                if(faces.size() >0) {
                    JSONArray jsonArray = new JSONArray();
                    for (Face face : faces) {
                        double age = face.faceAttributes.age;
                        String gender = face.faceAttributes.gender;
                        JSONObject resultObject = new JSONObject();
                        try {
                            resultObject.put("age", age);
                            resultObject.put("gender", gender);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        jsonArray.put(resultObject);
                    }

                    Log.d(TAG, "RESULT_JSonString: " + jsonArray.toString());
                    //Save result to preference
                    FaceDetectionApp.faceDataPreferences.edit().putString(Preferences.FACE_DATA_PREFERENCE_KEY, jsonArray.toString()).apply();

                    /*SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(DetectionService.this);
                    preferences.edit().putString(Preferences.FACE_DATA_PREFERENCE_KEY, jsonArray.toString());*/

                } else {
                    Log.d(TAG, "MSApiReturnFaceCount = 0");
                }
            }else {
                Log.d(TAG, "MSApi_Exception");
            }
        }
    };


    private static void saveDetectedFace(Bitmap bitmap) {
        if(bitmap != null) {
            String name = "detectedFace";
            File photo = new File(Environment.getExternalStorageDirectory(), name + ".jpg");
            if (photo.exists()) photo.delete();
            try {
                FileOutputStream fos = new FileOutputStream(photo.getPath());
                try {
                    //Rotate pic by 90 degree before saving
                    Matrix matrix = new Matrix();
                    matrix.postRotate(90);
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true)
                            .compress(Bitmap.CompressFormat.JPEG, 100, fos);
                }catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }

                fos.close();
                Log.d(TAG, "Photo Saved");
            } catch (java.io.IOException e) {
                Log.e("PictureDemo", "Exception in photoCallback", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()_called");
        return null;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()_called");

        /*if(mCamera !=null) {
            //TODo: with this getting crash on back pressed
            *//*if (inPreview){
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                inPreview = false;
            }*//*
            mCamera.release();
            mCamera = null;
        }*/
    }

    public void stopFaceDetection() {
        Log.d(TAG, "stopFaceDetection()_called");
        mCamera.stopPreview();
        if (faceDetectionRunning) {
            mCamera.stopFaceDetection();
        }
    }

    public void startFaceDetection() {
        Log.d(TAG, "startFaceDetection()_called");
    }
}