package org.kingdoms.remapper;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class PackageRelocator {
    // Configuration class to hold mapping of original package to new package and output JAR
    public static class PackageMapping {
        String originalPackage; // e.g., org.kingdoms.libs.snakeyml
        String newPackage;      // e.g., com.snakeyml
        String outputJar;       // e.g., snakeyaml.jar

        PackageMapping(String originalPackage, String newPackage, String outputJar) {
            this.originalPackage = originalPackage.replace('.', '/');
            this.newPackage = newPackage.replace('.', '/');
            this.outputJar = outputJar;
        }
    }

    public static void relocatePackages(File sourceJarFile, List<PackageMapping> mappings) throws IOException {
        // Map to store JarOutputStreams for each output JAR
        Map<String, JarOutputStream> jarOutputs = new HashMap<>();
        Map<String, PackageMapping> packageToMapping = new HashMap<>();

        // Initialize JarOutputStreams for each output JAR
        for (PackageMapping mapping : mappings) {
            JarOutputStream jos = new JarOutputStream(new FileOutputStream(mapping.outputJar));
            jarOutputs.put(mapping.originalPackage, jos);
            packageToMapping.put(mapping.originalPackage, mapping);
        }

        try (JarFile sourceJar = new JarFile(sourceJarFile)) {
            Enumeration<JarEntry> entries = sourceJar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String entryName = entry.getName();

                // Process only .class files
                if (!entryName.endsWith(".class")) {
                    continue;
                }

                // Find which package this class belongs to
                PackageMapping targetMapping = null;
                for (PackageMapping mapping : mappings) {
                    if (entryName.startsWith(mapping.originalPackage + "/")) {
                        targetMapping = mapping;
                        break;
                    }
                }

                if (targetMapping == null) {
                    continue; // Skip classes that don't belong to any specified package
                }

                // Read the class bytes
                try (InputStream inputStream = sourceJar.getInputStream(entry)) {
                    byte[] classBytes = inputStream.readAllBytes();

                    // Create a remapper for package relocation
                    PackageMapping finalTargetMapping = targetMapping;
                    Remapper remapper = new Remapper() {
                        @Override
                        public String map(String typeName) {
                            if (typeName.startsWith(finalTargetMapping.originalPackage + "/")) {
                                return finalTargetMapping.newPackage + typeName.substring(finalTargetMapping.originalPackage.length());
                            }
                            return typeName;
                        }
                    };

                    // Transform the class bytecode
                    ClassReader classReader = new ClassReader(classBytes);
                    ClassWriter classWriter = new ClassWriter(0);
                    ClassRemapper classRemapper = new ClassRemapper(classWriter, remapper);
                    classReader.accept(classRemapper, 0);

                    // Write the transformed class to the appropriate JAR
                    String newClassName = remapper.map(entryName);
                    JarOutputStream jos = jarOutputs.get(targetMapping.originalPackage);
                    jos.putNextEntry(new ZipEntry(newClassName));
                    jos.write(classWriter.toByteArray());
                    jos.closeEntry();
                }
            }
        }

        for (JarOutputStream jos : jarOutputs.values()) {
            jos.close();
        }

        System.out.println("JAR files created successfully.");
    }
}