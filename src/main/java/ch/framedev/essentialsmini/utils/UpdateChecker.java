package ch.framedev.essentialsmini.utils;

import ch.framedev.essentialsmini.main.Main;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * This Plugin was Created by FrameDev
 * Package : de.framedev.essentialsmini.utils
 * ClassName UpdateChecker
 * Date: 04.04.21
 * Project: EssentialsMini
 * Copyrighted by FrameDev
 */

public class UpdateChecker {

    public void download(String fileUrl, String fileName, String name) {
        File file = new File(fileName, name);
        if (!file.exists())
            if (!file.getParentFile().mkdirs())
                System.out.println("Updater couldn't create the necessary directory.");
        try {
            BufferedInputStream in = null;
            FileOutputStream fout = null;
            try {
                URL url = new URL(fileUrl);
                in = new BufferedInputStream(url.openStream());
                fout = new FileOutputStream(new File(fileName, name));
                final byte[] data = new byte[4096];
                int count;
                while ((count = in.read(data, 0, 4096)) != -1) {
                    fout.write(data, 0, count);
                }
            } catch (Exception e) {
                System.out.println("Updater tried to download the update, but was unsuccessful.");
                Main.getInstance().getLogger4J().error(e.getMessage(), e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (final IOException e) {
                    Main.getInstance().getLogger4J().error(e.getMessage(), e);
                }
                try {
                    if (fout != null) {
                        fout.close();
                    }
                } catch (final IOException e) {
                    Main.getInstance().getLogger4J().error(e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            Main.getInstance().getLogger4J().error(e.getMessage(), e);
        }
    }

    @SuppressWarnings("unused")
    public boolean hasUpdate() {
        try {
            URLConnection conn = new URL("https://framedev.ch/others/versions/essentialsmini-versions.json").openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String oldVersion = Main.getInstance().getDescription().getVersion();
            String newVersion = JsonParser.parseReader(br).getAsJsonObject().get("latest").getAsString();
            if (!newVersion.equalsIgnoreCase(oldVersion))
                if (!oldVersion.contains("PRE-RELEASE") || !oldVersion.contains("1.20.6-HIGHER-RELEASE"))
                    return true;
        } catch (IOException ignored) {
        }
        return false;
    }

    public boolean isOldVersionPreRelease() {
        String oldVersion = Main.getInstance().getDescription().getVersion();
        return oldVersion.contains("PRE-RELEASE") || oldVersion.contains("1.20.6-HIGHER-RELEASE");
    }

    public boolean hasPreReleaseUpdate() {
        if (isOldVersionPreRelease()) {
            try {
                URLConnection conn = new URL("https://framedev.ch/others/versions/essentialsmini-versions.json").openConnection();
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String oldVersion = Main.getInstance().getDescription().getVersion();
                String newVersion = JsonParser.parseReader(br).getAsJsonObject().get("1.20.6-higher-release").getAsString();
                if (!newVersion.equalsIgnoreCase(oldVersion))
                    return true;
            } catch (IOException ignored) {
            }
        }
        return false;
    }

    public String getLatestPreRelease() {
        try {
            URLConnection conn = new URL("https://framedev.ch/others/versions/essentialsmini-versions.json").openConnection();
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            return JsonParser.parseReader(br).getAsJsonObject().get("1.20.6-higher-release").getAsString();
        } catch (IOException ignored) {
        }
        return "";
    }
}
