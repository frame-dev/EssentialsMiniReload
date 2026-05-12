package ch.framedev.essentialsmini.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class TabCompleteUtils {

    private TabCompleteUtils() {
    }

    public static List<String> matchingStrings(Collection<String> values, String prefix) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<String> matches = new ArrayList<>();
        for (String value : values) {
            if (value != null && value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                matches.add(value);
            }
        }
        Collections.sort(matches);
        return matches;
    }

    public static List<String> matchingOnlinePlayers(String prefix) {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            names.add(player.getName());
        }
        return matchingStrings(names, prefix);
    }
}
