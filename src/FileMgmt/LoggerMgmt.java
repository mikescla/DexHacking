/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package FileMgmt;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 *
 * @author fabri
 */
public class LoggerMgmt {
    
    public static Logger getLogger() throws IOException{
        SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH.mm");
        String fileName = format.format(new Date()) + ".log";
        
        String currentPath = new File("").getAbsolutePath();
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("\\"));
        currentPath = currentPath.substring(0, currentPath.lastIndexOf("\\"));
        String logFilePath = currentPath.concat("\\Log\\" + fileName);
        
        Logger logger = Logger.getLogger("Log");
        FileHandler fh = new FileHandler(logFilePath);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        System.setProperty("-Djava.util.logging.SimpleFormatter.format","%5$s%6$s%n");
        logger.setUseParentHandlers(false);
        
        return logger;
    }
    
    

}
