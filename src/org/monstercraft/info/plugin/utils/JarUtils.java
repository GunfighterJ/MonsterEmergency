package org.monstercraft.info.plugin.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.monstercraft.info.MonsterEmergency;

public class JarUtils {

    public static boolean extractFromJar(final String fileName,
            final String dest) throws IOException {
        if (getRunningJarPath() == null) {
            return false;
        }
        final File file = new File(dest);
        if (file.isDirectory()) {
            file.mkdir();
            return false;
        }
        if (!file.exists()) {
            file.getParentFile().mkdirs();
        }

        final JarFile jar = getRunningJar();
        final Enumeration<JarEntry> e = jar.entries();
        while (e.hasMoreElements()) {
            final JarEntry je = e.nextElement();
            if (!je.getName().contains(fileName)) {
                continue;
            }
            final InputStream in = new BufferedInputStream(
                    jar.getInputStream(je));
            final OutputStream out = new BufferedOutputStream(
                    new FileOutputStream(file));
            copyInputStream(in, out);
            jar.close();
            return true;
        }
        jar.close();
        return false;
    }

    public static URL getJarUrl(final File file) throws IOException {
        URL url = file.toURI().toURL();
        url = new URL("jar:" + url.toExternalForm() + "!/");
        return url;
    }

    private final static void copyInputStream(final InputStream in,
            final OutputStream out) throws IOException {
        try {
            final byte[] buff = new byte[4096];
            int n;
            while ((n = in.read(buff)) > 0) {
                out.write(buff, 0, n);
            }
        } finally {
            out.flush();
            out.close();
            in.close();
        }
    }

    public static String getRunningJarPath()
            throws UnsupportedEncodingException {
        if (!RUNNING_FROM_JAR) {
            return null; // null if not running from jar
        }
        String path = new File(MonsterEmergency.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getParent();
        path = URLDecoder.decode(path, "UTF-8");
        return path;
    }

    public static JarFile getRunningJar() throws IOException {
        if (!RUNNING_FROM_JAR) {
            return null; // null if not running from jar
        }
        String path = new File(MonsterEmergency.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath()).getAbsolutePath();
        path = URLDecoder.decode(path, "UTF-8");
        return new JarFile(path);
    }

    private static boolean RUNNING_FROM_JAR = false;

    static {
        final URL resource = MonsterEmergency.class.getClassLoader()
                .getResource(Paths.PLUGIN);
        if (resource != null) {
            RUNNING_FROM_JAR = true;
        }
    }

    public static final class Paths {

        private static final String JARS = "lib" + File.separator;

        public static final String ACTIVATOR = JARS + "activation";

        public static final String MAILAPI = JARS + "mail.jar";

        private static final String PLUGIN = "plugin.yml";

    }

}
