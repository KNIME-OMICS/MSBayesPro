package uni.tubingen.protein.inference.msbayes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.StringTokenizer;

//This class is a wrapper for command-line tool MSBayesPro. It too perform parser for standard output (protein probability file).

public class MsBayesPro {
	
//fields
	
	private static volatile MsBayesPro instance = null;
	
	private  String PEPTIDE_PROBABILITY_FILE = null;

	private  String PEPTIDE_PROTEIN_DETECTABILITY_FILE = null;

    private static final String MSBayesPro_COMMAND = "./MSBayesPro.linux64";  //command for run MSBayesPro in console (linux)
    
    private static final String MSBAYESPRO_PARAMETER_1 = "-pospep"; //passing probability file
    
    private static final String MSBAYESPRO_PARAMETER_2 = "-detectability"; //passing detectability file
    
    private static final String MSBAYESPRO_PARAMETER_3 = "-s";    // [-s] Output a tab formatted result.

    private ProcessBuilder pb = null;
    
//constructor
    
    //singleton instance

    public static MsBayesPro getInstance(String probability_file, String detectability_file) throws Exception {
       if (instance == null) {
         synchronized (MsBayesPro.class){
             if (instance == null) {
                     instance = new MsBayesPro (probability_file, detectability_file);
             }
         }
       }
        return instance;
    }
    
    private MsBayesPro (String probability_file, String detectability_file) throws Exception {

        // URL url = MsBayesPro.class.getClassLoader().getResource("/temp/tool/MSBayesPro");
        // if (url == null) {
        //    throw new IllegalStateException("The MSBayesPro program was not found!!");
        // }
    	
    	  this.PEPTIDE_PROBABILITY_FILE = probability_file;
    	  this.PEPTIDE_PROTEIN_DETECTABILITY_FILE = detectability_file;

          //Here, pass the root of the command-line tool for run (MSBayesPro.linux64)...
    	  File ms_bayes_tool = new File("/media/Datos/TRABAJO/KNIME workflow/workspace/MSBayesPro/src/resources");  //partial solution- no functional in other platform!!!

          this.pb = new ProcessBuilder(MSBayesPro_COMMAND, MSBAYESPRO_PARAMETER_1, PEPTIDE_PROBABILITY_FILE, MSBAYESPRO_PARAMETER_2, PEPTIDE_PROTEIN_DETECTABILITY_FILE, MSBAYESPRO_PARAMETER_3 );
          this.pb.directory(ms_bayes_tool);
      }
    
    public HashMap<String, String> computeProteinInference() throws InterruptedException{

        String s;
        String [] protein_proba = new String [9];
        HashMap<String, String> proba_protList = new  HashMap<String, String>();
        String regex = "\t";

        try {

              Process p = pb.start(); //running command-line tool (MSBayesPro)
              
              BufferedReader stdOutPut = new BufferedReader(new InputStreamReader(p.getInputStream()));
              BufferedReader stdError     = new BufferedReader(new InputStreamReader(p.getErrorStream()));
              
              // read output from the attempted command
              System.out.println("Here is the standard output of the command (if any):\n");   //avoiding timeout for command, necessary for get protein-probability file.
               while ((s = stdOutPut.readLine()) != null) {
                   System.out.println(s);
               }
                 
               stdOutPut.close();
               
            // read any errors from the attempted command
             System.out.println("Here is the standard error of the command (if any):\n");
              while ((s = stdError.readLine()) != null) {
                  System.out.println(s);
              }
                         
              stdError.close();
                
            //  System.exit(0);
              BufferedReader reader_file = new BufferedReader (new FileReader (new File(PEPTIDE_PROBABILITY_FILE+".quantify.bayes53ss")));
              while ((s = reader_file.readLine()) != null){
            	  if(s.startsWith("protein")) { //avoiding header from output file
            	     continue;
            	  }
            	 protein_proba = s.split(regex);
            	    		proba_protList.put(protein_proba[0], protein_proba[8]);
            	    // System.out.println(s);
                }
              //System.out.println(proba_protList);
                reader_file.close();
        }

        catch (IOException e) {
            System.out.println("exception happened - here's what I know: ");
             e.printStackTrace();
           // System.exit(-1);
        }
        
        
        return proba_protList;

    }

}
