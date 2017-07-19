package com.rong.map.mylibrary;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;

/**
 * 作者：陈华榕
 * 邮箱:mpa.chen@sportq.com
 * 时间：2017/7/19  16:06
 */

public class ScreenUtils {

    public static DisplayMetrics getDeviceWidthHeight(Context context) {
        DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }
}
