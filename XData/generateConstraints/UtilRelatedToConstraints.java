package generateConstraints;

public class UtilRelatedToConstraints {

	public static String removeAssert(String constraint){
		if(constraint.startsWith("ASSERT ")){
			constraint=constraint.substring(7,constraint.length()-2);
		}
		return constraint;
	}	
}

