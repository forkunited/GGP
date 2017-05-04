package org.ggp.dhtp.learn;

import java.util.Arrays;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.util.Heuristic;

public class SAFeatureHeuristic extends SAFeature {
	private Heuristic heuristic;

	public SAFeatureHeuristic(Heuristic heuristic) {
		this.heuristic = heuristic;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public List<Double> compute(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return Arrays.asList(computeFirst(machine, role, state, move, timeoutDuration));
	}

	@Override
	public Double computeFirst(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		setTimeoutDuration(timeoutDuration);
		return this.heuristic.evalState(role, state);
	}
}
