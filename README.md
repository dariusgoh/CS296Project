# CS296Project
Location based Anroid app that creates chatrooms where nearby users with matching interests can chat with each other.

In the app folder of the project, there are two files, app-release.apk and app-debug.apk.  Either of these can be transferred to an Android device, installed and ran.  Alternatively, if you have Android Studio installed, you can open up Android Studio.  In the main menu, there is an option to check out from Version Control and you can clone this repo directly into Android Studio.  From there, you will have to setup and run an emulator.  You can do this by clicking Run, Run 'app' from the menu bar.  Here you can create a virtual device.  Choose the option with Google APIs.  After creating the emulator, click the green play button to launch the emulator without running the app, since it will install and run an unsigned version of the app which will not function.  To install the app on the emulator, run:
```java
adb install app-release.apk
```
or if re-installing
```java
adb install -r app-release.apk
```

In order to run the app, you must go into the settings menu of the phone/emulator and enable location services for the app as well as have location tracking enabled.
