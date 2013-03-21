package com.wyeknot.copeakid;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class LicenseView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.license_view);
		
		WebView web = (WebView)findViewById(R.id.license_webview);
		web.clearCache(true);
		web.loadUrl("file:///android_asset/gpl.html");
	}
}
