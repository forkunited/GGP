package org.ggp.dhtp.util;

import java.util.Map;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;

public class HeuristicOpponentFreedom extends HeuristicFreedom {
	private StateMachine machine;

	public HeuristicOpponentFreedom(StateMachine machine, Type type) {
		super(machine, type);
		this.machine = machine;
	}

	@Override
	public double evalState(Role role, MachineState state) throws MoveDefinitionException {
		Map<Role, Integer> roleIdxs = machine.getRoleIndices();
		double sum = 0;
		double count = 0;
		int playerRoleIdx = roleIdxs.get(role);

		for(Role otherRole : this.machine.getRoles()){
			if(roleIdxs.get(otherRole) != playerRoleIdx){
				count+=1;
				sum += super.evalState(otherRole, state);
			}
		}

		return (count == 0) ? 1.0 : 1.0 - sum/count; //take 1.0 - since we want to minimize
	}

}
