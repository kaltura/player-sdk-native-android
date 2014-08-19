Player SDK Native Android
=================
For detailed usage guide see [Knowledge Center Android SDK usage guide](http://knowledge.kaltura.com/kaltura-player-sdk-android)

**Note**: The Kaltura native player component is in beta. If your a Kaltura customer please contact your Kaltura customer success manager to help facilitate use of this component. 

The Kaltura player-sdk-native-android component enables embedding the [kaltura player](http://player.kaltura.com) into native environments. This enables full HTML5 player platform, without limitations of HTML5 video tag API in Android platforms. Currently for Android this enables: 
* Inline playback with HTML controls ( disable controls during ads etc. ) 
* Widevine DRM support
* AutoPlay
* Volume Control
* [player.kaltura.com](http://player.kaltura.com) feature set

For a full list of native embed advantages see native controls table within the [player toolkit basic usage guide](http://knowledge.kaltura.com/kaltura-player-v2-toolkit-theme-skin-guide). 

The Kaltura player-sdk-native component can be embedded into both native apps, and hybrid native apps ( via standard dynamic embed syntax ) 

![alt text](http://html5video.org/presentations/HTML5PartDeux.FOSDEM.2014/player-native.png "player in native")
![alt text](http://html5video.org/presentations/HTML5PartDeux.FOSDEM.2014/player-native2.png "player in webview")

Future support will include: 
* HLS software player
* PlayReady DRM
* Multiple stream playback
* Offline viewing

Architecture Overview
=====
![alt text](http://html5video.org/presentations/HTML5PartDeux.FOSDEM.2014/koverview.jpg "Architecture Overview")


Quick Start Guide
======

```
git clone https://github.com/kaltura/player-sdk-native-android.git
```

Import all 5 projects to your workspace:
* PlayerSDK
* CastCompanionLibrary-android
* google-play-services_lib
* android-support-v7-appcompat
* android-support-v7-mediarouter

Verify all projects dependencies are valid.


API Overview
=====

The player includes the same KDP api available in webviews this includes: 
* kdp.asyncEvaluate( property, callback );
* kdp.setKDPAttribute( property, value );
* kdp.addListener( name, callback );
