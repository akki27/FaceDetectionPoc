package com.akki.facedetectionpoc.data;

/**
 * Created by SadyAkki on 5/10/2017.
 */

public abstract class Preferences {

    private Preferences() {
    }

    // Time current processing face and next face processing
    public static int FACE_DETECTION_DELAY = 20000;

    public static int DETECTION_COUNTER_DELAY = 2000;

    public static final int STATUS_FACE_FOUND = 0;
    public static final int STATUS_FACE_NOT_FOUND = 1;
    public static final int STATUS_ERROR = 2;
    public static final int STATUS_NO_FACE_DETECTION_SUPPORT = 3;

    public static String NO_FD_SUPPORT = "noFdSupport";
    public static String FD_RESULT = "result";
    public static String NO_FACE_FOUND = "noFace";

    public static String FACE_DATA_PREFERENCE_KEY = "faceDataPreferenceKey";
    public static String DEFAULT_FACE_DATA = "noFaceData";

    public static String OLD_FACE_DATA_PREFERENCE_KEY = "currDataPreferenceKey";
    public static String OLD_FACE_DATA = "oldFaceData";




}
