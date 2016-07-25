package com.kaltura.kalturaplayerfragmentdemo;

import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import static com.kaltura.playersdk.utils.LogUtils.LOGD;
import static com.kaltura.playersdk.utils.LogUtils.LOGE;

public class MainActivity extends AppCompatActivity implements PlayerFragment.OnFragmentInteractionListener {

    private static final String TAG = "PlayerFragmentDemo";
    private PlayerFragment mPlayerFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Button button = (Button)findViewById(R.id.button);
        if (button != null) {
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    view.setVisibility(View.INVISIBLE);
                    boolean isPlayer = false;
                    if (mPlayerFragment == null) {
                        mPlayerFragment = new PlayerFragment();
                        isPlayer = true;
                    }
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    transaction.setCustomAnimations(R.animator.enter_from_right, R.animator.exit_to_left);
                    transaction.add(R.id.fragment_container, mPlayerFragment);
                    transaction.addToBackStack(mPlayerFragment.getClass().getName());
                    transaction.commit();
                    if (!isPlayer) {
                        mPlayerFragment.resumePlayer();
                    }
                }
            });
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (!isFinishing() && mPlayerFragment != null) {
            mPlayerFragment.killPlayer();
            mPlayerFragment = null;
            ((Button)findViewById(R.id.button)).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        LOGD(TAG, "URI " + uri.toString());
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            mPlayerFragment.pausePlayer();
            getFragmentManager().popBackStack();
            ((Button)findViewById(R.id.button)).setVisibility(View.VISIBLE);
        }
        else {
            super.onBackPressed();
        }
    }
}
