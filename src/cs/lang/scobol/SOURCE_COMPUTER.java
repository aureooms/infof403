package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class SOURCE_COMPUTER extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public SOURCE_COMPUTER(){
		super(SCobol.LexicalUnit.SOURCE_COMPUTER_KEYWORD);
		DFATools.fill(transition, Alphabet.IDENTIFIER, SCobol.DFAState.IDENTIFIER_15);
	}
}
