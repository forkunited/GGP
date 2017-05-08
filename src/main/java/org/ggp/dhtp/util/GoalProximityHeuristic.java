package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;

public class GoalProximityHeuristic extends Heuristic {
	public enum Mode {
		ONLY_TERMINAL,
		ONLY_NONTERMINAL,
		ALL
	}

	private StateMachine machine;
	private Mode mode;
	private double norm;

	public GoalProximityHeuristic(StateMachine machine) {
		this(machine, Mode.ALL, 100.0);
	}

	public GoalProximityHeuristic(StateMachine machine, Mode mode) {
		this(machine, mode, 100.0);
	}

	public GoalProximityHeuristic(StateMachine machine, Mode mode, double norm) {
		this.machine = machine;
		this.mode = mode;
		this.norm = norm;
	}

	@Override
	public String toString() {
		return "GoalProximity(" + this.mode + ")";
	}

	@Override
	public double evalState(Role role, MachineState state) throws GoalDefinitionException {
		if (this.mode == Mode.ONLY_TERMINAL)
			return !this.machine.isTerminal(state) ? 0.0 : this.machine.getGoal(state, role) / this.norm;
		else if (this.mode == Mode.ONLY_NONTERMINAL)
			return this.machine.isTerminal(state) ? 0.0 : this.machine.getGoal(state, role) / this.norm;
		else
			return this.machine.getGoal(state, role) / this.norm;
	}
}
