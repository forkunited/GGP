package org.ggp.dhtp.propnet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlSentence;
import org.ggp.base.util.propnet.architecture.Component;
import org.ggp.base.util.propnet.architecture.PropNet;
import org.ggp.base.util.propnet.architecture.components.Or;
import org.ggp.base.util.propnet.architecture.components.Proposition;
import org.ggp.base.util.statemachine.Role;
import org.ggp.dhtp.util.DebugLog;
import org.ggp.dhtp.util.PhaseTimeoutException;

public class PropNetAnalyzer {

	public PropNetAnalyzer() {

	}

	/*
	 * FIXME Returns components reachable from the disjunct that are reachable
	 * without passing through the other disjuncts. Note that this assumes that
	 * goals do not rely on conjunctions of any of the other disjuncts. If that
	 * happens, then the propnet may not factor entirely correctly in that goal
	 * values of one subnet may depend on the state of other subnets. It also
	 * assumes that there are no goals in other disconnected parts of the
	 * network.
	 */
	protected Set<Component> getDisjunctSubnet(PropNet propNet, Component currentDisjunct, Set<Component> disjuncts) {
		Queue<Component> toVisit = new LinkedList<Component>();
		Set<Component> subnet = new HashSet<Component>();
		toVisit.add(currentDisjunct);

		// DebugLog.output(currentDisjunct.getOutputs().iterator().next().toString());
		// DebugLog.output(String.valueOf(disjuncts.contains(propNet.getInitProposition())));
		// DebugLog.output(String.valueOf(subnet.contains(propNet.getInitProposition())));

		while (!toVisit.isEmpty()) {
			Component cur = toVisit.remove();
			subnet.add(cur);

			for (Component input : cur.getInputs()) {
				if (disjuncts.contains(input) || subnet.contains(input))
					continue;
				toVisit.add(input);
			}
			/*
			 * if(!currentDisjunct.equals(cur)){ for (Component output :
			 * cur.getOutputs()) { if (disjuncts.contains(output) ||
			 * subnet.contains(output)) continue; toVisit.add(output); } }
			 */
		}

		// Is this correct to do?
		subnet.add(propNet.getInitProposition());

		return subnet;
	}

	protected Set<Component> getReachableSubnet(PropNet propNet, Set<Component> starts, long factorTimeout)
			throws PhaseTimeoutException {
		Queue<Component> toVisit = new LinkedList<Component>();
		Set<Component> subnet = new HashSet<Component>();
		toVisit.addAll(starts);

		while (!toVisit.isEmpty()) {
			PhaseTimeoutException.checkTimeout(factorTimeout);
			Component cur = toVisit.remove();
			subnet.add(cur);

			for (Component input : cur.getInputs()) {
				if (subnet.contains(input))
					continue;
				toVisit.add(input);
			}
			/*
			for (Component output : cur.getOutputs()) {
				if (subnet.contains(output))
					continue;
				toVisit.add(output);
			}*/
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

	protected Set<Component> getLegals(PropNet propNet, Set<Proposition> reachableInputComponents) {
		Collection<Set<Proposition>> roleLegals = propNet.getLegalPropositions().values();
		Set<Component> legals = new HashSet<Component>();
		for (Set<Proposition> legalsForRole : roleLegals) {
			for (Proposition legalForRole : legalsForRole) {
				for (Proposition c : reachableInputComponents) {
					if (c.getName().getBody().equals(legalForRole.getName().getBody())) {
						legals.add(legalForRole);
					}
				}
			}
		}
		DebugLog.output("New legals size:"+legals.size());
		return legals;
	}

	public PropNet factorTerminalGoalReachable(PropNet propNet, long factorTimeout) throws PhaseTimeoutException {
		Proposition terminal = propNet.getTerminalProposition();
		Set<Proposition> goals = getGoals(propNet);
		Set<Component> termGoals = new HashSet<Component>();
		termGoals.add(terminal);
		termGoals.addAll(goals);

		Set<Component> termGoalComponents = getReachableSubnet(propNet, termGoals, factorTimeout);
		Set<Proposition> reachableInputComponents = new HashSet<Proposition>();
		for (Component c : propNet.getInputComponentSet()) {
			Proposition p = (Proposition) c;
			if (termGoalComponents.contains(p)) {
				reachableInputComponents.add(p);
			}
		}
		termGoalComponents
				.addAll(getReachableSubnet(propNet, getLegals(propNet, reachableInputComponents), factorTimeout));

		int num = 0;
		for(Component c : termGoalComponents){
			if(c instanceof Proposition){
				Proposition p = (Proposition)c;
				if(p.getName().getName().getValue().equals("legal")){
					num++;
				}
			}
		}
		DebugLog.output("Num legal props in net:"+num);

		PropNet newNet = propNet.clone(termGoalComponents, new HashSet<Component>());
		DebugLog.output("old net legal props: "+ propNet.getLegalInputMap().size());
		DebugLog.output("New net legal props: "+ newNet.getLegalInputMap().size());


		return newNet;
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

	public boolean shouldConsiderDisjunctive(PropNet propNet, Role role, long factorTimeout)
			throws PhaseTimeoutException {
		Map<GdlSentence, Proposition> map = propNet.getBasePropositions();

		List<Role> roles = Role.computeRoles(new ArrayList<GdlSentence>(propNet.getBasePropositions().keySet()));
		for (Role nrole : roles) {
			if (nrole.getClass().equals(role.getName())) {
				return true;
			}
		}
		return false;
	}

	public List<PropNet> factorDisjunctive(PropNet propNet, long factorTimeout) throws PhaseTimeoutException {
		List<PropNet> propNets = new ArrayList<PropNet>();
		if (!(propNet.getTerminalProposition().getSingleInput() instanceof Or)) {
			propNets.add(propNet);
			return propNets;
		}

		Set<Proposition> reachableInputComponents = new HashSet<Proposition>();
		for (Component c : propNet.getInputComponentSet()) {
			Proposition p = (Proposition) c;
			reachableInputComponents.add(p);
		}

		Set<Component> legalSubnet = getReachableSubnet(propNet, getLegals(propNet, reachableInputComponents), factorTimeout);
		Set<Component> disjuncts = new HashSet<Component>(
				propNet.getTerminalProposition().getSingleInput().getInputs());
		Set<Component> toRemove = new HashSet<Component>();
		for (Component disjunct : disjuncts) {
			PhaseTimeoutException.checkTimeout(factorTimeout);
			if (!disjunctHasInputs(propNet, disjunct))
				toRemove.add(disjunct);
		}
		disjuncts.removeAll(toRemove);
		DebugLog.output("Total props:" + propNet.getPropositions().size() + " props");

		for (Component disjunct : disjuncts) {
			PhaseTimeoutException.checkTimeout(factorTimeout);
			Set<Component> mustAdd = new HashSet<Component>();
			mustAdd.add(generateNewTerminalProposition(disjunct));
			Set<Component> subnet = getDisjunctSubnet(propNet, disjunct, disjuncts);
			PropNet roleNet = propNet.clone(subnet, new HashSet<Component>());
			List<Proposition> baseProps = new ArrayList<Proposition>(roleNet.getPropositions());
			List<GdlSentence> sentences = new ArrayList<GdlSentence>();
			DebugLog.output("Disjunct has " + baseProps.size() + " props");

			for (Proposition p : baseProps) {
				sentences.add(p.getName());
			}
			List<Role> roles = Role.computeRoles(sentences);
			DebugLog.output("Disjunct has " + roles.size() + "roles");
			subnet.addAll(legalSubnet);
			propNets.add(propNet.clone(subnet, mustAdd));
			DebugLog.output("Terminal prop is " + propNets.get(propNets.size() - 1).getTerminalProposition());
		}

		return propNets;
	}

	private Proposition generateNewTerminalProposition(Component currentDisjunct) {

		currentDisjunct.removeAllOutputs();
		Proposition p = new Proposition(new GdlProposition(new GdlConstant("terminal")));
		currentDisjunct.addOutput(p);
		p.addInput(currentDisjunct);
		return p;
	}
}
