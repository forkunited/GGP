package org.ggp.dhtp.learn;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class SAFeatureSet {
	private List<SAFeature> features;
	private int size;

	public SAFeatureSet(List<SAFeature> features) {
		this.features = features;
		this.size = 0;
		for (SAFeature feature : this.features) {
			this.size += feature.size();
		}
	}

	public int getFeatureTypeCount() {
		return this.features.size();
	}

	public int size() {
		return this.size;
	}

	public List<Double> compute(StateMachine machine, Role role, MachineState state, Move move, long timeoutDuration) throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		List<Double> vector = new ArrayList<>();
		long timeLeft = timeoutDuration;
		long endTime = System.currentTimeMillis() + timeoutDuration;

		for (int i = 0; i < this.features.size(); i++) {
			long featureDuration = timeLeft/(this.features.size() - i);
			SAFeature feature = this.features.get(i);
			if (feature.size() == 1)
				vector.add(feature.computeFirst(machine, role, state, move, featureDuration));
			else
				vector.addAll(feature.compute(machine, role, state, move, featureDuration));
			timeLeft = endTime - System.currentTimeMillis();
		}

		return vector;
	}
}
