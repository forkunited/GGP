package org.ggp.dhtp.propnet;


import java.util.HashSet;
import java.util.Iterator;
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

public class PropNetBackPropUtils {
	/* Update the assignment of a propnet given the new state */
	public static void markBases(MachineState state, PropNet propNet) {
		/* Remove sentences to only include base */
		Set<GdlSentence> baseSentences = state.clone().getContents(); /* Can probably optimize this clone */
		for (Iterator<GdlSentence> i = baseSentences.iterator(); i.hasNext();) {
			GdlSentence sentence = i.next();
			if (!"base".equals(sentence.getName().toString())) {
				i.remove();
			}
		}
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
			if (doeses.contains(p)) {
				p.setValue(true);
			} else {
				p.setValue(false);
			}
		}
	}

	/* Read the assignment of a proposition */

	public static boolean propMarkP(Component prop, PropNet propNet) {
		HashSet<Proposition> Base = new HashSet(propNet.getBasePropositions().values());
		HashSet<Proposition> Input = new HashSet(propNet.getInputPropositions().values());

		return propMarkPInternal(prop, Base, Input);
	}

    private static boolean propMarkPInternal(Component prop, Set<Proposition> Base, Set<Proposition> Input) {
    	if (Base.contains(prop) || Input.contains(prop)) {
    		return prop.getValue();
    	} else if (prop instanceof Proposition) {
    		return propMarkPInternal(prop.getSingleInput(), Base, Input);
    	} else if (prop instanceof And) {
    		return propMarkPAnd(prop, Base, Input);
    	} else if (prop instanceof Or) {
    		return propMarkPOr(prop, Base, Input);
    	} else if (prop instanceof Not) {
    		return propMarkPNot(prop, Base, Input);
    	} else if (prop instanceof Transition) {
    		assert(false);  // This branch should never be taken, as base props are base case
    		return false;
    	} else {
    		assert(prop instanceof Constant);
    		return prop.getValue();
    	}

    }

    private static boolean propMarkPOr(Component prop, Set<Proposition> Base, Set<Proposition> Input) {
    	for (Component component: prop.getInputs()) {
    		if (propMarkPInternal(component, Base, Input)) {
    			return true;
    		}
    	}
    	return false;
    }

    private static boolean propMarkPNot(Component prop, Set<Proposition> Base, Set<Proposition> Input) {
    	return !propMarkPInternal(prop.getSingleInput(), Base, Input);
    }

    private static boolean propMarkPAnd(Component prop, Set<Proposition> Base, Set<Proposition> Input) {
    	for (Component component: prop.getInputs()){
    		if (!propMarkPInternal(component, Base, Input)) {
    			return false;
    		}
    	}
    	return true;
    }

}
