package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class GoalProximityHeuristic extends Heuristic{
	StateMachine machine;
	public GoalProximityHeuristic(StateMachine machine){
		this.machine = machine;
	}
	@Override
	public int evalState(Role role, MachineState state) {
		try {
			return machine.getGoal(state, role);
		}
		catch (Exception e){
			return 0;
		}
	}
}
