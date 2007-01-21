package com.forgottenexpanse.laim;

public class ConnectionManager extends Thread {
	@SuppressWarnings("unused")
	private LAIM server;
    private Thread t;

	public void run() {
		while (true) {
			System.out.println("CM: Checking status");
			//first things first, monitor needs to be online always
			if (server.getMonitorStatus()) {
				//we only need to do this if we think the monitor is online
				server.checkMonitorStatus();
			}
			if (!server.getMonitorStatus()) {
				//the monitor is offline, get it back online
				System.out.println("CM: Monitor detected to be offline, trying to log it in.");
				server.loginMonitor();
				//stay in this loop until the login either failed or it was successful
				//TODO thread syncronization or something here?
				while (!server.getMonitorStatus() && !server.getMonitorFailedStatus()) {
					sleep(1);
				}
				//if it is still not logged in, sleep for 20 minutes before we try again
				if (!server.getMonitorStatus()) {
					System.out.println("CM: Monitor can't log in, sleeping for 20 minutes to try again");
					sleep(20 * 60);
					continue; //break the current loop and try again
				}
				System.out.println("CM: Monitor logged in");
			}
			
			//the monitor is online if we make it this far in the loop, so check the listener
			//make sure it should be online before we worry about it
			if (server.shouldBeOnline()) {
				System.out.println("CM: Listener should be online...");
				if (server.getListenerStatus()) {
					server.checkListenerStatus();
				}
				if (!server.getListenerStatus()) {
					//the listener is offline, get it back online
					System.out.println("CM: Listener detected to be offline, trying to log it in.");
					server.loginListener();
					//stay in this loop until the login either failed or it was successful
					while (!server.getListenerStatus() && !server.getListenerFailedStatus()) {
						sleep(1);
					}
					//if it is still not logged in, sleep for 20 minutes before we try again
					if (!server.getListenerStatus()) {
						System.out.println("CM: Listener can't log in.");
						System.out.println("Logging out monitor, then sleeping for 20 minutes to try again");
						server.stopMonitor();
						sleep(20 * 60);
						continue; //break the current loop and try again
					}
					System.out.println("CM: Listener logged in");
				}
			}
			System.out.println("CM: Sleeping connection monitor for five minutes");
			sleep(1 * 60); //sleep for five minutes before we try everything again
		}
    }
    
    @SuppressWarnings("static-access")
	private void sleep(int seconds) {
    	try {
        	t.sleep(1000 * seconds);
        } catch (Exception e) {
            System.err.println("Couldn't sleep?!?" + e.getMessage());
        }
    }

    public ConnectionManager(LAIM serverInstance) {
        server = serverInstance;
    }
}
