package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class COMPUTE extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public COMPUTE(){
		super(SCobol.LexicalUnit.COMPUTE_KEYWORD);
		DFATools.fill(transition, Alphabet.IDENTIFIER, SCobol.DFAState.IDENTIFIER_7);
	}
}
