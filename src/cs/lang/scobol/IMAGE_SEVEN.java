

package cs.lang.scobol;

import cs.lang.SCobol;
import cs.lang.DFAState;

import cs.lang.Alphabet;
import cs.lang.DFATools;

public class IMAGE_SEVEN extends DFAState<SCobol.DFAState, SCobol.LexicalUnit, Character>{
	public IMAGE_SEVEN(){
		super(null);

		transition.put(')', SCobol.DFAState.IMAGE_FINAL_TWO);
		DFATools.fill(transition, Alphabet.DIGIT, SCobol.DFAState.IMAGE_SEVEN);
	}
}
