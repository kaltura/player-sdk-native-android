# CastCompanionLibrary-android

CastCompanionLibrary-android is a library project to enable developers integrate Cast capabilities into their applications faster and easier.

## Dependencies
* google-play-services_lib library from the Android SDK (at least version 4.3)
* android-support-v7-appcompat (version 19.0.1 or above)
* android-support-v7-mediarouter (version 19.0.1 or above)

## Setup Instructions
* Set up the project dependencies

## Documentation
See the "CastCompanionLibray.pdf" inside the project for a more extensive documentation.

## References and How to report bugs
* [Cast Developer Documentation](http://developers.google.com/cast/)
* [Design Checklist](http://developers.google.com/cast/docs/design_checklist)
* If you find any issues with this library, please open a bug here on GitHub
* Question are answered on [StackOverflow](http://stackoverflow.com/questions/tagged/google-cast)

## How to make contributions?
Please read and follow the steps in the CONTRIBUTING.md

## License
See LICENSE

## Google+
Google Cast Developers Community on Google+ [http://goo.gl/TPLDxj](http://goo.gl/TPLDxj)

## Change List
1.1 -> 1.2
 * Improving thread-safety in calling various ConsumerImpl callbacks
 * (backward incompatible) Changing the signature of IMediaAuthListener.onResult
 * Adding an API to BaseCastManager so clients can clear the "context" to avoid any leaks
 * Various bug fixes

1.0 -> 1.1
 * Added gradle build scripts (make sure you have Android Support Repository)
 * For live media, the "pause" button at various places is replaced with a "stop" button
 * Refactored the VideoCastControllerActivity to enable configuration changes without losing any running process
 * Added new capabilities for clients to hook in an authorization process prior to casting a video
 * A number of bug fixes, style fixes, etc
 * Updated documentation
