package br.com.oi.reactnative.module.datausage.helper;

import android.annotation.TargetApi;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import java.util.Date;

@TargetApi(Build.VERSION_CODES.M)
public class NetworkStatsHelper {

    NetworkStatsManager networkStatsManager;
    int packageUid;

    public NetworkStatsHelper(NetworkStatsManager networkStatsManager) {
        this.networkStatsManager = networkStatsManager;
    }

    public NetworkStatsHelper(NetworkStatsManager networkStatsManager, int packageUid) {
        this.networkStatsManager = networkStatsManager;
        this.packageUid = packageUid;
    }

    public long getAllRxBytesMobile(Context context) {
        return getAllRxBytesMobile(context, null, null);
    }

    public long getAllRxBytesMobile(Context context, Date startDate, Date endDate) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                                "",
                                startDate != null ? startDate.getTime() : 0,
                                endDate != null ? endDate.getTime() : System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getRxBytes();
    }

    public long getAllTxBytesMobile(Context context) {
        return getAllTxBytesMobile(context, null, null);
    }

    public long getAllTxBytesMobile(Context context, Date startDate, Date endDate) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE,
                                    "",
                                    startDate != null ? startDate.getTime() : 0,
                                    endDate != null ? endDate.getTime() : System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getTxBytes();
    }

    public long getAllRxBytesWifi() {
        return getAllRxBytesWifi(null, null);
    }

    public long getAllRxBytesWifi(Date startDate, Date endDate) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                                null,
                                startDate != null ? startDate.getTime() : 0,
                                endDate != null ? endDate.getTime() : System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getRxBytes();
    }

    public long getAllTxBytesWifi() {
        return getAllTxBytesWifi(null, null);
    }

    public long getAllTxBytesWifi(Date startDate, Date endDate) {
        NetworkStats.Bucket bucket;
        try {
            bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI,
                    null,
                    startDate != null ? startDate.getTime() : 0,
                    endDate != null ? endDate.getTime() : System.currentTimeMillis());
        } catch (RemoteException e) {
            return -1;
        }
        return bucket.getTxBytes();
    }

    public long getPackageRxBytesMobile(Context context) {
        return getPackageRxBytesMobile(context, null, null);
    }

    public long getPackageRxBytesMobile(Context context, Date startDate, Date endDate) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                                    ConnectivityManager.TYPE_MOBILE,
                                    "",
                                    startDate != null ? startDate.getTime() : 0,
                                    endDate != null ? endDate.getTime() : System.currentTimeMillis(),
                                    packageUid);
        } catch (RemoteException e) {
            return -1;
        }
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        networkStats.getNextBucket(bucket);
        networkStats.getNextBucket(bucket);
        long rx = bucket.getRxBytes();
        networkStats.close();
        return rx;
    }

    public long getPackageTxBytesMobile(Context context) {
        return getPackageTxBytesMobile(context, null, null);
    }

    public long getPackageTxBytesMobile(Context context, Date startDate, Date endDate) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                                ConnectivityManager.TYPE_MOBILE,
                                "",
                                startDate != null ? startDate.getTime() : 0,
                                endDate != null ? endDate.getTime() : System.currentTimeMillis(),
                                packageUid);
        } catch (RemoteException e) {
            return -1;
        }
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        networkStats.getNextBucket(bucket);
        long tx = bucket.getTxBytes();
        networkStats.close();
        return tx;
    }

    public long getPackageRxBytesWifi() {
        return getPackageRxBytesWifi(null, null);
    }

    public long getPackageRxBytesWifi(Date startDate, Date endDate) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                                ConnectivityManager.TYPE_WIFI,
                                null,
                                startDate != null ? startDate.getTime() : 0,
                                endDate != null ? endDate.getTime() : System.currentTimeMillis(),
                    packageUid);
        } catch (RemoteException e) {
            return -1;
        }
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        networkStats.getNextBucket(bucket);
        long rx = bucket.getRxBytes();
        networkStats.close();
        return rx;
    }

    public long getPackageTxBytesWifi() {
        return getPackageTxBytesWifi(null, null);
    }

    public long getPackageTxBytesWifi(Date startDate, Date endDate) {
        NetworkStats networkStats = null;
        try {
            networkStats = networkStatsManager.queryDetailsForUid(
                                    ConnectivityManager.TYPE_WIFI,
                                    null,
                                    startDate != null ? startDate.getTime() : 0,
                                    endDate != null ? endDate.getTime() : System.currentTimeMillis(),
                                    packageUid);
        } catch (RemoteException e) {
            return -1;
        }
        NetworkStats.Bucket bucket = new NetworkStats.Bucket();
        networkStats.getNextBucket(bucket);
        long tx = bucket.getTxBytes();
        networkStats.close();
        return tx;
    }

    private String getSubscriberId(Context context, int networkType) {
        try{
            if (ConnectivityManager.TYPE_MOBILE == networkType) {
                TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
                return tm.getSubscriberId();
            }
        } catch(Exception e) {
            return null;
        }
        return null;
    }
}
