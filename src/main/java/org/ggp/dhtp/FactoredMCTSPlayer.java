package org.ggp.dhtp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.ggp.base.apps.player.Player;
import org.ggp.base.player.gamer.exception.GamePreviewException;
import org.ggp.base.player.gamer.statemachine.StateMachineGamer;
import org.ggp.base.util.Pair;
import org.ggp.base.util.game.Game;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.cache.CachedStateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;
import org.ggp.dhtp.mcts.MCTSNode;
import org.ggp.dhtp.propnet.PropNetAnalyzer;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class FactoredMCTSPlayer extends StateMachineGamer {
	private static final double TIMEOUT_SAFETY_MARGIN = 0.75;
	private static final double FACTOR_SAFETY_MARGIN = 0.50;
	private static final double BEST_MOVE_SELECTION_MARGIN = 0.10;
	private static final double EXPLORATION_COEFFICIENT = 120.0;

	private SamplePropNetStateMachine propNetMachine;

	private List<MCTSNode> currNodes;
	private List<CachedStateMachine> factoredMachines;

	Player p;

	/*
	 * private long getTimeoutDuration(long timeout) { return (long) ((1.0 -
	 * TIMEOUT_SAFETY_MARGIN) * (timeout - System.currentTimeMillis())); }
	 */

	@Override
	public StateMachine getInitialStateMachine() {
		this.propNetMachine = new SamplePropNetStateMachine();
		return new CachedStateMachine(this.propNetMachine);
	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// this.propNetMachine.getPropNet().renderToFile("/home/vk/0.dot");
		System.gc();
		DebugLog.output("Start Metagame");
		long factorTimeout = (long) (FACTOR_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
				+ System.currentTimeMillis();
		this.factoredMachines = new ArrayList<CachedStateMachine>();
		this.currNodes = new ArrayList<MCTSNode>();
		if (this.getStateMachine().getRoleIndices().size() == 1) {
			try {
				DebugLog.output("Propnet Analyzer Start");
				PropNetAnalyzer analyzer = new PropNetAnalyzer();
				DebugLog.output("Propnet Analyzer factor terminal goal reachable");
				PropNet reachablePropNet = analyzer.factorTerminalGoalReachable(this.propNetMachine.getPropNet(),
						factorTimeout);
				DebugLog.output("Propnet Analyzer factor disjunctive");
				List<PropNet> propNets = analyzer.factorDisjunctive(reachablePropNet, factorTimeout);

				for (int i = 0; i < propNets.size(); i++) {
					PropNet propNet = propNets.get(i);
					SamplePropNetStateMachine factoredMachine = new SamplePropNetStateMachine();
					factoredMachine.initialize(propNet); // This "initialize"
															// thing
															// is annoying. But
															// simplest given
															// existing codebase
					this.factoredMachines.add(new CachedStateMachine(factoredMachine));
					// propNet.renderToFile("C:/Users/forku_000/Documents/courses/spring17/cs227b/graphs/output"
					// + i + ".dot");
				}

				DebugLog.output("Reduced propnet from " + this.propNetMachine.getPropNet().getComponents().size()
						+ " to " + reachablePropNet.getComponents().size());
				DebugLog.output("Factored into " + this.factoredMachines.size() + " propnets");
			} catch (PhaseTimeoutException pte) {
				this.factoredMachines = new ArrayList<CachedStateMachine>();
				this.factoredMachines.add(new CachedStateMachine(this.propNetMachine));
			}
		} else {
			// SamplePropNetStateMachine singleMachine = new
			// SamplePropNetStateMachine();
			// PropNet propNet = this.propNetMachine.getPropNet();
			// singleMachine.initialize(propNet);
			this.factoredMachines.add(new CachedStateMachine(this.propNetMachine));
		}
		long turnTimeout = (long) (TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
				+ System.currentTimeMillis();
		int numDepthCharges = 0;
		Role role = getRole();
		// this.propNetMachine.getPropNet().renderToFile("/home/vk/1.dot");

		DebugLog.output("Could not find node in search tree - creating new MCTS tree");
		for (CachedStateMachine machine : this.factoredMachines) {
			this.currNodes.add(new MCTSNode(machine, machine.getInitialState(), null, role, EXPLORATION_COEFFICIENT,
					new HashMap<MachineState, MCTSNode>()));
		}

		try {
			while (System.currentTimeMillis() < turnTimeout) {
				for (MCTSNode node : this.currNodes)
					node.performIteration(turnTimeout);
				numDepthCharges++;
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			DebugLog.output("Metagame Num Depth Charges:" + numDepthCharges);
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		DebugLog.output("Start select move");
		System.gc();
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		Move randomMove = machine.getRandomMove(state, role);
		Move bestMove = null;
		try {
			long turnTimeout = (long) (TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
					+ System.currentTimeMillis();
			long mctsTimeout = (long) ((TIMEOUT_SAFETY_MARGIN - BEST_MOVE_SELECTION_MARGIN)
					* (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			int numDepthCharges = 0;

			for (int i = 0; i < this.currNodes.size(); i++) {
				MCTSNode currNode = this.currNodes.get(i);

				if (currNode != null) {
					currNode = currNode.updateState(state);
				}

				if (currNode == null) {
					DebugLog.output("Could not find node in search tree - creating new MCTS tree");
					currNode = new MCTSNode(machine, state, null, role, EXPLORATION_COEFFICIENT,
							new HashMap<MachineState, MCTSNode>());
				}

				this.currNodes.set(i, currNode);
			}

			try {
				boolean allFullyExplored = false;
				while (System.currentTimeMillis() < mctsTimeout && !allFullyExplored) {
					allFullyExplored = true;
					for (MCTSNode currNode : this.currNodes) {
						currNode.performIteration(mctsTimeout);
						numDepthCharges++;

						if (!currNode.isFullyExplored())
							allFullyExplored = false;
					}
				}
			} finally {
				DebugLog.output("Num Depth Charges:" + numDepthCharges);
				DebugLog.output("Picking best move");
				// FIXME Check for noops?
				Pair<Move, Double> bestMoveAndUtility = null;
				for (MCTSNode currNode : this.currNodes) {
					Pair<Move, Double> curMoveAndUtility = currNode.getBestMoveAndUtility(turnTimeout);
					if (bestMoveAndUtility == null || curMoveAndUtility.right > bestMoveAndUtility.right)
						bestMoveAndUtility = curMoveAndUtility;
				}
				bestMove = bestMoveAndUtility.left;
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
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
		return "Don't hate the factored MCTS player";
	}

}
