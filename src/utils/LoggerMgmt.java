/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * @author fabri
 */
public class LoggerMgmt {

    public static Logger getLogger() throws IOException {
        String callerName =
                Thread.currentThread().getStackTrace()[2].getClassName();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy HH.mm");

        String fileName = MessageFormat.format("{0}_{1}.log", callerName,
                dateFormat.format(new Date()));

        String baseDir =
                Paths.get(System.getProperty("user.dir")).getParent().toString();
        String logDir = Paths.get(baseDir, "logs", "DexHacking").toString();
        new File(logDir).mkdirs();

        String logFilePath = Paths.get(logDir, fileName).toString();

        Logger logger = Logger.getLogger("DexHackingLogger");

        FileHandler fh = new FileHandler(logFilePath);
        ConsoleHandler ch = new ConsoleHandler();

        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        ch.setFormatter(formatter);

        logger.addHandler(fh);
        logger.addHandler(ch);

        System.setProperty("java.util.logging.SimpleFormatter.format", "%5$s:" +
                " " + "%6$s %n");
        logger.setUseParentHandlers(false);
        logger.setLevel(Level.ALL);

        return logger;
    }


}
