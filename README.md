## React Native Module for Android - Data Usage per App 
This React Native module shows data usage per application in Android (required version 4.4 or later). Your React Native app should be able to read Android Data Usage statistics.

![alt text](https://raw.githubusercontent.com/shimatai/react-native-android-datausage/master/images/Screenshot_Android_Data_Usage.png)

It uses [TrafficStats](https://developer.android.com/reference/android/net/TrafficStats.html) or [NetworkStatsManager](https://developer.android.com/reference/android/app/usage/NetworkStatsManager.html) (requires Android 6.0 or later).

## Installing it as a module in your project
There are many ways to do this, here's the way I do it:

1. Do `npm install --save git+https://github.com/shimatai/react-native-android-datausage.git` in your React Native main project.

2. Link the library:
    * Add the following to `android/settings.gradle`:
        ```
        include ':react-native-android-datausage'
        project(':react-native-android-datausage').projectDir = new File(settingsDir, '../node_modules/react-native-android-datausage/android')
        ```

    * Add the following to `android/app/build.gradle`:
        ```xml
        ...

        dependencies {
            ...
            compile project(':react-native-android-datausage')
        }
        ```
    * Add the following to `android/app/src/main/java/**/MainApplication.java`:
        ```java
        package com.company.myapp;

        import br.com.oi.reactnative.module.datausage.DataUsagePackage;  // add this for react-native-android-datausage

        public class MainApplication extends Application implements ReactApplication {

            @Override
            protected List<ReactPackage> getPackages() {
                return Arrays.<ReactPackage>asList(
                    new MainReactPackage(),
                    new DataUsagePackage()     // add this for react-native-android-datausage
                );
            }
        }
        ```
    * You need to request some specific permissions to user, so add the following permissions to `AndroidManifest.xml` (don't forget to declare the XML namespace `xmlns:tools`):
        ```xml
        <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                xmlns:tools="http://schemas.android.com/tools"
                package="com.company.myapp"
                android:versionCode="1"
                android:versionName="1.0">

                <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
                <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE"/>
                <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
                <uses-permission
                        android:name="android.permission.PACKAGE_USAGE_STATS"
                        tools:ignore="ProtectedPermissions"/>

                <permission
                        android:name="android.permission.PACKAGE_USAGE_STATS"
                        android:protectionLevel="signature"/>

                ...

        </manifest>
        ```

    * For Android 6 or later, user needs to allow the requested permissions, so add the following code to `android/app/src/main/java/**/MainActivity.java` after the method `getMainComponentName()`:
        ```java
        package com.company.myapp;

        import android.Manifest;
        import android.annotation.TargetApi;
        import android.app.AppOpsManager;
        import android.content.Context;
        import android.content.Intent;
        import android.content.pm.PackageManager;
        import android.os.Build;
        import android.provider.Settings;
        import android.support.v4.app.ActivityCompat;

        private static final int READ_PHONE_STATE_REQUEST = 37;

        @Override
        protected void onResume() {
                super.onResume();
                requestPermissions();
        }

        private void requestPermissions() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!hasPermissionToReadNetworkHistory()) {
                        return;
                }

                if (!hasPermissionToReadPhoneStats()) {
                        requestPhoneStateStats();
                        return;
                }
            }
        }

        private boolean hasPermissionToReadNetworkHistory() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        return true;
                }
                final AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
                int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
                if (mode == AppOpsManager.MODE_ALLOWED) {
                        return true;
                }
                appOps.startWatchingMode(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        getApplicationContext().getPackageName(),
                        new AppOpsManager.OnOpChangedListener() {
                                @Override
                                @TargetApi(Build.VERSION_CODES.M)
                                public void onOpChanged(String op, String packageName) {
                                        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), getPackageName());
                                        if (mode != AppOpsManager.MODE_ALLOWED) {
                                                return;
                                        }
                                        appOps.stopWatchingMode(this);
                                        Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                        if (getIntent().getExtras() != null) {
                                                intent.putExtras(getIntent().getExtras());
                                        }
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        getApplicationContext().startActivity(intent);
                                }
                });
                requestReadNetworkHistoryAccess();
                return false;
        }

        private boolean hasPermissionToReadPhoneStats() {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED) {
                        return false;
                } else {
                        return true;
                }
        }

        private void requestReadNetworkHistoryAccess() {
                Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                startActivity(intent);
        }

        private void requestPhoneStateStats() {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, READ_PHONE_STATE_REQUEST);
        }
        ```

3. Simply add `NativeModules` to import this React Native module:

    ```javascript
    import { NativeModules} from 'react-native';

    if (NativeModules.DataUsageModule) {
        // Get data usage of all installed apps in current device
        NativeModules.DataUsageModule.listDataUsageByApps((err, jsonArrayStr) => {
            if (!err) {
		    var apps = JSON.parse(jsonArrayStr);
		    console.log(apps);
		    for (var i = 0; i < apps.length; i++) {
			    var app = apps[i];
			    console.log("App name: " + app.name + "\n" 
					    + "Package name: " + app.packageName + "\n"
					    + "Received bytes: " + app.rx + "bytes\n"
					    + "Transmitted bytes: " + app.tx + "bytes\n"
					    + "Received MB: " + app.rxMb + "\n"
					    + "Transmitted MB: " + app.txMb);
		    }
            }
        });

        // Get data usage of specific list of installed apps in current device
        // Example: get data usage for Facebook, YouTube and WhatsApp.
        NativeModules.DataUsageModule.getDataUsageByApp(["com.facebook.katana", "com.google.android.youtube", "com.whatsapp"], 
            (err, jsonArrayStr) => {
                if (!err) {
		    var apps = JSON.parse(jsonArrayStr);
		    console.log(apps);
		    for (var i = 0; i < apps.length; i++) {
			    var app = apps[i];
			    console.log("App name: " + app.name + "\n" 
					    + "Package name: " + app.packageName + "\n"
					    + "Received bytes: " + app.rx + "bytes\n"
					    + "Transmitted bytes: " + app.tx + "bytes\n"
					    + "Received MB: " + app.rxMb + "\n"
					    + "Transmitted MB: " + app.txMb);
		    }
            }
        });
    }
    ```

