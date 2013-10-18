package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class DISPLA extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public DISPLA(){
		super(SCobol.LexicalUnit.IDENTIFIER);
		transition.put('y', SCobol.DFAState.DISPLAY);
		DFATools.fill(transition, Alphabet.IDENTIFIER, SCobol.DFAState.IDENTIFIER_6);
	}
}
