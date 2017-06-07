package org.ggp.dhtp.mcts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

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
	private Map<MachineState, MCTSNode> cachedNodes;
	private boolean isFullyExplored;
	private double fullyExploredValue;
	private Move fullyExploredBestMove;
	private Heuristic h;
	private double heurVal;
	private double utilMean, utilVar;

	public MCTSNode(StateMachine machine, MachineState state, MCTSNode parent, Role player,
			double explorationCoefficient, Map<MachineState, MCTSNode> cachedNodes, Heuristic h)
			throws MoveDefinitionException, GoalDefinitionException, TransitionDefinitionException {
		this.machine = machine;
		this.state = state;
		// this.parents = new ArrayList<MCTSNode>();
		// this.parents.add(parent);
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
		this.utilMean = 0.0;
		this.utilVar = 0.0;
		if (machine.isTerminal(state)) {
			this.isTerminal = true;
			this.terminalValue = machine.getGoal(state, player);
			this.isFullyExplored = true;
			this.fullyExploredValue = terminalValue;
		} else {
			this.playerRoleIdx = machine.getRoleIndices().get(player);
			this.playerMoves = new CopyOnWriteArrayList<Move>(machine.getLegalMoves(state, player));
			List<List<Move>> jMoves = new CopyOnWriteArrayList<List<Move>>();
			for (List<Move> jm : machine.getLegalJointMoves(state, player, playerMoves.get(0)))
				jMoves.add(new CopyOnWriteArrayList<Move>(jm));
			this.opponentMoves = jMoves; //Collections.synchronizedList(jMoves);
			this.numPlayerMoves = playerMoves.size();
			this.numOpponentMoves = opponentMoves.size();

			this.playerUtil = Collections.synchronizedList(new ArrayList<Double>(numPlayerMoves));
			this.playerVisits = Collections.synchronizedList(new ArrayList<Integer>(numPlayerMoves));
			this.playerHeur = Collections.synchronizedList(new ArrayList<Double>(numPlayerMoves));
			for (int i = 0; i < numPlayerMoves; i++) {
				this.playerUtil.add(0.0);
				this.playerVisits.add(0);
				this.playerHeur.add(0.0);
			}
			this.opponentUtil = Collections.synchronizedList(new ArrayList<Double>(numOpponentMoves));
			this.opponentVisits = Collections.synchronizedList(new ArrayList<Integer>(numOpponentMoves));
			this.opponentHeur = Collections.synchronizedList(new ArrayList<Double>(numOpponentMoves));

			for (int i = 0; i < numOpponentMoves; i++) {
				this.opponentUtil.add(0.0);
				this.opponentVisits.add(0);
				this.opponentHeur.add(0.0);
			}
			this.combinedMoveVisits = Collections.synchronizedList(new ArrayList<Integer>(numPlayerMoves * numOpponentMoves));

			for (int i = 0; i < numPlayerMoves * numOpponentMoves; i++) {
				this.combinedMoveVisits.add(0);
			}

			this.children = Collections.synchronizedList(new ArrayList<MCTSNode>());
			this.isFullyExplored = false;
			this.fullyExploredValue = 0;
			this.h = h;
			if (this.h != null) {
				//DebugLog.output("Evaluating state using heuristic");
					this.heurVal = h.evalState(player, state);
			} else {
				heurVal = 0.0;
			}
		}
	}

	public boolean isFullyExplored() {
		return this.isFullyExplored;
	}

	private double getPlayerSelectionScore(int playerMoveIdx) {

		double playerUtility = playerUtil.get(playerMoveIdx);
		double playerHeuristic =  playerHeur.get(playerMoveIdx);
		int numPlayerVisits = playerVisits.get(playerMoveIdx);
		//DebugLog.output("Exploration coefficient is " +
		 //explorationCoefficient);

		return (playerUtility) / (numPlayerVisits) + 0.5*playerHeuristic / (numPlayerVisits)
				+ explorationCoefficient * Math.sqrt(Math.log(totalVisits) / numPlayerVisits);
	}

	private double getOpponentSelectionScore(int opponentMoveIdx) {

		double opponentUtility = opponentUtil.get(opponentMoveIdx);
		double opponentHeuristic = opponentHeur.get(opponentMoveIdx);
		int numOpponentVisits = opponentVisits.get(opponentMoveIdx);

		return -1 * (opponentUtility) / (numOpponentVisits) - 0.5*opponentHeuristic / (numOpponentVisits)
				// + Math.sqrt(opponentVarSum/numOpponentVisits)
				+ explorationCoefficient * Math.sqrt(Math.log(totalVisits) / numOpponentVisits);
	}

	public Move getBestMove(long turnTimeout) throws PhaseTimeoutException {
		return getBestMoveAndUtility(turnTimeout).left;
	}

	public synchronized Pair<Move, Double> getBestMoveAndUtility(long turnTimeout) throws PhaseTimeoutException {
		if (isFullyExplored) {
			DebugLog.output(
					"Fully explored -- returning best move (" + fullyExploredBestMove + " " + fullyExploredValue + ")");
			return Pair.of(fullyExploredBestMove, fullyExploredValue);
		}
		Move bestMove = null;
		double bestUtility = 0.0;
		double bestHeur = 0.0;
		int numSame = 0;
		for (int i = 0; i < numPlayerMoves; i++) {
			PhaseTimeoutException.checkTimeout(turnTimeout);
			double averageUtility = playerVisits.get(i) == 0 ? 0 : playerUtil.get(i) / playerVisits.get(i);
			double averageHeur = playerHeur.get(i)/playerVisits.get(i);
			DebugLog.output("Average utility of " + playerMoves.get(i) + " is " + averageUtility);
			DebugLog.output("Heur of " + playerMoves.get(i) + " is " + averageHeur);
			DebugLog.output("Visits of " + playerMoves.get(i) + " is " + playerVisits.get(i));
			if (bestMove == null || bestUtility < averageUtility) {
				bestMove = playerMoves.get(i);
				bestUtility = averageUtility;
				bestHeur = averageHeur;
				numSame = 0;
			} else if (bestUtility == averageUtility){
				if(averageHeur < bestHeur && Math.random() < 0.25){
					bestMove = playerMoves.get(i);
					bestUtility = averageUtility;
					bestHeur = averageHeur;
					numSame = 0;
				} else if(averageHeur > bestHeur && Math.random() < 0.90){
					bestMove = playerMoves.get(i);
					bestUtility = averageUtility;
					bestHeur = averageHeur;
					numSame = 0;
				} else if (averageHeur == bestHeur) {
				numSame ++;
				double cutoff = ((double)numSame)/(numSame+1.0);
				float randomVal = new Random().nextFloat();
				if(randomVal > cutoff){
					DebugLog.output("Found multiple best utility");;
					bestMove = playerMoves.get(i);
				}
				}
			}

		}

		return Pair.of(bestMove, bestUtility);
	}

	private synchronized boolean fullyExploreNode(long turnTimeout) throws PhaseTimeoutException {
		if (isFullyExplored) {
			return true;
		}
		if (children.size() != numPlayerMoves * numOpponentMoves) {
			return false;
		}
		for (int i = 0; i < children.size(); i++) {
			MCTSNode child = children.get(i);
			if (System.currentTimeMillis() > turnTimeout)
				return false;
			if (!child.isFullyExplored) {
				return false;
			}
		}
		// DebugLog.output("All children fully explored -- collapsing state!");
		double minimaxScore = 0;
		Move minimaxMove = null;
		for (int i = 0; i < numPlayerMoves; i++) {
			if (System.currentTimeMillis() > turnTimeout)
				return false;
			double minScore = 0;
			boolean foundMinScore = false;
			for (int j = 0; j < numOpponentMoves; j++) {
				if (System.currentTimeMillis() > turnTimeout)
					return false;
				// do minimax here
				int childIdx = i * numOpponentMoves + j;
				double childFullyExploredVal = children.get(childIdx).fullyExploredValue;
				if (!foundMinScore || childFullyExploredVal < minScore) {
					minScore = childFullyExploredVal;
					foundMinScore = true;
				}
			}
			if (minScore > minimaxScore || minimaxMove == null) {
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
		return childIdx / numOpponentMoves;
	}

	private int getOpponentIdxFromChildIdx(int childIdx) {
		return childIdx % numOpponentMoves;
	}

	private synchronized MachineState expand(long turnTimeout, StateMachine machine) throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException, PhaseTimeoutException {
		if (children.size() >= numPlayerMoves * numOpponentMoves)
			return null;
		// expand

		if (System.currentTimeMillis() > turnTimeout)
			throw new PhaseTimeoutException();

		MachineState newState = null;

		synchronized (cachedNodes) {
			if (System.currentTimeMillis() > turnTimeout)
				throw new PhaseTimeoutException();

			int playerMoveIdx = getPlayerIdxFromChildIdx(children.size());
			int opponentMoveIdx = getOpponentIdxFromChildIdx(children.size());
			List<Move> jointMoves = opponentMoves.get(opponentMoveIdx);
			Move playerMove = playerMoves.get(playerMoveIdx);
			jointMoves.set(playerRoleIdx, playerMove);
			newState = machine.getNextState(state, jointMoves);

			if (cachedNodes.containsKey(newState)) {
				MCTSNode newChild = cachedNodes.get(newState);
				children.add(newChild);
			} else {

				//if (cachedNodes.size() > 2000) {
					//cachedNodes.clear();
				//}
				MCTSNode newChild = new MCTSNode(machine, newState, this, player, this.explorationCoefficient, cachedNodes,
						this.h);
				children.add(newChild);
				cachedNodes.put(newState, newChild);
			}
		}

		return newState;
	}

	public double simulate(MachineState newState, double timeout, StateMachine machine) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		// simulate
		// DebugLog.output("Performing depth charge");
		MachineState mcState = newState;
		while (!machine.isTerminal(mcState)) {
			if (System.currentTimeMillis() > timeout)
				return -1;
			mcState = machine.getRandomNextState(mcState);
		}
		//DebugLog.output("GOAL STATE: " + mcState);
		return machine.getGoal(mcState, player);
	}

	public double performIteration(long turnTimeout, boolean first) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException {
		return performIteration(turnTimeout, first, null, false);
	}

	public double performIteration(long turnTimeout, boolean first, boolean probe) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException {
		return performIteration(turnTimeout, first, null, probe);
	}

	public double performIteration(long turnTimeout, boolean first, StateMachine machine, boolean probe) throws TransitionDefinitionException,
			MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException {
		PhaseTimeoutException.checkTimeout(turnTimeout);

		if (isTerminal) {
			return terminalValue;
		}

		if (machine == null)
			machine = this.machine;

		if (System.currentTimeMillis() > turnTimeout)
			throw new PhaseTimeoutException();

		double bpr, heur;
		int selectedIdx, bestIdx = -1;
		MachineState newState = null;
		MCTSNode selectedChild = null;
		synchronized (this) {
			if (System.currentTimeMillis() > turnTimeout)
				throw new PhaseTimeoutException();

			if (fullyExploreNode(turnTimeout)) {
				return fullyExploredValue;
			}

			if (System.currentTimeMillis() > turnTimeout)
				throw new PhaseTimeoutException();

			if ((newState = expand(turnTimeout, machine)) == null) {
				// select best player move based on criteria
				double bestPlayerScore = 0;
				int bestPlayerIdx = 0;
				for (int i = 0; i < numPlayerMoves; i++) {
					if (probe) {
						if (System.currentTimeMillis() > turnTimeout)
							throw new PhaseTimeoutException();
						boolean allChildrenFullyExploredForPlayerMove = true;
						for (int j = 0; j < numOpponentMoves; j++) {
							if (System.currentTimeMillis() > turnTimeout)
								throw new PhaseTimeoutException();
							if (!children.get(numOpponentMoves * i + j).isFullyExplored) {
								allChildrenFullyExploredForPlayerMove = false;
								break;
							}
						}

						if (allChildrenFullyExploredForPlayerMove) {
							continue;
						}
					}
					if (first) {
						DebugLog.output("Calculating selection score for child " + playerMoves.get(i).toString());
					}
					double playerScore = getPlayerSelectionScore(i);
					if (first) {
						DebugLog.output("Selection score for child " + playerMoves.get(i).toString() + ":" + playerScore);
					}
					if (playerScore > bestPlayerScore) {
						bestPlayerIdx = i;
						bestPlayerScore = playerScore;
						if (first) {
							DebugLog.output("Best player score is now " + bestPlayerScore);
						}
					}
				}
				// select best opponent move based on criteria
				double bestOpponentScore = Integer.MIN_VALUE;
				int bestOpponentIdx = 0;
				for (int i = 0; i < numOpponentMoves; i++) {

					if(probe){
						if(children.get(numOpponentMoves * bestPlayerIdx + i).isFullyExplored){
							continue;
						}
					}

					if (System.currentTimeMillis() > turnTimeout)
						throw new PhaseTimeoutException();

					if (first) {
						DebugLog.output("Calculating selection score for child " + opponentMoves.get(i).toString());
					}
					double opponentScore = getOpponentSelectionScore(i);
					if (first) {
						DebugLog.output(
								"Selection score for child " + opponentMoves.get(i).toString() + ":" + opponentScore);
					}
					if (opponentScore > bestOpponentScore) {
						bestOpponentIdx = i;
						bestOpponentScore = opponentScore;
						if (first) {
							DebugLog.output("Best opponent score is now " + bestOpponentScore);
						}
					}
				}
				bestIdx = bestPlayerIdx * numOpponentMoves + bestOpponentIdx;
				if (first) {

					DebugLog.output("best/player/opponent:" + bestIdx + ", " + bestPlayerIdx + "," + bestOpponentIdx);
					List<Move> jointMoves = opponentMoves.get(bestOpponentIdx);
					Move playerMove = playerMoves.get(bestPlayerIdx);
					jointMoves.set(playerRoleIdx, playerMove);
					DebugLog.output("Willl expand " + jointMoves.toString());
				}
				selectedChild = children.get(bestIdx);

			}
		}

		// This is outside of the lock
		if (newState == null) { // Select
			heur = selectedChild.heurVal;
			bpr = selectedChild.performIteration(turnTimeout, false, machine, probe);
			if (bpr == -1)
				throw new PhaseTimeoutException();
			selectedIdx = bestIdx;
		} else { // Expand and simulate
			selectedIdx = children.size() - 1;
			bpr = simulate(newState, turnTimeout, machine);
			if (bpr == -1) {
				throw new PhaseTimeoutException();
			}
			heur = children.get(selectedIdx).heurVal;
		}


		// update stats
		int playerMoveIdx = getPlayerIdxFromChildIdx(selectedIdx);
		int opponentMoveIdx = getOpponentIdxFromChildIdx(selectedIdx);
		if (first) {
			DebugLog.output("best/player/opponent:" + selectedIdx + ", " + playerMoveIdx + "," + opponentMoveIdx);
			//FIXME Race condition here
			//DebugLog.output("Player move is " + playerMoves.get(playerMoveIdx));
			//DebugLog.output("Opponent move is " + opponentMoves.get(opponentMoveIdx));
		}

		synchronized (this) {
			if (System.currentTimeMillis() > turnTimeout)
				throw new PhaseTimeoutException();

			updateUtilStats(bpr, playerMoveIdx, this.playerVisits, this.playerUtil);
			updateUtilStats(bpr, opponentMoveIdx, this.opponentVisits, this.opponentUtil);
			this.playerHeur.set(playerMoveIdx, this.playerHeur.get(playerMoveIdx) + heur);
			this.opponentHeur.set(opponentMoveIdx, this.opponentHeur.get(opponentMoveIdx) + heur);
			this.combinedMoveVisits.set(selectedIdx, this.combinedMoveVisits.get(selectedIdx) + 1);
			this.totalVisits += 1;

			// refer to
			// https://math.stackexchange.com/questions/20593/calculate-variance-from-a-stream-of-sample-values/116344#116344

			if (totalVisits == 1) {
				utilMean = bpr;
				utilVar = 0;
			} else {
				double m_k_1 = utilMean;
				double m_k = m_k_1 + (bpr - m_k_1) / totalVisits;
				double v_k_1 = utilVar;
				double v_k = v_k_1 + (bpr - m_k_1) * (bpr - m_k);
				utilMean = m_k;
				utilVar = v_k;
			}

			// DebugLog.output("Mean is "+ utilMean);
			// DebugLog.output("Setting exploration coef to "+
			// Math.max(Math.sqrt(utilVariance/(totalVisits-1)), 0));
			// TODO: uncomment following line to set exploration coefficient based
			// on variance of utility
			this.explorationCoefficient = Math.max(10,Math.sqrt(utilVar / (totalVisits)));

			if (System.currentTimeMillis() > turnTimeout)
				return bpr;

			if (fullyExploreNode(turnTimeout)) {
				return fullyExploredValue;
			}
		}

		return bpr;
	}

	private synchronized void updateUtilStats(double newUtil, int index, List<Integer> visits, List<Double> utils) {
		visits.set(index, visits.get(index) + 1);
		utils.set(index, utils.get(index) + newUtil);
	}

	public synchronized MCTSNode updateState(MachineState newState) {
		if (this.state.equals(newState)) {
			// handle case where init state is passed in
			return this;
		}
		// TODO Auto-generated method stub
		for (int i = 0; i < children.size(); i++) {
			MCTSNode child = children.get(i);
			if (child.state.equals(newState)) {
				return child;
			}
		}
		return null;

	}

}
