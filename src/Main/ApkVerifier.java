package Main;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import utils.LoggerMgmt;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static utils.Settings.DEX2JAR_PATH;
import static utils.Settings.TEMP_DIR;

public class ApkVerifier {

    private static final Path baseDir =
            Paths.get(System.getProperty("user" + ".dir"));
    private static final String parentDir = baseDir.getParent().toString();

    private static final String dex2JarPath = Paths.get(baseDir.toString(),
            DEX2JAR_PATH).toString();

    private static final String tempDir =
            Paths.get(parentDir, TEMP_DIR).toString();

    public static void main(String[] args) throws IOException {
        // logging
        Logger logger = LoggerMgmt.getLogger();

        // argument parsing
        CommandLine cmd = parseArgs(args);

        String inputDir = cmd.getOptionValue("inputd");

        Stream<Path> apkStream = Files.list(Paths.get(inputDir));
        // commands
        String baseCmd;
        String currSystem = System.getProperty("os.name");
        if (currSystem.toLowerCase().contains("windows"))
            baseCmd = MessageFormat.format("\"{0}\"", Paths.get(dex2JarPath,
                    "d2j-dex2jar.bat").toString());
        else baseCmd = Paths.get(dex2JarPath, "d2j-dex2jar.sh").toString();

        apkStream.parallel().forEach(apkPath -> {
            try {
                String currCommand = MessageFormat.format("{0} \"{1}\" " +
                        "--force", baseCmd, apkPath, tempDir);
                // TODO refactor to support linux
                ProcessBuilder pb = new ProcessBuilder("cmd", "/C",
                        currCommand);
                pb.directory(new File(tempDir));

                //                    pb.redirectOutput(ProcessBuilder
                //                    .Redirect.INHERIT);
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);

                Process p = pb.start();

                int exitCode = p.waitFor();
                if (exitCode != 0) {
                    logger.severe(IOUtils.toString(p.getErrorStream(),
                            StandardCharsets.UTF_8));
                } else {
                    logger.info(MessageFormat.format("{0} is OK",
                            apkPath.getFileName()));
                }

                p.destroy();
            } catch (IOException | InterruptedException ex) {
                logger.log(Level.SEVERE, ex.getMessage(), ex);
            }

        });

    }

    private static CommandLine parseArgs(String[] args) {
        Options options = new Options();

        Option apkFile = new Option("inputd", "inputDir", true, "input APKs' "
                + "dir");
        apkFile.setRequired(true);
        options.addOption(apkFile);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("-inputd DIR_PATH", options);

            System.exit(1);
        }
        return cmd;
    }

}
