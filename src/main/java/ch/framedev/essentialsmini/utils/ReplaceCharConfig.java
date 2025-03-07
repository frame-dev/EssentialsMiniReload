package ch.framedev.essentialsmini.utils;


public class ReplaceCharConfig {

    public static String replaceParagraph(String text) {
        if (text == null) return text;
        if (text.contains("&"))
            text = text.replace('&', '§');
        return text;
    }

    public static Boolean convertStringToBoolean(String text) {
        return Boolean.parseBoolean(text);
    }

    public static Double convertIntegerToDouble(int number) {
        return (double) number;
    }

    public static Double convertFloatToDouble(float number) {
        return (double) number;
    }

    public static Double convertLongToDouble(long number) {
        return (double) number;
    }

    public static Double convertShortToDouble(short number) {
        return (double) number;
    }

    public static Integer convertDoubleToInteger(double number) {
        return (int) number;
    }

    public static Integer convertFloatToInteger(float number) {
        return (int) number;
    }

    public static Integer convertLongToInteger(long number) {
        return (int) number;
    }

    public static Integer convertByteToInteger(byte number) {
        return (int) number;
    }

    public static Integer convertShortToInteger(short number) {
        return (int) number;
    }

    public static Integer convertStringToInteger(String text) {
        return Integer.parseInt(text);
    }


    public static Double convertStringToDouble(String text) {
        return Double.parseDouble(text);
    }

    public static String replaceObjectWithData(String text, String object, String data) {
        if (text.contains(object))
            text = text.replace(object, data);
        return text;
    }

    public static Integer convertObjectToInteger(Object object) {
        return Integer.parseInt(object.toString());
    }

    public static String convertObjectToString(Object object) {
        return String.valueOf(object.toString());
    }

    public static Double convertObjectToDouble(Object object) {
        return Double.parseDouble(object.toString());
    }

    public static Float convertObjectToFloat(Object object) {
        return Float.parseFloat(object.toString());
    }
}


