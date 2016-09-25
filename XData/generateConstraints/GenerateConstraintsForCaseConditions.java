package generateConstraints;

import java.util.HashMap;
import java.util.Vector;

import parsing.CaseCondition;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class GenerateConstraintsForCaseConditions {

	public static String getConstraintsInCaseStatement(GenerateCVC1 cvc, QueryBlockDetails qbt, CaseCondition cc, Vector<CaseCondition> caseConditonCompleted) throws Exception {
		//Constraints constraints = new Constraints();
		String constraints = "";
		int offset=0, count=0;
		boolean present = false;
		String tableNameNumber = null;
		
		
		if(! cc.getCaseCondition().equals("else")){
			
		tableNameNumber = UtilsRelatedToNode.getTableNameNo(cc.getCaseConditionNode());
				
		offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
		count = cvc.getNoOfTuples().get(tableNameNumber);
		
		/** Get the index of this subquery node*/

		constraints +="ASSERT (";
		constraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt, cc.getCaseConditionNode(), 0+offset);
		if(caseConditonCompleted == null || (caseConditonCompleted != null && caseConditonCompleted.size()==0)){
			
			constraints +=");";
		}
		for(int i = 0 ; i < caseConditonCompleted.size();i++){
			CaseCondition ccCompleted = caseConditonCompleted.get(i);
			//constraints +="ASSERT (";
			constraints +=") AND ( ";
			//Generate constraint that holds these conditions as false and current condition alone as true
			constraints += GenerateCVCConstraintForNode.genNegativeCondsForPred(qbt, ccCompleted.getCaseConditionNode(), 0+offset);
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
				tableNameNumber = UtilsRelatedToNode.getTableNameNo(ccForTableInfo.getCaseConditionNode());
			}
			
			offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
			count = cvc.getNoOfTuples().get(tableNameNumber);
			//Generate negative condition for all positive conditions in CASE stmnt
			for(int i = 0 ; i < caseConditonCompleted.size();i++){
				CaseCondition ccCompleted = caseConditonCompleted.get(i);
				constraints +="ASSERT (";
				//constraints +=") AND ( ";
				//Generate constraint that holds these conditions as false and current condition alone as true
				constraints += GenerateCVCConstraintForNode.genNegativeCondsForPred(qbt, ccCompleted.getCaseConditionNode(), 0+offset);
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
		HashMap<Integer,Vector<CaseCondition>> ccMap = cvc.getqParser().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
				Vector<CaseCondition> selectionConds = ccMap.get(2);
				for(int i=0; i < (selectionConds.size()-1); i++){
					
					CaseCondition sc = selectionConds.get(i);
					constraintString += getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted);
					
					caseConditionCompleted.add(sc);
					constraintString += ") OR (";
				}
				if(selectionConds!= null 
						&& selectionConds.size() >= 1
						&&  selectionConds.get(selectionConds.size()-1) != null
						&& selectionConds.get(selectionConds.size()-1).getCaseCondition().equals("else")){
					CaseCondition sc = selectionConds.get(selectionConds.size()-1);
					constraintString += getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted);
					constraintString += ")";
				}
				else{
					constraintString += ")";
				}
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
		HashMap<Integer,Vector<CaseCondition>> ccMap = cvc.getqParser().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		int offset = 0;
		int count=0;
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
				Vector<CaseCondition> selectionConds = ccMap.get(2);
				for(int i=0; i < (selectionConds.size()-1); i++){
					
					CaseCondition sc = selectionConds.get(i);
					String tableNameNumber = UtilsRelatedToNode.getTableNameNo(sc.getCaseConditionNode());			
					offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
					count = cvc.getNoOfTuples().get(tableNameNumber);
					constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc.getOuterBlock(), sc.getCaseConditionNode(), 0+offset);
					if(sc.getColValueForConjunct() != null){
					constraintString += " AND ("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getColValueForConjunct(), (0+offset)+"") +" "+ sc.getCaseOperator() +" "+//TODO REMOVE HARDCODING OF OPERATOR
								sc.getConstantValue()+")";
					constraintString += ") OR (";
					//genPositiveCondsForPred( queryBlock, n.getRight(), index) +")"
					}
					
				}
				if(selectionConds!= null 
						&& selectionConds.size() >= 1
						&&  selectionConds.get(selectionConds.size()-1) != null
						&& selectionConds.get(selectionConds.size()-1).getCaseCondition().equals("else")){
					CaseCondition sc = selectionConds.get(selectionConds.size()-1);
					//constraintString += getConstraintsInCaseStatement(cvc,sc,caseConditionCompleted);
					if(sc.getColValueForConjunct() != null){
						constraintString += "("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getColValueForConjunct(), (0+offset)+"") +" "+ "=" +" "+//TODO REMOVE HARDCODING OF OPERATOR
								sc.getConstantValue()+")";
					}
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
