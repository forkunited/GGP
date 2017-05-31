package org.ggp.dhtp.util;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.dhtp.propnet.PropNetForwardPropUtils;

public class GoalProximityHeuristic extends Heuristic {
	public enum Mode {
		ONLY_TERMINAL,
		ONLY_NONTERMINAL,
		ALL
	}

	private StateMachine machine;
	private Mode mode;
	private double norm;
	private PropNet propNet;

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

	public void setPropNet(PropNet propNet){
		this.propNet = propNet;
	}


	@Override
	public String toString() {
		return "GoalProximity(" + this.mode + ")";
	}

	private double getGoalValue(MachineState state, Role role) throws GoalDefinitionException{
		if(this.propNet == null || this.machine.getGoal(state, role) > 0){
			return this.machine.getGoal(state, role);
		}
		PropNetForwardPropUtils.markBases(state, propNet);
		PropNetForwardPropUtils.forwardProp(propNet);

		 Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);
		 int bestPropVal = 0;
		 Proposition bestProp = null;
		 for(Proposition p : goalProps){
			 int propVal = Integer.parseInt(p.getName().get(1).toString());
			 if(propVal > bestPropVal){
				 bestProp = p;
				 bestPropVal = propVal;
			 }
		 }
		 if(bestProp != null){
			 Component inputC = bestProp.getSingleInput();
			 if(inputC instanceof And){
				 double pctTrue = ((double)((And)inputC).numTrue) / ((double)((And)inputC).getInputArray().size());
				 System.out.println("Pct true is:"+pctTrue);
				 System.out.println("Returning:"+pctTrue*bestPropVal);
				 return pctTrue * bestPropVal;
			 }
		 }
		 return this.machine.getGoal(state, role);
	}

	@Override
	public double evalState(Role role, MachineState state) throws GoalDefinitionException {
		if (this.mode == Mode.ONLY_TERMINAL)
			return !this.machine.isTerminal(state) ? 0.0 : getGoalValue(state, role) / this.norm;
		else if (this.mode == Mode.ONLY_NONTERMINAL)
			return this.machine.isTerminal(state) ? 0.0 : getGoalValue(state, role) / this.norm;
		else
			return getGoalValue(state, role) / this.norm;
	}
}
