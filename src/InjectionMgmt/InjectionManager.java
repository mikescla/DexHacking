package InjectionMgmt;

import DexEditing.DexManager;
import FileMgmt.ApkManager;
import com.google.common.collect.Lists;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import utils.FeatureUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static InjectionMgmt.InstructionBuilder.INVOKE_DIRECT;
import static InjectionMgmt.InstructionBuilder.INVOKE_VIRTUAL;
import static java.util.stream.Collectors.*;
import static utils.FeatureUtils.*;
import static utils.Settings.*;

public class InjectionManager {
    private static final String baseDir =
            Paths.get(System.getProperty("user" + ".dir")).getParent().toString();
    private static final String outputDir =
            Paths.get(baseDir, OUT_DIR).toString();

    private final Logger logger;

    private final String featureMix;
    private final String apiSource;
    private final String apiDetail;

    private final List<String> refFeatureNames;
    private final String refApiLevel;

    private final ApkManager apkMng;
    private final DexManager dexMng;

    private final Map<String, String> uniqueUsableMethods;

    public InjectionManager(ApkManager apkMng, DexManager dexMng,
                            String featureMix, String apiSource,
                            String apiDetail, String refApiLevel,
                            List<String> refFeatureNames, Logger logger) {

        this.logger = logger;

        this.featureMix = featureMix;
        this.apiSource = apiSource;
        this.apiDetail = apiDetail;

        this.apkMng = apkMng;
        this.dexMng = dexMng;

        this.refApiLevel = refApiLevel;
        this.refFeatureNames = refFeatureNames;

        // GET AVAILABLE METHODS
        this.uniqueUsableMethods = this.getSingleUsableApi();


    }

    private static List<BuilderInstruction> callConstructorAndMethod(String className, String methodName, String returnClass) {
        List<String> params = new ArrayList<>();
        int regNumber = 0;

        BuilderInstruction21c instr1 =
                InstructionBuilder.NEW_INSTANCE(regNumber, className);
        BuilderInstruction35c instr2 = INVOKE_DIRECT(1, regNumber, 0, 0, 0, 0
                , className, "<init>", Lists.newArrayList(), "V");
        BuilderInstruction35c instr3 = INVOKE_VIRTUAL(1, regNumber, 0, 0, 0,
                0, className, methodName, params, returnClass);
        return List.of(instr1, instr2, instr3);
    }

    public String getOutApkPath() {
        return Paths.get(outputDir, MessageFormat.format("noparams_{0}_{1}_{2" +
                "}_upto_{3}", this.featureMix, this.apiDetail, this.apiSource
                , this.refApiLevel), "adv_" + apkMng.getApkName()).toString();
    }

    private Map<String, List<String>> getAllUsableApiPerDetail() {

        Path featureFileName = Paths.get(baseDir, DATA_DIR, FEATURE_DIR,
                MessageFormat.format("noparams_{0}_{1}_{2}_upto_{3}.txt",
                        this.featureMix, METHOD_ID, apiSource,
                        this.dexMng.getApiLevel()));
        Map<String, List<String>> allFeatures = new HashMap<>();
        try (Stream<String> stream = Files.lines(featureFileName)) {
            switch (apiDetail) {
                case PACKAGE_ID:
                    allFeatures =
                            stream.map(String::toString).collect(groupingBy(FeatureUtils::getPackageNameFromFeature, mapping(a -> a, toList())));
                    break;
                case CLASS_ID:
                    allFeatures =
                            stream.map(String::toString).collect(groupingBy(a -> MessageFormat.format("{0}.{1}", getPackageNameFromFeature(a), getRawClassNameFromFeature(a)), mapping(a -> a, toList())));
                    break;
                default:
                    allFeatures =
                            stream.map(String::toString).collect(groupingBy(FeatureUtils::removeReturnTypeFromFeature, mapping(a -> a, toList())));
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return allFeatures;
    }

    public Map<String, String> getSingleUsableApi() {

        Map<String, List<String>> availableMethods =
                this.getAllUsableApiPerDetail();

        Map<String, String> outMap = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry :
                availableMethods.entrySet())
            outMap.put(entry.getKey(), entry.getValue().get(0));

        return outMap;
    }

    public Map<String, Integer> mapFeatureToCall(Map<String, Integer> inputMap) {
        Map<String, Integer> tmp =
                inputMap.entrySet().stream().filter(s -> this.uniqueUsableMethods.containsKey(s.getKey())).collect(Collectors.toMap(x -> this.uniqueUsableMethods.get(x.getKey()), Map.Entry::getValue));

        // possibly correct the number of features to add
        if (!this.apiDetail.equals(METHOD_ID))
            return tmp.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> (int) Math.ceil(x.getValue() / 2.0)));
        else return tmp;
    }

    public void writeDexFile() {
        String writePath = Paths.get(this.apkMng.getDecodedApkDir(),
                dexMng.getDexName()).toString();

        try {
            DexFileFactory.writeDexFile(writePath, new DexFile() {
                @Nonnull
                @Override
                public Set<? extends ClassDef> getClasses() {
                    return new AbstractSet<ClassDef>() {

                        @Override
                        public Iterator<ClassDef> iterator() {
                            return dexMng.getDexClasses().iterator();
                        }

                        @Override
                        public int size() {
                            return dexMng.getDexClasses().size();
                        }
                    };
                }

                @Nonnull
                @Override
                public Opcodes getOpcodes() {
                    return dexMng.getDexFile().getOpcodes();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void injectFeatures(Map<Integer, Integer> advFeatures) {
        // - map to package names
        Map<String, Integer> advFeatureNames =
                advFeatures.entrySet().stream().collect(Collectors.toMap(s -> refFeatureNames.get(s.getKey()), Map.Entry::getValue));

        // - map package name to call to add
        Map<String, Integer> featuresToAdd =
                this.mapFeatureToCall(advFeatureNames);

        // - get APK available methods for injection
        List<List<Integer>> methodsForInjection =
                dexMng.getMethodsForInjection(false);

        // - randomly associate a new call to a method
        List<InjectionData> injectionMap =
                pairFeatureToMethod(methodsForInjection, featuresToAdd);

        for (InjectionData entry : injectionMap) {

            int containerClass = entry.getClassIndex();
            int containerMethod = entry.getMethodIndex();
            String currFeature = entry.getFeatureName();

            String className = getClassNameFromFeature(currFeature);
            String methodName = getMethodNameFromFeature(currFeature);
            String returnClass = getReturnTypeFromFeature(currFeature);

            List<BuilderInstruction> instructions =
                    callConstructorAndMethod(className, methodName,
                            returnClass);

            boolean success = dexMng.addInstruction(containerClass,
                    containerMethod, instructions, 1, false);
            if (!success) // TODO change
                System.exit(-1);


        }


    }

    public void finaliseApk() {
        //write the modified dex file
        this.writeDexFile();

        //rebuilds the decoded apk into an apk file
        apkMng.buildApk(apkMng.getDecodedApkDir(), dexMng.getApiLevel(),
                this.getOutApkPath());

        //signs the apk
        apkMng.signApk(this.getOutApkPath());

        logger.info("Apk " + apkMng.getApkName() + " successfully edited!");
    }
}
