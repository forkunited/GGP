package org.ggp.dhtp.util;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public abstract class Bounder {
	public abstract boolean shouldExpand(Role role, MachineState state, int level);
}


