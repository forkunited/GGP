package org.ggp.dhtp.util;

import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Role;

public class FixedBounder extends Bounder{
	int maxLevel;
	public FixedBounder(int maxLevel) {
		this.maxLevel = maxLevel;
	}

	@Override
	public boolean shouldExpand(Role role, MachineState state, int level) {
		return level >= this.maxLevel;
	}
}
