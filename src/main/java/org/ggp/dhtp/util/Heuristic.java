package org.ggp.dhtp.util;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public abstract class Heuristic {
	public abstract double evalState(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException;
}


