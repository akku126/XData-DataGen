package generateConstraints;

import java.util.ArrayList;
import java.util.Vector;

import testDataGen.GenerateCVC1;
import util.ConstraintObject;

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
	
	public static Constraints mergeConstraints(GenerateCVC1 cvc, Constraints constraints,Constraints constraint2){
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
				
				String temp = "";// "(" + eachConstraint + ") AND (" + eachConstraint2 +")";
				if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
					temp += "(" + eachConstraint + ") AND (" + eachConstraint2 +")";
				}else{
					temp += "(and ("+eachConstraint +") ("+eachConstraint2+"))";
				}
				
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
				String temp = "";
				if(cvc.getConstraintSolver().equalsIgnoreCase("cvc3")){
					temp += eachConstraint + " AND " + eachConstraint2 ;
				}else{
					temp += "(and "+eachConstraint +" "+eachConstraint2+")";
				}
				
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
		/*System.out.println("Constraints of constraints.constraints");
		for(String x:constraints.constraints)
			System.out.println(x);
		*/
		updatedConstraints.constraints.addAll(constraints2.constraints);
		/*System.out.println("Constraints of constraints2.constraints");
		for(String x:constraints2.constraints)
			System.out.println(x);
		*/
		updatedConstraints.stringConstraints.addAll(constraints.stringConstraints);
		/*System.out.println("Constraints of constraints.stringConstraints");
		for(String x:constraints.stringConstraints)
			System.out.println(x);
		*/
		updatedConstraints.stringConstraints.addAll(constraints2.stringConstraints);
		/*System.out.println("Constraints of constraints2.stringConstraints");
		for(String x:constraints2.stringConstraints)
			System.out.println(x);
		*/
		return updatedConstraints;
	}
	
	public static String getConstraint(GenerateCVC1 cvc,Constraints constraints){
		String constraintString= ""; //"ASSERT ";
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		ArrayList<ConstraintObject> constrList = new ArrayList<ConstraintObject>();
		if(constraints != null && constraints.constraints != null && !constraints.constraints.isEmpty()){
						
		for(String constrint:constraints.constraints){
			if(constrint!= null &&!constrint.isEmpty() &&!constrint.equalsIgnoreCase("")){
				ConstraintObject conObj = new ConstraintObject();
			    conObj.setLeftConstraint(constrint);
			    constrList.add(conObj);
			}
				//constraintString+= "(" + constrint + ")" + " OR ";
		}
		constraintString+=constraintGen.generateOrConstraintsWithAssert(constrList);
		
		}
		/*if(!constraints.constraints.isEmpty() && constraintString.length() > 7){
			constraintString=constraintString.substring(0, constraintString.length()-4);
			constraintString+=";";
		}
		else
			return "";*/
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

	public static ArrayList<String> getStringConstraints(GenerateCVC1 cvc,Constraints constraints){
		String constraintString="";
		ConstraintGenerator constraintGen = new ConstraintGenerator();
		ArrayList<String> constrList = new ArrayList<String>();
		for(String constraint:constraints.stringConstraints){
			if(!constraint.isEmpty() && !constraint.equalsIgnoreCase("")){
				constrList.add(constraint);
			/*	ConstraintObject conObj = new ConstraintObject();
			    conObj.setLeftConstraint(constraint);
			    
			    if(conObj != null && conObj.getLeftConstraint() != null && !conObj.getLeftConstraint().isEmpty()){
			    	constrList.add(conObj);
			    }*/
			}
		}
		
		//constraintString+=constraintGen.generateOrConstraintsWithAssert(constrList);
		/*if(!constraints.stringConstraints.isEmpty() && constraintString.length() > 7){
			if(!constraintString.equalsIgnoreCase(""))
				constraintString=constraintString.substring(0, constraintString.length()-4);
		}
		else 
			return "";*/
		return constrList;
	}

}
