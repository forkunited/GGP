package org.ggp.dhtp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

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
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.base.util.statemachine.implementation.propnet.SamplePropNetStateMachine;
import org.ggp.dhtp.mcts.MCTSNode;
import org.ggp.dhtp.propnet.PropNetAnalyzer;
import org.ggp.dhtp.util.Bounder;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.FixedBounder;
import org.ggp.dhtp.util.GoalProximityHeuristic;
import org.ggp.dhtp.util.Heuristic;
import org.ggp.dhtp.util.HeuristicFreedom;
import org.ggp.dhtp.util.HeuristicOpponentFreedom;
import org.ggp.dhtp.util.HeuristicWeighted;
import org.ggp.dhtp.util.MCSHeuristic;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class FactoredMCTSPlayer extends StateMachineGamer {
	private static final double TIMEOUT_SAFETY_MARGIN = 0.75;
	private static final double METAGAME_TIMEOUT_SAFETY_MARGIN = 0.75;
	private static final double FACTOR_SAFETY_MARGIN = 0.25;
	private static final double MCTS_SAFETY_MARGIN = 0.50;
	private static final double BEST_MOVE_SELECTION_MARGIN = 0.10;
	private static final double EXPLORATION_COEFFICIENT = 60.0;
	private static final double DEPTH_CHARGE_PER_SECOND_HEUR_CUTOFF = 0.0;

	private SamplePropNetStateMachine propNetMachine;

	private List<MCTSNode> currNodes;
	private List<StateMachine> factoredMachines;

	boolean reachedAllTerminal;
	boolean runHeur;
	int maxLevel;
	int shiftwidth;
	Bounder b;
	Heuristic h;
	Player p;

	class MoveContainer {
		private Move move;
		private int score;
		boolean timedOut;

		public MoveContainer(Move move, int score, boolean timedOut) {
			this.move = move;
			this.score = score;
			this.timedOut = timedOut;
		}

		public boolean getTimedOut() {
			return timedOut;
		}

		public Move getMove() {
			return move;
		}

		public int getScore() {
			return score;
		}
	}

	/*
	 * private long getTimeoutDuration(long timeout) { return (long) ((1.0 -
	 * TIMEOUT_SAFETY_MARGIN) * (timeout - System.currentTimeMillis())); }
	 */

	@Override
	public StateMachine getInitialStateMachine() {
		this.propNetMachine = new SamplePropNetStateMachine();
		return this.propNetMachine;
	}

	private void heuristicMetaGame() {
		this.shiftwidth = 0;
		List<Heuristic> hl = new ArrayList<Heuristic>();
		List<Double> weights = new ArrayList<Double>();

		hl.add(new GoalProximityHeuristic(getStateMachine()));
		hl.add(new HeuristicFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY));
		// hl.add(new HeuristicFreedom(getStateMachine(),
		// HeuristicFreedom.Type.FOCUS));
		hl.add(new HeuristicOpponentFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY));
		// hl.add(new HeuristicOpponentFreedom(getStateMachine(),
		// HeuristicFreedom.Type.FOCUS));

		weights.add(0.80);
		weights.add(0.10);
		// weights.add(0.10);
		weights.add(0.10);
		// weights.add(0.10);

		for (int i = 0; i < weights.size(); i++) {
			DebugLog.output("Weight " + i + " is:" + weights.get(i));
		}

		this.h = new HeuristicWeighted(hl, weights); // TODO reinstate the
														// weighted heuristic
														// after testing monte
														// carlo search
		this.h = new MCSHeuristic(getStateMachine());
		this.maxLevel = 50; // TODO Smarter here?
		this.b = new FixedBounder(this.maxLevel);
		this.reachedAllTerminal = false;

	}

	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		// this.propNetMachine.getPropNet().renderToFile("/home/vk/0.dot");

		long factorTimeout = (long) (FACTOR_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
				+ System.currentTimeMillis();
		long turnTimeout = (long) (METAGAME_TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
				+ System.currentTimeMillis();

		DebugLog.output("Timeout is "+ timeout +" will return by "+turnTimeout);
		DebugLog.output("Start Metagame");
		this.reachedAllTerminal = false;


		heuristicMetaGame();
		this.factoredMachines = new ArrayList<StateMachine>();
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
					this.factoredMachines.add(factoredMachine);
					// propNet.renderToFile("C:/Users/forku_000/Documents/courses/spring17/cs227b/graphs/output"
					// + i + ".dot");
				}

				DebugLog.output("Reduced propnet from " + this.propNetMachine.getPropNet().getComponents().size()
						+ " to " + reachablePropNet.getComponents().size());
				DebugLog.output("Factored into " + this.factoredMachines.size() + " propnets");
			} catch (PhaseTimeoutException pte) {
				this.factoredMachines = new ArrayList<StateMachine>();
				this.factoredMachines.add(this.propNetMachine);
			}
		} else {
			// SamplePropNetStateMachine singleMachine = new
			// SamplePropNetStateMachine();
			// PropNet propNet = this.propNetMachine.getPropNet();
			// singleMachine.initialize(propNet);
			this.factoredMachines.add(this.propNetMachine);
		}
		int numDepthCharges = 0;
		Role role = getRole();
		// this.propNetMachine.getPropNet().renderToFile("/home/vk/1.dot");

		DebugLog.output("Could not find node in search tree - creating new MCTS tree");


		for (StateMachine machine : this.factoredMachines) {
			MachineState initialState = machine.getInitialState();
			if(machine instanceof SamplePropNetStateMachine){
				initialState = ((SamplePropNetStateMachine)machine).getInitialStateInternal();
			}
			this.currNodes.add(new MCTSNode(machine, initialState, null, role, EXPLORATION_COEFFICIENT,
					new HashMap<MachineState, MCTSNode>(), this.h));
		}
		long depthChargeStart = System.currentTimeMillis();
		try {
			while (System.currentTimeMillis() < turnTimeout) {
				for (MCTSNode node : this.currNodes)
					node.performIteration(turnTimeout, false);
				numDepthCharges++;
			}
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			double dcps = (1000.0*(double) numDepthCharges) / (Math.max(System.currentTimeMillis() - depthChargeStart, 100.0));
			DebugLog.output("Metagame DCPS:" + dcps);
			runHeur =  dcps < DEPTH_CHARGE_PER_SECOND_HEUR_CUTOFF;
		}
	}

	private Move getIterDeepeningMove(long timeout) throws MoveDefinitionException {
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		Move bestMove = null;
		MoveContainer bestMoveResult = null;
		Move randomMove = machine.getRandomMove(state, role);
		this.reachedAllTerminal = false;
		this.shiftwidth = 0;
		try {
			// turnTimeout = (long)(timeoutSafetyMargin * (timeout -
			// System.currentTimeMillis())) + System.currentTimeMillis();
			// long proposedTimeout = timeout - 3000;
			// System.out.println("Timeout: " + timeout);
			// System.out.println("current Timeout: " + turnTimeout );
			// System.out.println("proposed Timeout: " + proposedTimeout);
			for (int iteration = 0; iteration < maxLevel && !reachedAllTerminal; iteration++) {
				this.b = new FixedBounder(iteration);
				this.reachedAllTerminal = true;
				MoveContainer candidateResult = bestMove(role, machine, state, timeout);
				if (bestMoveResult == null) {
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				} else if (!candidateResult.timedOut) {
					// iteration completed -- these estimates should be better
					// than previous
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				} else if (candidateResult.getScore() > bestMoveResult.getScore()) {
					// iteration timed out, pick based on score
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				}

			}
		} catch (Exception e) {

		} finally {
			if (bestMove == null) {
				DebugLog.output("Heur: Picking Random Move");
				bestMove = randomMove;
			}
		}
		return bestMove;
	}

	private MoveContainer bestMove(Role role, StateMachine machine, MachineState state, long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		int bestScore = 0;
		List<Move> rawMoves = machine.getLegalMoves(state, role);
		boolean timedOut = false;

		if (rawMoves.size() == 1) {
			DebugLog.output("Only one choice: picking " + rawMoves.get(0).toString());
			return new MoveContainer(rawMoves.get(0), 0, timedOut);
		}

		int alpha = 0;
		int beta = 100;
		MoveContainer bestMove = null;
		List<Move> moves = new ArrayList<Move>(rawMoves);

		Collections.shuffle(moves, new Random());

		List<Move> possibleMoves = new ArrayList<Move>();
		try {
			for (Move move : moves) {
				PhaseTimeoutException.checkTimeout(timeout);
				// List<Move> nextMoves = machine.getLegalJointMoves(state,
				// role, move).get(0);

				// DebugLog.output("Considering " + move.toString());
				shiftwidth++;
				int result = minScore(machine, move, state, alpha, beta, timeout);
				shiftwidth--;
				// DebugLog.output("Min score for " + move.toString() + " is " +
				// result);

				if (result > bestScore || possibleMoves.size() == 0) {
					possibleMoves.clear();
					possibleMoves.add(move);
					bestScore = result;
					alpha = bestScore;
				}
			}
		} catch (PhaseTimeoutException pt) {
			DebugLog.output("Heur: Timed out");
			shiftwidth = 0;
			timedOut = true;
		} finally {
			if (possibleMoves.size() == 0) {
				bestMove = new MoveContainer(machine.getRandomMove(state, role), 0, timedOut);
			} else {
				bestMove = new MoveContainer(possibleMoves.get(new Random().nextInt(possibleMoves.size())), bestScore,
						timedOut);
				// DebugLog.output("Best Move " + bestMove.getMove());
			}
			// DebugLog.output("Picking " + bestMove.getMove().toString());
		}
		return bestMove;
	}

	private int minScore(StateMachine machine, Move playerMove, MachineState state, int alpha, int beta, long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException,
			PhaseTimeoutException {

		// System.out.println("Mover is "+mover.toString());
		PhaseTimeoutException.checkTimeout(timeout);
		List<List<Move>> moves = machine.getLegalJointMoves(state, getRole(), playerMove);
		for (List<Move> move : moves) {
			// DebugLog.output("Min considering " + move.toString());
			int result = maxScore(machine, machine.getNextState(state, move), alpha, beta, timeout);
			beta = Math.min(beta, result);
			// DebugLog.output("Min considering " + move.toString()+" with score
			// " + result);
			if (beta <= alpha) {
				// DebugLog.output("Pruning ... ");
				return alpha;
			}
		}
		// DebugLog.output("Best score for min is " + beta);
		return beta;

	}

	private int maxScore(StateMachine machine, MachineState state, int alpha, int beta, long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException,
			PhaseTimeoutException {
		PhaseTimeoutException.checkTimeout(timeout);
		if (machine.isTerminal(state)) {
			int roleIdx = machine.getRoleIndices().get(getRole());
			// DebugLog.output("At terminal state. Score is: " +
			// machine.getGoals(state).get(roleIdx));
			return machine.getGoals(state).get(roleIdx);
		} else if (expFn(getRole(), state, shiftwidth)) {
			// DebugLog.output("Should not expand state -- defaulting to
			// heuristic");
			this.h.preEval(System.currentTimeMillis() + 100);
			return evalFn(getRole(), state);
		} else {
			List<Move> moves = machine.getLegalMoves(state, getRole());
			for (Move move : moves) {
				// DebugLog.output("Max considering " + move.toString());
				shiftwidth++;
				int result = minScore(machine, move, state, alpha, beta, timeout);
				shiftwidth--;
				// DebugLog.output("Max received " + result);
				alpha = Math.max(alpha, result);
				if (beta <= alpha) {
					return beta;
				}
			}
			return alpha;
		}
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {

		DebugLog.output("Start select move");
		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();

		if(machine instanceof SamplePropNetStateMachine){
			state = ((SamplePropNetStateMachine)machine).convertToInternal(state);
		}
		Role role = getRole();
		Move randomMove = machine.getRandomMove(state, role);
		Move bestMove = null;
		Move heurMove = null;
		try {
			long turnTime = timeout - System.currentTimeMillis();
			long turnTimeout = (long) (TIMEOUT_SAFETY_MARGIN * (timeout - System.currentTimeMillis()))
					+ System.currentTimeMillis();
			long mctsTimeout = (long) ((TIMEOUT_SAFETY_MARGIN - BEST_MOVE_SELECTION_MARGIN)
					* (timeout - System.currentTimeMillis())) + System.currentTimeMillis();

			long iterDeepeningTimeout = (long) ((TIMEOUT_SAFETY_MARGIN - BEST_MOVE_SELECTION_MARGIN
					- MCTS_SAFETY_MARGIN) * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			DebugLog.output("Timeout is "+ timeout +" will return by " + turnTimeout);
			//System.gc();
			//DebugLog.output("Done with gc");
			int numDepthCharges = 0;
			if (runHeur) {
				heurMove = getIterDeepeningMove(iterDeepeningTimeout);
			}

			for (int i = 0; i < this.currNodes.size(); i++) {
				MCTSNode currNode = this.currNodes.get(i);

				if (currNode != null) {
					currNode = currNode.updateState(state);
				}

				if (currNode == null) {
					DebugLog.output("Could not find node in search tree - creating new MCTS tree");
					currNode = new MCTSNode(machine, state, null, role, EXPLORATION_COEFFICIENT,
							new HashMap<MachineState, MCTSNode>(), this.h);
				}

				this.currNodes.set(i, currNode);
			}

			long depthChargeStart = System.currentTimeMillis();
			boolean allFullyExplored = false;
			try {
				while (System.currentTimeMillis() < mctsTimeout && !allFullyExplored) {
					allFullyExplored = true;
					for (MCTSNode currNode : this.currNodes) {
						//if(numDepthCharges % 500 == 0){
							//DebugLog.output("Completed "+numDepthCharges);
						//}
						currNode.performIteration(mctsTimeout, false);
						numDepthCharges++;

						if (!currNode.isFullyExplored())
							allFullyExplored = false;
					}
				}
			} catch (PhaseTimeoutException e){
				DebugLog.output("Timeout triggered during mcts");
			} finally {
				DebugLog.output("Picking best move");
				double dcps = (1000.0*(double) numDepthCharges) / (Math.max(System.currentTimeMillis() - depthChargeStart, 100.0));
				DebugLog.output("Turn DCPS:" + dcps);
				if (!allFullyExplored && dcps < DEPTH_CHARGE_PER_SECOND_HEUR_CUTOFF) {
					DebugLog.output("DCPS too low - will run iter deepening next time");
					if (heurMove != null) {
						DebugLog.output("Picking heuristic move");
						bestMove = heurMove;
					}
					runHeur = true;
				} else {
					DebugLog.output("DCPS not too low - will not run iter deepening");
					DebugLog.output("Picking MCTS move");
					// FIXME Check for noops?
					runHeur = false;
					Pair<Move, Double> bestMoveAndUtility = null;
					for (MCTSNode currNode : this.currNodes) {
						Pair<Move, Double> curMoveAndUtility = currNode.getBestMoveAndUtility(turnTimeout);
						if (bestMoveAndUtility == null || curMoveAndUtility.right > bestMoveAndUtility.right)
							bestMoveAndUtility = curMoveAndUtility;
					}
					bestMove = bestMoveAndUtility.left;
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			e.printStackTrace();
			if (bestMove == null) {
				DebugLog.output("Picking Random Move");
				bestMove = randomMove;
			}
		}
		if (bestMove == null) {
			DebugLog.output("Picking Random Move");
			bestMove = randomMove;
		}
		DebugLog.output("Picked move "+bestMove.toString());
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

	private int evalFn(Role role, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		this.reachedAllTerminal = false;
		return (int) (h.evalState(role, state) * 100);
	}

	private boolean expFn(Role role, MachineState state, int level)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return !b.shouldExpand(getRole(), state, level);
	}

}
