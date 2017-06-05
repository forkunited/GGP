package org.ggp.dhtp.util;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class HeuristicWeighted extends Heuristic {
	private List<Heuristic> heuristics;
	private List<Double> weights;

	public HeuristicWeighted(List<Heuristic> heuristics, List<Double> weights) {
		this.heuristics = heuristics;
		this.weights = weights;
	}

	@Override
	public synchronized double evalState(Role role, MachineState state)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {

		double h = 0.0;
		for (int i = 0; i < this.heuristics.size(); i++) {
			h += this.weights.get(i) * this.heuristics.get(i).evalState(role, state);
		}
		return h;
	}
}


