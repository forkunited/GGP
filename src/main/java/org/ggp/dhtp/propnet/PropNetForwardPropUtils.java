package org.ggp.dhtp.propnet;

import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
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

	public static boolean markActionsInternal(List<GdlSentence> doeses, PropNet propNet) {
		boolean modified = false;
		BitSet inputs = propNet.getInputBits();

		for (int i = inputs.nextSetBit(0); i >= 0; i = inputs.nextSetBit(i + 1)) {
			// operate on index i here

			Proposition p = (Proposition) propNet.getComponentList().get(i);
			if (doeses.contains(p.getName())) {
				if(p.state == false){
					p.setValue(true);
					propNet.getComponentBits().set(p.propNetId, true);
					propNet.getUpdatedBits().set(p.propNetId, true);
					modified = true;
				}
			} else {
				if(p.state == true){
					p.setValue(false);
					propNet.getComponentBits().set(p.propNetId, false);
					propNet.getUpdatedBits().set(p.propNetId, true);
					modified = true;
				}
			}

			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}


		//if(propNet.updateBitSets()){
			//DebugLog.output("Bad update in markActionsInternal");
		//}

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
			boolean newState = prop.getPropValue();
			//if(newState != propNet.propGetComponentState(prop)){
			//	System.err.println(prop.toString());
			//	throw new UnsupportedOperationException("Boo");
			//}
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


	public static void forwardPropInternal(PropNet propNet) {
		// System.out.println("Start forward prop");

		Proposition init = propNet.getInitProposition();

		BitSet toProcess = propNet.getUpdatedBits();

		//toProcess.or(propNet.getBaseBits());
		//toProcess.or(propNet.getInputBits());
		//toProcess.or(propNet.getConstantBits());
		//toProcess.or(propNet.getTransitionBits());
		//toProcess.set(init.propNetId, true);

		while (!toProcess.isEmpty()) {
			int i = toProcess.nextSetBit(0);
			toProcess.clear(i);
			Component prop = propNet.getComponentList().get(i);
			//prop.inQueue = false;
			boolean newState = prop.getPropValue();
			//if(newState != propNet.propGetComponentState(prop)){
			//	throw new UnsupportedOperationException("Baa");
			//}
			if(propNet.getComponentBits().get(prop.propNetId) != newState){
				propNet.getComponentBits().set(prop.propNetId, newState);
			}
			if (!prop.initialized || prop.state != newState) {
				prop.state = newState;
				//if(prop.getOutputArray().size() == 0 && !prop.isInput && !prop.isBase && prop != init && prop instanceof Proposition && prop.getValue() != newState){
				//	((Proposition)prop).setValue(newState);
				//}
				propNet.getComponentBits().set(prop.propNetId, newState);
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
					} else if (c.numOutputs == 1 && c.numInputs == 1){
						while(c.numOutputs == 1 && c.numInputs == 1){ //skip forward through linear chain
							boolean st = c.getPropValue();
							if(propNet.getComponentBits().get(c.propNetId) != st){
								propNet.getComponentBits().set(c.propNetId, st);
							}
							toProcess.clear(c.propNetId);
							if (!c.initialized || c.state != st) {
								c.state = st;
								propNet.getComponentBits().set(c.propNetId, st);
								c = c.getSingleOutput();
							} else {
								break;
							}
						}
					}
					// System.out.println("Adding to processing queue");

					toProcess.set(c.propNetId);
				}
				prop.initialized = true;
			}
		}


		//if(propNet.updateBitSets(false)){
			//DebugLog.output("Had to update in forwardPropInternal");
		//}
	}


	public static boolean markBasesInternal(InternalMachineState state, PropNet propNet) {
	// TODO Auto-generated method stub
		/* Remove sentences to only include base */

		//if(propNet.updateBitSets(true)){
		//	DebugLog.output("Had to update in markBasesInternal");
		//}

		BitSet stateToSet = (BitSet)state.getBitSet().clone();
		BitSet baseMask = (BitSet)propNet.getBaseBits().clone();
		BitSet oldState = (BitSet)propNet.getComponentBits();
		BitSet currState = (BitSet)propNet.getComponentBits().clone();

		currState.and(baseMask);
		baseMask.and(stateToSet);

		BitSet changedBits = new BitSet(stateToSet.size());
		changedBits.or(currState);
		changedBits.xor(baseMask);


		boolean modified = false;


		for (int i = changedBits.nextSetBit(0); i >= 0; i = changedBits.nextSetBit(i + 1)) {
			// operate on index i here
			Component c = propNet.getComponentList().get(i);
			if(oldState.get(i) != c.state){
				System.out.println("Bug");
			}
			if(changedBits.get(i) != (c.state != stateToSet.get(i)) ){
				System.out.println("Bug");
			}
			if(c.state != stateToSet.get(i)){
				//System.out.println("Updating state at "+i);
				((Proposition)c).setValue(stateToSet.get(i));
				propNet.getComponentBits().set(i, stateToSet.get(i));
				propNet.getUpdatedBits().set(i, true);
				modified = true;
			}

			if (i == Integer.MAX_VALUE) {
				break; // or (i+1) would overflow
			}
		}


		return modified;
	}
}
