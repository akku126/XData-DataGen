package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.CaseCondition;
import parsing.CaseExpression;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;
import util.ConstraintObject;

public class GenerateConstraintsForCaseConditions {

	public static String getConstraintsInCaseStatement(GenerateCVC1 cvc, QueryBlockDetails qbt, CaseCondition cc, Vector<CaseCondition> caseConditonCompleted) throws Exception {
		//Constraints constraints = new Constraints();
		String constraints = "";
		int offset=0, count=0;
		boolean present = false;
		String tableNameNumber = null;
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		
		if(cc.getWhenNode() != null && cc.getThenNode() != null){
			
		tableNameNumber = UtilsRelatedToNode.getTableNameNo(cc.getThenNode());
				
		offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
		count = cvc.getNoOfTuples().get(tableNameNumber);
		
		
		/** Get the index of this subquery node*/

		//constraints +="ASSERT (";
		
		if(cc.getWhenNode().getLeft() != null && cc.getWhenNode().getLeft().getType().equalsIgnoreCase(Node.getColRefType())){
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt,cc.getWhenNode().getLeft(), 0+offset));
			constrList.add(constrObj);
			//constraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt,cc.getWhenNode().getLeft(), 0+offset);
		}else{
			//constraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(qbt,cc.getWhenNode().getRight(), 0+offset);
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(qbt,cc.getWhenNode().getRight(), 0+offset));
			constrList.add(constrObj);
		}
		
		//if(caseConditonCompleted == null || (caseConditonCompleted != null && caseConditonCompleted.size()==0)){
			
			//constraints +=");";
		//}
		for(int i = 0 ; i < caseConditonCompleted.size();i++){
			CaseCondition ccCompleted = caseConditonCompleted.get(i);
			//constraints +="ASSERT (";
			//constraints +=") AND ( ";
			//Generate constraint that holds these conditions as false and current condition alone as true
			ConstraintObject constrObj = new ConstraintObject();
			constrObj.setLeftConstraint(GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,qbt, ccCompleted.getWhenNode().getLeft(), 0+offset));
			constrList.add(constrObj);
			
			if(i == (caseConditonCompleted.size()-1)){
			//	constraints +=");";
			}else{
				//constraints += ") AND (";
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
				//constraints +="ASSERT (";
				//constraints +=") AND ( ";
				//Generate constraint that holds these conditions as false and current condition alone as true
				ConstraintObject constrObj = new ConstraintObject();
				constrObj.setLeftConstraint( GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,qbt, ccCompleted.getWhenNode(), 0+offset));
				constrList.add(constrObj);
				
				
				//constraints += GenerateCVCConstraintForNode.genNegativeCondsForPred(cvc,qbt, ccCompleted.getWhenNode(), 0+offset);
				if(i == (caseConditonCompleted.size()-1)){
					//constraints +=");";
				}else{
					//constraints += ") AND (";
				}
				
			}
		}
		constraints += constrGen.generateANDConstraintsWithAssert(constrList);
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
		
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		
		String constraintString = "";//"ASSERT (";
		HashMap<Integer,CaseExpression> ccMap = cvc.getqStructure().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
				ArrayList<CaseCondition> selectionConds = ((CaseExpression)ccMap.get(2)).getWhenConditionals();
				for(int i=0; i < selectionConds.size(); i++){
					
					CaseCondition sc = selectionConds.get(i);
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted));
					constrList.add(constrObj);
					
					caseConditionCompleted.add(sc);
					//constraintString += ") OR (";
				}
				if((((CaseExpression)ccMap.get(2)).getElseConditional()) != null){
				
					CaseCondition sc = (((CaseExpression)ccMap.get(2)).getElseConditional());
					
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted));
					constrList.add(constrObj);
					
					//constraintString += getConstraintsInCaseStatement(cvc,qbt,sc,caseConditionCompleted);
					//constraintString += ")";
				}
			}
				else{
					//constraintString += ")";
				}
			}
			
		constraintString += constrGen.generateOrConstraintsWithAssert(constrList);
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
		String constraintString = "";//" ((";//"ASSERT ((";
		HashMap<Integer,CaseExpression> ccMap = cvc.getqStructure().getCaseConditionMap();
		Vector<CaseCondition> caseConditionCompleted = new Vector<CaseCondition>();
		int offset = 0;
		int count=0;
		
		ConstraintGenerator constrGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrANDObjList = new ArrayList<ConstraintObject>();
		
		ArrayList<ConstraintObject> constrORObjList = new ArrayList<ConstraintObject>();
		
		if(ccMap != null){
			//Add constraints for CASE statements in Projected columns
			if(ccMap.containsKey(2)){
			CaseExpression selectionConds = ccMap.get(2);
				for(int i=0; i < selectionConds.getWhenConditionals().size(); i++){
					
					CaseCondition sc = selectionConds.getWhenConditionals().get(i);
					String tableNameNumber = UtilsRelatedToNode.getTableNameNo(sc.getWhenNode());			
					offset = cvc.getRepeatedRelNextTuplePos().get(tableNameNumber)[1];
					count = cvc.getNoOfTuples().get(tableNameNumber);
					//constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(cvc.getOuterBlock(), sc.getWhenNode(), 0+offset);
					
					ConstraintObject constrObj = new ConstraintObject();
					constrObj.setLeftConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(), sc.getWhenNode().getLeft(), 0+offset));
					constrObj.setRightConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(), sc.getWhenNode().getRight(), 0+offset));
					constrObj.setOperator(sc.getWhenNode().getOperator());
					constrANDObjList.add(constrObj);
					
					if(sc.getWhenNode() != null){
						//if then node is colRef 
						if(sc.getWhenNode().getLeft() != null && sc.getWhenNode().getLeft().getType().equalsIgnoreCase(Node.getColRefType())){
							
							ConstraintObject constrnObj = new ConstraintObject();
							constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(),sc.getWhenNode().getLeft(), (0+offset)));
							constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(),sc.getWhenNode().getRight(), (0+offset)));
							constrnObj.setOperator(sc.getWhenNode().getOperator());
							constrANDObjList.add(constrnObj);
							
							
							//constraintString += " AND ("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getWhenNode().getLeft().getColumn(), (0+offset)+"") +" "+ sc.getWhenNode().getOperator() +" "+//TODO REMOVE HARDCODING OF OPERATOR
								//sc.getWhenNode().getRight()+")";
							
						}else if(sc.getWhenNode().getRight() != null && sc.getWhenNode().getRight().getType().equalsIgnoreCase(Node.getColRefType())){
							
							ConstraintObject constrnObj = new ConstraintObject();
							constrnObj.setLeftConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(),sc.getWhenNode().getRight(), (0+offset)));
							constrnObj.setRightConstraint(constrGen.genPositiveCondsForPred(cvc.getOuterBlock(),sc.getWhenNode().getLeft(), (0+offset)));
							constrnObj.setOperator(sc.getWhenNode().getOperator());
							constrANDObjList.add(constrnObj);
							
							//constraintString += " AND ("+ "O_"+GenerateCVCConstraintForNode.cvcMap(sc.getWhenNode().getRight().getColumn(), (0+offset)+"") +" "+ sc.getWhenNode().getOperator() +" "+//TODO REMOVE HARDCODING OF OPERATOR
									//sc.getWhenNode().getLeft()+")";
						}
						
						constraintString = "(assert "+ constrGen.generateANDConstraints(constrANDObjList)+ " )\n";
						
						ConstraintObject conOR = new ConstraintObject();
						conOR.setLeftConstraint(constraintString);
						constrORObjList.add(conOR);
						
						
					//constraintString += ") OR (";
					//genPositiveCondsForPred( queryBlock, n.getRight(), index) +")"
					}
					constraintString =constrGen.generateOrConstraints(constrORObjList);
				}
			/*if(selectionConds!= null &&
						selectionConds.getElseConditional() != null){
				
					//	constraintString += "("+ "O_"+GenerateCVCConstraintForNode.cvcMap(selectionConds.getElseConditional().getThenNode().getLeft(), (0+offset)+"") +" "+ "=" +" "+//TODO REMOVE HARDCODING OF OPERATOR
							//	selectionConds.getElseConditional().getThenNode().getRight()+")";
					
					constraintString += ")";
				}
				else{
					constraintString += ")";
				}*/
			}
			//constraintString += ")";
		}
		return constraintString;
	}
	
}
