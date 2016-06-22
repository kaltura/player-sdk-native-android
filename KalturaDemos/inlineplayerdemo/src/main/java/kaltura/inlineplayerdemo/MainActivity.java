package kaltura.inlineplayerdemo;

import android.app.FragmentTransaction;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.events.KPEventListener;
import com.kaltura.playersdk.events.KPlayerState;
import com.kaltura.playersdk.types.KPError;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PlayerFragment.OnFragmentInteractionListener {
    private PlayerViewController mPlayer;
    private static final String TAG = "MainActivity";
    private PlayerFragment mPlayerFragment;
    private Button inlineViewButton;
    private Button inlineFragmentButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        inlineViewButton = (Button) findViewById(R.id.inlineView);
        if (inlineViewButton != null) {
            inlineViewButton.setOnClickListener(this);
        }
        inlineFragmentButton = (Button) findViewById(R.id.inlineFragment);
        if (inlineFragmentButton != null) {
            inlineFragmentButton.setOnClickListener(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    public PlayerViewController getPlayer() {
        if (mPlayer == null) {
            mPlayer = (PlayerViewController)findViewById(R.id.videoContainer);

            if (mPlayer != null) {
                mPlayer.loadPlayerIntoActivity(this);
                KPPlayerConfig config = new KPPlayerConfig("http://kgit.html5video.org/tags/v2.43.rc11/mwEmbedFrame.php", "31638861", "1831271").setEntryId("1_ng282arr");
                config.setAutoPlay(true);
                mPlayer.initWithConfiguration(config);
            }


        }
        return mPlayer;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.inlineView:
                getPlayer().setVisibility(View.VISIBLE);
                break;
            case R.id.inlineFragment:
                findViewById(R.id.videoContainer).setVisibility(View.GONE);
                mPlayerFragment = PlayerFragment.newInstance(null, null);
                android.support.v4.app.FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                transaction.add(R.id.fragmentContainer, mPlayerFragment);
                transaction.commit();
                break;
        }
        inlineViewButton.setVisibility(View.GONE);
        inlineFragmentButton.setVisibility(View.GONE);
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        
    }
}
