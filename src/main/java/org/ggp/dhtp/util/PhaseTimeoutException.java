package org.ggp.dhtp.util;

public class PhaseTimeoutException extends Exception {
	public static void checkTimeout(long turnTimeout) throws PhaseTimeoutException{
		if(System.currentTimeMillis() > turnTimeout){
			DebugLog.output("Timed out!");
			throw new PhaseTimeoutException();
		}
	}
}
