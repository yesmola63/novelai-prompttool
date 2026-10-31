package com.novelai.prompttool;

import android.content.Context;
import android.util.Log;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.ByteStreamsKt;
import kotlin.io.CloseableKt;
import kotlin.io.FilesKt;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.Charsets;
import kotlin.text.StringsKt;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX INFO: compiled from: PromptManager.kt */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000\\\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0006\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\b\r\n\u0002\u0010\b\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0004\n\u0002\u0018\u0002\n\u0002\b\t\n\u0002\u0010 \n\u0002\b\f\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0007\u0018\u00002\u00020\u0001:\u0001IB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003¢\u0006\u0002\u0010\u0004J\u0016\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0010\u001a\u00020\u0011J\u0018\u0010\u0012\u001a\u00020\r2\u0006\u0010\u0013\u001a\u00020\u00062\u0006\u0010\u0014\u001a\u00020\u0006H\u0002J\u001e\u0010\u0015\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0017\u001a\u00020\u000fJ\u000e\u0010\u0018\u001a\u00020\r2\u0006\u0010\u0019\u001a\u00020\u000fJ\u0010\u0010\u001a\u001a\u00020\r2\u0006\u0010\u001b\u001a\u00020\u0006H\u0002J\u0010\u0010\u001c\u001a\u00020\u000f2\u0006\u0010\u001d\u001a\u00020\u000fH\u0002J \u0010\u001e\u001a\u00020\u001f2\u0006\u0010 \u001a\u00020!2\u0006\u0010\u0016\u001a\u00020\u000f2\u0006\u0010\u0017\u001a\u00020\u000fH\u0002J$\u0010\"\u001a\u00020\u00112\u0006\u0010#\u001a\u00020\u000f2\b\b\u0002\u0010$\u001a\u00020\u000f2\n\b\u0002\u0010%\u001a\u0004\u0018\u00010&J\u0006\u0010'\u001a\u00020\u000fJ\b\u0010(\u001a\u00020\u0011H\u0002J \u0010)\u001a\u00020\r2\u0006\u0010*\u001a\u00020\u00112\u0006\u0010+\u001a\u00020\u00112\u0006\u0010,\u001a\u00020\u0006H\u0002J(\u0010-\u001a\u00020\r2\u0006\u0010.\u001a\u00020\u000f2\f\u0010/\u001a\b\u0012\u0002\b\u0003\u0018\u0001002\n\b\u0002\u00101\u001a\u0004\u0018\u00010\u000fJ\u000e\u00102\u001a\u00020\r2\u0006\u00103\u001a\u00020\u000fJ\u000e\u00102\u001a\u00020\r2\u0006\u00104\u001a\u00020\u0011J.\u00105\u001a\u00020\r2\u0006\u00106\u001a\u00020\u000f2\u0006\u00107\u001a\u00020\u000f2\u0006\u00108\u001a\u00020\u000f2\u0006\u00109\u001a\u00020\u000f2\u0006\u0010:\u001a\u00020\u0011J \u0010;\u001a\u00020\r2\u0006\u0010<\u001a\u00020=2\u0006\u0010>\u001a\u00020\u00062\u0006\u0010?\u001a\u00020\u000fH\u0002J\u0018\u0010@\u001a\u00020\r2\u0006\u0010A\u001a\u00020!2\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010D\u001a\u00020\r2\u0006\u0010E\u001a\u00020\u00112\u0006\u0010B\u001a\u00020CH\u0002J\u0018\u0010F\u001a\u00020\r2\u0006\u00104\u001a\u00020\u00112\u0006\u0010\u001b\u001a\u00020\u0006H\u0002J\u001a\u0010G\u001a\u00020\r2\b\u0010H\u001a\u0004\u0018\u00010\u00012\u0006\u0010B\u001a\u00020CH\u0002R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\t\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\n\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\u0006X\u0082\u0004¢\u0006\u0002\n\u0000¨\u0006J"}, d2 = {"Lcom/novelai/prompttool/PromptManager;", "", "context", "Landroid/content/Context;", "(Landroid/content/Context;)V", "backupFile", "Ljava/io/File;", "filesDir", "packDir", "picDir", "promptFile", "tempDir", "addPrompt", "", "group", "", "item", "Lorg/json/JSONObject;", "copyFile", "src", "dest", "deletePrompt", "role", "common", "deletePromptById", "id", "deleteRecursively", "file", "escapeJson", "s", "findItemIndex", "", "items", "Lorg/json/JSONArray;", "loadBundle", "zipPath", "mode", "callback", "Lcom/novelai/prompttool/PromptManager$ProgressCallback;", "loadPrompts", "loadPromptsMap", "mergePromptsToFile", "oldData", "newData", "outputFile", "packPrompts", "filename", "selectedIds", "", "customOutputPath", "savePrompts", "jsonText", "jsonObj", "updatePrompt", "originalGroup", "originalRole", "originalCommon", "newGroup", "newItem", "walkAndZip", "zos", "Ljava/util/zip/ZipOutputStream;", "dir", "base", "writeArray", "arr", "writer", "Ljava/io/Writer;", "writeObject", "obj", "writeObjectToFile", "writeValue", "value", "ProgressCallback", "app_release"}, k = 1, mv = {1, 9, 0}, xi = ConstraintLayout.LayoutParams.Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE)
public final class PromptManager {
    private final File backupFile;
    private final Context context;
    private final File filesDir;
    private final File packDir;
    private final File picDir;
    private final File promptFile;
    private final File tempDir;

    /* JADX INFO: compiled from: PromptManager.kt */
    @Metadata(d1 = {"\u0000\u0016\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\bf\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H&¨\u0006\u0006"}, d2 = {"Lcom/novelai/prompttool/PromptManager$ProgressCallback;", "", "onProgress", "", "text", "", "app_release"}, k = 1, mv = {1, 9, 0}, xi = ConstraintLayout.LayoutParams.Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE)
    public interface ProgressCallback {
        void onProgress(String text);
    }

    public PromptManager(Context context) {
        Intrinsics.checkNotNullParameter(context, "context");
        this.context = context;
        File externalFilesDir = context.getExternalFilesDir(null);
        if (externalFilesDir == null) {
            externalFilesDir = context.getFilesDir();
            Intrinsics.checkNotNullExpressionValue(externalFilesDir, "getFilesDir(...)");
        }
        this.filesDir = externalFilesDir;
        this.promptFile = new File(externalFilesDir, "prompt.json");
        this.backupFile = new File(externalFilesDir, "prompt_backup.json");
        File file = new File(externalFilesDir, "temp");
        this.tempDir = file;
        File file2 = new File(externalFilesDir, "picture");
        this.picDir = file2;
        File file3 = new File(externalFilesDir, "prompt_pack");
        this.packDir = file3;
        file2.mkdirs();
        file.mkdirs();
        file3.mkdirs();
    }

    public final String loadPrompts() {
        if (!this.promptFile.exists()) {
            return "{}";
        }
        try {
            return FilesKt.readText(this.promptFile, Charsets.UTF_8);
        } catch (Exception unused) {
            return "{}";
        }
    }

    public final void savePrompts(String jsonText) {
        Intrinsics.checkNotNullParameter(jsonText, "jsonText");
        try {
            new JSONObject(jsonText);
            FilesKt.writeText(new File(this.filesDir, "prompt.json"), jsonText, Charsets.UTF_8);
            FilesKt.writeText(new File(this.filesDir, "prompt_backup.json"), jsonText, Charsets.UTF_8);
        } catch (JSONException e) {
            Log.e("PromptManager", "保存提示词失败，JSON 非法", e);
            throw new RuntimeException("JSON 数据非法");
        }
    }

    public final void savePrompts(JSONObject jsonObj) {
        Intrinsics.checkNotNullParameter(jsonObj, "jsonObj");
        try {
            File file = new File(this.filesDir, "prompt.json.tmp");
            File file2 = new File(this.filesDir, "prompt.json");
            File file3 = new File(this.filesDir, "prompt_backup.json");
            File file4 = new File(this.filesDir, "prompt_backup.json.tmp");
            writeObjectToFile(jsonObj, file);
            if (file2.exists()) {
                file2.delete();
            }
            file.renameTo(file2);
            writeObjectToFile(jsonObj, file4);
            if (file3.exists()) {
                file3.delete();
            }
            file4.renameTo(file3);
            Log.i("PromptManager", "保存提示词完成（流式），大小=" + file2.length());
        } catch (Exception e) {
            Log.e("PromptManager", "保存提示词失败（流式）", e);
            throw new RuntimeException("保存提示词失败：" + e.getMessage());
        }
    }

    public final void addPrompt(String group, JSONObject item) throws JSONException {
        Intrinsics.checkNotNullParameter(group, "group");
        Intrinsics.checkNotNullParameter(item, "item");
        JSONObject jSONObjectLoadPromptsMap = loadPromptsMap();
        JSONArray jSONArrayOptJSONArray = jSONObjectLoadPromptsMap.optJSONArray(group);
        if (jSONArrayOptJSONArray == null) {
            jSONArrayOptJSONArray = new JSONArray();
            jSONObjectLoadPromptsMap.put(group, jSONArrayOptJSONArray);
        }
        int length = jSONArrayOptJSONArray.length();
        for (int i = 0; i < length; i++) {
            JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i);
            if (Intrinsics.areEqual(jSONObject.optString("role"), item.optString("role")) && Intrinsics.areEqual(jSONObject.optString("common"), item.optString("common"))) {
                throw new RuntimeException("提示词已存在");
            }
        }
        jSONArrayOptJSONArray.put(item);
        String string = jSONObjectLoadPromptsMap.toString(2);
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        savePrompts(string);
    }

    public final void updatePrompt(String originalGroup, String originalRole, String originalCommon, String newGroup, JSONObject newItem) throws JSONException {
        Intrinsics.checkNotNullParameter(originalGroup, "originalGroup");
        Intrinsics.checkNotNullParameter(originalRole, "originalRole");
        Intrinsics.checkNotNullParameter(originalCommon, "originalCommon");
        Intrinsics.checkNotNullParameter(newGroup, "newGroup");
        Intrinsics.checkNotNullParameter(newItem, "newItem");
        JSONObject jSONObjectLoadPromptsMap = loadPromptsMap();
        JSONArray jSONArrayOptJSONArray = jSONObjectLoadPromptsMap.optJSONArray(originalGroup);
        if (jSONArrayOptJSONArray == null) {
            throw new RuntimeException("原始分组不存在");
        }
        int iFindItemIndex = findItemIndex(jSONArrayOptJSONArray, originalRole, originalCommon);
        if (iFindItemIndex < 0) {
            throw new RuntimeException("未找到原始提示词");
        }
        JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(iFindItemIndex);
        jSONArrayOptJSONArray.remove(iFindItemIndex);
        JSONArray jSONArrayOptJSONArray2 = jSONObjectLoadPromptsMap.optJSONArray(newGroup);
        if (jSONArrayOptJSONArray2 == null) {
            jSONArrayOptJSONArray2 = new JSONArray();
            jSONObjectLoadPromptsMap.put(newGroup, jSONArrayOptJSONArray2);
        }
        int length = jSONArrayOptJSONArray2.length();
        for (int i = 0; i < length; i++) {
            JSONObject jSONObject2 = jSONArrayOptJSONArray2.getJSONObject(i);
            if (Intrinsics.areEqual(jSONObject2.optString("role"), newItem.optString("role")) && Intrinsics.areEqual(jSONObject2.optString("common"), newItem.optString("common"))) {
                if (jSONArrayOptJSONArray.length() == 0) {
                    jSONObjectLoadPromptsMap.remove(originalGroup);
                }
                jSONArrayOptJSONArray.put(jSONObject);
                String string = jSONObjectLoadPromptsMap.toString(2);
                Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
                savePrompts(string);
                throw new RuntimeException("更新后与同组其他提示词冲突");
            }
        }
        jSONArrayOptJSONArray2.put(newItem);
        if (jSONArrayOptJSONArray.length() == 0) {
            jSONObjectLoadPromptsMap.remove(originalGroup);
        }
        String string2 = jSONObjectLoadPromptsMap.toString(2);
        Intrinsics.checkNotNullExpressionValue(string2, "toString(...)");
        savePrompts(string2);
    }

    public final void deletePromptById(String id) throws JSONException {
        Intrinsics.checkNotNullParameter(id, "id");
        JSONObject jSONObjectLoadPromptsMap = loadPromptsMap();
        Iterator<String> itKeys = jSONObjectLoadPromptsMap.keys();
        boolean z = false;
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            JSONArray jSONArrayOptJSONArray = jSONObjectLoadPromptsMap.optJSONArray(next);
            if (jSONArrayOptJSONArray != null) {
                int length = jSONArrayOptJSONArray.length();
                for (int i = 0; i < length; i++) {
                    if (Intrinsics.areEqual(jSONArrayOptJSONArray.getJSONObject(i).optString("id"), id)) {
                        jSONArrayOptJSONArray.remove(i);
                        z = true;
                        break;
                    }
                }
                if (z) {
                    if (jSONArrayOptJSONArray.length() != 0) {
                        break;
                    }
                    jSONObjectLoadPromptsMap.remove(next);
                    break;
                }
            }
        }
        if (!z) {
            throw new RuntimeException("未找到该提示词");
        }
        String string = jSONObjectLoadPromptsMap.toString(2);
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        savePrompts(string);
    }

    public final void deletePrompt(String group, String role, String common) throws JSONException {
        Intrinsics.checkNotNullParameter(group, "group");
        Intrinsics.checkNotNullParameter(role, "role");
        Intrinsics.checkNotNullParameter(common, "common");
        JSONObject jSONObjectLoadPromptsMap = loadPromptsMap();
        JSONArray jSONArrayOptJSONArray = jSONObjectLoadPromptsMap.optJSONArray(group);
        if (jSONArrayOptJSONArray == null) {
            throw new RuntimeException("分组不存在");
        }
        int iFindItemIndex = findItemIndex(jSONArrayOptJSONArray, role, common);
        if (iFindItemIndex < 0) {
            throw new RuntimeException("未找到该提示词");
        }
        jSONArrayOptJSONArray.remove(iFindItemIndex);
        if (jSONArrayOptJSONArray.length() == 0) {
            jSONObjectLoadPromptsMap.remove(group);
        }
        String string = jSONObjectLoadPromptsMap.toString(2);
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        savePrompts(string);
    }

    public static /* synthetic */ void packPrompts$default(PromptManager promptManager, String str, List list, String str2, int i, Object obj) throws JSONException, IOException {
        if ((i & 4) != 0) {
            str2 = null;
        }
        promptManager.packPrompts(str, list, str2);
    }

    public final void packPrompts(String filename, List<?> selectedIds, String customOutputPath) throws JSONException, IOException {
        Set set;
        File file;
        JSONObject jSONObject;
        Intrinsics.checkNotNullParameter(filename, "filename");
        JSONObject jSONObjectLoadPromptsMap = loadPromptsMap();
        if (selectedIds != null) {
            List<?> list = selectedIds;
            ArrayList arrayList = new ArrayList(CollectionsKt.collectionSizeOrDefault(list, 10));
            Iterator<T> it = list.iterator();
            while (it.hasNext()) {
                arrayList.add(String.valueOf(it.next()));
            }
            set = CollectionsKt.toSet(arrayList);
        } else {
            set = null;
        }
        JSONObject jSONObject2 = new JSONObject();
        LinkedHashSet<String> linkedHashSet = new LinkedHashSet();
        Iterator<String> itKeys = jSONObjectLoadPromptsMap.keys();
        Intrinsics.checkNotNullExpressionValue(itKeys, "keys(...)");
        while (itKeys.hasNext()) {
            String next = itKeys.next();
            JSONArray jSONArrayOptJSONArray = jSONObjectLoadPromptsMap.optJSONArray(next);
            if (jSONArrayOptJSONArray == null) {
                jSONObject = jSONObjectLoadPromptsMap;
            } else {
                Intrinsics.checkNotNull(jSONArrayOptJSONArray);
                JSONArray jSONArray = new JSONArray();
                int length = jSONArrayOptJSONArray.length();
                int i = 0;
                while (i < length) {
                    JSONObject jSONObject3 = jSONArrayOptJSONArray.getJSONObject(i);
                    String strOptString = jSONObject3.optString("id");
                    if (set == null || set.contains(strOptString)) {
                        jSONArray.put(jSONObject3);
                        JSONArray jSONArrayOptJSONArray2 = jSONObject3.optJSONArray("cover");
                        if (jSONArrayOptJSONArray2 != null) {
                            int length2 = jSONArrayOptJSONArray2.length();
                            int i2 = 0;
                            while (i2 < length2) {
                                linkedHashSet.add(jSONArrayOptJSONArray2.getString(i2));
                                i2++;
                                jSONObjectLoadPromptsMap = jSONObjectLoadPromptsMap;
                            }
                        }
                    }
                    i++;
                    jSONObjectLoadPromptsMap = jSONObjectLoadPromptsMap;
                }
                jSONObject = jSONObjectLoadPromptsMap;
                if (jSONArray.length() > 0) {
                    jSONObject2.put(next, jSONArray);
                }
            }
            jSONObjectLoadPromptsMap = jSONObject;
        }
        if (jSONObject2.length() == 0) {
            throw new RuntimeException("没有可打包的提示词");
        }
        File file2 = new File(this.tempDir, "pack_tmp");
        deleteRecursively(file2);
        file2.mkdirs();
        File file3 = new File(file2, "picture");
        file3.mkdirs();
        File file4 = new File(file2, "prompt.json");
        String string = jSONObject2.toString(2);
        Intrinsics.checkNotNullExpressionValue(string, "toString(...)");
        FilesKt.writeText(file4, string, Charsets.UTF_8);
        for (String str : linkedHashSet) {
            File file5 = new File(this.picDir, str);
            if (file5.exists()) {
                copyFile(file5, new File(file3, str));
            }
        }
        if (customOutputPath != null && (!StringsKt.isBlank(customOutputPath))) {
            File file6 = new File(customOutputPath);
            file6.mkdirs();
            file = new File(file6, filename + ".bundle");
        } else {
            file = new File(this.context.getExternalFilesDir(null), "prompt_pack/" + filename + ".bundle");
        }
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(file));
        try {
            ZipOutputStream zipOutputStream2 = zipOutputStream;
            walkAndZip(zipOutputStream2, file2, "");
            File[] fileArrListFiles = file3.listFiles();
            if (fileArrListFiles != null) {
                Intrinsics.checkNotNull(fileArrListFiles);
                if (fileArrListFiles.length == 0) {
                    zipOutputStream2.putNextEntry(new ZipEntry("picture/"));
                    zipOutputStream2.closeEntry();
                }
            }
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(zipOutputStream, null);
            deleteRecursively(file2);
            Log.i("PromptManager", "打包完成: " + file.getAbsolutePath());
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(zipOutputStream, th);
                throw th2;
            }
        }
    }

    public static /* synthetic */ JSONObject loadBundle$default(PromptManager promptManager, String str, String str2, ProgressCallback progressCallback, int i, Object obj) {
        if ((i & 2) != 0) {
            str2 = "merge";
        }
        if ((i & 4) != 0) {
            progressCallback = null;
        }
        return promptManager.loadBundle(str, str2, progressCallback);
    }

    public final JSONObject loadBundle(String zipPath, String mode, ProgressCallback callback) throws IOException {
        int i;
        File[] fileArrListFiles;
        File[] fileArrListFiles2;
        Intrinsics.checkNotNullParameter(zipPath, "zipPath");
        Intrinsics.checkNotNullParameter(mode, "mode");
        if (callback != null) {
            callback.onProgress("正在解压文件...");
        }
        File file = new File(this.tempDir, "load_tmp_" + System.currentTimeMillis());
        file.mkdirs();
        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(new File(zipPath)));
        try {
            ZipInputStream zipInputStream2 = zipInputStream;
            ZipEntry nextEntry = zipInputStream2.getNextEntry();
            while (true) {
                i = 0;
                if (nextEntry == null) {
                    break;
                }
                String name = nextEntry.getName();
                if (nextEntry.isDirectory()) {
                    new File(file, name).mkdirs();
                } else {
                    File file2 = new File(file, name);
                    File parentFile = file2.getParentFile();
                    if (parentFile != null) {
                        parentFile.mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file2);
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
                nextEntry = zipInputStream2.getNextEntry();
            }
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(zipInputStream, null);
            File file3 = new File(file, "prompt.json");
            if (!file3.exists()) {
                throw new RuntimeException("包中缺少 prompt.json");
            }
            if (callback != null) {
                callback.onProgress("正在解析提示词数据...");
            }
            JSONObject jSONObject = new JSONObject(FilesKt.readText(file3, Charsets.UTF_8));
            File file4 = new File(file, "picture");
            if (Intrinsics.areEqual(mode, "replace")) {
                if (callback != null) {
                    callback.onProgress("正在替换提示词和图片...");
                }
                File[] fileArrListFiles3 = this.picDir.listFiles();
                if (fileArrListFiles3 != null) {
                    for (File file5 : fileArrListFiles3) {
                        file5.delete();
                    }
                }
                if (file4.exists() && (fileArrListFiles2 = file4.listFiles()) != null) {
                    int length = fileArrListFiles2.length;
                    while (i < length) {
                        File file6 = fileArrListFiles2[i];
                        Intrinsics.checkNotNull(file6);
                        copyFile(file6, new File(this.picDir, file6.getName()));
                        i++;
                    }
                }
                writeObjectToFile(jSONObject, this.promptFile);
                writeObjectToFile(jSONObject, this.backupFile);
            } else {
                if (callback != null) {
                    callback.onProgress("正在合并提示词...");
                }
                if (file4.exists() && (fileArrListFiles = file4.listFiles()) != null) {
                    int length2 = fileArrListFiles.length;
                    while (i < length2) {
                        File file7 = fileArrListFiles[i];
                        File file8 = new File(this.picDir, file7.getName());
                        if (!file8.exists()) {
                            Intrinsics.checkNotNull(file7);
                            copyFile(file7, file8);
                        }
                        i++;
                    }
                }
                File file9 = new File(this.filesDir, "prompt.json.merge.tmp");
                mergePromptsToFile(loadPromptsMap(), jSONObject, file9);
                if (this.promptFile.exists()) {
                    this.promptFile.delete();
                }
                file9.renameTo(this.promptFile);
                copyFile(this.promptFile, this.backupFile);
            }
            if (callback != null) {
                callback.onProgress("正在保存数据...");
            }
            deleteRecursively(file);
            return new JSONObject("{\"message\":\"加载成功\",\"mode\":\"" + mode + "\"}");
        } catch (Throwable th3) {
            try {
                throw th3;
            } catch (Throwable th4) {
                CloseableKt.closeFinally(zipInputStream, th3);
                throw th4;
            }
        }
    }

    private final void writeObjectToFile(JSONObject jsonObj, File file) throws IOException {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), Charsets.UTF_8), 8192);
        try {
            BufferedWriter bufferedWriter2 = bufferedWriter;
            writeObject(jsonObj, bufferedWriter2);
            bufferedWriter2.flush();
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(bufferedWriter, null);
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(bufferedWriter, th);
                throw th2;
            }
        }
    }

    private final void writeObject(JSONObject obj, Writer writer) throws IOException {
        writer.write("{");
        Iterator<String> itKeys = obj.keys();
        boolean z = true;
        while (itKeys.hasNext()) {
            if (!z) {
                writer.write(",");
            }
            String next = itKeys.next();
            writer.write("\"");
            Intrinsics.checkNotNull(next);
            writer.write(escapeJson(next));
            writer.write("\":");
            writeValue(obj.opt(next), writer);
            z = false;
        }
        writer.write("}");
    }

    private final void writeArray(JSONArray arr, Writer writer) throws IOException {
        writer.write("[");
        int length = arr.length();
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                writer.write(",");
            }
            writeValue(arr.get(i), writer);
        }
        writer.write("]");
    }

    private final void writeValue(Object value, Writer writer) throws IOException {
        if (value == null || Intrinsics.areEqual(value, JSONObject.NULL)) {
            writer.write("null");
            return;
        }
        if (value instanceof JSONObject) {
            writeObject((JSONObject) value, writer);
            return;
        }
        if (value instanceof JSONArray) {
            writeArray((JSONArray) value, writer);
            return;
        }
        if (value instanceof String) {
            writer.write("\"");
            writer.write(escapeJson((String) value));
            writer.write("\"");
        } else if (value instanceof Number) {
            writer.write(((Number) value).toString());
        } else {
            if (value instanceof Boolean) {
                writer.write(String.valueOf(((Boolean) value).booleanValue()));
                return;
            }
            writer.write("\"");
            writer.write(escapeJson(value.toString()));
            writer.write("\"");
        }
    }

    private final void mergePromptsToFile(JSONObject oldData, JSONObject newData, File outputFile) throws IOException {
        JSONArray jSONArray;
        JSONArray jSONArray2;
        LinkedHashSet linkedHashSet = new LinkedHashSet();
        BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), Charsets.UTF_8), 8192);
        try {
            BufferedWriter bufferedWriter2 = bufferedWriter;
            bufferedWriter2.write("{");
            Iterator<String> itKeys = oldData.keys();
            boolean z = true;
            while (itKeys.hasNext()) {
                String next = itKeys.next();
                if (!z) {
                    bufferedWriter2.write(",");
                }
                Intrinsics.checkNotNull(next);
                linkedHashSet.add(next);
                bufferedWriter2.write("\"");
                bufferedWriter2.write(escapeJson(next));
                bufferedWriter2.write("\":[");
                JSONArray jSONArray3 = oldData.getJSONArray(next);
                JSONArray jSONArrayOptJSONArray = newData.optJSONArray(next);
                int length = jSONArray3.length();
                for (int i = 0; i < length; i++) {
                    if (i > 0) {
                        bufferedWriter2.write(",");
                    }
                    writeValue(jSONArray3.get(i), bufferedWriter2);
                }
                if (jSONArrayOptJSONArray != null) {
                    int length2 = jSONArrayOptJSONArray.length();
                    int i2 = 0;
                    while (i2 < length2) {
                        JSONObject jSONObject = jSONArrayOptJSONArray.getJSONObject(i2);
                        int length3 = jSONArray3.length();
                        Iterator<String> it = itKeys;
                        int i3 = 0;
                        while (true) {
                            if (i3 < length3) {
                                JSONObject jSONObject2 = jSONArray3.getJSONObject(i3);
                                jSONArray = jSONArrayOptJSONArray;
                                jSONArray2 = jSONArray3;
                                if (Intrinsics.areEqual(jSONObject2.optString("role"), jSONObject.optString("role")) && Intrinsics.areEqual(jSONObject2.optString("common"), jSONObject.optString("common"))) {
                                    break;
                                }
                                i3++;
                                jSONArrayOptJSONArray = jSONArray;
                                jSONArray3 = jSONArray2;
                            } else {
                                jSONArray = jSONArrayOptJSONArray;
                                jSONArray2 = jSONArray3;
                                bufferedWriter2.write(",");
                                writeValue(jSONObject, bufferedWriter2);
                                break;
                            }
                        }
                        i2++;
                        itKeys = it;
                        jSONArrayOptJSONArray = jSONArray;
                        jSONArray3 = jSONArray2;
                    }
                }
                bufferedWriter2.write("]");
                itKeys = itKeys;
                z = false;
            }
            Iterator<String> itKeys2 = newData.keys();
            while (itKeys2.hasNext()) {
                String next2 = itKeys2.next();
                if (!linkedHashSet.contains(next2)) {
                    if (!z) {
                        bufferedWriter2.write(",");
                    }
                    bufferedWriter2.write("\"");
                    Intrinsics.checkNotNull(next2);
                    bufferedWriter2.write(escapeJson(next2));
                    bufferedWriter2.write("\":");
                    writeValue(newData.getJSONArray(next2), bufferedWriter2);
                    z = false;
                }
            }
            bufferedWriter2.write("}");
            bufferedWriter2.flush();
            Unit unit = Unit.INSTANCE;
            CloseableKt.closeFinally(bufferedWriter, null);
        } catch (Throwable th) {
            try {
                throw th;
            } catch (Throwable th2) {
                CloseableKt.closeFinally(bufferedWriter, th);
                throw th2;
            }
        }
    }

    private final String escapeJson(String s) {
        return StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(StringsKt.replace$default(s, "\\", "\\\\", false, 4, (Object) null), "\"", "\\\"", false, 4, (Object) null), "\n", "\\n", false, 4, (Object) null), "\r", "\\r", false, 4, (Object) null), "\t", "\\t", false, 4, (Object) null);
    }

    private final JSONObject loadPromptsMap() {
        try {
            return new JSONObject(loadPrompts());
        } catch (Exception unused) {
            return new JSONObject();
        }
    }

    private final int findItemIndex(JSONArray items, String role, String common) throws JSONException {
        int length = items.length();
        for (int i = 0; i < length; i++) {
            JSONObject jSONObject = items.getJSONObject(i);
            if (Intrinsics.areEqual(jSONObject.optString("role"), role) && Intrinsics.areEqual(jSONObject.optString("common"), common)) {
                return i;
            }
        }
        return -1;
    }

    private final void copyFile(File src, File dest) throws IOException {
        File parentFile = dest.getParentFile();
        if (parentFile != null) {
            parentFile.mkdirs();
        }
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

    private final void walkAndZip(ZipOutputStream zos, File dir, String base) throws IOException {
        File[] fileArrListFiles = dir.listFiles();
        if (fileArrListFiles != null) {
            for (File file : fileArrListFiles) {
                String name = base.length() == 0 ? file.getName() : base + "/" + file.getName();
                if (file.isDirectory()) {
                    Intrinsics.checkNotNull(file);
                    Intrinsics.checkNotNull(name);
                    walkAndZip(zos, file, name);
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
    }

    private final void deleteRecursively(File file) {
        File[] fileArrListFiles;
        if (file.isDirectory() && (fileArrListFiles = file.listFiles()) != null) {
            for (File file2 : fileArrListFiles) {
                Intrinsics.checkNotNull(file2);
                deleteRecursively(file2);
            }
        }
        file.delete();
    }
}
