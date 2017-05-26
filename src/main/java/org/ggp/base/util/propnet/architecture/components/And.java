package org.ggp.base.util.propnet.architecture.components;

import java.util.Map;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The And class is designed to represent logical AND gates.
 */
@SuppressWarnings("serial")
public final class And extends Component
{

	public int numTrue = 0;

	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invhouse", "grey", "AND");
	}

	@Override
	public Component cloneHelper(Set<Component> filter, Map<Component, Component> oldToNew) {
		return new And();
	}

	@Override
	public boolean getPropValue() {
		// TODO Auto-generated method stub
		return numTrue == getInputArray().size();
	}
}
