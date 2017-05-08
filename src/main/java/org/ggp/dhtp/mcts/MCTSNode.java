package org.ggp.dhtp.mcts;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class MCTSNode {


	List<Double> playerUtil, opponentUtil;
	List<Integer> playerVisits, opponentVisits, combinedMoveVisits;
	MCTSNode parent;
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

	public MCTSNode(StateMachine machine, MachineState state, MCTSNode parent, Role player, double explorationCoefficient) throws MoveDefinitionException, GoalDefinitionException{
		this.machine = machine;
		this.state = state;
		this.parent = parent;
		this.playerUtil = null;
		this.playerVisits = null;
		this.opponentUtil = null;
		this.opponentVisits = null;
		this.player = player;
		this.totalVisits = 0;
		this.explorationCoefficient = explorationCoefficient;
		if(machine.isTerminal(state)){
			this.isTerminal = true;
			this.terminalValue = machine.getGoal(state, player);
		} else {
			this.playerRoleIdx = machine.getRoleIndices().get(player);
			this.playerMoves = machine.getLegalMoves(state, player);
			this.opponentMoves = machine.getLegalJointMoves(state, player, playerMoves.get(0));
			this.numPlayerMoves = playerMoves.size();
			this.numOpponentMoves = opponentMoves.size();

			this.playerUtil = new ArrayList<Double>(numPlayerMoves);
			this.playerVisits = new ArrayList<Integer>(numPlayerMoves);
			for(int i = 0; i < numPlayerMoves; i++){
				this.playerUtil.add(0.0);
				this.playerVisits.add(0);
			}
			this.opponentUtil = new ArrayList<Double>(numOpponentMoves);
			this.opponentVisits = new ArrayList<Integer>(numOpponentMoves);

			for(int i = 0; i < numOpponentMoves; i++){
				this.opponentUtil.add(0.0);
				this.opponentVisits.add(0);
			}

			this.combinedMoveVisits = new ArrayList<Integer>(numPlayerMoves*numOpponentMoves);

			for(int i = 0; i < numPlayerMoves*numOpponentMoves; i++){
				this.combinedMoveVisits.add(0);
			}

			this.children = new ArrayList<MCTSNode>();
		}
	}

	private double getSelectionScore(int childIdx){
		int playerMoveIdx = getPlayerIdxFromChildIdx(childIdx);
		int opponentMoveIdx = getOpponentIdxFromChildIdx(childIdx);

		DebugLog.output("Player Idx:"+playerMoveIdx);
		DebugLog.output("Opponent Idx:"+opponentMoveIdx);

		double playerUtility = playerUtil.get(playerMoveIdx);
		double opponentUtility = opponentUtil.get(opponentMoveIdx);
		int numPlayerVisits = playerVisits.get(playerMoveIdx);
		int numOpponentVisits = opponentVisits.get(opponentMoveIdx);
		int numCombinedMoveVisits = combinedMoveVisits.get(childIdx);


		DebugLog.output("Player Util :"+playerUtility);
		DebugLog.output("Opponent Util :"+opponentUtility);
		DebugLog.output("Player Visits :"+numPlayerVisits);
		DebugLog.output("Opponent Visits :"+numOpponentVisits);
		DebugLog.output("Combined Move Visits"+numCombinedMoveVisits);
		DebugLog.output("Total Visits"+totalVisits);

		return -1*opponentUtility/numOpponentVisits
				+ playerUtility/numPlayerVisits
				+ explorationCoefficient * Math.sqrt(Math.log(totalVisits)/numCombinedMoveVisits);
	}

	public Move getBestMove(long turnTimeout) throws PhaseTimeoutException{
		Move bestMove = null;
		double bestUtility = 0;
		for(int i = 0; i < numPlayerMoves; i++){
			PhaseTimeoutException.checkTimeout(turnTimeout);
			double averageUtility = playerVisits.get(i) == 0 ? 0 : playerUtil.get(i) / playerVisits.get(i);
			DebugLog.output("Utility for move "+i+" is "+averageUtility);
			if(bestMove == null || bestUtility < averageUtility){
				bestMove = playerMoves.get(i);
				bestUtility = averageUtility;
			}

		}
		return bestMove;
	}


	private int getPlayerIdxFromChildIdx(int childIdx){
		return childIdx % numPlayerMoves;
	}

	private int getOpponentIdxFromChildIdx(int childIdx){
		return  childIdx / numPlayerMoves;
	}

	private double expandAndSimulate() throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		//expand
		int playerMoveIdx = getPlayerIdxFromChildIdx(children.size());
		int opponentMoveIdx = getOpponentIdxFromChildIdx(children.size());
		List<Move> jointMoves = opponentMoves.get(opponentMoveIdx);
		Move playerMove = playerMoves.get(playerMoveIdx);
		jointMoves.set(playerRoleIdx, playerMove);
		MachineState newState = machine.getNextState(state, jointMoves);
		MCTSNode newChild = new MCTSNode(machine, newState, this, player, this.explorationCoefficient);
		children.add(newChild);

		//simulate
		MachineState mcState = newState;
		while(!machine.isTerminal(mcState)){
			//TODO: add timeout
			mcState = machine.getRandomNextState(mcState);
		}

		return machine.getGoal(mcState, player);
	}

	public double performIteration(long turnTimeout) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException{
		PhaseTimeoutException.checkTimeout(turnTimeout);

		if(isTerminal){
			return terminalValue;
		}

		double bpr;
		int selectedIdx;
		if(children.size() < numPlayerMoves*numOpponentMoves){ // in expansion phase
			selectedIdx = children.size();
			bpr = expandAndSimulate();
		} else {
			double bestScore = 0;
			int bestIdx = 0;
			for (int i =0; i < children.size(); i++){
				DebugLog.output("Calculating selection score for child "+i);
				double score = getSelectionScore(i);
				DebugLog.output("Selection score for child "+i+":"+score);
				if(score > bestScore){
					bestIdx = i;
					bestScore = score;
				}
			}
			MCTSNode selectedChild = children.get(bestIdx);
			bpr =  selectedChild.performIteration(turnTimeout);
			selectedIdx = bestIdx;
		}
		//update stats
		int playerMoveIdx = getPlayerIdxFromChildIdx(selectedIdx);
		int opponentMoveIdx = getOpponentIdxFromChildIdx(selectedIdx);

		this.playerUtil.set(playerMoveIdx, this.playerUtil.get(playerMoveIdx)+bpr);
		this.opponentUtil.set(opponentMoveIdx, this.opponentUtil.get(opponentMoveIdx)+bpr);
		this.playerVisits.set(playerMoveIdx, this.playerVisits.get(playerMoveIdx)+1);
		this.opponentVisits.set(opponentMoveIdx, this.opponentVisits.get(opponentMoveIdx)+1);
		this.combinedMoveVisits.set(selectedIdx, this.combinedMoveVisits.get(selectedIdx)+1);
		this.totalVisits+=1;

		return bpr;
	}

}
