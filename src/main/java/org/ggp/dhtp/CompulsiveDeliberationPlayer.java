package org.ggp.dhtp;
import java.util.Arrays;
import java.util.List;

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

/**
 * CompulsiveDeliberationPlayer for single player games described at
 * http://logic.stanford.edu/ggp/notes/chapter_05.html
 *
 * @author Bill McDowell
 *
 */
public class CompulsiveDeliberationPlayer extends StateMachineGamer {
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

	// NOTE: If this method is changed to type (Role x State x Action -> Integer)
	// then the code will be cleaner.  That would differ from the notes, though, so I
	// just left it this way.
	private int maxScore(Role role, MachineState state) throws GoalDefinitionException, MoveDefinitionException, TransitionDefinitionException {
		StateMachine machine = getStateMachine();
		if (machine.isTerminal(state)) {
			return machine.getGoal(state, role);
		}

		List<Move> moves = machine.getLegalMoves(state,role);
		int score = 0;
		for (Move move : moves) {
			int moveScore = maxScore(role, machine.getNextState(state, Arrays.asList(move)));
			if (moveScore > score)
				score = moveScore;
		}

		return score;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		List<Move> moves = machine.getLegalMoves(state,role);
		Move selectedMove = moves.get(0);
		int score = 0;
		for (Move move : moves) {
			int moveScore = maxScore(role, machine.getNextState(state, Arrays.asList(move)));
			if (moveScore == 100) {
				return move;
			} else if (moveScore > score) {
				score = moveScore;
				selectedMove = move;
			}
		}

		return selectedMove;
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
		return "Don't hate the compulsive deliberation player";
	}

}
