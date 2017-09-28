package cop5556sp17;
import static cop5556sp17.AST.Type.TypeName.BOOLEAN;
import static cop5556sp17.AST.Type.TypeName.FILE;
import static cop5556sp17.AST.Type.TypeName.FRAME;
import static cop5556sp17.AST.Type.TypeName.IMAGE;
import static cop5556sp17.AST.Type.TypeName.INTEGER;
import static cop5556sp17.AST.Type.TypeName.NONE;
import static cop5556sp17.AST.Type.TypeName.URL;
import static cop5556sp17.Scanner.Kind.ARROW;
import static cop5556sp17.Scanner.Kind.BARARROW;
import static cop5556sp17.Scanner.Kind.DIV;
import static cop5556sp17.Scanner.Kind.EQUAL;
import static cop5556sp17.Scanner.Kind.GE;
import static cop5556sp17.Scanner.Kind.GT;
import static cop5556sp17.Scanner.Kind.KW_HIDE;
import static cop5556sp17.Scanner.Kind.KW_MOVE;
import static cop5556sp17.Scanner.Kind.KW_SCALE;
import static cop5556sp17.Scanner.Kind.KW_SHOW;
import static cop5556sp17.Scanner.Kind.KW_XLOC;
import static cop5556sp17.Scanner.Kind.KW_YLOC;
import static cop5556sp17.Scanner.Kind.LE;
import static cop5556sp17.Scanner.Kind.LT;
import static cop5556sp17.Scanner.Kind.MINUS;
import static cop5556sp17.Scanner.Kind.NOTEQUAL;
import static cop5556sp17.Scanner.Kind.OP_BLUR;
import static cop5556sp17.Scanner.Kind.OP_CONVOLVE;
import static cop5556sp17.Scanner.Kind.OP_GRAY;
import static cop5556sp17.Scanner.Kind.OP_HEIGHT;
import static cop5556sp17.Scanner.Kind.OP_WIDTH;
import static cop5556sp17.Scanner.Kind.PLUS;
import static cop5556sp17.Scanner.Kind.TIMES;

import java.util.ArrayList;
import java.util.List;

import cop5556sp17.Scanner.Kind;
import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.ConstantExpression;
import cop5556sp17.AST.Dec;
import cop5556sp17.AST.Expression;
import cop5556sp17.AST.FilterOpChain;
import cop5556sp17.AST.FrameOpChain;
import cop5556sp17.AST.IdentChain;
import cop5556sp17.AST.IdentExpression;
import cop5556sp17.AST.IdentLValue;
import cop5556sp17.AST.IfStatement;
import cop5556sp17.AST.ImageOpChain;
import cop5556sp17.AST.IntLitExpression;
import cop5556sp17.AST.ParamDec;
import cop5556sp17.AST.Program;
import cop5556sp17.AST.SleepStatement;
import cop5556sp17.AST.Statement;
import cop5556sp17.AST.Tuple;
import cop5556sp17.AST.Type.TypeName;
import cop5556sp17.AST.WhileStatement;

public class TypeCheckVisitor implements ASTVisitor {

	@SuppressWarnings("serial")
	public static class TypeCheckException extends Exception {
		TypeCheckException(String message) {
			super(message);
		}
	}

	SymbolTable symtab = new SymbolTable();

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		TypeName type1 = (TypeName) binaryChain.getE0().visit(this, arg);
		Token op = binaryChain.getArrow();
		TypeName type2 = (TypeName) binaryChain.getE1().visit(this, arg);
		if(type1.isType(URL) && op.isKind(ARROW) &&type2.isType(IMAGE)){
			binaryChain.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(FILE) && op.isKind(ARROW) &&type2.isType(IMAGE)){
			binaryChain.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(FRAME) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof FrameOpChain) 
				&& (binaryChain.getE1().firstToken.isKind(KW_XLOC)
						|| binaryChain.getE1().firstToken.isKind(KW_YLOC))){
			binaryChain.setTypeName(INTEGER);
			return INTEGER;
		}
		else if(type1.isType(FRAME) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof FrameOpChain) 
				&& (binaryChain.getE1().firstToken.isKind(KW_SHOW)
						|| binaryChain.getE1().firstToken.isKind(KW_HIDE)
						|| binaryChain.getE1().firstToken.isKind(KW_MOVE))){
			binaryChain.setTypeName(FRAME);
			return FRAME;
		}
		else if(type1.isType(IMAGE) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof ImageOpChain) 
				&& (binaryChain.getE1().firstToken.isKind(OP_WIDTH)
						|| binaryChain.getE1().firstToken.isKind(OP_HEIGHT))){
			binaryChain.setTypeName(INTEGER);
			return INTEGER;
		}
		else if(type1.isType(IMAGE) && op.isKind(ARROW) &&type2.isType(FRAME)){
			binaryChain.setTypeName(FRAME);
			return FRAME;
		}
		else if(type1.isType(IMAGE) && op.isKind(ARROW) &&type2.isType(FILE)){
			binaryChain.setTypeName(NONE);
			return NONE;
		}
		else if(type1.isType(IMAGE) && (op.isKind(ARROW) || op.isKind(BARARROW) ) && 
				(binaryChain.getE1() instanceof FilterOpChain) 
				&& (binaryChain.getE1().firstToken.isKind(OP_GRAY)
						|| binaryChain.getE1().firstToken.isKind(OP_BLUR)
						|| binaryChain.getE1().firstToken.isKind(OP_CONVOLVE))){
			binaryChain.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(IMAGE) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof ImageOpChain) 
				&& (binaryChain.getE1().firstToken.isKind(KW_SCALE))){
			binaryChain.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(IMAGE) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof IdentChain && type2.isType(IMAGE))){
			binaryChain.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(INTEGER) && op.isKind(ARROW) && 
				(binaryChain.getE1() instanceof IdentChain && type2.isType(INTEGER))){
			binaryChain.setTypeName(INTEGER);
			return INTEGER;
		}
		throw new TypeCheckException("AT BINARY CHAIN:: type1 "+type1+" op is "+op.kind+" type2"+type2);
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		TypeName type1 = (TypeName) binaryExpression.getE0().visit(this, arg);
		Token op = binaryExpression.getOp();
		TypeName type2 = (TypeName) binaryExpression.getE1().visit(this, arg);
		if(type1.isType(INTEGER) && (op.isKind(PLUS) || op.isKind(MINUS)) && type2.isType(INTEGER)) {
			binaryExpression.setTypeName(INTEGER);
			return INTEGER;
		}
		else if(type1.isType(IMAGE) && (op.isKind(PLUS) || op.isKind(MINUS)) && type2.isType(IMAGE)) {
			binaryExpression.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(INTEGER) && (op.isKind(TIMES) || op.isKind(DIV) || op.isKind(Kind.MOD)) && type2.isType(INTEGER)) {
			binaryExpression.setTypeName(INTEGER);
			return INTEGER;
		}
		else if(type1.isType(INTEGER) && (op.isKind(TIMES)) && type2.isType(IMAGE)) {
			binaryExpression.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(IMAGE) && (op.isKind(TIMES) || op.isKind(DIV) || op.isKind(Kind.MOD)) && type2.isType(INTEGER)) {
			binaryExpression.setTypeName(IMAGE);
			return IMAGE;
		}
		else if(type1.isType(INTEGER) && (op.isKind(LT) || op.isKind(LE) ||
				op.isKind(GT) || op.isKind(GE)) && type2.isType(INTEGER)) {
			binaryExpression.setTypeName(BOOLEAN);
			return BOOLEAN;
		}
		else if(type1.isType(BOOLEAN) && (op.isKind(Kind.AND)|| op.isKind(Kind.OR) || op.isKind(LT) || op.isKind(LE) ||
				op.isKind(GT) || op.isKind(GE)) && type2.isType(BOOLEAN)) {
			binaryExpression.setTypeName(BOOLEAN);
			return BOOLEAN;
		}
		else if((op.isKind(EQUAL) || op.isKind(NOTEQUAL)) && type1.equals(type2)) {
			binaryExpression.setTypeName(BOOLEAN);
			return BOOLEAN;
		}
		throw new TypeCheckException("AT BINARY EXPRESSION:: Error as type1 is "+type1+" and type2 is "+type2);
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		symtab.enterScope();
		ArrayList<Dec> decList = block.getDecs();
		ArrayList<Statement> statementList = block.getStatements();
		for (Dec dec : decList) {
			dec.visit(this, arg);
		}
		for (Statement statement : statementList) {
			symtab.enterScope();
			statement.visit(this, arg);
			symtab.leaveScope();
		}
		symtab.leaveScope();
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		booleanLitExpression.setTypeName(BOOLEAN);
		return BOOLEAN;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		Tuple tuple = filterOpChain.getArg();
		tuple.visit(this, null);
		if(tuple.getExprList().size() == 0) {
			filterOpChain.setTypeName(IMAGE);
			return IMAGE;
		}
		throw new TypeCheckException("AT FILTER OP CHAIN :: The tuple size is not 0");		
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		if (frameOpChain.getFirstToken().isKind(KW_SHOW) || frameOpChain.getFirstToken().isKind(KW_HIDE)) {
		    	Tuple tuple = frameOpChain.getArg();
			tuple.visit(this, null);
			List<Expression> expressionList = tuple.getExprList();
			if(expressionList.size() != 0) {
				throw new TypeCheckException("Arg length is expected to be 0 but it is " + expressionList.size());
			}
			frameOpChain.setTypeName(NONE);
		   
		}
		else if (frameOpChain.getFirstToken().isKind(KW_XLOC) || frameOpChain.getFirstToken().isKind(KW_YLOC)) {
			
			Tuple tuple = frameOpChain.getArg();
			tuple.visit(this, null);
			List<Expression> expressionList = tuple.getExprList();
			if(expressionList.size() != 0) {
				throw new TypeCheckException("Arg length is expected to be 0 but it is " + expressionList.size());
			}
			frameOpChain.setTypeName(INTEGER);
		}	
		else if(frameOpChain.getFirstToken().isKind(KW_MOVE)) {
			Tuple tuple = frameOpChain.getArg();
			tuple.visit(this, null);
			List<Expression> expressionList = tuple.getExprList();
			if(expressionList.size() != 2) {
				throw new TypeCheckException("Arg length is expected to be 2 but it is " + expressionList.size());
			}
			frameOpChain.setTypeName(NONE);
		}
		else {
			throw new TypeCheckException("Invalid FrameOp!");
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		Token t = identChain.getFirstToken();
		if(symtab.lookup(t.getText()) != null){
			Dec dec = symtab.lookup(t.getText());
			identChain.setTypeName(dec.getTypeName());
			identChain.setDec(dec);
			return dec.getTypeName();
		}
		throw new TypeCheckException("AT IDENT CHAIN:: The identifier "+t.getText()+" not found in the symbol table");
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		Dec dec = symtab.lookup(identExpression.getFirstToken().getText());
		if(dec != null){
			identExpression.setTypeName(dec.getTypeName());
			identExpression.setDec(dec);
			return dec.getTypeName();
		}
		throw new TypeCheckException("AT IDENT EXPRESSION:: The ident value "+identExpression.getFirstToken()+" does not exist in the symbol table");
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		TypeName e = (TypeName) ifStatement.getE().visit(this, arg);
		if(e.isType(BOOLEAN)){
			ifStatement.getB().visit(this, arg);
			return null;
		}
			throw new TypeCheckException("AT IF STATEMENT:: The condition is not boolean and is "+e);
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		intLitExpression.setTypeName(INTEGER);
		return INTEGER;
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		Expression e = sleepStatement.getE();
		e.visit(this, arg);
		if(!e.getTypeName().isType(TypeName.INTEGER)){
			throw new TypeCheckException("AT SLEEP STATEMENT:: Type Exception as the expression is of the type "+e.getTypeName());
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		TypeName e = (TypeName) whileStatement.getE().visit(this, arg);
		if(e.isType(BOOLEAN)){
			whileStatement.getB().visit(this, arg);
			return null;
		}
			throw new TypeCheckException("AT WHILE STATEMENT:: The condition is not boolean and is "+e);
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		declaration.setTypeName(cop5556sp17.AST.Type.getTypeName(declaration.getType()));
		if(!symtab.insert(declaration.getIdent().getText(), declaration))
			throw new TypeCheckException("AT VISITING DEC::The variable "+declaration.getIdent().getText()+" is already presen in the symbol table");
		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		ArrayList<ParamDec> params = program.getParams();
		Block b = program.getB();
		for (ParamDec paramDec : params) {
			paramDec.visit(this, arg);
		}
		b.visit(this, arg);
		return null;
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		Expression e = assignStatement.getE();
		IdentLValue var = assignStatement.getVar();
		TypeName type = (TypeName) var.visit(this, arg);
		e.visit(this, arg);
		if(!e.getTypeName().isType(type)){
			throw new TypeCheckException("AT ASSIGNMENT STATEMENT::Type Exception as the expression is of the type "+e.getTypeName()+" and the identLvalue is in "+type);
		}
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		Dec dec = symtab.lookup(identX.getText());
		if(dec != null){
			identX.setDec(dec);
			return dec.getTypeName();
		}
		throw new TypeCheckException("AT IDENTLVALUE:: The identifier "+identX+" not found in the symbol table");
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		paramDec.setTypeName(cop5556sp17.AST.Type.getTypeName(paramDec.getType()));
		if(!symtab.insert(paramDec.getIdent().getText(), paramDec))
			throw new TypeCheckException("AT VISITING PARAMDEC:: Variable "+paramDec.getIdent().getText()+" is already present in the symbol table");
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		constantExpression.setTypeName(INTEGER);
		return INTEGER;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		if(imageOpChain.getFirstToken().getText().contentEquals(OP_WIDTH.getText()) || imageOpChain.getFirstToken().getText().contentEquals(OP_HEIGHT.getText())) {
			Tuple tuple = imageOpChain.getArg();
			tuple.visit(this, null);
			List<Expression> expressionList = tuple.getExprList();
			if(expressionList.size() != 0) {
				throw new TypeCheckException("Arg length is expected to be 0 but it is " + expressionList.size());
			}
			imageOpChain.setTypeName(INTEGER);
			return INTEGER;
		}
		else if(imageOpChain.getFirstToken().getText().contentEquals(KW_SCALE.getText())) {
			Tuple tuple = imageOpChain.getArg();
			tuple.visit(this, null);
			List<Expression> expressionList = tuple.getExprList();
			if(expressionList.size() != 1) {
				throw new TypeCheckException("Arg length is expected to be 0 but it is " + expressionList.size());
			}
			imageOpChain.setTypeName(IMAGE);
			return IMAGE;
		}
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		List<Expression> list = tuple.getExprList();
		for (Expression expression : list) {
			if(!((TypeName)expression.visit(this, arg)).isType(INTEGER))
				throw new TypeCheckException("AT TUPLE:: Expression has a non integer typename");
		}
		return null;
	}


}
