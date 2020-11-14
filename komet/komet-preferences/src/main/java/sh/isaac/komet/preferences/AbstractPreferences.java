/*
 * Copyright 2018 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
         US Veterans Health Administration, OSHERA, and the Health Services Platform Consortium..
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sh.isaac.komet.preferences;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WeakChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.PropertySheet;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.preferences.IsaacPreferences;

import sh.komet.gui.contract.preferences.KometPreferencesController;
import sh.komet.gui.contract.preferences.PreferenceGroup;
import sh.komet.gui.contract.preferences.PreferencesTreeItem;
import sh.komet.gui.control.concept.PreferenceChanged;
import sh.komet.gui.control.property.PropertyEditorFactory;
import sh.komet.gui.control.property.PropertySheetItem;
import sh.komet.gui.control.property.PropertySheetPurpose;
import sh.komet.gui.control.property.ViewProperties;

import static sh.komet.gui.contract.preferences.PreferenceGroup.Keys.*;

/**
 *
 * @author kec
 */
public abstract class AbstractPreferences implements PreferenceGroup {

    protected static final Logger LOG = LogManager.getLogger();

    protected final IsaacPreferences preferencesNode;
    private final BooleanProperty initialized = new SimpleBooleanProperty(this, INITIALIZED.toString());
    private final BooleanProperty changed = new SimpleBooleanProperty(this, "changed", false);
    private final SimpleStringProperty groupNameProperty = new SimpleStringProperty(this, "group name");
    private final ObservableList<PropertySheet.Item> itemList = FXCollections.observableArrayList();

    {
        itemList.addListener((ListChangeListener.Change<? extends PropertySheet.Item> c) -> {
            makePropertySheet();
        });
    }


    protected static final String getGroupName(IsaacPreferences preferencesNode) {
        return preferencesNode.get(PreferenceGroup.Keys.GROUP_NAME, UUID.randomUUID().toString());
    }


    protected static final String getGroupName(IsaacPreferences preferencesNode, String defaultValue) {
        return preferencesNode.get(PreferenceGroup.Keys.GROUP_NAME, defaultValue);
    }

    private final ViewProperties viewProperties;
    protected final KometPreferencesController kpc;
    protected PreferencesTreeItem treeItem;
    private final Button revertButton = new Button("Revert");
    private final Button saveButton = new Button("Save");
    private final Button deleteButton = new Button("Delete");
    private final BorderPane propertySheetBorderPane = new BorderPane();
    private PropertySheet propertySheet;

    {
        deleteButton.setOnAction(this::deleteSelf);

        revertButton.setOnAction((event) -> {
            revert();
            changed.setValue(Boolean.FALSE);
        });
        revertButton.setDisable(true);

        saveButton.setOnAction((event) -> {
            save();
            changed.setValue(Boolean.FALSE);
        });
        saveButton.setDisable(true);

        changed.addListener((observable, oldValue, newValue) -> {
            if (newValue == true) {
                revertButton.setDisable(false);
                saveButton.setDisable(false);
            } else {
                revertButton.setDisable(true);
                saveButton.setDisable(true);
            }
        });
    }
    Region spacer = new Region();
    Region spacer2 = new Region();

    {
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);
    }
    ToolBar bottomBar = new ToolBar(spacer, revertButton, saveButton, spacer2, deleteButton);

    public AbstractPreferences(IsaacPreferences preferencesNode, String groupName, ViewProperties viewProperties,
                               KometPreferencesController kpc) {
        if (preferencesNode == null) {
            throw new NullPointerException("preferencesNode cannot be null.");
        }
        if (groupName == null) {
            throw new NullPointerException("groupName cannot be null.");
        }
        if (viewProperties == null) {
            throw new NullPointerException("Manifold cannot be null.");
        }
        if (viewProperties.getManifoldCoordinate().getLanguageCoordinate() == null) {
            throw new NullPointerException("Manifold.getLanguageCoordinate() cannot be null.");
        }
        if (viewProperties.getManifoldCoordinate().getLogicCoordinate() == null) {
            throw new NullPointerException("Manifold.getLogicCoordinate() cannot be null.");
        }
        if (kpc == null) {
            throw new NullPointerException("KometPreferencesController cannot be null.");
        }
        this.preferencesNode = preferencesNode;
        this.initialized.setValue(preferencesNode.getBoolean(INITIALIZED, false));
        this.groupNameProperty.set(groupName);
        this.viewProperties = viewProperties;
        this.kpc = kpc;
        this.groupNameProperty.addListener(this::changeGroupName);
    }

    private void changeGroupName(ObservableValue<? extends String> observable, String oldValue, String newValue) {
        if (treeItem != null) {
            PreferenceGroup group = treeItem.getValue();
            group.groupNameProperty().setValue(newValue);
            treeItem.setValue(null);
            treeItem.setValue(group);
        }
    }

    protected void deleteSelf(ActionEvent event) {
        try {
            PreferencesTreeItem parentTreeItem = (PreferencesTreeItem) this.treeItem.getParent();
            ParentPanel parentPanel = (ParentPanel) parentTreeItem.getValue();
            parentPanel.removeChild(this);
            parentPanel.save();

            this.getPreferencesNode().removeNode();
            this.treeItem.getParent().getChildren().remove(this.treeItem);

        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex.getLocalizedMessage(), ex);
        }
    }

    @Override
    public PreferencesTreeItem getTreeItem() {
        return treeItem;
    }

    @Override
    public void setTreeItem(PreferencesTreeItem treeItem) {
        this.treeItem = treeItem;
        this.treeItem.setPreferences(this.preferencesNode);
        addChildren();
    }

    protected void addChildren() {
        // Override if node adds children to tree. 
    }

    public final IsaacPreferences getPreferencesNode() {
        return preferencesNode;
    }

    @Override
    public final void save() {
        try {
            initialized.set(true);
            preferencesNode.putBoolean(INITIALIZED, initialized.get());
            preferencesNode.putEnum(preferencesNode.getNodeType());
            saveFields();
            preferencesNode.sync();
            preferencesNode.flush();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected final IsaacPreferences addChild(String childName, Class<? extends AbstractPreferences> childPreferencesClass) {
        IsaacPreferences childNode = this.preferencesNode.node(childName);
        childNode.put(PROPERTY_SHEET_CLASS, childPreferencesClass.getName());
        List<String> childPreferences = this.preferencesNode.getList(CHILDREN_NODES);
        if (!childPreferences.contains(childName)) {
            childPreferences.add(childName);
        }

        this.preferencesNode.putList(CHILDREN_NODES, childPreferences);
        return childNode;
    }

    @Override
    public final String getGroupName() {
        return groupNameProperty.get();
    }

    @Override
    public final SimpleStringProperty groupNameProperty() {
        return groupNameProperty;
    }

    public final void setGroupName(String groupName) {
        this.groupNameProperty.set(groupName);
    }

    @Override
    public final boolean initialized() {
        return initialized.get();
    }

    @Override
    public final void revert() {
        try {
            initialized.setValue(preferencesNode.getBoolean(INITIALIZED, false));
            preferencesNode.putEnum(preferencesNode.getNodeType());
            revertFields();
        } catch (BackingStoreException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected abstract void saveFields() throws BackingStoreException;

    protected abstract void revertFields() throws BackingStoreException;

    @Override
    public String toString() {
        return getGroupName();
    }

    public final List<PropertySheet.Item> getItemList() {
        return itemList;
    }

    protected void makePropertySheet() {
        PropertySheet sheet = new PropertySheet();
        sheet.setMode(PropertySheet.Mode.NAME);
        sheet.setSearchBoxVisible(false);
        sheet.setModeSwitcherVisible(false);
        sheet.setPropertyEditorFactory(new PropertyEditorFactory(viewProperties.getManifoldCoordinate()));
        sheet.getItems().addAll(itemList);
        for (PropertySheet.Item item : itemList) {
            if (item instanceof PreferenceChanged) {
                PreferenceChanged preferenceChangedItem = (PreferenceChanged) item;
                preferenceChangedItem.changedProperty().addListener((obs, oldValue, newValue) -> {
                        if (newValue) {
                            changed.set(true);
                            preferenceChangedItem.changedProperty().set(false);
                        }
                    });
            } else {
                Optional<ObservableValue<? extends Object>> observable = item.getObservableValue();
                if (observable.isPresent()) {
                    observable.get().addListener((obs, oldValue, newValue) -> {
                        validateChange(oldValue, newValue);
                    });
                }
            }
        }
        this.propertySheetBorderPane.setCenter(sheet);
    }

    private void validateChange(Object oldValue, Object newValue) {
        if (oldValue != newValue) {
        if (newValue != null) {
            if (!newValue.equals(oldValue)) {
                changed.set(true);
            }
        } else {
            changed.set(true);
        }
    }
    }

    @Override
    public Node getCenterPanel(ViewProperties viewProperties) {
        if (this.propertySheet == null) {
            makePropertySheet();
        }
        return this.propertySheetBorderPane;
    }

    protected final void addProperty(ObservableValue<?> observableValue) {
        observableValue.addListener(new WeakChangeListener<>((observable, oldValue, newValue) -> {
            validateChange(oldValue, newValue);
        }));
    }

    protected final void addProperty(ObservableList<? extends Object> observableList) {
        observableList.addListener(new WeakListChangeListener<>((ListChangeListener.Change<? extends Object> c) -> {
            changed.set(true);
        }));
    }

    protected PropertySheetItem createPropertyItem(Property<?> property) {
        PropertySheetItem wrappedProperty = new PropertySheetItem(property.getValue(), property, viewProperties.getManifoldCoordinate(), PropertySheetPurpose.DESCRIPTION_DIALECT);
        return wrappedProperty;
    }

    @Override
    public Node getTopPanel(ViewProperties viewProperties) {
        return null;
    }

    @Override
    public Node getLeftPanel(ViewProperties viewProperties) {
        return null;
    }

    @Override
    public Node getBottomPanel(ViewProperties viewProperties) {
        if (showRevertAndSave()) {
            if (showDelete()) {
                bottomBar = new ToolBar(deleteButton, spacer, revertButton, saveButton);
            } else {
                bottomBar = new ToolBar(spacer, revertButton, saveButton);
            }
            return this.bottomBar;
        }
        return null;
    }

    @Override
    public Node getRightPanel(ViewProperties viewProperties) {
        return null;
    }

    public ManifoldCoordinate getManifoldCoordinate() {
        return viewProperties.getManifoldCoordinate();
    }

    public ViewProperties getViewProperties() {
        return viewProperties;
    }

    /**
     * Override for panels that have not state, such as parentTreeItem panels
     * with no fields.
     *
     * @return true if the revert and save buttons should be shown.
     */
    public boolean showRevertAndSave() {
        return true;
    }

    public boolean showDelete() {
        return false;
    }

}
