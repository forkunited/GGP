package org.ggp.dhtp.propnet;

import java.util.BitSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.components.And;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Not;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.architecture.components.Transition;
import org.ggp.base.util.statemachine.Role;

public class PropWyatt {
	/* Old structures kept around for compatibility */
	/* End old structures */

	private LinkedList<Component> components;
	private Map<GdlSentence, Integer> propositionIndex;
	private Map<Component, Integer> componentIndex;
	private List<Role> roleList;

	private BitSet componentVector;
	private BitSet baseVector;
	private BitSet goalVector;
	private BitSet inputVector;
	private BitSet transitionVector;
	private BitSet constantVector;
	private BitSet andVector;
	private BitSet orVector;
	private BitSet notVector;
	private int terminalIndex;
	private int initIndex;

	private List<Integer> toProcess;
	private int[] andCounters;
	private int[] orCounters;
	private int[] numberInputs;
	private BitSet[] inputArray;
	private BitSet[] outputArray;
	private Map<Role,BitSet> roleGoalMap;
	private Map<Role,BitSet> roleLegalMap;

	public PropWyatt(List<Role> roles, Set<Component> components) {
		this.components = new LinkedList<Component>(components);
		this.roleList = roles;
		this.toProcess = new LinkedList<Integer>();
		this.propositionIndex = new HashMap<GdlSentence, Integer>();
		this.componentIndex = new HashMap<Component, Integer>();
		this.roleGoalMap = new HashMap<Role, BitSet>();
		for (Role r : roles) {
			this.roleGoalMap.put(r, new BitSet(this.components.size()));
			this.roleLegalMap.put(r, new BitSet(this.components.size()));
		}

	}

	/* Sets propnet internals to array representation for lifetime of datastructure */
	public void freezeProp() {
		/* Map all proposition names to indexes in the component vector */
		int i = 0;
		Proposition p;
		for (Component c : this.components) {
			if (c instanceof Proposition) {
				p = (Proposition) c;
				this.propositionIndex.put(p.getName(), i);
			}
			i++;
		}

		/* Set size of all vectors */
		this.baseVector = new BitSet(this.components.size());
		this.componentVector = new BitSet(this.components.size());
		this.inputVector = new BitSet(this.components.size());
		this.transitionVector = new BitSet(this.components.size());
		this.constantVector = new BitSet(this.components.size());
		this.andVector = new BitSet(this.components.size());
		this.orVector = new BitSet(this.components.size());
		this.andCounters = new int[this.components.size()];
		this.orCounters = new int[this.components.size()];
		this.numberInputs = new int[this.components.size()];
		this.inputArray = new BitSet[this.components.size()];
		this.outputArray = new BitSet[this.components.size()];
		/* Initialize all internal structures based on component*/
		i = 0;
		for (Component c: this.components) {
			/* General Component Initializations */
			this.componentIndex.put(c, i);
			this.inputArray[i] = new BitSet(this.components.size());
			this.outputArray[i] = new BitSet(this.components.size());
			this.numberInputs[i] = c.getInputs().size();

			/* And Initializations */
			if (c instanceof And) {
				this.andVector.set(i);
				// Counter
			}

			/* Or Initializations */
			if (c instanceof Or) {
				this.orVector.set(i);
			}

			/* Not Initializations */
			if (c instanceof Not) {
				this.notVector.set(i);
			}

			/* Proposition initializations */
			if (c instanceof Proposition) {
				p = (Proposition) c;
				initPropFreeze(p, i);
			}

			/* Constant Initializations */
			if (c instanceof Constant) {
				this.constantVector.set(i);
			}

			/* Transition Initializations */
			if (c instanceof Transition) {
				this.transitionVector.set(i);
			}

			i++;
		}

		/* Now that each component index is recorded update input and output arrays */
		i = 0;
		for (Component c: this.components) {
			for (Component inputC: c.getInputArray()) {
				this.inputArray[i].set(this.componentIndex.get(inputC));
			}
			for (Component outputC: c.getOutputArray()) {
				this.outputArray[i].set(this.componentIndex.get(outputC));
			}
		}
		i++;
	}

	private void initPropFreeze(Proposition p, int i) {
		/* Base initializations */
		Component singleIn = p.getSingleInput();
		if (singleIn instanceof Transition) {
			this.baseVector.set(i);
		}

		/* Input Initializations */
		if (p.getName() instanceof GdlRelation) {
			GdlRelation relation = (GdlRelation) p.getName();
			if (relation.getName().getValue().equals("does")) {
				inputVector.set(i);
			}

			/* Legal Initializations */
			if (relation.getName().getValue().equals("legal")) {
				GdlConstant name = (GdlConstant) relation.get(0);
				Role r = new Role(name);
				if (!roleLegalMap.containsKey(r)) {
					roleLegalMap.put(r, new BitSet(this.components.size()));
				}
				roleLegalMap.get(r).set(i);
			}

			/* Goal Initializations */
			if (relation.getName().getValue().equals("goal")) {
				Role theRole = new Role((GdlConstant) relation.get(0));
				if (!this.roleGoalMap.containsKey(theRole)) {
					roleGoalMap.put(theRole, new BitSet(this.components.size()));
				}
				roleGoalMap.get(theRole).set(i);
			}

			/* Init and terminal initializations */
			GdlConstant constant = ((GdlProposition) p.getName()).getName();
			if (constant.getValue().toUpperCase().equals("INIT")) {
				this.initIndex = i;
			}
			if (constant.getValue().equals("terminal")) {
				this.terminalIndex = i;
			}
		}
	}

}
