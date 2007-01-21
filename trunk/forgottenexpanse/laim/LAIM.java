/*
 * CHANGES TO DAIM CODE:
 * File LocateTool.java
 * 		Added private String profileText.  
 * 		Method getReturnedText returns this.profileText.  
 * 		In userInfo() set this.profileText = text
 * 		Add gotProfile tracking variable, set to false
 * 			set to true when profile received
 */

package com.forgottenexpanse.laim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.xmlbeans.*;
import org.walluck.oscar.AIMConstants;
import org.walluck.oscar.client.DaimLoginEvent;

import com.forgottenexpanse.laim.LAIMPreferencesDocument;
import com.forgottenexpanse.laim.LAIMMessagesDocument.LAIMMessages;
import com.forgottenexpanse.laim.LAIMMessagesDocument.LAIMMessages.Message;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.AwayMessageSchedule;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.AwayMessageSchedule.AwayMessage;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.RandomMessageGroups;

public class LAIM {
	private Thread awayScheduler;
	private Monitor monitor;
	private Listener listener;
	private Thread connectionManager;
	private String configFolder = System.getProperty("user.home") + "/.LAIM/";
	private String monitorProfile = "Hi, this is a LAIM (Latent AIM) monitor.  I wouldn't bother IMing me because I don't talk back, sorry.";
    private String listenerProfile = "Powered by LAIM";
    private String awayMessage = "";
    private String scheduledAwayMessage;
    private AwayMessageSchedule awayMessageSchedule;
    private RandomMessageGroups randomMessageGroups;
    private boolean useSchedule = true;
    private String listenerScreenname;
    private String listenerPassword;
    private String monitorScreenname;
    private String monitorPassword;
    private boolean clearNextIM = true;
    private boolean monitorOnline = false;
    private boolean listenerOnline = false;
    private boolean listenerFailed = false;
    private boolean monitorFailed = false;
    private boolean listenerShouldBeOnline = false;
    private String[] messageBuffer;
    private int currentMessageIndex = 0;
    
    private boolean developerMode = false;
    
    @SuppressWarnings("unused")
	public static void main(String args[]) {
		LAIM instance = new LAIM();
	}
	
	private LAIM() {
		//read the prefernce file in
		readPreferences();
		//start the server
		startServer();
	}
	
	public void setListenerProfile(String profile) {
		this.listenerProfile = profile;
	}
	
	public String getListenerScreenname() {
		return listenerScreenname;
	}
	
	public String getMonitorScreenname() {
		return monitorScreenname;
	}
	
	public String getListenerProfile() {
		return this.listenerProfile;
	}
	
	public String getMonitorProfile() {
		return monitorProfile;
	}
	
	public void setUseSchedule(boolean useage) {
		this.useSchedule = useage;
	}
	
    public boolean getMonitorStatus() {
    	return monitorOnline;
    }
    
    public boolean getMonitorFailedStatus() {
    	return monitorFailed;
    }
    
    public boolean getListenerStatus() {
    	return listenerOnline;
    }
    
    public boolean getListenerFailedStatus() {
    	return listenerFailed;
    }
    
    public void setMonitorStatus(boolean status) {
    	this.monitorOnline = status;
    }
    
    public boolean shouldBeOnline() {
    	return listenerShouldBeOnline;
    }
    
    public void setListenerStatus(boolean status) {
    	this.listenerOnline = status;
    }
	
    public void loginMonitor() {
    	monitorFailed = false;
    	monitorOnline = false;
    	System.out.println("Logging in monitor");
		try {
    		monitor.login(); 
    	} catch (Exception e) {
    		monitorOnline = false;
    		monitorFailed = true;
    		return;
    	}	
    	System.out.println("Waiting for monitor status...");
    	//keep looping and pausing until we are either logged in or failed
    	while (!getMonitorStatus() && !monitorFailed) {
    		pause(1000);
    	}
    	System.out.println("Checking if screenname is online");
    	if (monitor.readProfile()) {
    		System.out.println("Screenname online, holding login");
    		listenerShouldBeOnline = false;
    	} else {
 			listenerShouldBeOnline = true;
 			System.out.println("Screenname not online, listener should log in");
 		}
    }
    
    public void loginListener() {
    	listenerFailed = false;
    	listenerOnline = false;
    	System.out.println("Logging in listener");
    	try {
    		listener.login();
    	} catch (Exception e) { 
    		listenerOnline = false;
    		listenerFailed = true;
    		return;
    	}
    	System.out.println("Waiting for listener status...");
    	//keep looping and pausing until we are either logged in or failed
    	while (!getListenerStatus() && !listenerFailed) {
    		pause(1000);
    	}
    }
    
    public void checkMonitorStatus() {
    	monitorOnline = monitor.checkOnline();
    	if (!monitorOnline) stopMonitor(); //TODO do I want this here?
    }
    
    public void checkListenerStatus() {
    	listenerOnline = listener.checkOnline();
    	if (!listenerOnline) stopListener(); //TODO do I want this here?
    }
    
	private void startServer() {
		monitor = new Monitor(this, monitorScreenname, monitorPassword);
		listener = new Listener(this, listenerScreenname, listenerPassword);
    	
		System.out.println("Starting scheduler");
 		startScheduler();	
 		System.out.println("Starting Connection Monitor");
		startConnectionManager();
	}
    
	public void addMessage(String sn, String messageText) {
		if (clearNextIM) {
    		createMessageFile();
    	}
		
		//read in the cache of incoming IMs
    	LAIMMessagesDocument messageDocument = null;
    	try {
    		messageDocument = getMessageDocument();
    	} catch (Exception e) {
    		System.err.println("Error getting copy of message document: " + e.getMessage());
    		return;
    	}
        
    	String messageBody = stripHTML(messageText.trim());
    	
    	LAIMMessages messages = messageDocument.getLAIMMessages();
    	Message message = messages.addNewMessage();
    	
    	message.setScreenname(sn);
    	message.setMessageText(messageBody);
    	message.setDateTime(new GregorianCalendar());

    	messageDocument.setLAIMMessages(messages);
    	
        try {
        	messageDocument.save(getMessageFile());
        	clearNextIM = false;
        } catch (Exception e) { 
        	System.err.println("Error saving incoming message file " + e.getMessage());
        }
	}
	
	public void notifyFailedLogin(String location, DaimLoginEvent dle) {
		String errorMsg;
    	switch (dle.getErrorCode()) {
	        case AIMConstants.AIM_LOGINERROR_WRONGAUTH:
	                errorMsg = "Incorrect nickname or password.";
	                break;
	        case AIMConstants.AIM_LOGINERROR_ACCOUNTSUSPENDED:
	                errorMsg = "Your account is currently suspended.";
	                break;
	        case AIMConstants.AIM_LOGINERROR_UNAVAILABLE:
	                errorMsg = "The AOL Instant Messenger service is "
	                        + "temporarily unavailable.";
	                break;
	        case AIMConstants.AIM_LOGINERROR_CONNECTIONFLOOD:
	                errorMsg = "You have been connecting and disconnecting too "
	                        + "frequently. If you continue to try, you will need "
	                        + "to wait even longer.";
	                break;
	        case AIMConstants.AIM_LOGINERROR_OLDCLIENT:
	                errorMsg = "The client version you are using is too old. "
	                        + "Please upgrade.";
	                break;
	        default:
	                errorMsg = "Unknown.";
	                break;	
    	}
    	if (location.equalsIgnoreCase("monitor")) {
    		monitorOnline = false;
    		monitorFailed = true;
    	} else {
    		listenerOnline = false;
    		listenerFailed = true;
    	}
    	System.err.println("Couldn't log in " + location + ": " + errorMsg);
	}
	
	public void relayMessages() {
		currentMessageIndex = 0;
		messageBuffer = new String[100];  //TODO fix this, shouldn't limited, should be dynamic
		LAIMMessagesDocument messageDocument = null;
		try {
			messageDocument = getMessageDocument();
			Vector screenNames = getMessageSNs(messageDocument);
			if (screenNames.size() > 0 && !clearNextIM) {
				//Ugh, I wish Java had a "toTitleCase" like PHP does
				String personCount = getEnglish(screenNames.size()).toUpperCase().substring(0,1) + getEnglish(screenNames.size()).substring(1);
				//header string
				messageBuffer[currentMessageIndex] = "<html>" + personCount + " " + ((screenNames.size() == 1)?"person":"people") + " IM'd you while you were away.";
				//for each person who IMd us, deal with their messages
				for (int i = 0; i < screenNames.size(); i++) {
					//get the SN's messages all pretty-like
					formatSNMessages(messageDocument, (String) screenNames.elementAt(i));
				}
				messageBuffer[currentMessageIndex] += "</html>";
				
			} else {
				messageBuffer[currentMessageIndex] = "<html>No new messages</html>";
			}
		} catch (Exception e) {
			logMessages();
			messageBuffer[currentMessageIndex] = "<html>Uh oh.  There was an error reading message file.</html>";
		}
		
		//send the messages to the user
		try {
			for (int i = 0; i < currentMessageIndex + 1; i++) {
				if (developerMode) monitor.sendIM("Parrots01", messageBuffer[i], 0);
				else monitor.sendIM(listenerScreenname, messageBuffer[i], 0);
			}
			//and set the flag to erase the file on the next new IM
			clearNextIM = true;
		} catch (Exception e) {
			System.err.println("Coudln't relay messages, keeping them in the buffer");
			clearNextIM = false;
			//TODO test this case, make sure messages are relayed next time
			//logMessages(messageBuffer);
		}
	}
	
	private void logMessages() {
		Format formatter = new SimpleDateFormat("yyyyMMdd_HHmm");
    	String timeStamp = formatter.format(new Date());
    	
		File inputFile = new File(configFolder + "incomingMessages.xml");
	    File outputFile = new File(configFolder + "LAIMLog_" + timeStamp + ".html");
	    FileInputStream in;
	    FileOutputStream out;
	    System.out.println("Backing up message file");
	    try {
		    in = new FileInputStream(inputFile);
		    out = new FileOutputStream(outputFile);
		    int c;
		    while ((c = in.read()) != -1) {
		    	out.write(c);
		    }
		    in.close();
		    out.close();
		    //assuming the backup was successful, overwrite the old file
		    createMessageFile();
		    
	    } catch (FileNotFoundException e) {
	    	System.err.println("Error backing up message file, file not found.");
	    	System.out.println("Creating new message file.");
	    	createMessageFile();

	    } catch (Exception e) {
	    	System.err.println("Error backing up message file: " + e.getMessage());
	    }
	}
	
	private void logMessages(String[] messages) {
		Format formatter = new SimpleDateFormat("yyyyMMdd_HHmm");
    	String timeStamp = formatter.format(new Date());
    	
		String logFile = configFolder + "LAIMLog_" + timeStamp + ".html";
		File LAIMRelayFile = new File(logFile);
        Writer output = null;
        
        //create the file
        try {
        	 LAIMRelayFile.createNewFile();
        } catch (Exception e) { System.err.println("Couldn't create log file."); }
        
        //write to the file
        try {
            output = new BufferedWriter(new FileWriter(logFile));
            for (int i = 0; i < currentMessageIndex + 1; i++) {
            	output.write(messages[i]);
            }
            output.close();
        } catch (Exception e) { System.err.println("Couldn't write log file."); }
    }
	
	@SuppressWarnings("deprecation")
	public void quit() {
		System.out.println("Quitting");
		try {
			awayScheduler.stop();
		} catch (Exception e) {}
		try {
			relayMessages();
			pause(5000);
		} catch (Exception e) {}

		stopMonitor();
		stopListener();
		System.exit(0);
	}
	
	private void pause(int mseconds) {
		try {
			Thread.sleep(mseconds);
		} catch (InterruptedException e) { }
	}
	
	private void quit(Exception e) {
		System.err.println(e.getMessage());
		e.printStackTrace();
		quit();
	}
	
	private void quit(String message) {
		System.err.println(message);
		quit();
	}
	
	@SuppressWarnings("deprecation")
	public void startScheduler() {
    	if (awayScheduler != null) awayScheduler.stop();
		awayScheduler = new Thread(new AwayScheduler(this));
		awayScheduler.start();
    }
	
	@SuppressWarnings("deprecation")
	public void startConnectionManager() {
    	if (connectionManager != null) connectionManager.stop();
    	else connectionManager = new Thread(new ConnectionManager(this));
    	connectionManager.start();
    }
	
	public AwayMessageSchedule getSchedule() {
        return awayMessageSchedule;
    }
    
    public RandomMessageGroups getRandomMessages() {
        return randomMessageGroups;
    }
    
    public void setScheduledAwayMessage(String awayMessage) {
        this.scheduledAwayMessage = awayMessage;
        if (this.useSchedule && getListenerStatus()) listener.setAwayMessage();
    }
    
    public void setCustomAwayMessage(String awayMessage) {
    	this.awayMessage = awayMessage;
    	setUseSchedule(false);
    }
    
    public void updateAwayMessage() {
    	listener.setAwayMessage();
    }
    
    public void notifyLogIn() {
		System.out.println("Another client logged in on the screen name.");
		listenerShouldBeOnline = false;
		stopListener();
		relayMessages();
	}
	
	private File getMessageFile() throws Exception {
        String xmlPath = configFolder + "incomingMessages.xml";
        File LAIMMessagesFile = new File(xmlPath);
        
        return LAIMMessagesFile;
    }
	
	private LAIMMessagesDocument getMessageDocument() throws Exception {
    	Collection errorList = new ArrayList();
        XmlOptions xo = new XmlOptions();
        xo.setErrorListener(errorList);
        LAIMMessagesDocument xmlDocument = null;
        
        File LAIMMessagesFile = getMessageFile();

        //once we have a doc, try to read it into the XMLBean
        try {
			xmlDocument = LAIMMessagesDocument.Factory.parse(LAIMMessagesFile);
		} catch (Exception e) {
			System.err.println("Error parsing incoming message file " + e.getMessage());
		}

        //make sure it's valid
        if (!xmlDocument.validate(xo)) {
        	System.err.println("Incoming message file isn't valid");
        }
        return xmlDocument;	
    }
    
    private Vector getMessageSNs(LAIMMessagesDocument messageDocument) {
    	String searchExpression = "declare namespace xq='http://laim.forgottenexpanse.com/';" + 
		"$this/xq:LAIMMessages/xq:Message";

    	Message[] messages = (Message[]) messageDocument.selectPath(searchExpression);
    	Vector<String> results = new Vector<String>();
    	for (int i = 0; i < messages.length; i++) {
    		Message currentMessage = messages[i];
			if (!results.contains(currentMessage.getScreenname())) {
    			results.add(currentMessage.getScreenname());
    		}
		}
    	return results;
    }
    
    private String formatMessage(Message currentMessage) {
    	SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss");
    	String message = "<br /><font color=\"#2b5674\">[" + formatter.format(currentMessage.getDateTime().getTime()) 
    					 + "]:</font> " + currentMessage.getMessageText();
    	return message;
    }
    
    private void formatSNMessages(LAIMMessagesDocument messageDocument, String SN) {
    	String searchExpression = "declare namespace xq='http://laim.forgottenexpanse.com/';" + 
								  "$this/xq:LAIMMessages/xq:Message[xq:Screenname='" + SN + "']";
    	Message[] userMessages = (Message[]) messageDocument.selectPath(searchExpression);
    	SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");
    	String previousDate = "";
    	boolean firstMessage = true;
		
    	for (int m = 0; m < userMessages.length; m++) {
    		Message currentMessage = userMessages[m];
    		String dateTime = formatter.format(currentMessage.getDateTime().getTime());
    		String header = "";
    		boolean useHeader = false;
    		header = "<br /><b>" + SN + "</b> on " + dateTime;
    		if (!dateTime.equals(previousDate)) {
    			useHeader = true;
    			previousDate = dateTime;
    		}
    		String message = formatMessage(currentMessage);
    		
    		//check to make sure each message isn't going to bring the buffer over 1024
    		if ((messageBuffer[currentMessageIndex] + ((useHeader)?header:"") + message + "<br /></html>").length() > 1024) {
    			messageBuffer[currentMessageIndex] += "</html>";
    			currentMessageIndex++;
    			messageBuffer[currentMessageIndex] = "<html>";
    			//if it's the first message for this user, normal header.  if not, do a "continued" header
    			if (firstMessage) messageBuffer[currentMessageIndex] += header + message;
    			else messageBuffer[currentMessageIndex] += "<br /><b>" + SN + "</b> (continued from above)" + message;
			} else {
				messageBuffer[currentMessageIndex] += ((useHeader)?header:"") + message;
			}
    		firstMessage = false;
		}
    	messageBuffer[currentMessageIndex] += "<br />";
    }
	
    private void readPreferences() {
        Collection errorList = new ArrayList();
        XmlOptions xo = new XmlOptions();
        xo.setErrorListener(errorList);

        //try to read in the document
        String xmlPath = configFolder + "config.xml";
        LAIMPreferencesDocument xmlDocument;
        File LAIMPreferencesFile = new File(xmlPath);

        //if it doesn't exist create a new one
        if (!LAIMPreferencesFile.exists()) {
            try {
                createPreferenceFile();
            } catch (IOException e) { quit(e); return; }
        }

        //once we have a doc, try to read it into the XMLBean
        try {
            xmlDocument = LAIMPreferencesDocument.Factory.parse(LAIMPreferencesFile);
        } catch (Exception e) { quit(e); return; }

        //make sure it's valid
        try {
            if (!xmlDocument.validate(xo)) {
                //if it isn't valid, see if the user wants to recreate it now
                if (!setupPreferences(xmlPath)) {
                    //if it isn't valid and the user doesn't want to recreate, die
                    quit("Malformed XML file: " + errorList.toString());
                    return;
                }
            }
        } catch (Exception e) { quit(e); }

        listenerScreenname = xmlDocument.getLAIMPreferences().getScreenname();
        listenerPassword = xmlDocument.getLAIMPreferences().getPassword();
        monitorScreenname = xmlDocument.getLAIMPreferences().getMonitorScreenname();
        monitorPassword = xmlDocument.getLAIMPreferences().getMonitorPassword();
        awayMessageSchedule = xmlDocument.getLAIMPreferences().getAwayMessageSchedule();
        randomMessageGroups = xmlDocument.getLAIMPreferences().getRandomMessageGroups();
        if (listenerScreenname.equalsIgnoreCase("parrotstogo")) developerMode = true;
    }
    
    private boolean setupPreferences(String xmlPath) {
        String overwrite = "";
        //make sure the user wants to overwrite the XML file.
        while (!overwrite.equalsIgnoreCase("n") && !overwrite.equalsIgnoreCase("y")) {
            overwrite = promptForInput("Bad config file, overwrite (y|n)?");
        }
        //if not, stop now
        if (overwrite.equalsIgnoreCase("n")) return false;

        //input from user for config values

        listenerScreenname = promptForInput("Screenname:");
        listenerPassword = promptForInput("Password:");
        monitorScreenname = promptForInput("Screenname to monitor with:");
        monitorPassword = promptForInput("Password for monitor:");
        String defaultAway = promptForInput("Default away message:");
        

        File LAIMPreferencesFile = new File(xmlPath);
        LAIMPreferencesDocument xmlDocument;
        try {
            xmlDocument = LAIMPreferencesDocument.Factory.parse(LAIMPreferencesFile);
        } catch (Exception e) { quit(e); return false; }

        //set up the base values within the config file
        LAIMPreferences prefs = xmlDocument.getLAIMPreferences();
        prefs.setScreenname(listenerScreenname);
        prefs.setPassword(listenerPassword);
        prefs.setMonitorScreenname(monitorScreenname);
        prefs.setMonitorPassword(monitorPassword);

        //set up the default away message
        GDuration duration = new GDuration("PT24H");
        Calendar startTime = new GregorianCalendar();
        startTime.set(2000,01,01,00,00,00);

        AwayMessageSchedule awaySched = prefs.addNewAwayMessageSchedule();
        AwayMessage defaultMessage = awaySched.addNewAwayMessage();
        defaultMessage.setName("Default");
        defaultMessage.setMessage(defaultAway);
        defaultMessage.setDaysOfWeek("ALL");
        defaultMessage.setDuration(duration);
        defaultMessage.setStartTime(startTime);
        
        randomMessageGroups = prefs.addNewRandomMessageGroups();

        xmlDocument.setLAIMPreferences(prefs);
        try {
            xmlDocument.save(LAIMPreferencesFile);
        } catch (Exception e) { quit(e); return false; }
        return true;
    }
    
    private void createMessageFile()  {
        String xmlPath = configFolder + "incomingMessages.xml";
        //make sure the parent directory exists
        File LAIMDir = new File(configFolder);
        if (!LAIMDir.exists()) LAIMDir.mkdir();
        
        File LAIMMessagesFile = new File(xmlPath);
        Writer output = null;
        String emptyXML = "<?xml version=\"1.0\" ?><LAIMMessages xmlns=\"http://laim.forgottenexpanse.com/\" xmlns:xq=\"http://laim.forgottenexpanse.com/\"></LAIMMessages>";

        //create the file
        try {
	        LAIMMessagesFile.createNewFile();
	
	        //Set up the basic structure for the XML file
            output = new BufferedWriter(new FileWriter(xmlPath));
            output.write(emptyXML);
	        
        } catch (IOException e) {
        	System.err.println("Error creating message cache file: " + e.getMessage());
        	quit(e);
        } finally { 
        	try {
        		output.close();
        	} catch (IOException e) { }
        }
    }
    
    private void createPreferenceFile() throws IOException {
        String xmlPath = configFolder + "config.xml";
        //make sure the parent directory exists
        File LAIMDir = new File(configFolder);
        if (!LAIMDir.exists()) LAIMDir.mkdir();
        
        File LAIMPreferencesFile = new File(xmlPath);
        Writer output = null;
        String emptyXML = "<?xml version=\"1.0\" ?><LAIMPreferences xmlns=\"http://laim.forgottenexpanse.com/\"></LAIMPreferences>";

        //create the file
        LAIMPreferencesFile.createNewFile();

        //Set up the basic structure for the XML file
        try {
            output = new BufferedWriter(new FileWriter(xmlPath));
            output.write(emptyXML);
        }
        finally { output.close(); }
    }
    
    private String promptForInput(String promptMessage) {
        System.out.print(promptMessage);

        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        String line = null;

        try {
            while (true) {
                line = stdin.readLine();
                if (line != null) break;
            }
        } catch (Exception e) { quit(e); }
        return line.trim();
    }
	
    public void cacheProfile() {
    	String profilePath = configFolder + "profile.html";
    	BufferedWriter writer;

    	try {
			writer = new BufferedWriter(new FileWriter(profilePath));
			writer.write(listenerProfile);
			writer.close();
		} catch (IOException e) {}
    }
    
    public void updateListenerProile() {
    	listener.setProfile();
		//we need to put up the away message too, since setting profile removes it
		listener.setAwayMessage();
    }
    
    public String getCachedProfile() {
    	String profilePath = configFolder + "profile.html";
    	BufferedReader reader;
    	String thisLine;
    	String profile = "";
    	try {
			reader = new BufferedReader(new FileReader(profilePath));
	        while ((thisLine = reader.readLine()) != null) {
	        	profile += thisLine;
	        }
	        reader.close();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {}
		
		return profile;
    }
    
    public void notifyLogout(String sn) {
    	if (sn.equalsIgnoreCase(listenerScreenname)) {
    		System.out.println("SN logged out, logging in");
    		listenerShouldBeOnline = true;
    		loginListener();
    	}
    }
    
    public void stopListener() {
    	if (getListenerStatus()) {
	    	System.out.println("Stopping listener");
	    	listenerOnline = false;
	    	try {
	    		listener.logout();
	    	} catch (Exception e) {}
    	} else {
    		System.out.println("Told to stop listener, but already stopped");
    	}
    }
    
    public void stopMonitor() {
    	if (getMonitorStatus()) {
	    	System.out.println("Stopping monitor");
	    	monitorOnline = false;
	    	try {
	    		monitor.logout();
	    	} catch (Exception e) {}
    	} else {
    		System.out.println("Told to stop monitor, but already stopped");
    	}
    }
    
    public String getAwayMessage() {
    	if (this.useSchedule) return scheduledAwayMessage;
    	else return awayMessage;
    }
    
    public String stripHTML(String htmlString) {
    	//remove HTML tags we don't want
        String htmlPattern = "(<[/]?(BODY|HTML|P|FONT)+[^>]*>)";
        Pattern pattern = Pattern.compile(htmlPattern, Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(htmlString);
    	htmlString = m.replaceAll("");
    	
    	//kill a BR at the end, if there is one
    	htmlPattern = "<(BR)[ ]?[/]?>$";
    	pattern = Pattern.compile(htmlPattern, Pattern.CASE_INSENSITIVE);
    	m = pattern.matcher(htmlString);
    	htmlString = m.replaceAll("");
    	
    	return htmlString;
    }
    
    public String getEnglish(int number) {
    	if (number > -1 && number < 14) {
    		switch (number) {
	    		case 0:
					return "zero";
	    		case 1:
	    			return "one";
	    		case 2:
	    			return "two";
	    		case 3:
	    			return "three";
	    		case 4:
	    			return "four";
	    		case 5:
	    			return "five";
	    		case 6:
	    			return "six";
	    		case 7:
	    			return "seven";
	    		case 8:
	    			return "eight";
	    		case 9:
					return "nine";
	    		case 10:
	    			return "ten";
	    		case 11:
	    			return "eleven";
	    		case 12:
	    			return "twelve";
    			default:
    				return "thirteen";
    		}
    	} else if (number > 13 && number < 20) {
    		return getEnglish(number % 10) + "teen";
    	} else if (number > 19 && number < 30) {
    		if (number == 20) return "twenty";
    		else return "twenty " + getEnglish(number % 20);
    	} else if (number > 29 && number < 40) {
    		if (number == 30) return "thirty";
    		else return "thirty " + getEnglish(number % 30);
    	} else if (number > 39 && number < 50) {
    		if (number == 40) return "fourty";
    		else return "fourty " + getEnglish(number % 40);
		} else {
			return "more than fifty";
		}
    }
}
