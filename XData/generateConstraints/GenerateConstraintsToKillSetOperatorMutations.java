package generateConstraints;

import java.util.ArrayList;

import parsing.Node;
import testDataGen.GenerateCVC1;
import testDataGen.GenerateUnionCVC;

public class GenerateConstraintsToKillSetOperatorMutations
{
	public static String generateConstraintsForMatchingTuples(GenerateUnionCVC cvc)
	{
		GenerateCVC1 left = cvc.getGenCVCleft();
		GenerateCVC1 right = cvc.getGenCVCright();
		ArrayList<Node> leftCols = left.getOuterBlock().getProjectedCols();
		ArrayList<Node> rightCols = right.getOuterBlock().getProjectedCols();
		
		String constraints = "ASSERT (";
		
		for(int i=0;i<leftCols.size();i++){
			Node n1=leftCols.get(i);
			Node n2=rightCols.get(i);
			constraints += GenerateCVCConstraintForNode.cvcMapNode(n1, "1") + "=" + GenerateCVCConstraintForNode.cvcMapNode(n2, "1") + " AND ";
		}
		
		constraints = constraints.substring(0, constraints.length()-4);
		constraints += ");";
		
		return constraints;
	}
	
}
