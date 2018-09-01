package util;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class InitLogger {
public static void initLogger() {
		
		LogManager.getLogManager().reset();
		Logger logger = Logger.getLogger("");
		String logLevel = "";
		Formatter textFormatter= new SimpleFormatter();
		
		
		try{
			String logFileName=Configuration.getProperty("logFile");
			FileHandler fileHandler=new FileHandler(logFileName,1000000,100,true);
			fileHandler.setFormatter(textFormatter);
			fileHandler.setLevel(Level.ALL);
			logger.addHandler(fileHandler);
			
			
		} catch(Exception npe){
			ConsoleHandler consoleHandler= new ConsoleHandler();
			consoleHandler.setFormatter(textFormatter);
			consoleHandler.setLevel(Level.ALL);
			logger.addHandler(consoleHandler);
		
			//logger.log(Level.SEVERE,npe.getMessage(),npe);
		} 
		try{
			
			logLevel=Configuration.getProperty("logLevel");
		} catch(NullPointerException npe){	
		}
		
		switch(logLevel){
			case "SEVERE":
				logger.setLevel(Level.SEVERE);
				break;
			case "WARNING":
				logger.setLevel(Level.WARNING);
				break;
			case "INFO":
				logger.setLevel(Level.INFO);
				break;
			case "CONFIG":
				logger.setLevel(Level.CONFIG);
				break;
			case "FINE":
				logger.setLevel(Level.FINE);
				break;
			case "FINER":
				logger.setLevel(Level.FINER);
				break;
			case "FINEST":
				logger.setLevel(Level.FINEST);
				break;
			default:
				logger.setLevel(Level.ALL);
		}
		
	}

}
