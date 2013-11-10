package com.kaltura.playersdk;

import java.net.URLDecoder;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.BounceInterpolator;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.kaltura.playersdk.events.OnPlayerStateChangeListener;
import com.kaltura.playersdk.events.OnPlayheadUpdateListener;
import com.kaltura.playersdk.events.OnProgressListener;
import com.kaltura.playersdk.events.OnToggleFullScreenListener;
import com.kaltura.playersdk.types.PlayerStates;

/**
 * Created by michalradwantzor on 9/24/13.
 */
public class CustomPlayerView extends RelativeLayout {
    private PlayerView mPlayerView;
    private WebView mWebView;
    private double mCurSec;
    private Activity mActivity;
    private OnToggleFullScreenListener mFSListener;


    public CustomPlayerView(Context context) {
        super(context);
    }

    public CustomPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
    }

    public void setOnFullScreenListener( OnToggleFullScreenListener listener ) {
        mFSListener = listener;
    }

    public void setPlayerViewDimensions ( int width, int height ) {
    	ViewGroup.LayoutParams lp = mWebView.getLayoutParams();
    	lp.width = width;
    	lp.height = height;
    	updateViewLayout(mWebView, lp);
       // mPlayerView.setDimensions(width, height);
    }


    public void addComponents( String iframeUrl, int width, int height, Activity activity) {
    	mActivity = activity;
        LayoutParams lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        lp.addRule(CENTER_VERTICAL);
        lp.addRule(CENTER_HORIZONTAL);
        mPlayerView = new PlayerView(mActivity);
        super.addView(mPlayerView, lp);
        //disables videoView auto resize according to content dimensions
       //  mPlayerView.setDimensions(width, height);

        setPlayerListeners();

        LayoutParams wvLp = new LayoutParams(width, height);
        mWebView = new WebView(mActivity);
        this.addView(mWebView, wvLp);
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.setWebViewClient(new CustomWebViewClient());
        mWebView.setWebChromeClient(new WebChromeClient());
        mWebView.getSettings().setUserAgentString(mWebView.getSettings().getUserAgentString() + " kalturaNativeCordovaPlayer");
        if (Build.VERSION.SDK_INT >= 11){
            mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mWebView.loadUrl( iframeUrl );
        mWebView.setBackgroundColor(0);
    }
    
    /**
     * slides with animation according the given values
     * @param x x offset to slide
     * @param duration animation time in milliseconds
     */
    public void slideView( int x, int duration ) {
    	this.animate().xBy(x).setDuration(duration).setInterpolator(new BounceInterpolator());
    }
    
    public boolean isPlaying() {
    	return ( mPlayerView!=null && mPlayerView.isPlaying() );
    }


    private void setPlayerListeners() {

        //notify player state change events
        mPlayerView.registerPlayerStateChange(new OnPlayerStateChangeListener() {
            @Override
            public boolean onStateChanged(PlayerStates state) {
                String stateName = "";
                switch (state) {
                    case PLAY:
                        stateName = "play";
                        break;
                    case PAUSE:
                        stateName = "pause";
                        break;
                    case END:
                        stateName = "ended";
                        break;
                }
                if (stateName != "") {
                    final String eventName = stateName;
                    mActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebView.loadUrl("javascript:NativeBridge.videoPlayer.trigger('" + eventName + "', '');");
                        }
                    });
                }

                return false;
            }
        });

        //trigger timeupdate events
        final Runnable runUpdatePlayehead = new Runnable() {
            @Override
            public void run() {
                mWebView.loadUrl("javascript:NativeBridge.videoPlayer.trigger('timeupdate', '" + mCurSec + "');");
            }
        };

        //listens for playhead update
        mPlayerView.registerPlayheadUpdate(new OnPlayheadUpdateListener() {
            @Override
            public void onPlayheadUpdated(int msec) {
                mCurSec = msec / 1000.0;
                mActivity.runOnUiThread(runUpdatePlayehead);
            }
        });

        //listens for progress events and notify javascript
        mPlayerView.registerProgressUpdate(new OnProgressListener() {
            @Override
            public void onProgressUpdate(int progress) {
                double percent = progress / 100.0;
                mWebView.loadUrl("javascript:NativeBridge.videoPlayer.trigger('progress', '" + percent + "');");
            }
        });
    }

    private class CustomWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if ( url != null ) {
                String[] arr = url.split( ":" );
                if ( arr != null && arr.length > 1 ) {
                    String action = arr[1];
                    if ( action.equals("play") ) {
                        mPlayerView.play();
                        return true;
                    }
                    if ( action.equals("pause") ) {
                        if ( mPlayerView.canPause() ) {
                            mPlayerView.pause();
                            return true;
                        }

                        return false;
                    }

                    if ( action.equals("toggleFullscreen") ) {
                        if ( mFSListener != null ) {
                            mFSListener.onToggleFullScreen();
                            return true;
                        }
                        return false;

                    }
                    if (  action.equals("setAttribute") ) {
                        if ( arr.length > 3 ) {
                            try {
                                String value = URLDecoder.decode(arr[3], "UTF-8");
                                if ( value!= null && value.length() > 2 ) {
                                    String[] params = value.substring( 1, value.length() - 1 ).split(",");
                                    if ( params != null && params.length > 1 ) {
                                        if ( params[0].equals("\"currentTime\"") ) {
                                            int seekTo = Math.round( Float.parseFloat( params[1] ) * 1000 );
                                            mPlayerView.seek(seekTo);
                                        }
                                        else if ( params[0].equals("\"src\"") ) {
                                            //remove " from the edges
                                            String urlToPlay = params[1].substring(1, params[1].length() - 1);
                                            mPlayerView.setVideoUrl(urlToPlay);
                                        }
                                    }
                                }
                            } catch ( Exception e) {
                                System.out.println( "failed to setAttribute" );
                            }

                        }
                    }
                }

            }

            return false;
        }
    }
}
