package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public class GoalProximityHeuristic extends Heuristic {
	private StateMachine machine;
	private double norm;

	public GoalProximityHeuristic(StateMachine machine) {
		this(machine, 100.0);
	}

	public GoalProximityHeuristic(StateMachine machine, double norm) {
		this.machine = machine;
		this.norm = norm;
	}

	@Override
	public double evalState(Role role, MachineState state) throws GoalDefinitionException {
		return this.machine.getGoal(state, role) / this.norm;
	}
}
