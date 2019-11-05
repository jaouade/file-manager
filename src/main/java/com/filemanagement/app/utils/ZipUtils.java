package com.filemanagement.app.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public final class ZipUtils {

    private ZipUtils() {
    }

    public static void compressDirectory(File sourceDirectory, File zipFile) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            compressDirectory(sourceDirectory.getAbsoluteFile(), sourceDirectory, out);
        }
    }

    private static void compressDirectory(File rootDir, File sourceDir, ZipOutputStream out) throws IOException {
        for (File file : sourceDir.listFiles()) {
            if (file.isDirectory()) {
                compressDirectory(rootDir, new File(sourceDir, file.getName()), out);
            } else {
                String zipEntryName = getRelativeZipEntryName(rootDir, file);
                compressFile(out, file, zipEntryName);
            }
        }
    }

    private static String getRelativeZipEntryName(File rootDir, File file) {
        return StringUtils.removeStart(file.getAbsolutePath(), rootDir.getAbsolutePath());
    }

    public static void compressFile(File file, File zipFile) throws IOException {
        try (ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFile))) {
            compressFile(out, file, file.getName());
        }
    }

    private static void compressFile(ZipOutputStream out, File file, String zipEntityName) throws IOException {
        ZipEntry entry = new ZipEntry(zipEntityName);
        out.putNextEntry(entry);

        try (FileInputStream in = new FileInputStream(file)) {
            IOUtils.copy(in, out);
        }
    }

    public static void extractArchive(File targetDirectory, File zipFile) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            extractStream(targetDirectory, zis);
        }
    }

    private static void extractStream(File targetDirectory, ZipInputStream zis) throws IOException {
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            extractEntry(targetDirectory, zis, zipEntry);
            zipEntry = zis.getNextEntry();
        }
        zis.closeEntry();
    }

    private static void extractEntry(File targetDirectory, ZipInputStream zis, ZipEntry zipEntry) throws IOException {
        File newFile = newFile(targetDirectory, zipEntry);
        if (zipEntry.isDirectory()) {
            FileUtils.forceMkdir(newFile);
        } else {
            new File(newFile.getParent()).mkdirs();
            newFile.createNewFile();
            try (FileOutputStream fos = new FileOutputStream(newFile)) {
                IOUtils.copy(zis, fos);
            }
        }
    }

    private static File newFile(File targetDirectory, ZipEntry zipEntry) throws IOException {
        File targetFile = new File(targetDirectory, zipEntry.getName());

        String targetDirPath = targetDirectory.getCanonicalPath();
        String targetFilePath = targetFile.getCanonicalPath();

        if (!targetFilePath.startsWith(targetDirPath + File.separator)) {
            throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
        }

        return targetFile;
    }
}