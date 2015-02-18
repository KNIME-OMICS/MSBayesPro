package uni.tubingen.inference.msbayes;

import org.knime.core.node.NodeView;


/**
 * <code>NodeView</code> for the "MSBayesPro" Node.
 * This node perform protein inference analysis using Bayes Network based on MSBayesPro model.
 *
 * @author enrique
 */
public class MSBayesProNodeView extends NodeView<MSBayesProNodeModel> {

    /**
     * Creates a new view.
     * 
     * @param nodeModel The model (class: {@link MSBayesProNodeModel})
     */
    protected MSBayesProNodeView(final MSBayesProNodeModel nodeModel) {
        super(nodeModel);
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void modelChanged() {
    	MSBayesProNodeModel nodeModel = 
                getNodeModel();
            assert nodeModel != null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onClose() {
        // TODO: generated method stub
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onOpen() {
        // TODO: generated method stub
    }

}

