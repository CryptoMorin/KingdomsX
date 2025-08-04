package org.kingdoms.remapper;

import org.kingdoms.dependencies.relocation.JarRelocator;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KingdomsRemapper {
    private static final Map<String, String> PACKAGE_MAPPING = new LinkedHashMap<>();

    static {
        String libs = "org.kingdoms.libs.";
        PACKAGE_MAPPING.put(libs + "xseries", "com.cryptomorin.xseries");
        PACKAGE_MAPPING.put(libs + "asm", "org.objectweb.asm");
        PACKAGE_MAPPING.put(libs + "kotlin", "kotlin");
        PACKAGE_MAPPING.put(libs + "mariadb", "org.mariadb.jdbc");
        PACKAGE_MAPPING.put(libs + "mysql", "com.mysql");
        PACKAGE_MAPPING.put(libs + "postgresql", "org.postgresql");
        PACKAGE_MAPPING.put(libs + "mongodb", "com.mongodb");
        PACKAGE_MAPPING.put(libs + "bson", "org.bson");
        PACKAGE_MAPPING.put(libs + "hikari", "com.zaxxer.hikari");
        PACKAGE_MAPPING.put(libs + "caffeine", "com.github.benmanes.caffeine");
        PACKAGE_MAPPING.put(libs + "xseries", "com.cryptomorin.xseries");
        PACKAGE_MAPPING.put(libs + "checkerframework", "org.checkerframework");
        PACKAGE_MAPPING.put(libs + "jetbrains", "org.jetbrains");
        PACKAGE_MAPPING.put(libs + "intellij", "org.intellij");
        PACKAGE_MAPPING.put(libs + "snakeyaml", "org.snakeyaml");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Usage: java KingdomsRemapper <input-jar> <output-jar>");
            System.exit(1);
        }

        File inputJar = new File(args[0]);
        File outputJar = new File(args[1]);

        // The jar relocator already relocates the packages to the root level.
        // So it's pointless to extract them as separate jars.
        new JarRelocator(inputJar, outputJar, PACKAGE_MAPPING).run();
        // List<PackageRelocator.PackageMapping> mappings = Arrays.asList(
        //         new PackageRelocator.PackageMapping("org.kingdoms.libs.snakeyml", "com.snakeyml", "snakeyaml.jar"),
        //         new PackageRelocator.PackageMapping("org.kingdoms.libs.asm", "org.asm.ow2", "asm.jar")
        // );
        // PackageRelocator.relocatePackages(outputJar, mappings);
    }
}
