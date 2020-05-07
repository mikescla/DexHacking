package utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class IOUtils {
    public static String getSHA256Hash(String input) throws IOException {
        FileInputStream fis = new FileInputStream(new File(input));
        String hashCode = DigestUtils.sha256Hex(fis);
        fis.close();

        return hashCode;
    }
}
