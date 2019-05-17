package sh.komet.gui.table.version;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.TableView;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.table.version.VersionTableController;

import java.io.IOException;
import java.net.URL;

public class VersionTable {
    final VersionTableController controller;
    final TableView tableView;
    public VersionTable(Manifold manifold) {
        try {
            URL resource = VersionTableController.class.getResource("VersionTable.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            loader.load();
            this.controller = loader.getController();
            tableView = loader.getRoot();
            controller.setManifold(manifold);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public VersionTableController getController() {
        return controller;
    }

    public TableView getRootNode() {
        return tableView;
    }
}
