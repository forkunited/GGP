package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;



public class MCSHeuristic extends Heuristic {
	private long finishTime;
	private int maxDepthCharge;
	private double depthChargeResult;
	private int depthChargeCount;
	private double resultSum;
	private StateMachine machine;
	private double norm = 100.0;

	public MCSHeuristic(StateMachine machine) {
		this.machine = machine;
		this.maxDepthCharge = 10;
	}

	@Override
	public void preEval(long finishTime) {
		this.finishTime = finishTime;
	}

	@Override
	public double evalState(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException  {
		depthChargeCount = 0;
		depthChargeResult = -1;
		while (depthChargeCount < maxDepthCharge) {
			depthCharge(role, state);

			if (depthChargeResult >= 0) { /* The depth charge wrote back a result */
				resultSum += depthChargeResult;
				depthChargeResult = -1;
				depthChargeCount += 1;
			}

			if ( System.currentTimeMillis() > finishTime) { /* This state eval has timed out */
				return resultSum / (norm *depthChargeCount);
			}
		}
		return resultSum / (norm * depthChargeCount);

	}

	/* Recursive function that simulates random game moves before reaching a terminal state */
	public void depthCharge(Role role, MachineState state) throws GoalDefinitionException, TransitionDefinitionException, MoveDefinitionException {
		if (System.currentTimeMillis() > finishTime) { /* Depth charge terminates early */
			return;
		}

		if (machine.isTerminal(state)) { /* Depth charge finished */
			depthChargeResult = machine.getGoal(state, role);
			return;
		}

		MachineState newState = machine.getNextState(state, machine.getRandomJointMove(state));
		depthCharge(role, newState);

	}
}
