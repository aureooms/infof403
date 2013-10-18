package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class M extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public M(){
		super(SCobol.LexicalUnit.IDENTIFIER);
		transition.put('o', SCobol.DFAState.MO);
		transition.put('u', SCobol.DFAState.MU);
		DFATools.fill(transition, Alphabet.IDENTIFIER, SCobol.DFAState.IDENTIFIER_1);
	}
}