package uni.tubingen.inference.msbayes;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.RowIterator;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;


/**
 * This is the model implementation of MSBayesPro.
 * This node perform protein inference analysis using Bayes Network based on MSBayesPro model.
 *
 * @author enrique
 */
public class MSBayesProNodeModel extends NodeModel {
	
	protected static final NodeLogger logger = NodeLogger.getLogger(MSBayesProNodeModel.class);
	
	static String CFGKEY_PEPTIDES = "peptides";
	static String CFGKEY_PROTEIN = "protein";
	static String CFGKEY_PROBABILITIES = "probabilities";
	static String CFGKEY_DETECTABILITY = "detectability";
	
	//fields to link execute variable with input variable...
	private final SettingsModelString m_peptide_column = new SettingsModelString(CFGKEY_PEPTIDES, "Peptides");
	private final SettingsModelString m_protein_column   = new SettingsModelString(CFGKEY_PROTEIN, "Protein");
	private final SettingsModelString m_probability_column   = new SettingsModelString(CFGKEY_PROBABILITIES, "Probabilities");
	private final SettingsModelString m_detectability_column   = new SettingsModelString(CFGKEY_PROBABILITIES, "Detectability");
	
	//fields to manage the input table...
	static int pep_idx    = 0;
	static int accsn_idx  = 0;
	static int proba_idx  = 0;
	static int detect_idx = 0;
	
	private File temporal_probability_file = null;
	private File temporal_detectability_file = null;
	
	// list holding the protein names, indices are used to call MSBAyesPro
	private List<String> proteinLongnames;
	
	/**
	 * Constructor for the node model.
	 */
	protected MSBayesProNodeModel() {
		super(1, 1);
		
		temporal_probability_file = null;
		temporal_detectability_file = null;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData,
			final ExecutionContext exec) throws Exception {
		
		this.checkTableConfiguraion(inData);
		
		DataTableSpec new_spec_table = new DataTableSpec(make_output_spec());  	
		BufferedDataContainer container = exec.createDataContainer(new_spec_table);
		
		createDetectabilityFile(inData[0]);
		createProbabilityFile(inData[0]);
		
		MsBayesPro process = new MsBayesPro(temporal_probability_file.getAbsolutePath(), temporal_detectability_file.getAbsolutePath());
		HashMap<String, Number[]> proteinsMap = process.computeProteinInference();
		writeToContainer(container, proteinsMap);
		container.close();
		
		temporal_probability_file.delete();
		temporal_detectability_file.delete();
		return new BufferedDataTable[]{ container.getTable() };
	}
	
	
	/**
	 * method for checking the table configuration coming...
	 * 
	 * @param inData
	 * @throws Exception
	 */
	private void checkTableConfiguraion(BufferedDataTable[] inData) throws Exception{
		//important!!! getting correct index from coming table 
		pep_idx  = inData[0].getDataTableSpec().findColumnIndex(m_peptide_column.getStringValue());
		accsn_idx= inData[0].getDataTableSpec().findColumnIndex(m_protein_column.getStringValue());
		proba_idx= inData[0].getDataTableSpec().findColumnIndex(m_probability_column.getStringValue());
		detect_idx= inData[0].getDataTableSpec().findColumnIndex(m_detectability_column.getStringValue());
			
		if (pep_idx < 0 || accsn_idx < 0 || proba_idx < 0 || detect_idx < 0 || pep_idx == accsn_idx ) {
			throw new Exception("Illegal columns: "+m_peptide_column+" "+m_protein_column+" "+m_probability_column+" "+m_detectability_column+", re-configure the node!");
		}
	}
	
	/**
	 * configure output table column name
	 * @return
	 */
	private DataColumnSpec[]  make_output_spec() {  	
		DataColumnSpec cols[] = new DataColumnSpec[4];
		cols[0] = new DataColumnSpecCreator("Protein ID", StringCell.TYPE).createSpec();
		cols[1] = new DataColumnSpecCreator("MSBayes Probability", DoubleCell.TYPE).createSpec();
		cols[2] = new DataColumnSpecCreator("nrPeptidesMod", IntCell.TYPE).createSpec();
		cols[3] = new DataColumnSpecCreator("nrPeptides", IntCell.TYPE).createSpec();
		
		return cols;
	}
	
	
	/**
	 * This function builds the detectability file, output: Peptide ---> Protein(index) ---> Detectability
	 * 
	 * @param data_table
	 * @throws IOException
	 */
	private void createDetectabilityFile(BufferedDataTable data_table) throws IOException {
		temporal_detectability_file = File.createTempFile("msbayes_detectability_file", ".txt");
		PrintWriter pw = new PrintWriter(new FileWriter(temporal_detectability_file));
		
		proteinLongnames = new ArrayList<String>();
		
		RowIterator row_it = data_table.iterator();
		while (row_it.hasNext()) {
			// getting information from current row...
			DataRow r = row_it.next();
			DataCell pep_cell    = r.getCell(MSBayesProNodeModel.pep_idx);
			DataCell accsn_cell  = r.getCell(MSBayesProNodeModel.accsn_idx);
			DataCell proba_cell  = r.getCell(MSBayesProNodeModel.proba_idx);
			DataCell detect_cell = r.getCell(MSBayesProNodeModel.detect_idx);
			
			// rows with missing cells cannot be processed (no missing values in PSM graph...)
			if (pep_cell.isMissing() || accsn_cell.isMissing() || proba_cell.isMissing()) {
				continue;
			}
			
			// getting value from cells
			String peptide_entry = ((StringValue) pep_cell).getStringValue();
			String protein_accsn = ((StringValue) accsn_cell).getStringValue();
			Double detect_entry  = ((DoubleValue) detect_cell).getDoubleValue();
			
			String [] proteins = protein_accsn.split(";");
			
			for (String proteinAcc : proteins) {
				if (!proteinLongnames.contains(proteinAcc)) {
					proteinLongnames.add(proteinAcc);
				}
				
				int idx = proteinLongnames.indexOf(proteinAcc);
				pw.println(peptide_entry + "\t" + (idx+100) + "\t" + detect_entry);
			}
		}
		
		pw.close();
	}
	
	
	/**
	 * This function builds the probability file
	 *    
	 * @param data_table
	 * @throws IOException
	 */
	private void createProbabilityFile (BufferedDataTable data_table) throws IOException {
		temporal_probability_file = File.createTempFile("ms_bayes_probability_file", ".txt");
		PrintWriter pw = new PrintWriter(new FileWriter(temporal_probability_file));
		
		RowIterator row_it = data_table.iterator();
		while (row_it.hasNext()) {
			// getting information from current row...
			DataRow r = row_it.next();
			DataCell pep_cell   = r.getCell(MSBayesProNodeModel.pep_idx);
			DataCell proba_cell = r.getCell(MSBayesProNodeModel.proba_idx);    		
			
			// rows with missing cells cannot be processed (no missing values in PSM graph...)
			if (pep_cell.isMissing() || proba_cell.isMissing()) {
				continue;
			}
			
			//getting value from cells
			String peptide_entry = ((StringValue) pep_cell).getStringValue();
			Double proba_entry   = ((DoubleValue) proba_cell).getDoubleValue();
			
			pw.println(peptide_entry + "\t" + proba_entry);
		}
		
		pw.close();
	}
	
	
	/**
	 * print the proteins and their probabilities in descending order.
	 * @param proteinMap
	 */
	private void writeToContainer(BufferedDataContainer container, HashMap<String, Number[]> proteinMap) {
		Set<String> set_protein = proteinMap.keySet();
		
		for(String proteinIDs : set_protein) {
			if(proteinIDs==null){
				continue;
			}
			
			RowKey key = new RowKey(proteinIDs);
			DataCell[] cells = new DataCell[4];
			
			StringBuilder accs = new StringBuilder();
			for (String acc : proteinIDs.split(";")) {
				if (accs.length() > 0) {
					accs.append(";");
				}
				accs.append(proteinLongnames.get(Integer.parseInt(acc)-100));
			}
			
			cells[0] = new StringCell(accs.toString());
			
			Number[] values = proteinMap.get(proteinIDs);
			cells[1] = new DoubleCell(values[0].doubleValue());
			cells[2] = new IntCell(values[1].intValue());
			cells[3] = new IntCell(values[2].intValue());
			
			DataRow row = new DefaultRow(key, cells);
			container.addRowToTable(row);
		}
	}
	
	
    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs)
            throws InvalidSettingsException {
    	return new DataTableSpec[]{new DataTableSpec(this.make_output_spec())};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
    	m_peptide_column.saveSettingsTo(settings);
        m_protein_column.saveSettingsTo(settings);
        m_probability_column.saveSettingsTo(settings);
        m_detectability_column.saveSettingsTo(settings);       
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_peptide_column.loadSettingsFrom(settings);
        m_protein_column.loadSettingsFrom(settings);
        m_probability_column.loadSettingsFrom(settings);
        m_detectability_column.loadSettingsFrom(settings);   
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
    	m_peptide_column.validateSettings(settings);
        m_protein_column.validateSettings(settings);
        m_probability_column.validateSettings(settings);
        m_detectability_column.validateSettings(settings);       
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File internDir, final ExecutionMonitor exec)
    		throws IOException, CanceledExecutionException {
        // TODO: generated method stub
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File internDir, final ExecutionMonitor exec)
    		throws IOException, CanceledExecutionException {
        // TODO: generated method stub
    }

}

