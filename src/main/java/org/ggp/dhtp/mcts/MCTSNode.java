package org.ggp.dhtp.mcts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.ggp.base.util.Pair;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.Heuristic;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class MCTSNode {

	List<Double> playerUtil, opponentUtil, playerHeur, opponentHeur;
	List<Integer> playerVisits, opponentVisits, combinedMoveVisits;
	List<MCTSNode> children;
	private MachineState state;
	private StateMachine machine;
	private Role player;
	List<Move> playerMoves;
	List<List<Move>> opponentMoves;
	private int playerRoleIdx;
	private int totalVisits;
	private int numPlayerMoves;
	private int numOpponentMoves;
	private boolean isTerminal;
	private int terminalValue;
	double explorationCoefficient;
	private Map<MachineState,MCTSNode> cachedNodes;
	private double utilMean;
	private double utilVariance;
	private boolean isFullyExplored;
	private double fullyExploredValue;
	private Move fullyExploredBestMove;
	private Heuristic h;
	private double heurVal;

	public MCTSNode(StateMachine machine, MachineState state, MCTSNode parent, Role player,
			double explorationCoefficient, Map<MachineState, MCTSNode> cachedNodes, Heuristic h) throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		this.machine = machine;
		this.state = state;
		//this.parents = new ArrayList<MCTSNode>();
		//this.parents.add(parent);
		this.playerUtil = null;
		this.playerVisits = null;
		this.opponentUtil = null;
		this.opponentVisits = null;
		this.playerHeur = null;
		this.opponentHeur = null;
		this.player = player;
		this.totalVisits = 0;
		this.explorationCoefficient = explorationCoefficient;
		this.cachedNodes = cachedNodes;
		if (machine.isTerminal(state)) {
			this.isTerminal = true;
			this.terminalValue = machine.getGoal(state, player);
			this.isFullyExplored = true;
			this.fullyExploredValue = terminalValue;
		} else {
			this.playerRoleIdx = machine.getRoleIndices().get(player);
			this.playerMoves = machine.getLegalMoves(state, player);
			this.opponentMoves = machine.getLegalJointMoves(state, player, playerMoves.get(0));
			this.numPlayerMoves = playerMoves.size();
			this.numOpponentMoves = opponentMoves.size();

			this.playerUtil = new ArrayList<Double>(numPlayerMoves);
			this.playerVisits = new ArrayList<Integer>(numPlayerMoves);
			this.playerHeur = new ArrayList<Double>(numPlayerMoves);
			for (int i = 0; i < numPlayerMoves; i++) {
				this.playerUtil.add(0.0);
				this.playerVisits.add(0);
				this.playerHeur.add(0.0);
			}
			this.opponentUtil = new ArrayList<Double>(numOpponentMoves);
			this.opponentVisits = new ArrayList<Integer>(numOpponentMoves);
			this.opponentHeur = new ArrayList<Double>(numOpponentMoves);

			for (int i = 0; i < numOpponentMoves; i++) {
				this.opponentUtil.add(0.0);
				this.opponentVisits.add(0);
				this.opponentHeur.add(0.0);
			}

			this.combinedMoveVisits = new ArrayList<Integer>(numPlayerMoves * numOpponentMoves);

			for (int i = 0; i < numPlayerMoves * numOpponentMoves; i++) {
				this.combinedMoveVisits.add(0);
			}

			this.children = new ArrayList<MCTSNode>();
			this.isFullyExplored = false;
			this.fullyExploredValue = 0;
			this.h = h;
			if(this.h != null){
				this.heurVal = h.evalState(player, state);
			} else {
				heurVal = 0.0;
			}
		}
	}

	public boolean isFullyExplored(){
		return this.isFullyExplored;
	}

	private double getPlayerSelectionScore(int playerMoveIdx) {

		double playerUtility = playerUtil.get(playerMoveIdx);
		double playerHeuristic = playerHeur.get(playerMoveIdx);
		int numPlayerVisits = playerVisits.get(playerMoveIdx);
		//DebugLog.output("Exploration coefficient is " + explorationCoefficient);

		return (playerUtility) / numPlayerVisits + playerHeuristic
				+ explorationCoefficient * Math.sqrt(Math.log(totalVisits) / numPlayerVisits);
	}

	private double getOpponentSelectionScore(int opponentMoveIdx) {

		double opponentUtility = opponentUtil.get(opponentMoveIdx);
		double opponentHeuristic = opponentHeur.get(opponentMoveIdx);
		int numOpponentVisits = opponentVisits.get(opponentMoveIdx);

		return -1 * (opponentUtility)/ numOpponentVisits - opponentHeuristic
				+ explorationCoefficient * Math.sqrt(Math.log(totalVisits) / numOpponentVisits);
	}

	public Move getBestMove(long turnTimeout) throws PhaseTimeoutException {
		return getBestMoveAndUtility(turnTimeout).left;
	}

	public Pair<Move, Double> getBestMoveAndUtility(long turnTimeout) throws PhaseTimeoutException {
		if(isFullyExplored){
			DebugLog.output("Fully explored -- returning best move (" + fullyExploredBestMove + " " + fullyExploredValue + ")");
			return Pair.of(fullyExploredBestMove, fullyExploredValue);
		}
		Move bestMove = null;
		double bestUtility = 0.0;
		for (int i = 0; i < numPlayerMoves; i++) {
			PhaseTimeoutException.checkTimeout(turnTimeout);
			double averageUtility = playerVisits.get(i) == 0 ? 0 : playerUtil.get(i) / playerVisits.get(i);
			DebugLog.output("Average utility of "+ playerMoves.get(i) + " is "+ averageUtility);
			if (bestMove == null || bestUtility < averageUtility) {
				bestMove = playerMoves.get(i);
				bestUtility = averageUtility;
			}

		}

		return Pair.of(bestMove, bestUtility);
	}

	private boolean fullyExploreNode(long turnTimeout) throws PhaseTimeoutException{
		if(isFullyExplored){
			return true;
		}
		if(children.size() != numPlayerMoves * numOpponentMoves){
			return false;
		}
		for(MCTSNode child : children){
			if(!child.isFullyExplored){
				return false;
			}
		}
		//DebugLog.output("All children fully explored -- collapsing state!");
		double minimaxScore = 0;
		Move minimaxMove = null;
		for (int i = 0; i < numPlayerMoves; i++){
			PhaseTimeoutException.checkTimeout(turnTimeout);
			double minScore = 0;
			boolean foundMinScore = false;
			for (int j = 0; j < numOpponentMoves; j++) {
				PhaseTimeoutException.checkTimeout(turnTimeout);
				// do minimax here
				int childIdx = i*numOpponentMoves+j;
				double childFullyExploredVal = children.get(childIdx).fullyExploredValue;
				if(!foundMinScore || childFullyExploredVal < minScore){
					minScore = childFullyExploredVal;
					foundMinScore = true;
				}
			}
			if(minScore > minimaxScore || minimaxMove == null){
				minimaxScore = minScore;
				minimaxMove = playerMoves.get(i);
			}
		}
		this.isFullyExplored = true;
		this.fullyExploredValue = minimaxScore;
		this.fullyExploredBestMove = minimaxMove;
		return true;
	}

	private int getPlayerIdxFromChildIdx(int childIdx) {
		return childIdx % numPlayerMoves;
	}

	private int getOpponentIdxFromChildIdx(int childIdx) {
		return childIdx / numPlayerMoves;
	}

	private double expandAndSimulate(long turnTimeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException {
		// expand
		int playerMoveIdx = getPlayerIdxFromChildIdx(children.size());
		int opponentMoveIdx = getOpponentIdxFromChildIdx(children.size());
		List<Move> jointMoves = opponentMoves.get(opponentMoveIdx);
		Move playerMove = playerMoves.get(playerMoveIdx);
		jointMoves.set(playerRoleIdx, playerMove);
		MachineState newState = machine.getNextState(state, jointMoves);

		if(cachedNodes.containsKey(newState)){
			MCTSNode newChild = cachedNodes.get(newState);
			children.add(newChild);
		} else {
			MCTSNode newChild = new MCTSNode(machine, newState, this, player, this.explorationCoefficient, cachedNodes, this.h);
			children.add(newChild);
			cachedNodes.put(newState, newChild);
		}

		// simulate
		//DebugLog.output("Performing depth charge");
		MachineState mcState = newState;
		while(!machine.isTerminal(mcState)){
			PhaseTimeoutException.checkTimeout(turnTimeout);
			mcState = machine.getRandomNextState(mcState);
		}
		//machine.performDepthCharge(newState, new int[1]);
		//DebugLog.output("Depth charge complete");
		return machine.getGoal(mcState, player);
	}

	public double performIteration(long turnTimeout) throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException, PhaseTimeoutException {
		PhaseTimeoutException.checkTimeout(turnTimeout);

		if (isTerminal) {
			return terminalValue;
		}

		if(fullyExploreNode(turnTimeout)){
			return fullyExploredValue;
		}

		double bpr, heur;
		int selectedIdx;
		if (children.size() < numPlayerMoves * numOpponentMoves) { // in
																	// expansion
																	// phase
			selectedIdx = children.size();
			bpr = expandAndSimulate(turnTimeout);
			heur = children.get(selectedIdx).heurVal;
		} else {
			// select best player move based on criteria
			double bestPlayerScore = 0;
			int bestPlayerIdx = 0;
			for (int i = 0; i < numPlayerMoves; i++) {
				PhaseTimeoutException.checkTimeout(turnTimeout);
				boolean allChildrenFullyExploredForPlayerMove = true;
				for(int j = 0; j < numOpponentMoves; j++){
					PhaseTimeoutException.checkTimeout(turnTimeout);
					if(!children.get(numOpponentMoves*i + j).isFullyExplored){
						allChildrenFullyExploredForPlayerMove = false;
						break;
					}
				}
				if(allChildrenFullyExploredForPlayerMove){
					continue;
				}
				// DebugLog.output("Calculating selection score for child "+i);
				double playerScore = getPlayerSelectionScore(i);
				// DebugLog.output("Selection score for child "+i+":"+score);
				if (playerScore > bestPlayerScore) {
					bestPlayerIdx = i;
					bestPlayerScore = playerScore;
				}
			}
			//select best opponent move based on criteria
			double bestOpponentScore = 0;
			int bestOpponentIdx = 0;
			for (int i = 0; i < numOpponentMoves; i++) {
				PhaseTimeoutException.checkTimeout(turnTimeout);
				if(children.get(numOpponentMoves*bestPlayerIdx + i).isFullyExplored){
					continue;
				}
				// DebugLog.output("Calculating selection score for child "+i);
				double opponentScore = getOpponentSelectionScore(i);
				// DebugLog.output("Selection score for child "+i+":"+score);
				if (opponentScore > bestOpponentScore) {
					bestOpponentIdx = i;
					bestOpponentScore = opponentScore;
				}
			}
			int bestIdx = bestPlayerIdx*numOpponentMoves+bestOpponentIdx;
			MCTSNode selectedChild = children.get(bestIdx);
			heur = selectedChild.heurVal;
			bpr = selectedChild.performIteration(turnTimeout);
			selectedIdx = bestIdx;
		}
		// update stats
		int playerMoveIdx = getPlayerIdxFromChildIdx(selectedIdx);
		int opponentMoveIdx = getOpponentIdxFromChildIdx(selectedIdx);

		this.playerUtil.set(playerMoveIdx, this.playerUtil.get(playerMoveIdx) + bpr);
		this.playerHeur.set(playerMoveIdx, this.playerHeur.get(playerMoveIdx) + heur);
		this.opponentUtil.set(opponentMoveIdx, this.opponentUtil.get(opponentMoveIdx) + bpr);
		this.opponentHeur.set(opponentMoveIdx, this.opponentHeur.get(opponentMoveIdx) + heur);
		this.playerVisits.set(playerMoveIdx, this.playerVisits.get(playerMoveIdx) + 1);
		this.opponentVisits.set(opponentMoveIdx, this.opponentVisits.get(opponentMoveIdx) + 1);
		this.combinedMoveVisits.set(selectedIdx, this.combinedMoveVisits.get(selectedIdx) + 1);
		this.totalVisits += 1;

			//refer to https://math.stackexchange.com/questions/20593/calculate-variance-from-a-stream-of-sample-values/116344#116344
		if(totalVisits == 1){
			utilMean =  bpr;
			utilVariance = 0;
		} else {
			double m_k_1 = utilMean;
			double m_k = m_k_1 + (bpr - m_k_1)/totalVisits;
			double v_k_1 = utilVariance;
			double v_k = v_k_1 + (bpr - m_k_1)*(bpr - m_k);
			utilMean = m_k;
			utilVariance = v_k;
		}
		//DebugLog.output("Mean is "+ utilMean);
		//DebugLog.output("Setting exploration coef to "+ Math.max(Math.sqrt(utilVariance/(totalVisits-1)), 0));
		//TODO: uncomment following line to set exploration coefficient based on variance of utility
		//this.explorationCoefficient = Math.min(Math.max(Math.sqrt(utilVariance/(totalVisits)), 0),1000);
		this.explorationCoefficient = Math.sqrt(utilVariance/(totalVisits));


		if(fullyExploreNode(turnTimeout)){
			return fullyExploredValue;
		}

		return bpr;
	}

	public MCTSNode updateState(MachineState newState) {
		if(this.state.equals(newState)){
			//handle case where init state is passed in
			return this;
		}
		// TODO Auto-generated method stub
		for (MCTSNode child : children) {
			if (child.state.equals(newState)) {
				return child;
			}
		}
		return null;

	}

}
