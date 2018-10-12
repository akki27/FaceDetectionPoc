package com.akki.facedetectionpoc.helper;

import android.app.Application;
import android.content.SharedPreferences;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;

/**
 * Created by e01106 on 5/4/2017.
 */
public class FaceDetectionApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        sFaceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));

        faceDataPreferences = getSharedPreferences(getPackageName() + "_faceDataPreferences", MODE_PRIVATE);
    }

    public static FaceServiceClient getFaceServiceClient() {
        return sFaceServiceClient;
    }

    private static FaceServiceClient sFaceServiceClient;

    public static SharedPreferences faceDataPreferences;

}
