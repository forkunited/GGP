package org.ggp.dhtp;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

public class BoundedDepthPlayer extends StateMachineGamer {

	Player p;
	int shiftwidth =0;
	int turn =0;
	boolean DEBUG = false;
	private void print_debug(String message) {
		if (!DEBUG || turn < 4){
			return;
		}
		System.out.print(turn);
		for (int i=0; i<= shiftwidth; i++) {
			System.out.print("  ");
		}
		System.out.println(message);
	}



	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// TODO Auto-generated method stub

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		turn++;
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		return bestMove(role, machine, state);
	}


	private Move bestMove(Role role, StateMachine machine, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		int bestScore = 0;
		Move bestMove = null;
		List<Move> moves = machine.getLegalMoves(state, role);

		if(moves.size() == 1){
			print_debug("Only one choice: picking " + moves.get(0).toString());
			return moves.get(0);
		}

		int alpha = 0;
		int beta = 100;

		for(Move move : moves){
//			List<Move> nextMoves = machine.getLegalJointMoves(state, role, move).get(0);

			print_debug("Considering " + move.toString());
			shiftwidth++;
			int result = minScore(machine, move, state, alpha, beta);
			shiftwidth--;
			print_debug("Min score for " + move.toString() +" is "+ result);


			if(result > bestScore || bestMove == null){
				bestMove = move;
				bestScore = result;
				alpha = bestScore;
			}
		}
		print_debug("Picking " + bestMove.toString());
		return bestMove;
	}


	private int minScore(StateMachine machine, Move playerMove, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{

		// System.out.println("Mover is "+mover.toString());

		List<List<Move>> moves = machine.getLegalJointMoves(state, getRole(), playerMove);
		for (List<Move> move : moves) {
			print_debug("Min considering " + move.toString());
			int result = maxScore(machine, machine.getNextState(state, move), alpha, beta);
			beta = Math.min(beta, result);
			print_debug("Min considering " + move.toString()+" with score " + result);
			if (beta <= alpha) {
				print_debug("Pruning ... ");
				return alpha;
			}
		}
		print_debug("Best score for min is " + beta);
		return beta;

	}

	private int maxScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if(machine.isTerminal(state)){
			int roleIdx = machine.getRoleIndices().get(getRole());
			print_debug("At terminal state. Score is: " + machine.getGoals(state).get(roleIdx));
			return machine.getGoals(state).get(roleIdx);
		} else {
			List<Move> moves = machine.getLegalMoves(state, getRole());
			for (Move move : moves) {
				print_debug("Max considering " + move.toString());
				shiftwidth++;
				int result = minScore(machine, move, state, alpha, beta);
				shiftwidth--;
				print_debug("Max received " + result);
				alpha = Math.max(alpha, result);
				if(beta <= alpha){
					return beta;
				}
			}
			return alpha;
		}
	}


	@Override
	public void stateMachineStop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stateMachineAbort() {
		// TODO Auto-generated method stub

	}

	@Override
	public void preview(Game g, long timeout) throws GamePreviewException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Don't hate the bounded depth player";
	}

}
