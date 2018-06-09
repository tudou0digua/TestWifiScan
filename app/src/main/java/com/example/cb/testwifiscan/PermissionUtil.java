package com.example.cb.testwifiscan;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Camera;
import android.os.Binder;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

import java.lang.reflect.Method;

/**
 * 关于包的工具类
 *
 * @author Air
 */
public class PermissionUtil {

    private static final String TAG = "PermissionUtil";

    /**
     *
     */
    public static boolean selfPermissionGranted(String permission) {
        // For Android < Android M, self permissions are always granted.
        boolean result = true;
//        int targetSdkVersion = getTargetSdkVersion();
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//
//            if (targetSdkVersion >= Build.VERSION_CODES.M) {
//                // targetSdkVersion >= Android M, we can
//                // use Context#checkSelfPermission
//                result = CommonResource.getInstance().getAppContext().checkSelfPermission(permission)
//                        == PackageManager.PERMISSION_GRANTED;
//            } else {
//                // targetSdkVersion < Android M, we have to use PermissionChecker
//                result = PermissionChecker.checkSelfPermission(CommonResource.getInstance().getAppContext(), permission)
//                        == PermissionChecker.PERMISSION_GRANTED;
//            }
//        }
        return result;
    }

    /**
     * 是否有读取联系人权限
     * （兼容OPPO手机，任何状态都返回true情况）
     *
     * @param activity
     * @return
     */
    public static boolean checkReadContactPermission(@NonNull Activity activity) {
        String model = Build.MODEL;
        if (model != null && model.toLowerCase().contains("oppo")) {
            ContentResolver resolver = activity.getContentResolver();
            Cursor cursor = null;
            try {
                cursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null, null, null, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (cursor == null) {
                return false; // error
            } else {
                cursor.close();
                return true;
            }
        } else {
            return selfPermissionGranted(Manifest.permission.READ_CONTACTS);
        }
    }

    /**
     * @param grantResults
     * @return
     */
    public static boolean verifyPermissions(int[] grantResults) {
        if (grantResults.length < 1) {
            return false;
        }
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     *
     */
    public static int getTargetSdkVersion() {
        int targetSdkVersion = -1;
//        try {
//            final PackageInfo info = CommonResource.getInstance().getAppContext().getPackageManager().getPackageInfo(
//                    CommonResource.getInstance().getAppContext().getPackageName(), 0);
//            targetSdkVersion = info.applicationInfo.targetSdkVersion;
//        } catch (PackageManager.NameNotFoundException e) {
//            e.printStackTrace();
//        }
        return targetSdkVersion;
    }

    /**
     * 6.0以下版本相机权限检测
     * 返回true 表示可以使用  返回false表示不可以使用
     */
    public static boolean cameraIsCanUse() {
        boolean isCanUse = true;
        Camera mCamera = null;
        try {
            mCamera = Camera.open();
            Camera.Parameters mParameters = mCamera.getParameters(); //针对魅族手机
            mCamera.setParameters(mParameters);
        } catch (Exception e) {
            isCanUse = false;
        }

        if (mCamera != null) {
            try {
                mCamera.release();
            } catch (Exception e) {
                e.printStackTrace();
                return isCanUse;
            }
        }
        return isCanUse;
    }

    /**
     * 摄像头是否可用
     *
     * @return
     */
    public static boolean canCameraUse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return selfPermissionGranted(Manifest.permission.CAMERA);
        } else {
            return cameraIsCanUse();
        }
    }



    private static final int OP_SYSTEM_ALERT_WINDOW = 24;

    /**
     *
     * @param context
     * @return
     */
    public static boolean canDrawOverlays(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            return checkOp(context, OP_SYSTEM_ALERT_WINDOW);
        } else {
            return true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private static boolean checkOp(Context context, int op) {
        AppOpsManager manager = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        try {
            Method method = AppOpsManager.class.getDeclaredMethod("checkOp", int.class, int.class, String.class);
            return AppOpsManager.MODE_ALLOWED == (int) method.invoke(manager, op, Binder.getCallingUid(), context.getPackageName());
        } catch (Exception e) {
        }
        return false;
    }






}
