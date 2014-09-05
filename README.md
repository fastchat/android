# FastChat Android #

The Android App for FastChat.

## Setup ##
Our IDE of choice is [Android Studio](https://developer.android.com/sdk/installing/studio.html). Download the latest version and install it.

Once installed, open it up. Go to Tools -> Android -> SDK Manager. We have to install some packages before even starting. Install the following:

* Tools/Android SDK Tools
* Tools/Android SDK Platform-Tools
* Tools/Android SDK Build-Tools (Highest version)
* Android L (API 20) Folder
* Android 4.4.2 Folder (API 19)
* Android 4.0.3 Folder (API 20)
* Extras/Android Support Repository
* Extras/Android Support Library
* Extras/Google Play Services
* Extras/Google Play Billing Library
* Extras/Google Play Licensing Library

Accept the agreements and install all those.
Close the SDK Manager.

Open the AVD Manager. In Android Studio, Tools -> Android -> AVD Manager.
Click "Create"
Create it with this exact setup:
![Setup](http://f.cl.ly/items/2n323S0X3f470l433H3S/Screen%20Shot%202014-09-04%20at%205.47.59%20PM.png)
Click the Emulator you just made. Click "Start". Launch.
Wait for Emulator to launch.
Unlock the Device
Go to the "All Apps"
Open Settings
+ Add Account. Add your actual google account

Open Maps app.
Accept & Continue
Sign in. Choose the Google Account you used.
Does it work? Good.
Go back to the home screen.

Open the Project in Android Studio.
Android Studio runs it's actions in the bottom bar, so you can see what's happening.
For example, updating Maven appears to take forever.

When things look good, hit run.
Run on the simulator (Running Device)
You'll get a warning that says "Update Google Play Services"
Clicking Update does nothing, sadly. Just click off the warning on the background a few times.

Login!




