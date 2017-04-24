package org.ggp.dhtp.util;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class HeuristicFreedom extends Heuristic {
	public enum Type {
		FOCUS,
		MOBILITY
	}

	private StateMachine machine;
	private Type type;

	public HeuristicFreedom(StateMachine machine, Type type) {
		this.machine = machine;
		this.type = type;
	}

	@Override
	public double evalState(Role role, MachineState state) throws MoveDefinitionException {
		int stateMoves = this.machine.getLegalMoves(state, role).size();
		int roleMoves = this.machine.findActions(role).size();
		double mobility = stateMoves/roleMoves;

		if (this.type == Type.MOBILITY) {
			return mobility;
		} else {
			return 1.0 - mobility;
		}
	}
}

