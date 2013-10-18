package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class AD extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public AD(){
		super(SCobol.LexicalUnit.IDENTIFIER);
		transition.put('d', SCobol.DFAState.ADD);
		DFATools.fill(transition, Alphabet.IDENTIFIER, SCobol.DFAState.IDENTIFIER_2);
	}
}