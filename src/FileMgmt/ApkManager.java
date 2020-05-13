/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileMgmt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Scanner;

import static utils.Settings.APKTOOL_PATH;
import static utils.Settings.SIGN_PATH;

/**
 *
 * @author fabri
 */
public class ApkManager {
    private final String signKeyPath;
    private final String apkToolPath;
    
    public ApkManager(){
        String baseDir = Paths.get(System.getProperty("user.dir")).toString();
        this.apkToolPath = Paths.get(baseDir, APKTOOL_PATH).toString();
        this.signKeyPath = Paths.get(baseDir, SIGN_PATH).toString();
    }

    public void signApk(String apkPath) {
        try{
            Process cmd = Runtime.getRuntime().exec("jarsigner -storepass keykey -sigalg SHA1withRSA -digestalg SHA1 -keystore \"" +
                    this.signKeyPath + "\" \"" + apkPath + "\" Key");
            Scanner scanner = new Scanner(cmd.getErrorStream());
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
            cmd.waitFor();
            cmd.destroy();
        } catch(IOException ex){
            System.out.println("Error! File not Found");
        } catch(InterruptedException ex){
            System.out.println("Error! Execution interrupted!");
        }
    }
    
    public void decodeApk(String apkPath, String outputPath) {
        try{
            Process cmd =
                    Runtime.getRuntime().exec("java -jar \"" + this.apkToolPath + "\" d -r -s -f -o \"" + outputPath + "\" \"" + apkPath + "\"");
            Scanner scanner = new Scanner(cmd.getErrorStream());
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
            cmd.waitFor();
            cmd.destroy();
        } catch(IOException ex){
            System.out.println("Error! File not Found");
        } catch(InterruptedException ex){
            System.out.println("Error! Execution interrupted!");
        }
    }

    public void buildApk(String apkPath, String outputPath) {
        try{
            Process cmd = Runtime.getRuntime().exec("java -jar \"" 
                    + this.apkToolPath + "\" b -f -o \""
                    + outputPath + "\" \"" + apkPath + "\" ");
            Scanner scanner = new Scanner(cmd.getErrorStream());
            while (scanner.hasNext()) {
                System.out.println(scanner.nextLine());
            }
            cmd.waitFor();
            cmd.destroy();
            File apkDir =  new File(apkPath);
            deleteDirectory(apkDir);
        } catch(IOException ex){
            System.out.println("Error! File not Found");
        } catch(InterruptedException ex){
            System.out.println("Error! Execution interrupted!");
        }
    }   
    
    private void deleteDirectory(File dir){
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            for (int i = 0; i < children.length; i++) {
                deleteDirectory(children[i]);
            }
        }
        dir.delete();
    }

}
