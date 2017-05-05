package org.ggp.dhtp.util;

public class DebugLog {
	private static final boolean DEBUG = true;

	public static void output(String str) {
		if (!DebugLog.DEBUG)
			return;
		System.err.println("[" + System.currentTimeMillis() + "] " + str);
	}
}
