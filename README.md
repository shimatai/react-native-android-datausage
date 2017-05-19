## React Native Module for Android 
This React Native module shows data usage per application in Android (required version 4.4 or later).

It uses [TrafficStats](https://developer.android.com/reference/android/net/TrafficStats.html) or [NetworkStatsManager](https://developer.android.com/reference/android/app/usage/NetworkStatsManager.html) (requires Android 6.0 or later).

## Installing it as a module in your project
There are many ways to do this, here's the way I do it:

1. Link the library:
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
2. Simply `import/require` it by the name defined in your library's `package.json`:

    ```javascript
    import DataUsageModule from 'react-native-android-datausage'
    DataUsageModule.listDataUsageByApps((err, jsonArrayStr) => {
	if (!err) {
		var apps = JSON.parse(jsonArrayStr);
		console.log(apps);
		for (var i = 0; i < apps.length; i++) {
			var app = apps[i];
			console.log("App name: " + app.name + "\n" 
					+ "Package name: " + app.packageName + "\n"
					+ "Received bytes: " + rx + "bytes\n"
					+ "Transmitted bytes: " + tx + "bytes\n"
					+ "Received MB: " + rxMb + "\n"
					+ "Transmitted MB: " + txMb);
		}
	}
      });
    ```

