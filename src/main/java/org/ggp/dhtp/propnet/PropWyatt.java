package org.ggp.dhtp.propnet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.gdl.grammar.GdlTerm;
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

	private ArrayList<Component> components;
	private Map<GdlSentence, Integer> propositionIndex;
	private Map<Integer, GdlSentence> indexProposition;
	private Map<Component, Integer> componentIndex;
	private List<Role> roleList;
	private Map<Proposition, Proposition> legalInputMap;
	private Map<GdlSentence, Proposition> inputPropositions;
	private Map<Role, Set<Proposition>> legalPropositions;

	private BitSet componentVector;  /* stores the truth value of component at index */
	private BitSet baseVector;
	private BitSet goalVector;
	private BitSet inputVector;
	private BitSet transitionVector;
	private BitSet constantVector;
	private BitSet andVector;
	private BitSet orVector;
	private BitSet notVector;
	private BitSet propVector;
	private BitSet initializedVector;
	private int terminalIndex;
	private int initIndex;

	private BitSet toProcess;
	private int[] andCounters;
	private int[] orCounters;
	private int[] numberInputs;

	private BitSet[] inputArray;
	private BitSet[] outputArray;
	private Map<Role,BitSet> roleGoalMap;
	private Map<Role,BitSet> roleLegalMap;

	public Map<Proposition, Proposition> getLegalInputMap()
	{
		return this.legalInputMap;
	}

	public Map<GdlSentence, Proposition> getInputPropositions() {

		return this.inputPropositions;
	}
	public BitSet getInitializedVector() {
		return this.initializedVector;
	}

	public ArrayList getRawComponents() {
		return this.components;
	}

	public BitSet getGoal(Role r){
		return this.roleGoalMap.get(r);
	}

	public BitSet getLegal(Role r) {
		return this.roleLegalMap.get(r);
	}

	public int getInitIndex(){
		return initIndex;
	}

	public int getTerminalIndex(){
		return terminalIndex;
	}

	public Map<GdlSentence, Integer> getPropositionMap() {
		return this.propositionIndex;
	}

	public Map<Integer, GdlSentence> getIndexPropMap() {
		return this.indexProposition;
	}

	public BitSet getToProcess() {
		return this.toProcess;
	}

	public BitSet getBaseVector() {
		return this.baseVector;
	}

	public BitSet getInputVector() {
		return this.inputVector;
	}

	public BitSet getAndVector() {
		return this.andVector;
	}

	public BitSet getOrVector() {
		return this.orVector;
	}

	public BitSet getNotVector() {
		return this.notVector;
	}

	public BitSet getComponents() {
		return this.componentVector;
	}

	public BitSet getConstantVector() {
		return this.constantVector;
	}

	public BitSet getPropVector() {
		return this.propVector;
	}

	public BitSet getComponentInputs(int i) {
		return this.inputArray[i];
	}

	public BitSet getComponentOutputs(int i) {
		return this.outputArray[i];
	}

	public int getAndCounter(int i) {
		return this.andCounters[i];
	}

	public int getOrCounter(int i) {
		return this.orCounters[i];
	}

	public void incrAndCounter(int i, int delta) {
		this.andCounters[i] += delta;
	}

	public void incrOrCounter(int i, int delta) {
		this.orCounters[i] += delta;
	}

	public int getTotalInputs(int i) {
		return this.numberInputs[i];
	}

	public BitSet getTransitionVector() {
		return this.transitionVector;
	}

	public PropWyatt(List<Role> roles, ArrayList<Component> components) {
		this.components = new ArrayList<Component>(components);
		this.roleList = roles;
		this.toProcess = new BitSet(this.components.size());
		this.propositionIndex = new HashMap<GdlSentence, Integer>();
		this.componentIndex = new HashMap<Component, Integer>();
		this.roleGoalMap = new HashMap<Role, BitSet>();
		for (Role r : roles) {
			this.roleGoalMap.put(r, new BitSet(this.components.size()));
			this.roleLegalMap.put(r, new BitSet(this.components.size()));
		}
		recordInputPropositions();
		recordLegalPropositions();
		recordLegalInputMap();
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
				this.indexProposition.put(i, p.getName());
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
		this.initializedVector = new BitSet(this.components.size());
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
				this.propVector.set(i);
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

	/* Old functions (for find actions) constructing map data structures slightly modified */

	private void recordLegalInputMap() {
		this.legalInputMap = new HashMap<Proposition, Proposition>();
		// Create a mapping from Body->Input.
		Map<List<GdlTerm>, Proposition> inputPropsByBody = new HashMap<List<GdlTerm>, Proposition>();
		for(Proposition inputProp : this.inputPropositions.values()) {
			List<GdlTerm> inputPropBody = (inputProp.getName()).getBody();
			inputPropsByBody.put(inputPropBody, inputProp);
		}
		// Use that mapping to map Input->Legal and Legal->Input
		// based on having the same Body proposition.
		for(Set<Proposition> legalProps : this.legalPropositions.values()) {
			for(Proposition legalProp : legalProps) {
				List<GdlTerm> legalPropBody = (legalProp.getName()).getBody();
				if (inputPropsByBody.containsKey(legalPropBody)) {
    				Proposition inputProp = inputPropsByBody.get(legalPropBody);
    				this.legalInputMap.put(inputProp, legalProp);
    				this.legalInputMap.put(legalProp, inputProp);
				}
			}
		}
	}


	private void recordInputPropositions()
	{
		this.inputPropositions = new HashMap<GdlSentence, Proposition>();
		for (Component component : this.components)
		{
			// Skip all components that aren't propositions
			if (!(component instanceof Proposition)){
				continue;
			}
			Proposition proposition = (Proposition)component;
		    // Skip all propositions that aren't GdlFunctions.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("does")) {
				this.inputPropositions.put(proposition.getName(), proposition);
				proposition.isInput = true;
			}
		}
	}

	private void recordLegalPropositions()
	{
		this.legalPropositions = new HashMap<Role, Set<Proposition>>();
		for (Component component : this.components)
		{
			// Skip all components that aren't propositions
			if (!(component instanceof Proposition)) {
				continue;
			}

			Proposition proposition = (Proposition)component;

		    // Skip all propositions that aren't GdlRelations.
			if (!(proposition.getName() instanceof GdlRelation))
			    continue;

			GdlRelation relation = (GdlRelation) proposition.getName();
			if (relation.getName().getValue().equals("legal")) {
				GdlConstant name = (GdlConstant) relation.get(0);
				Role r = new Role(name);
				if (!this.legalPropositions.containsKey(r)) {
					this.legalPropositions.put(r, new HashSet<Proposition>());
				}
				this.legalPropositions.get(r).add(proposition);
			}
		}
	}
}
