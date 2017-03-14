package generateCVC4Constraints;

import generateConstraints.AddDataBaseConstraints;
import generateConstraints.GenerateCommonConstraintsForQuery;

import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import testDataGen.GenerateCVC1;

public class GenerateCommonConstraintsForQueryUsingSMT {

	private static Logger logger=Logger.getLogger(GenerateCommonConstraintsForQueryUsingSMT.class.getName());
	
	public static void generateNullandDBConstraintsUsingSMT(GenerateCVC1 cvc, Boolean unique) throws Exception {
	try{
		/** Add null constraints for the query */
		//getNullConstraintsForQuery(cvc);


		if( cvc.getCVCStr() == null)
			cvc.setCVCStr("");
		String CVCStr = cvc.getCVCStr();

		/**Add constraints related to database */
		CVCStr += AddDataBaseConstraints.addDBConstraints(cvc);
		cvc.setCVCStr(CVCStr);
	}catch (TimeoutException e){
		logger.log(Level.SEVERE,e.getMessage(),e);		
		throw e;
	}catch(Exception e){
		logger.log(Level.SEVERE,e.getMessage(),e);		
		throw e;
	}
	}

}
