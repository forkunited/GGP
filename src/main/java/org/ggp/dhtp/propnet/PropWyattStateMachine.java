package org.ggp.dhtp.propnet;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.propnet.factory.OptimizingPropNetFactory;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.query.ProverQueryBuilder;


@SuppressWarnings("unused")
public class PropWyattStateMachine extends StateMachine {
    /** The underlying proposition network  */

    private PropWyatt propNet;
    /** The topological ordering of the propositions */
    private List<Proposition> ordering;
    /** The player roles */
    private List<Role> roles;
    private MachineState initialState;
    private InternalMachineState initialInternalState;

    /**
     * Initializes the PropNetStateMachine. You should compute the topological
     * ordering here. Additionally you may compute the initial state here, at
     * your discretion.
     */
    @Override
    public void initialize(List<Gdl> description) {
    	System.out.println("Initializing");
        try {
            initialize(OptimizingPropNetFactory.create(description));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /* Return the Gdl sentences that go along with this state (high baseProps) */
    public Set<GdlSentence> getContents(InternalMachineState state) {
    	BitSet stateBits = (BitSet)state.getBitSet().clone();
    	stateBits.and(propNet.getBaseVector());
    	Map<Integer, GdlSentence> indexPropMap = propNet.getIndexPropMap();
    	Set<GdlSentence> contents = new HashSet<GdlSentence>();
    	int i = stateBits.nextSetBit(0);
    	while (i>= 0) {
    		contents.add(indexPropMap.get(i));
    		/* Get the next output index avoiding exceptions */
			if (i == stateBits.size() - 1) {
				i = -1;
			} else {
				i = stateBits.nextSetBit(i + 1);
			}
    	}
    	System.out.println("The contents: " + contents);
    	System.out.println("Bitset: " + propNet.getComponents());
    	return contents;
    }

    public void initialize(PropNet propNet) {
    	//propNet.renderToFile("/Users/ZenGround0/1.dot");
        this.propNet = new PropWyatt(propNet.getRoles(), propNet.getComponentList());
        this.roles = propNet.getRoles();
        this.ordering = getOrdering();
        this.initialState = null;
        this.propNet.freezeProp();
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {

    	if(state instanceof InternalMachineState){
    		return isTerminalInternal((InternalMachineState)state);
    	}

    	if(PropWyattForwardUtils.markBases(state, propNet)){
    		PropWyattForwardUtils.forwardProp(propNet);
    	}
        return propNet.getComponents().get(propNet.getTerminalIndex());
    }

    private boolean isTerminalInternal(InternalMachineState state) {
    	if(PropWyattForwardUtils.markBasesInternal(state, propNet)){
    		PropWyattForwardUtils.forwardProp(propNet);
    	}
        return propNet.getComponents().get(propNet.getTerminalIndex());
    }

    public InternalMachineState convertToInternal(MachineState m){
    	if(!(m instanceof MachineState)){
    		throw new UnsupportedOperationException();
    	}

    	boolean basesMod = PropWyattForwardUtils.markBases(m, propNet);
    	PropWyattForwardUtils.forwardProp(propNet);

		return getInternalMachineState();
    }

    @Override
	public MachineState performDepthCharge(MachineState state, final int[] theDepth, StateMachine cmp, MachineState oldState) throws TransitionDefinitionException, MoveDefinitionException {
    	if(state instanceof InternalMachineState){
    		System.out.println("Perform depth charge");
    		return performDepthChargeInternal((InternalMachineState)state, theDepth, cmp, oldState);
    	} else {
    		return super.performDepthCharge(state, theDepth, null, null);
    	}
    }

    private InternalMachineState performDepthChargeInternal(InternalMachineState state, final int[] theDepth, StateMachine cmp, MachineState oldState) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        while(!isTerminalInternal(state)) {
        	System.out.println("Inside while");
            nDepth++;
            List<Move> m = getRandomJointMoveInternal(state);
            System.out.println("Moves: " + m);
            state = getNextStateInternal(state, m);
            oldState = cmp.getNextState(oldState, m);
            /* Compare the two states */
            ArrayList<String> sentences = new ArrayList<String>();

            for (GdlSentence sentence : this.getContents(state)) {
            	sentences.add(sentence.toString());
            }
            ArrayList<String> oldSentences = new ArrayList<String>();
            for (GdlSentence sentence : oldState.getContents()) {
            	oldSentences.add(sentence.toString());
            }
            Collections.sort(sentences);
            Collections.sort(oldSentences);
            System.out.println(sentences);
            System.out.println(oldSentences);
            if (!sentences.toString().equals(oldSentences.toString())) {
            	System.out.println("Error states are different between machines");
            	System.out.println("Propnet state: ");
            	System.out.println(sentences.toString());
            	System.out.println("Correct state: ");
            	System.out.println(oldSentences.toString());
            	throw new MoveDefinitionException(oldState, roles.get(0));
            }

            /* Compare the legal moves of role 0 */

            /* Compare the goal of role 0 */
        }
        if(theDepth != null)
            theDepth[0] = nDepth;
        return state;
    }

    @Override
	public List<Move> getRandomJointMove(MachineState state) throws MoveDefinitionException
    {
    	if(state instanceof InternalMachineState){
    		return getRandomJointMoveInternal((InternalMachineState)state);
    	} else {
    		return super.getRandomJointMove(state);
    	}

    }

    private List<Move> getRandomJointMoveInternal(InternalMachineState state) throws MoveDefinitionException
    {
        List<Move> random = new ArrayList<Move>();
        for (Role role : getRoles()) {
            random.add(getRandomMoveInternal(state, role));
        }

        return random;
    }

    @Override
   	public Move getRandomMove(MachineState state, Role role) throws MoveDefinitionException
       {
       	if(state instanceof InternalMachineState){
       		return getRandomMoveInternal((InternalMachineState)state, role);
       	} else {
       		return super.getRandomMove(state, role);
       	}
       }

    private Move getRandomMoveInternal(InternalMachineState state, Role role) throws MoveDefinitionException
    {
        List<Move> legals = getLegalMovesInternal(state, role);
        return legals.get(new Random().nextInt(legals.size()));
    }


    /**
     * Computes the goal for a role in the current state.
     * Should return the value of the goal proposition that
     * is true for that role. If there is not exactly one goal
     * proposition true for that role, then you should throw a
     * GoalDefinitionException because the goal is ill-defined.
     */
    @Override
    public int getGoal(MachineState state, Role role)
            throws GoalDefinitionException {
    	//System.out.println("Get Goal");

    	if(state instanceof InternalMachineState){
    		return getGoalInternal((InternalMachineState)state, role);
    	}

    	PropWyattForwardUtils.markBases(state, propNet);
    	PropWyattForwardUtils.forwardProp(propNet);
    	BitSet goalMask = propNet.getGoal(role);
    	BitSet components = propNet.getComponents();
    	ArrayList<Component> RawComponents = propNet.getRawComponents();
		int i = goalMask.nextSetBit(0);
		Proposition pMatch = null;

    	while (i >= 0) {
    		if (components.get(i)) {
    			if (pMatch != null) {
    				throw new GoalDefinitionException(state, role);
    			}
    			pMatch = (Proposition)RawComponents.get(i);
    		}

    		/* Get the next output index avoiding exceptions */
			if (i == goalMask.size() - 1) {
				i = -1;
			} else {
				i = goalMask.nextSetBit(i + 1);
			}
    	}
        if (pMatch == null) {
        	throw new GoalDefinitionException(state, role);
        }
        GdlSentence name = pMatch.getName();
        return getGoalValue(pMatch);
    }


    private int getGoalInternal(InternalMachineState state, Role role)
            throws GoalDefinitionException {

    	PropWyattForwardUtils.markBasesInternal(state, propNet);
    	PropWyattForwardUtils.forwardProp(propNet);
    	BitSet goalMask = propNet.getGoal(role);
    	BitSet components = propNet.getComponents();
    	ArrayList<Component> RawComponents = propNet.getRawComponents();
		int i = goalMask.nextSetBit(0);
		Proposition pMatch = null;

    	while (i >= 0) {
    		if (components.get(i)) {
    			if (pMatch != null) {
    				throw new GoalDefinitionException(state, role);
    			}
    			pMatch = (Proposition)RawComponents.get(i);
    		}

    		/* Get the next output index avoiding exceptions */
			if (i == goalMask.size() - 1) {
				i = -1;
			} else {
				i = goalMask.nextSetBit(i + 1);
			}
    	}
        if (pMatch == null) {
        	throw new GoalDefinitionException(state, role);
        }
        GdlSentence name = pMatch.getName();
        return getGoalValue(pMatch);
    }

    /**
     * Returns the initial state. The initial state can be computed
     * by only setting the truth value of the INIT proposition to true,
     * and then computing the resulting state.
     */
    @Override
    public MachineState getInitialState() {
    	System.out.println("Get Init state");

    	if(this.initialState != null){
    		return initialState;
    	}
    	int initIndex = propNet.getInitIndex();
    	BitSet components = propNet.getComponents();
        components.set(initIndex);
        propNet.getToProcess().set(0, propNet.getRawComponents().size(), true);
        propNet.getInitializedVector().or(propNet.getTransitionVector());
        PropWyattForwardUtils.forwardProp(propNet);
        /* Propagate to all base propositions */
        BitSet transitionMask = propNet.getTransitionVector();
        int i = transitionMask.nextSetBit(0);
        System.out.println("Before propagating through: " + components);
        while (i >= 0) {
        	ArrayList<Integer> inputs = propNet.getComponentInputs(i);
			int inputIdx = inputs.get(0);
        	ArrayList<Integer> outputs = propNet.getComponentOutputs(i);
        	int outputIdx = outputs.get(0);
        	/* Set output value to input */
        	components.set(outputIdx, components.get(inputIdx));
//        	System.out.println("Base prop being set from transition: " + propNet.getIndexPropMap().get(outputIdx));
//        	System.out.println("Incident name through transition: " + propNet.getIndexPropMap().get(inputIdx));
//        	System.out.println("Incident index: " + inputIdx);
//        	System.out.println("Incident truth value: " + components.get(inputIdx));
        	/* Get the next output index avoiding exceptions */
			if (i == transitionMask.size() - 1) {
				i = -1;
			} else {
				i = transitionMask.nextSetBit(i + 1);
			}
        }
        System.out.println("After propagating through: " + components);


        components.set(initIndex, false);
        this.initialState =  getStateFromBaseSimple(); //TODO This can be optimized
        return this.initialState;
    }


    public InternalMachineState getInitialStateInternal() {
    	System.out.println("Get Init state internal");

    	if(this.initialInternalState != null){
    		return initialInternalState;
    	}

    	this.initialInternalState =  convertToInternal(getInitialState());

    	return this.initialInternalState;
    }

    /**
     * Computes all possible actions for role.
     */
    @Override
    public List<Move> findActions(Role role)
            throws MoveDefinitionException {
    	System.out.println("Find actions new");
    	Map<GdlSentence, Proposition> inputMap = propNet.getInputPropositions();
    	HashSet<Proposition> legalProps = new HashSet<Proposition>();
    	Map<Proposition, Proposition> inputLegalMap = propNet.getLegalInputMap();
    	System.out.println("Everything set up");
    	for (GdlSentence sentence: inputMap.keySet()){
    		if (sentence.get(0).toString().equals(role.getName().toString())){
    			System.out.println("Adding legal prop");
    			Proposition inputProp = inputMap.get(sentence);
    			Proposition legalProp = inputLegalMap.get(inputProp);
    			if (legalProp != null) {
    				legalProps.add(legalProp);
    			}
    		}
    	}

    	ArrayList<Move> moves = new ArrayList<Move>();

    	for (Proposition p: legalProps) {
    		moves.add(getMoveFromProposition(p));
    	}
    	return moves;


//    	Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
//    	ArrayList<Move> moves = new ArrayList<Move>();
//    	for (Proposition p: legalProps) {
//    		moves.add(getMoveFromProposition(p));
//    	}
//        return moves;
    }

    /**
     * Computes the legal moves for role in state.
     */
    @Override
    public List<Move> getLegalMoves(MachineState state, Role role)
            throws MoveDefinitionException {

    	if(state instanceof InternalMachineState){
    		return getLegalMovesInternal((InternalMachineState)state, role);
    	}

    	System.out.println("Get Legal moves");
    	if(PropWyattForwardUtils.markBases(state, propNet)){
    		PropWyattForwardUtils.forwardProp(propNet);
    	}

    	BitSet legalMask = propNet.getLegal(role);
    	ArrayList<Move> moves = new ArrayList<Move>();
    	BitSet components = propNet.getComponents();
    	ArrayList<Component> rawComponents = propNet.getRawComponents();
    	int i = legalMask.nextSetBit(0);
    	while (i>=0){
    		/* Only add the move if its allowed in this state */
    		if (components.get(i)) {
    			Proposition p = (Proposition)rawComponents.get(i);
    			moves.add(getMoveFromProposition(p));
    		}

    		/* Get the next output index avoiding exceptions */
			if (i == legalMask.size() - 1) {
				i = -1;
			} else {
				i = legalMask.nextSetBit(i + 1);
			}
    	}
        return moves;
    }


    private List<Move> getLegalMovesInternal(InternalMachineState state, Role role)
            throws MoveDefinitionException {
    	System.out.println("Get Legal moves internal");
    	if(PropWyattForwardUtils.markBasesInternal(state, propNet)){
    		System.out.println("Starting forward prop");
    		PropWyattForwardUtils.forwardProp(propNet);
    		System.out.println("Ending forward prop");
    	}
    	BitSet legalMask = propNet.getLegal(role);
    	ArrayList<Move> moves = new ArrayList<Move>();
    	BitSet components = propNet.getComponents();
    	ArrayList<Component> rawComponents = propNet.getRawComponents();
    	int i = legalMask.nextSetBit(0);
    	while (i>=0){
    		/* Only add the move if its allowed in this state */
    		if (components.get(i)) {
    			Proposition p = (Proposition)rawComponents.get(i);
    			moves.add(getMoveFromProposition(p));
    		}

    		/* Get the next output index avoiding exceptions */
			if (i == legalMask.size() - 1) {
				i = -1;
			} else {
				i = legalMask.nextSetBit(i + 1);
			}
    	}
        return moves;
    }

    /**
     * Computes the next state given state and the list of moves.
     */
    @Override
    public MachineState getNextState(MachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	//String TS = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());

    	if(state instanceof InternalMachineState){
    		return getNextStateInternal((InternalMachineState)state, moves);
    	}

    	boolean basesMod = PropWyattForwardUtils.markBases(state, propNet);
    	boolean axnsMod = PropWyattForwardUtils.markActions(toDoes(moves), propNet); /* TODO toDoes() can be optimized */
    	if(basesMod || axnsMod){
    		PropWyattForwardUtils.forwardProp(propNet);
    	}

    	/* Update bases */
    	/* Propagate to all base propositions */
        BitSet transitionMask = propNet.getTransitionVector();
        BitSet components = propNet.getComponents();
        BitSet toProcess = propNet.getToProcess();
        int i = transitionMask.nextSetBit(0);
        while (i >= 0) {
        	ArrayList<Integer> inputs = propNet.getComponentInputs(i);
			int inputIdx = inputs.get(0);
        	ArrayList<Integer> outputs = propNet.getComponentOutputs(i);
        	int outputIdx = outputs.get(0);
        	/* Set output value to input */
        	components.set(outputIdx, components.get(inputIdx));
        	/* Add all bases to the ToProcess queue */
        	toProcess.set(i);

        	/* Get the next output index avoiding exceptions */
			if (i == transitionMask.size() - 1) {
				i = -1;
			} else {
				i = transitionMask.nextSetBit(i + 1);
			}
        }

    	PropWyattForwardUtils.forwardProp(propNet);
    	return getStateFromBaseSimple(); /* TODO this can be optimized */
    }


    private InternalMachineState getNextStateInternal(InternalMachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	//String TS = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
    	//System.out.println("Compute next state internal");

    	boolean basesMod = PropWyattForwardUtils.markBasesInternal(state, propNet);
    	boolean axnsMod = PropWyattForwardUtils.markActionsInternal(toDoes(moves), propNet); /* TODO toDoes() can be optimized */
    	if(basesMod || axnsMod){
    		PropWyattForwardUtils.forwardProp(propNet);
    	}

    	/*Update Bases */
    	 BitSet transitionMask = propNet.getTransitionVector();
         BitSet components = propNet.getComponents();
         BitSet toProcess = propNet.getToProcess();
         int i = transitionMask.nextSetBit(0);
         while (i >= 0) {
         	ArrayList<Integer> inputs = propNet.getComponentInputs(i);
 			int inputIdx = inputs.get(0);
         	ArrayList<Integer> outputs = propNet.getComponentOutputs(i);
         	int outputIdx = outputs.get(0);
         	if (components.get(outputIdx) != components.get(inputIdx)) {
         		/* Set output value to input */
         		components.set(outputIdx, components.get(inputIdx));
         		/* Add all bases to the ToProcess queue */
         		toProcess.set(i);
         	}

         	/* Get the next output index avoiding exceptions */
 			if (i == transitionMask.size() - 1) {
 				i = -1;
 			} else {
 				i = transitionMask.nextSetBit(i + 1);
 			}
         }

     	PropWyattForwardUtils.forwardProp(propNet);
     	return new InternalMachineState(propNet.getComponents()); /* TODO this can be optimized */
    }


    /**
     * This should compute the topological ordering of propositions.
     * Each component is either a proposition, logical gate, or transition.
     * Logical gates and transitions only have propositions as inputs.
     *
     * The base propositions and input propositions should always be exempt
     * from this ordering.
     *
     * The base propositions values are set from the MachineState that
     * operations are performed on and the input propositions are set from
     * the Moves that operations are performed on as well (if any).
     *
     * @return The order in which the truth values of propositions need to be set.
     */
    public List<Proposition> getOrdering()
    {
        // List to contain the topological ordering.
        List<Proposition> order = new LinkedList<Proposition>();

        return order;
    }

    /* Already implemented for you */
    @Override
    public List<Role> getRoles() {
        return roles;
    }

    /* Helper methods */

    /**
     * The Input propositions are indexed by (does ?player ?action).
     *
     * This translates a list of Moves (backed by a sentence that is simply ?action)
     * into GdlSentences that can be used to get Propositions from inputPropositions.
     * and accordingly set their values etc.  This is a naive implementation when coupled with
     * setting input values, feel free to change this for a more efficient implementation.
     *
     * @param moves
     * @return
     */
    private List<GdlSentence> toDoes(List<Move> moves)
    {
        List<GdlSentence> doeses = new ArrayList<GdlSentence>(moves.size());
        Map<Role, Integer> roleIndices = getRoleIndices();

        for (int i = 0; i < roles.size(); i++)
        {
            int index = roleIndices.get(roles.get(i));
            doeses.add(ProverQueryBuilder.toDoes(roles.get(i), moves.get(index)));
        }
        return doeses;
    }

    /**
     * Takes in a Legal Proposition and returns the appropriate corresponding Move
     * @param p
     * @return a PropNetMove
     */
    public static Move getMoveFromProposition(Proposition p)
    {
        return new Move(p.getName().get(1));
    }

    /**
     * Helper method for parsing the value of a goal proposition
     * @param goalProposition
     * @return the integer value of the goal proposition
     */
    private int getGoalValue(Proposition goalProposition)
    {
        GdlRelation relation = (GdlRelation) goalProposition.getName();
        GdlConstant constant = (GdlConstant) relation.get(1);
        return Integer.parseInt(constant.toString());
    }

    /**
     * Machine state generation from bases without directly reading
     * the incoming transitions value.  Allows for generalizing
     * across init and next state
     */
    public MachineState getStateFromBaseSimple()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        Map<Integer, GdlSentence> indexToProp = propNet.getIndexPropMap();
        BitSet components = propNet.getComponents();
        BitSet baseMask = propNet.getBaseVector();
		int i = baseMask.nextSetBit(0);
		while (i >= 0) {

			if (components.get(i)) {
				contents.add(indexToProp.get(i));
			}

			if (i == baseMask.size() - 1) {
				i = -1;
			} else {
				i = baseMask.nextSetBit(i + 1);
			}
		}

        return new MachineState(contents);
    }


    private InternalMachineState getInternalMachineState()
    {
        return new InternalMachineState(propNet.getComponents());
    }

    public PropWyatt getPropNet() {
    	return this.propNet;
    }
}