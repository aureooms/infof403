

package cs.parser.declaration;

public class RealDecl extends VariableDecl<Double>{
	
	public RealDecl(){
		super();
	}
	
	public RealDecl (String size){
		super(size);
	}

	public String getLLVMType(){return "i";}//replace by float declaration syntax in LLVM

	public String getValue(){return Double.toString(val);}

}