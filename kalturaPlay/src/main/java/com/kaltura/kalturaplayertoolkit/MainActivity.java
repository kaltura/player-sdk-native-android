package com.kaltura.kalturaplayertoolkit;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.kaltura.playersdk.KPPlayerConfig;
import com.kaltura.playersdk.PlayerViewController;
import com.kaltura.playersdk.RequestDataSource;
import com.kaltura.playersdk.events.KPlayerEventListener;
import com.kaltura.playersdk.events.KPlayerJsCallbackReadyListener;
import com.kaltura.playersdk.events.OnToggleFullScreenListener;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {
	public static final String TAG = MainActivity.class.getSimpleName();
	public static final String PROP_IFRAME_URL = "iframeUrl";
	
	private PlayerViewController mPlayerView;
    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        
        setContentView(R.layout.activity_main);
        
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
        
        mPlayerView = (PlayerViewController) findViewById(R.id.player);
        mPlayerView.setActivity(MainActivity.this);
        mPlayerView.setOnFullScreenListener(new OnToggleFullScreenListener() {
			
			@Override
			public void onToggleFullScreen() {
				setFullScreen();
				
			}
		});
        mPlayerView.registerJsCallbackReady(new KPlayerJsCallbackReadyListener() {
			
			@Override
			public void jsCallbackReady() {
				mPlayerView.addKPlayerEventListener("doPlay", new KPlayerEventListener() {
					
					@Override
					public void onKPlayerEvent(Object body) {
						setFullScreen();
                        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
					}
					
					@Override
					public String getCallbackName() {
						return "EventListenerDoPlay";
					}
				});
                mPlayerView.addKPlayerEventListener("doPause", new KPlayerEventListener() {

                    @Override
                    public void onKPlayerEvent(Object body) {
                        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    }

                    @Override
                    public String getCallbackName() {
                        return "EventListenerDoPause";
                    }
                });
				
			}
		});
        Button demoBtn = (Button)findViewById( R.id.demoBtn );
        demoBtn.setOnClickListener( new OnClickListener() {

			@Override
			public void onClick(View v) {
				showPlayerView();
				//show demo
				mPlayerView.setComponents(new RequestDataSource() {
					
					@Override
					public String getWid() {
						return "_243342";
					}
					
					@Override
					public String getUrid() {
						// TODO Auto-generated method stub
						return null;
					}
					
					@Override
					public String getUiConfId() {
						return "21384602";
					}
					
					@Override
					public String getServerAddress() {
//						return "http://10.0.21.71/html5.kaltura/mwEmbed/mwEmbedFrame.php";
						return "http://cdnbakmi.kaltura.com/html5/html5lib/v2.24/mwEmbedFrame.php";
					}
					
					@Override
					public KPPlayerConfig getFlashVars() {
						KPPlayerConfig playerConfig = new KPPlayerConfig();
						playerConfig.setConfigKey(KPPlayerConfig.Key.KP_PLAYER_CONFIG_LEAD_ANDROID_HLS, "true");
						return playerConfig;
					}
					
					@Override
					public String getEntryId() {
						return "0_c0r624gh";
					}
					
					@Override
					public String getCacheStr() {
						// TODO Auto-generated method stub
						return null;
					}
				});
			}
        	
        });
        
        Intent intent = getIntent();
        
        ///////////////////////////////////////////////////////////////////////////
        //for tests
    //doubleclick
      // intent.putExtra(PROP_IFRAME_URL, "http://10.0.21.62/html5.kaltura/mwEmbed/mwEmbedFrame.php/p/524241/sp/52424100/uiconf_id/25906371/wid/_524241/entry_id/0_8zzalxul?iframeembed=true&#038;playerId=kaltura_player_1404535475&#038;entry_id=0_8zzalxul");
     //vast
     //   intent.putExtra(PROP_IFRAME_URL, "http://192.168.1.14/html5.kaltura/mwEmbed/mwEmbedFrame.php/wid/_243342/uiconf_id/13920942/entry_id/0_uka1msg4/?&flashvars%5BimageDefaultDuration%5D=2&flashvars%5BautoPlay%5D=false&flashvars%5BautoMute%5D=false&");
      // widevine 
      //  intent.putExtra(PROP_IFRAME_URL, "http://192.168.1.14/html5.kaltura/mwEmbed/mwEmbedFrame.php/p/524241/sp/52424100/uiconf_id/26356811/wid/_524241/entry_id/0_lnthb45u?iframeembed=true&#038;playerId=kaltura_player_1404535475&#038;entry_id=0_lnthb45u");
 
       /////////////////////////////////////////////
        
	     // check if this intent is started via browser
	     if ( Intent.ACTION_VIEW.equals( intent.getAction() ) ) {
	       Uri uri = intent.getData();
	       String[] params = null;
			try {
				params = URLDecoder.decode(uri.toString(), "UTF-8").split(":=");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	       if ( params!=null && params.length > 1 ) {
		       String iframeUrl = params[1];
		       intent.putExtra(PROP_IFRAME_URL, iframeUrl);
	       } else {
	    	   Log.w(TAG, "didn't load iframe, invalid iframeUrl parameter was passed" ); 
	       }

	     }
	     
	     if ( intent.getStringExtra(PROP_IFRAME_URL)!= null ) {
	    	 showIframeView();
	     } else {
	    	 
	    	 TextView infoMsg = (TextView)findViewById(R.id.infoMsg);
	    	 Spanned spanned = Html.fromHtml(getString(R.string.main_message));
	    	 infoMsg.setMovementMethod(LinkMovementMethod.getInstance());
	    	 infoMsg.setText(spanned);
	    	 
	    	/*  Button goBtn = (Button) findViewById(R.id.submitBtn);
	          final EditText urlText = (EditText) findViewById(R.id.iframeUrl);

	          mPlayerView.setVisibility(RelativeLayout.GONE);
	  		
	          if ( goBtn != null) {
	              goBtn.setOnClickListener( new OnClickListener() {
	              	
	  				@Override
	  				public void onClick(View v) {
	  					//TODO validate url
	  					getIntent().putExtra(PROP_IFRAME_URL, urlText.getText().toString());
	  	            	showPlayerView();
	  				}
	              	
	              });
	          } */
	     }

    }
    
    private void setFullScreen (){
        View decorView = getWindow().getDecorView(); //navigation view
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);

        Point size = getRealScreenSize();
        mPlayerView.setPlayerViewDimensions(size.x, size.y);
    }

     @SuppressLint("NewApi") private Point getRealScreenSize(){
    	 Display display = getWindowManager().getDefaultDisplay();
    	    int realWidth = 0;
    	    int realHeight = 0;

    	    if (Build.VERSION.SDK_INT >= 17){
    	        //new pleasant way to get real metrics
    	        DisplayMetrics realMetrics = new DisplayMetrics();
    	        display.getRealMetrics(realMetrics);
    	        realWidth = realMetrics.widthPixels;
    	        realHeight = realMetrics.heightPixels;

    	    } else {
    	        try {
    	            Method mGetRawH = Display.class.getMethod("getRawHeight");
    	            Method mGetRawW = Display.class.getMethod("getRawWidth");
    	            realWidth = (Integer) mGetRawW.invoke(display);
    	            realHeight = (Integer) mGetRawH.invoke(display);
    	        } catch (Exception e) {
    	            realWidth = display.getWidth();
    	            realHeight = display.getHeight();
    	            Log.e("Display Info", "Couldn't use reflection to get the real display metrics.");
    	        }

    	    }
    	   return new Point(realWidth,realHeight);
     }
    private void showPlayerView() {
    	setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
		mPlayerView.setVisibility(RelativeLayout.VISIBLE);
        Point size = new Point();
        getWindowManager().getDefaultDisplay().getSize(size);       
        mPlayerView.setPlayerViewDimensions( size.x, size.y, 0, 0 );
    }
    
    private void showIframeView() {
    	showPlayerView();
    	mPlayerView.setComponents( getIntent().getStringExtra(PROP_IFRAME_URL));
    }
    
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if ( mPlayerView.getVisibility() == RelativeLayout.VISIBLE ) {
        	Timer swapTimer = new Timer();
        	swapTimer.schedule(new TimerTask() {
	    			            @Override
					            public void run() {
					                runOnUiThread(new Runnable() {
					                    public void run(){
					                    		Point size = new Point();
					                    		getWindowManager().getDefaultDisplay().getSize(size);
						                    	mPlayerView.setPlayerViewDimensions( size.x, size.y, 0, 0 );
						                    	View decorView = getWindow().getDecorView();
						                    	int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
						                    	decorView.setSystemUiVisibility(uiOptions); 
					                    }
					                });
					            }
				        }, 100 );    
        			        	
        }
    }
    
    @Override
    public void onPause() {
    	super.onPause();
    	if ( mPlayerView!=null ) {
    		mPlayerView.releaseAndSavePosition();
    	}
    }

    @Override
    public void onResume() {
    	super.onResume();
    	if ( mPlayerView!=null ) {
//    		mPlayerView.resumePlayer();
    	}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
