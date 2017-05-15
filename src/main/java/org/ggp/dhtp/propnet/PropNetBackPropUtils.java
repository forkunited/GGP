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
			assert("true".equals(sentence.getName()));
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
			if (doeses.contains(p.getName())) {
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
		Proposition Init = propNet.getInitProposition();

		return propMarkPInternal(prop, Base, Input, Init);
	}

    private static boolean propMarkPInternal(Component prop, Set<Proposition> Base, Set<Proposition> Input, Proposition Init) {
    	if (Base.contains(prop) ) {
//    		System.out.println("Base prop: ");
//    		System.out.println(prop.toString());
    		return prop.getValue();
    	}  else if ( Input.contains(prop)){
//    		System.out.println("Input prop: ");
//    		System.out.println(prop.toString());
    		return prop.getValue();
    	} else if (prop == Init) {
//    		System.out.println("Init prop: ");
//    		System.out.println(prop.toString());
    		return false;
        } else if (prop instanceof Proposition) {
//    		System.out.println("View prop: ");
//    		System.out.println(prop.toString());
    		return propMarkPInternal(prop.getSingleInput(), Base, Input, Init);
    	} else if (prop instanceof And) {
//    		System.out.println("Conjunction: ");
//    		System.out.println(prop.toString());
    		return propMarkPAnd(prop, Base, Input, Init);
    	} else if (prop instanceof Or) {
//    		System.out.println("Disjunction: ");
//    		System.out.println(prop.toString());
    		return propMarkPOr(prop, Base, Input, Init);
    	} else if (prop instanceof Not) {
//    		System.out.println("Inversion: ");
//    		System.out.println(prop.toString());
    		return propMarkPNot(prop, Base, Input, Init);
    	} else if (prop instanceof Transition) {
//    		System.out.println("Transition: ");
//    		System.out.println(prop.toString());
    		assert(false);  // This branch should never be taken, as base props are base case
    		return false;
    	} else {
//    		System.out.println("Constant prop: ");
//    		System.out.println(prop.toString());
    		assert(prop instanceof Constant);
    		return prop.getValue();
    	}

    }

    private static boolean propMarkPOr(Component prop, Set<Proposition> Base, Set<Proposition> Input, Proposition Init) {
    	for (Component component: prop.getInputs()) {
    		if (propMarkPInternal(component, Base, Input, Init)) {
    			return true;
    		}
    	}
    	return false;
    }

    private static boolean propMarkPNot(Component prop, Set<Proposition> Base, Set<Proposition> Input, Proposition Init) {
    	return !propMarkPInternal(prop.getSingleInput(), Base, Input, Init);
    }

    private static boolean propMarkPAnd(Component prop, Set<Proposition> Base, Set<Proposition> Input, Proposition Init) {
    	for (Component component: prop.getInputs()){
    		if (!propMarkPInternal(component, Base, Input, Init)) {
    			return false;
    		}
    	}
    	return true;
    }

}
