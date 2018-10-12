package com.akki.facedetectionpoc.utils;

import android.graphics.Color;
import android.util.Log;

/**
 * Created by e01106 on 5/2/2017.
 */
public class RgbMotionDetection {

    private static final String TAG = "RgbMotionDetection";

    // Specific settings
    private static final int mPixelThreshold = 50; // Difference in pixel (RGB)
    private static final int mThreshold = 50000; //10000; // Number of different pixels (RGB)

    private static int[] mPrevious = null;
    private static int mPreviousWidth = 0;
    private static int mPreviousHeight = 0;
    private int[] motionDetectedFrameBit = null;

    /**
     * {@inheritDoc}
     */
    public int[] getPrevious() {
        return ((mPrevious != null) ? mPrevious.clone() : null);
    }

    protected static boolean isDifferent(int[] first, int width, int height) {
        boolean different = false;
        if (first == null) throw new NullPointerException();

        if (mPrevious == null) return false;
        //Log.d(TAG, "isDifferent_DATA: " +first.length + "::: " +mPrevious.length);
        if (first.length != mPrevious.length) return true;
        if (mPreviousWidth != width || mPreviousHeight != height) return true;

        int totDifferentPixels = 0;
        for (int i = 0, ij = 0; i < height; i++) {
            for (int j = 0; j < width; j++, ij++) {
                int pix = (0xff & (first[ij]));
                int otherPix = (0xff & (mPrevious[ij]));

                // Catch any pixels that are out of range
                if (pix < 0) pix = 0;
                if (pix > 255) pix = 255;
                if (otherPix < 0) otherPix = 0;
                if (otherPix > 255) otherPix = 255;

                if (Math.abs(pix - otherPix) >= mPixelThreshold) {
                    totDifferentPixels++;
                    // Paint different pixel red
                    first[ij] = Color.RED;
                }
            }
        }
        if (totDifferentPixels <= 0)
            totDifferentPixels = 1;
        //different = totDifferentPixels > mThreshold;

        int size = height * width;
        int percent = 100/(size/totDifferentPixels);
        if(percent > 10) {
            different = true;
        }
        if(different) {
            Log.d(TAG, "DifferenceDetected_TotalDifferentPixels: " +totDifferentPixels + "DifferentPercentage: " +percent);
        }

        if(different) {
            Log.d(TAG, "DifferenceDetected_TotalDifferentPixels: " +totDifferentPixels);
        }

        return different;
    }

    /**
     * Detect motion comparing RGB pixel values. {@inheritDoc}
     */
    public boolean detect(int[] rgb, int width, int height) {
        if (rgb == null) throw new NullPointerException();
        //Log.d(TAG, "DATASIZE: " +rgb.length);
        int[] original = rgb.clone();

        // Create the "mPrevious" picture, the one that will be used to check the next frame against.
        if (mPrevious == null) {
            mPrevious = original;
            mPreviousWidth = width;
            mPreviousHeight = height;
            //Log.i(TAG, "Creating background image");
            return false;
        }

        // long bDetection = System.currentTimeMillis();
        boolean motionDetected = isDifferent(rgb, width, height);
        // long aDetection = System.currentTimeMillis();
        // Log.d(TAG, "Detection "+(aDetection-bDetection));

        // Replace the current image with the previous.
        mPrevious = original;
        mPreviousWidth = width;
        mPreviousHeight = height;

        motionDetectedFrameBit = original;

        /*if(motionDetected) {
            long curTime_start = System.currentTimeMillis();
            Log.d(TAG, "MotionDetectedAt_TimeInMillis: " +curTime_start);
        }*/

        return motionDetected;
    }

    public int[] motionDetectedFrame(){
        return motionDetectedFrameBit;
    }
}
