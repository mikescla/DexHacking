package utils;

import InjectionMgmt.InjectionData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static DexEditing.ReturnType.getReturnTypeFromString;
import static utils.Settings.DATASETS_DIR;
import static utils.Settings.DATA_DIR;

/**
 * Convenience class of static methods for manipulating features according to
 * the R-PackDroid convention.
 * Each feature contains four separators: . @ & |
 * - example: "package.Class@Method&returnType|static" for a
 * static method, "package.Class@Method&returnType|" otherwise
 *
 * @author Michele Scalas
 */
public class FeatureUtils {
    // todo maybe the name RPackFeatureUtils is clearer
    /*R-PACKDROID CONVENTION TO SMALI*/
    public static String getPackageNameFromFeature(String inputString) {

        String[] splitted = inputString.split("(.[A-Z])");
        if (splitted.length != 0) return splitted[0];
        else return inputString;
    }

    public static String getClassNameFromFeature(String inputString) {

        String[] splitted = inputString.split("@");
        String intermediate = splitted[0].replace(".", "/");
        return MessageFormat.format("L{0};", intermediate);
    }

    public static String getMethodNameFromFeature(String inputString) {

        String[] f_splitted = inputString.split("@");
        String[] splitted = f_splitted[1].split("&");
        return splitted[0];
    }

    public static String getReturnTypeFromFeature(String inputString) {

        String[] f_splitted = inputString.split("@");
        String[] s_splitted = f_splitted[1].split("&");
        String t_splitted = s_splitted[1].split("\\|")[0];

        String rawRet = t_splitted.replace("[]", "");

        String ret = getReturnTypeFromString(rawRet);
        // format if it is not primitive
        if (ret == null) {
            String intermediate = rawRet.replace(".", "/");
            ret = MessageFormat.format("L{0};", intermediate);
        }

        // format if it is an array
        if (inputString.contains("[]")) ret = "[" + ret;

        return ret;
    }

    /*R-PACKDROID CONVENTION TO PLAIN NAME*/

    public static String getRawClassNameFromFeature(String inputString) {

        String[] splitted = inputString.split("@");
        return splitted[0];
    }

    public static String getRawReturnTypeFromFeature(String inputString) {

        String[] f_splitted = inputString.split("@");
        String[] splitted = f_splitted[1].split("&");
        return splitted[1];
    }

    /*OTHER METHODS*/

    public static boolean checkIfStaticFromFeature(String inputString) {

        String[] splitted = inputString.split("\\|");

        return splitted.length > 1;
    }

    public static String removeReturnTypeFromFeature(String inputString) {
        return inputString.split("&")[0];
    }

    /**
     * Randomly associates features to add to existent usable methods to be
     * modified
     *
     * @param methodsForInjection
     * @param featuresToAdd
     * @return
     */
    public static List<InjectionData> pairFeatureToMethod(List<List<Integer>> methodsForInjection, Map<String, Integer> featuresToAdd) {

        // create a flat list of repeated elements
        List<String> flatFeatureList = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : featuresToAdd.entrySet())
            flatFeatureList.addAll(Collections.nCopies(entry.getValue(),
                    entry.getKey()));

        int nFeaturesToAdd = flatFeatureList.size();
        int nUsableMethods = methodsForInjection.size();

        //shuffle list
        Collections.shuffle(methodsForInjection, new Random(92));

        // assign each adversarial call to one existent class through a Round
        // Robin
        List<InjectionData> outMap = new ArrayList<>();
        for (int i = 0; i < nFeaturesToAdd; i++) {
            List<Integer> indexes = methodsForInjection.get(i % nUsableMethods);
            outMap.add(new InjectionData(indexes.get(0), indexes.get(1),
                    flatFeatureList.get(i)));
        }

        return outMap;
    }

    /**
     * load the number of features to add to evade the classifier
     *
     * @param featureMix
     * @param apiDetail
     * @param apiSource
     * @param apiLevel
     * @param baseDir
     * @return
     */
    public static Map<String, Map<Integer, Integer>> loadAdversarialData(String featureMix, String apiDetail, String apiSource, String apiLevel, String baseDir) {
        String dataFilename = MessageFormat.format("adv_{0}_{1}_{2}_upto_{3}" +
                ".libsvm", featureMix, apiDetail, apiSource, apiLevel);
        String dataPath = Paths.get(baseDir, DATA_DIR, DATASETS_DIR,
                dataFilename).toString();
        return loadLibsvm(dataPath);

    }

    /**
     * loads R-PackDroid libsvm
     *
     * @param dataPath input libsvm file
     * @return a map of feature indexes and values for each hash (sample)
     */
    public static Map<String, Map<Integer, Integer>> loadLibsvm(String dataPath) {
        Map<String, Map<Integer, Integer>> outMap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(dataPath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" # ");

                String hash = splitted[1];

                String vals = splitted[0];
                String[] valsArray = vals.split(" ");

                List<String> fVals = Arrays.asList(valsArray).subList(1, valsArray.length - 1);

                Map<Integer, Integer> fMap = fVals.stream().map(s -> s.split(":")).collect(Collectors.toMap(a -> Integer.parseInt(a[0]) - 1, a -> Integer.valueOf(a[1])));

                outMap.put(hash, fMap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return outMap;
    }
}
