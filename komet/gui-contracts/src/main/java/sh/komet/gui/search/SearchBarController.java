package sh.komet.gui.search;


import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ToolBar;
import org.controlsfx.control.textfield.CustomTextField;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.style.StyleClasses;
import sh.komet.gui.util.FxGet;

import java.util.Collection;
import java.util.function.Consumer;

public class SearchBarController {

    @FXML
    private ProgressIndicator progressIndicator;

    @FXML
    private ToolBar searchToolbar;

    @FXML
    private Button closeSearchButton;

    @FXML
    private CustomTextField searchTextField;

    @FXML
    private Button previousMatchButton;

    @FXML
    private Button nextMatchButton;

    @FXML
    private Label matchLabel;

    private final ObservableList<Object> resultList = FXCollections.observableArrayList();

    private final SimpleObjectProperty<Object> selectedObject = new SimpleObjectProperty<>();

    private int matchIndex = 0;

    private Consumer<String> searchConsumer;

    private Node arrowDownNode = Iconography.ARROW_DOWN.getStyledIconographic();
    private Node arrowUpNode = Iconography.ARROW_UP.getStyledIconographic();


    @FXML
    void initialize() {
        assert searchToolbar != null : "fx:id=\"searchToolbar\" was not injected: check your FXML file 'SearchBar.fxml'.";
        assert closeSearchButton != null : "fx:id=\"closeSearchButton\" was not injected: check your FXML file 'SearchBar.fxml'.";
        assert searchTextField != null : "fx:id=\"searchTextField\" was not injected: check your FXML file 'SearchBar.fxml'.";
        assert previousMatchButton != null : "fx:id=\"previousMatchButton\" was not injected: check your FXML file 'SearchBar.fxml'.";
        assert nextMatchButton != null : "fx:id=\"nextMatchButton\" was not injected: check your FXML file 'SearchBar.fxml'.";
        assert matchLabel != null : "fx:id=\"matchLabel\" was not injected: check your FXML file 'SearchBar.fxml'.";

        Node searchMagnifier = Iconography.MAGNIFY.getStyledIconographic();
        searchMagnifier.getStyleClass().setAll(StyleClasses.SEARCH_MAGNIFY.toString());

        this.searchTextField.setLeft(searchMagnifier);
        this.searchTextField.setPromptText("Enter search text...");
        this.matchLabel.setText("");
        nextMatchButton.setDisable(true);
        previousMatchButton.setDisable(true);
        closeSearchButton.setDisable(true);
        closeSearchButton.setText("");
        closeSearchButton.setGraphic(Iconography.ICON_CLOSE.getStyledIconographic());
        matchIndex = -1;

        closeSearchButton.getStyleClass().setAll(StyleClasses.RESET_SEARCH.toString());
        nextMatchButton.getStyleClass().setAll(StyleClasses.NEXT_MATCH.toString());
        nextMatchButton.setText("");
        previousMatchButton.getStyleClass().setAll(StyleClasses.PREVIOUS_MATCH.toString());
        previousMatchButton.setText("");
        this.progressIndicator.setDisable(true);
    }

    @FXML
    void closeSearch(ActionEvent event) {
        nextMatchButton.setGraphic(null);
        previousMatchButton.setGraphic(null);
        this.searchTextField.setText("");
        this.matchLabel.setText("");
        this.resultList.clear();
        this.selectedObject.setValue(null);
        nextMatchButton.setDisable(true);
        previousMatchButton.setDisable(true);
        this.closeSearchButton.setDisable(true);
        this.progressIndicator.setProgress(0);
        this.progressIndicator.setDisable(true);
        this.resultList.clear();
    }

    @FXML
    void nextMatch(ActionEvent event) {
        if (this.resultList != null) {
            this.matchIndex++;
            if (this.matchIndex == this.resultList.size()) {
                this.matchIndex = 0;
            }
        }
        this.selectedObject.setValue(this.resultList.get(this.matchIndex));
        updateMatchLabel();
    }

    void updateMatchLabel() {
        this.nextMatchButton.setDisable(this.resultList.isEmpty());
        this.previousMatchButton.setDisable(this.resultList.isEmpty());
        if (this.resultList.isEmpty()) {
            this.matchLabel.setText("    No matches");
            nextMatchButton.setGraphic(null);
            previousMatchButton.setGraphic(null);
        } else {
            nextMatchButton.setGraphic(arrowDownNode);
            previousMatchButton.setGraphic(arrowUpNode);
            StringBuilder sb = new StringBuilder();
            sb.append("    ");
            sb.append(this.matchIndex + 1);
            sb.append(" of ");
            sb.append(this.resultList.size());
            sb.append(" matches ");
            this.matchLabel.setText(sb.toString());
        }
    }

    @FXML
    void previousMatch(ActionEvent event) {
        if (this.resultList != null) {
            this.matchIndex--;
            if (this.matchIndex < 0) {
                this.matchIndex = resultList.size() - 1;
            }
        }
        this.selectedObject.setValue(this.resultList.get(this.matchIndex));
        updateMatchLabel();
    }

    @FXML
    void startSearch(ActionEvent event) {
        this.resultList.clear();
        matchIndex = -1;
        if (this.searchConsumer != null) {
            this.progressIndicator.setDisable(false);
            progressIndicator.setProgress(0);
            matchLabel.setText("Searching...");
            searchConsumer.accept(this.searchTextField.getText());
            closeSearchButton.setDisable(false);
            updateMatchLabel();
        } else {
            matchLabel.setText("Search runnable is null");
            FxGet.dialogs().showErrorDialog("Configuration Error",
                    "Search runnable is null",
                    "The SearchBarController is not properly set up. The Search runnable method has not been set. ");
        }

    }

    public void setResults(Collection<Object> resultCollection) {
        Platform.runLater(() -> {
            this.resultList.setAll(resultCollection);
            this.selectedObject.setValue(null);
            this.matchIndex = 0;
            this.updateMatchLabel();
        });
    }

    public void addResult(Object match) {
        Platform.runLater(() -> {
            this.resultList.add(match);
            if (matchIndex == -1) {
                this.selectedObject.setValue(match);
                this.matchIndex = 0;
            }
            this.updateMatchLabel();
        });
    }

    // TODO redo this with javafx worker?
    public void setSearchConsumer(Consumer<String> searchConsumer) {
        this.searchConsumer = searchConsumer;
    }

    public Object getSelectedObject() {
        return selectedObject.get();
    }

    public SimpleObjectProperty<Object> selectedObjectProperty() {
        return selectedObject;
    }

    public void setProgress(double value) {
        if (Platform.isFxApplicationThread()) {
            progressIndicator.setProgress(value);
        } else {
            Platform.runLater(() -> progressIndicator.setProgress(value));
        }

    }
}
