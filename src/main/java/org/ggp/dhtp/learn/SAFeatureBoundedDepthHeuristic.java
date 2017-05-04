package org.ggp.dhtp.learn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;
import org.ggp.dhtp.util.Bounder;
import org.ggp.dhtp.util.FixedBounder;
import org.ggp.dhtp.util.Heuristic;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class SAFeatureBoundedDepthHeuristic extends SAFeature {
	private Bounder bounder;
	private int shiftwidth = 0;
	private int maxLevel;
	private boolean reachedAllTerminal;
	private Heuristic heuristic;

	@Override
	protected boolean printDebug(String message) {
		if (!SAFeature.DEBUG)
			return false;

		for (int i=0; i<= this.shiftwidth; i++) {
			System.err.print("  ");
		}

		return super.printDebug(message);
	}

	private double evalFn(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return this.heuristic.evalState(role, state);
	}

	private boolean expFn(Role role, StateMachine machine, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (machine.isTerminal(state)) {
			return false;
		} else {
			this.reachedAllTerminal = false;
			return !this.bounder.shouldExpand(role, state, level);
		}
	}

	public SAFeatureBoundedDepthHeuristic(Heuristic heuristic, int maxLevel) {
		this.heuristic = heuristic;
		this.maxLevel = maxLevel;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public List<Double> compute(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return Arrays.asList(computeFirst(machine, role, state, move, timeoutDuration));
	}

	@Override
	public Double computeFirst(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		setTimeoutDuration(timeoutDuration);

		if (machine.isTerminal(state))
			return this.heuristic.evalState(role, state);
		double worstResult = 1.0;
		for(int iteration = 0; iteration < this.maxLevel && !this.reachedAllTerminal ; iteration++){
			this.bounder = new FixedBounder(iteration);
			this.reachedAllTerminal = true;
			double iterResult = worstMoveValue(role, machine, state, move);
			if (iterResult < worstResult)
				worstResult = iterResult;
		}

		return worstResult;
	}


	private double worstMoveValue(Role role, StateMachine machine, MachineState state, Move firstMove) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		double worstScore = 1.0;
		double alpha = 0.0;
		double beta = 1.0;

		List<List<Move>> moves = new ArrayList<List<Move>>(machine.getLegalJointMoves(state, role, firstMove));
		Collections.shuffle(moves, new Random());

		List<List<Move>> possibleMoves = new ArrayList<List<Move>>();
		try {
			for (List<Move> move : moves) {
				checkTimeout();

				//printDebug("Considering " + move.toString());
				this.shiftwidth++;
				double result = maxScore(role, machine, machine.getNextState(state, move), alpha, beta);
				this.shiftwidth--;
				//printDebug("Max score for " + move.toString() + " is " + result);

				if (result < worstScore || possibleMoves.size() == 0) {
					possibleMoves.clear();
					possibleMoves.add(move);
					worstScore = result;
					beta = worstScore;
				}
			}
		} catch (PhaseTimeoutException pt) {
			printDebug("Timed out");
			this.shiftwidth = 0;
		}

		return worstScore;
	}


	private double minScore(Role role, StateMachine machine, MachineState state, Move playerMove, double alpha, double beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException{
		checkTimeout();
		List<List<Move>> moves = machine.getLegalJointMoves(state, role, playerMove);
		for (List<Move> move : moves) {
			//printDebug("Min considering " + move.toString());
			double result = maxScore(role, machine, machine.getNextState(state, move), alpha, beta);
			beta = Math.min(beta, result);
			//printDebug("Min considering " + move.toString()+" with score " + result);
			if (beta <= alpha) {
				printDebug("Pruning ... ");
				return alpha;
			}
		}
		printDebug("Best score for min is " + beta);
		return beta;

	}

	private double maxScore(Role role, StateMachine machine, MachineState state, double alpha, double beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException{
		checkTimeout();
		if(expFn(role, machine, state, this.shiftwidth)) {
			printDebug("Should not expand state -- defaulting to heuristic");
			return evalFn(role, state);
		} else {
			List<Move> moves = machine.getLegalMoves(state, role);
			for (Move move : moves) {
				printDebug("Max considering " + move.toString());
				this.shiftwidth++;
				double result = minScore(role, machine, state, move, alpha, beta);
				this.shiftwidth--;
				printDebug("Max received " + result);
				alpha = Math.max(alpha, result);
				if(beta <= alpha) {
					return beta;
				}
			}
			return alpha;
		}
	}
}
