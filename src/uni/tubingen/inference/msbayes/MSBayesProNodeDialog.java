package uni.tubingen.inference.msbayes;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDoubleBounded;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.util.ColumnFilter;

/**
 * <code>NodeDialog</code> for the "MSBayesPro" Node.
 * This node perform protein inference analysis using Bayes Network based on MSBayesPro model.
 *
 * This node dialog derives from {@link DefaultNodeSettingsPane} which allows
 * creation of a simple dialog with standard components. If you need a more 
 * complex dialog please derive directly from 
 * {@link org.knime.core.node.NodeDialogPane}.
 * 
 * @author enrique
 */
public class MSBayesProNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the MSBayesPro node.
     */
    protected MSBayesProNodeDialog() {
    	super ();
        
        //fields to match with coming table...
        final SettingsModelString matches_peptides  = new SettingsModelString(MSBayesProNodeModel.CFGKEY_PEPTIDES, "Peptides");
        final SettingsModelString accsn_protein     = new SettingsModelString(MSBayesProNodeModel.CFGKEY_PROTEIN, "Protein");
        final SettingsModelString probabilities     = new SettingsModelString(MSBayesProNodeModel.CFGKEY_PROBABILITIES, "Probabilities");
        final SettingsModelString detectability     = new SettingsModelString(MSBayesProNodeModel.CFGKEY_DETECTABILITY, "Detectabilities");
        

        addDialogComponent(new DialogComponentColumnNameSelection(accsn_protein, "Proteins Column", 0, true, StringValue.class));
        addDialogComponent(new DialogComponentColumnNameSelection(matches_peptides, "Peptides Column", 0, true, 
        		           
      		               new ColumnFilter() {

		                	@Override
							public boolean includeColumn(DataColumnSpec colSpec) {
								if (colSpec.getType().isCollectionType() && colSpec.getType().getCollectionElementType().isCompatible(StringValue.class))
									return true;
								
								if (colSpec.getType().isCompatible(StringValue.class)) 
									return true;
								
								return false;
							}

							@Override
							public String allFilteredMsg() {
								return "No suitable columns (string or List/Set column) to select!";
							}
        			
        		}));
        
         addDialogComponent(new DialogComponentColumnNameSelection(probabilities, "Probabilities", 0, true, DoubleValue.class));
         addDialogComponent(new DialogComponentColumnNameSelection(detectability, "Detectability", 0, true, DoubleValue.class));

    }
}

