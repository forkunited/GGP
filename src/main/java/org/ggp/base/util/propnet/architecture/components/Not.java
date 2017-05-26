package org.ggp.base.util.propnet.architecture.components;

import java.util.Map;
import java.util.Set;

import org.ggp.base.util.propnet.architecture.Component;

/**
 * The Not class is designed to represent logical NOT gates.
 */
@SuppressWarnings("serial")
public final class Not extends Component
{
	/**
	 * @see org.ggp.base.util.propnet.architecture.Component#toString()
	 */
	@Override
	public String toString()
	{
		return toDot("invtriangle", "grey", "NOT");
	}

	@Override
	public Component cloneHelper(Set<Component> filter, Map<Component, Component> oldToNew) {
		return new Not();
	}

	@Override
	public boolean getPropValue() {
		// TODO Auto-generated method stub
		return !getSingleInput().state;
	}
}