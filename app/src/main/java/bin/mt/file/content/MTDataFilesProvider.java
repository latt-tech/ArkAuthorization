package bin.mt.file.content;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsProvider;
import android.system.Os;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MTDataFilesProvider extends DocumentsProvider {
    public static final String[] g = {"root_id", "mime_types", "flags", "icon", "title", "summary", "document_id"};
    public static final String[] h = {"document_id", "mime_type", "_display_name", "last_modified", "flags", "_size", "mt_extras"};
    public String b;
    public File c;
    public File d;
    public File e;
    public File f;

    /* JADX WARNING: Removed duplicated region for block: B:16:0x002f  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean a(java.io.File r5) {
        /*
            boolean r0 = r5.isDirectory()
            if (r0 == 0) goto L_0x003b
            r0 = 0
            java.lang.String r1 = r5.getPath()     // Catch:{ ErrnoException -> 0x001e }
            android.system.StructStat r1 = android.system.Os.lstat(r1)     // Catch:{ ErrnoException -> 0x001e }
            int r1 = r1.st_mode     // Catch:{ ErrnoException -> 0x001e }
            r2 = 61440(0xf000, float:8.6096E-41)
            r1 = r1 & r2
            r2 = 40960(0xa000, float:5.7397E-41)
            if (r1 != r2) goto L_0x001c
            r1 = 1
            goto L_0x0023
        L_0x001c:
            r1 = 0
            goto L_0x0023
        L_0x001e:
            r1 = move-exception
            r1.printStackTrace()
            goto L_0x001c
        L_0x0023:
            if (r1 != 0) goto L_0x003b
            java.io.File[] r1 = r5.listFiles()
            if (r1 == 0) goto L_0x003b
            int r2 = r1.length
            r3 = 0
        L_0x002d:
            if (r3 >= r2) goto L_0x003b
            r4 = r1[r3]
            boolean r4 = a(r4)
            if (r4 != 0) goto L_0x0038
            return r0
        L_0x0038:
            int r3 = r3 + 1
            goto L_0x002d
        L_0x003b:
            boolean r5 = r5.delete()
            return r5
        */
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.a(java.io.File):boolean");
    }

    /* JADX WARNING: Code restructure failed: missing block: B:5:0x0015, code lost:
        r1 = android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(r1.substring(r0 + 1).toLowerCase());
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.lang.String c(java.io.File r1) {
        /*
            boolean r0 = r1.isDirectory()
            if (r0 == 0) goto L_0x0009
            java.lang.String r1 = "vnd.android.document/directory"
            return r1
        L_0x0009:
            java.lang.String r1 = r1.getName()
            r0 = 46
            int r0 = r1.lastIndexOf(r0)
            if (r0 < 0) goto L_0x002a
            int r0 = r0 + 1
            java.lang.String r1 = r1.substring(r0)
            java.lang.String r1 = r1.toLowerCase()
            android.webkit.MimeTypeMap r0 = android.webkit.MimeTypeMap.getSingleton()
            java.lang.String r1 = r0.getMimeTypeFromExtension(r1)
            if (r1 == 0) goto L_0x002a
            return r1
        L_0x002a:
            java.lang.String r1 = "application/octet-stream"
            return r1
        */
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.c(java.io.File):java.lang.String");
    }

    public final void attachInfo(Context context, ProviderInfo providerInfo) {
        super.attachInfo(context, providerInfo);
        this.b = context.getPackageName();
        File parentFile = context.getFilesDir().getParentFile();
        this.c = parentFile;
        String path = parentFile.getPath();
        if (path.startsWith("/data/user/")) {
            this.d = new File("/data/user_de/" + path.substring(11));
        }
        File externalFilesDir = context.getExternalFilesDir((String) null);
        if (externalFilesDir != null) {
            this.e = externalFilesDir.getParentFile();
        }
        this.f = context.getObbDir();
    }

    public final File b(String str, boolean z) {
        String str2;
        if (str.startsWith(this.b)) {
            String substring = str.substring(this.b.length());
            if (substring.startsWith("/")) {
                substring = substring.substring(1);
            }
            File file = null;
            if (substring.isEmpty()) {
                return null;
            }
            int indexOf = substring.indexOf(47);
            if (indexOf == -1) {
                str2 = "";
            } else {
                String substring2 = substring.substring(0, indexOf);
                str2 = substring.substring(indexOf + 1);
                substring = substring2;
            }
            if (substring.equalsIgnoreCase("data")) {
                file = new File(this.c, str2);
            } else if (substring.equalsIgnoreCase("android_data") && this.e != null) {
                file = new File(this.e, str2);
            } else if (substring.equalsIgnoreCase("android_obb") && this.f != null) {
                file = new File(this.f, str2);
            } else if (substring.equalsIgnoreCase("user_de_data") && this.d != null) {
                file = new File(this.d, str2);
            }
            if (file != null) {
                if (z) {
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            Os.lstat(file.getPath());
                        }
                    } catch (Exception unused) {
                        try {
                            throw new FileNotFoundException(str.concat(" not found"));
                        } catch (FileNotFoundException e) {}
                    }
                }
                return file;
            }
            try {
                throw new FileNotFoundException(str.concat(" not found"));
            } catch (FileNotFoundException e) {}
        }
        try {
            throw new FileNotFoundException(str.concat(" not found"));
        } catch (FileNotFoundException e) {}
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0074 A[Catch:{ Exception -> 0x00dd }] */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x00c8 A[Catch:{ Exception -> 0x00dd }] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final android.os.Bundle call(java.lang.String r10, java.lang.String r11, android.os.Bundle r12) {
        /*
            r9 = this;
            java.lang.String r0 = "message"
            java.lang.String r1 = "result"
            java.lang.String r2 = "Unsupported method: "
            android.os.Bundle r11 = super.call(r10, r11, r12)
            if (r11 == 0) goto L_0x000d
            return r11
        L_0x000d:
            java.lang.String r11 = "mt:"
            boolean r11 = r10.startsWith(r11)
            if (r11 != 0) goto L_0x0017
            r10 = 0
            return r10
        L_0x0017:
            android.os.Bundle r11 = new android.os.Bundle
            r11.<init>()
            r3 = 0
            java.lang.String r4 = "uri"
            android.os.Parcelable r4 = r12.getParcelable(r4)     // Catch:{ Exception -> 0x00dd }
            android.net.Uri r4 = (android.net.Uri) r4     // Catch:{ Exception -> 0x00dd }
            java.util.List r4 = r4.getPathSegments()     // Catch:{ Exception -> 0x00dd }
            int r5 = r4.size()     // Catch:{ Exception -> 0x00dd }
            r6 = 4
            r7 = 1
            if (r5 < r6) goto L_0x0039
            r5 = 3
            java.lang.Object r4 = r4.get(r5)     // Catch:{ Exception -> 0x00dd }
        L_0x0036:
            java.lang.String r4 = (java.lang.String) r4     // Catch:{ Exception -> 0x00dd }
            goto L_0x003e
        L_0x0039:
            java.lang.Object r4 = r4.get(r7)     // Catch:{ Exception -> 0x00dd }
            goto L_0x0036
        L_0x003e:
            int r5 = r10.hashCode()     // Catch:{ Exception -> 0x00dd }
            r6 = -1645162251(0xffffffff9df0d0f5, float:-6.3743528E-21)
            r8 = 2
            if (r5 == r6) goto L_0x0067
            r6 = 214442514(0xcc82212, float:3.0835384E-31)
            if (r5 == r6) goto L_0x005d
            r6 = 1713485102(0x6621b52e, float:1.909108E23)
            if (r5 == r6) goto L_0x0053
            goto L_0x0071
        L_0x0053:
            java.lang.String r5 = "mt:setLastModified"
            boolean r5 = r10.equals(r5)     // Catch:{ Exception -> 0x00dd }
            if (r5 == 0) goto L_0x0071
            r5 = 0
            goto L_0x0072
        L_0x005d:
            java.lang.String r5 = "mt:createSymlink"
            boolean r5 = r10.equals(r5)     // Catch:{ Exception -> 0x00dd }
            if (r5 == 0) goto L_0x0071
            r5 = 2
            goto L_0x0072
        L_0x0067:
            java.lang.String r5 = "mt:setPermissions"
            boolean r5 = r10.equals(r5)     // Catch:{ Exception -> 0x00dd }
            if (r5 == 0) goto L_0x0071
            r5 = 1
            goto L_0x0072
        L_0x0071:
            r5 = -1
        L_0x0072:
            if (r5 == 0) goto L_0x00c8
            if (r5 == r7) goto L_0x00a7
            if (r5 == r8) goto L_0x0083
            r11.putBoolean(r1, r3)     // Catch:{ Exception -> 0x00dd }
            java.lang.String r10 = r2.concat(r10)     // Catch:{ Exception -> 0x00dd }
        L_0x007f:
            r11.putString(r0, r10)     // Catch:{ Exception -> 0x00dd }
            goto L_0x00e8
        L_0x0083:
            java.io.File r10 = r9.b(r4, r3)     // Catch:{ Exception -> 0x00dd }
            if (r10 != 0) goto L_0x008d
        L_0x0089:
            r11.putBoolean(r1, r3)     // Catch:{ Exception -> 0x00dd }
            goto L_0x00e8
        L_0x008d:
            java.lang.String r2 = "path"
            java.lang.String r12 = r12.getString(r2)     // Catch:{ Exception -> 0x00dd }
            java.lang.String r10 = r10.getPath()     // Catch:{ ErrnoException -> 0x009e }
            android.system.Os.symlink(r12, r10)     // Catch:{ ErrnoException -> 0x009e }
            r11.putBoolean(r1, r7)     // Catch:{ ErrnoException -> 0x009e }
            goto L_0x00e8
        L_0x009e:
            r10 = move-exception
            r11.putBoolean(r1, r3)     // Catch:{ Exception -> 0x00dd }
            java.lang.String r10 = r10.getMessage()     // Catch:{ Exception -> 0x00dd }
            goto L_0x007f
        L_0x00a7:
            java.io.File r10 = r9.b(r4, r7)     // Catch:{ Exception -> 0x00dd }
            if (r10 != 0) goto L_0x00ae
            goto L_0x0089
        L_0x00ae:
            java.lang.String r2 = "permissions"
            int r12 = r12.getInt(r2)     // Catch:{ Exception -> 0x00dd }
            java.lang.String r10 = r10.getPath()     // Catch:{ ErrnoException -> 0x00bf }
            android.system.Os.chmod(r10, r12)     // Catch:{ ErrnoException -> 0x00bf }
            r11.putBoolean(r1, r7)     // Catch:{ ErrnoException -> 0x00bf }
            goto L_0x00e8
        L_0x00bf:
            r10 = move-exception
            r11.putBoolean(r1, r3)     // Catch:{ Exception -> 0x00dd }
            java.lang.String r10 = r10.getMessage()     // Catch:{ Exception -> 0x00dd }
            goto L_0x007f
        L_0x00c8:
            java.io.File r10 = r9.b(r4, r7)     // Catch:{ Exception -> 0x00dd }
            if (r10 != 0) goto L_0x00cf
            goto L_0x0089
        L_0x00cf:
            java.lang.String r2 = "time"
            long r4 = r12.getLong(r2)     // Catch:{ Exception -> 0x00dd }
            boolean r10 = r10.setLastModified(r4)     // Catch:{ Exception -> 0x00dd }
            r11.putBoolean(r1, r10)     // Catch:{ Exception -> 0x00dd }
            goto L_0x00e8
        L_0x00dd:
            r10 = move-exception
            r11.putBoolean(r1, r3)
            java.lang.String r10 = r10.toString()
            r11.putString(r0, r10)
        L_0x00e8:
            return r11
        */
        super.call(r10, r11, r12);
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.call(java.lang.String, java.lang.String, android.os.Bundle):android.os.Bundle");
    }

    public final String createDocument(String str, String str2, String str3) {
        StringBuilder sb;
        File b2 = b(str, true);
        if (b2 != null) {
            File file = new File(b2, str3);
            int i = 2;
            while (file.exists()) {
                file = new File(b2, str3 + " (" + i + ")");
                i++;
            }
            try {
                if ("vnd.android.document/directory".equals(str2) ? file.mkdir() : file.createNewFile()) {
                    if (str.endsWith("/")) {
                        sb = new StringBuilder();
                        sb.append(str);
                        sb.append(file.getName());
                    } else {
                        sb = new StringBuilder();
                        sb.append(str);
                        sb.append("/");
                        sb.append(file.getName());
                    }
                    return sb.toString();
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        try {
            throw new FileNotFoundException("Failed to create document in " + str + " with name " + str3);
        } catch (FileNotFoundException e) {}
        return null;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0065  */
    /* JADX WARNING: Removed duplicated region for block: B:21:0x0079  */
    /* JADX WARNING: Removed duplicated region for block: B:22:0x007c  */
    /* JADX WARNING: Removed duplicated region for block: B:40:0x00ee A[SYNTHETIC, Splitter:B:40:0x00ee] */
    /* JADX WARNING: Removed duplicated region for block: B:48:? A[RETURN, SYNTHETIC] */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final void d(android.database.MatrixCursor r13, java.lang.String r14, java.io.File r15) {
        /*
            r12 = this;
            java.lang.String r0 = "|"
            r1 = 1
            if (r15 != 0) goto L_0x0009
            java.io.File r15 = r12.b(r14, r1)
        L_0x0009:
            r2 = 0
            java.lang.String r3 = "flags"
            java.lang.String r4 = "last_modified"
            java.lang.String r5 = "mime_type"
            java.lang.String r6 = "_size"
            java.lang.String r7 = "_display_name"
            java.lang.String r8 = "document_id"
            if (r15 != 0) goto L_0x0043
            android.database.MatrixCursor$RowBuilder r13 = r13.newRow()
            java.lang.String r14 = r12.b
            r13.add(r8, r14)
            java.lang.String r14 = r12.b
            r13.add(r7, r14)
            r14 = 0
            java.lang.Long r14 = java.lang.Long.valueOf(r14)
            r13.add(r6, r14)
            java.lang.String r14 = "vnd.android.document/directory"
            r13.add(r5, r14)
            java.lang.Integer r14 = java.lang.Integer.valueOf(r2)
            r13.add(r4, r14)
            java.lang.Integer r14 = java.lang.Integer.valueOf(r2)
            r13.add(r3, r14)
            return
        L_0x0043:
            boolean r9 = r15.isDirectory()
            if (r9 == 0) goto L_0x0052
            boolean r9 = r15.canWrite()
            if (r9 == 0) goto L_0x005a
            r9 = 8
            goto L_0x005b
        L_0x0052:
            boolean r9 = r15.canWrite()
            if (r9 == 0) goto L_0x005a
            r9 = 2
            goto L_0x005b
        L_0x005a:
            r9 = 0
        L_0x005b:
            java.io.File r10 = r15.getParentFile()
            boolean r10 = r10.canWrite()
            if (r10 == 0) goto L_0x0069
            r9 = r9 | 4
            r9 = r9 | 64
        L_0x0069:
            java.lang.String r10 = r15.getPath()
            java.io.File r11 = r12.c
            java.lang.String r11 = r11.getPath()
            boolean r11 = r10.equals(r11)
            if (r11 == 0) goto L_0x007c
            java.lang.String r1 = "data"
            goto L_0x00b5
        L_0x007c:
            java.io.File r11 = r12.e
            if (r11 == 0) goto L_0x008d
            java.lang.String r11 = r11.getPath()
            boolean r11 = r10.equals(r11)
            if (r11 == 0) goto L_0x008d
            java.lang.String r1 = "android_data"
            goto L_0x00b5
        L_0x008d:
            java.io.File r11 = r12.f
            if (r11 == 0) goto L_0x009e
            java.lang.String r11 = r11.getPath()
            boolean r11 = r10.equals(r11)
            if (r11 == 0) goto L_0x009e
            java.lang.String r1 = "android_obb"
            goto L_0x00b5
        L_0x009e:
            java.io.File r11 = r12.d
            if (r11 == 0) goto L_0x00af
            java.lang.String r11 = r11.getPath()
            boolean r11 = r10.equals(r11)
            if (r11 == 0) goto L_0x00af
            java.lang.String r1 = "user_de_data"
            goto L_0x00b5
        L_0x00af:
            java.lang.String r2 = r15.getName()
            r1 = r2
            r2 = 1
        L_0x00b5:
            android.database.MatrixCursor$RowBuilder r13 = r13.newRow()
            r13.add(r8, r14)
            r13.add(r7, r1)
            long r7 = r15.length()
            java.lang.Long r14 = java.lang.Long.valueOf(r7)
            r13.add(r6, r14)
            java.lang.String r14 = c(r15)
            r13.add(r5, r14)
            long r5 = r15.lastModified()
            java.lang.Long r14 = java.lang.Long.valueOf(r5)
            r13.add(r4, r14)
            java.lang.Integer r14 = java.lang.Integer.valueOf(r9)
            r13.add(r3, r14)
            java.lang.String r14 = "mt_path"
            java.lang.String r15 = r15.getAbsolutePath()
            r13.add(r14, r15)
            if (r2 == 0) goto L_0x012f
            java.lang.StringBuilder r14 = new java.lang.StringBuilder     // Catch:{ Exception -> 0x012b }
            r14.<init>()     // Catch:{ Exception -> 0x012b }
            android.system.StructStat r15 = android.system.Os.lstat(r10)     // Catch:{ Exception -> 0x012b }
            int r1 = r15.st_mode     // Catch:{ Exception -> 0x012b }
            r14.append(r1)     // Catch:{ Exception -> 0x012b }
            r14.append(r0)     // Catch:{ Exception -> 0x012b }
            int r1 = r15.st_uid     // Catch:{ Exception -> 0x012b }
            r14.append(r1)     // Catch:{ Exception -> 0x012b }
            r14.append(r0)     // Catch:{ Exception -> 0x012b }
            int r1 = r15.st_gid     // Catch:{ Exception -> 0x012b }
            r14.append(r1)     // Catch:{ Exception -> 0x012b }
            int r15 = r15.st_mode     // Catch:{ Exception -> 0x012b }
            r1 = 61440(0xf000, float:8.6096E-41)
            r15 = r15 & r1
            r1 = 40960(0xa000, float:5.7397E-41)
            if (r15 != r1) goto L_0x0121
            r14.append(r0)     // Catch:{ Exception -> 0x012b }
            java.lang.String r15 = android.system.Os.readlink(r10)     // Catch:{ Exception -> 0x012b }
            r14.append(r15)     // Catch:{ Exception -> 0x012b }
        L_0x0121:
            java.lang.String r15 = "mt_extras"
            java.lang.String r14 = r14.toString()     // Catch:{ Exception -> 0x012b }
            r13.add(r15, r14)     // Catch:{ Exception -> 0x012b }
            goto L_0x012f
        L_0x012b:
            r13 = move-exception
            r13.printStackTrace()
        L_0x012f:
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: bin.mt.file.content.MTDataFilesProvider.d(android.database.MatrixCursor, java.lang.String, java.io.File):void");
    }

    public final void deleteDocument(String str) {
        File b2 = b(str, true);
        if (b2 == null || !a(b2)) {
            try {
                throw new FileNotFoundException("Failed to delete document ".concat(str));
            } catch (FileNotFoundException e) {}
        }
    }

    public final String getDocumentType(String str) {
        File b2 = b(str, true);
        return b2 == null ? "vnd.android.document/directory" : c(b2);
    }

    public final boolean isChildDocument(String str, String str2) {
        return str2.startsWith(str);
    }

    public final String moveDocument(String str, String str2, String str3) {
        File b2 = b(str, true);
        File b3 = b(str3, true);
        if (!(b2 == null || b3 == null)) {
            File file = new File(b3, b2.getName());
            if (!file.exists() && b2.renameTo(file)) {
                if (str3.endsWith("/")) {
                    return str3 + file.getName();
                }
                return str3 + "/" + file.getName();
            }
        }
        try {
            throw new FileNotFoundException("Filed to move document " + str + " to " + str3);
        } catch (FileNotFoundException e) {}
        return null;
    }

    public final boolean onCreate() {
        return true;
    }

    public final ParcelFileDescriptor openDocument(String str, String str2, CancellationSignal cancellationSignal) {
        File b2 = b(str, false);
        if (b2 != null) {
            try {
                return ParcelFileDescriptor.open(b2, ParcelFileDescriptor.parseMode(str2));
            } catch (FileNotFoundException e) {}
        }
        try {
            throw new FileNotFoundException(str.concat(" not found"));
        } catch (FileNotFoundException e) {}
        return null;
    }

    public final Cursor queryChildDocuments(String str, String[] strArr, String str2) {
        if (str.endsWith("/")) {
            str = str.substring(0, str.length() - 1);
        }
        if (strArr == null) {
            strArr = h;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        File b2 = b(str, true);
        if (b2 == null) {
            d(matrixCursor, str.concat("/data"), this.c);
            File file = this.e;
            if (file != null && file.exists()) {
                d(matrixCursor, str.concat("/android_data"), this.e);
            }
            File file2 = this.f;
            if (file2 != null && file2.exists()) {
                d(matrixCursor, str.concat("/android_obb"), this.f);
            }
            File file3 = this.d;
            if (file3 != null && file3.exists()) {
                d(matrixCursor, str.concat("/user_de_data"), this.d);
            }
        } else {
            File[] listFiles = b2.listFiles();
            if (listFiles != null) {
                for (File file4 : listFiles) {
                    d(matrixCursor, str + "/" + file4.getName(), file4);
                }
            }
        }
        return matrixCursor;
    }

    public final Cursor queryDocument(String str, String[] strArr) {
        if (strArr == null) {
            strArr = h;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        d(matrixCursor, str, (File) null);
        return matrixCursor;
    }

    public final Cursor queryRoots(String[] strArr) {
        ApplicationInfo applicationInfo = getContext().getApplicationInfo();
        String charSequence = applicationInfo.loadLabel(getContext().getPackageManager()).toString();
        if (strArr == null) {
            strArr = g;
        }
        MatrixCursor matrixCursor = new MatrixCursor(strArr);
        MatrixCursor.RowBuilder newRow = matrixCursor.newRow();
        newRow.add("root_id", this.b);
        newRow.add("document_id", this.b);
        newRow.add("summary", this.b);
        newRow.add("flags", 17);
        newRow.add("title", charSequence);
        newRow.add("mime_types", "*/*");
        newRow.add("icon", Integer.valueOf(applicationInfo.icon));
        return matrixCursor;
    }

    public final void removeDocument(String str, String str2) {
        deleteDocument(str);
    }

    public final String renameDocument(String str, String str2) {
        File b2 = b(str, true);
        if (b2 == null || !b2.renameTo(new File(b2.getParentFile(), str2))) {
            try {
                throw new FileNotFoundException("Failed to rename document " + str + " to " + str2);
            } catch (FileNotFoundException e) {}
        }
        int lastIndexOf = str.lastIndexOf(47, str.length() - 2);
        return str.substring(0, lastIndexOf) + "/" + str2;
    }
}
