package com.kaltura.kalturaplayertoolkit;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * Created by itayi on 2/24/15.
 */
public class FragmentUtilities {
    public static void loadFragment(boolean addToBackStack , Fragment newFragment, Bundle extras, FragmentManager fragmentManager){

        newFragment.setArguments(extras);

        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.fragment_container, newFragment);

        if(addToBackStack) {
            transaction.addToBackStack(null);
        }

        transaction.commit();
    }
}
