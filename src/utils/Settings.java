package utils;

import java.nio.file.Paths;

public final class Settings {
    public static final String DATA_DIR = "Data";
    public static final String FEATURE_DIR = "features";
    public static final String DATASETS_DIR = "datasets";

    public static final String RES_DIR = "res";
    public static final String APKTOOL_DIR = "ApkTool";
    public static final String SIGN_DIR = "Key";
    public static final String DEX2JAR_DIR = "Dex2jar";
    public static final String APKTOOL_FILEPATH = Paths.get(RES_DIR,
            APKTOOL_DIR, "apktool.jar").toString();
    public static final String SIGN_FILEPATH = Paths.get(RES_DIR, SIGN_DIR,
            "key" + ".jks").toString();
    public static final String DEX2JAR_PATH =
            Paths.get(RES_DIR, DEX2JAR_DIR).toString();

    public static final String TEMP_DIR = "temp";
    public static final String OUT_DIR = "Out";


    // RPackDroid params
    // Feature mix
    public static final String API_ID = "api";
    public static final String PERMISSION_ID = "permission";
    public static final String MIXED_ID = "mixed";
    // API source
    public static final String PLATFORM_ID = "platform";
    public static final String SUPPORT_ID = "support";
    public static final String ANDROIDX_ID = "androidx";
    public static final String ALL_ID = "all";
    // API detail
    public static final String PACKAGE_ID = "packages";
    public static final String CLASS_ID = "classes";
    public static final String METHOD_ID = "methods";
    // API level
    public static final int DEF_API_LEVEL = 26;

}
