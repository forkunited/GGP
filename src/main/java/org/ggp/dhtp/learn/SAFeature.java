package org.ggp.dhtp.learn;

import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.PhaseTimeoutException;

public abstract class SAFeature {
	protected static final boolean DEBUG = false;

	private long timeout;

	public abstract int size();
	public abstract List<String> toStrings();
	public abstract List<Double> compute(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;

	protected boolean printDebug(String message) {
		if (!SAFeature.DEBUG){
			return false;
		}

		DebugLog.output(message);
		return true;
	}

	protected void checkTimeout() throws PhaseTimeoutException{
		if(this.timeout > 0 && System.currentTimeMillis() > this.timeout){
			printDebug("Timed out!");
			throw new PhaseTimeoutException();
		}
	}

	protected void setTimeoutDuration(long timeoutDuration) {
		if (timeoutDuration <= 0)
			this.timeout = -1;
		this.timeout = System.currentTimeMillis() + timeoutDuration;
	}

	public Double computeFirst(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return compute(machine, role, state, move, timeoutDuration).get(0);
	}
}
