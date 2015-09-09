package uni.tubingen.inference.msbayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is a wrapper for command-line tool MSBayesPro. It too perform
 * parser for standard output (protein probability file).
 * @author enruque, julian
 *
 */
public class MsBayesPro {
		
	private String PEPTIDE_PROBABILITY_FILE = null;
	
	private String PEPTIDE_PROTEIN_DETECTABILITY_FILE = null;
	
	private static final String MSBAYESPRO_PARAMETER_1 = "-pospep"; 		//passing probability file
	
	private static final String MSBAYESPRO_PARAMETER_2 = "-detectability";	//passing detectability file
	
	private static final String MSBAYESPRO_PARAMETER_3 = "-s";				// [-s] Output a tab formatted result.
	
	
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
	
	
	/**
	 * 
	 * @return a map from the protein group's accession to array[probability, nrModPeps, nrPeps]
	 * @throws InterruptedException
	 */
	public HashMap<String, Number[]> computeProteinInference() throws InterruptedException{
		HashMap<String, Number[]> proteinsMap = new HashMap<String, Number[]>();
		
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
			String line;
			while ((line = stdOutPut.readLine()) != null) {
				// idling to avoid timeout for command, necessary for get protein-probability file.
				//System.out.println(s);
			}
			stdOutPut.close();
			   
			// read any errors from the attempted command
			System.out.println("Here is the standard error of the command (if any):\n");
			while ((line = stdError.readLine()) != null) {
				System.out.println(line);
			}
			stdError.close();
			
			// readme of MSBAyesPro:
			// When interpreting the result, use MAP_state_by_Memorizing to tell whether a protein exist,
			// and use Positive_Probability_by_memorizing (the marginal posterior probability) as a confidence
			// measure for protein identification. Notice that proteins with MAP_state_by_Memorizing = 1 could
			// have posterior probability < 0.5.
			
			// if there are only "same-peptide-proteins" in set -> get set probability,
			// else get each's protein "Positive_Probability_by_memorizing" (and return only the highest probability in set)
			// to get reported, a protein needs MAP_state_by_Memorizing=1
			
			// get the information from the complex file
			BufferedReader reader_file = new BufferedReader (new FileReader (new File(PEPTIDE_PROBABILITY_FILE+".quantify.bayes53")));
			
			// pattern to parse set probability
			Pattern setProbaPattern = Pattern.compile("^.+Set probability: ([^;]+);.+$");
			
			// pattern to parse the protein header
			Pattern proteinHeaderPattern = Pattern.compile("^(\\d+)	\\S+	(\\d+)	\\d+	(\\S+)	\\d+	\\d+	\\d+	\\S+.*$");
			
			// pattern to parse a peptide
			Pattern peptidePattern = Pattern.compile("^(\\S+)	\\S+	\\S+	\\S+	\\S+	\\S+.*$");
			
			// probability of the current set
			Double setProba = null;
			
			// the current proteins in the set
			Set<Integer> proteinsSet = null;
			
			// the current protein ID
			Integer proteinID = null;
			
			// mapping from the proteins to their probabilities
			HashMap<Integer, Double> proteinProbabilities = new HashMap<Integer, Double>();
			
			// mapping from the proteins to their peptides
			HashMap<Integer, Set<String>> proteinPeptides = new HashMap<Integer, Set<String>>();
			
			while ((line = reader_file.readLine()) != null) {
				if (line.startsWith("Set #")) {
					// start a new set
					Matcher m = setProbaPattern.matcher(line);
					if (m.matches()) {
						setProba = Double.parseDouble(m.group(1));
						proteinsSet = new HashSet<Integer>();
						proteinID = null;
					} else {
						setProba = null;
						proteinsSet = null;
						proteinID = null;
						MSBayesProNodeModel.logger.error("no probability found for set: '" + line + "'");
					}
				} else if (setProba != null) {
					// in a set
					Matcher headerMatcher = proteinHeaderPattern.matcher(line);
					Matcher peptideMatcher = peptidePattern.matcher(line);
					if (headerMatcher.matches()) {
						if (headerMatcher.group(2).equals("1")) {
							// starting a new protein and it has MAP==1
							proteinID = Integer.parseInt(headerMatcher.group(1));
							proteinsSet.add(proteinID);
							
							proteinProbabilities.put(proteinID, Double.parseDouble(headerMatcher.group(3)));
							proteinPeptides.put(proteinID, new HashSet<String>());
						}
					} else if ((proteinID != null) && peptideMatcher.matches()) {
						proteinPeptides.get(proteinID).add(peptideMatcher.group(1));
					} else if ((proteinID != null) && (line.trim().length() == 0)) {
						// this protein is done
						proteinID = null;
					} else if ((proteinID == null) && (line.trim().length() == 0)) {
						// the set is finished (after two empty lines)
						
						if (proteinsSet.size() == 0) {
							// no reportable proteins in set -> go on
							setProba = null;
							proteinsSet = null;
							proteinID = null;
							continue;
						}
						
						if (checkSamePeptides(proteinsSet, proteinPeptides)) {
							// all proteins have the same peptides -> report the proteins with the set's probability
							proteinsMap.putAll(putIntoReportMap(setProba, proteinsSet, proteinPeptides));
						} else {
							// sort proteins by probability and report them
							Map<Double, Set<Integer>> probasToProteins = new HashMap<Double, Set<Integer>>();
							for (Integer protID : proteinsSet) {
								Double proba = proteinProbabilities.get(protID);
								if (!probasToProteins.containsKey(proba)) {
									probasToProteins.put(proba, new HashSet<Integer>());
								}
							}
							
							for (Double proba : probasToProteins.keySet()) {
								proteinsMap.putAll(
										putIntoReportMap(proba, probasToProteins.get(proba), proteinPeptides));
							}
						}
						
						// reset the set
						setProba = null;
						proteinsSet = null;
						proteinID = null;
					}
				}
			}
			
			// 
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
		
		return proteinsMap;
	}
	
	
	private Number[] getValuesFromPeptides(Set<String> modifiedPeptides, Double setProba) {
		Number[] values = new Number[3];
		
		values[0] = setProba;
		values[1] = modifiedPeptides.size();
		
		Set<String> sequenceWithoutMods = new HashSet<String>();
		for (String modSeq : modifiedPeptides) {
			sequenceWithoutMods.add(modSeq.replaceAll("\\([^\\)]+\\)", ""));
		}
		
		values[2] = sequenceWithoutMods.size();
		
		return values;
	}
	
	
	/**
	 * check whether all proteins have the same peptides and if, report the proteins with the set's probability
	 * 
	 * @param protIDs
	 * @param protPeptides
	 * @return
	 */
	private boolean checkSamePeptides(Set<Integer> protIDs, Map<Integer, Set<String>> protPeptides) {
		boolean allSamePeptides = true;
		
		if (protIDs.size() > 1) {
			Set<String> checkPeptides;
			
			Iterator<Integer> protIt = protIDs.iterator();
			checkPeptides = protPeptides.get(protIt.next());
			
			while (protIt.hasNext() && allSamePeptides) {
				Integer protID = protIt.next();
				if (!checkPeptides.equals(protPeptides.get(protID))) {
					allSamePeptides = false;
				}
			}
		}
		
		return allSamePeptides;
	}
	
	
	/**
	 * Sort the given proteins into peptide same-set groups and report them with the given probability 
	 * 
	 * @param proteinProba
	 * @param protIDs
	 * @param protPeptides
	 * @return
	 */
	private HashMap<String, Number[]> putIntoReportMap(Double proteinProba, Set<Integer> protIDs,
			Map<Integer, Set<String>> protPeptides) {
		HashMap<String, Number[]> proteinsMap = new HashMap<String, Number[]>();
		
		// get protein IDs with same peptide sets
		HashMap<String, Set<Integer>> peptidesToProtIDs = new HashMap<String, Set<Integer>>();
		for (Integer protID : protIDs) {
			TreeSet<String> peptides = new TreeSet<String>(protPeptides.get(protID));
			
			String key = peptides.toString();
			if (!peptidesToProtIDs.containsKey(key)) {
				peptidesToProtIDs.put(key, new HashSet<Integer>());
			}
			
			peptidesToProtIDs.get(key).add(protID);
		}
		
		// report each protein group (with same peptide set)
		for (Set<Integer> protIdSet : peptidesToProtIDs.values()) {
			Integer firstID = null;
			StringBuilder accs = new StringBuilder();
			for (Integer protID : protIdSet) {
				if (accs.length() > 0) {
					accs.append(";");
				} else {
					firstID = protID;
				}
				accs.append(protID);
			}
			
			proteinsMap.put(accs.toString(), getValuesFromPeptides(protPeptides.get(firstID), proteinProba));
		}
		
		return proteinsMap;
	}
}
