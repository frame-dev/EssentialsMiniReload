package ch.framedev.essentialsmini.utils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This Plugin was Created by FrameDev
 * Package : ch.framedev.simplejavautils
 * ClassName TextUtils
 * Date: 03.05.21
 * Project: JavaUtils
 * Copyrighted by FrameDev
 */

public class TextUtils {

    public String replaceAndWithParagraph(String text) {
        if(text == null) return null;
        if (text.contains("&"))
            text = text.replace('&', '§');
        return text;
    }

    public String replaceObject(String text, String format, Object data) {
        if(text == null) return null;
        if (text.contains(format)) {
            text = text.replace(format, String.valueOf(data));
        }
        return text;
    }
    
    public <T> List<T> arrayToList(T[] array) {
        return new ArrayList<>(Arrays.asList(array));
    }

    public String[] stringListToArray(List<String> list) {
        return list.toArray(new String[0]);
    }

    public Integer[] intListToArray(List<Integer> list) {
        return list.toArray(new Integer[0]);
    }

    public Double[] doubleListToArray(List<Double> list) {
        return list.toArray(new Double[0]);
    }

    public Float[] floatListToArray(List<Float> list) {
        return list.toArray(new Float[0]);
    }

    public Short[] shortListToArray(List<Short> list) {
        return list.toArray(new Short[0]);
    }

    public Long[] longListToArray(List<Long> list) {
        return list.toArray(new Long[0]);
    }

    public Byte[] byteListToArray(List<Byte> list) {
        return list.toArray(new Byte[0]);
    }

    public Object[] objectListToArray(List<Object> list) {
        return list.toArray(new Object[0]);
    }


    /**
     * Converts a list to an array.
     * Suppressed (unchecked)
     * @param list the list to be converted
     * @param <T>  the type of elements in the list
     * @return an array containing the elements of the list
     */
    public <T> T[] listToArray(List<T> list, Class<T> clazz) {
        @SuppressWarnings("unchecked")
        T[] array = (T[]) Array.newInstance(clazz, list.size());
        return list.toArray(array);
    }

    public int doubleToInt(double d) {
        return (int) d;
    }

    public int longToInt(long number) {
        return (int) number;
    }

    public  int floatToInt(float number) {
        return (int) number;
    }

    public String centerTextWithSymbol(String text, char symbol, int length) {
        int half = length/2;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < half; i++) {
            sb.append(symbol);
        }
        sb.append(text);
        for (int i = 0; i < half; i++) {
            sb.append(symbol);
        }
        return sb.toString();
    }

    public void printBox(String... text) {
        ArrayList<String> lines = new ArrayList<>();
        int lastLength = 0;
        for (String t : text) {
            if (lastLength <= t.length())
                lastLength = t.length();
            lines.add(t);
        }

        topBottomVoid(lastLength);
        for (String s : lines) {
            int spaces = lastLength - s.length();
            if (!s.contains("[") || !s.contains("]")) {
                System.out.print("| " + s);
            } else {
                String updated = s;
                int updatedSpaces = spaces / 2;
                for (int i = 0; i < updatedSpaces; i++)
                    updated = " " + updated;
                System.out.print("| " + updated);
                spaces = updatedSpaces;
            }
            while (spaces-- > 0)
                System.out.print(" ");
            System.out.println(" |");
        }
        topBottomVoid(lastLength);
    }

    public String topBottom(int length) {
        StringBuilder lineBuilder = new StringBuilder();
        lineBuilder.append("+");
        for (int i = 0; i < length + 2; i++) // Adjusted loop condition
            lineBuilder.append("-");
        lineBuilder.append("+\n");
        return lineBuilder.toString();
    }

    public void topBottomVoid(int length) {
        System.out.print("+");
        for (int i = 0; i <= length + 1; i++)
            System.out.print("-");
        System.out.println("+");
    }

    public String generateBox(String... text) {
        ArrayList<String> lines = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        int lastLength = 0;

        // Find the length of the longest line
        for (String t : text) {
            if (lastLength < t.length()) // Adjusted condition to ensure lastLength holds the length of the longest line
                lastLength = t.length();
            lines.add(t);
        }

        // Create top border
        sb.append(topBottom(lastLength));

        // Add text lines
        for (String s : lines) {
            int spaces = lastLength - s.length();
            if (!s.contains("[") || !s.contains("]")) {
                sb.append("| ").append(s);
                int addedSpaces = 0;
                while (spaces-- > 0) {
                    sb.append(" ");
                    addedSpaces++;
                }
            } else {
                String updated = s;
                int updatedSpaces = spaces / 2;
                for (int i = 0; i < updatedSpaces; i++) {
                    updated = " " + updated;
                }
                sb.append("| ").append(updated);
                spaces = lastLength - s.length() - updatedSpaces;
                while (spaces-- > 0) {
                    sb.append(" ");
                }
            }
            sb.append(" |\n");
        }

        // Create bottom border
        sb.append(topBottom(lastLength));

        return sb.toString();
    }

    public double milesToMeters(double miles) {
        return miles * 1609.344;
    }

    public double metersToMiles(double meters) {
        return meters / 1609.344;
    }
}
