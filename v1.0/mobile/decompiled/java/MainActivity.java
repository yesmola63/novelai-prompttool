package com.novelai.prompttool;

import android.content.ClipData;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.io.ByteStreamsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* JADX INFO: compiled from: MainActivity.kt */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0011\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\u0018\u0000 \u001a2\u00020\u0001:\u0001\u001aB\u0005¢\u0006\u0002\u0010\u0002J\u001a\u0010\t\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0002J\u001a\u0010\u000f\u001a\u00020\n2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0002J\"\u0010\u0010\u001a\u00020\n2\u0006\u0010\u0011\u001a\u00020\f2\u0006\u0010\u000b\u001a\u00020\f2\b\u0010\r\u001a\u0004\u0018\u00010\u000eH\u0014J\b\u0010\u0012\u001a\u00020\nH\u0016J\u0012\u0010\u0013\u001a\u00020\n2\b\u0010\u0014\u001a\u0004\u0018\u00010\u0015H\u0014J\b\u0010\u0016\u001a\u00020\nH\u0014J\u0010\u0010\u0017\u001a\u00020\u00182\u0006\u0010\u0019\u001a\u00020\u0006H\u0002R\u001c\u0010\u0003\u001a\u0010\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00060\u0005\u0018\u00010\u0004X\u0082\u000e¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\bX\u0082.¢\u0006\u0002\n\u0000¨\u0006\u001b"}, d2 = {"Lcom/novelai/prompttool/MainActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "multiFilePathCallback", "Landroid/webkit/ValueCallback;", "", "Landroid/net/Uri;", "server", "Lcom/novelai/prompttool/PromptsServer;", "handleDirPickerResult", "", "resultCode", "", "data", "Landroid/content/Intent;", "handleLoadFilePickerResult", "onActivityResult", "requestCode", "onBackPressed", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onDestroy", "queryFileName", "", "uri", "Companion", "app_release"}, k = 1, mv = {1, 9, 0}, xi = ConstraintLayout.LayoutParams.Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE)
public final class MainActivity extends AppCompatActivity {
    public static final int REQUEST_DIR_PICKER = 1003;
    public static final int REQUEST_IMAGE_PICKER = 1001;
    public static final int REQUEST_LOAD_FILE_PICKER = 1002;
    private ValueCallback<Uri[]> multiFilePathCallback;
    private PromptsServer server;

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PromptsServer promptsServer = new PromptsServer(this, 0);
        this.server = promptsServer;
        promptsServer.start(2000);
        PromptsServer promptsServer2 = this.server;
        if (promptsServer2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("server");
            promptsServer2 = null;
        }
        int listeningPort = promptsServer2.getListeningPort();
        WebView webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        Intrinsics.checkNotNullExpressionValue(settings, "getSettings(...)");
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setCacheMode(2);
        settings.setLoadWithOverviewMode(false);
        settings.setUseWideViewPort(false);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setLayerType(2, null);
        webView.setBackgroundColor(0);
        webView.setWebViewClient(new WebViewClient() { // from class: com.novelai.prompttool.MainActivity.onCreate.1
            @Override // android.webkit.WebViewClient
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        });
        webView.setOverScrollMode(2);
        webView.setWebChromeClient(new WebChromeClient() { // from class: com.novelai.prompttool.MainActivity.onCreate.2
            @Override // android.webkit.WebChromeClient
            public boolean onShowFileChooser(WebView webView2, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                MainActivity.this.multiFilePathCallback = filePathCallback;
                MainActivity.this.startActivityForResult(new Intent("android.intent.action.PICK", MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 1001);
                return true;
            }
        });
        webView.loadUrl("http://127.0.0.1:" + listeningPort + "/");
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri[] uriArr;
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 1001) {
            if (requestCode == 1002) {
                handleLoadFilePickerResult(resultCode, data);
                return;
            } else {
                if (requestCode == 1003) {
                    handleDirPickerResult(resultCode, data);
                    return;
                }
                return;
            }
        }
        ValueCallback<Uri[]> valueCallback = this.multiFilePathCallback;
        this.multiFilePathCallback = null;
        if (valueCallback == null) {
            return;
        }
        if (resultCode == -1 && data != null) {
            ClipData clipData = data.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                int itemCount = clipData.getItemCount();
                uriArr = new Uri[itemCount];
                for (int i = 0; i < itemCount; i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    Intrinsics.checkNotNullExpressionValue(uri, "getUri(...)");
                    uriArr[i] = uri;
                }
            } else {
                uriArr = new Uri[1];
                Uri data2 = data.getData();
                if (data2 == null) {
                    data2 = Uri.EMPTY;
                }
                Intrinsics.checkNotNull(data2);
                uriArr[0] = data2;
            }
        } else {
            uriArr = new Uri[0];
        }
        valueCallback.onReceiveValue(uriArr);
    }

    private final void handleLoadFilePickerResult(int resultCode, Intent data) {
        PromptsServer promptsServer = this.server;
        PromptsServer promptsServer2 = null;
        if (promptsServer == null) {
            Intrinsics.throwUninitializedPropertyAccessException("server");
            promptsServer = null;
        }
        CompletableFuture<FilePickerResult> filePickerFuture = promptsServer.getFilePickerFuture();
        if (filePickerFuture == null) {
            PromptsServer promptsServer3 = this.server;
            if (promptsServer3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("server");
            } else {
                promptsServer2 = promptsServer3;
            }
            Log.i("MainActivity", "load_file_picker_result: no_pending_future (seq=" + promptsServer2.getExpectedPickerSeq() + ")");
            return;
        }
        if (resultCode != -1 || data == null || data.getData() == null) {
            Log.i("MainActivity", "load_file_picker_result: cancelled");
            filePickerFuture.complete(new FilePickerResult(false, null, null, null, false, false, 54, null));
            return;
        }
        Uri data2 = data.getData();
        Intrinsics.checkNotNull(data2);
        try {
            String strQueryFileName = queryFileName(data2);
            File file = new File(getCacheDir(), "load_cache");
            file.mkdirs();
            File file2 = new File(file, strQueryFileName);
            InputStream inputStreamOpenInputStream = getContentResolver().openInputStream(data2);
            if (inputStreamOpenInputStream == null) {
                Log.w("MainActivity", "load_file_picker_result: cannot_open_uri");
                filePickerFuture.complete(new FilePickerResult(false, null, null, "无法读取文件内容", false, false, 54, null));
                return;
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                InputStream inputStream = inputStreamOpenInputStream;
                try {
                    ByteStreamsKt.copyTo$default(inputStream, fileOutputStream, 0, 2, null);
                    CloseableKt.closeFinally(inputStream, null);
                    CloseableKt.closeFinally(fileOutputStream, null);
                    PromptsServer promptsServer4 = this.server;
                    if (promptsServer4 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("server");
                        promptsServer4 = null;
                    }
                    if (promptsServer4.getFilePickerFuture() != filePickerFuture) {
                        PromptsServer promptsServer5 = this.server;
                        if (promptsServer5 == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("server");
                        } else {
                            promptsServer2 = promptsServer5;
                        }
                        Log.i("MainActivity", "load_file_picker_result: stale_future_dropped (seq=" + promptsServer2.getExpectedPickerSeq() + ")");
                        file2.delete();
                        return;
                    }
                    String absolutePath = file2.getAbsolutePath();
                    Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
                    filePickerFuture.complete(new FilePickerResult(true, absolutePath, strQueryFileName, null, true, false, 40, null));
                    String absolutePath2 = file2.getAbsolutePath();
                    PromptsServer promptsServer6 = this.server;
                    if (promptsServer6 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("server");
                    } else {
                        promptsServer2 = promptsServer6;
                    }
                    Log.i("MainActivity", "load_file_picker_result: completed path=" + absolutePath2 + " seq=" + promptsServer2.getExpectedPickerSeq());
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        CloseableKt.closeFinally(inputStream, th);
                        throw th2;
                    }
                }
            } catch (Throwable th3) {
                try {
                    throw th3;
                } catch (Throwable th4) {
                    CloseableKt.closeFinally(fileOutputStream, th3);
                    throw th4;
                }
            }
        } catch (Exception e) {
            Log.w("MainActivity", "load_file_picker_result: exception", e);
            String message = e.getMessage();
            if (message == null) {
                message = "选择文件失败";
            }
            filePickerFuture.complete(new FilePickerResult(false, null, null, message, false, false, 54, null));
        }
    }

    private final String queryFileName(Uri uri) throws IOException {
        String string;
        Cursor cursorQuery = getContentResolver().query(uri, null, null, null, null);
        if (cursorQuery == null) {
            string = "selected_bundle.bundle";
        } else {
            Cursor cursor = cursorQuery;
            try {
                Cursor cursor2 = cursor;
                if (!cursor2.moveToFirst()) {
                    string = "selected_bundle.bundle";
                } else {
                    string = cursor2.getString(cursor2.getColumnIndexOrThrow("_display_name"));
                    Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                }
                Unit unit = Unit.INSTANCE;
                CloseableKt.closeFinally(cursor, null);
            } catch (Throwable th) {
                try {
                    throw th;
                } catch (Throwable th2) {
                    CloseableKt.closeFinally(cursor, th);
                    throw th2;
                }
            }
        }
        if (string.length() == 0) {
            String lastPathSegment = uri.getLastPathSegment();
            string = lastPathSegment != null ? lastPathSegment : "selected_bundle.bundle";
        }
        if (StringsKt.endsWith(string, ".bundle", true)) {
            return string;
        }
        return StringsKt.endsWith(string, ".zip", true) ? ((Object) string) + ".bundle" : ((Object) string) + ".bundle";
    }

    private final void handleDirPickerResult(int resultCode, Intent data) {
        PromptsServer promptsServer = this.server;
        if (promptsServer == null) {
            Intrinsics.throwUninitializedPropertyAccessException("server");
            promptsServer = null;
        }
        CompletableFuture<String> dirPickerFuture = promptsServer.getDirPickerFuture();
        if (dirPickerFuture == null) {
            return;
        }
        if (resultCode != -1 || data == null || data.getData() == null) {
            dirPickerFuture.complete("");
            return;
        }
        Uri data2 = data.getData();
        Intrinsics.checkNotNull(data2);
        dirPickerFuture.complete(data2.toString());
    }

    @Override // androidx.activity.ComponentActivity, android.app.Activity
    public void onBackPressed() {
        ((WebView) findViewById(R.id.webView)).evaluateJavascript("typeof closeTopModal === 'function' && closeTopModal()", new ValueCallback() { // from class: com.novelai.prompttool.MainActivity$$ExternalSyntheticLambda0
            @Override // android.webkit.ValueCallback
            public final void onReceiveValue(Object obj) {
                MainActivity.onBackPressed$lambda$3(this.f$0, (String) obj);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onBackPressed$lambda$3(MainActivity this$0, String str) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (Intrinsics.areEqual(str, "true")) {
            return;
        }
        super.onBackPressed();
    }

    @Override // androidx.appcompat.app.AppCompatActivity, androidx.fragment.app.FragmentActivity, android.app.Activity
    protected void onDestroy() {
        PromptsServer promptsServer = this.server;
        if (promptsServer == null) {
            Intrinsics.throwUninitializedPropertyAccessException("server");
            promptsServer = null;
        }
        promptsServer.stop();
        super.onDestroy();
    }
}
