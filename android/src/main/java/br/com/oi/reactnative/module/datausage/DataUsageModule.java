package br.com.oi.reactnative.module.datausage;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.TrafficStats;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.util.Base64;
import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import br.com.oi.reactnative.module.datausage.helper.NetworkStatsHelper;

import static android.content.pm.PackageManager.GET_META_DATA;

public class DataUsageModule extends ReactContextBaseJavaModule {

    private static final String TAG = "DataUsageModule";
    private static final int READ_PHONE_STATE_REQUEST = 37;

    public DataUsageModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void getDataUsageByApp(final ReadableMap map, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                JSONArray apps = new JSONArray();
                try {
                    ReadableArray packageNames = map.hasKey("packages") ? map.getArray("packages") : null;
                    Date startDate = map.hasKey("startDate") ? new Date(Double.valueOf(map.getDouble("startDate")).longValue()) : null;
                    Date endDate = map.hasKey("endDate") ? new Date(Double.valueOf(map.getDouble("endDate")).longValue()) : null;

                    if (packageNames != null && packageNames.size() > 0) {
                        for (int i = 0; i < packageNames.size(); i++) {
                            String packageName = packageNames.getString(i);
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
                                JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage, startDate, endDate);
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
    public void getDataUsageByAppWithTotal(final ReadableMap map, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                JSONObject result = new JSONObject();
                JSONArray apps = new JSONArray();
                Double totalGeral = 0D;
                try {
                    ReadableArray packageNames = map.hasKey("packages") ? map.getArray("packages") : null;
                    Date startDate = map.hasKey("startDate") ? new Date(Double.valueOf(map.getDouble("startDate")).longValue()) : null;
                    Date endDate = map.hasKey("endDate") ? new Date(Double.valueOf(map.getDouble("endDate")).longValue()) : null;

                    if (packageNames != null && packageNames.size() > 0) {
                        Log.i(TAG, "##### Qtd. de aplicativos a analisar: " + packageNames.size());

                        for (int i = 0; i < packageNames.size(); i++) {
                            String packageName = packageNames.getString(i);
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
                                if (appStats != null) {
                                    totalGeral += appStats.getDouble("total");
                                    apps.put(appStats);
                                }
                            } else {
                                // Android 6+
                                Log.i(TAG, "##### Android 6+ App: " + name + "     packageName: " + packageName);
                                JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage, startDate, endDate);
                                if (appStats != null) {
                                    totalGeral += appStats.getDouble("total");
                                    apps.put(appStats);
                                }
                            }
                        }
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(TAG, "Error getting app info: " + e.getMessage(), e);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
                }

                try {
                    result.put("totalGeral", totalGeral)
                          .put("totalGeralMb", String.format("%.2f MB", ((totalGeral / 1024D) / 1024D)))
                          .put("apps", apps);
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON: " + e.getMessage(), e);
                }

                callback.invoke(null, result.toString());
            }
        });
    }

    @ReactMethod
    public void listDataUsageByApps(final ReadableMap map, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Date startExecDate = new Date();
                Context context = getReactApplicationContext();

                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                //List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);
                JSONArray apps = new JSONArray();
                Date startDate = map.hasKey("startDate") ? new Date(Double.valueOf(map.getDouble("startDate")).longValue()) : null;
                Date endDate = map.hasKey("endDate") ? new Date(Double.valueOf(map.getDouble("endDate")).longValue()) : null;

                final List<PackageInfo> packageInfoList = packageManager.getInstalledPackages(PackageManager.GET_PERMISSIONS);
                for (PackageInfo packageInfo : packageInfoList) {
                    if (packageInfo.requestedPermissions == null || isSystemPackage(packageInfo))
                        continue;

                    List<String> permissions = Arrays.asList(packageInfo.requestedPermissions);
                    if (permissions.contains(android.Manifest.permission.INTERNET)) {
                        int uid = packageInfo.applicationInfo.uid;
                        String packageName = packageInfo.packageName;

                        ApplicationInfo appInfo = null;
                        try {
                            appInfo = packageManager.getApplicationInfo(packageName, 0);

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
                                JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage, startDate, endDate);
                                if (appStats != null) apps.put(appStats);
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Log.e(TAG, "Error getting application info: " + e.getMessage(), e);
                        }

                    }
                }

                long seconds = new Date().getTime() - startExecDate.getTime();
                Log.i(TAG, "##### Time elapsed: " + (seconds/1000L) + " seconds");

                callback.invoke(null, apps.toString());
            }
        });
    }

    @ReactMethod
    public void listDataUsageByApps2(final ReadableMap map, final Callback callback) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                Date startExecDate = new Date();
                Context context = getReactApplicationContext();

                final PackageManager packageManager = getReactApplicationContext().getPackageManager();
                JSONArray apps = new JSONArray();
                Date startDate = map.hasKey("startDate") ? new Date(Double.valueOf(map.getDouble("startDate")).longValue()) : null;
                Date endDate = map.hasKey("endDate") ? new Date(Double.valueOf(map.getDouble("endDate")).longValue()) : null;

                Intent mainIntent = new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER);
                List<ResolveInfo> packages = packageManager.queryIntentActivities(mainIntent, 0);
                for (int i = 0; i < packages.size(); i++) {
                    try {
                        ResolveInfo resolveInfo = packages.get(i);

                        String packageName = resolveInfo.activityInfo.packageName;
                        String name = resolveInfo.activityInfo.name;
                        int uid = 0;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                            uid = packageManager.getPackageUid(packageName, 0);
                        } else {
                            uid = packageManager.getApplicationInfo(packageName, 0).uid;
                        }

                        Drawable icon = resolveInfo.loadIcon(packageManager);
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
                            JSONObject appStats = getNetworkManagerStats(uid, name, packageName, encodedImage, startDate, endDate);
                            if (appStats != null) apps.put(appStats);
                        }

                    } catch (Exception e) {
                        Log.e(TAG, "Error querying intent activities: " + e.getMessage(), e);
                    }
                }

                long seconds = new Date().getTime() - startExecDate.getTime();
                Log.i(TAG, "##### Time elapsed: " + (seconds/1000L) + " seconds");

                callback.invoke(null, apps.toString());
            }
        });
    }

    private JSONObject getNetworkManagerStats(int uid, String name, String packageName, String encodedImage, Date startDate, Date endDate) {
        //Log.i(TAG, "##### Step getNetworkManagerStats(" + uid + ", " + name + ", ...)");
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) getReactApplicationContext().getSystemService(Context.NETWORK_STATS_SERVICE);
        NetworkStatsHelper networkStatsHelper = new NetworkStatsHelper(networkStatsManager, uid);

        //long wifiBytesRx = networkStatsHelper.getAllRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getAllRxBytesWifi();
        //long wifiBytesTx = networkStatsHelper.getAllRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getAllRxBytesWifi();

        double gsmBytesRx = (double) networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext(), startDate, endDate);
        double gsmBytesTx = (double) networkStatsHelper.getPackageTxBytesMobile(getReactApplicationContext()) + networkStatsHelper.getPackageRxBytesMobile(getReactApplicationContext(), startDate, endDate);
        double total = gsmBytesRx + gsmBytesTx;
        Log.i(TAG, "##### getNetworkManagerStats - " + packageName + " - tx: " + gsmBytesTx + " | rx: " + gsmBytesRx + " | total: " + total);

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
                Log.i(TAG, "##### getTrafficStats - " + packageName + " - tx: " + tx + " | rx: " + rx + " | total: " + total);
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

    @ReactMethod
    public void requestPermissions(final ReadableMap map, final Callback callback) {
        Log.i(TAG, "##### Executando requestPermissions(" + (map != null && map.hasKey("requestPermission") ? map.getString("requestPermission") : "null") + ")");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            boolean requestPermission = map.hasKey("requestPermission") ? Boolean.parseBoolean(map.getString("requestPermission")) : true;
            try {
                if (!hasPermissionToReadNetworkHistory(requestPermission)) {
                    callback.invoke(null, new JSONObject().put("permissions", hasPermissionToReadNetworkHistory(false)).toString());
                    return;
                }

                if (requestPermission && !hasPermissionToReadPhoneStats()) {
                    requestPhoneStateStats();
                    callback.invoke(null, new JSONObject().put("permissions", hasPermissionToReadPhoneStats()).toString());
                    return;
                }

                callback.invoke(null, new JSONObject().put("permissions", true).toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error requesting permissions: " + e.getMessage(), e);
            }
        } else {
            try {
                callback.invoke(null, new JSONObject().put("permissions", true).toString());
            } catch (JSONException e) {
                Log.e(TAG, "Error requesting permissions: " + e.getMessage(), e);
            }
        }
    }

    private boolean hasPermissionToReadNetworkHistory(boolean requestPermission) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        final AppOpsManager appOps = (AppOpsManager) getCurrentActivity().getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getCurrentActivity().getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            return true;
        }
        appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                getCurrentActivity().getApplicationContext().getPackageName(),
                new AppOpsManager.OnOpChangedListener() {
                    @Override
                    @TargetApi(Build.VERSION_CODES.M)
                    public void onOpChanged(String op, String packageName) {
                        try {
                            int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getCurrentActivity().getPackageName());
                            if (mode != AppOpsManager.MODE_ALLOWED) {
                                return;
                            }
                            appOps.stopWatchingMode(this);
                            Intent intent = new Intent(getCurrentActivity(), getCurrentActivity().getClass());
                            if (getCurrentActivity().getIntent().getExtras() != null) {
                                intent.putExtras(getCurrentActivity().getIntent().getExtras());
                            }
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            getCurrentActivity().getApplicationContext().startActivity(intent);
                        } catch (Exception e) {
                            Log.e(TAG, "Error reading data usage statistics: " + e.getMessage(), e);
                        }
                    }
                });
        if (requestPermission) requestReadNetworkHistoryAccess();
        return false;
    }

    private boolean hasPermissionToReadPhoneStats() {
        if (ActivityCompat.checkSelfPermission(getCurrentActivity(), android.Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
            return false;
        } else {
            return true;
        }
    }

    private void requestReadNetworkHistoryAccess() {
        Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        getCurrentActivity().startActivity(intent);
    }

    private void requestPhoneStateStats() {
        ActivityCompat.requestPermissions(getCurrentActivity(), new String[]{ android.Manifest.permission.READ_PHONE_STATE }, READ_PHONE_STATE_REQUEST);
    }

    /**
     * Return whether the given PackgeInfo represents a system package or not.
     * User-installed packages (Market or otherwise) should not be denoted as
     * system packages.
     *
     * @param pkgInfo
     * @return
     */
    private boolean isSystemPackage(PackageInfo pkgInfo) {
        return ((pkgInfo.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) ? true : false;
    }
}
