package sh.komet.gui.search.flwor;

import javafx.scene.Node;
import javafx.scene.control.TreeTableCell;


/**
 *
 * @author aks8m
 */

public class WhereParameterCell extends TreeTableCell<QueryClause, Object>{

    public WhereParameterCell() {
        getStyleClass().add("where-cell");
    }

    @Override
    protected void updateItem(Object item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setText(null);
            setGraphic(null);
        } else {
            if(this.getTreeTableRow().getTreeItem() != null) {
                this.setGraphic(getPropertySheet());
            }
        }
    }

    private Node getPropertySheet(){
        return this.getTreeTableRow().getTreeItem().getValue().getPropertySheet();
    }

}
