package org.kingdoms.versioning;

import java.util.Locale;

public final class JavaVersion {
    private static final int VERSION = getVersion0();

    private static int getVersion0() {
        // Runtime.version()
        // Runtime.class.getPackage().getSpecificationVersion();

        // There are also "java.specification.version", "java.vm.specification.version" and "java.runtime.version"
        // but the one below hasn't caused any issues so far.

        // Java 8  uses "java -version"  E.g. 1.8.0_301
        // Java 9+ uses "java --version" E.g. 17.0.3.1
        final String ver = System.getProperty("java.version");
        try {
            String parse = ver.toLowerCase(Locale.ENGLISH);
            if (parse.startsWith("1.8")) return 8;
            if (parse.endsWith("-ea")) parse = parse.substring(0, parse.length() - "-ea".length()); // Early Access
            if (!parse.contains(".")) return Integer.parseInt(ver);

            // Watch out for Early Access versions
            // E.g. 18.0.2-ea
            // E.g. 16-ea
            return Integer.parseInt(parse.split("\\.")[0]);
        } catch (Throwable ex) {
            throw new IllegalStateException("Unknown Java version: '" + ver + '\'');
        }
    }

    public static String getVersionString() {
        return System.getProperty("java.version");
    }

    public static boolean supports(int version) {
        return VERSION >= version;
    }

    public static int getVersion() {
        return VERSION;
    }
}
