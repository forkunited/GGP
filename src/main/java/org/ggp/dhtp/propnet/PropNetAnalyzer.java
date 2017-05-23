package org.ggp.dhtp.propnet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;

public class PropNetAnalyzer {

	public PropNetAnalyzer() {

	}

	/* FIXME
	 * Returns components reachable from the disjunct that are reachable
	 * without passing through the other disjuncts.  Note that this assumes
	 * that goals do not rely on conjunctions of any of the other disjuncts.  If
	 * that happens, then the propnet may not factor entirely correctly in
	 * that goal values of one subnet may depend on the state of other subnets.
	 * It also assumes that there are no goals in other disconnected parts of the
	 * network.
	 */
	protected Set<Component> getDisjunctSubnet(PropNet propNet, Component currentDisjunct, Set<Component> disjuncts) {
		Queue<Component> toVisit = new LinkedList<Component>();
		Set<Component> subnet = new HashSet<Component>();
		toVisit.add(currentDisjunct);

		//DebugLog.output(currentDisjunct.getOutputs().iterator().next().toString());
		//DebugLog.output(String.valueOf(disjuncts.contains(propNet.getInitProposition())));
		//DebugLog.output(String.valueOf(subnet.contains(propNet.getInitProposition())));

		while (!toVisit.isEmpty()) {
			Component cur = toVisit.remove();
			subnet.add(cur);

			for (Component input : cur.getInputs()) {
				if (disjuncts.contains(input) || subnet.contains(input))
					continue;
				toVisit.add(input);
			}

			for (Component output : cur.getOutputs()) {
				if (disjuncts.contains(output) || subnet.contains(output))
					continue;
				toVisit.add(output);
			}
		}

		// Is this correct to do?
		subnet.add(propNet.getInitProposition());

		return subnet;
	}

	protected Set<Component> getReachableSubnet(PropNet propNet, Set<Component> starts) {
		Queue<Component> toVisit = new LinkedList<Component>();
		Set<Component> subnet = new HashSet<Component>();
		toVisit.addAll(starts);

		while (!toVisit.isEmpty()) {
			Component cur = toVisit.remove();
			subnet.add(cur);

			for (Component input : cur.getInputs()) {
				if (subnet.contains(input))
					continue;
				toVisit.add(input);
			}

			for (Component output : cur.getOutputs()) {
				if (subnet.contains(output))
					continue;
				toVisit.add(output);
			}
		}

		// Is this correct to do?
		subnet.add(propNet.getInitProposition());

		return subnet;
	}

	protected Set<Proposition> getGoals(PropNet propNet) {
		Collection<Set<Proposition>> roleGoals = propNet.getGoalPropositions().values();
		Set<Proposition> goals = new HashSet<Proposition>();
		for (Set<Proposition> goalsForRole : roleGoals)
			goals.addAll(goalsForRole);
		return goals;
	}

	protected Set<Component> getLegals(PropNet propNet) {
		Collection<Set<Proposition>> roleLegals = propNet.getLegalPropositions().values();
		Set<Component> legals = new HashSet<Component>();
		for (Set<Proposition> legalsForRole : roleLegals)
			legals.addAll(legalsForRole);
		return legals;
	}

	public PropNet factorTerminalGoalReachable(PropNet propNet) {
		Proposition terminal = propNet.getTerminalProposition();
		Set<Proposition> goals = getGoals(propNet);
		Set<Component> termGoals = new HashSet<Component>();
		termGoals.add(terminal);
		termGoals.addAll(goals);

		Set<Component> termGoalComponents = getReachableSubnet(propNet, termGoals);
		termGoalComponents.addAll(getReachableSubnet(propNet, getLegals(propNet)));

		return propNet.clone(termGoalComponents);
	}

	protected boolean disjunctHasInputs(PropNet propNet, Component disjunct) {
		if (disjunct.isInput())
			return true;

		Queue<Component> toVisit = new LinkedList<Component>();
		Set<Component> subnet = new HashSet<Component>();
		subnet.add(disjunct);
		toVisit.addAll(disjunct.getInputs());

		while (!toVisit.isEmpty()) {
			Component cur = toVisit.remove();
			subnet.add(cur);

			if (cur.isInput())
				return true;

			for (Component input : cur.getInputs()) {
				if (subnet.contains(input))
					continue;
				toVisit.add(input);
			}

			for (Component output : cur.getOutputs()) {
				if (subnet.contains(output))
					continue;
				toVisit.add(output);
			}
		}

		return false;
	}

	public List<PropNet> factorDisjunctive(PropNet propNet) {
		List<PropNet> propNets = new ArrayList<PropNet>();
		if (!(propNet.getTerminalProposition().getSingleInput() instanceof Or)) {
			propNets.add(propNet);
			return propNets;
		}

		Set<Component> legalSubnet = getReachableSubnet(propNet, getLegals(propNet));
		Set<Component> disjuncts = new HashSet<Component>(propNet.getTerminalProposition().getSingleInput().getInputs());
		Set<Component> toRemove = new HashSet<Component>();
		for (Component disjunct : disjuncts)
			if (!disjunctHasInputs(propNet, disjunct))
				toRemove.add(disjunct);
		disjuncts.removeAll(toRemove);

		for (Component disjunct : disjuncts) {
			Set<Component> subnet = getDisjunctSubnet(propNet, disjunct, disjuncts);
			subnet.addAll(legalSubnet);
			propNets.add(propNet.clone(subnet));
		}

		return propNets;
	}
}
