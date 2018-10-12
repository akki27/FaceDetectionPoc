package com.akki.facedetectionpoc.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;

/**
 * Created by e01106 on 5/15/2017.
 */
public class Utils {

    public static Context mContext;




    private static boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) mContext.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Rotate the given Bitmap by the given degrees.
     *
     * @param bmp
     *            Bitmap to rotate.
     * @param degrees
     *            Degrees to rotate.
     * @return Bitmap which was rotated.
     */
    public static Bitmap rotate(Bitmap bmp, int degrees) {
        if (bmp == null) throw new NullPointerException();

        // getting scales of the image
        int width = bmp.getWidth();
        int height = bmp.getHeight();

        // Creating a Matrix and rotating it to 90 degrees
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);

        // Getting the rotated Bitmap
        Bitmap rotatedBmp = Bitmap.createBitmap(bmp, 0, 0, width, height, matrix, true);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return rotatedBmp;
    }

    /**
     * Rotate the given image in byte array format by the given degrees.
     *
     * @param data
     *            Bitmap to rotate in byte array form.
     * @param degrees
     *            Degrees to rotate.
     * @return Byte array format of an image which was rotated.
     */
    public static byte[] rotate(byte[] data, int degrees) {
        if (data == null) throw new NullPointerException();

        // Convert the byte data into a Bitmap
        Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);

        // Getting the rotated Bitmap
        Bitmap rotatedBmp = rotate(bmp, degrees);

        // Get the byte array from the Bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        rotatedBmp.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    // Get the rotation angle of the image taken.
    private static int getImageRotationAngle(
            Uri imageUri, ContentResolver contentResolver) throws IOException {
        int angle = 0;
        Cursor cursor = contentResolver.query(imageUri,
                new String[] { MediaStore.Images.ImageColumns.ORIENTATION }, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() == 1) {
                cursor.moveToFirst();
                angle = cursor.getInt(0);
            }
            cursor.close();
        } else {
            ExifInterface exif = new ExifInterface(imageUri.getPath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    angle = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    angle = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    angle = 90;
                    break;
                default:
                    break;
            }
        }
        return angle;
    }

    /**
     * Decode a YUV420SP image to RGB.
     *
     * @param yuv420sp
     *            Byte array representing a YUV420SP image.
     * @param width
     *            Width of the image.
     * @param height
     *            Height of the image.
     * @return Integer array representing the RGB image.
     * @throws NullPointerException
     *             if yuv420sp byte array is NULL.
     */
    public static int[] decodeYUV420SPtoRGB(byte[] yuv420sp, int width, int height) {
        if (yuv420sp == null) throw new NullPointerException();

        final int frameSize = width * height;
        int[] rgb = new int[frameSize];

        for (int j = 0, yp = 0; j < height; j++) {
            int uvp = frameSize + (j >> 1) * width, u = 0, v = 0;
            for (int i = 0; i < width; i++, yp++) {
                int y = (0xff & (yuv420sp[yp])) - 16;
                if (y < 0) y = 0;
                if ((i & 1) == 0) {
                    v = (0xff & yuv420sp[uvp++]) - 128;
                    u = (0xff & yuv420sp[uvp++]) - 128;
                }
                int y1192 = 1192 * y;
                int r = (y1192 + 1634 * v);
                int g = (y1192 - 833 * v - 400 * u);
                int b = (y1192 + 2066 * u);

                if (r < 0) r = 0;
                else if (r > 262143) r = 262143;
                if (g < 0) g = 0;
                else if (g > 262143) g = 262143;
                if (b < 0) b = 0;
                else if (b > 262143) b = 262143;

                rgb[yp] = 0xff000000 | ((r << 6) & 0xff0000) | ((g >> 2) & 0xff00) | ((b >> 10) & 0xff);
            }
        }
        return rgb;
    }

    /**
     * Convert an RGB image into a Bitmap.
     *
     * @param rgb
     *            Integer array representing an RGB image.
     * @param width
     *            Width of the image.
     * @param height
     *            Height of the image.
     * @return Bitmap of the RGB image.
     * @throws NullPointerException
     *             if RGB integer array is NULL.
     */
    public static Bitmap rgbToBitmap(int[] rgb, int width, int height) {
        if (rgb == null) throw new NullPointerException();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        bitmap.setPixels(rgb, 0, width, 0, 0, width, height);
        return bitmap;
    }


    public static boolean isInternetReachable()
    {
        try {
            //make a URL to a known source
            URL url = new URL("http://www.google.com");

            //open a connection to that source
            HttpURLConnection urlConnect = (HttpURLConnection)url.openConnection();

            //trying to retrieve data from the source. If there is no connection, this line will fail
            Object objData = urlConnect.getContent();

        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return false;
        }
        return true;
    }


    public static boolean isSignificantChange(String oldPreferenceData, String curPreferenceData) {

        return false;
    }
}
