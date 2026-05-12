package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.utils
 * ClassName UpdateChecker
 * Date: 04.04.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class UpdateChecker {

    private static final String VERSION_URL = "https://framedev.ch/others/versions/essentialsmini-versions.json";
    private static final String LATEST_VERSION_KEY = "latest";
    private static final String PRE_RELEASE_VERSION_KEY = "1.20.6-higher-release";
    private static final int CONNECT_TIMEOUT_MS = 5000;
    private static final int READ_TIMEOUT_MS = 5000;
    private static final int BUFFER_SIZE = 4096;

    public void download(String fileUrl, String fileName, String name) {
        File targetFile = new File(fileName, name);
        File parent = targetFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            logError("Updater couldn't create the necessary directory: " + parent.getAbsolutePath(), null);
            return;
        }

        try {
            URLConnection connection = openConnection(fileUrl);
            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(targetFile))) {
                byte[] data = new byte[BUFFER_SIZE];
                int count;
                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                }
            }
        } catch (Exception e) {
            logError("Updater tried to download the update, but was unsuccessful.", e);
        }
    }

    @SuppressWarnings("unused")
    public boolean hasUpdate() {
        if (isOldVersionPreRelease()) {
            return false;
        }

        String latestVersion = getVersionValue(LATEST_VERSION_KEY);
        return isDifferentVersion(latestVersion);
    }

    public boolean isOldVersionPreRelease() {
        String oldVersion = Main.getInstance().getDescription().getVersion();
        String normalizedVersion = oldVersion.toLowerCase();
        return normalizedVersion.contains("pre-release") || normalizedVersion.contains("1.20.6-higher-release");
    }

    public boolean hasPreReleaseUpdate() {
        if (!isOldVersionPreRelease()) {
            return false;
        }

        String latestPreRelease = getLatestPreRelease();
        return isDifferentVersion(latestPreRelease);
    }

    public String getLatestPreRelease() {
        return getVersionValue(PRE_RELEASE_VERSION_KEY);
    }

    private String getVersionValue(String key) {
        try {
            JsonObject versions = fetchVersions();
            if (!versions.has(key) || versions.get(key).isJsonNull()) {
                logError("Version key '" + key + "' was not found in update metadata.", null);
                return "";
            }
            return versions.get(key).getAsString();
        } catch (Exception e) {
            logError("Failed to check EssentialsMini updates.", e);
        }
        return "";
    }

    private JsonObject fetchVersions() throws IOException {
        URLConnection connection = openConnection(VERSION_URL);
        try (Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private URLConnection openConnection(String url) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
        connection.setReadTimeout(READ_TIMEOUT_MS);
        return connection;
    }

    private boolean isDifferentVersion(String version) {
        if (version == null || version.isBlank()) {
            return false;
        }
        return !version.equalsIgnoreCase(Main.getInstance().getDescription().getVersion());
    }

    private void logError(String message, Throwable throwable) {
        Main instance = Main.getInstance();
        if (instance != null && instance.getLogger4J() != null) {
            if (throwable == null) {
                instance.getLogger4J().error(message);
            } else {
                instance.getLogger4J().error(message, throwable);
            }
            return;
        }

        System.err.println(message);
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }
}
