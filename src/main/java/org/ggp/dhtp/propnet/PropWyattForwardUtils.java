package org.ggp.dhtp.propnet;

import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.statemachine.MachineState;

public class PropWyattForwardUtils {


	public static void forwardProp(PropWyatt propNet) {
		BitSet toProcess = propNet.getToProcess();
		BitSet components = propNet.getComponents();
		BitSet initialized = propNet.getInitializedVector();
		while (!toProcess.isEmpty()) {
			int i = toProcess.nextSetBit(0);
			toProcess.clear(i);
			/* Check if the value has been updated */
			boolean oldVal = components.get(i);
			boolean newVal = getNewValue(propNet, i);
			if (oldVal != newVal || !initialized.get(i)) { /* Differential propagation */
				components.flip(i); // update components vector to reflect new value
				int delta = newVal == true ? 1 : -1;
				BitSet outputComponents = propNet.getComponentOutputs(i);

				/* Propagate changes to the outputs */
				BitSet andVector = propNet.getAndVector();
				BitSet orVector = propNet.getOrVector();
				int outputIdx = outputComponents.nextSetBit(0);
				while (outputIdx >= 0) {
					if (andVector.get(outputIdx)) {
						if (!initialized.get(i)) {
							BitSet incidentMask = (BitSet)propNet.getComponentInputs(outputIdx).clone();
							incidentMask.and(components);
							propNet.incrAndCounter(outputIdx, incidentMask.cardinality());
						} else {
							propNet.incrAndCounter(outputIdx, delta);
						}
					} else if (orVector.get(outputIdx)) {
						if (!initialized.get(i)) {
							BitSet incidentMask = (BitSet)propNet.getComponentInputs(outputIdx).clone();
							incidentMask.and(components);
							propNet.incrOrCounter(outputIdx,  incidentMask.cardinality());
						} else {
							propNet.incrOrCounter(outputIdx, delta);
						}
					} // TODO Add the optimization used in the other forward prop here
					toProcess.set(outputIdx);
					/* Get the next output index avoiding exceptions */
					if (outputIdx == outputComponents.size() - 1) {
						outputIdx = -1;
					}
					outputIdx = outputComponents.nextSetBit(outputIdx + 1);
				}
				initialized.set(i);
			}
		}
	}

	private static boolean getNewValue(PropWyatt propNet, int i) {
		BitSet componentVector = propNet.getComponents();
		BitSet propVector = propNet.getPropVector();
		/* Handle proposition */
		if (propVector.get(i)) {
			BitSet baseVector = propNet.getBaseVector();

			BitSet inputVector = propNet.getInputVector();
			/* Value is already set */
			if (baseVector.get(i) || inputVector.get(i)) {
				return componentVector.get(i);
			}

			/* Read the single input */
			BitSet componentInputs = propNet.getComponentInputs(i);
			if (componentInputs.cardinality() == 0) {
				/* Why hello there init proposition! */
				return componentVector.get(i);
			}
			assert(componentInputs.cardinality() == 1);
			int inputIdx = componentInputs.nextSetBit(0);
			return componentVector.get(inputIdx);
		}

		/* Handle constant and transition (which doesn't update unless we are querying next state) */
		BitSet constantVector = propNet.getConstantVector();
		BitSet transitionVector = propNet.getTransitionVector();

		if (constantVector.get(i) || transitionVector.get(i)) {
			return componentVector.get(i);
		}

		/* Handle and */
		BitSet andVector = propNet.getAndVector();
		if (andVector.get(i)) {
			int numTrue = propNet.getAndCounter(i);
			int totalNum = propNet.getTotalInputs(i);
			return numTrue == totalNum;
		}

		/* Handle or */
		BitSet orVector = propNet.getOrVector();
		if (orVector.get(i)) {
			int numTrue = propNet.getOrCounter(i);
			return numTrue != 0;
		}

		/* Handle not */
		BitSet notVector = propNet.getNotVector();
		assert(notVector.get(i));
		BitSet componentInputs = propNet.getComponentInputs(i);
		assert(componentInputs.cardinality() == 1);
		int inputIdx = componentInputs.nextSetBit(0);
		return !componentVector.get(inputIdx);
	}


	public static boolean markActions(List<GdlSentence> doeses, PropWyatt propNet) {
		boolean modified = false;
		Map<Integer, GdlSentence> indexToProp = propNet.getIndexPropMap();
		BitSet componentValues = propNet.getComponents();
		BitSet inputMask = propNet.getInputVector();
		int inputIndex = inputMask.nextSetBit(0);
		BitSet toProcess = propNet.getToProcess();

		while (inputIndex >= 0) {
			GdlSentence sentence = indexToProp.get(inputIndex);
			/* Set actions */
			if (doeses.contains(sentence)) {
				if(!componentValues.get(inputIndex)){
					componentValues.set(inputIndex);
					toProcess.set(inputIndex);
					modified = true;
				}
			} else {
				if(componentValues.get(inputIndex)){
					componentValues.flip(inputIndex);
					toProcess.set(inputIndex);
					modified = true;
				}
			}


			/* Get the next output index avoiding exceptions */
			if (inputIndex == inputMask.size() - 1) {
				inputIndex = -1;
			}
			inputIndex = inputMask.nextSetBit(inputIndex + 1);

		}
		return modified;
	}

	public static boolean markActionsInternal(List<GdlSentence> doeses, PropWyatt propNet) {
		BitSet oldInputs = (BitSet)propNet.getComponents().clone();
		BitSet newState = new BitSet(oldInputs.size());
		Map<GdlSentence, Integer> propIndexMap = propNet.getPropositionMap();
		for (GdlSentence does : doeses) {
			int bitIndex = propIndexMap.get(does);
			newState.set(bitIndex);
		}

		BitSet inputMask = propNet.getInputVector();
		BitSet changedComponents = new BitSet(newState.size());
		oldInputs.and(inputMask);
		newState.and(inputMask);
		/* Get changed bits by setting to high any bases that differ in old and new */
		changedComponents.or(oldInputs);
		changedComponents.xor(newState);
		boolean modified = false;

		/* Add changed bases to toProcess */
		int i = changedComponents.nextSetBit(0);
		BitSet toProcess = propNet.getToProcess();
		while (i >= 0) {
			modified = true;
			toProcess.set(i);
			propNet.getComponents().set(i, newState.get(i)); // Set input in state


			/* Get the next output index avoiding exceptions */
			if (i == changedComponents.size() - 1) {
				i = -1;
			}
			i = changedComponents.nextSetBit(i + 1);
		}
		return modified;
	}


	public static boolean markBases(MachineState state, PropWyatt propNet) {

		/* Remove sentences to only include base */
		Set<GdlSentence> baseSentences = state.getContents(); /* Can probably optimize this clone */

		/* Set propnet base values to match state */
		boolean modified = false;
		Map<Integer, GdlSentence> propToIndex = propNet.getIndexPropMap();
		BitSet componentValues = propNet.getComponents();
		BitSet baseMask = propNet.getBaseVector();
		int baseIndex = baseMask.nextSetBit(0);
		BitSet toProcess = propNet.getToProcess();
		while (baseIndex >= 0) {
			GdlSentence sentence = propToIndex.get(baseIndex);
			/* Set bases */
			if (baseSentences.contains(sentence)) {
				if(!componentValues.get(baseIndex)){
					componentValues.set(baseIndex);
					toProcess.set(baseIndex);
					modified = true;
				}
			} else {
				if(componentValues.get(baseIndex)){
					componentValues.flip(baseIndex);
					toProcess.set(baseIndex);
					modified = true;
				}
			}

			/* Get the next output index avoiding exceptions */
			if (baseIndex == baseMask.size() - 1) {
				baseIndex = -1;
			}
			baseIndex = baseMask.nextSetBit(baseIndex + 1);

		}

		return modified;
	}



	public static boolean markBasesInternal(InternalMachineState state, PropWyatt propNet) {
		BitSet newState = (BitSet)state.getBitSet().clone(); /* New state provided */
		BitSet oldBases = (BitSet)propNet.getComponents().clone();
		BitSet baseMask = propNet.getBaseVector();
		BitSet changedComponents = new BitSet(newState.size());
		oldBases.and(baseMask);
		newState.and(baseMask);

		/* Get changed bits by setting to high any bases that differ in old and new */
		changedComponents.or(oldBases);
		changedComponents.xor(newState);
		boolean modified = false;

		/* Add changed bases to toProcess */
		int i = changedComponents.nextSetBit(0);
		BitSet toProcess = propNet.getToProcess();
		while (i >= 0) {
			modified = true;
			toProcess.set(i);
			propNet.getComponents().set(i, newState.get(i)); // Set base in state

			/* Get the next output index avoiding exceptions */
			if (i == changedComponents.size() - 1) {
				i = -1;
			}
			i = changedComponents.nextSetBit(i + 1);
		}
		return modified;
	}

}
