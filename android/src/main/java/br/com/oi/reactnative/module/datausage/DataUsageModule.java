package br.com.oi.reactnative.module.datausage;

import android.app.usage.NetworkStatsManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Build;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import br.com.oi.reactnative.module.datausage.helper.NetworkStatsHelper;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static android.content.pm.PackageManager.GET_META_DATA;

public class DataUsageModule extends ReactContextBaseJavaModule {

    private static final String TAG = "DataUsageModule";

    public DataUsageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return TAG;
    }

    public void getDataUsageByApp(final String[] packageNames, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                JSONArray apps = new JSONArray();
                try {
                    if (packageNames != null && packageNames.length > 0) {
                        for (String packageName : packageNames) {
                            final PackageInfo packageInfo = packageManager.getPackageInfo(packageName, GET_META_DATA);
                            int uid = packageInfo.applicationInfo.uid;

                            ApplicationInfo appInfo = null;
                            try {
                                appInfo = packageManager.getApplicationInfo(packageName, 0);
                            } catch (PackageManager.NameNotFoundException e) {
                                Log.e(TAG, "Error getting application info: " + e.getMessage(), e);
                            }

                            String name = (String) packageManager.getApplicationLabel(appInfo);
                            Drawable icon = packageManager.getApplicationIcon(appInfo);

                            Bitmap bitmap = drawableToBitmap(icon);
                            String encodedImage = encodeBitmapToBase64(bitmap);

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                // < Android 6.0
                                Log.i(TAG, "##### Android 5- App: " + name + "     packageName: " + packageName);
                                JSONObject appStats = getTrafficStats(uid, name, packageName, encodedImage);
                                if (appStats != null) apps.put(appStats);
                            } else {
                                // Android 6+
                                Log.i(TAG, "##### Android 6+ App: " + name + "     packageName: " + packageName);
                                JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage);
                                if (appStats != null) apps.put(appStats);
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Error getting app info: " + e.getMessage(), e);
                }

                callback.invoke(null, apps.toString());
            }
        });
    }

    @ReactMethod
    public void listDataUsageByApps(final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "##### Listar todos os aplicativos por uso de dados...");
                Context context = getReactApplicationContext();

                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                //List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);
                JSONArray apps = new JSONArray();

                final List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
                for (PackageInfo packageInfo : packageInfoList) {
                    if (packageInfo.requestedPermissions == null)
                        continue;

                    for (String permission : packageInfo.requestedPermissions) {
                        if (TextUtils.equals(permission, android.Manifest.permission.INTERNET)) {
                            int uid = packageInfo.applicationInfo.uid;
                            String packageName = packageInfo.packageName;

                            ApplicationInfo appInfo = null;
                            try {
                                appInfo = packageManager.getApplicationInfo(packageName, 0);
                            } catch (PackageManager.NameNotFoundException e) {
                                Log.e(TAG, "Error getting application info: " + e.getMessage(), e);
                            }

                            String name = (String) packageManager.getApplicationLabel(appInfo);
                            Drawable icon = packageManager.getApplicationIcon(appInfo);

                            Bitmap bitmap = drawableToBitmap(icon);
                            String encodedImage = encodeBitmapToBase64(bitmap);

                            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                                // < Android 6.0
                                Log.i(TAG, "##### Android 5- App: " + name + "     packageName: " + packageName);
                                JSONObject appStats = getTrafficStats(uid, name, packageName, encodedImage);
                                if (appStats != null) apps.put(appStats);
                            } else {
                                // Android 6+
                                Log.i(TAG, "##### Android 6+ App: " + name + "     packageName: " + packageName);
                                JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage);
                                if (appStats != null) apps.put(appStats);
                            }

                            break;
                        }
                    }
                }

                callback.invoke(null, apps.toString());
            }
        });
    }

    private JSONObject getNetworkManagerStats(int uid, String name, String packageName, String encodedImage) {
        //Log.i(TAG, "##### Step getNetworkManagerStats(" + uid + ", " + name + ", ...)");
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getReactApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStatsHelper networkStatsHelper = new NetworkStatsHelper(networkStatsManager, uid);

        //long wifiBytesRx = networkStatsHelper.getAllRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getAllRxBytesWifi();
        //long wifiBytesTx = networkStatsHelper.getAllRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getAllRxBytesWifi();

        double gsmBytesRx = (double) networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext());
        double gsmBytesTx = (double) networkStatsHelper.getPackageTxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext());
        double total = gsmBytesRx + gsmBytesTx;
        Log.i(TAG, "getNetworkManagerStats - tx: " + gsmBytesTx + " | rx: " + gsmBytesRx + " | total: " + total);

        try {
            if (total > 0D) {
                return new JSONObject().put("name", name)
                                        .put("packageName", packageName)
                                        .put("rx", gsmBytesRx)
                                        .put("rxMb", String.format("%.2f MB", ((gsmBytesRx / 1024D) / 1024D)))
                                        .put("tx", gsmBytesTx)
                                        .put("txMb", String.format("%.2f MB", ((gsmBytesTx / 1024D) / 1024D)))
                                        .put("total", total)
                                        .put("totalMb", String.format("%.2f MB", (total / 1024D) / 1024D))
                                        .put("icon", encodedImage);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error putting app info: " + e.getMessage(), e);
        }

        return null;
    }

    private JSONObject getTrafficStats(int uid, String name, String packageName, String encodedImage) {
        double rx = (double) TrafficStats.getUidRxBytes(uid);
        double tx = (double) TrafficStats.getUidTxBytes(uid);
        double total = rx + tx;

        try {
            if (total > 0) {
                return new JSONObject().put("name", name)
                                        .put("packageName", packageName)
                                        .put("rx", rx)
                                        .put("received", String.format("%.2f MB", ((rx / 1024D) / 1024D) ))
                                        .put("tx", tx)
                                        .put("sent", String.format("%.2f MB", ((tx / 1024D) / 1024D) ))
                                        .put("total", total)
                                        .put("totalMb", String.format("%.2f MB", (total / 1024D) / 1024D ))
                                        .put("icon", encodedImage);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error putting app info: " + e.getMessage(), e);
        }

        return null;
    }

    private String encodeBitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = null;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if(bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

}
