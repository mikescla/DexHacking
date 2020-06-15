/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileMgmt;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.logging.Logger;

import static utils.Settings.*;

/**
 * @author fabri
 */
public class ApkManager {
    private final String baseDir =
            Paths.get(System.getProperty("user.dir")).toString();

    private final String signKeyPath =
            Paths.get(baseDir, SIGN_FILEPATH).toString();
    private final String apkToolPath =
            Paths.get(baseDir, APKTOOL_FILEPATH).toString();

    private final String decodingDir =
            Paths.get(Paths.get(baseDir).getParent().toString(), TEMP_DIR).toString();


    private final Logger logger;

    private final String apkName;
    private final String apkPath;
    private final String decodedApkDir;

    public ApkManager(Path apkPath, Logger logger) {

        this.logger = logger;

        this.apkName = apkPath.getFileName().toString();
        this.apkPath = apkPath.toString();

        this.decodedApkDir = Paths.get(this.decodingDir,
                this.getBaseApkName()).toString();

    }

    public void signApk(String apkPath) {
        try {
            Process cmd =
                    Runtime.getRuntime().exec("jarsigner -storepass " +
                            "keykey -sigalg SHA1withRSA -digestalg SHA1 " +
                            "-keystore \"" + this.signKeyPath + "\" \"" + apkPath + "\" Key");
            int exitCode = cmd.waitFor();
            if (exitCode != 0)
                logger.severe(IOUtils.toString(cmd.getErrorStream(),
                        StandardCharsets.UTF_8));

            cmd.destroy();
        } catch (IOException ex) {
            logger.severe("Error! File not Found");
        } catch (InterruptedException ex) {
            logger.severe("Error! Execution interrupted!");
        }
    }

    public int decodeApk() {
        try {
            Process cmd =
                    Runtime.getRuntime().exec("java -jar \"" + this.apkToolPath + "\" d -r -s -f -o \"" + this.decodedApkDir + "\" \"" + this.apkPath + "\"");

            int exitCode = cmd.waitFor();
            cmd.destroy();
            if (exitCode != 0) {
                logger.severe(IOUtils.toString(cmd.getErrorStream(),
                        StandardCharsets.UTF_8));
                return exitCode;
            } else return 0;
        } catch (IOException ex) {
            logger.severe("Error! File not Found");
        } catch (InterruptedException ex) {
            logger.severe("Error! Execution interrupted!");
        }
        return 0;
    }

    public void buildApk(String apkPath, int apkApiLevel, String outputPath) {
        try {
            Process cmd = Runtime.getRuntime().exec(MessageFormat.format(
                    "java -jar \"{0}\" b --force-all --debug -api {3}" + " " + "-o" + " " + "\"{1}\" \"{2}\" ", this.apkToolPath, outputPath, apkPath, apkApiLevel));

            int exitCode = cmd.waitFor();
            if (exitCode != 0)
                logger.severe(IOUtils.toString(cmd.getErrorStream(),
                        StandardCharsets.UTF_8));

            cmd.destroy();

            FileUtils.deleteDirectory(new File(apkPath));
        } catch (IOException ex) {
            logger.severe("Error! File not Found");
        } catch (InterruptedException ex) {
            logger.severe("Error! Execution interrupted!");
        }
    }

    // GETTERS

    public String getBaseApkName() {
        return Files.getNameWithoutExtension(this.apkPath);
    }

    public String getApkName() {
        return apkName;
    }

    public String getDecodedApkDir() {
        return decodedApkDir;
    }

    public String getApkPath() {
        return apkPath;
    }
}
