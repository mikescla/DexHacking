/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileMgmt;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Scanner;
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
            Scanner scanner = new Scanner(cmd.getErrorStream());
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
            cmd.waitFor();
            cmd.destroy();
        } catch (IOException ex) {
            System.out.println("Error! File not Found");
        } catch (InterruptedException ex) {
            System.out.println("Error! Execution interrupted!");
        }
    }

    public int decodeApk() {
        try {
            Process cmd =
                    Runtime.getRuntime().exec("java -jar \"" + this.apkToolPath + "\" d -r -s -f -o \"" + this.decodedApkDir + "\" \"" + this.apkPath + "\"");
            int exitCode = cmd.waitFor();
            cmd.destroy();
            if (exitCode != 0) {
                Scanner scanner = new Scanner(cmd.getErrorStream());
                while (scanner.hasNext()) {
                    this.logger.severe(scanner.nextLine());
                }
                return exitCode;
                //                throw new IOException("Command exited with
                //                " + exitCode);
            } else return 0;
        } catch (IOException ex) {
            System.out.println("Error! File not Found");
        } catch (InterruptedException ex) {
            System.out.println("Error! Execution interrupted!");
        }
        return 0;
    }

    public void buildApk(String apkPath, int apkApiLevel, String outputPath) {
        try {
            Process cmd = Runtime.getRuntime().exec(MessageFormat.format(
                    "java -jar \"{0}\" b --force-all --debug -api {3}" + " -o" +
                            " " + "\"{1}\" \"{2}\" ", this.apkToolPath,
                    outputPath, apkPath, apkApiLevel));
            Scanner scanner = new Scanner(cmd.getErrorStream());
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
            cmd.waitFor();
            cmd.destroy();
            File apkDir = new File(apkPath);
            deleteDirectory(apkDir);
        } catch (IOException ex) {
            System.out.println("Error! File not Found");
        } catch (InterruptedException ex) {
            System.out.println("Error! Execution interrupted!");
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                deleteDirectory(children[i]);
            }
        }
        dir.delete();
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
