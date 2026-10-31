package com.novelai.prompttool;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import fi.iki.elonen.NanoHTTPD;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import kotlin.Metadata;
import kotlin.UByte;
import kotlin.Unit;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.ByteStreamsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.ranges.RangesKt;
import kotlin.text.Charsets;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlin.text.Typography;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: compiled from: PromptsServer.kt */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000 \u0001\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\b\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0002\b\u000b\n\u0002\u0010\u000b\n\u0002\b\u000b\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0012\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u000e\n\u0002\u0010$\n\u0002\b\u0011\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 l2\u00020\u0001:\u0001lB\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005¢\u0006\u0002\u0010\u0006J \u0010 \u001a\u00020!2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u0011H\u0002J \u0010%\u001a\u00020!2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\r2\u0006\u0010&\u001a\u00020\u0011H\u0002J \u0010'\u001a\u00020!2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\r2\u0006\u0010$\u001a\u00020\u0011H\u0002J\u0018\u0010(\u001a\u00020!2\u0006\u0010\"\u001a\u00020\r2\u0006\u0010#\u001a\u00020\rH\u0002J(\u0010)\u001a\u00020!2\u0006\u0010*\u001a\u00020\r2\u0006\u0010+\u001a\u00020\r2\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020\u0005H\u0002J\u0018\u0010/\u001a\u00020!2\u0006\u0010*\u001a\u00020\r2\u0006\u0010+\u001a\u00020\rH\u0002J\u0010\u00100\u001a\u00020!2\u0006\u00101\u001a\u00020\rH\u0002J\u0010\u00102\u001a\u00020\u00112\u0006\u00103\u001a\u00020\u0011H\u0002J\u0018\u00104\u001a\u00020!2\u0006\u00105\u001a\u00020\r2\u0006\u00106\u001a\u00020\rH\u0002J\u0016\u00107\u001a\u00020\u00112\f\u00108\u001a\b\u0012\u0004\u0012\u00020\u001109H\u0002J\u000e\u0010:\u001a\n\u0012\u0004\u0012\u00020\u0011\u0018\u00010\u0010J\u0006\u0010;\u001a\u00020\u0005J\u000e\u0010<\u001a\n\u0012\u0004\u0012\u00020\u0013\u0018\u00010\u0010J\u0010\u0010=\u001a\u00020\u00112\u0006\u0010>\u001a\u00020?H\u0002J\u0010\u0010@\u001a\u00020\u00112\u0006\u0010A\u001a\u00020BH\u0002J \u0010C\u001a\u00020D2\u0006\u0010E\u001a\u00020F2\u0006\u0010G\u001a\u00020\u00112\u0006\u0010H\u001a\u00020IH\u0002J\u0010\u0010J\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010K\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010L\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010M\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010N\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010O\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010P\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010Q\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010R\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010S\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0002J\u0018\u0010T\u001a\u00020D2\u0006\u0010U\u001a\u00020\u00052\u0006\u0010V\u001a\u00020\u0011H\u0002J\u001c\u0010W\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00110X2\u0006\u0010G\u001a\u00020\u0011H\u0002J\u0018\u0010Y\u001a\u00020?2\u0006\u0010A\u001a\u00020B2\u0006\u0010Z\u001a\u00020\u0005H\u0002J\u0010\u0010[\u001a\u00020\u00182\u0006\u0010E\u001a\u00020FH\u0002J\u0010\u0010\\\u001a\u00020D2\u0006\u0010E\u001a\u00020FH\u0016J\u0010\u0010]\u001a\u00020D2\u0006\u0010G\u001a\u00020\u0011H\u0002J\u0010\u0010^\u001a\u00020D2\u0006\u0010G\u001a\u00020\u0011H\u0002J \u0010_\u001a\u00020D2\u0006\u0010`\u001a\u00020\r2\u0006\u0010a\u001a\u00020\u00052\u0006\u0010b\u001a\u00020\u0011H\u0002J\u0010\u0010c\u001a\u00020\u00112\u0006\u0010>\u001a\u00020?H\u0002J\u0010\u0010d\u001a\u00020\u00112\u0006\u0010A\u001a\u00020BH\u0002J\u0010\u0010e\u001a\u00020!2\b\b\u0002\u0010f\u001a\u00020\u0011J\u0006\u0010g\u001a\u00020!J \u0010h\u001a\u00020!2\u0006\u0010i\u001a\u00020j2\u0006\u00101\u001a\u00020\r2\u0006\u0010k\u001a\u00020\u0011H\u0002R\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u000e\u0010\t\u001a\u00020\nX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\u000e\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00110\u00100\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\u0012\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\u00130\u00100\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0014\u001a\u00020\u0015X\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\u0016\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00180\u0017X\u0082\u0004¢\u0006\u0002\n\u0000R\u001a\u0010\u0019\u001a\u000e\u0012\u0004\u0012\u00020\u0011\u0012\u0004\u0012\u00020\u00180\u0017X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u001a\u001a\u00020\u001bX\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u001c\u001a\b\u0012\u0004\u0012\u00020\u00110\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u001d\u001a\b\u0012\u0004\u0012\u00020\u00110\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u001e\u001a\b\u0012\u0004\u0012\u00020\u00110\u000fX\u0082\u0004¢\u0006\u0002\n\u0000R\u0014\u0010\u001f\u001a\b\u0012\u0004\u0012\u00020\u00110\u000fX\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006m"}, d2 = {"Lcom/novelai/prompttool/PromptsServer;", "Lfi/iki/elonen/NanoHTTPD;", "activity", "Lcom/novelai/prompttool/MainActivity;", "port", "", "(Lcom/novelai/prompttool/MainActivity;I)V", "getActivity", "()Lcom/novelai/prompttool/MainActivity;", "appContext", "Landroid/content/Context;", "context", "dataDir", "Ljava/io/File;", "dirPickerFuture", "Ljava/util/concurrent/atomic/AtomicReference;", "Ljava/util/concurrent/CompletableFuture;", "", "filePickerFuture", "Lcom/novelai/prompttool/FilePickerResult;", "filePickerSeq", "Ljava/util/concurrent/atomic/AtomicInteger;", "loadResults", "Ljava/util/concurrent/ConcurrentHashMap;", "Lorg/json/JSONObject;", "packResults", "promptManager", "Lcom/novelai/prompttool/PromptManager;", "selectedLoadFilePath", "selectedLoadPathDecrypted", "selectedPackDirPath", "selectedPackDirUri", "compressTo7z", "", "sourceDir", "destFile", "password", "compressToZip", "zipMode", "compressToZipEncrypted", "compressToZipStored", "convertToWebp", "src", "dest", "lossless", "", "quality", "copyFileStream", "deleteDir", "dir", "escapeJson", "s", "extractZip", "zipFile", "destDir", "fastJsonArr", "list", "Ljava/util/ArrayList;", "getDirPickerFuture", "getExpectedPickerSeq", "getFilePickerFuture", "guessExtension", "data", "", "guessExtensionStream", "inputStream", "Ljava/io/InputStream;", "handleApi", "Lfi/iki/elonen/NanoHTTPD$Response;", "session", "Lfi/iki/elonen/NanoHTTPD$IHTTPSession;", "uri", "method", "Lfi/iki/elonen/NanoHTTPD$Method;", "handleApplyLoad", "handleCheckPackFilename", "handleChooseDir", "handlePack", "handlePollChooseDir", "handlePollSelectLoadFile", "handleSaveImage", "handleSelectLoadFile", "handleUnlockLoadFile", "handleUploadImages", "jsonResp", NotificationCompat.CATEGORY_STATUS, "body", "parseQuery", "", "readBytesFully", "expectedSize", "readJsonBody", "serve", "serveAsset", "servePicture", "serveThumbnail", "imgFile", "thumbWidth", "mimeType", "sha1Hex", "sha1HexStream", "triggerDirPicker", "filename", "triggerFilePicker", "walkAndZipStored", "zos", "Ljava/util/zip/ZipOutputStream;", "base", "Companion", "app_release"}, k = 1, mv = {1, 9, 0}, xi = ConstraintLayout.LayoutParams.Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE)
public final class PromptsServer extends NanoHTTPD {
    private static final String PACK_DIR_NAME = "prompt_pack";
    private final MainActivity activity;
    private final Context appContext;
    private final MainActivity context;
    private final File dataDir;
    private final AtomicReference<CompletableFuture<String>> dirPickerFuture;
    private final AtomicReference<CompletableFuture<FilePickerResult>> filePickerFuture;
    private final AtomicInteger filePickerSeq;
    private final ConcurrentHashMap<String, JSONObject> loadResults;
    private final ConcurrentHashMap<String, JSONObject> packResults;
    private final PromptManager promptManager;
    private final AtomicReference<String> selectedLoadFilePath;
    private final AtomicReference<String> selectedLoadPathDecrypted;
    private final AtomicReference<String> selectedPackDirPath;
    private final AtomicReference<String> selectedPackDirUri;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public PromptsServer(MainActivity activity, int i) {
        super(i);
        Intrinsics.checkNotNullParameter(activity, "activity");
        this.activity = activity;
        this.context = activity;
        MainActivity mainActivity = activity;
        this.appContext = mainActivity;
        File externalFilesDir = mainActivity.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = mainActivity.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(externalFilesDir, "getFilesDir(...)");
        }
        this.dataDir = externalFilesDir;
        this.promptManager = new PromptManager(mainActivity);
        this.filePickerFuture = new AtomicReference<>(null);
        this.filePickerSeq = new AtomicInteger(0);
        this.dirPickerFuture = new AtomicReference<>(null);
        this.selectedLoadFilePath = new AtomicReference<>("");
        this.selectedLoadPathDecrypted = new AtomicReference<>("");
        this.selectedPackDirPath = new AtomicReference<>("");
        this.selectedPackDirUri = new AtomicReference<>("");
        this.packResults = new ConcurrentHashMap<>();
        this.loadResults = new ConcurrentHashMap<>();
    }

    public /* synthetic */ PromptsServer(MainActivity mainActivity, int i, int i2, DefaultConstructorMarker defaultConstructorMarker) {
        this(mainActivity, (i2 & 2) != 0 ? 8080 : i);
    }

    public final MainActivity getActivity() {
        return this.activity;
    }

    public final CompletableFuture<FilePickerResult> getFilePickerFuture() {
        return this.filePickerFuture.get();
    }

    public final CompletableFuture<String> getDirPickerFuture() {
        return this.dirPickerFuture.get();
    }

    public final int getExpectedPickerSeq() {
        return this.filePickerSeq.get();
    }

    public final void triggerFilePicker() {
        this.activity.runOnUiThread(new Runnable() { // from class: com.novelai.prompttool.PromptsServer$$ExternalSyntheticLambda2
            @Override // java.lang.Runnable
            public final void run() {
                PromptsServer.triggerFilePicker$lambda$1(this.f$0);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void triggerFilePicker$lambda$1(PromptsServer this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intent intent = new Intent("android.intent.action.GET_CONTENT");
        intent.setType("*/*");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.addFlags(1);
        this$0.activity.startActivityForResult(intent, 1002);
    }

    public static /* synthetic */ void triggerDirPicker$default(PromptsServer promptsServer, String str, int i, Object obj) {
        if ((i & 1) != 0) {
            str = "prompt.bundle";
        }
        promptsServer.triggerDirPicker(str);
    }

    public final void triggerDirPicker(final String filename) {
        Intrinsics.checkNotNullParameter(filename, "filename");
        this.activity.runOnUiThread(new Runnable() { // from class: com.novelai.prompttool.PromptsServer$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                PromptsServer.triggerDirPicker$lambda$3(this.f$0, filename);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void triggerDirPicker$lambda$3(PromptsServer this$0, String filename) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(filename, "$filename");
        Intent intent = new Intent("android.intent.action.CREATE_DOCUMENT");
        intent.setType("*/*");
        intent.addCategory("android.intent.category.OPENABLE");
        intent.putExtra("android.intent.extra.TITLE", filename);
        this$0.activity.startActivityForResult(intent, 1003);
    }

    @Override // fi.iki.elonen.NanoHTTPD
    public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
        Intrinsics.checkNotNullParameter(session, "session");
        String uri = session.getUri();
        NanoHTTPD.Method method = session.getMethod();
        Intrinsics.checkNotNull(uri);
        if (StringsKt.startsWith$default(uri, "/picture/", false, 2, (Object) null)) {
            return servePicture(uri);
        }
        if (StringsKt.startsWith$default(uri, "/api/", false, 2, (Object) null)) {
            Intrinsics.checkNotNull(method);
            return handleApi(session, uri, method);
        }
        return serveAsset(uri);
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Code duplicated, block: B:13:0x0056 A[PHI: r12
  0x0056: PHI (r12v2 java.lang.String) = 
  (r12v1 java.lang.String)
  (r12v24 java.lang.String)
  (r12v24 java.lang.String)
  (r12v24 java.lang.String)
  (r12v24 java.lang.String)
 binds: [B:3:0x001c, B:5:0x003b, B:7:0x0041, B:9:0x0049, B:11:0x004f] A[DONT_GENERATE, DONT_INLINE]] */
    /* JADX WARN: Code duplicated, block: B:44:0x013d  */
    /* JADX WARN: Code duplicated, block: B:53:0x0158  */
    private final NanoHTTPD.Response servePicture(String uri) {
        int iIntValue;
        String strDecode;
        String str;
        List<String> groupValues;
        String str2;
        Integer intOrNull;
        String strSubstring = uri.substring(9);
        Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
        int iIndexOf$default = StringsKt.indexOf$default((CharSequence) strSubstring, '?', 0, false, 6, (Object) null);
        File file = null;
        if (iIndexOf$default >= 0) {
            String strSubstring2 = strSubstring.substring(iIndexOf$default + 1);
            Intrinsics.checkNotNullExpressionValue(strSubstring2, "substring(...)");
            strSubstring = strSubstring.substring(0, iIndexOf$default);
            Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
            MatchResult matchResultFind$default = Regex.find$default(new Regex("w=(\\d+)"), strSubstring2, 0, 2, null);
            if (matchResultFind$default == null || (groupValues = matchResultFind$default.getGroupValues()) == null || (str2 = groupValues.get(1)) == null || (intOrNull = StringsKt.toIntOrNull(str2)) == null) {
                iIntValue = 0;
            } else {
                iIntValue = intOrNull.intValue();
            }
        } else {
            iIntValue = 0;
        }
        try {
            strDecode = URLDecoder.decode(strSubstring, Charsets.UTF_8.name());
        } catch (Exception unused) {
            strDecode = strSubstring;
        }
        for (File file2 : CollectionsKt.listOf((Object[]) new File[]{new File(this.dataDir, "picture/" + strDecode), new File(this.dataDir, "picture/" + strSubstring), new File(this.appContext.getFilesDir(), "picture/" + strDecode), new File(this.appContext.getFilesDir(), "picture/" + strSubstring)})) {
            if (file2.exists()) {
                file = file2;
                break;
            }
        }
        if (file == null) {
            NanoHTTPD.Response responseNewFixedLengthResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "application/json", "{\"error\":\"image_not_found\"}");
            Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse, "newFixedLengthResponse(...)");
            return responseNewFixedLengthResponse;
        }
        String lowerCase = StringsKt.substringAfterLast(strSubstring, '.', "").toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
        switch (lowerCase) {
            case "bmp":
                str = "image/bmp";
                break;
            case "gif":
                str = "image/gif";
                break;
            case "jpg":
                str = "image/jpeg";
                break;
            case "png":
                str = "image/png";
                break;
            case "jpeg":
                str = "image/jpeg";
                break;
            case "webp":
                str = "image/webp";
                break;
            default:
                str = "image/octet-stream";
                break;
        }
        if (32 <= iIntValue && iIntValue < 801) {
            return serveThumbnail(file, iIntValue, str);
        }
        NanoHTTPD.Response responseNewFixedLengthResponse2 = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, str, new FileInputStream(file), file.length());
        Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse2, "newFixedLengthResponse(...)");
        return responseNewFixedLengthResponse2;
    }

    private final NanoHTTPD.Response serveThumbnail(File imgFile, int thumbWidth, String mimeType) {
        File file = new File(this.dataDir, "thumb_cache");
        file.mkdirs();
        File file2 = new File(file, imgFile.getName() + "_w" + thumbWidth + ".jpg");
        if (file2.exists() && file2.length() > 0) {
            NanoHTTPD.Response responseNewFixedLengthResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/jpeg", new FileInputStream(file2), file2.length());
            Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse, "newFixedLengthResponse(...)");
            return responseNewFixedLengthResponse;
        }
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
            int i = options.outWidth;
            int i2 = 1;
            while (true) {
                int i3 = i2 * 2;
                if (i / i3 < thumbWidth || i3 > 16) {
                    break;
                }
                i2 = i3;
            }
            BitmapFactory.Options options2 = new BitmapFactory.Options();
            options2.inSampleSize = i2;
            Bitmap bitmapDecodeFile = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options2);
            if (bitmapDecodeFile == null) {
                PromptsServer promptsServer = this;
                NanoHTTPD.Response responseNewFixedLengthResponse2 = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeType, new FileInputStream(imgFile), imgFile.length());
                Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse2, "newFixedLengthResponse(...)");
                return responseNewFixedLengthResponse2;
            }
            Bitmap bitmapCreateScaledBitmap = Bitmap.createScaledBitmap(bitmapDecodeFile, thumbWidth, RangesKt.coerceAtLeast((bitmapDecodeFile.getHeight() * thumbWidth) / bitmapDecodeFile.getWidth(), 1), true);
            Intrinsics.checkNotNullExpressionValue(bitmapCreateScaledBitmap, "createScaledBitmap(...)");
            if (bitmapCreateScaledBitmap != bitmapDecodeFile) {
                bitmapDecodeFile.recycle();
            }
            File file3 = new File(file, imgFile.getName() + "_w" + thumbWidth + ".tmp");
            FileOutputStream fileOutputStream = new FileOutputStream(file3);
            try {
                bitmapCreateScaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, fileOutputStream);
                CloseableKt.closeFinally(fileOutputStream, null);
                bitmapCreateScaledBitmap.recycle();
                if (file3.renameTo(file2)) {
                    NanoHTTPD.Response responseNewFixedLengthResponse3 = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/jpeg", new FileInputStream(file2), file2.length());
                    Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse3, "newFixedLengthResponse(...)");
                    return responseNewFixedLengthResponse3;
                }
            } catch (Throwable th) {
                try {
                    throw th;
                } catch (Throwable th2) {
                    CloseableKt.closeFinally(fileOutputStream, th);
                    throw th2;
                }
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "error";
            }
            Log.w("PromptsServer", "thumbnail failed: " + message);
        }
        NanoHTTPD.Response responseNewFixedLengthResponse4 = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, mimeType, new FileInputStream(imgFile), imgFile.length());
        Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse4, "newFixedLengthResponse(...)");
        return responseNewFixedLengthResponse4;
    }

    private final NanoHTTPD.Response handleApi(NanoHTTPD.IHTTPSession session, String uri, NanoHTTPD.Method method) {
        NanoHTTPD.Response responseHandleSaveImage;
        JSONObject jSONObject;
        String str;
        String str2;
        try {
            if (Intrinsics.areEqual(uri, "/api/prompts") && method == NanoHTTPD.Method.GET) {
                responseHandleSaveImage = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", this.promptManager.loadPrompts());
            } else if (Intrinsics.areEqual(uri, "/api/prompts/check-empty") && method == NanoHTTPD.Method.GET) {
                String strLoadPrompts = this.promptManager.loadPrompts();
                responseHandleSaveImage = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/json", "{\"empty\":" + (Intrinsics.areEqual(strLoadPrompts, "{}") || Intrinsics.areEqual(StringsKt.trim((CharSequence) strLoadPrompts).toString(), "{}")) + "}");
            } else if (Intrinsics.areEqual(uri, "/api/prompts") && method == NanoHTTPD.Method.POST) {
                JSONObject jsonBody = readJsonBody(session);
                String strOptString = jsonBody.optString("group");
                Intrinsics.checkNotNull(strOptString);
                if (strOptString.length() == 0) {
                    return jsonResp(400, "{\"error\":\"missing_group\"}");
                }
                JSONObject jSONObjectOptJSONObject = jsonBody.optJSONObject("item");
                if (jSONObjectOptJSONObject == null) {
                    return jsonResp(400, "{\"error\":\"missing_item\"}");
                }
                this.promptManager.addPrompt(strOptString, jSONObjectOptJSONObject);
                responseHandleSaveImage = jsonResp(201, "{\"message\":\"added\"}");
            } else {
                if (Intrinsics.areEqual(uri, "/api/prompts") && method == NanoHTTPD.Method.PUT) {
                    JSONObject jsonBody2 = readJsonBody(session);
                    String strOptString2 = jsonBody2.optString("originalGroup");
                    String strOptString3 = jsonBody2.optString("originalRole");
                    String strOptString4 = jsonBody2.optString("originalCommon");
                    String strOptString5 = jsonBody2.optString("newGroup");
                    JSONObject jSONObjectOptJSONObject2 = jsonBody2.optJSONObject("newItem");
                    Intrinsics.checkNotNull(strOptString2);
                    if (strOptString2.length() != 0) {
                        Intrinsics.checkNotNull(strOptString3);
                        if (strOptString3.length() != 0) {
                            Intrinsics.checkNotNull(strOptString4);
                            if (strOptString4.length() != 0) {
                                Intrinsics.checkNotNull(strOptString5);
                                if (strOptString5.length() != 0 && jSONObjectOptJSONObject2 != null) {
                                    this.promptManager.updatePrompt(strOptString2, strOptString3, strOptString4, strOptString5, jSONObjectOptJSONObject2);
                                    responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"message\":\"updated\"}");
                                }
                            }
                        }
                    }
                    return jsonResp(400, "{\"error\":\"missing_params\"}");
                }
                if (StringsKt.startsWith$default(uri, "/api/prompts", false, 2, (Object) null) && method == NanoHTTPD.Method.DELETE) {
                    Map<String, String> parms = session.getParms();
                    String str3 = parms.get("id");
                    if (str3 == null) {
                        String str4 = parms.get("group");
                        if (str4 != null && (str = parms.get("role")) != null && (str2 = parms.get("common")) != null) {
                            this.promptManager.deletePrompt(str4, str, str2);
                        }
                        return jsonResp(400, "{\"error\":\"missing_params\"}");
                    }
                    this.promptManager.deletePromptById(str3);
                    responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"message\":\"deleted\"}");
                } else if (Intrinsics.areEqual(uri, "/api/upload-images") && method == NanoHTTPD.Method.POST) {
                    responseHandleSaveImage = handleUploadImages(session);
                } else if (Intrinsics.areEqual(uri, "/api/images") && method == NanoHTTPD.Method.DELETE) {
                    JSONArray jSONArrayOptJSONArray = readJsonBody(session).optJSONArray("filenames");
                    if (jSONArrayOptJSONArray == null) {
                        return jsonResp(400, "{\"error\":\"no_filenames\"}");
                    }
                    File file = new File(this.dataDir, "picture");
                    ArrayList<String> arrayList = new ArrayList<>();
                    int length = jSONArrayOptJSONArray.length();
                    for (int i = 0; i < length; i++) {
                        String string = jSONArrayOptJSONArray.getString(i);
                        File file2 = new File(file, string);
                        if (file2.exists()) {
                            file2.delete();
                            arrayList.add(string);
                        }
                    }
                    responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"deleted\":" + fastJsonArr(arrayList) + "}");
                } else if (Intrinsics.areEqual(uri, "/api/prompts/replace") && method == NanoHTTPD.Method.PUT) {
                    JSONObject jsonBody3 = readJsonBody(session);
                    PromptManager promptManager = this.promptManager;
                    String string2 = jsonBody3.toString(2);
                    Intrinsics.checkNotNullExpressionValue(string2, "toString(...)");
                    promptManager.savePrompts(string2);
                    responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"message\":\"updated\"}");
                } else if (Intrinsics.areEqual(uri, "/api/prompt/recovery-check") && method == NanoHTTPD.Method.GET) {
                    responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"needRecovery\":false,\"params\":null}");
                } else if (Intrinsics.areEqual(uri, "/api/prompt/recover") && method == NanoHTTPD.Method.POST) {
                    responseHandleSaveImage = jsonResp(400, "{\"error\":\"not_supported\"}");
                } else if (Intrinsics.areEqual(uri, "/api/pack") && method == NanoHTTPD.Method.POST) {
                    responseHandleSaveImage = handlePack(session);
                } else if (Intrinsics.areEqual(uri, "/api/pack/choose-dir") && method == NanoHTTPD.Method.POST) {
                    responseHandleSaveImage = handleChooseDir(session);
                } else if (StringsKt.startsWith$default(uri, "/api/pack/choose-dir", false, 2, (Object) null) && method == NanoHTTPD.Method.GET) {
                    responseHandleSaveImage = handlePollChooseDir(session);
                } else if (Intrinsics.areEqual(uri, "/api/pack/check-filename") && method == NanoHTTPD.Method.POST) {
                    responseHandleSaveImage = handleCheckPackFilename(session);
                } else {
                    String str5 = "";
                    if (StringsKt.startsWith$default(uri, "/api/pack/progress", false, 2, (Object) null)) {
                        String str6 = session.getParms().get("task_id");
                        if (str6 != null) {
                            str5 = str6;
                        }
                        jSONObject = str5.length() > 0 ? this.packResults.get(str5) : null;
                        if (jSONObject != null) {
                            Log.i("PromptsServer", "pack_progress_poll task=" + str5);
                            String string3 = jSONObject.toString();
                            Intrinsics.checkNotNullExpressionValue(string3, "toString(...)");
                            responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, string3);
                        } else {
                            Log.i("PromptsServer", "pack_progress_poll_unknown_task taskId=" + str5);
                            responseHandleSaveImage = jsonResp(404, "{\"error\":\"unknown_task\"}");
                        }
                    } else if (Intrinsics.areEqual(uri, "/api/load/select-file") && method == NanoHTTPD.Method.POST) {
                        responseHandleSaveImage = handleSelectLoadFile(session);
                    } else if (StringsKt.startsWith$default(uri, "/api/load/select-file", false, 2, (Object) null) && method == NanoHTTPD.Method.GET) {
                        responseHandleSaveImage = handlePollSelectLoadFile(session);
                    } else if (Intrinsics.areEqual(uri, "/api/load/unlock") && method == NanoHTTPD.Method.POST) {
                        responseHandleSaveImage = handleUnlockLoadFile(session);
                    } else if (Intrinsics.areEqual(uri, "/api/load/apply") && method == NanoHTTPD.Method.POST) {
                        responseHandleSaveImage = handleApplyLoad(session);
                    } else if (StringsKt.startsWith$default(uri, "/api/load/progress", false, 2, (Object) null)) {
                        String str7 = session.getParms().get("task_id");
                        if (str7 != null) {
                            str5 = str7;
                        }
                        jSONObject = str5.length() > 0 ? this.loadResults.get(str5) : null;
                        if (jSONObject != null) {
                            Log.i("PromptsServer", "load_progress_poll task=" + str5 + " status=" + jSONObject.optString(NotificationCompat.CATEGORY_STATUS, "?") + " done=" + jSONObject.optBoolean("done", false));
                            String string4 = jSONObject.toString();
                            Intrinsics.checkNotNullExpressionValue(string4, "toString(...)");
                            responseHandleSaveImage = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, string4);
                        } else {
                            Log.i("PromptsServer", "load_progress_poll_unknown_task taskId=" + str5);
                            responseHandleSaveImage = jsonResp(404, "{\"error\":\"unknown_task\"}");
                        }
                    } else {
                        responseHandleSaveImage = (Intrinsics.areEqual(uri, "/api/save-image") && method == NanoHTTPD.Method.POST) ? handleSaveImage(session) : jsonResp(404, "{\"error\":\"Not found\"}");
                    }
                }
            }
            Intrinsics.checkNotNull(responseHandleSaveImage);
            return responseHandleSaveImage;
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "error";
            }
            Log.e("PromptsServer", "API error: " + uri + " " + message, e);
            return jsonResp(500, "{\"error\":\"" + message + "\"}");
        }
    }

    private final NanoHTTPD.Response handleUploadImages(NanoHTTPD.IHTTPSession session) {
        File file = new File(this.dataDir, "picture");
        file.mkdirs();
        ArrayList<String> arrayList = new ArrayList<>();
        try {
            String str = session.getHeaders().get("content-type");
            if (str == null) {
                str = "";
            }
            if (!StringsKt.contains$default((CharSequence) str, (CharSequence) "multipart/form-data", false, 2, (Object) null)) {
                return jsonResp(400, "{\"error\":\"no_file_uploaded\"}");
            }
            HashMap map = new HashMap();
            session.parseBody(map);
            for (Map.Entry<String, String> entry : map.entrySet()) {
                entry.getKey();
                String value = entry.getValue();
                if (value.length() != 0) {
                    File file2 = new File(value);
                    if (file2.exists()) {
                        String str2 = sha1HexStream(new FileInputStream(file2)) + "." + guessExtensionStream(new FileInputStream(file2));
                        File file3 = new File(file, str2);
                        if (!file3.exists()) {
                            copyFileStream(file2, file3);
                        }
                        arrayList.add(str2);
                    }
                }
            }
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"filenames\":" + fastJsonArr(arrayList) + "}");
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "upload_failed";
            }
            Log.e("PromptsServer", "upload_failed", e);
            return jsonResp(500, "{\"error\":\"" + message + "\"}");
        }
    }

    private final NanoHTTPD.Response handleCheckPackFilename(NanoHTTPD.IHTTPSession session) {
        try {
            String strOptString = readJsonBody(session).optString("filename", "");
            Intrinsics.checkNotNullExpressionValue(strOptString, "optString(...)");
            String string = StringsKt.trim((CharSequence) strOptString).toString();
            if (string.length() == 0) {
                string = "prompt";
            }
            File file = new File(new File(Environment.getExternalStorageDirectory(), "Download/prompt_pack"), string + ".bundle");
            boolean zExists = file.exists();
            String absolutePath = file.getAbsolutePath();
            Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"exists\":" + zExists + ",\"path\":\"" + escapeJson(absolutePath) + "\"}");
        } catch (Exception unused) {
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"exists\":false}");
        }
    }

    private final NanoHTTPD.Response handlePack(NanoHTTPD.IHTTPSession session) throws JSONException {
        final ArrayList arrayList;
        JSONObject jsonBody = readJsonBody(session);
        String strOptString = jsonBody.optString("filename");
        Intrinsics.checkNotNullExpressionValue(strOptString, "optString(...)");
        String string = StringsKt.trim((CharSequence) strOptString).toString();
        if (string.length() == 0) {
            string = "prompt";
        }
        final String str = string;
        JSONArray jSONArrayOptJSONArray = jsonBody.optJSONArray("selectedIds");
        if (jSONArrayOptJSONArray != null) {
            ArrayList arrayList2 = new ArrayList();
            int length = jSONArrayOptJSONArray.length();
            for (int i = 0; i < length; i++) {
                arrayList2.add(jSONArrayOptJSONArray.getString(i));
            }
            arrayList = arrayList2;
        } else {
            arrayList = null;
        }
        final boolean zOptBoolean = jsonBody.optBoolean("needPassword", false);
        final String strOptString2 = jsonBody.optString("password", "");
        final String strOptString3 = jsonBody.optString("imageMode", "none");
        final boolean zOptBoolean2 = jsonBody.optBoolean("webpLossless", false);
        final int iOptInt = jsonBody.optInt("webpQuality", 80);
        final String strOptString4 = jsonBody.optString("zipMode", "standard");
        String str2 = this.selectedPackDirUri.get();
        String str3 = str2;
        Intrinsics.checkNotNull(str3);
        final String str4 = StringsKt.isBlank(str3) ^ true ? str2 : null;
        this.selectedPackDirUri.set("");
        final String string2 = UUID.randomUUID().toString();
        Intrinsics.checkNotNullExpressionValue(string2, "toString(...)");
        ConcurrentHashMap<String, JSONObject> concurrentHashMap = this.packResults;
        JSONObject jSONObjectPut = new JSONObject().put(NotificationCompat.CATEGORY_STATUS, "pending").put("text", "packing...");
        Intrinsics.checkNotNullExpressionValue(jSONObjectPut, "put(...)");
        concurrentHashMap.put(string2, jSONObjectPut);
        new Thread(new Runnable() { // from class: com.novelai.prompttool.PromptsServer$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() throws JSONException {
                PromptsServer.handlePack$lambda$14(this.f$0, string2, str, arrayList, strOptString3, zOptBoolean2, iOptInt, zOptBoolean, strOptString2, strOptString4, str4);
            }
        }).start();
        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"task_id\":\"" + string2 + "\"}");
    }

    /* JADX INFO: Access modifiers changed from: private */
    /*  JADX ERROR: Type inference failed
        jadx.core.utils.exceptions.JadxOverflowException: Type inference error: updates count limit reached with updateSeq = 11181. Try increasing type updates limit count.
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:59)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:31)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:19)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:79)
        */
    public static final void handlePack$lambda$14(com.novelai.prompttool.PromptsServer r32, java.lang.String r33, java.lang.String r34, java.util.ArrayList r35, java.lang.String r36, boolean r37, int r38, boolean r39, java.lang.String r40, java.lang.String r41, java.lang.String r42) throws org.json.JSONException {
        /*
            Method dump skipped, instruction units count: 1118
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.novelai.prompttool.PromptsServer.handlePack$lambda$14(com.novelai.prompttool.PromptsServer, java.lang.String, java.lang.String, java.util.ArrayList, java.lang.String, boolean, int, boolean, java.lang.String, java.lang.String, java.lang.String):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean handlePack$lambda$14$lambda$9(File file, String str) {
        Intrinsics.checkNotNull(str);
        String lowerCase = str.toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
        return StringsKt.endsWith$default(lowerCase, ".png", false, 2, (Object) null);
    }

    private final void convertToWebp(File src, File dest, boolean lossless, int quality) {
        Bitmap.CompressFormat compressFormat;
        Bitmap bitmapDecodeStream = BitmapFactory.decodeStream(new FileInputStream(src));
        if (bitmapDecodeStream == null) {
            throw new RuntimeException("cannot_decode: " + src.getName());
        }
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(dest);
            try {
                FileOutputStream fileOutputStream2 = fileOutputStream;
                if (lossless) {
                    compressFormat = Bitmap.CompressFormat.WEBP_LOSSLESS;
                } else {
                    compressFormat = Bitmap.CompressFormat.WEBP;
                }
                bitmapDecodeStream.compress(compressFormat, RangesKt.coerceIn(quality, 0, 100), fileOutputStream2);
                CloseableKt.closeFinally(fileOutputStream, null);
                bitmapDecodeStream.recycle();
                src.delete();
            } catch (Throwable th) {
                try {
                    throw th;
                } catch (Throwable th2) {
                    CloseableKt.closeFinally(fileOutputStream, th);
                    throw th2;
                }
            }
        } catch (Throwable th3) {
            bitmapDecodeStream.recycle();
            throw th3;
        }
    }

    private final void compressTo7z(File sourceDir, File destFile, String password) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidAlgorithmParameterException {
        compressToZipEncrypted(sourceDir, destFile, password);
    }

    private final void compressToZipEncrypted(File sourceDir, File destFile, String password) throws BadPaddingException, NoSuchPaddingException, IllegalBlockSizeException, NoSuchAlgorithmException, IOException, InvalidKeyException, InvalidAlgorithmParameterException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destFile));
        zipOutputStream.setLevel(9);
        walkAndZipStored(zipOutputStream, sourceDir, "");
        zipOutputStream.close();
        byte[] bArr = new byte[16];
        new SecureRandom().nextBytes(bArr);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = password.getBytes(Charsets.UTF_8);
        Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
        cipher.init(1, new SecretKeySpec(messageDigest.digest(bytes), "AES"), new IvParameterSpec(bArr));
        File file = this.dataDir;
        String string = UUID.randomUUID().toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        String strSubstring = string.substring(0, 8);
        Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
        File file2 = new File(file, "temp_enc_" + strSubstring);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(destFile);
        try {
            ByteStreamsKt.copyTo$default(fileInputStream, byteArrayOutputStream, 0, 2, null);
            CloseableKt.closeFinally(fileInputStream, null);
            byte[] bArrDoFinal = cipher.doFinal(byteArrayOutputStream.toByteArray());
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            try {
                DataOutputStream dataOutputStream = new DataOutputStream(fileOutputStream);
                try {
                    DataOutputStream dataOutputStream2 = dataOutputStream;
                    dataOutputStream2.write(bArr, 0, 16);
                    dataOutputStream2.write(bArrDoFinal);
                    Unit unit = Unit.INSTANCE;
                    CloseableKt.closeFinally(dataOutputStream, null);
                    Unit unit2 = Unit.INSTANCE;
                    CloseableKt.closeFinally(fileOutputStream, null);
                    destFile.delete();
                    file2.renameTo(destFile);
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        CloseableKt.closeFinally(dataOutputStream, th);
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
        } catch (Throwable th5) {
            try {
                throw th5;
            } catch (Throwable th6) {
                CloseableKt.closeFinally(fileInputStream, th5);
                throw th6;
            }
        }
    }

    private final void compressToZip(File sourceDir, File destFile, String zipMode) throws IOException {
        int i = 9;
        if (!Intrinsics.areEqual(zipMode, "max") && !Intrinsics.areEqual(zipMode, "extreme")) {
            i = 6;
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destFile));
        try {
            ZipOutputStream zipOutputStream2 = zipOutputStream;
            zipOutputStream2.setLevel(i);
            walkAndZipStored(zipOutputStream2, sourceDir, "");
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(zipOutputStream, null);
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(zipOutputStream, th);
                throw th2;
            }
        }
    }

    private final void compressToZipStored(File sourceDir, File destFile) throws IOException {
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(destFile));
        try {
            ZipOutputStream zipOutputStream2 = zipOutputStream;
            zipOutputStream2.setLevel(0);
            walkAndZipStored(zipOutputStream2, sourceDir, "");
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(zipOutputStream, null);
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(zipOutputStream, th);
                throw th2;
            }
        }
    }

    private final void walkAndZipStored(ZipOutputStream zos, File dir, String base) throws IOException {
        File[] fileArrListFiles = dir.listFiles();
        if (fileArrListFiles == null) {
            return;
        }
        for (File file : fileArrListFiles) {
            String name = base.length() == 0 ? file.getName() : base + "/" + file.getName();
            if (file.isDirectory()) {
                zos.putNextEntry(new ZipEntry(name + "/"));
                zos.closeEntry();
                Intrinsics.checkNotNull(file);
                Intrinsics.checkNotNull(name);
                walkAndZipStored(zos, file, name);
            } else {
                zos.putNextEntry(new ZipEntry(name));
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    ByteStreamsKt.copyTo$default(fileInputStream, zos, 0, 2, null);
                    CloseableKt.closeFinally(fileInputStream, null);
                    zos.closeEntry();
                } catch (Throwable th) {
                    try {
                        throw th;
                    } catch (Throwable th2) {
                        CloseableKt.closeFinally(fileInputStream, th);
                        throw th2;
                    }
                }
            }
        }
    }

    private final void extractZip(File zipFile, File destDir) throws IOException {
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
        try {
            ZipInputStream zipInputStream2 = zipInputStream;
            for (ZipEntry nextEntry = zipInputStream2.getNextEntry(); nextEntry != null; nextEntry = zipInputStream2.getNextEntry()) {
                File file = new File(destDir, nextEntry.getName());
                if (nextEntry.isDirectory()) {
                    file.mkdirs();
                } else {
                    File parentFile = file.getParentFile();
                    if (parentFile != null) {
                        parentFile.mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        ByteStreamsKt.copyTo$default(zipInputStream2, fileOutputStream, 0, 2, null);
                        CloseableKt.closeFinally(fileOutputStream, null);
                    } catch (Throwable th) {
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            CloseableKt.closeFinally(fileOutputStream, th);
                            throw th2;
                        }
                    }
                }
                zipInputStream2.closeEntry();
            }
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(zipInputStream, null);
        } catch (Throwable th3) {
            try {
                throw th3;
            } catch (Throwable th4) {
                CloseableKt.closeFinally(zipInputStream, th3);
                throw th4;
            }
        }
    }

    private final void deleteDir(File dir) {
        File[] fileArrListFiles;
        if (dir.exists() && (fileArrListFiles = dir.listFiles()) != null) {
            for (File file : fileArrListFiles) {
                if (file.isDirectory()) {
                    Intrinsics.checkNotNull(file);
                    deleteDir(file);
                } else {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    private final NanoHTTPD.Response handleChooseDir(NanoHTTPD.IHTTPSession session) {
        String str = "prompt";
        this.dirPickerFuture.set(new CompletableFuture<>());
        try {
            String strOptString = readJsonBody(session).optString("filename", "prompt");
            Intrinsics.checkNotNullExpressionValue(strOptString, "optString(...)");
            String string = StringsKt.trim((CharSequence) strOptString).toString();
            if (string.length() == 0) {
                string = "prompt";
            }
            str = string;
        } catch (Exception unused) {
        }
        triggerDirPicker(str + ".bundle");
        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
    }

    private final NanoHTTPD.Response handlePollChooseDir(NanoHTTPD.IHTTPSession session) {
        CompletableFuture<String> completableFuture = this.dirPickerFuture.get();
        if (completableFuture == null) {
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
        }
        if (completableFuture.isDone()) {
            this.dirPickerFuture.set(null);
            String now = completableFuture.getNow("");
            Intrinsics.checkNotNull(now);
            if (StringsKt.isBlank(now)) {
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"cancelled\":true}");
            }
            this.selectedPackDirUri.set(now);
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"done\",\"path\":\"" + escapeJson(now) + "\"}");
        }
        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
    }

    private final NanoHTTPD.Response handleSelectLoadFile(NanoHTTPD.IHTTPSession session) {
        int iIncrementAndGet = this.filePickerSeq.incrementAndGet();
        this.filePickerFuture.set(new CompletableFuture<>());
        Log.i("PromptsServer", "select_load_file_launch seq=" + iIncrementAndGet);
        triggerFilePicker();
        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
    }

    private final NanoHTTPD.Response handlePollSelectLoadFile(NanoHTTPD.IHTTPSession session) {
        byte[] bArr;
        CompletableFuture<FilePickerResult> completableFuture = this.filePickerFuture.get();
        if (completableFuture == null) {
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
        }
        if (completableFuture.isDone()) {
            this.filePickerFuture.set(null);
            FilePickerResult now = completableFuture.getNow(new FilePickerResult(false, null, null, null, false, false, 63, null));
            if (!now.getSuccess() && now.getError() == null) {
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"cancelled\":true}");
            }
            if (now.getError() != null) {
                String error = now.getError();
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"error\":\"" + escapeJson(error != null ? error : "") + "\"}");
            }
            String path = now.getPath();
            if (path == null) {
                path = "";
            }
            this.selectedLoadFilePath.set(path);
            try {
                File file = new File(path);
                if (file.length() < 4) {
                    bArr = new byte[0];
                } else {
                    byte[] bArr2 = new byte[4];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    try {
                        FileInputStream fileInputStream2 = fileInputStream;
                        int i = 0;
                        while (i < 4) {
                            int i2 = fileInputStream2.read(bArr2, i, 4 - i);
                            if (i2 == -1) {
                                break;
                            }
                            i += i2;
                        }
                        Unit unit = Unit.INSTANCE;
                        CloseableKt.closeFinally(fileInputStream, null);
                        bArr = bArr2;
                    } catch (Throwable th) {
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            CloseableKt.closeFinally(fileInputStream, th);
                            throw th2;
                        }
                    }
                }
                if (bArr.length >= 4 && (bArr[0] != 80 || bArr[1] != 75)) {
                    return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"needPassword\":true,\"path\":\"" + escapeJson(path) + "\"}");
                }
                String strEscapeJson = escapeJson(path);
                String name = new File(path).getName();
                Intrinsics.checkNotNullExpressionValue(name, "getName(...)");
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":true,\"path\":\"" + strEscapeJson + "\",\"filename\":\"" + escapeJson(name) + "\"}");
            } catch (Exception e) {
                String message = e.getMessage();
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"error\":\"" + escapeJson(message != null ? message : "") + "\"}");
            }
        }
        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"status\":\"pending\"}");
    }

    /* JADX WARN: Code duplicated, block: B:117:0x01ee A[Catch: Exception -> 0x0232, TryCatch #8 {Exception -> 0x0232, blocks: (B:133:0x022e, B:134:0x0231, B:115:0x01e8, B:117:0x01ee, B:118:0x01f1, B:121:0x01f9, B:123:0x0217, B:125:0x021d, B:126:0x0220, B:130:0x022b), top: B:160:0x004b, inners: #6 }] */
    /* JADX WARN: Code duplicated, block: B:120:0x01f7  */
    /* JADX WARN: Code duplicated, block: B:125:0x021d A[Catch: Exception -> 0x0232, TryCatch #8 {Exception -> 0x0232, blocks: (B:133:0x022e, B:134:0x0231, B:115:0x01e8, B:117:0x01ee, B:118:0x01f1, B:121:0x01f9, B:123:0x0217, B:125:0x021d, B:126:0x0220, B:130:0x022b), top: B:160:0x004b, inners: #6 }] */
    private final NanoHTTPD.Response handleUnlockLoadFile(NanoHTTPD.IHTTPSession session) {
        Throwable th;
        String message;
        String str = "error";
        try {
            String strOptString = readJsonBody(session).optString("password", "");
            String str2 = this.selectedLoadFilePath.get();
            Intrinsics.checkNotNull(str2);
            if (str2.length() == 0) {
                return jsonResp(400, "{\"error\":\"no_file_selected\"}");
            }
            File file = new File(str2);
            long length = file.length();
            if (length < 16) {
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"error\":\"file_corrupted\"}");
            }
            try {
                byte[] bArr = new byte[4];
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    FileInputStream fileInputStream2 = fileInputStream;
                    int i = 0;
                    for (int i2 = 4; i < i2; i2 = 4) {
                        try {
                            int i3 = fileInputStream2.read(bArr, i, 4 - i);
                            if (i3 == -1) {
                                break;
                            }
                            i += i3;
                        } catch (Throwable th2) {
                            th = th2;
                            try {
                                throw th;
                            } catch (Throwable th3) {
                                CloseableKt.closeFinally(fileInputStream, th);
                                throw th3;
                            }
                        }
                    }
                    Unit unit = Unit.INSTANCE;
                    CloseableKt.closeFinally(fileInputStream, null);
                    if (length >= 4 && bArr[0] == 80 && bArr[1] == 75) {
                        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":true}");
                    }
                    File file2 = this.dataDir;
                    String string = UUID.randomUUID().toString();
                    Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
                    String strSubstring = string.substring(0, 8);
                    Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
                    File file3 = new File(file2, "temp_decoded_" + strSubstring);
                    try {
                        try {
                            FileInputStream fileInputStream3 = new FileInputStream(file);
                            try {
                                FileInputStream fileInputStream4 = fileInputStream3;
                                byte[] bArr2 = new byte[16];
                                int i4 = 0;
                                while (i4 < 16) {
                                    int i5 = fileInputStream4.read(bArr2, i4, 16 - i4);
                                    if (i5 == -1) {
                                        break;
                                    }
                                    i4 += i5;
                                }
                                if (i4 < 16) {
                                    file3.delete();
                                    NanoHTTPD.Response responseJsonResp = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"error\":\"file_corrupted\"}");
                                    CloseableKt.closeFinally(fileInputStream3, null);
                                    return responseJsonResp;
                                }
                                try {
                                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                                    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
                                    Intrinsics.checkNotNull(strOptString);
                                    byte[] bytes = strOptString.getBytes(Charsets.UTF_8);
                                    Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
                                    cipher.init(2, new SecretKeySpec(messageDigest.digest(bytes), "AES"), new IvParameterSpec(bArr2));
                                    FileOutputStream fileOutputStream = new FileOutputStream(file3);
                                    try {
                                        FileOutputStream fileOutputStream2 = fileOutputStream;
                                        int i6 = 8192;
                                        byte[] bArr3 = new byte[8192];
                                        boolean z = true;
                                        while (z) {
                                            str = str;
                                            try {
                                                int i7 = fileInputStream4.read(bArr3, 0, i6);
                                                if (i7 > 0) {
                                                    byte[] bArrUpdate = cipher.update(bArr3, 0, i7);
                                                    if (bArrUpdate != null && bArrUpdate.length > 0) {
                                                        fileOutputStream2.write(bArrUpdate, 0, bArrUpdate.length);
                                                    }
                                                    i6 = 8192;
                                                } else {
                                                    z = false;
                                                }
                                            } catch (Throwable th4) {
                                                th = th4;
                                                Throwable th5 = th;
                                                try {
                                                    throw th5;
                                                } catch (Throwable th6) {
                                                    CloseableKt.closeFinally(fileOutputStream, th5);
                                                    throw th6;
                                                }
                                            }
                                        }
                                        byte[] bArrDoFinal = cipher.doFinal();
                                        if (bArrDoFinal.length > 0) {
                                            fileOutputStream2.write(bArrDoFinal, 0, bArrDoFinal.length);
                                        }
                                        Unit unit2 = Unit.INSTANCE;
                                        CloseableKt.closeFinally(fileOutputStream, null);
                                        Unit unit3 = Unit.INSTANCE;
                                        CloseableKt.closeFinally(fileInputStream3, null);
                                        FileInputStream fileInputStream5 = new FileInputStream(file3);
                                        try {
                                            byte[] bArr4 = new byte[2];
                                            if (fileInputStream5.read(bArr4, 0, 2) >= 2 && bArr4[0] == 80 && bArr4[1] == 75) {
                                                Unit unit4 = Unit.INSTANCE;
                                                CloseableKt.closeFinally(fileInputStream5, null);
                                                this.selectedLoadFilePath.set(file3.getAbsolutePath());
                                                this.selectedLoadPathDecrypted.set(file3.getAbsolutePath());
                                                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":true}");
                                            }
                                            file3.delete();
                                            NanoHTTPD.Response responseJsonResp2 = jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"needPassword\":true,\"error\":\"wrong_password\"}");
                                            CloseableKt.closeFinally(fileInputStream5, null);
                                            return responseJsonResp2;
                                        } catch (Throwable th7) {
                                            try {
                                                throw th7;
                                            } catch (Throwable th8) {
                                                CloseableKt.closeFinally(fileInputStream5, th7);
                                                throw th8;
                                            }
                                        }
                                    } catch (Throwable th9) {
                                        th = th9;
                                    }
                                } catch (Throwable th10) {
                                    th = th10;
                                    Throwable th11 = th;
                                    try {
                                        throw th11;
                                    } catch (Throwable th12) {
                                        CloseableKt.closeFinally(fileInputStream3, th11);
                                        throw th12;
                                    }
                                }
                            } catch (Throwable th13) {
                                th = th13;
                            }
                        } catch (BadPaddingException unused) {
                            if (file3.exists()) {
                                file3.delete();
                            }
                            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"needPassword\":true,\"error\":\"wrong_password\"}");
                        } catch (Exception e) {
                            e = e;
                            if (file3.exists()) {
                                file3.delete();
                            }
                            message = e.getMessage();
                            if (message == null) {
                                message = "error";
                            }
                            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"error\":\"" + message + "\"}");
                        }
                    } catch (BadPaddingException unused2) {
                        if (file3.exists()) {
                            file3.delete();
                        }
                        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"needPassword\":true,\"error\":\"wrong_password\"}");
                    } catch (Exception e2) {
                        e = e2;
                        if (file3.exists()) {
                            file3.delete();
                        }
                        message = e.getMessage();
                        if (message == null) {
                            message = "error";
                        }
                        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"error\":\"" + message + "\"}");
                    }
                } catch (Throwable th14) {
                    th = th14;
                }
            } catch (Exception e3) {
                e = e3;
                String message2 = e.getMessage();
                return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"valid\":false,\"error\":\"" + (message2 == null ? str : message2) + "\"}");
            }
        } catch (Exception e4) {
            e = e4;
            str = "error";
        }
    }

    private final NanoHTTPD.Response handleApplyLoad(NanoHTTPD.IHTTPSession session) {
        try {
            final String strOptString = readJsonBody(session).optString("mode", "merge");
            Log.i("PromptsServer", "handleApplyLoad_enter mode=" + strOptString);
            final String str = this.selectedLoadPathDecrypted.get();
            Intrinsics.checkNotNull(str);
            final String str2 = (str.length() <= 0 || !new File(str).exists()) ? this.selectedLoadFilePath.get() : str;
            Intrinsics.checkNotNull(str2);
            if (str2.length() > 0) {
                this.selectedLoadFilePath.set("");
                this.selectedLoadPathDecrypted.set("");
            }
            if (str2.length() == 0) {
                return jsonResp(400, "{\"error\":\"no_file_selected\"}");
            }
            final String string = UUID.randomUUID().toString();
            Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
            ConcurrentHashMap<String, JSONObject> concurrentHashMap = this.loadResults;
            JSONObject jSONObjectPut = new JSONObject().put(NotificationCompat.CATEGORY_STATUS, "pending").put("text", "正在加载提示词包...");
            Intrinsics.checkNotNullExpressionValue(jSONObjectPut, "put(...)");
            concurrentHashMap.put(string, jSONObjectPut);
            Log.i("PromptsServer", "handleApplyLoad task=" + string + " path=" + str2 + " mode=" + strOptString);
            new Thread(new Runnable() { // from class: com.novelai.prompttool.PromptsServer$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() throws JSONException {
                    PromptsServer.handleApplyLoad$lambda$30(string, str2, this, strOptString, str);
                }
            }).start();
            Log.i("PromptsServer", "handleApplyLoad_exit task=" + string + " response_sent");
            return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"task_id\":\"" + string + "\"}");
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "error";
            }
            Log.e("PromptsServer", "handleApplyLoad_error", e);
            return jsonResp(500, "{\"error\":\"" + message + "\"}");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void handleApplyLoad$lambda$30(final String taskId, String str, final PromptsServer this$0, String str2, String str3) throws JSONException {
        Intrinsics.checkNotNullParameter(taskId, "$taskId");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        try {
            Log.i("PromptsServer", "load_thread_start task=" + taskId + " path=" + str);
            PromptManager promptManager = this$0.promptManager;
            Intrinsics.checkNotNull(str);
            Intrinsics.checkNotNull(str2);
            JSONObject jSONObjectLoadBundle = promptManager.loadBundle(str, str2, new PromptManager.ProgressCallback() { // from class: com.novelai.prompttool.PromptsServer$handleApplyLoad$1$result$1
                @Override // com.novelai.prompttool.PromptManager.ProgressCallback
                public void onProgress(String text) throws JSONException {
                    Intrinsics.checkNotNullParameter(text, "text");
                    ConcurrentHashMap concurrentHashMap = this.this$0.loadResults;
                    String str4 = taskId;
                    JSONObject jSONObjectPut = new JSONObject().put(NotificationCompat.CATEGORY_STATUS, "pending").put("text", text);
                    Intrinsics.checkNotNullExpressionValue(jSONObjectPut, "put(...)");
                    concurrentHashMap.put(str4, jSONObjectPut);
                }
            });
            Intrinsics.checkNotNull(str3);
            if (str3.length() > 0) {
                File file = new File(str3);
                if (file.exists()) {
                    file.delete();
                }
            }
            ConcurrentHashMap<String, JSONObject> concurrentHashMap = this$0.loadResults;
            JSONObject jSONObjectPut = new JSONObject().put("done", true).put("success", true).put("message", jSONObjectLoadBundle.optString("message", "加载成功")).put("mode", jSONObjectLoadBundle.optString("mode", str2));
            Intrinsics.checkNotNullExpressionValue(jSONObjectPut, "put(...)");
            concurrentHashMap.put(taskId, jSONObjectPut);
            Log.i("PromptsServer", "load_thread_done task=" + taskId + " success=true");
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "load_failed";
            }
            ConcurrentHashMap<String, JSONObject> concurrentHashMap2 = this$0.loadResults;
            JSONObject jSONObjectPut2 = new JSONObject().put("done", true).put("success", false).put("error", message);
            Intrinsics.checkNotNullExpressionValue(jSONObjectPut2, "put(...)");
            concurrentHashMap2.put(taskId, jSONObjectPut2);
            Log.e("PromptsServer", "load_bundle_failed task=" + taskId, e);
        }
    }

    /* JADX WARN: Code duplicated, block: B:36:0x010b  */
    private final NanoHTTPD.Response handleSaveImage(NanoHTTPD.IHTTPSession session) {
        String strDecode;
        File file;
        try {
            String strOptString = readJsonBody(session).optString("filename");
            Intrinsics.checkNotNull(strOptString);
            if (strOptString.length() == 0) {
                return jsonResp(400, "{\"error\":\"missing_filename\"}");
            }
            try {
                strDecode = URLDecoder.decode(strOptString, Charsets.UTF_8.name());
            } catch (Exception unused) {
                strDecode = strOptString;
            }
            Iterator it = CollectionsKt.listOf((Object[]) new File[]{new File(this.dataDir, "picture/" + strDecode), new File(this.dataDir, "picture/" + strOptString), new File(this.appContext.getFilesDir(), "picture/" + strDecode), new File(this.appContext.getFilesDir(), "picture/" + strOptString)}).iterator();
            do {
                if (!it.hasNext()) {
                    file = null;
                    break;
                }
                file = (File) it.next();
            } while (!file.exists());
            if (file == null) {
                return jsonResp(404, "{\"error\":\"image_not_found\"}");
            }
            String name = file.getName();
            Intrinsics.checkNotNullExpressionValue(name, "getName(...)");
            String lowerCase = StringsKt.substringAfterLast(name, '.', "").toLowerCase(Locale.ROOT);
            Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
            String str = "image/png";
            switch (lowerCase.hashCode()) {
                case 102340:
                    if (lowerCase.equals("gif")) {
                        str = "image/gif";
                    }
                    break;
                case 105441:
                    if (lowerCase.equals("jpg")) {
                        str = "image/jpeg";
                    }
                    break;
                case 111145:
                    lowerCase.equals("png");
                    break;
                case 3268712:
                    if (lowerCase.equals("jpeg")) {
                        str = "image/jpeg";
                    }
                    break;
                case 3645340:
                    if (lowerCase.equals("webp")) {
                        str = "image/webp";
                    }
                    break;
            }
            ContentValues contentValues = new ContentValues();
            contentValues.put("_display_name", strDecode);
            contentValues.put("mime_type", str);
            contentValues.put("relative_path", "Pictures/prompt_image");
            contentValues.put("is_pending", (Integer) 1);
            Uri uriInsert = this.activity.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uriInsert == null) {
                return jsonResp(500, "{\"error\":\"cannot_create_image\"}");
            }
            try {
                OutputStream outputStreamOpenOutputStream = this.activity.getContentResolver().openOutputStream(uriInsert);
                if (outputStreamOpenOutputStream == null) {
                    this.activity.getContentResolver().delete(uriInsert, null, null);
                    NanoHTTPD.Response responseJsonResp = jsonResp(500, "{\"error\":\"cannot_write_image\"}");
                    ContentValues contentValues2 = new ContentValues();
                    contentValues2.put("is_pending", (Integer) 0);
                    this.activity.getContentResolver().update(uriInsert, contentValues2, null, null);
                    return responseJsonResp;
                }
                FileInputStream fileInputStream = new FileInputStream(file);
                try {
                    OutputStream outputStream = outputStreamOpenOutputStream;
                    try {
                        ByteStreamsKt.copyTo$default(fileInputStream, outputStream, 0, 2, null);
                        CloseableKt.closeFinally(outputStream, null);
                        CloseableKt.closeFinally(fileInputStream, null);
                        ContentValues contentValues3 = new ContentValues();
                        contentValues3.put("is_pending", (Integer) 0);
                        this.activity.getContentResolver().update(uriInsert, contentValues3, null, null);
                        return jsonResp(ItemTouchHelper.Callback.DEFAULT_DRAG_ANIMATION_DURATION, "{\"message\":\"image_saved\"}");
                    } catch (Throwable th) {
                        try {
                            throw th;
                        } catch (Throwable th2) {
                            CloseableKt.closeFinally(outputStream, th);
                            throw th2;
                        }
                    }
                } catch (Throwable th3) {
                    try {
                        throw th3;
                    } catch (Throwable th4) {
                        CloseableKt.closeFinally(fileInputStream, th3);
                        throw th4;
                    }
                }
            } catch (Throwable th5) {
                ContentValues contentValues4 = new ContentValues();
                contentValues4.put("is_pending", (Integer) 0);
                this.activity.getContentResolver().update(uriInsert, contentValues4, null, null);
                throw th5;
            }
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                message = "error";
            }
            return jsonResp(500, "{\"error\":\"" + message + "\"}");
        }
    }

    /* JADX WARN: Code duplicated, block: B:38:0x008e  */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    private final NanoHTTPD.Response serveAsset(String uri) {
        String strSubstring;
        String str;
        if (Intrinsics.areEqual(uri, "/") || uri.length() == 0) {
            strSubstring = "index.html";
        } else {
            strSubstring = uri.substring(1);
            Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
        }
        try {
            String lowerCase = StringsKt.substringAfterLast(strSubstring, '.', "").toLowerCase(Locale.ROOT);
            Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
            switch (lowerCase.hashCode()) {
                case 3401:
                    str = !lowerCase.equals("js") ? "application/octet-stream" : "application/javascript; charset=utf-8";
                    break;
                case 98819:
                    if (lowerCase.equals("css")) {
                        str = "text/css; charset=utf-8";
                    }
                    break;
                case 102340:
                    if (lowerCase.equals("gif")) {
                        str = "image/gif";
                    }
                    break;
                case 104085:
                    if (lowerCase.equals("ico")) {
                        str = "image/x-icon";
                    }
                    break;
                case 105441:
                    if (lowerCase.equals("jpg")) {
                        str = "image/jpeg";
                    }
                    break;
                case 111145:
                    if (lowerCase.equals("png")) {
                        str = "image/png";
                    }
                    break;
                case 114276:
                    if (lowerCase.equals("svg")) {
                        str = "image/svg+xml";
                    }
                    break;
                case 3213227:
                    if (lowerCase.equals("html")) {
                        str = "text/html; charset=utf-8";
                    }
                    break;
                case 3268712:
                    if (lowerCase.equals("jpeg")) {
                        str = "image/jpeg";
                    }
                    break;
                case 3271912:
                    if (lowerCase.equals("json")) {
                        str = "application/json; charset=utf-8";
                    }
                    break;
                case 3645340:
                    if (lowerCase.equals("webp")) {
                        str = "image/webp";
                    }
                    break;
                default:
                    break;
            }
            InputStream inputStreamOpen = this.appContext.getAssets().open(strSubstring);
            Intrinsics.checkNotNullExpressionValue(inputStreamOpen, "open(...)");
            byte[] bytes = ByteStreamsKt.readBytes(inputStreamOpen);
            NanoHTTPD.Response responseNewFixedLengthResponse = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, str, new ByteArrayInputStream(bytes), bytes.length);
            Intrinsics.checkNotNull(responseNewFixedLengthResponse);
            return responseNewFixedLengthResponse;
        } catch (Exception unused) {
            NanoHTTPD.Response responseNewFixedLengthResponse2 = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, NanoHTTPD.MIME_PLAINTEXT, "Not found: " + strSubstring);
            Intrinsics.checkNotNull(responseNewFixedLengthResponse2);
            return responseNewFixedLengthResponse2;
        }
    }

    private final NanoHTTPD.Response jsonResp(int status, String body) {
        NanoHTTPD.Response.Status status2;
        if (200 <= status && status < 300) {
            status2 = NanoHTTPD.Response.Status.OK;
        } else if (status == 400) {
            status2 = NanoHTTPD.Response.Status.BAD_REQUEST;
        } else if (status == 404) {
            status2 = NanoHTTPD.Response.Status.NOT_FOUND;
        } else {
            status2 = NanoHTTPD.Response.Status.INTERNAL_ERROR;
        }
        NanoHTTPD.Response responseNewFixedLengthResponse = NanoHTTPD.newFixedLengthResponse(status2, "application/json; charset=utf-8", body);
        Intrinsics.checkNotNullExpressionValue(responseNewFixedLengthResponse, "newFixedLengthResponse(...)");
        return responseNewFixedLengthResponse;
    }

    private final JSONObject readJsonBody(NanoHTTPD.IHTTPSession session) {
        Integer intOrNull;
        String str = session.getHeaders().get("content-length");
        int iIntValue = (str == null || (intOrNull = StringsKt.toIntOrNull(str)) == null) ? 0 : intOrNull.intValue();
        if (iIntValue <= 0) {
            return new JSONObject();
        }
        InputStream inputStream = session.getInputStream();
        Intrinsics.checkNotNullExpressionValue(inputStream, "getInputStream(...)");
        try {
            return new JSONObject(new String(readBytesFully(inputStream, iIntValue), Charsets.UTF_8));
        } catch (Exception unused) {
            return new JSONObject();
        }
    }

    private final String guessExtension(byte[] data) {
        if (data.length >= 4) {
            int i = data[0] & UByte.MAX_VALUE;
            int i2 = data[1] & UByte.MAX_VALUE;
            int i3 = data[2] & UByte.MAX_VALUE;
            int i4 = data[3] & UByte.MAX_VALUE;
            if (i == 137 && i2 == 80 && i3 == 78 && i4 == 71) {
                return "png";
            }
            if (i == 255 && i2 == 216 && i3 == 255) {
                return "jpg";
            }
            if (i == 71 && i2 == 73 && i3 == 70 && i4 == 56) {
                return "gif";
            }
            if (i == 82 && i2 == 73 && i3 == 70 && i4 == 70 && data.length >= 12) {
                int i5 = data[8] & UByte.MAX_VALUE;
                int i6 = data[9] & UByte.MAX_VALUE;
                int i7 = data[10] & UByte.MAX_VALUE;
                int i8 = data[11] & UByte.MAX_VALUE;
                if (i5 == 87 && i6 == 69 && i7 == 66 && i8 == 80) {
                    return "webp";
                }
                return "dat";
            }
            return "dat";
        }
        return "dat";
    }

    private final byte[] readBytesFully(InputStream inputStream, int expectedSize) throws IOException {
        if (expectedSize <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[expectedSize];
        int i = 0;
        while (i < expectedSize) {
            int i2 = inputStream.read(bArr, i, expectedSize - i);
            if (i2 <= 0) {
                break;
            }
            i += i2;
        }
        return i == expectedSize ? bArr : ArraysKt.copyOfRange(bArr, 0, i);
    }

    private final Map<String, String> parseQuery(String uri) {
        LinkedHashMap linkedHashMap = new LinkedHashMap();
        int iIndexOf$default = StringsKt.indexOf$default((CharSequence) uri, '?', 0, false, 6, (Object) null);
        if (iIndexOf$default >= 0) {
            String strSubstring = uri.substring(iIndexOf$default + 1);
            Intrinsics.checkNotNullExpressionValue(strSubstring, "substring(...)");
            Iterator it = StringsKt.split$default((CharSequence) strSubstring, new char[]{Typography.amp}, false, 0, 6, (Object) null).iterator();
            while (it.hasNext()) {
                List listSplit$default = StringsKt.split$default((CharSequence) it.next(), new char[]{'='}, false, 2, 2, (Object) null);
                if (listSplit$default.size() == 2) {
                    linkedHashMap.put(listSplit$default.get(0), listSplit$default.get(1));
                }
            }
        }
        return linkedHashMap;
    }

    private final String sha1Hex(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(data);
        byte[] bArrDigest = messageDigest.digest();
        Intrinsics.checkNotNullExpressionValue(bArrDigest, "digest(...)");
        return ArraysKt.joinToString$default(bArrDigest, (CharSequence) "", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, (Function1) new Function1<Byte, CharSequence>() { // from class: com.novelai.prompttool.PromptsServer.sha1Hex.1
            public final CharSequence invoke(byte b) {
                String str = String.format("%02x", Arrays.copyOf(new Object[]{Byte.valueOf(b)}, 1));
                Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                return str;
            }

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ CharSequence invoke(Byte b) {
                return invoke(b.byteValue());
            }
        }, 30, (Object) null);
    }

    private final String sha1HexStream(InputStream inputStream) throws NoSuchAlgorithmException, IOException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        byte[] bArr = new byte[8192];
        int i = inputStream.read(bArr);
        while (i > 0) {
            messageDigest.update(bArr, 0, i);
            i = inputStream.read(bArr);
        }
        byte[] bArrDigest = messageDigest.digest();
        Intrinsics.checkNotNullExpressionValue(bArrDigest, "digest(...)");
        return ArraysKt.joinToString$default(bArrDigest, (CharSequence) "", (CharSequence) null, (CharSequence) null, 0, (CharSequence) null, (Function1) new Function1<Byte, CharSequence>() { // from class: com.novelai.prompttool.PromptsServer.sha1HexStream.1
            public final CharSequence invoke(byte b) {
                String str = String.format("%02x", Arrays.copyOf(new Object[]{Byte.valueOf(b)}, 1));
                Intrinsics.checkNotNullExpressionValue(str, "format(...)");
                return str;
            }

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ CharSequence invoke(Byte b) {
                return invoke(b.byteValue());
            }
        }, 30, (Object) null);
    }

    private final String guessExtensionStream(InputStream inputStream) throws IOException {
        byte[] bArr = new byte[12];
        int i = 0;
        while (i < 12) {
            int i2 = inputStream.read(bArr, i, 12 - i);
            if (i2 == -1) {
                break;
            }
            i += i2;
        }
        if (i >= 4) {
            int i3 = bArr[0] & UByte.MAX_VALUE;
            int i4 = bArr[1] & UByte.MAX_VALUE;
            int i5 = bArr[2] & UByte.MAX_VALUE;
            int i6 = bArr[3] & UByte.MAX_VALUE;
            if (i3 == 137 && i4 == 80 && i5 == 78 && i6 == 71) {
                return "png";
            }
            if (i3 == 255 && i4 == 216 && i5 == 255) {
                return "jpg";
            }
            if (i3 == 71 && i4 == 73 && i5 == 70 && i6 == 56) {
                return "gif";
            }
            if (i3 == 82 && i4 == 73 && i5 == 70 && i6 == 70 && i >= 12) {
                int i7 = bArr[8] & UByte.MAX_VALUE;
                int i8 = bArr[9] & UByte.MAX_VALUE;
                int i9 = bArr[10] & UByte.MAX_VALUE;
                int i10 = bArr[11] & UByte.MAX_VALUE;
                if (i7 == 87 && i8 == 69 && i9 == 66 && i10 == 80) {
                    return "webp";
                }
                return "dat";
            }
            return "dat";
        }
        return "dat";
    }

    private final void copyFileStream(File src, File dest) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(src);
        try {
            FileInputStream fileInputStream2 = fileInputStream;
            FileOutputStream fileOutputStream = new FileOutputStream(dest);
            try {
                ByteStreamsKt.copyTo$default(fileInputStream2, fileOutputStream, 0, 2, null);
                CloseableKt.closeFinally(fileOutputStream, null);
                CloseableKt.closeFinally(fileInputStream, null);
            } catch (Throwable th) {
                try {
                    throw th;
                } catch (Throwable th2) {
                    CloseableKt.closeFinally(fileOutputStream, th);
                    throw th2;
                }
            }
        } catch (Throwable th3) {
            try {
                throw th3;
            } catch (Throwable th4) {
                CloseableKt.closeFinally(fileInputStream, th3);
                throw th4;
            }
        }
    }

    private final String fastJsonArr(ArrayList<String> list) {
        StringBuilder sb = new StringBuilder("[");
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (i > 0) {
                sb.append(",");
            }
            StringBuilder sbAppend = sb.append("\"");
            String str = list.get(i);
            Intrinsics.checkNotNullExpressionValue(str, "get(...)");
            sbAppend.append(escapeJson(str)).append("\"");
        }
        sb.append("]");
        String string = sb.toString();
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        return string;
    }

    private final String escapeJson(String s) {
        return StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(s, "\\", "\\\\", false, 4, (Object) null), "\"", "\\\"", false, 4, (Object) null), "\n", "\\n", false, 4, (Object) null), "\r", "\\r", false, 4, (Object) null), "\t", "\\t", false, 4, (Object) null);
    }
}
