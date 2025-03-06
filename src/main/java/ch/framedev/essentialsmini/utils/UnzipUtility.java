package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnzipUtility {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;
    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if it does not exist)
     */
    public void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists() && !destDir.mkdirs()) {
            throw new IOException("Could not create destination directory: " + destDirectory);
        }

        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry = zipIn.getNextEntry();

            while (entry != null) {
                String filePath = destDirectory + File.separator + entry.getName();

                // Skip macOS-specific files and hidden metadata files
                if (entry.getName().startsWith("__MACOSX") || entry.getName().contains("._")) {
                    Main.getInstance().getLogger4J().info("Skipping file: " + entry.getName());
                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                    continue;
                }

                if (entry.isDirectory()) {
                    // Create the directory
                    File dir = new File(filePath);
                    if (!dir.exists() && !dir.mkdirs()) {
                        throw new IOException("Could not create directory: " + filePath);
                    }
                } else {
                    // Extract the file
                    extractFile(zipIn, filePath);
                }

                zipIn.closeEntry();
                entry = zipIn.getNextEntry();
            }
        }
    }

    private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
}