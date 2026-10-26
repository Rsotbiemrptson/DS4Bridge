package com.odin2.ds4bridgehelper;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileOutputStream;

/** Read-only bridge settings endpoint for the shell-owned firmware watcher. */
public final class TargetsProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        return true;
    }

    private File targetFile() {
        return new File(getContext().createDeviceProtectedStorageContext().getFilesDir(),
                "targets.conf");
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        if (!"r".equals(mode)
                || !(getContext().getPackageName() + ".targets").equals(uri.getAuthority())
                || !("/targets".equals(uri.getPath()) || "/profile".equals(uri.getPath()) || "/report".equals(uri.getPath()))) {
            throw new java.io.FileNotFoundException("Unknown or non-read-only bridge URI");
        }
        File file = targetFile();
        if ("/profile".equals(uri.getPath())) file = new File(file.getParentFile(), "profile.conf");
        if ("/report".equals(uri.getPath())) {
            int uid = android.os.Binder.getCallingUid();
            if (uid != 0 && uid != 2000 && uid != android.os.Process.myUid())
                throw new java.io.FileNotFoundException("Report is private; use Share report");
            file = new File(file.getParentFile(), "diagnostics.txt");
        }
        if (!file.exists()) {
            try (FileOutputStream output = new FileOutputStream(file)) {
                output.getFD().sync();
            } catch (Exception e) {
                throw new java.io.FileNotFoundException("Could not initialize targets: " + e);
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return "text/plain"; }
    @Override public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read only");
    }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read only");
    }
    @Override public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs) {
        throw new UnsupportedOperationException("read only");
    }
}
