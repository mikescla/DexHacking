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
 * Main entry point.
 * For each sample in the adversarial feature list, it picks the real apks
 * (locations must be inserted via command line argument) and makes the
 * adversarial sample.
 *
 * @author Michele Scalas
 */
public class HackingSystem {
    // todo LINE CONVENTION: HARD WRAP AT 80 CHARS. SET THE IDE AS EXPLAINED IN
    // https://stackoverflow.com/questions/24376980/how-to-split-long-strings
    // -in-intellij-idea-automatically
    // TODO LIBRARIES: USE MAVEN TO ADD NEEDED LIBRARIES. FOR DEXLIB, USE
    //  MULTIDEXLIB2
    public static void main(String[] args) throws IOException {
        /*LOGGING*/
        Logger logger = LoggerMgmt.getLogger();

        /*ARGUMENT PARSING*/
        // see function for details about args. Specifying -inputd is enough
        CommandLine cmd = parseArgs(args);

        String inputApkDir = cmd.getOptionValue("inputd");
        String featureMix = cmd.getOptionValue("fm", API_ID);
        String apiSource = cmd.getOptionValue("apis", PLATFORM_ID);
        String apiDetail = cmd.getOptionValue("apid", PACKAGE_ID);
        String refApiLevel = cmd.getOptionValue("apil",
                Integer.toString(DEF_API_LEVEL));
        boolean injectMethods = Boolean.parseBoolean(cmd.getOptionValue(
                "injmeths", "false"));

        /*ADVERSARIAL FEATURES LOADING*/
        String parentDir =
                Paths.get(System.getProperty("user.dir")).getParent().toString();

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
        // this can be slow,
        // todo check if it is feasible to do a sort of cache or read a text
        //  file with all the paths
        Map<String, String> availableApks = findFiles(inputApkDir);

        /*INJECTION*/
        // todo add some timing log
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
            // TODO: check what's the best way in Java to check successful
            //  result of a function
            if (decCode != 0) {
                logger.warning("Could not decode APK. Skipped");
                return;
            }

            // get api level of the apk, so that the injected code is
            // selected according to the max supported api level
            // todo switch to aapt or apkanalyzer to find targetsdkversion.
            //  we need the same thing for the adversarial part
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
                logger.warning("Could not find dex file. Skipped.");
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

    // todo use constant values for args' names
    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option apkFile = new Option("inputd", "inputDir", true, "input APKs' "
                + "dir");
        apkFile.setRequired(true);
        options.addOption(apkFile);

        // see settings for supported values
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

        Option apiLevel = new Option("apil", "apiLevel", true, "Api level of "
                + "the classifier under attack");
        apiLevel.setRequired(false);
        options.addOption(apiLevel);

        Option injMeths = new Option("injmeths", "injectmethods", false,
                "whether or not inject methods, constructors" + " only " +
                        "otherwise");
        injMeths.setRequired(false);
        options.addOption(injMeths);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());

            System.exit(1);
        }
        return cmd;
    }

}
