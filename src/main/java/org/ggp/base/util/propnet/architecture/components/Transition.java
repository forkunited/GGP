package org.ggp.base.util.propnet.architecture.components;

import java.util.Map;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Transition class is designed to represent pass-through gates.
 */
@SuppressWarnings("serial")
public final class Transition extends Component
{
	/**
	 * Returns the value of the input to the transition.
	 *
	 * @see org.ggp.base.util.propnet.architecture.Component#getValue()
	 */
	@Override
	public boolean getValue()
	{
		return getSingleInput().getValue();
	}

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("box", "grey", "TRANSITION");
	}

	@Override
	public Component cloneHelper(Set<Component> filter, Map<Component, Component> oldToNew) {
		return new Transition();
	}

	@Override
	public boolean getPropValue() {
		// TODO Auto-generated method stub
		return state;
	}
}