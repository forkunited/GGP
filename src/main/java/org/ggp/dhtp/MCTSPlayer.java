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
	private static final double TIMEOUT_SAFETY_MARGIN = 0.75;
	private static final double BEST_MOVE_SELECTION_MARGIN = 0.05;
	private static final double EXPLORATION_COEFFICIENT = 70.0;
	private MCTSNode currNode;

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
		long turnTimeout = (long)(TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
		int numDepthCharges = 0;

		StateMachine machine = getStateMachine();
		Role role = getRole();

		DebugLog.output("Could not find node in search tree - creating new MCTS tree");
		currNode = new MCTSNode(machine, machine.getInitialState(), null, role, EXPLORATION_COEFFICIENT);


		try{
			while (System.currentTimeMillis() < turnTimeout) {
				currNode.performIteration(turnTimeout);
				numDepthCharges++;
			}
		} catch (Exception e){
			System.out.println(e);
		}
		finally {
			DebugLog.output("Metagame Num Depth Charges:"+numDepthCharges);
		}
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
			long turnTimeout = (long)(TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			long mctsTimeout = (long)((TIMEOUT_SAFETY_MARGIN-BEST_MOVE_SELECTION_MARGIN) * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			int numDepthCharges = 0;
			if(currNode != null){
				currNode = currNode.updateState(state);
			}

			if(currNode == null){
				DebugLog.output("Could not find node in search tree - creating new MCTS tree");
				currNode = new MCTSNode(machine, state, null, role, EXPLORATION_COEFFICIENT);
			}

			try{
				while (System.currentTimeMillis() < mctsTimeout) {
					currNode.performIteration(mctsTimeout);
					numDepthCharges++;
				}
			} finally {
				DebugLog.output("Num Depth Charges:"+numDepthCharges);
				bestMove = currNode.getBestMove(turnTimeout);
			}
		} catch (Exception e) {
			System.out.println(e);
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
