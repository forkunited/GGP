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
    			GdlRule gdr = reorderRule((GdlRule)g);
    			newList.add(gdr);
    		} else {
    			newList.add(g);
    		}

    	}

    	return newList;
    }

	private GdlRule reorderRule(GdlRule g) {
		System.out.println(">>RULE<<");
		System.out.println(g.getHead().toString());
		List<GdlVariable> boundVars = new ArrayList<GdlVariable>();
		List<GdlLiteral> literalsRemaining = new ArrayList<GdlLiteral>(g.getBody());
		List<GdlLiteral> newOrder = new ArrayList<GdlLiteral>();
		while(!literalsRemaining.isEmpty()){
			List<GdlVariable> newVars = new ArrayList<GdlVariable>();
			GdlLiteral nextToAdd = getBestForReordering(literalsRemaining, boundVars, newVars);
			literalsRemaining.remove(nextToAdd);
			newOrder.add(nextToAdd);
			System.out.println("Added subgoal"+nextToAdd.toString());
			System.out.println("It has "+newVars.size() + " new variables");
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
}
