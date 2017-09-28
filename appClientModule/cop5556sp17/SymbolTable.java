package cop5556sp17;



import java.util.HashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Stack;

import cop5556sp17.AST.Dec;


public class SymbolTable {
	
	
	//TODO  add fields - identScopedecMap
	Map<String, Map<Integer, Dec> > map = new HashMap<String, Map<Integer, Dec>>();
	Stack<Integer> scopeStack = new Stack<Integer>();
	
	static Integer currentScope = 0;

	/** 
	 * to be called when block entered
	 */
	public void enterScope(){
		//TODO:  IMPLEMENT THIS
		scopeStack.push(++currentScope);
	}
	
	
	/**
	 * leaves scope
	 */
	public void leaveScope(){
		//TODO:  IMPLEMENT THIS
		scopeStack.pop();
	}
	
	public boolean insert(String ident, Dec dec){
		if(map.containsKey(ident)) {
			Map<Integer, Dec> decMap = map.get(ident);
			if(decMap.containsKey(scopeStack.peek())){
				return false;
			}
			decMap.put(scopeStack.peek(), dec);
		}
		else{
			Map<Integer, Dec> decMap = new HashMap<Integer, Dec>();
			decMap.put(scopeStack.peek(), dec);
			map.put(ident, decMap);
		}
		return true;
	}
	
	public Dec lookup(String ident){
		if(map.containsKey(ident)){
			Map<Integer, Dec> decMap = map.get(ident);
			ListIterator<Integer> listItr = scopeStack.listIterator(scopeStack.size());
			while(listItr.hasPrevious()) {
				Integer topScope = listItr.previous();
				if(decMap.containsKey(topScope)){
					return decMap.get(topScope);
				}
			}
		}
		return null;
	}
		
	public SymbolTable() {
		scopeStack.push(0);		
	}


	@Override
	public String toString() {
		//TODO:  IMPLEMENT THIS
		return "";
	}
	
	


}
