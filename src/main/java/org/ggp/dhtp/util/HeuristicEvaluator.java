package org.ggp.dhtp.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;

public class HeuristicEvaluator {

	private List<Heuristic> heuristics;
	private List<Double> weights;
	private long timeout;
	private Role role;
	private MachineState startState;
	private StateMachine machine;
	private final int TURN_LIMIT = 50;

	public HeuristicEvaluator(long timeout, Role role, MachineState startState, StateMachine machine) {
		this.heuristics = new ArrayList<Heuristic>();
		this.weights = new ArrayList<Double>();
		this.timeout = timeout;
		this.role = role;
		this.startState = startState;
		this.machine = machine;
	}

	public void addHeuristic(Heuristic h) {
		heuristics.add(h);
		weights.add(0.0);
	}

	private void checkTimeout() throws PhaseTimeoutException {
		if (System.currentTimeMillis() > timeout) {
			throw new PhaseTimeoutException();
		}
	}

	/***
	 * Generate weights for heuristics using simulation
	 * @return
	 */
	public List<Double> generateSimulatedWeights() {
		List<Double> m = new ArrayList<Double>();
		List<Double> v = new ArrayList<Double>();
		int numScores = 0;
		for(int i = 0; i < weights.size(); i++){
			weights.set(i, 1.0/weights.size()); // evenly weight by default
		}
		try {
			while (System.currentTimeMillis() < timeout) {
				List<MachineState> states = new ArrayList<MachineState>();
				MachineState state = startState;
				for (int i = 0; i < TURN_LIMIT && !machine.isTerminal(state); i++) {
					checkTimeout();
					states.add(state);
					state = machine.getNextState(state, machine.getRandomJointMove(state));
				}
				if (machine.isTerminal(state)) {
					checkTimeout();
					int score = machine.getGoal(state, role);
					numScores++;
					// pick a random state in states
					MachineState pastState = states.get(new Random().nextInt(states.size()));
					for (int i = 0; i < heuristics.size(); i++) {
						Heuristic h = heuristics.get(i);
						double heuristicPrediction = h.evalState(role, pastState);
						double x_k = 0;
						if(heuristicPrediction != 0.0){
							x_k = score / heuristicPrediction / 100.0 ;
						}
						//refer to https://math.stackexchange.com/questions/20593/calculate-variance-from-a-stream-of-sample-values/116344#116344
						if(numScores == 1){
							m.add(x_k);
							v.add(0.0);
						} else {
							double m_k_1 = m.get(i);
							double m_k = m_k_1 + (x_k - m_k_1)/numScores;
							double v_k_1 = v.get(i);
							double v_k = v_k_1 + (x_k - m_k_1)*(x_k - m_k);
							m.set(i, m_k);
							v.set(i, v_k);
						}
					}
				}
			}
		} catch (Exception e) {
			System.err.println(e.toString());
			e.printStackTrace(System.err);
		} finally {
			if(numScores > 1){
				List<Double> qualities  = new ArrayList<Double>();
				double qualitySum = 0.0;
				//get quality metric
				for(int i = 0; i < heuristics.size(); i++){
					double variance = v.get(i)/(numScores-1.0);
					System.err.println("Heuristic "+i+" mean:"+m.get(i));
					System.err.println("Heuristic "+i+" var:"+variance);
					//get signal to noise ratio by calculating mean/stdev. Also, heuristics should be scaled up by how much they predict score on average
					// so we have mean * snr as raw quality
					// we take a square root of raw quality to reduce extreme assignments
					double quality = variance == 0 ? 0.0 : Math.sqrt(m.get(i)*(m.get(i))/Math.sqrt(variance));
					qualities.add(quality);
					qualitySum += quality;
				}

				//set weight on quality metric, normalizing to 1.0
				for(int i = 0; i < heuristics.size(); i++){
					double weight = qualities.get(i)/qualitySum;
					weights.set(i, weight);
				}
			}

		}

		return weights;
	}
}
