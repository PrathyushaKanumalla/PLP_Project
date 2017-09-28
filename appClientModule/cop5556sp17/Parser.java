package cop5556sp17;

import cop5556sp17.Scanner.Kind;
import static cop5556sp17.Scanner.Kind.*;

import java.util.ArrayList;
import java.util.List;

import cop5556sp17.Scanner.Token;
import cop5556sp17.AST.ASTNode;
import cop5556sp17.AST.ASTVisitor;
import cop5556sp17.AST.AssignmentStatement;
import cop5556sp17.AST.BinaryChain;
import cop5556sp17.AST.BinaryExpression;
import cop5556sp17.AST.Block;
import cop5556sp17.AST.BooleanLitExpression;
import cop5556sp17.AST.Chain;
import cop5556sp17.AST.ChainElem;
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
import cop5556sp17.AST.WhileStatement;

public class Parser {

	/**
	 * Exception to be thrown if a syntax error is detected in the input.
	 * You will want to provide a useful error message.
	 *
	 */
	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		public SyntaxException(String message) {
			super(message);
		}
	}

	/**
	 * Useful during development to ensure unimplemented routines are
	 * not accidentally called during development.  Delete it when 
	 * the Parser is finished.
	 *
	 */
	@SuppressWarnings("serial")	
	public static class UnimplementedFeatureException extends RuntimeException {
		public UnimplementedFeatureException() {
			super();
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	/**
	 * parse the input using tokens from the scanner.
	 * Check for EOF (i.e. no trailing junk) when finished
	 * 
	 * @throws SyntaxException
	 */
	Program parse() throws SyntaxException {
		Program p =program();
		matchEOF();
		return p;
	}

	Expression expression() throws SyntaxException {
		Expression e = term();
		Token firstToken = e.getFirstToken();
		while(t.isKind(Kind.LT) || t.isKind(Kind.LE) || t.isKind(Kind.GT) || t.isKind(Kind.GE) || 
				t.isKind(Kind.EQUAL) || t.isKind(Kind.NOTEQUAL)) {
			Token t = consume();
			e = new BinaryExpression(firstToken, e, t, term());
		}
		return e;
	}

	Expression term() throws SyntaxException {
		Expression e = elem();
		Token firstToken = e.getFirstToken();
		while(t.isKind(Kind.PLUS) || t.isKind(Kind.MINUS) || t.isKind(Kind.OR)) {
			Token t = consume();
			e = new BinaryExpression(firstToken, e, t, elem());
		}
		return e;
	}

	Expression elem() throws SyntaxException {
		Expression e = factor();
		Token firstToken = e.getFirstToken();
		while(t.isKind(Kind.TIMES) || t.isKind(Kind.DIV) || t.isKind(Kind.AND) || t.isKind(Kind.MOD)) {
			Token t = consume();
			e = new BinaryExpression(firstToken, e, t, factor());
		}
		return e;
	}

	Expression factor() throws SyntaxException {
		Kind kind = t.kind;
		switch (kind) {
			case IDENT: {
				Token ft = consume();
				return new IdentExpression(ft);
			}
			case INT_LIT: {
				Token ft = consume();
				return new IntLitExpression(ft);
			}
			case KW_TRUE: case KW_FALSE: {
				Token ft = consume();
				return new BooleanLitExpression(ft);
			}
			case KW_SCREENWIDTH:
			case KW_SCREENHEIGHT: {
				Token ft = consume();
				return new ConstantExpression(ft);
			}
			case LPAREN: {
				consume();
				Expression e = expression();
				match(RPAREN);
				return e;
			}
			default:
			//you will want to provide a more useful error message
				throw new SyntaxException("illegal factor");
		}
	}

	Block block() throws SyntaxException {
		/* block ::= { ( dec | statement) * }
		dec ::= (  KW_INTEGER | KW_BOOLEAN | KW_IMAGE | KW_FRAME)    IDENT
		statement ::=   OP_SLEEP expression ; | whileStatement | ifStatement | chain ; | assign ;
		 */
		Token firstToken = match(Kind.LBRACE);
		ArrayList<Dec> decs = new ArrayList<Dec>();
		ArrayList<Statement> statements = new ArrayList<Statement>();
		while(!t.isKind(Kind.RBRACE)){
			if((t.isKind(Kind.KW_INTEGER) || t.isKind(Kind.KW_BOOLEAN) || t.isKind(Kind.KW_IMAGE) 
					|| t.isKind(Kind.KW_FRAME) )) {
				decs.add(dec());
			}
			else {
				statements.add(statement());
			}
		}
		match(Kind.RBRACE);
		return new Block(firstToken, decs, statements);
	}

	Program program() throws SyntaxException {
		Token firstToken = match(Kind.IDENT);
		ArrayList<ParamDec> paramList = new ArrayList<>();
		if(!(t.isKind(Kind.LBRACE))) {
			paramList.addAll(paramDecs());
		}
		Block block = block();
		Program program = new Program(firstToken, paramList, block);
		return program;
	}

	ArrayList<ParamDec> paramDecs() throws SyntaxException {
		ArrayList<ParamDec> paramList = new ArrayList<>();
		paramList.add(paramDec());
		while(t.isKind(Kind.COMMA)){
			consume();
			paramList.add(paramDec());
		}
		return paramList;
	}

	public ParamDec paramDec() throws SyntaxException {
	    //paramDec ::= ( KW_URL | KW_FILE | KW_INTEGER | KW_BOOLEAN)   IDENT
		return new ParamDec(match(Kind.KW_URL, Kind.KW_FILE, Kind.KW_INTEGER,Kind.KW_BOOLEAN), match(Kind.IDENT));
	}

	Dec dec() throws SyntaxException {
		//dec ::= (  KW_INTEGER | KW_BOOLEAN | KW_IMAGE | KW_FRAME)    IDENT
		return new Dec(match(Kind.KW_BOOLEAN, Kind.KW_INTEGER,Kind.KW_IMAGE,Kind.KW_FRAME),match(Kind.IDENT));
	}

	Statement statement() throws SyntaxException {
		/*statement ::=   OP_SLEEP expression ; | whileStatement | ifStatement | chain ; | assign ;
		*/
		switch (t.kind) {
			case KW_WHILE : {
				return whileLoop();
			}
			case KW_IF : {
				return ifCondition();
			}
			case IDENT : {
				Statement s = null;
				if(scanner.peek().isKind(Kind.ASSIGN)) {
					Token firstToken = consume();
					consume();
					Expression e = expression();
					s = new AssignmentStatement(firstToken, new IdentLValue(firstToken), e);
				}
				else {
					s = chain();
				}
				match(Kind.SEMI);
				return s;
			}
			case OP_BLUR: case OP_GRAY: case OP_CONVOLVE:
			case KW_SHOW: case KW_HIDE: case KW_MOVE: case KW_XLOC: case KW_YLOC:
			case OP_WIDTH: case OP_HEIGHT: case KW_SCALE: {
				Statement s= chain();
				match(Kind.SEMI);
				return s;
			}
			case OP_SLEEP: {
				Token firstToken = match(Kind.OP_SLEEP);
				Expression e = expression();
				match(Kind.SEMI);
				return new SleepStatement(firstToken,e);
			}
		}
		throw new SyntaxException("illegal Statement");
	}

	public WhileStatement whileLoop() throws SyntaxException {
		Token firstToken = match(Kind.KW_WHILE);
		match(Kind.LPAREN);
		Expression e = expression();
		match(Kind.RPAREN);
		Block b = block();
		return new WhileStatement(firstToken, e, b);
	}
	
	public Statement ifCondition() throws SyntaxException {
		Token firstToken = match(Kind.KW_IF);
		match(Kind.LPAREN);
		Expression e = expression();
		match(Kind.RPAREN);
		Block b = block();
		return new IfStatement(firstToken, e, b);
	}

	Chain chain() throws SyntaxException {
		/* chain ::=  chainElem arrowOp chainElem ( arrowOp  chainElem)*
 			chainElem ::= IDENT | filterOp arg | frameOp arg | imageOp arg
	    */
		Token fToken = t;
		ChainElem e0 = chainElem();
		Token temp = match((Kind.ARROW),(Kind.BARARROW));
		ChainElem e1 = chainElem();
		Chain c = new BinaryChain(fToken, e0, temp, e1);
		while(t.isKind(Kind.ARROW) || t.isKind(Kind.BARARROW)) {
			temp = consume();
			c = new BinaryChain(fToken, c, temp, chainElem()); 
		}
		return c;
	}

	ChainElem chainElem() throws SyntaxException {
		/*chainElem ::= IDENT | filterOp arg | frameOp arg | imageOp arg
	filterOp ::= OP_BLUR |OP_GRAY | OP_CONVOLVE
	frameOp ::= KW_SHOW | KW_HIDE | KW_MOVE | KW_XLOC |KW_YLOC
	imageOp ::= OP_WIDTH |OP_HEIGHT | KW_SCALE
	arg ::= E | ( expression (   ,expression)* )
		 */
		switch (t.kind) {
			case OP_BLUR: case OP_GRAY: case OP_CONVOLVE: {
				Token temp = consume();
				Tuple tuple = arg();
				return new FilterOpChain(temp, tuple);
			}
			case KW_SHOW: case KW_HIDE: case KW_MOVE: case KW_XLOC: case KW_YLOC:{
				Token temp = consume();
				Tuple tuple = arg();
				return new FrameOpChain(temp, tuple);
			}
			case OP_WIDTH: case OP_HEIGHT: case KW_SCALE:{
				Token temp = consume();
				Tuple tuple = arg();
				return new ImageOpChain(temp, tuple);
			}
			case IDENT: {
				Token temp = match(Kind.IDENT);
				return new IdentChain(temp);
			}
		}
		throw new SyntaxException("Expected element in Chain Element method");
	}

	Tuple arg() throws SyntaxException {
		List<Expression> argList = new ArrayList<>();
		Token temp = t;
		if(t.isKind(Kind.LPAREN)) {
			consume();
			argList.add(expression());
			while(t.isKind(COMMA)){
				consume();
				argList.add(expression());
			}
			match(Kind.RPAREN);
			return new Tuple(temp, argList);
		}
		return new Tuple(temp, argList);
	}

	/**
	 * Checks whether the current token is the EOF token. If not, a
	 * SyntaxException is thrown.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (t.isKind(EOF)) {
			return t;
		}
		throw new SyntaxException("expected EOF");
	}

	/**
	 * Checks if the current token has the given kind. If so, the current token
	 * is consumed and returned. If not, a SyntaxException is thrown.
	 * 
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		if (t.isKind(kind)) {
			return consume();
		}
		throw new SyntaxException("saw " + t.kind + "expected " + kind);
	}

	/**
	 * Checks if the current token has one of the given kinds. If so, the
	 * current token is consumed and returned. If not, a SyntaxException is
	 * thrown.
	 * 
	 * * Precondition: for all given kinds, kind != EOF
	 * 
	 * @param kinds
	 *            list of kinds, matches any one
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind... kinds) throws SyntaxException {
		for (Kind kind : kinds) {
			if (t.isKind(kind)) {
				return consume();
			}
		}
		throw new SyntaxException("saw " + t.kind + "expected " + " different Kind");
	}

	/**
	 * Gets the next token and returns the consumed token.
	 * 
	 * Precondition: t.kind != EOF
	 * 
	 * @return
	 * 
	 */
	private Token consume() throws SyntaxException {
		Token tmp = t;
		t = scanner.nextToken();
		return tmp;
	}

}
