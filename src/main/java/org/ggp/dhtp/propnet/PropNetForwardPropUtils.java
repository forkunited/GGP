package org.ggp.dhtp.propnet;

import java.util.HashSet;
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
	public static void markBases(MachineState state, PropNet propNet) {

		/* Remove sentences to only include base */
		Set<GdlSentence> baseSentences = state
				.getContents(); /* Can probably optimize this clone */

		/* Set propnet base values to match state */
		for (Proposition p : propNet.getBasePropositions().values()) {
			if (baseSentences.contains(p.getName())) {
				p.setValue(true);
			} else {
				p.setValue(false);
			}
		}
	}

	/* Update the assignment of a propnet given the new moves */
	public static void markActions(List<GdlSentence> doeses, PropNet propNet) {

		for (Proposition p : propNet.getInputPropositions().values()) {
			if (doeses.contains(p.getName())) {
				p.setValue(true);
			} else {
				p.setValue(false);
			}
		}
	}

	/* Read the assignment of a proposition */

	public static boolean propMarkP(Component prop, PropNet propNet) {
		return prop.state;
	}

	public static void forwardProp(PropNet propNet) {
		//System.out.println("Start forward prop");
		HashSet<Proposition> base = new HashSet<Proposition>(propNet.getBasePropositions().values());
		HashSet<Proposition> input = new HashSet<Proposition>(propNet.getInputPropositions().values());
		Proposition init = propNet.getInitProposition();

		LinkedList<Component> toProcess = new LinkedList<Component>();
		//toProcess.addAll((Collection<? extends Component>) base);
		//toProcess.addAll((Collection<? extends Component>) input);
		toProcess.add(init);
		for (Component c : propNet.getComponents()) {
			if (base.contains(c) || input.contains(c) || c instanceof Constant || c instanceof Transition) {
				toProcess.add(c);
			}
		}
		//toProcess.addAll(propNet.getComponents());

		while (!toProcess.isEmpty()) {
			Component prop = toProcess.poll();

			boolean newState = propGetPInternal(prop, base, input, init);
			if (!prop.initialized || prop.state != newState) {
				prop.state = newState;
				for (Component c : prop.getOutputs()) {
					// int delta = prop.state == true ? 1 : -1;
					if (c instanceof And) {

						// if (!prop.initialized) {
						int actualNumTrue = 0;
						for (Component in : c.getInputs()) {
							if (in.state) {
								actualNumTrue += 1;
							}
						}
						((And) c).numTrue = actualNumTrue;
						// } else {
						// ((And) c).numTrue += delta;
						// }
					} else if (c instanceof Or) {
						// if (!prop.initialized) {
						int actualNumTrue = 0;
						for (Component in : c.getInputs()) {
							if (in.state) {
								actualNumTrue += 1;
							}
						}
						((Or) c).numTrue = actualNumTrue;
						// } else {
						// ((Or) c).numTrue += delta;
						// }
					}
					// System.out.println("Adding to processing queue");
					toProcess.add(c);
				}
				prop.initialized = true;
			}
		}
	}

	private static boolean propGetPInternal(Component prop, Set<Proposition> Base, Set<Proposition> Input,
			Proposition Init) {
		if (Base.contains(prop)) {
			//if(prop.getValue()){
			//System.out.println("Base prop: ");
			//System.out.println(prop.getValue() + prop.toString());
			//}
			return prop.getValue();
		} else if (Input.contains(prop)) {
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
			return ((And) prop).numTrue == prop.getInputs().size();
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
			//System.out.println("Constant prop: ");
			// System.out.println(prop.toString());
			assert (prop instanceof Constant);
			return prop.getValue();
		}

	}
}
