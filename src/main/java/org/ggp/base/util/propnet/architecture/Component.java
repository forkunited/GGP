package org.ggp.base.util.propnet.architecture;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ggp.base.util.gdl.grammar.GdlConstant;
import org.ggp.base.util.gdl.grammar.GdlProposition;
import org.ggp.base.util.gdl.grammar.GdlRelation;
import org.ggp.base.util.propnet.architecture.components.Constant;
import org.ggp.base.util.propnet.architecture.components.Proposition;

/**
 * The root class of the Component hierarchy, which is designed to represent
 * nodes in a PropNet. The general contract of derived classes is to override
 * all methods.
 */

public abstract class Component implements Serializable
{

	private static final long serialVersionUID = 352524175700224447L;
    /** The inputs to the component. */
    private final Set<Component> inputs;
    /** The outputs of the component. */
    private final Set<Component> outputs;
    public boolean state = false;
    public boolean initialized = false;
    public boolean isBase = false;
    public boolean isInput = false;
    public boolean inQueue = false;

    /**
     * Creates a new Component with no inputs or outputs.
     */
    public Component()
    {
        this.inputs = new HashSet<Component>();
        this.outputs = new HashSet<Component>();
    }

    /**
     * Adds a new input.
     *
     * @param input
     *            A new input.
     */
    public void addInput(Component input)
    {
        inputs.add(input);
    }

    public void removeInput(Component input)
    {
    	inputs.remove(input);
    }

    public void removeOutput(Component output)
    {
    	outputs.remove(output);
    }

    public void removeAllInputs()
    {
		inputs.clear();
	}

	public void removeAllOutputs()
	{
		outputs.clear();
	}

    /**
     * Adds a new output.
     *
     * @param output
     *            A new output.
     */
    public void addOutput(Component output)
    {
        outputs.add(output);
    }

    /**
     * Getter method.
     *
     * @return The inputs to the component.
     */
    public Set<Component> getInputs()
    {
        return inputs;
    }

    /**
     * A convenience method, to get a single input.
     * To be used only when the component is known to have
     * exactly one input.
     *
     * @return The single input to the component.
     */
    public Component getSingleInput() {
        assert inputs.size() == 1;
        return inputs.iterator().next();
    }

    /**
     * Getter method.
     *
     * @return The outputs of the component.
     */
    public Set<Component> getOutputs()
    {
        return outputs;
    }

    /**
     * A convenience method, to get a single output.
     * To be used only when the component is known to have
     * exactly one output.
     *
     * @return The single output to the component.
     */
    public Component getSingleOutput() {
        assert outputs.size() == 1;
        return outputs.iterator().next();
    }

    /**
     * Returns the value of the Component.
     *
     * @return The value of the Component.
     */
    public boolean getValue()
	{
		return this.state;
	}
    /**
     * Returns a representation of the Component in .dot format.
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public abstract String toString();

    /**
     * Returns a configurable representation of the Component in .dot format.
     *
     * @param shape
     *            The value to use as the <tt>shape</tt> attribute.
     * @param fillcolor
     *            The value to use as the <tt>fillcolor</tt> attribute.
     * @param label
     *            The value to use as the <tt>label</tt> attribute.
     * @return A representation of the Component in .dot format.
     */
    protected String toDot(String shape, String fillcolor, String label)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("\"@" + Integer.toHexString(hashCode()) + "\"[shape=" + shape + ", style= filled, fillcolor=" + fillcolor + ", label=\"" + label + "\"]; ");
        for ( Component component : getOutputs() )
        {
            sb.append("\"@" + Integer.toHexString(hashCode()) + "\"->" + "\"@" + Integer.toHexString(component.hashCode()) + "\"; ");
        }

        return sb.toString();
    }

    public Component clone(Set<Component> filter, Map<Component, Component> oldToNew) {
    	if (oldToNew.containsKey(this))
    		return oldToNew.get(this);

    	Component cloneInstance = cloneHelper(filter, oldToNew);
    	oldToNew.put(this, cloneInstance);

    	for (Component input : this.inputs) {
    		if (!filter.contains(input))
    			continue;
    		Component cloneInput = null;
    		if (oldToNew.containsKey(input))
    			cloneInput = oldToNew.get(input);
    		else
    			cloneInput = input.clone(filter, oldToNew);

    		cloneInstance.addInput(cloneInput);
    	}

    	for (Component output : this.outputs) {
    		if (!filter.contains(output))
    			continue;
    		Component cloneOutput = null;
    		if (oldToNew.containsKey(output))
    			cloneOutput = oldToNew.get(output);
    		else
    			cloneOutput = output.clone(filter, oldToNew);

    		cloneInstance.addOutput(cloneOutput);
    	}

    	return cloneInstance;
    }

    public boolean isInput() {
		if (!(this instanceof Proposition))
			return false;

		Proposition proposition = (Proposition)this;

	    // Skip all propositions that aren't GdlFunctions.
		if (!(proposition.getName() instanceof GdlRelation))
		    return false;

		GdlRelation relation = (GdlRelation) proposition.getName();
		return relation.getName().getValue().equals("does");
	}

	public boolean isInit() {
		if (!(this instanceof Proposition))
			return false;

		Proposition proposition = (Proposition)this;

		if (!(proposition.getName() instanceof GdlProposition))
		   return false;

		GdlConstant constant = ((GdlProposition) proposition.getName()).getName();
		return constant.getValue().toUpperCase().equals("INIT");
	}

	public boolean isDangling() {
		return getInputs().size() == 0 && !isInput() && !isInit() && !(this instanceof Constant);
	}

    public abstract Component cloneHelper(Set<Component> filter, Map<Component, Component> oldToNew);
}