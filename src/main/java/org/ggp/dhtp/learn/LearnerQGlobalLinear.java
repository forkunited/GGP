package org.ggp.dhtp.learn;

import java.util.ArrayList;
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

public class LearnerQGlobalLinear {
	private static final long LEARNING_BUFFER_TIME = 1000;

	public enum ExplorationStrategy {
		NONE,
		EPSILON_GREEDY
	}

	private Role role;
	private StateMachine machine;
	private SAFeatureSet features;
	private double alpha; // Learning rate
	private double epsilon; // epsilon-greedy exploration parameter
	private List<Double> weights;
	private Random random;

	public LearnerQGlobalLinear(Role role, StateMachine machine, SAFeatureSet features, double alpha, double epsilon) {
		this.role = role;
		this.machine = machine;
		this.features = features;
		this.alpha = alpha;
		this.epsilon = epsilon;

		this.weights = new ArrayList<Double>(Collections.nCopies(this.features.size(), 0.0));
		this.random = new Random();
	}

	// FIXME Vectorize with linear algebra library
	protected List<List<Double>> computeFeatures(MachineState state, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Double>> F = new ArrayList<List<Double>>();
		List<Move> moves = this.machine.getLegalMoves(state, this.role);

		long timeLeft = timeoutDuration;
		long endTime = System.currentTimeMillis() + timeoutDuration;

		for (int i = 0; i < moves.size(); i++) {
			long moveDuration = timeLeft/(moves.size() - i);
			F.add(this.features.compute(this.machine, this.role, state, moves.get(i), moveDuration));
			timeLeft = endTime - System.currentTimeMillis();
		}

		return F;
	}

	public void learn(int iterations, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		MachineState curState = this.machine.getInitialState();
		List<List<Double>> curF = null;
		long timeLeft = timeoutDuration;
		long endTime = System.currentTimeMillis() + timeoutDuration - LEARNING_BUFFER_TIME;

		int i = 0;
		while (System.currentTimeMillis() < endTime && i < iterations) {
			if (curF == null) {
				long iterationDuration = timeLeft / (iterations - i + 1);
				curF = computeFeatures(curState, iterationDuration);
			}

			int learnerMoveIndex = actIndex(curState, ExplorationStrategy.EPSILON_GREEDY, curF);
			Move learnerMove = this.machine.getLegalMoves(curState, this.role).get(learnerMoveIndex);
			List<Move> moves = this.machine.getRandomJointMove(curState, this.role, learnerMove);
			MachineState nextState = this.machine.getNextState(curState, moves);

			timeLeft = endTime - System.currentTimeMillis();
			long iterationDuration = timeLeft / (iterations - i);
			List<List<Double>> nextF = computeFeatures(nextState, iterationDuration);

			updateF(curState, learnerMoveIndex, nextState, curF, nextF);

			if (this.machine.isTerminal(nextState)) {
				curState = this.machine.getInitialState();
				curF = null;
			} else {
				curState = nextState;
				curF = nextF;
			}

			timeLeft = endTime - System.currentTimeMillis();
			i++;
		}
	}

	public Move act(MachineState state, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		return act(state, ExplorationStrategy.NONE, timeoutDuration);
	}

	public Move act(MachineState state, ExplorationStrategy strategy, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Double>> F = computeFeatures(state, timeoutDuration);
		return this.machine.getLegalMoves(state, this.role).get(actIndex(state, strategy, F));
	}

	public int actIndex(MachineState state, ExplorationStrategy strategy, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Double>> F = computeFeatures(state, timeoutDuration);
		return actIndex(state, strategy, F);
	}

	protected int actIndex(MachineState state, ExplorationStrategy strategy, List<List<Double>> F) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (strategy == ExplorationStrategy.EPSILON_GREEDY) {
			if (this.random.nextDouble() < this.epsilon) {
				// Choose random action
				List<Move> moves = this.machine.getLegalMoves(state, this.role);
				return this.random.nextInt(moves.size());
			} else {
				// Choose best action
				return actIndex(state, ExplorationStrategy.NONE, F);
			}
		} else {
			List<Move> moves = this.machine.getLegalMoves(state, this.role);
			double bestQ = Double.NEGATIVE_INFINITY;
			int bestMoveIndex = 0;
			for (int i = 0; i < moves.size(); i++) {
				double curQ = Qf(state, F.get(i));
				if (curQ > bestQ) {
					bestQ = curQ;
					bestMoveIndex = i;
				}
			}

			return bestMoveIndex;
		}
	}

	public double U(MachineState state, long timeoutDuration) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		List<List<Double>> F = this.computeFeatures(state, timeoutDuration);
		return UF(state, F);
	}

	protected double UF(MachineState state, List<List<Double>> F) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double Q_max = Double.NEGATIVE_INFINITY;

		for (int i = 0; i < F.size(); i++) {
			Q_max = Math.max(Qf(state, F.get(i)), Q_max);
		}

		return Q_max;
	}

	public double Q(MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Double> f = this.features.compute(this.machine, this.role, state, move, timeoutDuration);
		return Qf(state, f);
	}

	// FIXME To make fast, do this using some linear algebra library
	protected double Qf(MachineState state, List<Double> f) {
		double Q = 0.0;
		for (int i = 0; i < f.size(); i++) {
			Q += f.get(i) * this.weights.get(i);
		}
		return Q;
	}

	// FIXME To make fast, do this using some linear algebra library
	protected void updateF(MachineState state, int moveIndex, MachineState nextState, List<List<Double>> curF, List<List<Double>> nextF) throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		double reward = 0.0;
		if (this.machine.isTerminal(nextState))
			reward = this.machine.getGoal(nextState, this.role);
		double tdError = reward + UF(nextState, nextF) - Qf(state, curF.get(moveIndex));
		List<Double> f = curF.get(moveIndex);
		for (int i = 0; i < f.size(); i++)
			this.weights.set(i, this.weights.get(i) + this.alpha * tdError * f.get(i));
	}
}
