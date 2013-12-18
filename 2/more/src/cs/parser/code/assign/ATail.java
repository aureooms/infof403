package cs.parser.code.assign; 

import cs.parser.data.variable.*;
import cs.parser.data.declaration.*;

public class ATail {


	public IntegerVariable l;
	public IntegerVariable r;
	public VariableDecl to;

	public ATail (IntegerVariable l, IntegerVariable r, VariableDecl to){
		this.to = to;
		this.l = l;
		this.r = r;

	}

	public IntegerVariable getL(){
		return l;
	}
	public IntegerVariable getR(){
		return r;
	}
	public VariableDecl getTo(){
		return to;
	}

}
