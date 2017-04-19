package org.ggp.dhtp;
import java.util.Arrays;
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

public class DontHateThePlayer extends StateMachineGamer {

	Player p;
	boolean singlePlayer = false;

	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		if(machine.getRoles().size() == 1){
			System.out.println("Single player game");
			singlePlayer = true;
		} else {
			System.out.println("Multiplayer game");
			singlePlayer = false;
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if(singlePlayer){
			return compulsiveStateMachineSelectMove(timeout);
		} else {
			return alphaBetaStateMachineSelectMove(timeout);
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
		return "Don't hate the player";
	}

	// NOTE: If this method is changed to type (Role x State x Action -> Integer)
		// then the code will be cleaner.  That would differ from the notes, though, so I
		// just left it this way.
		private int compulsiveMaxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
			StateMachine machine = getStateMachine();
			if (machine.isTerminal(state)) {
				return machine.getGoal(state, role);
			}

			List<Move> moves = machine.getLegalMoves(state,role);
			int score = 0;
			for (Move move : moves) {
				int moveScore = compulsiveMaxScore(role, machine.getNextState(state, Arrays.asList(move)));
				if (moveScore > score)
					score = moveScore;
			}

			return score;
		}

		public Move compulsiveStateMachineSelectMove(long timeout)
				throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
			StateMachine machine = getStateMachine();
			MachineState state = getCurrentState();
			Role role = getRole();
			List<Move> moves = machine.getLegalMoves(state,role);
			Move selectedMove = moves.get(0);
			int score = 0;
			for (Move move : moves) {
				int moveScore = compulsiveMaxScore(role, machine.getNextState(state, Arrays.asList(move)));
				if (moveScore == 100) {
					return move;
				} else if (moveScore > score) {
					score = moveScore;
					selectedMove = move;
				}
			}

			return selectedMove;
		}

		public Move alphaBetaStateMachineSelectMove(long timeout)
				throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
			StateMachine machine = getStateMachine();
			MachineState state = getCurrentState();
			Role role = getRole();
			return alphaBetaBestMove(role, machine, state);
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

		private Move alphaBetaBestMove(Role role, StateMachine machine, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
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

				int result = alphaBetaMinScore(machine, nextState, alpha, beta);
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


		private int alphaBetaMinScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
			if(machine.isTerminal(state)){
				int roleIdx = machine.getRoleIndices().get(getRole());
				return machine.getGoals(state).get(roleIdx);
			} else {
				Role mover = getOpponent(machine);
				//System.out.println("Mover is "+mover.toString());

				List<Move> moves = machine.getLegalMoves(state, mover);
				for (Move move : moves) {
					//System.out.println("Min considering " + move.toString());
					int result = alphaBetaMaxScore(machine, machine.getNextState(state, machine.getLegalJointMoves(state, mover, move).get(0)), alpha, beta);
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

		private int alphaBetaMaxScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
			if(machine.isTerminal(state)){
				int roleIdx = machine.getRoleIndices().get(getRole());
				return machine.getGoals(state).get(roleIdx);
			} else {
				Role mover = getRole();

				List<Move> moves = machine.getLegalMoves(state, mover);
				for (Move move : moves) {
					int result = alphaBetaMinScore(machine, machine.getNextState(state, machine.getLegalJointMoves(state, mover, move).get(0)), alpha, beta);
					alpha = Math.max(alpha, result);
					if(beta <= alpha){
						return beta;
					}
				}
				return alpha;

			}
		}

}
