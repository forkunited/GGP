package org.ggp.dhtp.util;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public abstract class Heuristic {
	public abstract int evalState(Role role, MachineState state);
}


