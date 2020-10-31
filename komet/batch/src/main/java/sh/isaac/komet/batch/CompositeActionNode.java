package sh.isaac.komet.batch;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import sh.isaac.komet.batch.fxml.CompositeActionNodeController;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.control.property.ActivityFeed;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.interfaces.ExplorationNodeAbstract;

import java.io.IOException;
import java.util.Optional;

import static sh.isaac.komet.batch.CompositeActionFactory.ACTION_VIEW;


public class CompositeActionNode extends ExplorationNodeAbstract {

    public static final String ACTION_VIEW_TO_CREATE_COMPOSITE_ACTIONS = "Action view to create composite actions";

    {
        titleProperty.setValue(ACTION_VIEW);
        toolTipProperty.setValue(ACTION_VIEW_TO_CREATE_COMPOSITE_ACTIONS);
        menuIconProperty.setValue(Iconography.EDIT_PENCIL.getStyledIconographic());
    }


    final BorderPane root;
    final CompositeActionNodeController controller;

    public CompositeActionNode(ViewProperties manifold) {
        super(manifold);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/sh/isaac/komet/batch/fxml/CompositeActionNode.fxml"));
            this.root = loader.load();
            this.controller = loader.getController();
            this.controller.setViewProperties(manifold);

            this.controller.getActionNameProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue.isBlank()) {
                    titleProperty.setValue(ACTION_VIEW);
                    toolTipProperty.setValue(ACTION_VIEW_TO_CREATE_COMPOSITE_ACTIONS);
                } else {
                    titleProperty.setValue(newValue);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public Node getMenuIconGraphic() {
        return Iconography.EDIT_PENCIL.getStyledIconographic();
    }

    @Override
    public void revertPreferences() {

    }

    @Override
    public void savePreferences() {
        throw new UnsupportedOperationException();
    }


    @Override
    public Optional<Node> getTitleNode() {
        return Optional.empty();
    }

    @Override
    public Node getNode() {
        return root;
    }

    @Override
    public ActivityFeed getActivityFeed() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        // nothing to do...
    }

    @Override
    public boolean canClose() {
        return true;
    }
}
