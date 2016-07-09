[![Build Status](https://travis-ci.org/kaltura/player-sdk-native-android.svg?branch=master)](https://travis-ci.org/kaltura/player-sdk-native-android)


Player SDK Native Android
=========================

**Note**: The Kaltura native player component is in beta. If your a Kaltura customer please contact your Kaltura customer success manager to help facilitate use of this component.

The Kaltura player-sdk-native component enables embedding the [kaltura player](http://player.kaltura.com) into native environments. This enables full HTML5 player platform, without limitations of HTML5 video tag API in Android platforms. Currently for Android this enables:
* Inline playback with HTML controls ( disable controls during ads etc. )
* Widevine DRM support
* AutoPlay
* Volume Control
* Full [player.kaltura.com](http://player.kaltura.com) feature set for themes and plugins
* HLS Playback
* DFP IMA SDK


For a full list of native embed advantages see native controls table within the [player toolkit basic usage guide](http://knowledge.kaltura.com/kaltura-player-v2-toolkit-theme-skin-guide).

The Kaltura player-sdk-native component can be embedded into both native apps, and hybrid native apps ( via standard dynamic embed syntax )

Future support will include:
* PlayReady DRM
* Multiple stream playback
* Offline viewing

Architecture Overview
=====
![alt text](http://html5video.org/presentations/HTML5PartDeux.FOSDEM.2014/koverview.jpg "Architecture Overview")


Quick Start Guide
======

```
1. git clone https://github.com/kaltura/player-sdk-native-android.git to the same folder of your app.
```
```
2. Add reference to PlayerSDK module from your project:
```

#####Select _`settings.gradle`_ and add:

```
include ':googlemediaframework'
project(':googlemediaframework').projectDir=new File('../player-sdk-native-android/googlemediaframework')

include ':playerSDK'
project(':playerSDK').projectDir=new File('../player-sdk-native-android/playerSDK')

```
#####Right click on your app folder ->_`Open Module Settings`_.

![alt text](https://9e7704fa-a-62cb3a1a-s-sites.googlegroups.com/site/kalturaimages/shareicons/ModuleSettings.png?attachauth=ANoY7co3Fibe4sZcIY5K1QBU7L74Y4Jp71WJbMJ4vKagckhsYzA2qxzAT5myeKeizQrUsOqn7c-MCNU6jKJi-SZwMWHv2JMcmM7xs-O2FkQUoebdD7SFScNdrUV8sfdaAq0GrNYgrSEk0_4S0bYErXbg0nEzLlOHLOURwMzhZsEvMFdjj_Qe6vfUCsFdlOm6BHOV8FjrA8azbx-ywPWn13SirFrVD71PmbrMftmv6NivJOzaes9lois%3D&attredirects=0)

#####Select _`Dependencies`_ tab -> click on the _`+`_ button and choos the _`playerSDK`_ module:
![alt text](https://9e7704fa-a-62cb3a1a-s-sites.googlegroups.com/site/kalturaimages/shareicons/AddDependencies.png?attachauth=ANoY7cqDWyp0Wk-K-EcsLqf1Iad71Hm8WXS55nmpkaKjw6Me79OXBPoUb8_utColKQgLHC-NL8Q4MD6jabqeUvnYiW9nANA_kcjGbgx8tFndx-_nwrdKLawmpJYN24XMl2g9EvR6SfVwLpMHOymUnN868yvIJQiIOeYpVjtKW67Fr13tD3mVVMSzqoPC1hbTnMiJE-r6msrIkqy4SZFsTXk39swMea7UAEN1heb6u_AdsU-UxUBfTyg%3D&attredirects=0)

Now, you are linked to the playerSDK by reference. Be sure that you cloned the playerSDK to the same folder of your project.


Make sure that you cloned the **_player-sdk-native-android_** project to the same folder of your project, if you prefer to clone it else where, you should update the _**`settings.gradle`**_.

API Overview
=====

###Loading Kaltura player into Fragment - OVP:
```

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        if(mFragmentView == null) {
            mFragmentView = inflater.inflate(R.layout.fragment_fullscreen, container, false);
        }

        mPlayerView = (PlayerViewController) mFragmentView.findViewById(R.id.player);
        mPlayerView.loadPlayerIntoActivity(getActivity());

        KPPlayerConfig config = new  KPPlayerConfig("http://cdnapi.kaltura.com", "26698911", "1831271");
        config.setEntryId("1_o426d3i4");
        mPlayerView.initWithConfiguration(config);        mPlayerView.addEventListener(new KPEventListener() {
            @Override
            public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                Log.d("KPlayer State Changed", state.toString());
            }

            @Override
            public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
                Log.d("KPlayer State Changed", Float.toString(currentTime));
            }

            @Override
            public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
                Log.d("KPlayer toggeled", Boolean.toString(isFullscreen));
            }
        });
        return mFragmentView;
    }
```

###Loading Kaltura player into Fragment - OTT:
```

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        if(mFragmentView == null) {
            mFragmentView = inflater.inflate(R.layout.fragment_fullscreen, container, false);
        }

        mPlayerView = (PlayerViewController) mFragmentView.findViewById(R.id.player);
        mPlayerView.loadPlayerIntoActivity(getActivity());
        KPPlayerConfig config = null;
        try {
              config = KPPlayerConfig.fromJSONObject(new JSONObject(getConfigJson("123","456","tvpapi_000")));
        } catch (JSONException e) {
              e.printStackTrace();
        }

        mPlayerView.initWithConfiguration(config);        mPlayerView.addEventListener(new KPEventListener() {
            @Override
            public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                Log.d("KPlayer State Changed", state.toString());
            }

            @Override
            public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
                Log.d("KPlayer State Changed", Float.toString(currentTime));
            }

            @Override
            public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
                Log.d("KPlayer toggeled", Boolean.toString(isFullscreen));
            }
        });
        return mFragmentView;
    }

    public String getConfigJson(String mediaID, String uiConfID, String tvpApi) {
     String json = "{\n" +
             "  \"base\": {\n" +
             "    \"server\": \"http://192.168.160.160/html5.kaltura/mwEmbed/mwEmbedFrame.php\",\n" +
             "    \"partnerId\": \"\",\n" +
             "    \"uiConfId\": \"" + uiConfID + "\",\n" +
             "    \"entryId\": \"" + mediaID + "\"\n" +
             "  },\n" +
             "  \"extra\": {\n" +
             "    \"controlBarContainer.hover\": true,\n" +
             "    \"controlBarContainer.plugin\": true,\n" +
             "    \n" +
             "    \"liveCore.disableLiveCheck\": true,\n" +
             "    \"tvpapiGetLicensedLinks.plugin\": true,\n" +
             "    \"TVPAPIBaseUrl\": \"http://tvpapi-stg.as.tvinci.com/v3_9/gateways/jsonpostgw.aspx?m=\",\n" +
             "    \"proxyData\": {\n";

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 /*4.3*/) {
//            json = json + "\"config\": {\n" +
//                    "                                    \"flavorassets\": {\n" +
//                    "                                        \"filters\": {\n" +
//                    "                                            \"include\": {\n" +
//                    "                                                \"Format\": [\n" +
//                    "                                                    \"dash Main\"\n" +
//                    "                                                ]\n" +
//                    "                                            }\n" +
//                    "                                        }\n" +
//                    "                                    }\n" +
//                    "                                },";
//        }
     json = json + "      \"MediaID\": \"" + mediaID + "\",\n" +
             "      \"iMediaID\": \"" + mediaID + "\",\n" +
             "      \"mediaType\": \"0\",\n" +
             "      \"picSize\": \"640x360\",\n" +
             "      \"withDynamic\": \"false\",\n" +
             "      \"initObj\": {\n" +
             "        \"ApiPass\": \"11111\",\n" +
             "        \"ApiUser\": \"" + tvpApi + "\",\n" +
             "        \"DomainID\": 0,\n" +
             "        \"Locale\": {\n" +
             "            \"LocaleCountry\": \"null\",\n" +
             "            \"LocaleDevice\": \"null\",\n" +
             "            \"LocaleLanguage\": \"null\",\n" +
             "            \"LocaleUserState\": \"Unknown\"\n" +
             "        },\n" +
             "        \"Platform\": \"Cellular\",\n" +
             "        \"SiteGuid\": \"\",\n" +
             "        \"UDID\": \"aa5e1b6c96988d68\"\n" +
             "      }\n" +
             "    }\n" +
             "  }\n" +
             "}\n";
     return json;
 }
```


###Fetching duration:
For fetching the duration of a video, the player must be in READY state:

```
mPlayerView.addEventListener(new KPEventListener() {
            @Override
            public void onKPlayerStateChanged(PlayerViewController playerViewController, KPlayerState state) {
                Log.d("KPlayer State Changed", state.toString());
                if (state == KPlayerState.READY) {
                    Log.d("Duration", Double.toString(playerViewController.getDurationSec()) );
                }
            }

            @Override
            public void onKPlayerPlayheadUpdate(PlayerViewController playerViewController, float currentTime) {
                Log.d("KPlayer State Changed", Float.toString(currentTime));
            }

            @Override
            public void onKPlayerFullScreenToggeled(PlayerViewController playerViewController, boolean isFullscreen) {
                Log.d("KPlayer toggeled", Boolean.toString(isFullscreen));
            }
        });
```
DEMO - Better Than Words
=====

You can check out our demo which will help you to better understand our SDK:
[Kaltura Demos](https://github.com/kaltura/player-sdk-native-android/tree/develop/KalturaDemos)



License and Copyright Information
===

All player-sdk-native-ios code is released under the AGPLv3 unless a different license for a particular library is specified in the applicable library path
