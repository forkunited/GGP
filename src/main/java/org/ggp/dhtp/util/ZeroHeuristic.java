package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public class ZeroHeuristic extends Heuristic{
	@Override
	public double evalState(Role role, MachineState state) {
		return 0.0;
	}
}
