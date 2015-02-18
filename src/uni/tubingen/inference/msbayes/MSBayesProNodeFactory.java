package uni.tubingen.inference.msbayes;

import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeFactory;
import org.knime.core.node.NodeView;

/**
 * <code>NodeFactory</code> for the "MSBayesPro" Node.
 * This node perform protein inference analysis using Bayes Network based on MSBayesPro model.
 *
 * @author enrique
 */
public class MSBayesProNodeFactory 
        extends NodeFactory<MSBayesProNodeModel> {

    /**
     * {@inheritDoc}
     */
    @Override
    public MSBayesProNodeModel createNodeModel() {
        return new MSBayesProNodeModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrNodeViews() {
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeView<MSBayesProNodeModel> createNodeView(final int viewIndex,
            final MSBayesProNodeModel nodeModel) {
        return new MSBayesProNodeView(nodeModel);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasDialog() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NodeDialogPane createNodeDialogPane() {
        return new MSBayesProNodeDialog();
    }

}

