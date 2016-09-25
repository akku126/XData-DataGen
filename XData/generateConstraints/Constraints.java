package generateConstraints;

import java.util.Vector;

public class Constraints {

	/** General constraints **/
	public Constraints() {
		// TODO Auto-generated constructor stub
		constraints=new Vector<String>();
		stringConstraints=new Vector<String>();
	}
	
	public Vector<String> constraints;
	
	/** String constraints**/
	public Vector<String> stringConstraints;
	
	public static Constraints mergeConstraints(Constraints constraints,Constraints constraint2){
		Constraints updatedConstraints=new Constraints();
		Vector<String> newConstraints=new Vector<String>();
		
		if(constraints.constraints.isEmpty() || constraint2.constraints.isEmpty()){
			if(constraints.constraints.isEmpty())
				updatedConstraints.constraints.addAll(constraint2.constraints);
			else
				updatedConstraints.constraints.addAll(constraints.constraints);
		}
		
		if(constraints.stringConstraints.isEmpty() || constraint2.stringConstraints.isEmpty()){
			if(constraints.stringConstraints.isEmpty()){
				updatedConstraints.stringConstraints.addAll(constraint2.stringConstraints);
			} else {
				updatedConstraints.stringConstraints.addAll(constraints.stringConstraints);
			}
		}
		
		for(String eachConstraint:constraints.constraints){
			for(String eachConstraint2:constraint2.constraints){
				String temp = "(" + eachConstraint + ") AND (" + eachConstraint2 +")";
				if(eachConstraint.equalsIgnoreCase("")){
					temp = eachConstraint2;
				} else if (eachConstraint2.equalsIgnoreCase("")){
					temp = eachConstraint;
				}
				newConstraints.add(temp);
			}
		}
		
		updatedConstraints.constraints.addAll(newConstraints);
		newConstraints.removeAllElements();
		for(String eachConstraint:constraints.stringConstraints){
			for(String eachConstraint2:constraint2.stringConstraints){
				String temp = eachConstraint + " AND " + eachConstraint2 ;
				if(eachConstraint.equalsIgnoreCase("")){
					temp = eachConstraint2;
				} else if (eachConstraint2.equalsIgnoreCase("")){
					temp = eachConstraint;
				}
				newConstraints.add(temp);
			}
		}
		
		updatedConstraints.stringConstraints.addAll(newConstraints);
		
		return updatedConstraints;
	}
	
	public static Constraints orConstraints(Constraints constraints,Constraints constraints2){
		Constraints updatedConstraints=new Constraints();
		updatedConstraints.constraints.addAll(constraints.constraints);
		updatedConstraints.constraints.addAll(constraints2.constraints);
		updatedConstraints.stringConstraints.addAll(constraints.stringConstraints);
		updatedConstraints.stringConstraints.addAll(constraints2.stringConstraints);
		return updatedConstraints;
	}
	
	public static String getConstraint(Constraints constraints){
		String constraintString="ASSERT ";
		for(String constrint:constraints.constraints){
			if(!constrint.equalsIgnoreCase(""))
				constraintString+= "(" + constrint + ")" + " OR ";
		}
		if(!constraints.constraints.isEmpty() && constraintString.length() > 7){
			constraintString=constraintString.substring(0, constraintString.length()-4);
			constraintString+=";";
		}
		else
			return "";
		return constraintString;
	}
	
	public static String getStringConstraints(Constraints constraints){
		String constraintString="";
		for(String constraint:constraints.stringConstraints){
			if(!constraint.equalsIgnoreCase(""))
				constraintString+= constraint +  " OR ";
		}
		if(!constraints.stringConstraints.isEmpty() && constraintString.length() > 7){
			if(!constraintString.equalsIgnoreCase(""))
				constraintString=constraintString.substring(0, constraintString.length()-4);
		}
		else 
			return "";
		return constraintString;
	}
}
