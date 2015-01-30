package uni.tubingen.protein.inference.msbayes;

import java.util.HashMap;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.RowIterator;
import org.knime.core.data.StringValue;
import org.knime.core.node.BufferedDataTable;

public class InternalMapProteins {
	   
	    public static HashMap<Integer, String> InternalMap = new HashMap<Integer, String>();
	    public static Integer ProteinID = 100;
	     
	     static private void SetInternalMapProteinID (BufferedDataTable data_table){
  		     
	   		
	   		RowIterator row_it = data_table.iterator();
	     	while (row_it.hasNext()) {
	     		
	      		//getting information from current row...
	     		DataRow r = row_it.next();
	     		DataCell accsn_cell   = r.getCell(MSBayesProNodeModel.accsn_idx);
	     		
	     		
	     		//rows with missing cells cannot be processed (no missing values in PSM graph...)
	     		if (accsn_cell.isMissing()) {
	     			continue;
	     		}
	     		
	     		//getting value from cells
	     		String protein_accsn   =   ((StringValue) accsn_cell).getStringValue();
	     		 
	     		String [] protein_group = protein_accsn.split(";");
	     		 
	     	    	for(int i = 0; i < protein_group.length; i++){				
	     	    		InternalMap.put(ProteinID++, protein_group[i]);
	     	        }			 
	 			}		
	   	}
}
