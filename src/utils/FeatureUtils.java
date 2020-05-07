package utils;

import java.util.HashMap;
import java.util.Map;

public class FeatureUtils {
    public static boolean isSystemFeature(String packageName) {
        //TODO implement
        return true;
    }

    public static String getPackageNameFromFeature(String inputString) {

        String[] splitted = inputString.split("(.[A-Z])");
        if (splitted.length != 0) return splitted[0];
        else return inputString;
    }

    public static Map<String, String> getSuitableSystemMethod(String packageName) {
        // TODO implement
        Map<String, String> empty = new HashMap<>();
        return empty;
    }
}
