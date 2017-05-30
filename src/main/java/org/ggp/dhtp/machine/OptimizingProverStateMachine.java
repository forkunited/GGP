package org.ggp.dhtp.machine;

import java.util.ArrayList;
import java.util.List;

import org.ggp.base.util.gdl.grammar.Gdl;
import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlDistinct;
import org.ggp.base.util.gdl.grammar.GdlFunction;
import org.ggp.base.util.gdl.grammar.GdlLiteral;
import org.ggp.base.util.gdl.grammar.GdlNot;
import org.ggp.base.util.gdl.grammar.GdlOr;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.gdl.grammar.GdlRule;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.gdl.grammar.GdlVariable;
import org.ggp.base.util.statemachine.implementation.prover.ProverStateMachine;

import com.google.common.collect.ImmutableList;


public class OptimizingProverStateMachine extends ProverStateMachine
{

    /**
     * Initialize must be called before using the StateMachine
     */
    public OptimizingProverStateMachine()
    {

    }

    @Override
    public void initialize(List<Gdl> description)
    {
    	description = reorderRules(description);
    	super.initialize(description);
    }

    private List<Gdl> reorderRules(List<Gdl> description){
    	List<Gdl> newList = new ArrayList<Gdl>();
    	for(Gdl g : description){
    		if(g instanceof GdlRule){
    			GdlRule gdr = (GdlRule)g;
    			gdr = pruneSubgoals(gdr);
    			gdr = reorderRule(gdr);
    			newList.add(gdr);
    		} else {
    			newList.add(g);
    		}

    	}

    	return newList;
    }

	private GdlRule reorderRule(GdlRule g) {
		//System.out.println(">>RULE<<");
		//System.out.println(g.getHead().toString());
		List<GdlVariable> boundVars = new ArrayList<GdlVariable>();
		List<GdlLiteral> literalsRemaining = new ArrayList<GdlLiteral>(g.getBody());
		List<GdlLiteral> newOrder = new ArrayList<GdlLiteral>();
		while(!literalsRemaining.isEmpty()){
			List<GdlVariable> newVars = new ArrayList<GdlVariable>();
			GdlLiteral nextToAdd = getBestForReordering(literalsRemaining, boundVars, newVars);
			literalsRemaining.remove(nextToAdd);
			newOrder.add(nextToAdd);
			//System.out.println("Added subgoal"+nextToAdd.toString());
			//System.out.println("It has "+newVars.size() + " new variables");
			boundVars.addAll(newVars);
		}

		GdlRule gr = new GdlRule(g.getHead(), ImmutableList.copyOf(newOrder));

		return gr;
	}

	private GdlLiteral getBestForReordering(List<GdlLiteral > gll, List<GdlVariable> boundVars, List<GdlVariable> newVars){

		int bestUnbound = Integer.MAX_VALUE;
		GdlLiteral toAdd = null;
		for(GdlLiteral gl : gll){

			List<GdlVariable> foundVars = new ArrayList<GdlVariable>();
			unboundVars(gl, foundVars, boundVars);
			int numUnboundVars = foundVars.size();
			if(numUnboundVars < bestUnbound || toAdd == null){
				bestUnbound = numUnboundVars;
				toAdd = gl;
				newVars.clear();
				newVars.addAll(foundVars);
			}
		}
		return toAdd;
	}

	private void unboundVars(GdlLiteral gl, List<GdlVariable> foundVars, List<GdlVariable> boundVars){
		if(gl instanceof GdlDistinct){
			unboundVars(((GdlDistinct)gl).getArg1(), foundVars, boundVars);
			unboundVars(((GdlDistinct)gl).getArg2(), foundVars, boundVars);
		} else if (gl instanceof GdlNot){
			unboundVars(((GdlNot)gl).getBody(), foundVars, boundVars);
		} else if (gl instanceof GdlOr){
			for (GdlLiteral gll : ((GdlOr)gl).getDisjuncts()){
				unboundVars(gll, foundVars, boundVars);
			}
		} else if (gl instanceof GdlProposition){
			for(GdlTerm gt : ((GdlProposition) gl).getBody()){
				unboundVars(gt, foundVars, boundVars);
			}
		} else if (gl instanceof GdlRelation){
			for(GdlTerm gt : ((GdlRelation) gl).getBody()){
				unboundVars(gt, foundVars, boundVars);
			}
		}
	}

	private void unboundVars(GdlTerm gt, List<GdlVariable> foundVars, List<GdlVariable> boundVars){
		if(gt instanceof GdlVariable){
			if(!boundVars.contains(gt) && !foundVars.contains(gt)){
				foundVars.add((GdlVariable) gt);
			}
		} else if (gt instanceof GdlConstant){
		} else if (gt instanceof GdlFunction){
			for(GdlTerm gtt : ((GdlFunction)gt).getBody()){
				unboundVars(gtt, foundVars, boundVars);
			}
		}
	}

	private List<GdlVariable> getVars(GdlLiteral gs){
		List<GdlVariable> boundVars = new ArrayList<GdlVariable>();
		List<GdlVariable> foundVars = new ArrayList<GdlVariable>();
		unboundVars(gs, foundVars, boundVars);
		return boundVars;
	}

	private void varsexp(GdlLiteral gl, List<GdlVariable> vars){
		List<GdlVariable> newVars = getVars(gl);
		for(GdlVariable gv : newVars){
			if(!vars.contains(gv)){
				vars.add(gv);
			}
		}
	}

	private GdlRule pruneSubgoals(GdlRule g){
		System.out.println(">>RULE<<");
		System.out.println(g.getHead().toString());
		List<GdlVariable> vars = getVars(g.getHead());
		List<GdlLiteral> newLits = new ArrayList<GdlLiteral>();
		List<GdlLiteral> remainingLits = new ArrayList<GdlLiteral>(g.getBody());
		while(!remainingLits.isEmpty()){
			GdlLiteral gl = remainingLits.remove(0);
			List<GdlLiteral> synthRule = new ArrayList<GdlLiteral>(newLits);
			synthRule.addAll(remainingLits);
			if(!pruneWorthy(synthRule, gl, vars)){
				newLits.add(gl);
			} else {
				System.out.println("Pruning "+gl.toString());
			}
		}

		GdlRule gr = new GdlRule(g.getHead(), ImmutableList.copyOf(newLits));

		return gr;
	}

	private boolean pruneWorthy(List<GdlLiteral> synthRule, GdlLiteral gl, List<GdlVariable> vars) {
		List<GdlVariable> boundVars = new ArrayList<GdlVariable>(vars);
		for(GdlLiteral gll : synthRule){
			varsexp(gll, boundVars);
		}
		return compfindp(gl, synthRule, vars);
	}

	private boolean compfindp(GdlLiteral goal, List<GdlLiteral> facts, List<GdlVariable> gv){
		if(goal instanceof GdlNot){
			return compfindp(((GdlNot)goal).getBody(), facts, gv);
		} else if (goal instanceof GdlOr){
			for(GdlLiteral gl : ((GdlOr)goal).getDisjuncts()){
				if(!compfindp(gl, facts,gv)){
					return false;
				}
			}
			return true;
		} else if (goal instanceof GdlDistinct){
			GdlDistinct gdd = (GdlDistinct)goal;
			//TODO: check
			// both variables must be bound for us to retain this
			for(GdlLiteral fact : facts){
				if(fact instanceof GdlDistinct){
					GdlDistinct factDis = (GdlDistinct)fact;
					if(canProveTerm(gdd.getArg1(),  gv, factDis.getArg1())
							&& canProveTerm(gdd.getArg2(),  gv, factDis.getArg2())){
						return true;
					}
				}
			}
			return false;
		} else if (goal instanceof GdlProposition){
			return facts.contains(goal);
		} else if (goal instanceof GdlRelation){
			for(GdlLiteral fact : facts){
				if(fact instanceof GdlRelation){
					GdlRelation factRel = (GdlRelation)fact;
					GdlRelation goalRel = (GdlRelation)goal;
					if(factRel.getName().equals(goalRel.getName()) && factRel.arity() == goalRel.arity()){
						boolean canProve = true;

						for(int i = 0 ; i < factRel.arity(); i ++){
							GdlTerm fdt = factRel.get(i);
							GdlTerm gdt = goalRel.get(i);
							if(!canProveTerm(gdt, gv, fdt)){
								canProve = false;
								break;
							}
						}

						if(canProve){
							return true;
						}
					}
				}
			}
			return false;
		}
		System.out.println("Unmatched!");
		return false;
	}

	private boolean canProveTerm(GdlTerm arg1, List<GdlVariable> gv, GdlTerm gr) {
		if(arg1 instanceof GdlConstant){
			if(gr instanceof GdlConstant){
				return gr.equals(arg1);
			}else {
				return false;
			}
		} else if (arg1 instanceof GdlVariable){
			if(gr instanceof GdlVariable){
				return !gv.contains(arg1);
			} else {
				return false;
			}
		} else if (arg1 instanceof GdlFunction){
			if(gr instanceof GdlFunction){
				GdlFunction gfr = (GdlFunction)gr;
				GdlFunction argfr = (GdlFunction)arg1;
				if(gfr.getName().equals(argfr.getName()) && gfr.arity() == argfr.arity()){
					for(int i = 0; i < gfr.arity(); i++){
						if(!canProveTerm(argfr.get(i), gv, gfr.get(i))){
							return false;
						}
					}
					return true;
				} else {
					return false;
				}
			} else {
				return false;
			}
		}
		return true;
	}
}
