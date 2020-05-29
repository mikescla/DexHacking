package utils;

import DexEditing.InjectionData;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;
import static utils.Settings.*;

public class FeatureUtils {

    public static Map<String, List<String>> getAllUsableMethods(String baseDir, String featureMix, String apiSource, String apiLevel) {

        Path featureFileName = Paths.get(baseDir, DATA_DIR, FEATURE_DIR,
                MessageFormat.format("noparams_{0}_{1}_{2}_upto_{3}.txt",
                        featureMix, METHOD_ID, apiSource, apiLevel));
        Map<String, List<String>> allPackages = new HashMap<>();
        try (Stream<String> stream = Files.lines(featureFileName)) {
            allPackages =
                    stream.map(String::toString).collect(groupingBy(FeatureUtils::getPackageNameFromFeature, mapping(a -> a, toList())));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allPackages;
    }

    public static Map<String, String> getUsableMethodsPerPackage(Map<String,
            List<String>> availableMethods) {
        Map<String, String> outMap = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry :
                availableMethods.entrySet())
            outMap.put(entry.getKey(), entry.getValue().get(0));

        return outMap;
    }

    public static boolean isSystemFeature(String packageName) {
        //TODO implement
        return true;
    }

    public static String getPackageNameFromFeature(String inputString) {

        String[] splitted = inputString.split("(.[A-Z])");
        if (splitted.length != 0) return splitted[0];
        else return inputString;
    }

    public static String getClassNameFromFeature(String inputString) {

        String[] splitted = inputString.split("@");
        return MessageFormat.format("L{0};", splitted[0]);
    }

    public static String getRawClassNameFromFeature(String inputString) {

        String[] splitted = inputString.split("@");
        return splitted[0];
    }

    public static String getMethodNameFromFeature(String inputString) {

        String[] f_splitted = inputString.split("@");
        String[] splitted = f_splitted[1].split("&");
        return splitted[0];
    }

    public static String getReturnTypeFromFeature(String inputString) {

        String[] f_splitted = inputString.split("@");
        String[] splitted = f_splitted[1].split("&");
        //        return MessageFormat.format("L{0};", splitted[1]);
        return splitted[1];
    }

    public static Map<String, String> getSuitableSystemMethod(String packageName) {
        // TODO implement
        Map<String, String> empty = new HashMap<>();
        return empty;
    }

    public static List<InjectionData> pairFeatureToMethod(List<List<Integer>> methodsForInjection, Map<String, Integer> featuresToAdd) {

        List<String> flatFeatureList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : featuresToAdd.entrySet())
            flatFeatureList.addAll(Collections.nCopies(entry.getValue(),
                    entry.getKey()));

        int nFeaturesToAdd = flatFeatureList.size();
        int nUsableMethods = methodsForInjection.size();

        //shuffle list
        Collections.shuffle(methodsForInjection, new Random(2222));
        //        Collections.shuffle(methodsForInjection);

        List<InjectionData> outMap = new ArrayList<>();
        for (int i = 0; i < nFeaturesToAdd; i++) {
            List<Integer> indexes = methodsForInjection.get(i % nUsableMethods);
            outMap.add(new InjectionData(indexes.get(0), indexes.get(1),
                    flatFeatureList.get(i)));
        }

        return outMap;
    }
}
