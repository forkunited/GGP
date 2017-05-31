package org.ggp.dhtp.util;

import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Or;
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
			// System.out.println("Checking "+bestProp);
			 Component inputC = bestProp;
			 while((inputC instanceof Proposition )&& !inputC.isBase && !inputC.isInput){
				 //System.out.println("Rewinding over "+inputC.toString());
				 inputC = inputC.getSingleInput();
				 //System.out.println("Rewound to "+inputC);
			 }

			 if(inputC instanceof Or){
				 double estVal = 0.0;
				 for(Component input : inputC.getInputs()){
					 //System.out.println("Checking OR input:"+input.toString());
					 double inputVal = getComponentContrib(input.getInputs(), bestPropVal, 0, 3, input.getValue());
					 if(inputVal > estVal){
						 estVal = inputVal;
					 }
					 break;
				 }
				 //System.out.println("Or Est Val:"+estVal);
				 return estVal;
			 }else if(inputC instanceof And){
				 double estVal = getComponentContrib(inputC.getInputs(), bestPropVal, 0, 3, inputC.getValue());
				 //System.out.println("And Est Val:"+estVal);
				 return estVal;
			 } else {
				// System.out.println("Could not calc: input C is "+inputC);
			 }
		 }
		 return this.machine.getGoal(state, role);
	}

	private double getComponentContrib(Set<Component> set, double bestPropVal, int depth, int depthLimit, boolean parentWasSet){
		if(depth >= depthLimit){
			//System.out.println("Hit max depth");
			return parentWasSet ? bestPropVal : 0.0;
		}
		double indivContrib = bestPropVal / (double)set.size();
		double total = 0.0;
		for(Component c : set){
			//System.out.println("Depth:"+depth+" Checking "+c.toString());
			while(c instanceof Proposition && !c.isInput && !c.isBase){
				c = c.getSingleInput();
			}
			if(c instanceof And && !c.getValue()){
				total += getComponentContrib(c.getInputs(), indivContrib, depth+1, depthLimit, false);
			} else {
				if(c.getValue()){
					total += indivContrib;
					//System.out.println("Set:"+c.toString());
				} else {
					//System.out.println("Not set:"+c.toString());
				}
			}

		}
		return total;
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
