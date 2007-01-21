package com.forgottenexpanse.laim;

import java.io.IOException;
import java.util.Map;
import org.walluck.oscar.AIMConnection;
import org.walluck.oscar.AIMConstants;
import org.walluck.oscar.AIMSession;
import org.walluck.oscar.SNACFamily;
import org.walluck.oscar.UserInfo;
import org.walluck.oscar.channel.aolim.AOLIM;
import org.walluck.oscar.client.Buddy;
import org.walluck.oscar.client.DaimBuddyListener;
import org.walluck.oscar.client.DaimLoginEvent;
import org.walluck.oscar.client.DaimLoginListener;
import org.walluck.oscar.client.DaimMsgListener;
import org.walluck.oscar.handlers.icq.ICQSMSMessage;
import org.walluck.oscar.requests.JoinRoomRequest;
import org.walluck.oscar.tools.AdminTool;
import org.walluck.oscar.tools.AdvertTool;
import org.walluck.oscar.tools.BOSTool;
import org.walluck.oscar.tools.BuddyListTool;
import org.walluck.oscar.tools.ChatNavTool;
import org.walluck.oscar.tools.ChatTool;
import org.walluck.oscar.tools.FileTransferTool;
import org.walluck.oscar.tools.ICBMTool;
import org.walluck.oscar.tools.ICQTool;
import org.walluck.oscar.tools.IconTool;
import org.walluck.oscar.tools.InviteTool;
import org.walluck.oscar.tools.LocateTool;
import org.walluck.oscar.tools.LoginTool;
import org.walluck.oscar.tools.MailTool;
import org.walluck.oscar.tools.MiscTool;
import org.walluck.oscar.tools.ODirTool;
import org.walluck.oscar.tools.PopupTool;
import org.walluck.oscar.tools.SSITool;
import org.walluck.oscar.tools.ServiceTool;
import org.walluck.oscar.tools.StatsTool;
import org.walluck.oscar.tools.TranslateTool;
import org.walluck.oscar.tools.UserLookupTool;


/**
 * Class AbstractOscarClient
 *
 * The easiest way to create an Oscar client is to extend this abstract class.
 * This class listens on all the Oscar events available to clients, and provides
 * default responses to them.  For example, a file request by default is
 * refused.  The more methods you override, the more of the Oscar functionality
 * becomes available to your client.
 *
 * If you use this class, you MUST call it's constructor.  If you have a constructor
 * in your own class, just call super(); as the first thing in it!
 *
 * @author  <a href="mailto:alain@rexorient.com">Alain Penders</a>
 * @version 1.0
 * @since 1.0
 */
public class Monitor implements DaimLoginListener, DaimMsgListener, DaimBuddyListener {

    protected AIMSession session;

    /* Our internal tool references */
    protected AdminTool        admtool;
    protected AdvertTool       advtool;
    protected BOSTool          bostool;
    protected BuddyListTool    budtool;
    protected ChatNavTool      chntool;
    protected ChatTool         chttool;
    protected FileTransferTool filtool;
    protected ICBMTool         msgtool;
    protected IconTool         icotool;
    protected ICQTool          icqtool;
    protected InviteTool       invtool;
    protected LocateTool       loctool;
    protected LoginTool        logtool;
    protected MailTool         maltool;
    protected MiscTool         msctool;
    protected ODirTool         dirtool;
    protected PopupTool        poptool;
    protected ServiceTool      svctool;
    protected SSITool          ssitool;
    protected StatsTool        ststool;
    protected TranslateTool    trntool;
    protected UserLookupTool   loktool;

    private String screenname;
    private String password;
    private String profile;
    private LAIM server;
    
    private int aimCaps = AIMConstants.AIM_CAPS_SENDBUDDYLIST | AIMConstants.AIM_CAPS_CHAT | AIMConstants.AIM_CAPS_BUDDYICON | AIMConstants.AIM_CAPS_SAVESTOCKS | AIMConstants.AIM_CAPS_INTEROPERATE | AIMConstants.AIM_CAPS_SHORT;
    
    public Monitor(LAIM server, String screenname, String password) {
    	this.screenname = screenname;
    	this.password = password;
    	this.server = server;
    	this.profile = server.getMonitorProfile();
    	
        /* Create our AIMSession. */
        session = new AIMSession();
        session.setFlags(aimCaps);
        
        /* Get the tools -- faster if we cache 'em */
        admtool = (AdminTool) session.getTool(SNACFamily.AIM_CB_FAM_ADM);
        advtool = (AdvertTool) session.getTool(SNACFamily.AIM_CB_FAM_ADS);
        bostool = (BOSTool) session.getTool(SNACFamily.AIM_CB_FAM_BOS);
        budtool = (BuddyListTool) session.getTool(SNACFamily.AIM_CB_FAM_BUD);
        chntool = (ChatNavTool) session.getTool(SNACFamily.AIM_CB_FAM_CTN);
        chttool = (ChatTool) session.getTool(SNACFamily.AIM_CB_FAM_CHT);
        filtool = (FileTransferTool) session.getTool(SNACFamily.AIM_CB_FAM_FILETRANSFER);
        msgtool = (ICBMTool) session.getTool(SNACFamily.AIM_CB_FAM_MSG);
        icotool = (IconTool) session.getTool(SNACFamily.AIM_CB_FAM_ICO);
        icqtool = (ICQTool) session.getTool(SNACFamily.AIM_CB_FAM_ICQ);
        invtool = (InviteTool) session.getTool(SNACFamily.AIM_CB_FAM_INV);
        loctool = (LocateTool) session.getTool(SNACFamily.AIM_CB_FAM_LOC);
        logtool = (LoginTool) session.getTool(SNACFamily.AIM_CB_FAM_ATH);
        maltool = (MailTool) session.getTool(SNACFamily.AIM_CB_FAM_MAL);
        msctool = (MiscTool) session.getTool(SNACFamily.AIM_CB_FAM_SPL);
        dirtool = (ODirTool) session.getTool(SNACFamily.AIM_CB_FAM_ODR);
        poptool = (PopupTool) session.getTool(SNACFamily.AIM_CB_FAM_POP);
        svctool = (ServiceTool) session.getTool(SNACFamily.AIM_CB_FAM_GEN);
        ssitool = (SSITool) session.getTool(SNACFamily.AIM_CB_FAM_SSI);
        ststool = (StatsTool) session.getTool(SNACFamily.AIM_CB_FAM_STS);
        trntool = (TranslateTool) session.getTool(SNACFamily.AIM_CB_FAM_TRN);
        loktool = (UserLookupTool) session.getTool(SNACFamily.AIM_CB_FAM_LOK);
        
        /* Register us as a listener */
        if(logtool != null) logtool.addLoginListener(this);

        if(msgtool != null) msgtool.addListener(this);

        if(budtool != null) budtool.addListener(this);
	}

    public void setProfile() {
    	try {
    		loctool.setInfo("us-ascii", profile);
    	} catch (Exception e) {}
    }
    
    public void login() throws Exception {
		login(screenname, password); 
	}

	private void pause(int mseconds) {
		try {
			Thread.sleep(mseconds);
		} catch (InterruptedException e) { }
	}
        
    /**
     * Log in using the provided screenname and password.
     *
     * @param screenname
     * @param password
     * @throws IOException
     */
    public void login(String screenname, String password) throws IOException {
    	session.setFlags(this.aimCaps);
        session.setSN(screenname);
        session.setPassword(password);
        session.setFlags(aimCaps);
        logtool.login();
    }


    /**
     * Log out and close the connection.
     */
    public void logout() {
    	try {
			AIMConnection.killAllInSess(session);
		} catch (Exception e) {}
    }


    /**
     * Send an IM
     *
     * @param sn       Screen Name you're sending to.
     * @param message  Message to send
     * @param imflags  Flags.  Only flag used is AIMConstant.AIM_IMFLAG_AWAY
     * @throws java.io.IOException if an error occurs
     */
    public void sendIM(String sn, String message, int imflags) throws IOException {
        msgtool.sendIM(sn, message, imflags);
    }


    /**
     * Add a buddy to the buddy list.
     *
     * @param name  Name of the buddy
     * @param group Group to add him in
     * @return true if the add is possible and was sent to the server.
     *
     * @exception java.io.IOException if an error occurs
     */
    public boolean addBuddy(String name, String group) throws IOException {
        return budtool.addBuddy(name, group);
    }


    /**
     * Move a Buddy to another buddy group.
     *
     * @param name     Name of buddy to move
     * @param oldGroup Old buddy group
     * @param newGroup New buddy group
     * @return true if the move is possible and sent to the server.
     *
     * @exception java.io.IOException if an error occurs
     */
    public boolean moveBuddy(String name, String oldGroup, String newGroup) throws IOException {
        return budtool.moveBuddy(name, oldGroup, newGroup);
    }


    /**
     * Remove a buddy from the buddy list.
     *
     * If the buddy doesn't exist in the Buddy list the server sent us, or the AIMSession is not in a
     * state where the remove command can be sent, this method will return false, and the budddy will
     * not be removed!
     *
     * @param name  Screen name of buddy to remove.
     * @param group Group the buddy belongs to.
     * @return true if the buddy can be removed
     *
     * @exception java.io.IOException if an error occurs
     */
    public boolean removeBuddy(String name, String group) throws IOException {
        return budtool.removeBuddy(name, group);
    }


    /**
     * Rename a group.
     *
     * @param oldGroup Old name
     * @param newGroup New name
     * @return true if the group can be renamed and the request is sent to the server
     *
     * @exception java.io.IOException if an error occurs
     */
    public boolean renameGroup(String oldGroup, String newGroup) throws IOException {
        return budtool.renameGroup(oldGroup, newGroup);
    }

    /**
     * Method called when a login login fails.
     * <p/>
     * AIMSession will be set with the session that failed.  errorCode will be set with the
     * failure error code.  See AIMConstants.AIM_LOGINERROR_* for error codes.
     *
     * @param dle DaimLoginEvent
     */
    public void loginError(DaimLoginEvent dle) {
    	server.notifyFailedLogin("monitor", dle);
    }


    /**
     * Method called when the login is succesful.
     * <p/>
     * AIMSession will be set.
     *
     * @param dle
     */
    public void loginDone(DaimLoginEvent dle) {
    	System.out.println("Setting monitor profile");
    	pause(2000);
		setProfile();
		pause(4000);
		try {
			loctool.setAwayAIM(" ");
		} catch (Exception e) {}
 		pause(1000);
 		try {
			loctool.setAwayAIM(profile);
		} catch (Exception e) {}
		System.out.println("Monitor online");
		server.setMonitorStatus(true);
    }


    /**
     * Method called when a new UIN is assigned by the server (ICQ account registration).
     * <p/>
     * AIMSession and UIN will be set.
     *
     * @param dle
     */
    public void newUIN(DaimLoginEvent dle) {
    }

    /**
     * Called when a message is received from someone.
     * <p/>
     * Use from.getSN() to get the buddy's ScreenName if Buddy is null.
     * <p/>
     * Use args.getMsg() to get the message.
     *
     * @param buddy Buddy who sent the message, or null if not created yet.
     * @param from  UserInfo
     * @param args
     */
    public void incomingIM(Buddy buddy, UserInfo from, AOLIM args) {
    	String message = server.stripHTML(args.getMsg());
    	String sn = (buddy == null)?from.getSN():buddy.getName();
    	//System.out.println(sn);
		if (sn.equalsIgnoreCase(screenname) || sn.equalsIgnoreCase("parrots01")) {
			//if I'm getting a message from myself, that means I'm trying to sent a command
			if (message.equals("?")) {
				String helpMessage = "<b>Command help:\n</b>";
				helpMessage += "<i>quit</i> - Shutdown the server.\n";
				helpMessage += "<i>logout</i> - Force logout the listener, and relay any messages.  Will auto log back in when you sign off.\n";
				helpMessage += "<i>profile</i> - Reads your current profile and stores it for the listener.\n";
				helpMessage += "<i>schedule</i> - Resumes scheduled away messages.\n";
				helpMessage += "<i>messages</i> - Relays the currently saved messages to you.\n";
				helpMessage += "Any other text sent will be used as the away message.  This stops the server from using any scheduled messages."; 
				try {
					msgtool.sendIM(sn, helpMessage, 0);
				} catch (Exception e) {}
			} else if (message.equalsIgnoreCase("logout")) {
				server.relayMessages();
				try {
					msgtool.sendIM(sn, "Logging the listener for " + server.getListenerScreenname() + " out.", 0);
				} catch (Exception e) {}
				server.stopListener();
			} else if (message.equalsIgnoreCase("quit")) {
				try {
					msgtool.sendIM(sn, "Quitting", 0);
				} catch (Exception e) {}
				server.quit();
			} else if (message.equalsIgnoreCase("messages")) {
				server.relayMessages();
			} else if (message.equalsIgnoreCase("schedule")) {
				server.setUseSchedule(true);
				server.updateAwayMessage();
			} else if (message.equalsIgnoreCase("profile")) {
				try {
					msgtool.sendIM(sn, "Reading your current profile please wait...", 0);
				} catch (Exception e) {}
				if (readProfile()) {
					server.updateListenerProile();
					try {
						msgtool.sendIM(sn, "Done", 0);
					} catch (Exception e) {}
				} else {
					try {
						msgtool.sendIM(sn, "Failed", 0);
					} catch (Exception e) {}
				}
			} else {
				server.setCustomAwayMessage(message);
	            server.updateAwayMessage();
			}
		} else {
			//a message from anyone else but the SN needs to be ignored
	    	try {
	    		msgtool.sendIM(sn, profile, 0);
	    	} catch (Exception e) { }
		}
    }
    
    public boolean readProfile() {
    	try {
			loctool.getAway(server.getListenerScreenname());
			int counter = 0;
			//wait till we get a notification about the profile, or it times out
			while (!loctool.hasReturnedProfile() && counter < 15) {
				pause(1000);
				counter++;
			}
			String profile = server.stripHTML(loctool.getReturnedText());
			if (profile != null && !profile.equalsIgnoreCase("")) {
				server.setListenerProfile(profile);
				server.cacheProfile();
	    		return true;
			}
		} catch (Exception e) {}
		server.setListenerProfile(server.getCachedProfile());
		return false;
    }
    
    public boolean checkOnline() {
    	try {
			loctool.getAway(server.getMonitorScreenname());
			int counter = 0;
			//wait till we get a notification about the profile, or it times out
			while (!loctool.hasReturnedProfile() && counter < 15) {
				pause(1000);
				counter++;
			}
			return loctool.hasReturnedProfile();
		} catch (Exception e) { }
		return false;
    }
    
    /**
     * Someone requested us to join a chat room.
     * <p/>
     * To accept, call ChatTool.joinRoom() with the JoinRoomRequest instance.
     */
    public void joinRoomRequest(JoinRoomRequest jrr) {
            /* Do nothing. */
    }


    /**
     * Typing notification for a user.
     *
     * @param sn     Screen Name
     * @param typing Typing code.
     */
    public void typingNotification(String sn, short typing) {
    }


    /**
     * We received an ICQ message.  This is a regular IM message if type == AIMConstants.AIM_ICQMSG_NORMAL.
     * <p/>
     * SMS, URL, and CONTACT type messages are handled with different methods.
     *
     * @param from    UserInfo
     * @param uin     UIN#
     * @param args    Args. See AIMConstants.AIM_ICQMSG_*
     * @param message Message.
     */
    public void incomingICQ(UserInfo from, int uin, int args, String message) {
    }


    /**
     * Received an ICQ URL message
     *
     * @param from        UserInfo
     * @param uin         UIN#
     * @param url         URL
     * @param description URL Description or null
     * @param massmessage Is this a mass-message?
     */
    public void receivedURL(UserInfo from, int uin, String url, String description, boolean massmessage) {
    }


    /**
     * Received a list of contacts in an ICQ message
     * <p/>
     * In the contact Map, the keys are UINs as Strings, and the values are the correcponding nick names.
     *
     * @param from        UserInfo
     * @param uin         UIN#
     * @param contact     HashMap
     * @param massmessage Is this a mass-message?
     */
    public void receivedContacts(UserInfo from, int uin, Map contact, boolean massmessage) {
    }


    /**
     * Received SMS message over ICQ
     *
     * @param from        UserInfo
     * @param uin         UIN#
     * @param message     ICQSMSMessage
     * @param massmessage Is this a mass-message?
     */
    public void receivedICQSMS(UserInfo from, int uin, ICQSMSMessage message, boolean massmessage) {
    }


    /**
     * The server sent us our buddy list.  (In general this list replaces everyone who
     * was already in the buddy list.)
     *
     * @param buddies Array of Buddy's
     */
    public void newBuddyList(Buddy[] buddies) {
    }


    /**
     * Called when a buddy goes offline.
     * <p/>
     * Note: it's possible that this method gets called with buddy == null.  Check for it!
     *
     * @param sn    ScreenName
     * @param buddy Buddy
     */
    public void buddyOffline(String sn, Buddy buddy) {
    	server.notifyLogout(sn);
    }


    /**
     * Called when a buddy goes online.
     * <p/>
     * The buddy's attributes are all updated before this is called.  If you cache them,
     * you must update them as well!
     * <p/>
     * Note: it's possible that this method gets called with buddy == null.  Check for it!
     *
     * @param sn    ScreenName
     * @param buddy Buddy
     */
    public void buddyOnline(String sn, Buddy buddy) {
    }


}
