package com.wyeknot.copeakid;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

public class HelpView extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_view);
		
		WebView web = (WebView)findViewById(R.id.help_webview);
		web.clearCache(true);
		web.loadUrl("file:///android_asset/help.html");
	}
}
