package org.ggp.dhtp;
import java.util.List;
import java.util.Map;

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

public class AlphaBetaPlayer extends StateMachineGamer {

	Player p;

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
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		return bestMove(role, machine, state);
	}

	private Role getOpponent(StateMachine machine) throws MoveDefinitionException{
		//pick some other mover
		Map<Role,Integer> roleIdxs = machine.getRoleIndices();
		for(Role other : machine.getRoles()){
			if(roleIdxs.get(other) != roleIdxs.get(getRole())){
				return other;
			}
		}

		throw new Error("Single player game");
	}

	private Move bestMove(Role role, StateMachine machine, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		int bestScore = 0;
		Move bestMove = null;
		List<Move> moves = machine.getLegalMoves(state, role);

		if(moves.size() == 1){
			System.out.println("Only one choice: picking " + moves.get(0).toString());
			return moves.get(0);
		}

		int alpha = 0;
		int beta = 100;

		for(Move move : moves){
			List<Move> nextMoves = machine.getLegalJointMoves(state, role, move).get(0);

			//System.out.println("Considering " + move.toString());
			MachineState nextState = machine.getNextState(state, nextMoves);

			int result = minScore(machine, nextState, alpha, beta);
			System.out.println("Min score for " + move.toString() +" is "+ result);


			if(result > bestScore || bestMove == null){
				bestMove = move;
				bestScore = result;
				alpha = bestScore;
			}
		}
		System.out.println("Picking " + bestMove.toString());
		return bestMove;
	}


	private int minScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if(machine.isTerminal(state)){
			int roleIdx = machine.getRoleIndices().get(getRole());
			return machine.getGoals(state).get(roleIdx);
		} else {
			Role mover = getOpponent(machine);
			//System.out.println("Mover is "+mover.toString());

			List<Move> moves = machine.getLegalMoves(state, mover);
			for (Move move : moves) {
				//System.out.println("Min considering " + move.toString());
				int result = maxScore(machine, machine.getNextState(state, machine.getLegalJointMoves(state, mover, move).get(0)), alpha, beta);
				beta = Math.min(beta, result);
				//System.out.println("Min considering " + move.toString()+" with score " + result);
				if(beta <= alpha){
					return alpha;
				}
			}
			//System.out.println("Best score for min is " + bestScore);
			return beta;

		}
	}

	private int maxScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		if(machine.isTerminal(state)){
			int roleIdx = machine.getRoleIndices().get(getRole());
			return machine.getGoals(state).get(roleIdx);
		} else {
			Role mover = getRole();

			List<Move> moves = machine.getLegalMoves(state, mover);
			for (Move move : moves) {
				int result = minScore(machine, machine.getNextState(state, machine.getLegalJointMoves(state, mover, move).get(0)), alpha, beta);
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
		return "Don't hate the alphabeta player";
	}

}
