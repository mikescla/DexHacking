package Main;

import DexEditing.Dex;
import DexEditing.InjectionData;
import FileMgmt.ApkManager;
import FileMgmt.DexMgmt;
import FileMgmt.LoggerMgmt;
import InstructionMgmt.InstructionBuilder;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import org.apache.commons.cli.*;
import org.jf.dexlib2.builder.BuilderInstruction;
import org.jf.dexlib2.builder.instruction.BuilderInstruction21c;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static InstructionMgmt.InstructionBuilder.INVOKE_DIRECT;
import static InstructionMgmt.InstructionBuilder.INVOKE_VIRTUAL;
import static utils.FeatureUtils.*;
import static utils.IOUtils.getSHA256Hash;
import static utils.Settings.*;

/**
 * @author fabri
 */
public class HackingSystem {

    public static void main(String[] args) throws IOException {
        // logging
        Logger logger = LoggerMgmt.getLogger();

        // argument parsing
        CommandLine cmd = parseArgs(args);

        String parentDir =
                Paths.get(System.getProperty("user.dir")).getParent().toString();
        String resourceDir = cmd.getOptionValue("res", Paths.get(parentDir,
                RES_DIR).toString());
        String decodedApkDir = Paths.get(parentDir, TEMP_DIR).toString();
        String outputDir = Paths.get(parentDir, OUT_DIR).toString();

        String inputApkDir = cmd.getOptionValue("inputd");
        String featureMix = cmd.getOptionValue("fm", API_ID);
        String apiSource = cmd.getOptionValue("apis", PLATFORM_ID);
        String apiDetail = cmd.getOptionValue("apid", PACKAGE_ID);
        String apiLevel = cmd.getOptionValue("apil",
                Integer.toString(DEF_API_LEVEL));

        // feature data
        Map<String, List<String>> availableMethods =
                getAllUsableMethods(parentDir, featureMix, apiSource, apiLevel);
        Map<String, String> uniqueUsableMethods =
                getUsableMethodsPerPackage(availableMethods);

        // APKs
        DirectoryStream<Path> inputStream =
                java.nio.file.Files.newDirectoryStream(Paths.get(inputApkDir));

        for (Path apk : inputStream) {
            ApkManager apkMng = new ApkManager();
            Dex dexFile = null;
            boolean success = false;

            String baseApkName = Files.getNameWithoutExtension(apk.toString());
            String apkName = apk.getFileName().toString();
            String apkPath = apk.toString();
            String apkHash = getSHA256Hash(apkPath);

            String decodedApkPath =
                    Paths.get(decodedApkDir, baseApkName).toString();
            apkMng.decodeApk(apkPath, decodedApkPath);

            DirectoryStream<Path> decodedApkStream =
                    java.nio.file.Files.newDirectoryStream(Paths.get(decodedApkPath));
            dexFile = null;
            String dexPath = null;
            // TODO: implement support multiple dex files
            for (Path decodedFile : decodedApkStream) {
                if (decodedFile.toString().contains(".dex")) {
                    dexFile =
                            new Dex(DexMgmt.loadDexFile(decodedFile.toString(), DEF_API_LEVEL));
                    dexPath = decodedFile.toString();
                    break;
                }
            }

            // adversarial features loading
            // TODO implement:
            // - load differential feature vector

            // - map to package names
            //            String packageName = getPackageNameFromFeature
            //            (currFeature);

            //                    Map<String, String> instrInfoMap =
            //                            getSuitableSystemMethod
            //                            (packageName);
            //                    String className = instrInfoMap.get
            //                    ("class");
            //                    String methodName = instrInfoMap
            //                    .get("method");

            // - map package name to call to add
            Map<String, Integer> featuresToAdd = new HashMap<>();
            featuresToAdd.put("android/net/wifi/p2p/WifiP2pDevice@hashCode" + "&I", 300);
            featuresToAdd.put("android/animation/AnimatorSet@clone&Landroid" + "/animation/AnimatorSet;", 300);

            // - get APK available methods for injection
            List<List<Integer>> methodsForInjection =
                    dexFile.getMethodsForInjection(false);

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

                //                List<BuilderInstruction> implOrig =
                //                        callToVoidMethod(className,
                //                        methodName);
                success = dexFile.addInstruction(containerClass,
                        containerMethod, instructions, 1, false);
                if (!success) // TODO change
                    System.exit(-1);


            }

            String dexName = Paths.get(dexPath).getFileName().toString();
            String writePath = Paths.get(decodedApkPath, dexName).toString();
            DexMgmt.writeDexFile(writePath, dexFile.getDexClasses(),
                    dexFile.getDexFile());
            //write the modified dex file
            String outApkPath =
                    Paths.get(outputDir, "adv_" + apkName).toString();
            apkMng.buildApk(decodedApkPath, outApkPath);
            //rebuilds the decoded apk into an apk file
            apkMng.signApk(outApkPath);
            //signs the apk
            logger.info("Apk " + apkName + " successfully edited!");

        }


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

    public static List<BuilderInstruction> callToVoidMethod(String classToCall, String methodToCall) {
        List<BuilderInstruction> methodImplementation = new ArrayList<>();

        if (classToCall.equals(methodToCall)) {
            methodImplementation.add(InstructionBuilder.NEW_INSTANCE(0,
                    classToCall));
            methodImplementation.add(InstructionBuilder.INVOKE_DIRECT(1, 0, 0
                    , 0, 0, 0, classToCall, methodToCall,
                    Lists.newArrayList(), "V"));
        } else {
            methodImplementation.add(InstructionBuilder.NEW_INSTANCE(0,
                    classToCall));
            methodImplementation.add(INVOKE_VIRTUAL(1, 0, 0, 0, 0, 0,
                    classToCall, methodToCall, Lists.newArrayList(), "V"));
        }

        return methodImplementation;
    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option apkFile = new Option("inputd", "inputDir", true, "input APKs' "
                + "dir");
        apkFile.setRequired(true);
        options.addOption(apkFile);

        Option resourceDir = new Option("res", "resDir", true, "resource " +
                "directory path");
        resourceDir.setRequired(false);
        options.addOption(resourceDir);

        Option featureMix = new Option("fm", "featureMix", true, "Feature mix");
        featureMix.setRequired(false);
        options.addOption(featureMix);

        Option apiSource = new Option("apis", "apiSource", true, "Api source");
        apiSource.setRequired(false);
        options.addOption(apiSource);

        Option apiDetail = new Option("apid", "apiDetail", true, "Api detail "
                + "level");
        apiDetail.setRequired(false);
        options.addOption(apiDetail);

        Option apiLevel = new Option("apil", "apiLevel", true, "Api level");
        apiLevel.setRequired(false);
        options.addOption(apiLevel);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("-apk APK_FILE_NAME", options);

            System.exit(1);
        }
        return cmd;
    }

}
