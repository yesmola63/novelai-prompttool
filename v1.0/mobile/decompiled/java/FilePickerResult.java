package com.novelai.prompttool;

import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;

/* JADX INFO: compiled from: PromptsServer.kt */
/* JADX INFO: loaded from: classes.dex */
@Metadata(d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0017\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001BC\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0005\u0012\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u0005\u0012\b\b\u0002\u0010\b\u001a\u00020\u0003\u0012\b\b\u0002\u0010\t\u001a\u00020\u0003¢\u0006\u0002\u0010\nJ\t\u0010\u0013\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0014\u001a\u00020\u0005HÆ\u0003J\t\u0010\u0015\u001a\u00020\u0005HÆ\u0003J\u000b\u0010\u0016\u001a\u0004\u0018\u00010\u0005HÆ\u0003J\t\u0010\u0017\u001a\u00020\u0003HÆ\u0003J\t\u0010\u0018\u001a\u00020\u0003HÆ\u0003JG\u0010\u0019\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00052\n\b\u0002\u0010\u0007\u001a\u0004\u0018\u00010\u00052\b\b\u0002\u0010\b\u001a\u00020\u00032\b\b\u0002\u0010\t\u001a\u00020\u0003HÆ\u0001J\u0013\u0010\u001a\u001a\u00020\u00032\b\u0010\u001b\u001a\u0004\u0018\u00010\u0001HÖ\u0003J\t\u0010\u001c\u001a\u00020\u001dHÖ\u0001J\t\u0010\u001e\u001a\u00020\u0005HÖ\u0001R\u0013\u0010\u0007\u001a\u0004\u0018\u00010\u0005¢\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\u0006\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\r\u0010\fR\u0011\u0010\t\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005¢\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\fR\u0011\u0010\u0002\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u000fR\u0011\u0010\b\u001a\u00020\u0003¢\u0006\b\n\u0000\u001a\u0004\b\u0012\u0010\u000f¨\u0006\u001f"}, d2 = {"Lcom/novelai/prompttool/FilePickerResult;", "", "success", "", "path", "", "filename", "error", "valid", "needPassword", "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;ZZ)V", "getError", "()Ljava/lang/String;", "getFilename", "getNeedPassword", "()Z", "getPath", "getSuccess", "getValid", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "other", "hashCode", "", "toString", "app_release"}, k = 1, mv = {1, 9, 0}, xi = ConstraintLayout.LayoutParams.Table.LAYOUT_CONSTRAINT_VERTICAL_CHAINSTYLE)
public final /* data */ class FilePickerResult {
    private final String error;
    private final String filename;
    private final boolean needPassword;
    private final String path;
    private final boolean success;
    private final boolean valid;

    public FilePickerResult() {
        this(false, null, null, null, false, false, 63, null);
    }

    public static /* synthetic */ FilePickerResult copy$default(FilePickerResult filePickerResult, boolean z, String str, String str2, String str3, boolean z2, boolean z3, int i, Object obj) {
        if ((i & 1) != 0) {
            z = filePickerResult.success;
        }
        if ((i & 2) != 0) {
            str = filePickerResult.path;
        }
        String str4 = str;
        if ((i & 4) != 0) {
            str2 = filePickerResult.filename;
        }
        String str5 = str2;
        if ((i & 8) != 0) {
            str3 = filePickerResult.error;
        }
        String str6 = str3;
        if ((i & 16) != 0) {
            z2 = filePickerResult.valid;
        }
        boolean z4 = z2;
        if ((i & 32) != 0) {
            z3 = filePickerResult.needPassword;
        }
        return filePickerResult.copy(z, str4, str5, str6, z4, z3);
    }

    /* JADX INFO: renamed from: component1, reason: from getter */
    public final boolean getSuccess() {
        return this.success;
    }

    /* JADX INFO: renamed from: component2, reason: from getter */
    public final String getPath() {
        return this.path;
    }

    /* JADX INFO: renamed from: component3, reason: from getter */
    public final String getFilename() {
        return this.filename;
    }

    /* JADX INFO: renamed from: component4, reason: from getter */
    public final String getError() {
        return this.error;
    }

    /* JADX INFO: renamed from: component5, reason: from getter */
    public final boolean getValid() {
        return this.valid;
    }

    /* JADX INFO: renamed from: component6, reason: from getter */
    public final boolean getNeedPassword() {
        return this.needPassword;
    }

    public final FilePickerResult copy(boolean success, String path, String filename, String error, boolean valid, boolean needPassword) {
        Intrinsics.checkNotNullParameter(path, "path");
        Intrinsics.checkNotNullParameter(filename, "filename");
        return new FilePickerResult(success, path, filename, error, valid, needPassword);
    }

    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof FilePickerResult)) {
            return false;
        }
        FilePickerResult filePickerResult = (FilePickerResult) other;
        return this.success == filePickerResult.success && Intrinsics.areEqual(this.path, filePickerResult.path) && Intrinsics.areEqual(this.filename, filePickerResult.filename) && Intrinsics.areEqual(this.error, filePickerResult.error) && this.valid == filePickerResult.valid && this.needPassword == filePickerResult.needPassword;
    }

    public int hashCode() {
        int iHashCode = ((((Boolean.hashCode(this.success) * 31) + this.path.hashCode()) * 31) + this.filename.hashCode()) * 31;
        String str = this.error;
        return ((((iHashCode + (str == null ? 0 : str.hashCode())) * 31) + Boolean.hashCode(this.valid)) * 31) + Boolean.hashCode(this.needPassword);
    }

    public String toString() {
        return "FilePickerResult(success=" + this.success + ", path=" + this.path + ", filename=" + this.filename + ", error=" + this.error + ", valid=" + this.valid + ", needPassword=" + this.needPassword + ")";
    }

    public FilePickerResult(boolean z, String path, String filename, String str, boolean z2, boolean z3) {
        Intrinsics.checkNotNullParameter(path, "path");
        Intrinsics.checkNotNullParameter(filename, "filename");
        this.success = z;
        this.path = path;
        this.filename = filename;
        this.error = str;
        this.valid = z2;
        this.needPassword = z3;
    }

    public /* synthetic */ FilePickerResult(boolean z, String str, String str2, String str3, boolean z2, boolean z3, int i, DefaultConstructorMarker defaultConstructorMarker) {
        this((i & 1) != 0 ? false : z, (i & 2) != 0 ? "" : str, (i & 4) == 0 ? str2 : "", (i & 8) != 0 ? null : str3, (i & 16) != 0 ? false : z2, (i & 32) != 0 ? false : z3);
    }

    public final boolean getSuccess() {
        return this.success;
    }

    public final String getPath() {
        return this.path;
    }

    public final String getFilename() {
        return this.filename;
    }

    public final String getError() {
        return this.error;
    }

    public final boolean getValid() {
        return this.valid;
    }

    public final boolean getNeedPassword() {
        return this.needPassword;
    }
}
