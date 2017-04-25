package org.ggp.dhtp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

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
import org.ggp.dhtp.util.Bounder;
import org.ggp.dhtp.util.FixedBounder;
import org.ggp.dhtp.util.GoalProximityHeuristic;
import org.ggp.dhtp.util.Heuristic;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class BoundedDepthPlayer extends StateMachineGamer {

	Player p;
	Heuristic h;
	Bounder b;
	int shiftwidth =0;
	int turn =0;
	int maxLevel;
	boolean reachedAllTerminal;
	boolean DEBUG = true;
	long turnTimeout = 0;
	double timeoutSafetyMargin = 0.75;

	private void print_debug(String message) {
		if (!DEBUG || turn < -1){
			return;
		}
		System.err.print(turn);
		for (int i=0; i<= shiftwidth; i++) {
			System.err.print("  ");
		}
		System.err.println(message);
	}

	private int evalFn(Role role, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		this.reachedAllTerminal= false;
		return (int)(h.evalState(role, state) * 100);
	}

	private boolean expFn(Role role, MachineState state, int level) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		return !b.shouldExpand(getRole(), state, level);
	}

	private void checkTimeout() throws PhaseTimeoutException{
		if(System.currentTimeMillis() > turnTimeout){
			print_debug("Timed out!");
			throw new PhaseTimeoutException();
		}
	}


	@Override
	public StateMachine getInitialStateMachine() {
		// TODO Auto-generated method stub
		return new CachedStateMachine(new ProverStateMachine());
	}


	@Override
	public void stateMachineMetaGame(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
			this.turn = 0;
			this.shiftwidth = 0;
			this.h = new GoalProximityHeuristic(getStateMachine());
			this.maxLevel = 50;  //TODO Smarter here?
			this.b = new FixedBounder(this.maxLevel);
			this.reachedAllTerminal = false;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		turn++;

		StateMachine machine = getStateMachine();
		MachineState state = getCurrentState();
		Role role = getRole();
		Move bestMove = null;
		Move randomMove = machine.getRandomMove(state, role);
		this.reachedAllTerminal = false;
		try {
			turnTimeout = (long)(timeoutSafetyMargin * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();

			for(int iteration = 0; iteration < maxLevel && !reachedAllTerminal ; iteration++){
				this.b = new FixedBounder(iteration);
				this.reachedAllTerminal = true;
				bestMove = bestMove(role, machine, state);

			}
		} catch (Exception e){

		} finally {
			if(bestMove == null){
				print_debug("Picking Random Move");
				bestMove = randomMove;
			}
		}
		return bestMove;
	}


	private Move bestMove(Role role, StateMachine machine, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		int bestScore = 0;
		List<Move> rawMoves = machine.getLegalMoves(state, role);


		if(rawMoves.size() == 1){
			print_debug("Only one choice: picking " + rawMoves.get(0).toString());
			return rawMoves.get(0);
		}

		int alpha = 0;
		int beta = 100;
		Move bestMove = null;
		List<Move> moves = new ArrayList<Move>(rawMoves);

		Collections.shuffle(moves, new Random());

		List<Move> possibleMoves = new ArrayList<Move>();
		try {
			for (Move move : moves) {
				checkTimeout();
				// List<Move> nextMoves = machine.getLegalJointMoves(state,
				// role, move).get(0);

				print_debug("Considering " + move.toString());
				shiftwidth++;
				int result = minScore(machine, move, state, alpha, beta);
				shiftwidth--;
				print_debug("Min score for " + move.toString() + " is " + result);

				if (result > bestScore || possibleMoves.size() == 0) {
					possibleMoves.clear();
					possibleMoves.add(move);
					bestScore = result;
					alpha = bestScore;
				}
			}
		} catch (PhaseTimeoutException pt) {
			print_debug("Timed out");
			shiftwidth = 0;
		} finally {
			if(possibleMoves.size() == 0){
				bestMove = machine.getRandomMove(state, role);
			}else {
				bestMove = possibleMoves.get(new Random().nextInt(possibleMoves.size()));
				print_debug("Best Move " + bestMove);
			}
			print_debug("Picking " + bestMove.toString());
		}
		return bestMove;
	}


	private int minScore(StateMachine machine, Move playerMove, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException{

		// System.out.println("Mover is "+mover.toString());
		checkTimeout();
		List<List<Move>> moves = machine.getLegalJointMoves(state, getRole(), playerMove);
		for (List<Move> move : moves) {
			print_debug("Min considering " + move.toString());
			int result = maxScore(machine, machine.getNextState(state, move), alpha, beta);
			beta = Math.min(beta, result);
			print_debug("Min considering " + move.toString()+" with score " + result);
			if (beta <= alpha) {
				print_debug("Pruning ... ");
				return alpha;
			}
		}
		print_debug("Best score for min is " + beta);
		return beta;

	}

	private int maxScore(StateMachine machine, MachineState state, int alpha, int beta) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException, PhaseTimeoutException{
		checkTimeout();
		if(machine.isTerminal(state)){
			int roleIdx = machine.getRoleIndices().get(getRole());
			print_debug("At terminal state. Score is: " + machine.getGoals(state).get(roleIdx));
			return machine.getGoals(state).get(roleIdx);
		}
		else if(expFn(getRole(), state, shiftwidth)){
			print_debug("Should not expand state -- defaulting to heuristic");
			return evalFn(getRole(), state);
		}
		else {
			List<Move> moves = machine.getLegalMoves(state, getRole());
			for (Move move : moves) {
				print_debug("Max considering " + move.toString());
				shiftwidth++;
				int result = minScore(machine, move, state, alpha, beta);
				shiftwidth--;
				print_debug("Max received " + result);
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
		return "Don't hate the bounded depth player";
	}

}
