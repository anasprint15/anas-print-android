package com.anasprint.comptabilite;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int PICK_FILE = 101;
    private static final int SAVE_FILE = 102;
    private String pendingBackup = "";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(18,115,185));
        getWindow().setNavigationBarColor(android.graphics.Color.BLACK);
        webView = new WebView(this);
        webView.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                startActivityForResult(intent, PICK_FILE);
                return true;
            }
        });
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {
        @JavascriptInterface public void saveBackup(String json) {
            pendingBackup = json;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "anas_print_comptabilite_backup.json");
                startActivityForResult(intent, SAVE_FILE);
            });
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri[] result = (resultCode == RESULT_OK && data != null) ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        } else if (requestCode == SAVE_FILE && resultCode == RESULT_OK && data != null) {
            try (OutputStream out = getContentResolver().openOutputStream(data.getData())) {
                if (out != null) out.write(pendingBackup.getBytes(StandardCharsets.UTF_8));
                new AlertDialog.Builder(this).setMessage("تم حفظ النسخة الاحتياطية بنجاح.")
                    .setPositiveButton("حسناً", null).show();
            } catch (Exception e) {
                new AlertDialog.Builder(this).setMessage("تعذر حفظ النسخة الاحتياطية.")
                    .setPositiveButton("حسناً", null).show();
            }
        }
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else new AlertDialog.Builder(this).setMessage("هل تريد الخروج من التطبيق؟")
            .setNegativeButton("إلغاء", null)
            .setPositiveButton("خروج", (d,w) -> finish()).show();
    }
}
