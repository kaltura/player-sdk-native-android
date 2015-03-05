package com.kaltura.playersdk;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.util.List;

/**
 * Created by itayi on 3/5/15.
 */
public class Utilities {
    public static boolean doesPackageExist(String targetPackage, Context context){
        List<ApplicationInfo> packages;
        PackageManager pm;
        pm = context.getPackageManager();
        packages = pm.getInstalledApplications(0);

        for (ApplicationInfo packageInfo : packages) {
            if(packageInfo.packageName.equals(targetPackage)){
                return true;
            }
        }

        return false;
    }
}
