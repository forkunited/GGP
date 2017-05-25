package org.ggp.dhtp.propnet;

import java.util.BitSet;

import org.ggp.base.util.statemachine.MachineState;

public class InternalMachineState extends MachineState {

	private final BitSet bits;
	public InternalMachineState(BitSet inBits){
		this.bits = (BitSet)inBits.clone();
	}

	public BitSet getBitSet(){
		return bits;
	}

	@Override
	public java.util.Set<org.ggp.base.util.gdl.grammar.GdlSentence> getContents() {
		throw new UnsupportedOperationException();
	};

	@Override
    public InternalMachineState clone() {
        return new InternalMachineState((BitSet)getBitSet().clone());
    }

    /* Utility methods */
    @Override
    public int hashCode()
    {
        return getBitSet().hashCode();
    }

    @Override
    public String toString()
    {
        BitSet contents = getBitSet();
        if(contents == null)
            return "(MachineState with null contents)";
        else
            return contents.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        if ((o != null) && (o instanceof InternalMachineState))
        {
        	InternalMachineState state = (InternalMachineState) o;
            return state.getBitSet().equals(getBitSet());
        }

        return false;
    }

}
