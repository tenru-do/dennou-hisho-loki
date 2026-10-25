# Navigation header layout check

Runs the production NavigationHeaderStyle and NavigationHeaderLayout source in a separate, permission-free Android package. It measures and draws real TextViews for Japanese status text at three font scales, two widths and full/half-height budgets. It never reads Loki data or starts navigation, microphones, cameras or provider requests.

Use an authorized Android device with USB debugging, Android SDK 35/build tools 37 and a JDK (JAVA_HOME). Set ANDROID_SDK_ROOT if necessary:

```powershell
python tools/navigation-header-ui/run.py --serial <ADB_SERIAL> --output .build-header-validation
```

The output directory must be new. The temporary diagnostic package is removed on completion; if USB disconnects, reconnect before removing that diagnostic package. Preserve result.json and instrumentation.txt to inspect failures.
