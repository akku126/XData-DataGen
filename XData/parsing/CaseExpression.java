/**@author mathew
 * 
 * Class used for encoding a CaseExpression. Any Case expression (currently assumes that
 * case expressions are non-nested, i.e. no case expression within  another) is 
 * encoded as a list of when Conditionals and an else conditional. Also 
 * assumes that cases with switch expressions are normalized to ones that does not 
 * have switch expressions. For instance, Case A When X1 then Y1 When X2 Then Y2 ..
 * is normalized to Case When A=X1 then Y1 When A=X2 Then Y2 ...
 * 
 * */
package parsing;

import java.io.Serializable;
import java.util.ArrayList;

public class CaseExpression implements Cloneable,Serializable{
	
	private static final long serialVersionUID = -7192918525557389737L;
	
	ArrayList<CaseCondition> whenConditionals;
	CaseCondition elseConditional;
	
	public ArrayList<CaseCondition> getWhenConditionals(){
		return whenConditionals;
	}
	
	public void setWhenConditionals(ArrayList<CaseCondition> conditionals){
		whenConditionals=conditionals;
	}
	
	public CaseCondition getElseConditional(){
		return elseConditional;
	}
	
	public void setElseConditional(CaseCondition condition){
		elseConditional=condition;
	}
	
	@Override
	public String toString(){
		String tempString="";
		for(CaseCondition cond:whenConditionals)
			tempString+=cond.toString()+", ";
		if(elseConditional!=null)
			tempString+=" ELSE "+elseConditional.getThenNode().toString();
		return tempString;
	}
	
	@Override
	public CaseExpression clone() throws CloneNotSupportedException{
		Object obj=super.clone();
		((CaseExpression)obj).setElseConditional(elseConditional.clone());
		ArrayList<CaseCondition> whenConds=(ArrayList<CaseCondition>)whenConditionals.clone();
		((CaseExpression)obj).setWhenConditionals(whenConds);
		return ((CaseExpression)obj);
	}
}
