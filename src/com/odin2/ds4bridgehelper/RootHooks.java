package com.odin2.ds4bridgehelper;

import android.os.IBinder;
import android.os.Parcel;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

/** Known OEM PServer command interface. Unknown Binder services are never invoked. */
final class RootHooks {
    static IBinder service() throws Exception {
        Class<?> manager = Class.forName("android.os.ServiceManager");
        Method get = manager.getDeclaredMethod("getService", String.class);
        return (IBinder) get.invoke(null, "PServerBinder");
    }
    static String execute(String command) throws Exception {
        if (command.getBytes(StandardCharsets.UTF_8).length > 192)
            throw new IllegalArgumentException("Privileged request exceeds the safe command length");
        IBinder service = service();
        if (service == null) throw new IllegalStateException("OEM privileged service (PServerBinder) unavailable");
        Parcel input = Parcel.obtain(), output = Parcel.obtain();
        try {
            input.writeStringArray(new String[]{command, "1"});
            if (!service.transact(0, input, output, 0))
                throw new IllegalStateException("Root service rejected the request");
            byte[] bytes = output.createByteArray();
            return bytes == null ? "" : new String(bytes, StandardCharsets.UTF_8).trim();
        } finally { input.recycle(); output.recycle(); }
    }
    static void requireRoot() throws Exception {
        if (!"0".equals(execute("id -u"))) throw new IllegalStateException("Service did not provide root");
    }
    static String output(String command) throws Exception {
        // Stock PServer returns only the first output line. Bound and encode
        // diagnostic output as one line, then decode inside the app.
        String encoded = execute("(" + command + ") 2>&1 | head -c 32000 | base64 -w 0");
        return new String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), StandardCharsets.UTF_8).trim();
    }
    static String candidates() {
        StringBuilder result = new StringBuilder();
        try {
            Class<?> manager = Class.forName("android.os.ServiceManager");
            String[] names = (String[]) manager.getDeclaredMethod("listServices").invoke(null);
            if (names != null) for (String name : names) {
                String lower = name.toLowerCase(java.util.Locale.ROOT);
                if (lower.contains("pserver") || lower.contains("root") || lower.contains("mapping")
                        || lower.contains("controller") || lower.contains("pservice")) result.append(name).append('\n');
            }
        } catch (Exception error) { result.append("Service inventory unavailable: ").append(error.getClass().getSimpleName()); }
        return result.toString();
    }
}
