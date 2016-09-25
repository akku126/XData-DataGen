package killMutations;

import java.io.BufferedReader;
import java.io.FileReader;

import testDataGen.GenerateCVC1;
import util.Configuration;

public class Utils {

	/**
	 * Checks whether the data generation is succeded or not
	 * @param cvc
	 * @param attempt
	 * @param i
	 * @return
	 */
	public static int[] checkIfSucces(GenerateCVC1 cvc, int attempt, int i) throws Exception{
		BufferedReader br = null;
		int [] list = new int[2];
		try{	
			cvc.setCount(cvc.getCount() - 1);
			br =  new BufferedReader(new FileReader(Configuration.homeDir+"/temp_cvc"+ cvc.getFilePath() +"/cvc3_"+ cvc.getCount()+".out"));
			
			String str = br.readLine();
			if((str == null || str.equalsIgnoreCase("Valid.")) && attempt<2){
				list[0] = attempt + 1;
				list[1] = i - 1;
	
			}
			else{
				cvc.setCount(cvc.getCount() + 1);
				list[0] = 0;
				list[1] = i;
			}
			}
			finally{
				if(br != null)
					br.close();
			}
			
			return list;
		}
}
