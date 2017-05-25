package org.ggp.base.util.statemachine.implementation.propnet;

import java.util.ArrayList;
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
import org.ggp.dhtp.propnet.InternalMachineState;
import org.ggp.dhtp.propnet.PropNetForwardPropUtils;


@SuppressWarnings("unused")
public class SamplePropNetStateMachine extends StateMachine {
    /** The underlying proposition network  */
    private PropNet propNet;
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
    	//System.out.println("Initializing");
        try {
            initialize(OptimizingPropNetFactory.create(description));
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void initialize(PropNet propNet) {
        this.propNet = propNet;
        this.roles = propNet.getRoles();
        this.ordering = getOrdering();
        this.initialState = null;
    }

    /**
     * Computes if the state is terminal. Should return the value
     * of the terminal proposition for the state.
     */
    @Override
    public boolean isTerminal(MachineState state) {
    	//System.out.println("Is terminal");

    	if(state instanceof InternalMachineState){
    		return isTerminalInternal((InternalMachineState)state);
    	}

    	if(PropNetForwardPropUtils.markBases(state, propNet)){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}
        return PropNetForwardPropUtils.propMarkP(propNet.getTerminalProposition(), propNet);
    }

    private boolean isTerminalInternal(InternalMachineState state) {
    	//System.out.println("Is terminal");
    	if(PropNetForwardPropUtils.markBasesInternal(state, propNet)){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}
        return PropNetForwardPropUtils.propMarkP(propNet.getTerminalProposition(), propNet);
    }

    public InternalMachineState convertToInternal(MachineState m){
    	if(!(m instanceof MachineState)){
    		throw new UnsupportedOperationException();
    	}

    	boolean basesMod = PropNetForwardPropUtils.markBases(m, propNet);
    	PropNetForwardPropUtils.forwardProp(propNet);
    	propNet.updateBitSets();

		return getInternalMachineState();
    }

    @Override
	public MachineState performDepthCharge(MachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
    	if(state instanceof InternalMachineState){
    		return performDepthChargeInternal((InternalMachineState)state, theDepth);
    	} else {
    		return super.performDepthCharge(state, theDepth);
    	}
    }

    private InternalMachineState performDepthChargeInternal(InternalMachineState state, final int[] theDepth) throws TransitionDefinitionException, MoveDefinitionException {
        int nDepth = 0;
        while(!isTerminalInternal(state)) {
        	System.out.println("Performing internal depth charge");
            nDepth++;
            List<Move> m = getRandomJointMoveInternal(state);
            System.out.println("Picking move " +m.toString());
            state = getNextStateInternal(state, m);
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

    	PropNetForwardPropUtils.markBases(state, propNet);
    	PropNetForwardPropUtils.forwardProp(propNet);
        Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);
        Proposition pMatch = null;
        for (Proposition p : goalProps) {
        	if (PropNetForwardPropUtils.propMarkP(p, propNet)) {
        		if (pMatch != null) {
        			throw new GoalDefinitionException(state, role);
        		}
        		pMatch = p;
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

    	PropNetForwardPropUtils.markBasesInternal(state, propNet, true);
    	PropNetForwardPropUtils.forwardProp(propNet);
        Set<Proposition> goalProps = propNet.getGoalPropositions().get(role);
        Proposition pMatch = null;
        for (Proposition p : goalProps) {
        	System.out.println("Checking goal "+p.getName().toString());
        	if (PropNetForwardPropUtils.propMarkP(p, propNet)) {
        		if (pMatch != null) {
        			throw new GoalDefinitionException(state, role);
        		}
        		pMatch = p;
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
    	//System.out.println("Get Init state");

    	if(this.initialState != null){
    		return initialState;
    	}

        Proposition init = propNet.getInitProposition();
        init.setValue(true);
        PropNetForwardPropUtils.forwardProp(propNet);
        /* Propagate to all base propositions */
        for (Proposition baseProp : propNet.getBasePropositions().values()) {
        	/* Transition.getValue() returns true if INIT feeds into Transition */
        	baseProp.setValue(baseProp.getSingleInput().getValue());
        }
        init.setValue(false);
        this.initialState =  getStateFromBaseSimple(); //TODO This can be optimized
        return this.initialState;
    }


    public InternalMachineState getInitialStateInternal() {
    	//System.out.println("Get Init state");

    	if(this.initialInternalState != null){
    		return initialInternalState;
    	}

        Proposition init = propNet.getInitProposition();
        init.setValue(true);
        PropNetForwardPropUtils.forwardProp(propNet);
        /* Propagate to all base propositions */
        for (Proposition baseProp : propNet.getBasePropositions().values()) {
        	/* Transition.getValue() returns true if INIT feeds into Transition */
        	baseProp.setValue(baseProp.getSingleInput().getValue());
        }
        init.setValue(false);
        this.initialInternalState =  getInternalMachineState(); //TODO This can be optimized
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
    	//System.out.println("Constructing moves from legals");
    	//System.out.println(legalProps);
    	ArrayList<Move> moves = new ArrayList<Move>();
    	//System.out.println(moves);
    	for (Proposition p: legalProps) {
    		//System.out.println(p.toString());
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

    	//System.out.println("Get Legal moves");
    	if(PropNetForwardPropUtils.markBases(state, propNet)){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}
        Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
        ArrayList<Move> moves = new ArrayList<Move>();
    	for (Proposition p: legalProps) {
    		//System.out.println("Checking legality of " + p.toString());
    		//System.out.println("State is " + p.state);
    		//for(Component c : p.getInputs()){
    		//	System.out.println(c);
    		//	for(Component c2 : c.getInputs()){
    		//		System.out.println(c2);
    		//	}
    		//}
    		/* Only add the move if its allowed in this state */
    		if (PropNetForwardPropUtils.propMarkP(p, propNet)) {
    			moves.add(getMoveFromProposition(p));
    		}
    	}
        return moves;
    }


    private List<Move> getLegalMovesInternal(InternalMachineState state, Role role)
            throws MoveDefinitionException {
    	//System.out.println("Get Legal moves");
    	if(PropNetForwardPropUtils.markBasesInternal(state, propNet)){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}
        Set<Proposition> legalProps = propNet.getLegalPropositions().get(role);
        ArrayList<Move> moves = new ArrayList<Move>();
    	for (Proposition p: legalProps) {
    		//System.out.println("Checking legality of " + p.toString());
    		//System.out.println("State is " + p.state);
    		//for(Component c : p.getInputs()){
    		//	System.out.println(c);
    		//	for(Component c2 : c.getInputs()){
    		//		System.out.println(c2);
    		//	}
    		//}
    		/* Only add the move if its allowed in this state */
    		if (PropNetForwardPropUtils.propMarkP(p, propNet)) {
    			moves.add(getMoveFromProposition(p));
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
    	//System.out.println("Compute next state");

    	if(state instanceof InternalMachineState){
    		return getNextStateInternal((InternalMachineState)state, moves);
    	}

    	boolean basesMod = PropNetForwardPropUtils.markBases(state, propNet);
    	boolean axnsMod = PropNetForwardPropUtils.markActions(toDoes(moves), propNet); /* TODO toDoes() can be optimized */
    	if(basesMod || axnsMod){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}

    	/* Update bases */
    	for (Component c : propNet.getBaseComponentSet()) {
    		Proposition baseProp = (Proposition) c;
    		if (PropNetForwardPropUtils.propMarkP(baseProp.getSingleInput().getSingleInput(), propNet)) {

        		baseProp.setValue(true);
    		} else {

        		baseProp.setValue(false);
    		}
        }

    	PropNetForwardPropUtils.forwardProp(propNet);
    	return getStateFromBaseSimple(); /* TODO this can be optimized */
    }


    private InternalMachineState getNextStateInternal(InternalMachineState state, List<Move> moves)
            throws TransitionDefinitionException {
    	//String TS = new SimpleDateFormat("yyyyMMddHHmm'.txt'").format(new Date());
    	//System.out.println("Compute next state");

    	boolean basesMod = PropNetForwardPropUtils.markBasesInternal(state, propNet);
    	boolean axnsMod = PropNetForwardPropUtils.markActions(toDoes(moves), propNet); /* TODO toDoes() can be optimized */
    	if(basesMod || axnsMod){
    		PropNetForwardPropUtils.forwardProp(propNet);
    	}

    	/* Update bases */
    	for (Component c : propNet.getBaseComponentSet()) {
    		Proposition baseProp = (Proposition) c;
    		if (PropNetForwardPropUtils.propMarkP(baseProp.getSingleInput().getSingleInput(), propNet)) {

        		baseProp.setValue(true);
    		} else {

        		baseProp.setValue(false);
    		}
        }

    	PropNetForwardPropUtils.forwardProp(propNet);
    	return getInternalMachineState(); /* TODO this can be optimized */
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

        // All of the components in the PropNet
        List<Component> components = new ArrayList<Component>(propNet.getComponents());

        // All of the propositions in the PropNet.
        List<Proposition> propositions = new ArrayList<Proposition>(propNet.getPropositions());

        // TODO: Compute the topological ordering.

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
        for (Component c : propNet.getBaseComponentSet())
        {
        	Proposition p = (Proposition)c;
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }


    public InternalMachineState getInternalMachineState()
    {
    	propNet.updateBitSets();
        return new InternalMachineState(propNet.getComponentBits());
    }

    /**
     * A Naive implementation that computes a PropNetMachineState
     * from the true BasePropositions.  This is correct but slower than more advanced implementations
     * You need not use this method!
     * @return PropNetMachineState
     */
    public MachineState getStateFromBase()
    {
        Set<GdlSentence> contents = new HashSet<GdlSentence>();
        for (Proposition p : propNet.getBasePropositions().values())
        {
            p.setValue(p.getSingleInput().getValue());
            if (p.getValue())
            {
                contents.add(p.getName());
            }

        }
        return new MachineState(contents);
    }

    public PropNet getPropNet() {
    	return this.propNet;
    }
}