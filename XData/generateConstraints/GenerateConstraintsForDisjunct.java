package generateConstraints;

import java.util.Vector;

import parsing.ConjunctQueryStructure;
import parsing.Disjunct;
import parsing.DisjunctQueryStructure;
import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.QueryBlockDetails;

public class GenerateConstraintsForDisjunct {

	public static Constraints getConstraintsForDisjuct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, DisjunctQueryStructure disjunct) throws Exception{
		Constraints constraints=new Constraints();
		//Vector<String> constraints=new Vector<String>();
		String constraintString = "";
		Vector<String> temp=new Vector<String>();
		Vector<Vector<Node>> equivalenceClasses = disjunct.getEquivalenceClasses();
		for(int k=0; k<equivalenceClasses.size(); k++){
			Vector<Node> ec = equivalenceClasses.get(k);
			for(int i=0;i<ec.size()-1;i++){
				Node n1 = ec.get(i);
				Node n2 = ec.get(i+1);
				constraintString = GenerateJoinPredicateConstraints.getConstraintsForEquiJoins1(cvc, queryBlock, n1,n2);
				constraintString = constraintString.substring(0, constraintString.length()-5);
				constraints.constraints.add(constraintString);
				constraintString = "";
			}
		}
		
		Vector<Node> selectionConds = disjunct.getSelectionConds();
		for(int k=0; k< selectionConds.size(); k++){

			String tableNo = selectionConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */
			for(int l=1;l<=count;l++) {
				constraintString += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, selectionConds.get(k),l+offset-1) +" AND ";
			}
			constraintString = constraintString.substring(0, constraintString.length()-5);
			constraints.constraints.add(constraintString);
			constraintString = "";
		}
		
		Vector<Node> allConds = disjunct.getAllConds();
		for(int k=0; k<allConds.size(); k++){
			constraintString += GenerateJoinPredicateConstraints.getConstraintsForNonEquiJoins(cvc, queryBlock, allConds);
			constraints.constraints.add(constraintString);
			constraintString = "";
		}
		
		Vector<Node> stringSelectionConds = disjunct.getStringSelectionConds();
		String stringConstraints="";
		for(int k=0; k<stringSelectionConds.size(); k++){

			String tableNo = stringSelectionConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++){
				//cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1)+";" +"\n" );
				stringConstraints += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, stringSelectionConds.get(k),l+offset-1) + " AND ";
			}
			if(stringConstraints.endsWith(" AND ")){
				stringConstraints = stringConstraints.substring(0, stringConstraints.length()-5);
			}
			constraints.stringConstraints.add(stringConstraints);
			stringConstraints = "";
		}
		
		Vector<Node> likeConds = disjunct.getLikeConds();
		for(int k=0; k<likeConds.size(); k++){

			String tableNo = likeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo) * queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;
			for(int l=1;l<=count;l++){
				//cvc.getStringConstraints().add( "ASSERT " + GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+" AND " );
				stringConstraints+= GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, likeConds.get(k),l+offset-1)+" AND ";
			}
			stringConstraints = stringConstraints.substring(0, stringConstraints.length()-5);
			constraints.stringConstraints.add(stringConstraints);
			stringConstraints = "";	
		}
		
		for(ConjunctQueryStructure conjunct:disjunct.conjuncts){
			constraints=Constraints.orConstraints(constraints,GenerateConstraintsForConjunct.getConstraintsInConjuct(cvc, queryBlock, conjunct));
		}
		
		return constraints;
	}
		
	public Constraints generateNegativeConstraintsForDisjunct(GenerateCVC1 cvc, QueryBlockDetails queryBlock, Disjunct disjunct) throws Exception{
		Constraints constraints=new Constraints();
		/**Now generate Positive conditions for each of the non equi join conditions 
		 * that were not considered when building equivalence classes*/
		Vector<Node> allConds = disjunct.getAllConds();
		
		/**get constraint*/
		String constraint = GenerateJoinPredicateConstraints.getNegativeConstraintsForNonEquiJoins(cvc, queryBlock, allConds) ;

		
		constraint=UtilRelatedToConstraints.removeAssert(constraint);
		
		if(!constraint.equalsIgnoreCase("")){
			constraint+=" AND ";
		}

		/** Now generate Negative constraints for selection conditions */
		Vector<Node> selectionConds = disjunct.getSelectionConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeSelConds = GenerateCVCConstraintForNode.getNegativeConditions(selectionConds);

		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++)
				 constraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeSelConds.get(k),l+offset-1) + " AND ";
		}

		if(!constraint.equalsIgnoreCase("")){
			constraint=constraint.substring(0, constraint.length()-5);
		}
		
		constraints.constraints.add(constraint);
		String stringConstraint="";

		/**Generate negative constraints for string selection conditions */
		Vector<Node> stringSelectionConds = disjunct.getStringSelectionConds();	

		/**get negative conditions for these nodes*/
		Vector<Node> negativeStringSelConds = GenerateCVCConstraintForNode.getNegativeConditions(stringSelectionConds);

		/**Generate constraints for the negative conditions*/
		for(int k = 0; k < negativeStringSelConds.size(); k++){

			/**get table details*/
			String tableNo = negativeStringSelConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l = 1; l <= count; l++)
				stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeStringSelConds.get(k),l+offset-1) +" AND ";
		}


		/**Generate negative constraints for like conditions */
		Vector<Node> likeConds = disjunct.getLikeConds();

		/**get negative conditions for these nodes*/
		Vector<Node> negativeLikeConds = GenerateCVCConstraintForNode.getNegativeConditions(likeConds);

		for(int k=0; k<likeConds.size(); k++){

			/**get table details*/
			String tableNo = negativeLikeConds.get(k).getLeft().getTableNameNo();
			int offset = cvc.getRepeatedRelNextTuplePos().get(tableNo)[1];

			int count = cvc.getNoOfTuples().get(tableNo)* queryBlock.getNoOfGroups();/** We should generate the constraints across all groups */;;
			for(int l=1;l<=count;l++)
				stringConstraint += GenerateCVCConstraintForNode.genPositiveCondsForPred(queryBlock, negativeLikeConds.get(k),l+offset-1) +" AND ";
		}
		
		if(!stringConstraint.equalsIgnoreCase("")){
			stringConstraint = stringConstraint.substring(0, stringConstraint.length()-5);
		}
		constraints.stringConstraints.add(stringConstraint);
		return constraints;
	}
}
