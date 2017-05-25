package org.ggp.dhtp.propnet;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.statemachine.MachineState;

public class PropNetForwardPropUtils {
	/* Update the assignment of a propnet given the new state */
	public static boolean markBases(MachineState state, PropNet propNet) {

		/* Remove sentences to only include base */
		Set<GdlSentence> baseSentences = state
				.getContents(); /* Can probably optimize this clone */

		/* Set propnet base values to match state */
		boolean modified = false;
		for (Component c : propNet.getBaseComponentSet()) {
			Proposition p = (Proposition) c;
			if (baseSentences.contains(p.getName())) {
				if(p.state == false){
					p.setValue(true);
					modified = true;
				}
			} else {
				if(p.state == true){
					p.setValue(false);
					modified = true;
				}
			}
		}
		return modified;
	}

	/* Update the assignment of a propnet given the new moves */
	public static boolean markActions(List<GdlSentence> doeses, PropNet propNet) {
		boolean modified = false;
		for (Component c : propNet.getInputComponentSet()) {
			Proposition p = (Proposition)c;
			if (doeses.contains(p.getName())) {
				if(p.state == false){
					p.setValue(true);
					modified = true;
				}
			} else {
				if(p.state == true){
					p.setValue(false);
					modified = true;
				}
			}
		}
		return modified;
	}

	/* Read the assignment of a proposition */

	public static boolean propMarkP(Component prop, PropNet propNet) {
		return prop.state;
	}

	public static void forwardProp(PropNet propNet) {
		// System.out.println("Start forward prop");
		Proposition init = propNet.getInitProposition();

		LinkedList<Component> toProcess = new LinkedList<Component>();
		toProcess.addAll(propNet.getInputComponentSet());
		toProcess.addAll(propNet.getBaseComponentSet());
		toProcess.add(init);
		toProcess.addAll(propNet.getConstantComponents());
		toProcess.addAll(propNet.getTransitionComponents());

		while (!toProcess.isEmpty()) {
			Component prop = toProcess.poll();
			prop.inQueue = false;
			boolean newState = propGetPInternal(prop, init);
			if (!prop.initialized || prop.state != newState) {
				prop.state = newState;
				for (Component c : prop.getOutputArray()) {
					int delta = prop.state == true ? 1 : -1;
					if (c instanceof And) {

						if (!prop.initialized) {
							int actualNumTrue = 0;
							for (Component in : c.getInputArray()) {
								if (in.state) {
									actualNumTrue += 1;
								}
							}
							((And) c).numTrue = actualNumTrue;
						} else {
							((And) c).numTrue += delta;
						}
					} else if (c instanceof Or) {
						if (!prop.initialized) {
							int actualNumTrue = 0;
							for (Component in : c.getInputArray()) {
								if (in.state) {
									actualNumTrue += 1;
								}
							}
							((Or) c).numTrue = actualNumTrue;
						} else {
							((Or) c).numTrue += delta;
						}
					}
					// System.out.println("Adding to processing queue");
					if(!c.inQueue){
						toProcess.add(c);
						c.inQueue = true;
					}
				}
				prop.initialized = true;
			}
		}
	}

	private static boolean propGetPInternal(Component prop, Proposition Init) {
		if (prop.isBase) {
			// if(prop.getValue()){
			// System.out.println("Base prop: ");
			// System.out.println(prop.getValue() + prop.toString());
			// }
			return prop.getValue();
		} else if (prop.isInput) {
			// System.out.println("Input prop: ");
			// System.out.println(prop.toString());
			return prop.getValue();
		} else if (prop == Init) {
			// System.out.println("Init prop: ");
			// System.out.println(prop.toString());
			return prop.getValue();
		} else if (prop instanceof Proposition) {
			// System.out.println("View prop: ");
			// System.out.println(prop.toString());
			return prop.getSingleInput().state;
		} else if (prop instanceof And) {
			// System.out.println("Conjunction: ");
			// System.out.println(prop.toString());
			return ((And) prop).numTrue == prop.getInputArray().size();
		} else if (prop instanceof Or) {
			// System.out.println("Disjunction: ");
			// System.out.println(prop.toString());
			return ((Or) prop).numTrue != 0;
		} else if (prop instanceof Not) {
			// System.out.println("Inversion: ");
			// System.out.println(prop.toString());
			return !prop.getSingleInput().state;
		} else if (prop instanceof Transition) {
			// System.out.println("Transition: ");
			// System.out.println(prop.toString());
			// assert(false); // This branch should never be taken, as base
			// props are base case
			return prop.getValue();
		} else {
			// System.out.println("Constant prop: ");
			// System.out.println(prop.toString());
			assert (prop instanceof Constant);
			return prop.getValue();
		}

	}
}
