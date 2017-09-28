package cop5556sp17;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import cop5556sp17.Scanner.Kind;

public class Scanner {
	/**
	 * Kind enum
	 */
	
	public static enum Kind {
		IDENT("IDENT"), INT_LIT("INT_LIT"), KW_INTEGER("integer"), KW_BOOLEAN("boolean"), 
		KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), KW_FRAME("frame"), 
		KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"), 
		SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"), 
		RBRACE("}"), ARROW("->"), BARARROW("|->"), OR("|"), AND("&"), 
		EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(">="), 
		PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"), 
		ASSIGN("<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"), 
		KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"), 
		OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"), 
		KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"), 
		KW_SCALE("scale"), EOF("eof"), COMMENT("comment");

		Kind(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
		
		//to remove
		public static Kind fromString(String text) {
			if (text != null) {
				for (Kind b : Kind.values()) {
					if (text.equalsIgnoreCase(b.text)) {
					return b;
					}
				}
			}
			return null;
		}

	}
	
	public static enum ScanState{
		START("START"), IN_DIGIT("IN_DIGIT"), IN_IDENT("IN_IDENT"),
		AFTER_EQ("AFTER_EQ"), AFTER_BAR("AFTER_BAR"), AFTER_MINUS("AFTER_MINUS"),
		AFTER_BARMINUS("AFTER_BARMINUS"), AFTER_NOT("AFTER_NOT"), AFTER_G("AFTER_G"),
		AFTER_L("AFTER_G"), AFTER_SLASH("AFTER_SLASH"), COMMENT("COMMENT");
		
		ScanState(String text) {
			this.text = text;
		}

		final String text;

		String getText() {
			return text;
		}
	}
	

/**
 * Thrown by Scanner when an illegal character is encountered
 */
	@SuppressWarnings("serial")
	public static class IllegalCharException extends Exception {
		public IllegalCharException(String message) {
			super(message);
		}
	}
	
	/**
	 * Thrown by Scanner when an int literal is not a value that can be represented by an int.
	 */
	@SuppressWarnings("serial")
	public static class IllegalNumberException extends Exception {
	public IllegalNumberException(String message){
		super(message);
		}
	}
	

	/**
	 * Holds the line and position in the line of a token.
	 */
	static class LinePos {
		public final int line;
		public final int posInLine;
		
		public LinePos(int line, int posInLine) {
			super();
			this.line = line;
			this.posInLine = posInLine;
		}

		@Override
		public String toString() {
			return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
		}
	}
		

	public class Token {
		public final Kind kind;
		public final int pos;  //position in input array
		public final int length;  
		
		//returns the text of this Token
		public String getText() {
			//TODO IMPLEMENT THIS
			return chars.substring(pos, pos+length);
		}
				
		//returns a LinePos object representing the line and column of this Token
		LinePos getLinePos(){
			//TODO IMPLEMENT THIS
			int i=0;
			LinePos linePos = null;
			if(chars.length()==0){
				linePos=new LinePos(i,0);
			}
			else{
				while(i<linePositions.size()){
					if(linePositions.get(i)<=pos){
						linePos=new LinePos(i,pos-linePositions.get(i));
						i=i+1;
					}
					else{
						break;
					}
				}				
			}
			return linePos;
		}

		Token(Kind kind, int pos, int length) {
			this.kind = kind;
			this.pos = pos;
			this.length = length;
			
		}
		
		/** 
		 * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
		 * Note that the validity of the input should have been checked when the Token was created.
		 * So the exception should never be thrown.
		 * 
		 * @return  int value of this token, which should represent an INT_LIT
		 * @throws NumberFormatException
		 */
		public int intVal() throws NumberFormatException{
			//TODO IMPLEMENT THIS
			if(kind != Kind.INT_LIT){
				throw new NumberFormatException("Invalied conversion to integer from " + getText());
			}
				return (int) Integer.parseInt(getText());
		}

		public boolean isKind(Kind eof) {
			if(this.kind.equals(eof)) return true;
			return false;
		}
		
		  @Override
		  public int hashCode() {
		   final int prime = 31;
		   int result = 1;
		   result = prime * result + getOuterType().hashCode();
		   result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		   result = prime * result + length;
		   result = prime * result + pos;
		   return result;
		  }

		  @Override
		  public boolean equals(Object obj) {
		   if (this == obj) {
		    return true;
		   }
		   if (obj == null) {
		    return false;
		   }
		   if (!(obj instanceof Token)) {
		    return false;
		   }
		   Token other = (Token) obj;
		   if (!getOuterType().equals(other.getOuterType())) {
		    return false;
		   }
		   if (kind != other.kind) {
		    return false;
		   }
		   if (length != other.length) {
		    return false;
		   }
		   if (pos != other.pos) {
		    return false;
		   }
		   return true;
		  }

		 

		  private Scanner getOuterType() {
		   return Scanner.this;
		  }
		  
	}

	 


	Scanner(String chars) {
		this.chars = chars;
		tokens = new ArrayList<Token>();
		initScan();
	}


	
	/**
	 * Initializes Scanner object by traversing chars and adding tokens to tokens list.
	 * 
	 * @return this scanner
	 * @throws IllegalCharException 
	 * @throws IllegalNumberException 
	 * @throws Exception 
	 */
	public Scanner scan() throws IllegalCharException, IllegalNumberException {
		int pos = 0; 
		//TODO IMPLEMENT THIS!!!!
		int length=chars.length();
		int ch;
		ScanState state= ScanState.START;
		int startPos=0;
		while(pos<=length){
		  ch=pos<length ?chars.charAt(pos):-1;
		  switch(state){
		  case START:{
			  pos = skipWhiteSpace(pos);
			  ch=pos<length ?chars.charAt(pos):-1;
			  startPos=pos;
			switch (ch) {
				//operators
				case -1:{
					tokens.add(new Token(Kind.EOF,pos,0));
					pos=pos+1;
					break;
				}
				case '|':{
					state=ScanState.AFTER_BAR;
					pos=pos+1;
					break;
				}
				case '&':{
					tokens.add(new Token(Kind.AND, pos, 1));
					pos=pos+1;
					break;
				}
				case '=':{
					state=ScanState.AFTER_EQ;
					pos=pos+1;
					break;
				}
				case '+':{
					tokens.add(new Token(Kind.PLUS, pos, 1));
					pos=pos+1;
					break;
				}
				case '-':{
					state=ScanState.AFTER_MINUS;
					pos=pos+1;
					break;
				}
				case '*':{
					tokens.add(new Token(Kind.TIMES, pos, 1));
					pos=pos+1;
					break;
				}
				case '/':{
					state=ScanState.AFTER_SLASH;
					pos=pos+1;
					break;
				}
				case '%':{
					tokens.add(new Token(Kind.MOD, pos, 1));
					pos=pos+1;
					break;
				}
				
				case '!':{
					state=ScanState.AFTER_NOT;
					pos=pos+1;
					break;
				}
				case '<':{
					state=ScanState.AFTER_L;
					pos=pos+1;
					break;
				}				
				case '>':{
					state=ScanState.AFTER_G;
					pos=pos+1;
					break;
				}
				case ';':{
					tokens.add(new Token(Kind.SEMI,pos,1));
					pos=pos+1;
					break;
				}
				case ',':{
					tokens.add(new Token(Kind.COMMA,pos,1));
					pos=pos+1;
					break;
				}
				case '(':{
					tokens.add(new Token(Kind.LPAREN,pos,1));
					pos=pos+1;
					break;
				}
				case ')':{
					tokens.add(new Token(Kind.RPAREN,pos,1));
					pos=pos+1;
					break;
				}
				case '{':{
					tokens.add(new Token(Kind.LBRACE, pos, 1));
					pos=pos+1;
					break;
				}
				case '}':{
					tokens.add(new Token(Kind.RBRACE, pos, 1));
					pos=pos+1;
					break;
				}
				case '0':{
					tokens.add(new Token(Kind.INT_LIT, startPos, 1));
					pos=pos+1;
					break;
				}
				default:
					if(Character.isDigit(ch)){
						state=ScanState.IN_DIGIT;
						pos=pos+1;
						break;
					}
					else if(Character.isJavaIdentifierStart(ch)){
						state=ScanState.IN_IDENT;
						pos=pos+1;
						break;
					}
					else{
						throw new IllegalCharException("Illegal char" + (char) ch +" at position "+pos);
					}
				}
			break;
		  }
		  
		  case AFTER_BAR:{
			 ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '-':{
					state=ScanState.AFTER_BARMINUS;
					pos=pos+1;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.OR, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_BARMINUS:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '>':{
			  		tokens.add(new Token(Kind.BARARROW, startPos, 3));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.OR, startPos, 1));
					tokens.add(new Token(Kind.MINUS, startPos+1, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_EQ:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
     		  	case '=':{
			  		tokens.add(new Token(Kind.EQUAL, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					pos=pos+1;
					throw new IllegalCharException("Illegal char" + (char) ch +" at position "+pos);
				}
			  break;
		  }
		  case AFTER_MINUS:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '>':{
			  		tokens.add(new Token(Kind.ARROW, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.MINUS, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_NOT:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '=':{
			  		tokens.add(new Token(Kind.NOTEQUAL, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.NOT, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_L:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '=':{
			  		tokens.add(new Token(Kind.LE, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
			  	case '-':{
			  		tokens.add(new Token(Kind.ASSIGN, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.LT, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_G:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '=':{
			  		tokens.add(new Token(Kind.GE, startPos, 2));
					pos=pos+1;
					state=ScanState.START;
					break;
			  	}
				default:
					tokens.add(new Token(Kind.GT, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case AFTER_SLASH:{
			  ch=pos<length?chars.charAt(pos):-1;
			  switch (ch) {
			  	case '*':{
			  			state=ScanState.COMMENT;
			  			pos=pos+1;
			  			break;
			  	}
				default:
					tokens.add(new Token(Kind.DIV, startPos, 1));
					state=ScanState.START;
					break;
				}
			  break;
		  }
		  case IN_DIGIT:{
			  ch=pos<length?chars.charAt(pos):-1;
			  if(ch!=-1){
				  while(ch!=-1 && Character.isDigit(ch)){
					  pos=pos+1;
					  ch=pos<length?chars.charAt(pos):-1;
				  }
				  String literal=chars.substring(startPos, pos);
				  try{
					  Integer.parseInt(literal);
					  tokens.add(new Token(Kind.INT_LIT, startPos, pos-startPos));
					  state=ScanState.START;
				  }
				  catch(Exception e){
					  throw new IllegalNumberException("The Number"+literal+" is out of range of a Java-int");
				  }
			  }
			  else{
				  tokens.add(new Token(Kind.INT_LIT, startPos, 1));
				  state=ScanState.START;
			  }
			  break;
		  }
		  case IN_IDENT:{
			  ch=pos<length?chars.charAt(pos):-1;
			  if(ch!=-1){
				  while((ch!=-1) && (Character.isJavaIdentifierPart(ch))){
					  pos=pos+1;
					  ch=pos<length?chars.charAt(pos):-1;
				  }
				  String literal=chars.substring(startPos, pos);
				  state=ScanState.START;
				  if(keyWords.containsKey(literal)) {
				     	tokens.add(new Token(keyWords.get(literal), startPos, pos-startPos));
				  }
				  else{
					  tokens.add(new Token(Kind.IDENT, startPos, pos-startPos));
				  }
			  }
			  else{
				  tokens.add(new Token(Kind.IDENT, startPos, 1));
			  }
				  state=ScanState.START;
				  break;
		  }
		  case COMMENT:{
			  ch=pos<length?chars.charAt(pos):-1;
			  if(ch == '*'){
				  if(pos+1 < chars.length() && chars.charAt(pos+1)=='/'){
					  state=ScanState.START;
					  pos=pos+2;
				  }
				  else{
					  pos++;
				  }
			  }
			  else if (ch == -1) {
				  state= ScanState.START;
			  }
			  else if (ch == '\n') {
				  linePositions.add(linePositions.size(), pos+1);
				  pos=pos+1;
			  }
			  else{
				  pos++;
			  }
		  }
		  }
		}
		return this;  
	}

	private int skipWhiteSpace(int pos) {
		while(pos<chars.length()){
			if(chars.charAt(pos)==' '  || chars.charAt(pos)=='\r' || chars.charAt(pos)=='\t'){
				pos=skipWhiteSpace(pos+1);
				break;
			}else if (chars.charAt(pos)=='\n') {
					linePositions.add(linePositions.size(),pos+1);
					pos=skipWhiteSpace(pos+1);
					break;
			 }
			else{
				return pos;
			}
		 }
		return pos;
	}

	final ArrayList<Token> tokens;
	final String chars;
	int tokenNum;
	//extra variable
	static Map<String,Kind> keyWords ;
	static ArrayList<Integer> linePositions;
	
	//extra method --implemented
	public static void initScan(){
		linePositions=new ArrayList<Integer>();
		linePositions.add(0,0);
		keyWords=new HashMap<String,Kind>()
				{{
					put("integer", Kind.KW_INTEGER);
					put("boolean",Kind.KW_BOOLEAN);
					put("image",Kind.KW_IMAGE);
					put("url",Kind.KW_URL);
					put("file",Kind.KW_FILE);
					put("frame",Kind.KW_FRAME);
					put("while", Kind.KW_WHILE);
					put("if",Kind.KW_IF);
					put("sleep",Kind.OP_SLEEP);
					put("screenheight",Kind.KW_SCREENHEIGHT);
					put("screenwidth", Kind.KW_SCREENWIDTH);
					put("gray",Kind.OP_GRAY);
					put("convolve",Kind.OP_CONVOLVE);
					put("blur",Kind.OP_BLUR);
					put("scale",Kind.KW_SCALE);
					put("width",Kind.OP_WIDTH);
					put("height",Kind.OP_HEIGHT);
					put("xloc",Kind.KW_XLOC);
					put("yloc",Kind.KW_YLOC);
					put("hide",Kind.KW_HIDE);
					put("show",Kind.KW_SHOW);
					put("move",Kind.KW_MOVE);
					put("true",Kind.KW_TRUE);
					put("false",Kind.KW_FALSE);
				}};
	}

	/*
	 * Return the next token in the token list and update the state so that
	 * the next call will return the Token..  
	 */
	public Token nextToken() {
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum++);
	}
	
	/*
	 * Return the next token in the token list without updating the state.
	 * (So the following call to next will return the same token.)
	 */
	public Token peek(){
		if (tokenNum >= tokens.size())
			return null;
		return tokens.get(tokenNum);		
	}

	

	/**
	 * Returns a LinePos object containing the line and position in line of the 
	 * given token.  
	 * 
	 * Line numbers start counting at 0
	 * 
	 * @param t
	 * @return
	 */
	public LinePos getLinePos(Token t) {
		//TODO IMPLEMENT THIS
		return t.getLinePos();
	}


}
