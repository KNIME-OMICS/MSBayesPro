package uni.tubingen.inference.msbayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//This class is a wrapper for command-line tool MSBayesPro. It too perform parser for standard output (protein probability file).

public class MsBayesPro {
	
//fields
	private String PEPTIDE_PROBABILITY_FILE = null;

	private String PEPTIDE_PROTEIN_DETECTABILITY_FILE = null;

    private static final String MSBAYESPRO_PARAMETER_1 = "-pospep"; //passing probability file
    
    private static final String MSBAYESPRO_PARAMETER_2 = "-detectability"; //passing detectability file
    
    private static final String MSBAYESPRO_PARAMETER_3 = "-s";    // [-s] Output a tab formatted result.
    
    
    /**
     * Constructor
     * 
     * @param probability_file
     * @param detectability_file
     * @throws Exception
     */
    public MsBayesPro (String probability_file, String detectability_file) {
    	this.PEPTIDE_PROBABILITY_FILE = probability_file;
    	this.PEPTIDE_PROTEIN_DETECTABILITY_FILE = detectability_file;
    }
    
    public HashMap<String, String> computeProteinInference() throws InterruptedException{

        String s;
        String [] protein_proba = new String [9];
        HashMap<String, String> proba_protList = new  HashMap<String, String>();
        String regex = "\t";

        try {
        	
        	String msbayesPath = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
			if (!msbayesPath.endsWith(File.separator)) {
				// we are in the jar, only get the path to it
				msbayesPath = msbayesPath.substring(0, msbayesPath.lastIndexOf(File.separator) + 1);
			}
			msbayesPath += "executables" + File.separator + "MSBayesPro.linux64";
        	
        	ProcessBuilder pb = new ProcessBuilder(msbayesPath,
        			MSBAYESPRO_PARAMETER_1, PEPTIDE_PROBABILITY_FILE,
        			MSBAYESPRO_PARAMETER_2, PEPTIDE_PROTEIN_DETECTABILITY_FILE,
        			MSBAYESPRO_PARAMETER_3 );
        	pb.directory(null);
            
        	Process p = pb.start(); //running command-line tool (MSBayesPro)
        	
        	BufferedReader stdOutPut = new BufferedReader(new InputStreamReader(p.getInputStream()));
        	BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        	
        	//read output from the attempted command
        	//System.out.println("Here is the standard output of the command (if any):\n");
        	while ((s = stdOutPut.readLine()) != null) {
        		// idling to avoid timeout for command, necessary for get protein-probability file.
        		//System.out.println(s);
        	}
        	stdOutPut.close();
			   
			// read any errors from the attempted command
        	System.out.println("Here is the standard error of the command (if any):\n");
        	while ((s = stdError.readLine()) != null) {
        		System.out.println(s);
        	}
        	stdError.close();
        	
        	// readme of MSBAyesPro:
        	// When interpreting the result, use MAP_state_by_Memorizing to tell whether a protein exist,
        	// and use Positive_Probability_by_memorizing (the marginal posterior probability) as a confidence
        	// measure for protein identification. Notice that proteins with MAP_state_by_Memorizing = 1 could
        	// have posterior probability < 0.5. 
        	BufferedReader reader_file = new BufferedReader (new FileReader (new File(PEPTIDE_PROBABILITY_FILE+".quantify.bayes53ss")));
        	
        	// 1) collect all proteins in the group
        	// 2) all with MAP_state_by_Memorizing=1 will be reported
        	// 3) directly following ones with same "Positive_Probability_by_memorizing" are in one group
        	int groupCount = 0;
        	
        	// the last group, mapping from the probability to the accessions
        	String lastProbability = null;
        	List<String> lastAccessions = null;
        	while ((s = reader_file.readLine()) != null) {
        		if(s.startsWith("protein")) { //avoiding header from output file
        			continue;
        		}
        		
        		protein_proba = s.split(regex);
        		
        		groupCount--;
        		if ((groupCount < 1) || !protein_proba[4].equals(lastProbability)) {
        			// add the last group
        			if (lastProbability != null) {
        				StringBuilder accs = new StringBuilder();
        				for (String acc : lastAccessions) {
        					if (accs.length() > 0) {
        						accs.append(";");
        					}
    						accs.append(acc);
        				}
        				
        				proba_protList.put(accs.toString(), lastProbability);
        			}
        			
        			lastAccessions = new ArrayList<String>();
        		}
        		
        		if (groupCount < 1) {
        			// NO. of proteins = protein_proba[7]
        			groupCount = Integer.parseInt(protein_proba[7]);
        		}
        		
        		// MAP_state_by_Memorizing = protein_proba[2]
        		// Positive_Probability_by_memorizing = protein_proba[4]
        		// proba_protList.put(protein_proba[0], protein_proba[8]);
        		
        		if (protein_proba[2].equals("1")) {
            		lastProbability = protein_proba[4];
            		lastAccessions.add(protein_proba[0]);
        		} else {
        			lastProbability = null;
        		}
        	}
        	
        	if (lastProbability != null) {
				StringBuilder accs = new StringBuilder();
				for (String acc : lastAccessions) {
					if (accs.length() > 0) {
						accs.append(";");
					}
					accs.append(acc);
				}
				
    			proba_protList.put(accs.toString(), lastProbability);
			}
        	
        	reader_file.close();
        	
        	// delete the temporal files of MSBayesPro
        	File tmpFile = new File(PEPTIDE_PROBABILITY_FILE + ".quantify.bayes53ss");
        	tmpFile.delete();
        	tmpFile = new File(PEPTIDE_PROBABILITY_FILE + ".quantify.bayes53");
        	tmpFile.delete();
        	tmpFile = new File(PEPTIDE_PROBABILITY_FILE + ".quantify.peppost");
        	tmpFile.delete();
        } catch (Exception e) {
        	MSBayesProNodeModel.logger.error("exception while executing MSBayesPro" , e);
        }
        
        return proba_protList;
    }

}
