package com.akki.facedetectionpoc;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.akki.facedetectionpoc.data.Preferences;
import com.akki.facedetectionpoc.helper.FaceDetectionApp;
import com.akki.facedetectionpoc.services.DetectionService;
import com.akki.facedetectionpoc.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    Intent detectionServiceIntent;
    private TextView textView;
    private TextView resultSummaryTextView;
    private ListView listView;
    Button btn_startDemo;

    String oldPreferenceData = null;
    SharedPreferences faceDataPreference;

    boolean significantChanges = false;

    private static final String TAG = "MainActivity";

    // lists for permissions
    private ArrayList<String> permissionsToRequest;
    private ArrayList<String> permissionsRejected = new ArrayList<>();
    private ArrayList<String> permissions = new ArrayList<>();
    private static final int ALL_PERMISSIONS_RESULT = 1000;
    private boolean mAllPermissionGranted = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        faceDataPreference = FaceDetectionApp.faceDataPreferences;  //PreferenceManager.getDefaultSharedPreferences(this);
        faceDataPreference.registerOnSharedPreferenceChangeListener(spChangedListener);


        btn_startDemo = (Button) findViewById(R.id.btn_startDemo);
        btn_startDemo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "StartDetection");

                if(mAllPermissionGranted) {
                    resetResults();

                    setInfo(getString(R.string.detection_started));
                    detectionServiceIntent = new Intent(MainActivity.this, DetectionService.class);
                    startService(detectionServiceIntent);

                    btn_startDemo.setEnabled(false);
                    btn_startDemo.setClickable(false);
                } else {
                    Toast.makeText(MainActivity.this, "Permission Failed",
                            Toast.LENGTH_LONG).show();
                }

            }
        });

        Button btn_resetResult = (Button) findViewById(R.id.btn_reset);
        btn_resetResult.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetResults();
            }
        });

        permissionsToRequest = permissionsToRequest(permissions);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (permissionsToRequest.size() > 0) {
                requestPermissions(permissionsToRequest.toArray(
                        new String[permissionsToRequest.size()]), ALL_PERMISSIONS_RESULT);
            }
        }

    }

    private ArrayList<String> permissionsToRequest(ArrayList<String> wantedPermissions) {
        ArrayList<String> result = new ArrayList<>();

        for (String perm : wantedPermissions) {
            if (!hasPermission(perm)) {
                result.add(perm);
            }
        }

        return result;
    }

    private boolean hasPermission(String permission) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch(requestCode) {
            case ALL_PERMISSIONS_RESULT:
                for (String perm : permissionsToRequest) {
                    if (!hasPermission(perm)) {
                        permissionsRejected.add(perm);
                    }
                }

                if (permissionsRejected.size() > 0) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (shouldShowRequestPermissionRationale(permissionsRejected.get(0))) {
                            new AlertDialog.Builder(MainActivity.this).
                                    setMessage("These permissions are mandatory to get your location. You need to allow them.").
                                    setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                requestPermissions(permissionsRejected.
                                                        toArray(new String[permissionsRejected.size()]), ALL_PERMISSIONS_RESULT);
                                            }
                                        }
                                    }).setNegativeButton("Cancel", null).create().show();

                            return;
                        }
                    }
                } else {
                    mAllPermissionGranted = true;
                }

                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume()_called:" + isMyServiceRunning(DetectionService.class));

        if(isMyServiceRunning(DetectionService.class)) {
            //Get current face data
            try {
                oldPreferenceData = faceDataPreference.getString(Preferences.FACE_DATA_PREFERENCE_KEY, Preferences.DEFAULT_FACE_DATA);
                Log.d(TAG, "OldPreferenceFaceData: " + oldPreferenceData);

                if(!oldPreferenceData.isEmpty() && !oldPreferenceData.equalsIgnoreCase(Preferences.DEFAULT_FACE_DATA)) {
                    validateDataForChange(oldPreferenceData);
                }
                setInfo(getString(R.string.detection_inProgress));
            }catch (ClassCastException e) {
                e.printStackTrace();
                setInfo(getString(R.string.error_results));
            }
        }else {
            setInfo(getString(R.string.initial_info));
        }
    }

    SharedPreferences.OnSharedPreferenceChangeListener spChangedListener = new
            SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
                    Log.d(TAG, "onSharedPreferenceChanged()_called");
                    try {
                        String curFaceData = FaceDetectionApp.faceDataPreferences.getString(Preferences.FACE_DATA_PREFERENCE_KEY, Preferences.DEFAULT_FACE_DATA);
                        Log.d(TAG, "NewPreferenceFaceData: " + curFaceData);

                        if(curFaceData.equalsIgnoreCase(Preferences.DEFAULT_FACE_DATA)) {
                            setInfo(getString(R.string.no_face_found));
                        }else {
                            validateDataForChange(curFaceData);
                        }
                    }catch (ClassCastException e) {
                        e.printStackTrace();
                        setInfo(getString(R.string.error_results));
                    }
                }
            };


    private void validateDataForChange(String curFaceData) {
        //TODO: Compare curFaceData and oldPreferenceData for significant change & if significant change then request for updated media

        /*String oldSavedPreferenceData =  faceDataPreference.getString(Preferences.OLD_FACE_DATA_PREFERENCE_KEY, Preferences.OLD_FACE_DATA);
        checkFaceDataChanges(oldSavedPreferenceData, curFaceData);*/

        //Currently Showing result on UI
        runOnUiThread(new UpdateUI(curFaceData));
    }


    class UpdateUI implements Runnable
    {
        String updateString;
        public UpdateUI(String updateString) {
            this.updateString = updateString;
        }
        public void run() {
            // Clear the old result if any.
            setResultSummary("");

            //Set new result
            setUiAfterDetection(updateString);
        }
    }

    // Show the result on screen when detection is done.
    private void setUiAfterDetection(String result) {
        Log.d(TAG, "setUiAfterDetection(): " + result);

        // Set the adapter of the ListView which contains the details of the detected faces.
        FaceListAdapter faceListAdapter = new FaceListAdapter(result);

        // Show the detailed list of detected faces.
        listView = (ListView) findViewById(R.id.list_detected_faces);
        listView.setAdapter(faceListAdapter);


        //Show result summary
        setResultSummary(result);
    }

    // Set the status information panel on screen.
    private void setInfo(String info) {
        Log.d(TAG, "setInfo: " + info);
        textView = (TextView) findViewById(R.id.info);
        textView.setText(info);
    }

    //Set result summary on screen
    private void setResultSummary(String curFaceData) {
        if(curFaceData.isEmpty()) {
            resultSummaryTextView = (TextView) findViewById(R.id.avrg_result_info);
            resultSummaryTextView.setText(curFaceData);
            return;
        }

        String resultSummary = "";
        String resultInfo = "";
        int maleCount = 0;
        int femaleCount = 0;

        float mAgeSum = 0;
        float fAgeSum = 0;
        JSONArray jsonArr;

        try {
            jsonArr = new JSONArray(curFaceData);
            for (int i = 0; i < jsonArr.length(); i++)
            {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Log.d(TAG, "JsonObjValue: " +jsonObj);
                if(jsonObj.getString("gender").equalsIgnoreCase("male")) {
                    mAgeSum = mAgeSum + Float.valueOf(jsonObj.getString("age"));
                    maleCount++;
                } else if(jsonObj.getString("gender").equalsIgnoreCase("female")) {
                    fAgeSum = fAgeSum + Float.valueOf(jsonObj.getString("age"));
                    femaleCount++;
                }
            }
            Log.d(TAG, "mAgeSum: : " +mAgeSum + "::fAgeSum: " +fAgeSum);
            resultInfo= jsonArr.length() + " face"
                    + (jsonArr.length() != 1 ? "s" : "") + " detected.";


            if(maleCount >0 && femaleCount > 0) {
                resultSummary = resultInfo + "\n" + maleCount + " Male with Average Age: " + Math.round(mAgeSum/maleCount) + " Years"
                        + "\n" +femaleCount + " Female with Average Age: " + Math.round(fAgeSum/femaleCount) + " Years";
            } else if(maleCount > 1 && femaleCount == 0) {
                resultSummary = resultInfo + "\n" + " All Male with Average Age: " + Math.round(mAgeSum/maleCount) + " Years";
            } else if(maleCount == 1 && femaleCount == 0) {
                resultSummary = resultInfo + "\n" + " One Male with Age: " + Math.round(mAgeSum/maleCount) + " Years";
            } else if(maleCount == 0 && femaleCount >1) {
                resultSummary = resultInfo + "\n" + "All Female with Average Age: " + Math.round(fAgeSum/femaleCount) + " Years";
            } else if(maleCount == 0 && femaleCount == 1) {
                resultSummary = resultInfo + "\n" + "One Female with Age: " + Math.round(fAgeSum/femaleCount) + " Years";
            } else {
                resultSummary = resultInfo;
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        resultSummaryTextView = (TextView) findViewById(R.id.avrg_result_info);
        resultSummaryTextView.setText(resultSummary);
    }


    private void resetResults() {
        ((TextView)findViewById(R.id.info)).setText("");

        //reset list view
        if(listView != null) {
            listView.setAdapter(null);
        }
        btn_startDemo.setEnabled(true);
        btn_startDemo.setClickable(true);

        setInfo("");
        setInfo(getString(R.string.initial_info));
    }

    // The adapter of the GridView which contains the details of the detected faces.
    private class FaceListAdapter extends BaseAdapter {
        // The detected faces.
        List<String> faces = new ArrayList<>();

        // Initialize with detection result.
        FaceListAdapter(String detectionResult) {
            try {
                JSONArray jasonArrayResult = new JSONArray(detectionResult);
                for (int i = 0; i < jasonArrayResult.length(); i++) {
                    JSONObject jsonobject = jasonArrayResult.getJSONObject(i);
                    double age = jsonobject.getDouble("age");
                    String gender = jsonobject.getString("gender");
                    faces.add("Gender: "+gender +" and  Age: " +String.valueOf(age) +" years");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public int getCount() {
            if(faces != null && faces.size() >0) {
                return faces.size();
            } else
                return 0;
        }

        @Override
        public Object getItem(int position) {
            return faces.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater layoutInflater =
                        (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = layoutInflater.inflate(R.layout.item_face_with_description, parent, false);
            }
            convertView.setId(position);

            String face_description = faces.get(position);

            ((TextView) convertView.findViewById(R.id.text_detected_face)).setText(face_description);

            return convertView;
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        Log.d(TAG, "onPause()_called");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()_called");
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()_called");
        if(detectionServiceIntent != null)
            stopService(detectionServiceIntent);

        faceDataPreference.unregisterOnSharedPreferenceChangeListener(spChangedListener);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Log.d(TAG, "onBackPressed()_called");
    }


    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void checkFaceDataChanges(String oldSavedPreferenceData, String curPreferenceData) {

        //For the 1st time copy currently found face data for the old face data and set significantChanges as TRUE
        if(oldSavedPreferenceData.isEmpty() || oldSavedPreferenceData.equalsIgnoreCase(Preferences.DEFAULT_FACE_DATA)) {
            FaceDetectionApp.faceDataPreferences.edit()
                    .putString(Preferences.OLD_FACE_DATA_PREFERENCE_KEY, curPreferenceData).commit();
            significantChanges = true;
            //TODO: Request for updated media
        } else {
            if(Utils.isSignificantChange(oldSavedPreferenceData, curPreferenceData)) {
                significantChanges = true;

                //update oldSavedPreferenceData with curPreferenceData
                FaceDetectionApp.faceDataPreferences.edit()
                        .putString(Preferences.OLD_FACE_DATA_PREFERENCE_KEY, curPreferenceData).commit();
                //TODO: Request for updated media

            } else{
                significantChanges = false;
                //TODO: Keep playing current media
            }
        }
    }
}
