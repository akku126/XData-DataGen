package generateConstraints;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import parsing.Column;
import parsing.ConjunctQueryStructure;
import parsing.DisjunctQueryStructure;
import parsing.Node;
import parsing.Table;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

/**
 * This method is used to get positive constraints for the given conjunct of the query block
 * This class contains different methods to generate constraints by considering only specific conditions of the given conjunct
 * @author mahesh
 *
 */

public class GenerateConstraintsForConjunct {

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the selection conditions of the form A.x = Constant of this conjunct */
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k< selectionConds.size(); k++){

			//String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			
			if(tableNo == null || tableNo.isEmpty()){
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */
			for(int l=1;l<=count;l++)
				constraintString += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1)+";" +"\n";
		}



		constraintString += "\n%---------------------------------\n% ALL CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the non equi-join conditions */
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++)
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds) +"\n";



		constraintString += "\n%---------------------------------\n% STRING SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/**get the constraints for the conditions of the form A.x=cons where cons is a string constant */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(stringSelectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k));
			}
			else if((stringSelectionConds.get(k).getLeft() != null && 
					stringSelectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getLeft());
				}
			else if(stringSelectionConds.get(k).getRight() != null && 
					stringSelectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getRight());
			}
				else{
				tableNo =  stringSelectionConds.get(k).getLeft().getTableNameNo();
			}
			if(tableNo == null || tableNo.isEmpty()){
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
		}


		constraintString += "\n%---------------------------------\n% LIKE CLAUSE CONSTRAINTS\n%---------------------------------\n";

		/** Generate constraints for the like conditions of this conjunct*/
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+";\n" );
		}


		constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, queryBlock, conjunct);
		constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";

		Constraints finalConstraints=new Constraints();
		finalConstraints.constraints.add("");
		finalConstraints.stringConstraints.add("");
		for(DisjunctQueryStructure disjunct:conjunct.disjuncts){
			Constraints constraints=GenerateConstraintsForDisjunct.getConstraintsForDisjuct(cvc, queryBlock, disjunct);
			finalConstraints=Constraints.mergeConstraints(finalConstraints,constraints);
		}
		if(!conjunct.disjuncts.isEmpty()){
			constraintString+="\n"+Constraints.getConstraint(finalConstraints) + "\n";
			cvc.getStringConstraints().add(Constraints.getStringConstraints(finalConstraints));
		}
		
		return constraintString;
	}

	public static Constraints getConstraintsInConjuct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {
		Constraints constraints= new Constraints();
		String constraintString="";
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins1(cvc, queryBlock, n1,n2);
			}
		}
		
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k< selectionConds.size(); k++){

			String tableNo = "";
			if(selectionConds != null && selectionConds.size() > 0 
					&& selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if(selectionConds != null && selectionConds.size() > 0 
					&& (selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds != null && selectionConds.size() > 0 
					&& selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			if(tableNo == null || tableNo.isEmpty()){
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */
			for(int l=1;l<=count;l++)
				constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1) +" AND ";
		}
		
		//FOR CASE CONDITION IN WHERE CLAUSE - ADD CONSTRAINTS APPENDING AND HERE
				if(cvc.getqStructure().getCaseConditionMap() != null && !cvc.getqStructure().getCaseConditionMap().isEmpty()){
					/*if(!constraints.constraints.isEmpty()){
						constraintString = constraints.constraints.toString();
						if(!constraintString.equalsIgnoreCase("") && constraintString.length() > 7)
							constraintString = constraintString.substring(0,constraintString.length()-4);
					}*/
					constraintString += (GenerateConstraintsForCaseConditions.getCaseConditionConstraintsForOriginalQuery(cvc,cvc.getOuterBlock()));
					constraintString += " AND ";
				}
				
				 
		//End OF CASE CONDITION		
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++) {
			String nonEquiJoinConstraint = GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds);
			
			if(nonEquiJoinConstraint.startsWith("ASSERT")) {
				//Changed by shree for non-equi join conditions in selection clause
				//if(constraintString != null && constraintString.length()>6 && constraintString.substring(constraintString.length()-5,constraintString.length()).equalsIgnoreCase(" AND ")){
					nonEquiJoinConstraint= nonEquiJoinConstraint.substring(7,nonEquiJoinConstraint.length()-2);
				   
				/*}else{
					 nonEquiJoinConstraint = nonEquiJoinConstraint.replace("ASSERT"," AND ");
				}
			}
			
			if(nonEquiJoinConstraint.contains(";")){
				nonEquiJoinConstraint = nonEquiJoinConstraint.replace(";", " ");
			}
			if(nonEquiJoinConstraint.contains("ASSERT")){
				
					nonEquiJoinConstraint = nonEquiJoinConstraint.replace("ASSERT"," AND");*/
			}
			constraintString += nonEquiJoinConstraint +" AND ";
		}
		
		if(!constraintString.equalsIgnoreCase("")){
			constraintString=constraintString.substring(0,constraintString.length()-5);
		}
		constraints.constraints.add(constraintString);
		
		String stringConstraint="";
		
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			String tableNo = "";
			if(stringSelectionConds != null && stringSelectionConds.size() > 0 
					&& stringSelectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k));
			}
			else if(stringSelectionConds != null && stringSelectionConds.size() > 0 
					&& (stringSelectionConds.get(k).getLeft() != null && 
							stringSelectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getLeft());
				}
			else if(stringSelectionConds != null && stringSelectionConds.size() > 0 
					&& stringSelectionConds.get(k).getRight() != null && 
							stringSelectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getRight());
			}
				else{
				tableNo =  stringSelectionConds.get(k).getLeft().getTableNameNo();
			}
			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++)
				stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1) +" AND ";
		}
		
		
	
		
				
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++)
				stringConstraint+= GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+" AND ";
		}
		
		if(!stringConstraint.equalsIgnoreCase("")){
			stringConstraint = stringConstraint.substring(0, stringConstraint.length()-5);
		}
		constraints.stringConstraints.add(stringConstraint);
		
		for(DisjunctQueryStructure disjunct:conjunct.disjuncts){
			constraints = Constraints.mergeConstraints(constraints,GenerateConstraintsForDisjunct.getConstraintsForDisjuct(cvc, queryBlock, disjunct));
		}
		
		return constraints;
	}

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct, except Non equi-join conds
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuctExceptNonEquiJoins(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the selection conditions of the form A.x = Constant of this conjunct */
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k<selectionConds.size(); k++){

			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			//String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				constraintString += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1)+";" +"\n";
		}



		constraintString += "\n%---------------------------------\n% STRING SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/**get the constraints for the conditions of the form A.x=cons where cons is a string constant */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
		}


		constraintString += "\n%---------------------------------\n% LIKE CLAUSE CONSTRAINTS\n%---------------------------------\n";

		/** Generate constraints for the like conditions of this conjunct*/
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+";\n" );
		}


		constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, queryBlock, conjunct);
		constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";


		return constraintString;
	}

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct, without including constraints for the selection conditions
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuctExceptSelectionConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% ALL CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the non equi-join conditions */
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++)
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds) +"\n";



		constraintString += "\n%---------------------------------\n% STRING SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/**get the constraints for the conditions of the form A.x=cons where cons is a string constant */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(stringSelectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k));
			}
			else if((stringSelectionConds.get(k).getLeft() != null && 
					stringSelectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getLeft());
				}
			else if(stringSelectionConds.get(k).getRight() != null && 
					stringSelectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getRight());
			}
				else{
				tableNo =  stringSelectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
		}


		constraintString += "\n%---------------------------------\n% LIKE CLAUSE CONSTRAINTS\n%---------------------------------\n";

		/** Generate constraints for the like conditions of this conjunct*/
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+";\n" );
		}


		constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, queryBlock, conjunct);
		constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";


		return constraintString;
	}

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct, without including constraints for string selection conditions
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuctExceptStringSelectionConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the selection conditions of the form A.x = Constant of this conjunct */
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k<selectionConds.size(); k++){

			//String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				constraintString += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1)+";" +"\n";
		}



		constraintString += "\n%---------------------------------\n% ALL CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the non equi-join conditions */
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++)
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds) +"\n";



		constraintString += "\n%---------------------------------\n% LIKE CLAUSE CONSTRAINTS\n%---------------------------------\n";

		/** Generate constraints for the like conditions of this conjunct*/
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+";\n" );
		}


		constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, queryBlock, conjunct);
		constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";


		return constraintString;
	}

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct, except like conditions
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuctExceptLikeConds(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the selection conditions of the form A.x = Constant of this conjunct */
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k<selectionConds.size(); k++){

			//String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				constraintString += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1)+";" +"\n";
		}



		constraintString += "\n%---------------------------------\n% ALL CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the non equi-join conditions */
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++)
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds) +"\n";



		constraintString += "\n%---------------------------------\n% STRING SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/**get the constraints for the conditions of the form A.x=cons where cons is a string constant */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
		}


		constraintString += "\n%---------------------------------\n% WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";
		constraintString += GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereClauseSubQueryBlock(cvc, queryBlock, conjunct);
		constraintString += "\n%---------------------------------\n% END OF WHERE CLAUSE SUBQUERY BLOCK CONSTRAINTS\n%---------------------------------\n";


		return constraintString;
	}

	/**
	 * This method generates constraints by considering all the conditions of the given conjunct, except where clause subquery constraints
	 * @param cvc
	 * @param queryBlock
	 * @return
	 * @throws Exception
	 */
	public static String getConstraintsForConjuctExceptWhereClauseSubQueryBlock(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		String constraintString = "";

		/** If the given conjunct is null then no constraints need to be generated */
		if(conjunct == null)
			return constraintString;

		constraintString += "\n%---------------------------------\n% EQUIVALENCE CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the equivalence class constraints for this conjunct*/
		Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString += GenerateJoinPredicateConstraints.getConstraintsForEquiJoins(cvc, queryBlock, n1,n2) +"\n";
			}
		}


		constraintString += "\n%---------------------------------\n% SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the selection conditions of the form A.x = Constant of this conjunct */
		Vector<Node> selectionConds = conjunct.getSelectionConds();
		for(int k=0; k<selectionConds.size(); k++){

			//String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(selectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k));
			}
			else if((selectionConds.get(k).getLeft() != null && 
					selectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getLeft());
				}
			else if(selectionConds.get(k).getRight() != null && 
					selectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(selectionConds.get(k).getRight());
			}
				else{
				tableNo =  selectionConds.get(k).getLeft().getTableNameNo();
			}
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				constraintString += "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1)+";" +"\n";
		}



		constraintString += "\n%---------------------------------\n% ALL CLASS CONSTRAINTS\n%---------------------------------\n";

		/** Get the constraints for the non equi-join conditions */
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		for(int k=0; k<allConds.size(); k++)
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds) +"\n";



		constraintString += "\n%---------------------------------\n% STRING SELECTION CLASS CONSTRAINTS\n%---------------------------------\n";

		/**get the constraints for the conditions of the form A.x=cons where cons is a string constant */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();
		for(int k=0; k<stringSelectionConds.size(); k++){

			//String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			String tableNo = "";
			if(stringSelectionConds.get(k).getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k));
			}
			else if((stringSelectionConds.get(k).getLeft() != null && 
					stringSelectionConds.get(k).getLeft().getType().equalsIgnoreCase(Node.getBaoNodeType()))){
				
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getLeft());
				}
			else if(stringSelectionConds.get(k).getRight() != null && 
					stringSelectionConds.get(k).getRight().getType().equalsIgnoreCase(Node.getBaoNodeType())){
				tableNo = getTableNameNoForBAONode(stringSelectionConds.get(k).getRight());
			}
				else{
				tableNo =  stringSelectionConds.get(k).getLeft().getTableNameNo();
			}
			
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
		}


		constraintString += "\n%---------------------------------\n% LIKE CLAUSE CONSTRAINTS\n%---------------------------------\n";

		/** Generate constraints for the like conditions of this conjunct*/
		Vector<Node> likeConds = conjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+";\n" );
		}

		return constraintString;
	}
	
	public static String generateJoinConditionConstraintsForNotExists(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct){
		String constraintString = "";
		
		Vector<String> OrConstraints=new Vector<String>();		
		Vector<Node> joinConds = conjunct.getJoinCondsAllOther();
		
		for(Node n : joinConds){
			Node left = n.getLeft();
			Node right = n.getRight();
			
			int leftTuples = cvc.getNoOfOutputTuples().get(left.getTable().getTableName());
			int rightTuples = cvc.getNoOfOutputTuples().get(right.getTable().getTableName());
			OrConstraints.add(GenerateJoinPredicateConstraints.genNegativeCondsEqClassForAllTuplePairs(cvc, queryBlock, right, left, rightTuples, leftTuples));
		}
		
		for(String s: OrConstraints){
			constraintString += "(" + s + ") OR ";
		}
		
		constraintString = constraintString.substring(0, constraintString.length()-3);
		
		return constraintString.trim();
	}
	
	public static String generateConstraintsNotExists(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct, String relation) throws Exception {
		
		String constraintString = "";
		
		Vector<String> andConstraints = new Vector<String>();
		Vector<String> OrConstraints=new Vector<String>();
		Vector<String> OrStringConstraints = new Vector<String>();
		
		for(int k = 1; k <= cvc.getNoOfOutputTuples().get(relation); k++){
			//System.out.print(k);
			
			/** Generate negative constraint for correlation condition.*/
			if(!conjunct.joinCondsAllOther.isEmpty()){

				/**Get the correlation variables*/
				Vector<Node> joinConds = conjunct.getJoinCondsAllOther();

				for(Node n : joinConds){

					Node node = null;
					Node other = null;
					Node left = n.getLeft();
					Node right = n.getRight();
					
					if(left.getTable().getTableName().endsWith(relation)){
						node = left;
						other = right;
					} else if(right.getTable().getTableName().endsWith(relation)){
						node = right;
						other = left;
					}

					if(node != null) {
						OrConstraints.add(GenerateJoinPredicateConstraints.genNegativeCondsEqClass(cvc, queryBlock, other, node, k));
					}					
				}
				
				joinConds = conjunct.getJoinCondsForEquivalenceClasses();

				for(Node n : joinConds){

					Node node = null;
					Node other = null;
					Node left = n.getLeft();
					Node right = n.getRight();
					
					if(left.getTable().getTableName().endsWith(relation)){
						node = left;
						other = right;
					} else if(right.getTable().getTableName().endsWith(relation)){
						node = right;
						other = left;
					}

					if(node != null) {
						OrConstraints.add(GenerateJoinPredicateConstraints.genNegativeCondsEqClass(cvc, queryBlock, other, node, k));
					}					
				}
				
			}
			
			/** Now generate Negative constraints for selection conditions */
			Vector<Node> selectionConds = conjunct.getSelectionConds();
			
			Vector<Node> tempSelectionConds = new Vector<Node>();
			for(Node n: selectionConds){
				if(n.getLeft().getTable().getTableName().equals(relation)){
					tempSelectionConds.add(n);
				}
			}

			/**get negative conditions for these nodes*/
			Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(tempSelectionConds);

			/**Generate constraints for the negative conditions*/
			for(int i = 0; i < negativeSelConds.size(); i++){

				/**get table details*/
				String tableNo = negativeSelConds.get(i).getLeft().getTableNameNo();

				OrConstraints.add(GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(i), k));
			}
			
			/**Generate negative constraints for string selection conditions */
			Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();	

			/**get negative conditions for these nodes*/
			Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);

			/**Generate constraints for the negative conditions*/
			for(int i = 0; i < negativeStringSelConds.size(); i++){

				/**get table details*/
				String tableNo = negativeStringSelConds.get(i).getLeft().getTableNameNo();

				OrStringConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeStringSelConds.get(i),k)+";" );
			}


			/**Generate negative constraints for like conditions */
			Vector<Node> likeConds = conjunct.getLikeConds();

			/**get negative conditions for these nodes*/
			Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);

			for(int i = 0; i<likeConds.size(); i++){

				/**get table details*/
				String tableNo = negativeLikeConds.get(i).getLeft().getTableNameNo();
				OrStringConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeLikeConds.get(i), k)+";");
			}
			
			if(!OrStringConstraints.isEmpty()) {
				Vector<String> tempVector = cvc.getStringSolver().solveOrConstraints( new Vector<String>(OrStringConstraints), cvc.getResultsetColumns(), cvc.getTableMap());		
				
				for(int i = 0; i < tempVector.size(); i++){
					String cond = tempVector.get(i).trim();
					cond = cond.replace("ASSERT", "");
					cond = cond.replace("\n", "");
					cond = cond.replace(";", "");
					cond = cond.trim();
					OrConstraints.add(cond);
				}
				
				OrStringConstraints.clear();					
			}
			
			if(!OrConstraints.isEmpty() && OrConstraints.size() != 0){
				andConstraints.add(processOrConstraintsNotExistsWithoutAssert(OrConstraints));
				OrConstraints.clear();
			}

		}

		return processAndConstraintsNotExists(andConstraints);
	}
	
	/**
	 * This method is used to get negative constraints for the given conjunct of the query block
	 * @param cvc
	 * @param queryBlock
	 * @param conjunct
	 * @return
	 * @throws Exception
	 */
	public static String generateNegativeConstraintsConjunct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception {

		
		String constraintString = "";

		constraintString += "%--------------------------------\n%NEGATIVE CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------------\n";
		
		Vector<String> OrConstraints=new Vector<String>();
		Vector<String> OrStringConstraints = new Vector<String>();

	
		/** Generate negative constraint for equivalence class.*/
		/**FIXME: Mahesh..Ask Amol why the below code and why cant't we use getconstraintsforequijoins()*/
		if(!conjunct.getEquivalenceClasses().isEmpty()){

			/**Get the equivalence classes*/
			Vector<Vector<Node>> equivalenceClasses = conjunct.getEquivalenceClasses();

			for(int i=0; i<equivalenceClasses.size(); i++){	/** For each equivalence class list*/

				/**Get this equivalence */
				Vector<Node> ec = equivalenceClasses.get(i);

				/**for each node in this equivalence*/
				for(int j=0;j<ec.size(); j++)
				{

					Node eceNulled = ec.get(j);			/** This is R.a - to be nulled */


					String CVCStr = "%DataSet Generated By Nulling: "+ eceNulled.toString() + "\n";
					Table tableNulled = eceNulled.getTable();
					Column colNulled = eceNulled.getColumn();

					/** TODO: Have one vector for positive and negative conditions. */

					cvc.setResultsetTableColumns1( new HashMap<Table, Vector<String>>() );					

					/** S = set of elements in ec which have a foreign key relationship with R.a <use closure for this>  */
					ArrayList<Node> S = new ArrayList<Node>();
					Vector<Node> nullableFKs = new Vector<Node>();
					S.add(eceNulled);

					for(int k=0; k<ec.size(); k++)
					{
						Node ece = ec.get(k);
						Table tableEce = ece.getTable();
						Column colEce = ece.getColumn();
						/**TODO Maintain a datasructure for list of PK and FK so as to compare in one pass.*/
						for(int l=0; l < cvc.getForeignKeys().size(); l++)
						{
							/**In ForeignKeys Left points to the foreign key attribute while the right 
							 * points to the Primary or the referenced column*/
							Node fk = cvc.getForeignKeys().get(l);

							/**Adapted for Nullable foreign keys.
							 **If FK is not nullable, then we need to null it along with the referenced 
							 *relation which is eceNulled**/
							if( (fk.getLeft().getTable() == tableEce && fk.getLeft().getColumn() == colEce) 
									&& (fk.getRight().getTable() == tableNulled && fk.getRight().getColumn() == colNulled)){
								/**Shree changed it for : 
								*If FK is not nullable, check whether the other 
								*column in the relation is nullable - if either is nullable, add it**/
								if(!colEce.isNullable() && colNulled.isNullable())
									S.add(ece);/**To be taken along with nulled Column*/
								if(colEce.isNullable())
									nullableFKs.add(ece);/** To be taken along with nulled column, in case P.size > 1 */								
							}
						}
					}
					/**
					 * Now, we have in S, the nulled column along with the foreign key columns in that equivalence class
					 * that reference the nulled column and are not nullable.
					 * But, if EC - S, contains a nullable column which references the nulled column, 
					 * AND also contains some other column which may or may not be nullable, 
					 * then we cannot assign NULL to the nullable FK column. We then need to nullify the nullable FK column
					 * along with the nulled column. If EC - S, just contains a single nullable column referencing the 
					 * nulled column, then we can assign NULL to that column in order to nullify the nulled column.   
					 */


					/** Form P = EC - S */
					Vector<Node> P = new Vector<Node>();
					for(int k=0; k<ec.size(); k++)						
						if( cvc.getqStructure().alreadyNotExistInEquivalenceClass(S, ec.get(k)))
							P.add(ec.get(k));


					/**For Now : if P is empty continue;*/
					if(P.size() == 0)
						continue;

					if(P.size() == 1 
							&& P.get(0).getColumn().getReferenceColumn() == eceNulled.getColumn()
							&& P.get(0).getColumn().isNullable()){

						/**check if the column in P is referencing the nulled column and is nullable.
						 *If so, then we can nullify the nulled relation inspite of the FK*/
						OrConstraints.add( GenerateCVCConstraintForNode.cvcSetNull(cvc, P.get(0).getColumn(), "1"));
					}
					else{
						/**Otherwise, we need to nullify the foreign keys along with the nulled column.
						 * Note that we need to do this irrespective of whether the column in P is FK 
						 * or whether it is nullable. Because, then we cannot assign NULL to any of the columns in P
						 * This is because, NULLs cannot be equated and hence the joins in P will not be propogated up.
						 */
						P.removeAll(nullableFKs);

						/** Generate positiveConds for members in P*/ 
						GenerateJoinPredicateConstraints.genPositiveConds(P);
				 	} 

					/**Now generate negative conditions for Nulled relation
					 *i.e. NOT EXISTS (i: Nulled Rel): NulledRel[i].col = P[1].col*/
					
					/**Shree changed it for the following reason:
					//If Not Exists holds any one primary key relation as a condition
					//Ex:Not Exists(takes.year=section.year and section.year='2009')
					//In this example, P will hold 2 values and reverse constraint is to be added for
					//(O_SECTION[1].3 /= O_TAKES[1].4)  OR  (O_TAKES[1].4 /= O_SECTION[1].3) */
					
					if(P!= null && P.size() == 1){
						OrConstraints.add( GenerateJoinPredicateConstraints.genNegativeConds( cvc, queryBlock, colNulled, P.get(0)));
					}
					else if(P != null && P.size() > 1){
						for(int k=0;k<P.size(); k++){
							OrConstraints.add( GenerateJoinPredicateConstraints.genNegativeConds( cvc, queryBlock, colNulled, P.get(k)));
						}
					} 
					
					
					
				}
			}
		}



		/**Now generate Positive conditions for each of the non equi join conditions 
		 * that were not considered when building equivalence classes*/
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		/**get constraint*/
		String constraint = GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, queryBlock, allConds) ;

		if( constraint.length() != 0)
			OrConstraints.add( constraint );

		/** Now generate Negative constraints for selection conditions */
		Vector<Node> selectionConds = conjunct.getSelectionConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selectionConds);

		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++)
				OrConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(k),l+offset-1)+";" +"\n" );
		}



		/**Generate negative constraints for string selection conditions */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();	

		/**get negative conditions for these nodes*/
		Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);

		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeStringSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeStringSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++)
				OrStringConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeStringSelConds.get(k),l+offset-1)+";" +"\n" );
		}


		/**Generate negative constraints for like conditions */
		Vector<Node> likeConds = conjunct.getLikeConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);

		for(int k=0; k<likeConds.size(); k++){

			/**get table details*/
			String tableNo = negativeLikeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				OrStringConstraints.add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeLikeConds.get(k),l+offset-1)+";" +"\n" );
		}


		/**get the where clause sub query conditions in this conjunct*/
		if(conjunct.getAllSubQueryConds() != null){
			for(int i=0; i < conjunct.getAllSubQueryConds().size(); i++){
				
				Node subQ = conjunct.getAllSubQueryConds().get(i);
				
				/**FIXME: Add negative constraints for this where clause sub query block
				 * We could use methods of class: GenerateConstraintsForWhereClauseSubQueryBlock*/
				/**FIXME:If the given conjunct has NOT EXISTS conditions, then negative of that becomes positive*/
				
				if(queryBlock.getWhereClauseSubQueries() != null
						&& ! queryBlock.getWhereClauseSubQueries().isEmpty()){
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

				/**get sub query block*/
				QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);
				
				String negativeConstraint = "";
				
				/**if this sub query is of EXISTS Type*/
				if(subQ.getType().equals(Node.getExistsNodeType()) ){
					
					for (ConjunctQueryStructure con: subQuery.getConjunctsQs())
						negativeConstraint += generateNegativeConstraintsConjunct(cvc, subQuery, con);
				}
				
				/**if sub query is of type NOT Exists*/
				/**We need to get positive constraints for this sub query*/
				else if (  subQ.getType().equals(Node.getNotExistsNodeType() ) ){
					
					for (ConjunctQueryStructure con: subQuery.getConjunctsQs())
						negativeConstraint += getConstraintsForConjuct(cvc, queryBlock, con);
				}
				else{
					
					/**get negative condition for this sub query node*/
					Node subQNegative = GenerateCVCConstraintForNode.getNegativeCondition(subQ);
					
					/**get negative constraints for where clause connective*/					
					negativeConstraint = GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereSubQueryConnective(cvc, queryBlock, subQNegative);
				}
				
				constraintString += negativeConstraint;
			}
		}
		
		}
		
		
		
		
		if(!OrConstraints.isEmpty() && OrConstraints.size() != 0)
			constraintString += processOrConstraints(OrConstraints);
		constraintString += "\n%--------------------------------\n%END OF NEGATIVE CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------------";

		if(!OrStringConstraints.isEmpty() && OrStringConstraints.size() != 0 )
			cvc.getStringConstraints().add(processOrConstraints(OrStringConstraints));

		return constraintString;
	}

	public static Constraints generateNegativeConstraintsForConjunct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, ConjunctQueryStructure conjunct) throws Exception{
		Constraints constraints=new Constraints();
		
		String constraintString = "";

		//constraintString += "%--------------------------------\n%NEGATIVE CONSTRAINTS FOR THIS CONJUNCT\n%---------------------------------------\n";
		
		Vector<String> OrConstraints=new Vector<String>();
		Vector<String> OrStringConstraints = new Vector<String>();

		/**Now generate Positive conditions for each of the non equi join conditions 
		 * that were not considered when building equivalence classes*/
		//Vector<Node> allConds = conjunct.getAllConds();
		Vector<Node> allConds = conjunct.getJoinCondsAllOther();
		/**get constraint*/
		String constraint = GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, queryBlock, allConds) ;

		constraint=UtilRelatedToConstraints.removeAssert(constraint);
		
		if(!constraint.equalsIgnoreCase("")){
			constraints.constraints.add(constraint);
		}

		/** Now generate Negative constraints for selection conditions */
		Vector<Node> selectionConds = conjunct.getSelectionConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selectionConds);
		
		constraint="";
		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++){
				constraint+=GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(k),l+offset-1) + " AND ";
			}
			
			if(!constraint.equalsIgnoreCase("")){
				constraint=constraint.substring(0, constraint.length()-5);
				constraints.constraints.add(constraint);
			}
			
			constraint="";
		}
		
		/**Generate negative constraints for string selection conditions */
		Vector<Node> stringSelectionConds = conjunct.getStringSelectionConds();	

		/**get negative conditions for these nodes*/
		Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);


		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeStringSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeStringSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++)
					constraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeStringSelConds.get(k),l+offset-1) + " AND ";
			if(!constraint.equalsIgnoreCase("")){
				constraint=constraint.substring(0, constraint.length()-5);
				constraints.stringConstraints.add(constraint);
			}
			constraint="";
		}
		/**Generate negative constraints for like conditions */
		Vector<Node> likeConds = conjunct.getLikeConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);

		for(int k=0; k<likeConds.size(); k++){

			/**get table details*/
			String tableNo = negativeLikeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				constraint=GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeLikeConds.get(k),l+offset-1) +" AND ";
			
			if(!constraint.equalsIgnoreCase("")){
				constraint=constraint.substring(0, constraint.length()-5);
				constraints.stringConstraints.add(constraint);
			}

			constraint="";
		}
		

		/**get the where clause sub query conditions in this conjunct*/
		if(conjunct.getAllSubQueryConds() != null){
			for(int i=0; i < conjunct.getAllSubQueryConds().size(); i++){
				
				Node subQ = conjunct.getAllSubQueryConds().get(i);
				
				/**FIXME: Add negative constraints for this where clause sub query block
				 * We could use methods of class: GenerateConstraintsForWhereClauseSubQueryBlock*/
				/**FIXME:If the given conjunct has NOT EXISTS conditions, then negative of that becomes positive*/
				if(queryBlock.getWhereClauseSubQueries() != null
						&& !queryBlock.getWhereClauseSubQueries().isEmpty()){
				int index = UtilsRelatedToNode.getQueryIndexOfSubQNode(subQ);

				/**get sub query block*/
				QueryBlockDetails subQuery = queryBlock.getWhereClauseSubQueries().get(index);
				
				String negativeConstraint = "";
				
				/**if this sub query is of EXISTS Type*/
				if(subQ.getType().equals(Node.getExistsNodeType()) ){
					
					for (ConjunctQueryStructure con: subQuery.getConjunctsQs())
						negativeConstraint += generateNegativeConstraintsConjunct(cvc, subQuery, con);
				}
				
				/**if sub query is of type NOT Exists*/
				/**We need to get positive constraints for this sub query*/
				else if (  subQ.getType().equals(Node.getNotExistsNodeType() ) ){
					
					for (ConjunctQueryStructure con: subQuery.getConjunctsQs())
						negativeConstraint += getConstraintsForConjuct(cvc, queryBlock, con);
				}
				else{
					
					/**get negative condition for this sub query node*/
					Node subQNegative = GenerateCVCConstraintForNode.getNegativeCondition(subQ);
					
					/**get negative constraints for where clause connective*/					
					negativeConstraint = GenerateConstraintsForWhereClauseSubQueryBlock.getConstraintsForWhereSubQueryConnective(cvc, queryBlock, subQNegative);
				}
				
				constraintString += negativeConstraint;
				UtilRelatedToConstraints.removeAssert(constraintString);
				
				if(!constraintString.equalsIgnoreCase("")){
					constraints.constraints.add(constraintString);
				}
				constraintString= "";
			}
		}}

		return constraints;
	}
	
	public static String processAndConstraintsNotExists(Vector<String> andConstraints){

		String str = "";

		for(String constraint: andConstraints)
			if(constraint.length() != 0) {
				str += "(" + constraint + ") AND ";
			}

		str = str.substring(0,str.length() - 4);

		return str.trim();
	}
	
	public static String processOrConstraintsNotExistsWithoutAssert(Vector<String> OrConstraints){

		String str = "";

		for(String constraint: OrConstraints)
			if(constraint.length() != 0) {
				str += "(" + constraint + ") OR ";
			}

		str = str.substring(0,str.length()-4);

		return str.trim();
	}
	
	public static String processOrConstraintsWithoutAssertNotExists(Vector<String> OrConstraints){

		String str = "ASSERT ";

		for(String constraint: OrConstraints)
			if( constraint.length() != 0) {
				str += "(" + constraint.trim() + ") OR ";
			}

		str = str.substring(0,str.length()-4);
		str+=";";

		return str;
	}
	
	public static String processOrConstraintsNotExists(Vector<String> OrConstraints){

		String str = "ASSERT ";

		for(String constraint: OrConstraints)
			if( constraint.length() != 0) {
				int index = constraint.indexOf(";");
				String temp = constraint.substring(6, index);
				str += "(" + temp.trim() + ") OR ";
			}

		str = str.substring(0,str.length()-4);
		str+=";";

		return str;
	}
	

	public static String processOrConstraints(Vector<String> OrConstraints){

		String str = "ASSERT ";

		/**If any of these conditions is violated then its ok*/
		for(String constraint: OrConstraints)
			if( constraint.length() != 0)
				
				str += constraint.trim().substring(6,constraint.trim().length()-1)+" OR ";

		str = str.substring(0,str.length()-4);
		str+=";";

		return str;
	}
	
	/**
	 * Get the table name number from the BAO node. It contains expression with a column
	 * So traverse and find the table name number of the column on which expression is given 
	 * 
	 * @param Node - BAONode
	 * @return
	 */
	public static String getTableNameNoForBAONode(Node n1) {
		
		if(n1.getRight() != null && n1.getRight().getTableNameNo() != null){
			return n1.getRight().getTableNameNo();
		}	
		else if(n1.getLeft() != null && n1.getLeft().getTableNameNo() != null){
			return n1.getLeft().getTableNameNo();	
		}
		else {
			if(n1.getLeft() != null){
				return getTableNameNoForBAONode(n1.getLeft());
			}else if(n1.getRight() != null){
				return getTableNameNoForBAONode(n1.getRight());
			}
		}
		return null;
		
	}
}
