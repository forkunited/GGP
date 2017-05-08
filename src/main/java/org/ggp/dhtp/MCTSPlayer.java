package org.ggp.dhtp;

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
import org.ggp.dhtp.mcts.MCTSNode;
import org.ggp.dhtp.util.DebugLog;

public class MCTSPlayer extends StateMachineGamer {
	private static final double TIMEOUT_SAFETY_MARGIN = 0.25;
	private static final double BEST_MOVE_SELECTION_MARGIN = 0.05;
	private static final double EXPLORATION_COEFFICIENT = 1.0; // learning rate

	Player p;

	private long getTimeoutDuration(long timeout) {
		return (long) ((1.0 - TIMEOUT_SAFETY_MARGIN) * (timeout - System.currentTimeMillis()));
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		Move randomMove = machine.getRandomMove(state, role);
		Move bestMove = null;
		try {
			long mcsTimeout = (long)((TIMEOUT_SAFETY_MARGIN + BEST_MOVE_SELECTION_MARGIN) * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			long turnTimeout = (long)(TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			int numDepthCharges = 0;
			MCTSNode rootNode = new MCTSNode(machine, state, null, role, EXPLORATION_COEFFICIENT);

			try{
				while (System.currentTimeMillis() < mcsTimeout) {
					rootNode.performIteration(mcsTimeout);
					numDepthCharges++;
				}
			} finally {
				DebugLog.output("Num Depth Charges:"+numDepthCharges);
				bestMove = rootNode.getBestMove(turnTimeout);
			}
		} catch (Exception e) {
			if (bestMove == null) {
				DebugLog.output("Picking Random Move");
				bestMove = randomMove;
			}
		}
		return bestMove;
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
		return "Don't hate the MCTS player";
	}

}
