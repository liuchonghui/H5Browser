
Compile:
```
implementation 'tools.android:h5browser:0.0.1'
implementation 'tools.android:utils:1.1.4'
```
Usage:
```
// u can
intent = new Intent("tools.android.h5browser.launch_action");
intent.putExtra("url", url);
// also u can
intent = new Intent(Intent.ACTION_VIEW);
intent.setData(Uri.parse("h5br://launch?url=" + url));
// and also
intent = new Intent(Intent.ACTION_VIEW);
intent.setData(Uri.parse("h5br://launch/h5?url=" + url));
```
