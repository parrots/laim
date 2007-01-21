package com.forgottenexpanse.laim;

import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.AwayMessageSchedule;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.RandomMessageGroups;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.AwayMessageSchedule.AwayMessage;
import com.forgottenexpanse.laim.LAIMPreferencesDocument.LAIMPreferences.RandomMessageGroups.MessageGroup;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Random;
import org.apache.xmlbeans.GDuration;

public class AwayScheduler implements Runnable {
    LAIM server;
    AwayMessageSchedule awayMessageSchedule;
    RandomMessageGroups randomMessageGroups;
    AwayMessage[] awayMessages;
    AwayMessage currentAwayMessage;
    Hashtable<String, String> nextAwayTimes = new Hashtable<String, String>();
    Calendar currentMessageEndDate = new GregorianCalendar();
    Thread t;

    @SuppressWarnings("static-access")
	public void run() {
        //if there was only one we don't need this thread
        if (awayMessages.length == 1) {
            server.setScheduledAwayMessage(awayMessages[0].getMessage());
            return;
        //more than one we use it
        } else {
            while (true) {
            	//get the current away message and pass it to the server
            	currentAwayMessage = getAwayMessage();
            	String message = "";
            	//if the group isn't null, we want to use a random message
            	if (!currentAwayMessage.getGroup().equalsIgnoreCase("none")) {
            		//loop through the groups to find the one with the name given
            		MessageGroup groupArray[] = randomMessageGroups.getMessageGroupArray();
            		for (int counter=0; counter < groupArray.length; counter++) {
            			if (groupArray[counter].getName().equalsIgnoreCase(currentAwayMessage.getGroup())) {
            				String[] messageArray = groupArray[counter].getRandomMessageArray();
            				Random generator = new Random();
            				int randomIndex = generator.nextInt( messageArray.length );
            				message = messageArray[randomIndex];
            				break;
            			}
            			message = currentAwayMessage.getMessage();
            		}
            	} else {
            		message = currentAwayMessage.getMessage();
            	}
            	server.setScheduledAwayMessage(message);
            	//System.out.println(currentAwayMessage.getName());
            	calculateNextTimes();
                try {
                	t.sleep(getSleepTime() + 5000); //just to be safe add 5 seconds
                } catch (Exception e) {
                    System.err.println("Couldn't sleep?!?" + e.getMessage());
                }
            }
        }
    }
    
    private void calculateNextTimes() {
    	//TODO deal with dropping down to previous message
    	//TODO better handling of length = 0?
    	if (awayMessages.length == 0) return;
    	nextAwayTimes.clear();
    	//loop through all but the default message and figure out the next time they will start
    	for (int counter = awayMessages.length - 1; counter>=1; counter--) {
    		AwayMessage currentAway = awayMessages[counter];
            Calendar startTime = currentAway.getStartTime();
            Calendar now = new GregorianCalendar();
            //assume it starts today and we'll add days as needed
            Calendar startDate = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE),
					 startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), 01);
            //if we already did this event for the day, pretend it's tomorrow
            if (now.after(startDate)) {
            	startDate.add(Calendar.DATE, 1);
            }
            //get the day of week
            int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
            
            //this away message is only for weekdays
            if (currentAway.getDaysOfWeek().equalsIgnoreCase("Weekdays")) {
            	//if it's a sunday, add one day since monday is next run
            	if (dayOfWeek == Calendar.SUNDAY) {
            		startDate.add(Calendar.DATE, 1);
            	//if it's a saturday, it won't happen for 2 days
            	} else if (dayOfWeek == Calendar.SATURDAY) {
            		startDate.add(Calendar.DATE, 2);
            	}
            //this away message is only for weekends
            } else if (currentAway.getDaysOfWeek().equalsIgnoreCase("Weekends")) {
            	//if we are currently on a weekday, set it to occur on saturday
            	if (dayOfWeek > Calendar.SUNDAY && dayOfWeek < Calendar.SATURDAY) {
            		startDate.add(Calendar.DATE, Calendar.SATURDAY - dayOfWeek);
            	}
            }
            
            //TODO away messages with a specific repetition
            
            //figure out how long to sleep for
            long timeTillEvent = startDate.getTimeInMillis() - new GregorianCalendar().getTimeInMillis();
            nextAwayTimes.put(currentAway.getName(), String.valueOf(timeTillEvent));
    	}
    }
    
    public void calcHM(long timeInSeconds) {
        long hours, minutes;
        hours = timeInSeconds / 3600;
        timeInSeconds = timeInSeconds - (hours * 3600);
        minutes = timeInSeconds / 60;
        timeInSeconds -= minutes * 60;
        System.out.println(hours + " hour(s) " + minutes + " minute(s) " + timeInSeconds + " second(s)" );
     }
    
    private AwayMessage getAwayMessage() {
    	AwayMessage currentAway = awayMessages[0];
    	for (int counter = awayMessages.length - 1; counter>=0; counter--) {
    		currentAway = awayMessages[counter];
    		if (isCurrentMessage(currentAway)) break;
    	}
    	return currentAway;
    }
    
    private boolean isCurrentMessage(AwayMessage message) {
    	Calendar startTime = message.getStartTime();
        Calendar now = new GregorianCalendar();
        //assume it starts today and we'll add days as needed
        Calendar startDate = new GregorianCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DATE),
				 startTime.get(Calendar.HOUR_OF_DAY), startTime.get(Calendar.MINUTE), 01);
        //if we already did this event for the day, pretend it's tomorrow
        if (now.before(startDate)) {
        	startDate.add(Calendar.DATE, -1);
        }
        //get the day of week
        int dayOfWeek = startDate.get(Calendar.DAY_OF_WEEK);
        
        //this away message is only for weekdays
        if (message.getDaysOfWeek().equalsIgnoreCase("Weekdays")) {
        	//if it's a sunday, add one day since monday is next run
        	if (dayOfWeek == Calendar.SUNDAY) {
        		startDate.add(Calendar.DATE, -2);
        	//if it's a saturday, it won't happen for 2 days
        	} else if (dayOfWeek == Calendar.SATURDAY) {
        		startDate.add(Calendar.DATE, -1);
        	}
        //this away message is only for weekends
        } else if (message.getDaysOfWeek().equalsIgnoreCase("Weekends")) {
        	//if we are currently on a weekday, set it to occur on saturday
        	if (dayOfWeek > Calendar.SUNDAY && dayOfWeek < Calendar.SATURDAY) {
        		startDate.add(Calendar.DATE, 0 - (dayOfWeek - 1));
        	}
        }
        
        //add in the duration for testing
        startDate.add(Calendar.SECOND, getDuration(message.getDuration()));
        currentMessageEndDate = startDate;
        return (startDate.after(now));
    }
    
    private int getDuration(GDuration duration) {
    	int seconds = duration.getSecond();
        seconds += duration.getMinute() * 60;
        seconds += duration.getHour() * 60 * 60;
        seconds += duration.getDay() * 60 * 60 * 24;
    	return seconds;
    }
    
    private long getSleepTime() {
    	//Figure how long it is until the next message is needed
    	long currentSleepTime = 0;
    	for (int counter = awayMessages.length - 1; counter>=1; counter--) {
    		long nextTime = Long.parseLong((String) nextAwayTimes.get(awayMessages[counter].getName()));
    		if (currentSleepTime == 0) currentSleepTime = nextTime;
    		else if (nextTime < currentSleepTime) currentSleepTime = nextTime;
    	}
    	//compare that to how long until the current one expires
    	Calendar nextStartTime = new GregorianCalendar();
    	Calendar now = new GregorianCalendar();
    	nextStartTime.add(Calendar.SECOND, (int)(currentSleepTime / 1000));
    	if (nextStartTime.after(currentMessageEndDate)) {
    		currentSleepTime = currentMessageEndDate.getTimeInMillis() - now.getTimeInMillis();
    	}
    	return currentSleepTime;
    }

    public AwayScheduler(LAIM server) {
        this.server = server;
        this.awayMessageSchedule = server.getSchedule();
        this.randomMessageGroups = server.getRandomMessages();
        //read the away messages into an array
        awayMessages = awayMessageSchedule.getAwayMessageArray();
    }
}
