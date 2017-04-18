package org.ggp.dhtp;
import java.util.ArrayList;
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

public class MinimaxPlayer extends StateMachineGamer {

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
		List<Move> moves = machine.getLegalMoves(state,role);
		return bestMove(moves, role, state);
	}

	private Role getOpponent(StateMachine machine) {
		Map<Role,Integer> roleIdxs = machine.getRoleIndices();
		for(Role other : machine.getRoles()){
			if(roleIdxs.get(other) != roleIdxs.get(getRole())){
				return other;
			}
		}
		throw new Error("Single player game");
	}

	private List<Move> makePlayerMoves(Move opMove, Move playerMove, Role opponent) {
		ArrayList nextMoves = new ArrayList<Move>();
		StateMachine machine = getStateMachine();
		Map<Role,Integer> roleIdxs = machine.getRoleIndices();
		if (roleIdxs.get(getRole()) < roleIdxs.get(opponent)) {
			// player precedes opponent in move list
			nextMoves.add(playerMove);
			nextMoves.add(opMove);
		} else {
			nextMoves.add(opMove);
			nextMoves.add(playerMove);
		}
		return nextMoves;
	}

	private Move bestMove(List<Move> moves, Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		Move chosenMove = moves.get(0);
		int maxScore = 0;
		int result;
		for (Move move : moves) {
			result = minScore(role, move, state);
			if (result > maxScore) {
				maxScore = result;
				chosenMove = move;
			}
		}

		return chosenMove;
	}

	private int minScore(Role role, Move move, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		Role opponent = getOpponent(machine);
		List<Move> moves = machine.getLegalMoves(state, opponent);
		int score = 100;
		int result;
		for (Move opMove : moves) {
			// create the proper order of moves to add to structure
			List<Move> playerMoves = makePlayerMoves(opMove, move, opponent);
			MachineState newState = machine.getNextState(state, playerMoves);
			result = maxScore(role, newState);
			if (result < score) {
				score = result;
			}
		}
		return score;
	}

	private int maxScore(Role role, MachineState state) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException{
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}
		List<Move> moves = machine.getLegalMoves(state,role);
		int score = 0;
		int result;
		for (Move move: moves) {
			result = minScore(role,move,state);
			if (result > score) {
				score = result;
			}
		}
		return score;
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
		return "Don't hate the minimax player";
	}

}
