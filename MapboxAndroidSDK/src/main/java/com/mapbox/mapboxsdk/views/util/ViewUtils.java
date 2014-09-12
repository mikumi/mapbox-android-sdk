package com.mapbox.mapboxsdk.views.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.View;

public class ViewUtils {
    private static final boolean HONEYCOMB_OR_GREATER = (Build.VERSION.SDK_INT >= 11);
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void disableHWAcceleration(View view)
    {
        if (HONEYCOMB_OR_GREATER) {
            view.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }
    
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public static void enableHWAcceleration(View view)
    {
        if (HONEYCOMB_OR_GREATER) {
            view.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }
}
