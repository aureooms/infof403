package cs.parser;


import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.HashMap;



import cs.lexer.*;
import cs.parser.functAST.*;
import cs.parser.exprAST.*;

import cs.parser.io.*;
import cs.parser.variable.*;

import cs.parser.declaration.*;
import cs.parser.assign.*;

public class Parser{

	protected Scanner cobolScanner;
	protected Symbol<String> token;
	protected boolean inBuffer = false;
	protected VariableAllocator variableAllocator = new VariableAllocator();
	
	protected HashMap<String,VariableDecl> variables = new HashMap<String,VariableDecl>();


	public Parser(Scanner cobolScanner){
		this.cobolScanner = cobolScanner;
	}

	public void read() throws Exception{
		if(this.inBuffer) this.inBuffer = false;
		else this.token = cobolScanner.next_token();
	}

	public void unread() throws Exception{
		this.inBuffer = true;
	}

	public void match(LexicalUnit unit) throws Exception{
		if(!this.is_token_unit(unit)) this.handle_bad_token(unit);
	}

	public boolean is_token_unit(LexicalUnit unit){
		return this.token.unit.equals(unit);
	}

	public void handle_bad_token(LexicalUnit[] units) throws Exception{
		throw new SCOBOLGrammaticalException(units, this.token.unit, this.token.getValue(), (Integer) this.token.get(Symbol.LINE), (Integer) this.token.get(Symbol.COLUMN));
	}

	public void handle_bad_token(LexicalUnit unit) throws Exception{
		throw new SCOBOLGrammaticalException(unit, this.token.unit, this.token.getValue(), (Integer) this.token.get(Symbol.LINE), (Integer) this.token.get(Symbol.COLUMN));
	}

	public void compile() throws Exception{
		this.handle_S();
	}

	protected int parseInteger(){
		return Integer.decode(token.getValue());
	}

	protected double parseReal(){
		return Double.parseDouble(token.getValue());
	}


	protected VariableDecl parseImage(){
		VariableDecl ret = null;
		int imSize = 0; // # of 9 in () AND after v
		String image = token.getValue();
		boolean signed = image.contains("s");
		boolean floating = image.contains("v");
		String nines ="";

		boolean inside = false;
		
		for(char c : image.toCharArray()) {
  			
			if(c==')'){
				inside=false;
				imSize+=Integer.decode(nines);
				nines="";
			}
			if(inside){
				nines+=c;
			}
			if(c=='('){inside=true;}
		}
		
		int imageBitSize = 0;
		if(imSize > 0 ){

			imageBitSize = (int)Math.ceil( ( Math.log(Math.pow(imSize,10))/Math.log(2) ) /8 );
		}


		if(imageBitSize < 16 ){// in future make different classes 
			if(floating){ ret = new RealDecl(Integer.toString(16),signed);}
			else{ ret = new IntegerDecl(Integer.toString(16),signed);}
		}
		else if(imageBitSize < 32 ){
			if(floating){ ret = new RealDecl(Integer.toString(32),signed);}
			else{ ret = new IntegerDecl(Integer.toString(32),signed);}
		}
		else if(imageBitSize < 64 ){
			if(floating){ ret = new RealDecl(Integer.toString(64),signed);}
			else{ ret = new IntegerDecl(Integer.toString(64),signed);}
		}
		else if(imageBitSize < 128 ){
			if(floating){ ret = new RealDecl(Integer.toString(128),signed);}
			else {ret = new IntegerDecl(Integer.toString(128),signed);}
		}
		else{
			//ouuuups
		}
				
		return ret;
	}

	protected Assign createAssign(String var, IntegerVariable expr){
		return new Assign(variables.get(var),expr);
	}

	

	public void handle_ASSIGNATION() throws Exception{
		this.read();
		Assign assign = null;
		String varName;
		IntegerVariable newVal;
		ATail tail;
		switch(this.token.unit){
			case MOVE:
				newVal = this.handle_EXPRESSION(); 

				this.read();
				this.match(LexicalUnit.TO);
				this.read();
				this.match(LexicalUnit.IDENTIFIER);

				varName = token.getValue();

				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				this.createAssign(varName,newVal);
				break;
			case COMPUTE:
				this.read();

				this.match(LexicalUnit.IDENTIFIER);

				varName = token.getValue();
				
				this.read();
				this.match(LexicalUnit.EQUALS_SIGN);

				newVal = this.handle_EXPRESSION();

				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				this.createAssign(varName,newVal);

				break;
			case ADD:
				
				newVal = this.handle_EXPRESSION();

				this.read();
				this.match(LexicalUnit.TO);
				this.read();
				this.match(LexicalUnit.IDENTIFIER);

				varName = token.getValue();

				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				new AssignSA(this.variables.get(varName),newVal,this.variableAllocator.getNext(),this.variableAllocator.getNext(),"add").genCode();

				break;
			case SUBTRACT:
				
				newVal = this.handle_EXPRESSION();

				this.read();
				this.match(LexicalUnit.FROM);
				this.read();
				this.match(LexicalUnit.IDENTIFIER);

				varName = token.getValue();

				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				new AssignSA(this.variables.get(varName),newVal,this.variableAllocator.getNext(),this.variableAllocator.getNext(), "sub").genCode();

				break;
			case MULTIPLY:

				//(VariableDecl l, Variable r, String temp, VariableDecl to, String op)
				tail = this.handle_ASSIGN_TAIL();
					
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				new AssignOp (tail.getL(), tail.getR(), this.variableAllocator.getNext(), tail.getTo(), "mul").genCode();
				
				break;
			case DIVIDE:
				
				tail = this.handle_ASSIGN_TAIL();
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				new AssignOp (tail.getL(), tail.getR(), this.variableAllocator.getNext(), tail.getTo(), "div").genCode();
				
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.MOVE, LexicalUnit.COMPUTE, LexicalUnit.ADD, LexicalUnit.SUBTRACT, LexicalUnit.MULTIPLY, LexicalUnit.DIVIDE});
				break;
		}
	}
	
	public ATail handle_ASSIGN_TAIL() throws Exception{

		IntegerVariable l = (IntegerVariable)this.handle_EXPRESSION();
		this.read();
		this.match(LexicalUnit.COMMA);
		IntegerVariable r = (IntegerVariable)this.handle_EXPRESSION();
		this.read();
		this.match(LexicalUnit.GIVING);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);

		return new ATail(l,r,this.variables.get(token.getValue()));
	}
	
	public void handle_CALL() throws Exception{
		this.read();
		this.match(LexicalUnit.PERFORM);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);
		this.handle_CALL_TAIL();
	}
	
	public void handle_CALL_TAIL() throws Exception{
		this.read();
		switch(this.token.unit){
			case UNTIL:
				this.handle_EXPRESSION();
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);
				break;
			case END_OF_INSTRUCTION:
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.UNTIL, LexicalUnit.END_OF_INSTRUCTION});
				break;
		}
	}
	
	public void handle_COMP() throws Exception{
		this.read();
		switch(this.token.unit){
			case EQUALS_SIGN:
				break;
			case LOWER_THAN:
				break;
			case GREATER_THAN:
				break;
			case LOWER_OR_EQUALS:
				break;
			case GREATER_OR_EQUALS:
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.EQUALS_SIGN, LexicalUnit.LOWER_THAN, LexicalUnit.GREATER_THAN, LexicalUnit.LOWER_OR_EQUALS, LexicalUnit.GREATER_OR_EQUALS});
				break;
		}
	}
	
	public void handle_DATA() throws Exception{
		this.read();
		this.match(LexicalUnit.DATA);
		this.read();
		this.match(LexicalUnit.DIVISION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.WORKING_STORAGE);
		this.read();
		this.match(LexicalUnit.SECTION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.handle_VAR_LIST();
	}
	
	public void handle_ENV() throws Exception{
		this.read();
		this.match(LexicalUnit.ENVIRONMENT);
		this.read();
		this.match(LexicalUnit.DIVISION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.CONFIGURATION);
		this.read();
		this.match(LexicalUnit.SECTION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.SOURCE_COMPUTER);
		this.read();
		this.match(LexicalUnit.DOT);
		this.handle_WORDS();
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.OBJECT_COMPUTER);
		this.read();
		this.match(LexicalUnit.DOT);
		this.handle_WORDS();
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
	}
	


	public IntegerVariable handle_EXPRESSION() throws Exception{
		this.handle_EXPRESSION_1();
		this.handle_EXPRESSION_TAIL();
		return new IntegerVariable(true,32,"%test");
	}
	
	public ExprAST handle_EXPRESSION_1() throws Exception{
		ExprAST ret = null;
		this.handle_EXPRESSION_2();
		this.handle_EXPRESSION_1_TAIL();
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_1_TAIL() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case AND:
				this.handle_EXPRESSION_2();
				this.handle_EXPRESSION_1_TAIL();
				break;
			case END_OF_INSTRUCTION:
			case THEN:
			case FROM:
			case COMMA:
			case TO:
			case GIVING:
			case OR:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.AND, LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.THEN, LexicalUnit.FROM, LexicalUnit.COMMA, LexicalUnit.TO, LexicalUnit.GIVING, LexicalUnit.OR});
				break;
		}
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_2() throws Exception{
		ExprAST ret = null;
		this.handle_EXPRESSION_3();
		this.handle_EXPRESSION_2_TAIL();
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_2_TAIL() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case EQUALS_SIGN:
			case LOWER_THAN:
			case GREATER_THAN:
			case LOWER_OR_EQUALS:
			case GREATER_OR_EQUALS:
				this.unread();
				this.handle_COMP();
				this.handle_EXPRESSION_3();
				this.handle_EXPRESSION_2_TAIL();
				break;
			case END_OF_INSTRUCTION:
			case THEN:
			case FROM:
			case COMMA:
			case TO:
			case AND:
			case GIVING:
			case OR:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.EQUALS_SIGN, LexicalUnit.LOWER_THAN, LexicalUnit.GREATER_THAN, LexicalUnit.LOWER_OR_EQUALS, LexicalUnit.GREATER_OR_EQUALS, LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.THEN, LexicalUnit.FROM, LexicalUnit.COMMA, LexicalUnit.TO, LexicalUnit.AND, LexicalUnit.GIVING, LexicalUnit.OR});
				break;
		}
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_3() throws Exception{
		ExprAST ret = null;
		this.handle_EXPRESSION_4();
		this.handle_EXPRESSION_3_TAIL();
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_3_TAIL() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case PLUS_SIGN:
				this.handle_EXPRESSION_4();
				this.handle_EXPRESSION_3_TAIL();
				break;
			case MINUS_SIGN:
				this.handle_EXPRESSION_4();
				this.handle_EXPRESSION_3_TAIL();
				break;
			case THEN:
			case LOWER_OR_EQUALS:
			case GREATER_OR_EQUALS:
			case GIVING:
			case OR:
			case EQUALS_SIGN:
			case END_OF_INSTRUCTION:
			case FROM:
			case COMMA:
			case TO:
			case AND:
			case GREATER_THAN:
			case LOWER_THAN:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.PLUS_SIGN, LexicalUnit.MINUS_SIGN, LexicalUnit.THEN, LexicalUnit.LOWER_OR_EQUALS, LexicalUnit.GREATER_OR_EQUALS, LexicalUnit.GIVING, LexicalUnit.OR, LexicalUnit.EQUALS_SIGN, LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.FROM, LexicalUnit.COMMA, LexicalUnit.TO, LexicalUnit.AND, LexicalUnit.GREATER_THAN, LexicalUnit.LOWER_THAN});
				break;
		}
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_4() throws Exception{
		ExprAST ret = null;
		this.handle_EXPRESSION_BASE();
		this.handle_EXPRESSION_4_TAIL();
		return ret;

	}

	
	public ExprAST handle_EXPRESSION_4_TAIL() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case ASTERISK:
				this.handle_EXPRESSION_BASE();
				this.handle_EXPRESSION_4_TAIL();
				break;
			case SLASH:
				this.handle_EXPRESSION_BASE();
				this.handle_EXPRESSION_4_TAIL();
				break;
			case THEN:
			case LOWER_OR_EQUALS:
			case GREATER_OR_EQUALS:
			case GIVING:
			case OR:
			case EQUALS_SIGN:
			case END_OF_INSTRUCTION:
			case PLUS_SIGN:
			case MINUS_SIGN:
			case FROM:
			case COMMA:
			case TO:
			case AND:
			case GREATER_THAN:
			case LOWER_THAN:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.ASTERISK, LexicalUnit.SLASH, LexicalUnit.THEN, LexicalUnit.LOWER_OR_EQUALS, LexicalUnit.GREATER_OR_EQUALS, LexicalUnit.GIVING, LexicalUnit.OR, LexicalUnit.EQUALS_SIGN, LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.PLUS_SIGN, LexicalUnit.MINUS_SIGN, LexicalUnit.FROM, LexicalUnit.COMMA, LexicalUnit.TO, LexicalUnit.AND, LexicalUnit.GREATER_THAN, LexicalUnit.LOWER_THAN});
				break;
		}
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_BASE() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case LEFT_PARENTHESIS:
				this.handle_EXPRESSION_BASE();
				this.read();
				this.match(LexicalUnit.RIGHT_PARENTHESIS);
				break;
			case NOT:
				this.handle_EXPRESSION_BASE();
				break;
			case MINUS_SIGN:
				this.handle_EXPRESSION_BASE();
				break;
			case IDENTIFIER:
				break;
			case INTEGER:
				break;
			case TRUE:
				break;
			case FALSE:
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.LEFT_PARENTHESIS, LexicalUnit.NOT, LexicalUnit.MINUS_SIGN, LexicalUnit.IDENTIFIER, LexicalUnit.INTEGER, LexicalUnit.TRUE, LexicalUnit.FALSE});
				break;
		}
		return ret;

	}
	
	public ExprAST handle_EXPRESSION_TAIL() throws Exception{
		ExprAST ret = null;
		this.read();
		switch(this.token.unit){
			case OR:
				this.handle_EXPRESSION_1();
				this.handle_EXPRESSION_TAIL();
				break;
			case END_OF_INSTRUCTION:
			case THEN:
			case FROM:
			case COMMA:
			case TO:
			case GIVING:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.OR, LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.THEN, LexicalUnit.FROM, LexicalUnit.COMMA, LexicalUnit.TO, LexicalUnit.GIVING});
				break;
		}
		return ret;
	}
	
	public void handle_IDENT() throws Exception{
		this.read();
		this.match(LexicalUnit.IDENTIFICATION);
		this.read();
		this.match(LexicalUnit.DIVISION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.PROGRAM_ID);
		this.read();
		this.match(LexicalUnit.DOT);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.AUTHOR);
		this.read();
		this.match(LexicalUnit.DOT);
		this.handle_WORDS();
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.DATE_WRITTEN);
		this.read();
		this.match(LexicalUnit.DOT);
		this.handle_WORDS();
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
	}
	
	public void handle_IF() throws Exception{
		this.read();
		this.match(LexicalUnit.IF);
		this.handle_EXPRESSION();
		this.read();
		this.match(LexicalUnit.THEN);
		this.handle_INSTRUCTION_LIST();
		this.handle_IF_TAIL();
	}
	
	public void handle_IF_TAIL() throws Exception{
		this.read();
		switch(this.token.unit){
			case ELSE:
				this.handle_INSTRUCTION_LIST();
				this.read();
				this.match(LexicalUnit.END_IF);
				break;
			case END_IF:
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.ELSE, LexicalUnit.END_IF});
				break;
		}
	}
	
	public void handle_INSTRUCTION() throws Exception{
		this.read();
		switch(this.token.unit){
			case MOVE:
			case COMPUTE:
			case ADD:
			case SUBTRACT:
			case MULTIPLY:
			case DIVIDE:
				this.unread();
				this.handle_ASSIGNATION();
				break;
			case IF:
				this.unread();
				this.handle_IF();
				break;
			case PERFORM:
				this.unread();
				this.handle_CALL();
				break;
			case ACCEPT:
				this.unread();
				this.handle_READ();
				break;
			case DISPLAY:
				this.unread();
				this.handle_WRITE();
				break;
			case STOP:
				this.read();
				this.match(LexicalUnit.RUN);
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.MOVE, LexicalUnit.COMPUTE, LexicalUnit.ADD, LexicalUnit.SUBTRACT, LexicalUnit.MULTIPLY, LexicalUnit.DIVIDE, LexicalUnit.IF, LexicalUnit.PERFORM, LexicalUnit.ACCEPT, LexicalUnit.DISPLAY, LexicalUnit.STOP});
				break;
		}
	}
	
	public void handle_INSTRUCTION_LIST() throws Exception{
		this.read();
		switch(this.token.unit){
			case MOVE:
			case COMPUTE:
			case ADD:
			case SUBTRACT:
			case MULTIPLY:
			case DIVIDE:
			case IF:
			case PERFORM:
			case ACCEPT:
			case DISPLAY:
			case STOP:
				this.unread();
				this.handle_INSTRUCTION();
				this.handle_INSTRUCTION_LIST();
				break;
			case ELSE:
			case END:
			case IDENTIFIER:
			case END_IF:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.MOVE, LexicalUnit.COMPUTE, LexicalUnit.ADD, LexicalUnit.SUBTRACT, LexicalUnit.MULTIPLY, LexicalUnit.DIVIDE, LexicalUnit.IF, LexicalUnit.PERFORM, LexicalUnit.ACCEPT, LexicalUnit.DISPLAY, LexicalUnit.STOP, LexicalUnit.ELSE, LexicalUnit.END, LexicalUnit.IDENTIFIER, LexicalUnit.END_IF});
				break;
		}
	}
	
	public void handle_LABELS() throws Exception{
		this.read();
		this.match(LexicalUnit.IDENTIFIER);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.handle_INSTRUCTION_LIST();
		this.handle_LABELS_TAIL();
	}
	
	public void handle_LABELS_TAIL() throws Exception{
		this.read();
		switch(this.token.unit){
			case IDENTIFIER:
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);
				this.handle_INSTRUCTION_LIST();
				this.handle_LABELS_TAIL();
				break;
			case END:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.IDENTIFIER, LexicalUnit.END});
				break;
		}
	}
	
	public void handle_PROC() throws Exception{
		this.read();
		this.match(LexicalUnit.PROCEDURE);
		this.read();
		this.match(LexicalUnit.DIVISION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);
		this.read();
		this.match(LexicalUnit.SECTION);
		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
		this.handle_LABELS();
		this.read();
		this.match(LexicalUnit.END);
		this.read();
		this.match(LexicalUnit.PROGRAM);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);
		this.read();
		this.match(LexicalUnit.DOT);
	}
	
	public void handle_PROGRAM() throws Exception{
		this.handle_IDENT();
		this.handle_ENV();
		this.handle_DATA();
		this.handle_PROC();
	}
	
	public void handle_READ() throws Exception{
		this.read();
		this.match(LexicalUnit.ACCEPT);
		this.read();
		this.match(LexicalUnit.IDENTIFIER);

		//new Accept("i" + this.variables.get(this.token.getValue()).getLLVMSize(), "%" + this.token.getValue()).genCode();
		new Accept("i32", "%" + this.token.getValue()).genCode();

		this.read();
		this.match(LexicalUnit.END_OF_INSTRUCTION);
	}
	
	public void handle_S() throws Exception{
		this.handle_PROGRAM();
		this.read();
		this.match(LexicalUnit.EOF);
	}
	
	public void handle_VAR_DECL() throws Exception{

		this.read();
		this.match(LexicalUnit.INTEGER);

		this.read();
		this.match(LexicalUnit.IDENTIFIER);

		String variableName = token.getValue();

		this.read();
		this.match(LexicalUnit.PIC);

		this.read();
		this.match(LexicalUnit.IMAGE);

		VariableDecl newVariable = (VariableDecl) this.parseImage();

		newVariable.setName(variableName);


		this.handle_VAR_DECL_TAIL(newVariable);
		newVariable.genCode();
		this.variables.put(variableName,newVariable);

	}

	public void handle_VAR_DECL_TAIL(VariableDecl newVariable) throws Exception{
		this.read();
		switch(this.token.unit){
			case END_OF_INSTRUCTION:
				break;
			case VALUE:
				this.read();
				this.match(LexicalUnit.INTEGER);

				newVariable.setValue(this.parseInteger());

				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);

				break;

			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.END_OF_INSTRUCTION, LexicalUnit.VALUE});
				break;
		}

	}
	
	public void handle_VAR_LIST() throws Exception{
		this.read();
		switch(this.token.unit){
			case INTEGER:
				this.unread();
				this.handle_VAR_DECL();
				this.handle_VAR_LIST();
				break;
			case PROCEDURE:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.INTEGER, LexicalUnit.PROCEDURE});
				break;
		}
	}
	
	public void handle_WORDS() throws Exception{
		this.read();
		switch(this.token.unit){
			case IDENTIFIER:
				this.handle_WORDS();
				break;
			case END_OF_INSTRUCTION:
				this.unread();
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.IDENTIFIER, LexicalUnit.END_OF_INSTRUCTION});
				break;
		}
	}
	
	public void handle_WRITE() throws Exception{
		this.read();
		this.match(LexicalUnit.DISPLAY);
		this.handle_WRITE_TAIL();
	}
	
	public void handle_WRITE_TAIL() throws Exception{
		this.read();
		switch(this.token.unit){
			case LEFT_PARENTHESIS:
			case NOT:
			case MINUS_SIGN:
			case IDENTIFIER:
			case INTEGER:
			case TRUE:
			case FALSE:
				this.unread();
				Variable expression = this.handle_EXPRESSION();
				new Display(expression.getType(), expression.getName()).genCode();
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);
				break;
			case STRING:
				new Display(StringVariable.TYPE, token.getValue()).genCode();
				this.read();
				this.match(LexicalUnit.END_OF_INSTRUCTION);
				break;
			default:
				this.handle_bad_token(new LexicalUnit[]{LexicalUnit.LEFT_PARENTHESIS, LexicalUnit.NOT, LexicalUnit.MINUS_SIGN, LexicalUnit.IDENTIFIER, LexicalUnit.INTEGER, LexicalUnit.TRUE, LexicalUnit.FALSE, LexicalUnit.STRING});
				break;
		}
	}
	
}
