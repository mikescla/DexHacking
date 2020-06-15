package utils;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileUtils {
    public static String getSHA256Hash(String input) {
        FileInputStream fis = null;
        String hashCode = null;
        try {
            fis = new FileInputStream(new File(input));
            hashCode = DigestUtils.sha256Hex(fis);
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return hashCode;
    }

    public static Map<String, String> findFiles(String inputDir) throws IOException {
        System.out.println("Collecting file list...");
        return Files.walk(Paths.get(inputDir)).filter(Files::isRegularFile).map(Path::toString).collect(Collectors.toMap(FileUtils::getSHA256Hash, s -> s));

    }

    public static List<String> parseTextFile(String filePath) throws IOException {
        return Files.lines(Paths.get(filePath)).collect(Collectors.toList());
    }
}
