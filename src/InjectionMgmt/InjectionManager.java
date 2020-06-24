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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static InjectionMgmt.InstructionBuilder.*;
import static java.util.stream.Collectors.*;
import static utils.FeatureUtils.*;
import static utils.Settings.*;

/**
 * Manages the injection chain for an apk
 *
 * @author Michele Scalas
 */
public class InjectionManager {
    private static final String baseDir =
            Paths.get(System.getProperty("user" + ".dir")).getParent().toString();
    private static final String outputDir =
            Paths.get(baseDir, OUT_DIR).toString();

    private static final String CLASS_ONLY_PREFIX = "nometh_noparams";
    private static final String CLASS_AND_METHOD_PREFIX = "noparams";

    private final Logger logger;

    private final String featureMix;
    private final String apiSource;
    private final String apiDetail;

    private final List<String> refFeatureNames;
    private final String refApiLevel;

    private final ApkManager apkMng;
    private final DexManager dexMng;

    private final boolean injectMethods;
    private final String injTypePrefix;

    private final Map<String, String> uniqueUsableMethods;

    // TODO: add permission injection if required by API
    // TODO: add injection into overrided classes (onPause, onStart, etc.)
    public InjectionManager(ApkManager apkMng, DexManager dexMng,
                            String featureMix, String apiSource,
                            String apiDetail, String refApiLevel,
                            List<String> refFeatureNames,
                            boolean injectMethods, Logger logger) {
        /*LOGGER*/
        this.logger = logger;
        /*API DETAILS*/
        this.featureMix = featureMix; // <- we are interested to api only
        this.apiSource = apiSource; // <- we focus on system api only
        this.apiDetail = apiDetail; //<- probably only for packages the whole
        // chain is implemented properly at the moment
        /*APK AND DEX MANAGER*/
        this.apkMng = apkMng;
        this.dexMng = dexMng;
        /*FEATURE REFERENCE OF THE SYSTEM UNDER ATTACK*/
        this.refApiLevel = refApiLevel;
        this.refFeatureNames = refFeatureNames;
        /*INJECTION TYPE*/
        this.injectMethods = injectMethods;
        if (this.injectMethods) this.injTypePrefix = CLASS_AND_METHOD_PREFIX;
        else this.injTypePrefix = CLASS_ONLY_PREFIX;

        // GET AVAILABLE METHODS
        this.uniqueUsableMethods = this.getSingleUsableApi();

    }

    /*PRIVATE METHODS*/

    /**
     * Creates the list of instructions needed to call a non-static method
     *
     * @param className
     * @param methodName
     * @param returnClass
     * @return
     */
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

    /**
     * Creates the list of instructions needed to call a constructor
     *
     * @param className
     * @return
     */
    private static List<BuilderInstruction> callConstructor(String className) {
        int regNumber = 0;

        BuilderInstruction21c instr1 =
                InstructionBuilder.NEW_INSTANCE(regNumber, className);
        BuilderInstruction35c instr2 = INVOKE_DIRECT(1, regNumber, 0, 0, 0, 0
                , className, "<init>", Lists.newArrayList(), "V");
        return List.of(instr1, instr2);
    }

    /**
     * Create the instruction needed to call a static method
     *
     * @param className
     * @param methodName
     * @param returnClass
     * @return
     */
    private List<BuilderInstruction> callStaticMethod(String className,
                                                      String methodName,
                                                      String returnClass) {
        List<String> params = new ArrayList<>();

        BuilderInstruction35c instr = INVOKE_STATIC(0, 0, 0, 0, 0, 0,
                className, methodName, params, returnClass);

        return List.of(instr);
    }

    /**
     * Loads the appropriate list of features that are suitable for
     * injection, depending on the injection type and the apk's api level
     *
     * @return a map where each key (package, class or method) contains a list
     * of usable classes and methods, formatted according to the R-PackDroid
     * convention
     */
    private Map<String, List<String>> loadAllUsableApis() {
        // load reference feature list
        Path featureFileName = Paths.get(baseDir, DATA_DIR, FEATURE_DIR,
                MessageFormat.format("{0}_{1}_{2}_{3}_upto_{4}.txt",
                        this.injTypePrefix, this.featureMix, METHOD_ID,
                        apiSource, this.dexMng.getApiLevel()));

        // group by current api granularity
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
            logger.log(Level.SEVERE, e.getMessage(), e);
        }

        return allFeatures;
    }

    /*PUBLIC METHODS*/

    public String getOutApkPath() {
        return Paths.get(outputDir, MessageFormat.format("{0}_{1}_{2}_{3" +
                "}_upto_{4}", this.injTypePrefix, this.featureMix,
                this.apiDetail, this.apiSource, this.refApiLevel),
                "adv_" + apkMng.getApkName()).toString();
    }

    /**
     * For each package, picks a single method among all the usable ones
     *
     * @return a map of one RPack feature for each package
     */
    public Map<String, String> getSingleUsableApi() {
        // todo possibly implement random choice
        Map<String, List<String>> availableMethods = this.loadAllUsableApis();

        Map<String, String> outMap = new HashMap<String, String>();
        for (Map.Entry<String, List<String>> entry :
                availableMethods.entrySet())
            if (entry.getValue().size() == 1) // <- only one class/method
                // available
                outMap.put(entry.getKey(), entry.getValue().get(0));
            else outMap.put(entry.getKey(), entry.getValue().get(1)); //<-
        // arbitrary choice

        return outMap;
    }

    public Map<String, Integer> mapFeatureToCall(Map<String, Integer> inputMap) {
        Map<String, Integer> tmp =
                inputMap.entrySet().stream().filter(s -> this.uniqueUsableMethods.containsKey(s.getKey())).collect(Collectors.toMap(x -> this.uniqueUsableMethods.get(x.getKey()), Map.Entry::getValue));

        // correct the number of features to add when we inject both
        // calls and methods and the api granularity is package or class.
        // In fact, each call in these cases adds the considered feature
        // twice, so we must decrease the original number of features to add.
        // The possible side effect is that, when the original number is odd,
        // we add the feature one time more than necessary.
        if (!this.apiDetail.equals(METHOD_ID) && this.injectMethods)
            return tmp.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, x -> (int) Math.ceil(x.getValue() / 2.0)));
        else return tmp;
    }

    public void writeDexFile() {
        String writePath = Paths.get(this.apkMng.getDecodedApkDir(), dexMng.getDexName()).toString();

        try {
            DexFileFactory.writeDexFile(writePath, new DexFile() {
                @Nonnull
                @Override
                public Set<? extends ClassDef> getClasses() {
                    return new AbstractSet<ClassDef>() {

                        @Override
                        public Iterator<ClassDef> iterator() {
                            return dexMng.getClasses().iterator();
                        }

                        @Override
                        public int size() {
                            return dexMng.getClasses().size();
                        }
                    };
                }

                @Nonnull
                @Override
                public Opcodes getOpcodes() {
                    return dexMng.getOrigDexFile().getOpcodes();
                }
            });
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
    }

    /**
     * Performs the injection chain
     *
     * @param advFeatures the map with the number of additions for each
     *                    feature index
     */
    public void injectFeatures(Map<Integer, Integer> advFeatures) {
        // Map indexes to package names
        Map<String, Integer> advFeatureNames =
                advFeatures.entrySet().stream().collect(Collectors.toMap(s -> refFeatureNames.get(s.getKey()), Map.Entry::getValue));

        // Map package name to call to add
        Map<String, Integer> featuresToAdd =
                this.mapFeatureToCall(advFeatureNames);

        // get APK available methods for injection
        // N.B. we always choose also methods, the choice of injecting only
        // the constructor is made later.
        List<List<Integer>> methodsForInjection =
                dexMng.getMethodsForInjection(false);

        // randomly associate a new call to a method
        List<InjectionData> injectionMap =
                pairFeatureToMethod(methodsForInjection, featuresToAdd);

        // todo check if ok to make parallel (there could be concurrence
        //  problems)
        for (InjectionData entry : injectionMap) {

            int containerClass = entry.getClassIndex();
            int containerMethod = entry.getMethodIndex();
            String currFeature = entry.getFeatureName();

            // reformat names from R-Pack convention (see FeatureUtils) to smali
            String className = getClassNameFromFeature(currFeature);
            String methodName = getMethodNameFromFeature(currFeature);
            String returnClass = getReturnTypeFromFeature(currFeature);
            boolean isStatic = checkIfStaticFromFeature(currFeature);

            List<BuilderInstruction> instructions;
            if (this.injectMethods) {
                if (isStatic)
                    instructions = callStaticMethod(className, methodName,
                            returnClass);
                else
                    instructions = callConstructorAndMethod(className,
                            methodName, returnClass);
            } else {
                instructions = callConstructor(className);
            }

            // add instruction. N.B. we always put code at the end, it seems
            // putting it the beginning causes a lot problems. If there are
            // invocations to super(), maybe the code should be put after that.
            boolean success = dexMng.addInstruction(containerClass,
                    containerMethod, instructions, 1, false);
            if (!success) // TODO change
                System.exit(-1);

        }


    }

    /**
     * Write, build and sign the modified apk
     */
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
