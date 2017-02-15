package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.CaseCondition;
import parsing.CaseExpression;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class GenerateConstraintsForCaseConditions {

	public static String getConstraintsInCaseStatement(GenerateCVC1 cvc, QueryBlockDetails qbt, CaseCondition cc, Vector<CaseCondition> caseConditonCompleted) throws Exception {
		//Constraints constraints = new Constraints();
		String constraints = "";
		int offset=0, count=0;
		boolean present = false;
		String tableNameNumber = null;
		
		
		if(cc.getWhenNode() != null && cc.getThenNode() != null){
			
		tableNameNumber = UtilsRelatedToNode.getTableNameNo(cc.getThenNode());
				
		offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
		count = cvc.getNoOfTuples().get(tableNameNumber);
		
		/** Get the index of this subquery node*/

		constraints +="ASSERT (";
		
		if(cc.getWhenNode().getLeft() != null && cc.getWhenNode().getLeft().getType().equalsIgnoreCase(Node.getColRefType())){
			constraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt,cc.getWhenNode().getLeft(), 0+offset);
		}else{
			constraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt,cc.getWhenNode().getRight(), 0+offset);
		}
		
		if(caseConditonCompleted == null || (caseConditonCompleted != null && caseConditonCompleted.size()==0)){
			
			constraints +=");";
		}
		for(int i = 0 ; i < caseConditonCompleted.size();i++){
			CaseCondition ccCompleted = caseConditonCompleted.get(i);
			//constraints +="ASSERT (";
			constraints +=") AND ( ";
			//Generate constraint that holds these conditions as false and current condition alone as true
			constraints += GenerateCVCConstraintForNode.genNegativeCondsForPred(qbt, ccCompleted.getWhenNode().getLeft(), 0+offset);
			if(i == (caseConditonCompleted.size()-1)){
				constraints +=");";
			}else{
				constraints += ") AND (";
			}
			
		}
		}//If loop for case conditions ends
		//Handle ELSE part of CASE condition here
		else{
			//Get table details from completed case nodes
			if(caseConditonCompleted != null && caseConditonCompleted.size() > 0){
				CaseCondition ccForTableInfo = caseConditonCompleted.get(0);
				tableNameNumber = UtilsRelatedToNode.getTableNameNo(ccForTableInfo.getThenNode());
			}
			
			offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
			count = cvc.getNoOfTuples().get(tableNameNumber);
			//Generate negative condition for all positive conditions in CASE stmnt
			for(int i = 0 ; i < caseConditonCompleted.size();i++){
				CaseCondition ccCompleted = caseConditonCompleted.get(i);
				constraints +="ASSERT (";
				//constraints +=") AND ( ";
				//Generate constraint that holds these conditions as false and current condition alone as true
				constraints += GenerateCVCConstraintForNode.genNegativeCondsForPred(qbt, ccCompleted.getWhenNode(), 0+offset);
				if(i == (caseConditonCompleted.size()-1)){
					constraints +=");";
				}else{
					constraints += ") AND (";
				}
				
			}
		}
		return constraints;
	}
	
	/**
	 * This method generates constraint string for the Case Conditions
	 * to generate data for original query - consider only where clause CASE stmnts
	 * 
	 * @param cvc
	 * @return
	 */
	public static String getCaseConditionConstraints(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{
		String constraintString = "ASSERT (";
		HashMap<Integer,CaseExpression> ccMap = cvc.getqStructure().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
				ArrayList<CaseCondition> selectionConds = ccMap.get(2).getWhenConditionals();
				for(int i=0; i < selectionConds.size(); i++){
					
					CaseCondition sc = selectionConds.get(i);
					constraintString += getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted);
					
					caseConditionCompleted.add(sc);
					constraintString += ") OR (";
				}
				if((ccMap.get(2).getElseConditional()) != null){
				
					CaseCondition sc = (ccMap.get(2).getElseConditional());
					constraintString += getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted);
					constraintString += ")";
				}
			}
				else{
					constraintString += ")";
				}
			}
			
		
		return constraintString;
	}
	
	/**
	 * This method generates constraint string for the Case Conditions
	 * to generate data for original query - consider only where clause CASE stmnts
	 * 
	 * @param cvc
	 * @return 
	 */
	public static String getCaseConditionConstraintsForOriginalQuery(GenerateCVC1 cvc, QueryBlockDetails qbt) throws Exception{
		String constraintString = " ((";//"ASSERT ((";
		HashMap<Integer,CaseExpression> ccMap = cvc.getqStructure().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		int offset = 0;
		int count=0;
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
			CaseExpression selectionConds = ccMap.get(2);
				for(int i=0; i < selectionConds.getWhenConditionals().size(); i++){
					
					CaseCondition sc = selectionConds.getWhenConditionals().get(i);
					String tableNameNumber = UtilsRelatedToNode.getTableNameNo(sc.getWhenNode());			
					offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
					count = cvc.getNoOfTuples().get(tableNameNumber);
					constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc.getOuterBlock(), sc.getWhenNode(), 0+offset);
					if(sc.getWhenNode() != null){
						//if then node is colRef 
						if(sc.getWhenNode().getLeft() != null && sc.getWhenNode().getLeft().getType().equalsIgnoreCase(Node.getColRefType())){
							constraintString += " AND ("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getWhenNode().getLeft().getColumn(), (0+offset)+"") +" "+ sc.getWhenNode().getOperator() +" "+//TODO REMOVE HARDCODING OF OPERATOR
								sc.getWhenNode().getRight()+")";
							
						}else if(sc.getWhenNode().getRight() != null && sc.getWhenNode().getRight().getType().equalsIgnoreCase(Node.getColRefType())){
							constraintString += " AND ("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getWhenNode().getRight().getColumn(), (0+offset)+"") +" "+ sc.getWhenNode().getOperator() +" "+//TODO REMOVE HARDCODING OF OPERATOR
									sc.getWhenNode().getLeft()+")";
						}
					constraintString += ") OR (";
					//genPositiveCondsForPred( queryBlock, n.getRight(), index) +")"
					}
					
				}
				if(selectionConds!= null &&
						selectionConds.getElseConditional() != null){
				
					//	constraintString += "("+ "O_"+GenerateCVCConstraintForNode.cvcMap(selectionConds.getElseConditional().getThenNode().getLeft(), (0+offset)+"") +" "+ "=" +" "+//TODO REMOVE HARDCODING OF OPERATOR
							//	selectionConds.getElseConditional().getThenNode().getRight()+")";
					
					constraintString += ")";
				}
				else{
					constraintString += ")";
				}
			}
			constraintString += ")";
		}
		return constraintString;
	}
	
}
