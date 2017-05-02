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
import org.ggp.dhtp.util.HeuristicEvaluator;
import org.ggp.dhtp.util.HeuristicFreedom;
import org.ggp.dhtp.util.HeuristicOpponentFreedom;
import org.ggp.dhtp.util.HeuristicWeighted;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class BoundedDepthPlayer extends StateMachineGamer {
	class MoveContainer{
		private Move move;
		private int score;
		boolean timedOut;
		public MoveContainer(Move move, int score, boolean timedOut){
			this.move = move;
			this.score = score;
			this.timedOut = timedOut;
		}
		public boolean getTimedOut(){
			return timedOut;
		}
		public Move getMove() {
			return move;
		}
		public int getScore() {
			return score;
		}
	}

	Player p;
	Heuristic h;
	Bounder b;
	int shiftwidth =0;
	int turn =0;
	int maxLevel;
	boolean reachedAllTerminal;
	boolean DEBUG = false;
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
			long metagameTimeout = (long)(timeoutSafetyMargin * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();
			List<Heuristic> hl = new ArrayList<Heuristic>();
			List<Double> weights = new ArrayList<Double>();

			hl.add(new GoalProximityHeuristic(getStateMachine()));
			hl.add(new HeuristicFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY));
			hl.add(new HeuristicFreedom(getStateMachine(), HeuristicFreedom.Type.FOCUS));
			hl.add(new HeuristicOpponentFreedom(getStateMachine(), HeuristicFreedom.Type.MOBILITY));
			hl.add(new HeuristicOpponentFreedom(getStateMachine(), HeuristicFreedom.Type.FOCUS));


			try{
				HeuristicEvaluator he = new HeuristicEvaluator(metagameTimeout, getRole(), getStateMachine().getInitialState(), getStateMachine());
				for(Heuristic h : hl){
					he.addHeuristic(h);
				}
				weights = he.generateSimulatedWeights();

			} catch (Exception e){
				System.err.println(e);
				weights.add(0.20);
				weights.add(0.20);
				weights.add(0.20);
				weights.add(0.20);
				weights.add(0.20);
			}
			DEBUG = true;
			for(int i = 0; i < weights.size(); i++){
				print_debug("Weight " + i+" is:"+weights.get(i));
			}
			DEBUG = false;

			this.h = new HeuristicWeighted(hl, weights);
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
		MoveContainer bestMoveResult = null;
		Move randomMove = machine.getRandomMove(state, role);
		this.reachedAllTerminal = false;
		try {
			turnTimeout = (long)(timeoutSafetyMargin * (timeout - System.currentTimeMillis())) + System.currentTimeMillis();

			for(int iteration = 0; iteration < maxLevel && !reachedAllTerminal ; iteration++){
				this.b = new FixedBounder(iteration);
				this.reachedAllTerminal = true;
				MoveContainer candidateResult =  bestMove(role, machine, state);
				if(bestMoveResult == null){
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				} else if(!candidateResult.timedOut){
					//iteration completed -- these estimates should be better than previous
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				} else if (candidateResult.getScore() > bestMoveResult.getScore()){
					//iteration timed out, pick based on score
					bestMove = candidateResult.getMove();
					bestMoveResult = candidateResult;
				}

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


	private MoveContainer bestMove(Role role, StateMachine machine, MachineState state) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException{
		int bestScore = 0;
		List<Move> rawMoves = machine.getLegalMoves(state, role);
		boolean timedOut = false;

		if(rawMoves.size() == 1){
			print_debug("Only one choice: picking " + rawMoves.get(0).toString());
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
			timedOut = true;
		} finally {
			if(possibleMoves.size() == 0){
				bestMove = new MoveContainer(machine.getRandomMove(state, role), 0, timedOut);
			}else {
				bestMove = new MoveContainer(possibleMoves.get(new Random().nextInt(possibleMoves.size())),bestScore, timedOut);
				print_debug("Best Move " + bestMove.getMove());
			}
			print_debug("Picking " + bestMove.getMove().toString());
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
