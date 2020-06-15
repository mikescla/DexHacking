package Main;

import DexEditing.DexManager;
import FileMgmt.ApkManager;
import FileMgmt.DexMgmt;
import InjectionMgmt.InjectionManager;
import lanchon.multidexlib2.BasicDexFileNamer;
import lanchon.multidexlib2.MultiDexIO;
import org.apache.commons.cli.*;
import org.jf.dexlib2.iface.DexFile;
import utils.LoggerMgmt;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static utils.FeatureUtils.loadAdversarialData;
import static utils.FileUtils.findFiles;
import static utils.FileUtils.parseTextFile;
import static utils.Settings.*;

/**
 * @author Michele Scalas
 */
public class HackingSystem {

    public static void main(String[] args) throws IOException {
        /*LOGGING*/
        Logger logger = LoggerMgmt.getLogger();

        /*ARGUMENT PARSING*/
        CommandLine cmd = parseArgs(args);

        String parentDir =
                Paths.get(System.getProperty("user.dir")).getParent().toString();

        String inputApkDir = cmd.getOptionValue("inputd");
        String featureMix = cmd.getOptionValue("fm", API_ID);
        String apiSource = cmd.getOptionValue("apis", PLATFORM_ID);
        String apiDetail = cmd.getOptionValue("apid", PACKAGE_ID);
        String refApiLevel = cmd.getOptionValue("apil",
                Integer.toString(DEF_API_LEVEL));
        boolean injectMethods = Boolean.parseBoolean(cmd.getOptionValue(
                "injmeths", "true"));

        /*ADVERSARIAL FEATURES LOADING*/

        // load differential feature vector
        Map<String, Map<Integer, Integer>> advVectors =
                loadAdversarialData(featureMix, apiDetail, apiSource,
                        refApiLevel, parentDir);
        // get attacked classifier's feature reference
        List<String> featureNamesRef = parseTextFile(Paths.get(parentDir,
                DATA_DIR, FEATURE_DIR, MessageFormat.format("{0}_{1}_{2" +
                        "}_upto_{3}.txt", featureMix, apiDetail, apiSource,
                        refApiLevel)).toString());

        /*APK FILES LISTING*/
        Map<String, String> availableApks = findFiles(inputApkDir);

        /*INJECTION*/
        advVectors.entrySet().parallelStream().forEach(apkEntry -> {
            // check file presence
            String advHash = apkEntry.getKey();

            if (!availableApks.containsKey(advHash)) {
                logger.warning(advHash + " not present. Skipped.");
                return;
            }

            Path apk = Paths.get(availableApks.get(advHash));
            ApkManager apkMng = new ApkManager(apk, logger);

            // decode apk
            int decCode = apkMng.decodeApk();
            if (decCode != 0) {
                logger.warning("Could not decode APK. Skipped");
                return;
            }

            // get api level
            DexFile dFile;
            try {
                dFile = MultiDexIO.readDexFile(true,
                        new File(apkMng.getApkPath()),
                        new BasicDexFileNamer(), null, null);
            } catch (IOException ioe) {
                logger.log(Level.SEVERE, ioe.getMessage(), ioe);
                return;
            }
            String apkApiLevel = String.valueOf(dFile.getOpcodes().api);

            logger.info(MessageFormat.format("Modifying {0} (level {1})",
                    advHash, apkApiLevel));

            // search dex file(s)
            DirectoryStream<Path> decodedApkStream;
            try {
                decodedApkStream =
                        java.nio.file.Files.newDirectoryStream(Paths.get(apkMng.getDecodedApkDir()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            // TODO: support multiple dex files
            /*MultiDexContainer<DexBackedDexFile> multiDFile =
                    MultiDexIO.readMultiDexContainer(new File(apkPath),
                            new BasicDexFileNamer(), dFile.getOpcodes());*/
            DexManager dexMng = null;
            for (Path decodedFile : decodedApkStream) {
                if (decodedFile.toString().contains(".dex")) {
                    dexMng =
                            new DexManager(DexMgmt.loadDexFile(decodedFile.toString(), Integer.parseInt(apkApiLevel)), decodedFile.toString());
                    break;
                }
            }

            if (dexMng == null) {
                logger.info("Not an apk file. Skipped.");
                return;
            }

            // inject features
            InjectionManager injMng = new InjectionManager(apkMng, dexMng,
                    featureMix, apiSource, apiDetail, refApiLevel,
                    featureNamesRef, injectMethods, logger);

            Map<Integer, Integer> advFeatures = apkEntry.getValue();
            injMng.injectFeatures(advFeatures);

            // build and sign apk
            injMng.finaliseApk();

        });


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

        Option injMeths = new Option("injmeths", "injectmethods", false,
                "true if inject methods");
        injMeths.setRequired(false);
        options.addOption(injMeths);

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
