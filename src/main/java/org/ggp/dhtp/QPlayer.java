package org.ggp.dhtp;
import java.util.Arrays;

import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;
import org.ggp.dhtp.learn.LearnerQGlobalLinear;
import org.ggp.dhtp.learn.SAFeature;
import org.ggp.dhtp.learn.SAFeatureBoundedDepthHeuristic;
import org.ggp.dhtp.learn.SAFeatureSet;
import org.ggp.dhtp.util.GoalProximityHeuristic;
import org.ggp.dhtp.util.HeuristicFreedom;
import org.ggp.dhtp.util.HeuristicOpponentFreedom;

public class QPlayer extends StateMachineGamer {
	private static final double TIMEOUT_SAFETY_MARGIN = 0.25;
	private static final double ALPHA = 0.01; // learning rate
	private static final double EPSILON = 0.1; // For epsilon greedy exploration
	private static final int LEARNING_ITERATIONS = 100;

	private LearnerQGlobalLinear agent;

	private long getTimeoutDuration(long timeout) {
		return (long)((1.0 - TIMEOUT_SAFETY_MARGIN)*(timeout - System.currentTimeMillis()));
	}

	@Override
	public StateMachine getInitialStateMachine() {
		return new CachedStateMachine(new ProverStateMachine());
	}


	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		SAFeatureSet features = new SAFeatureSet(Arrays.asList(new SAFeature[] {
			new SAFeatureBoundedDepthHeuristic(new HeuristicFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY), 3),
			new SAFeatureBoundedDepthHeuristic(new HeuristicFreedom(getStateMachine(), HeuristicFreedom.Type.FOCUS), 3),
			new SAFeatureBoundedDepthHeuristic(new HeuristicOpponentFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY), 3),
			new SAFeatureBoundedDepthHeuristic(new HeuristicOpponentFreedom(getStateMachine(), HeuristicFreedom.Type.FOCUS), 3),
			new SAFeatureBoundedDepthHeuristic(new GoalProximityHeuristic(getStateMachine(), GoalProximityHeuristic.Mode.ONLY_NONTERMINAL), 3),
			new SAFeatureBoundedDepthHeuristic(new GoalProximityHeuristic(getStateMachine(), GoalProximityHeuristic.Mode.ONLY_TERMINAL), 3)
		}));

		this.agent = new LearnerQGlobalLinear(getRole(), getStateMachine(), features, ALPHA, EPSILON);
		this.agent.learn(LEARNING_ITERATIONS, getTimeoutDuration(timeout));
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return this.agent.act(getCurrentState(), getTimeoutDuration(timeout));
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
