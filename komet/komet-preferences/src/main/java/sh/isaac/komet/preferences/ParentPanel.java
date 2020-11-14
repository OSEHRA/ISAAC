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

import java.util.Optional;
import java.util.Stack;
import java.util.UUID;
import java.util.prefs.BackingStoreException;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ToolBar;
import javafx.stage.Stage;
import sh.isaac.api.preferences.IsaacPreferences;

import static sh.komet.gui.contract.preferences.PreferenceGroup.Keys.CHILDREN_NODES;
import static sh.komet.gui.contract.preferences.PreferenceGroup.Keys.GROUP_NAME;

import sh.komet.gui.contract.preferences.KometPreferencesController;
import sh.komet.gui.contract.preferences.PreferencesTreeItem;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.menu.MenuItemWithText;
import sh.komet.gui.util.FxGet;

/**
 *
 * @author kec
 */
public abstract class ParentPanel extends AbstractPreferences {

    Stack<PreferencesTreeItem> childrenToAdd = new Stack<>();
    
    public ParentPanel(IsaacPreferences preferencesNode, String groupName,
                       ViewProperties viewProperties, KometPreferencesController kpc) {
        super(preferencesNode, groupName, viewProperties, kpc);
    }

    @Override
    protected final void addChildren() {
        while (!childrenToAdd.empty()) {
            getTreeItem().getChildren().add(childrenToAdd.pop());
            getTreeItem().setExpanded(true);
        }
    }
    
    protected final void removeChild(AbstractPreferences child) {
        this.getTreeItem().removeChild(child.preferencesNode.name());
        save();
    }

    protected final void newChild(ActionEvent action) {
        UUID newUuid = UUID.randomUUID();
        addChildPanel(newUuid, Optional.empty());
    }
    
    final protected IsaacPreferences addChildPanel(UUID childUuid, Optional<String> groupName) {
        IsaacPreferences preferencesNode = getPreferencesNode().node(childUuid.toString());
        if (groupName.isPresent()) {
            preferencesNode.put(GROUP_NAME, groupName.get());
        }
        addChild(childUuid.toString(), getChildClass());
        Optional<PreferencesTreeItem> optionalActionItem = PreferencesTreeItem.from(preferencesNode,
                getViewProperties(), kpc);
        if (optionalActionItem.isPresent()) {
            PreferencesTreeItem actionItem = optionalActionItem.get();
            if (getTreeItem() == null) {
                childrenToAdd.push(actionItem);
            } else {
                getTreeItem().getChildren().add(actionItem);
                getTreeItem().setExpanded(true);
                actionItem.select();
            }
        }
        save();
        return preferencesNode;
    }
    
    abstract protected Class getChildClass();
    
    @Override
    public Node getTopPanel(ViewProperties viewProperties) {
        Button addButton = new Button("Add");
        addButton.setOnAction(this::newChild);
        ToolBar toolbar = new ToolBar(addButton);
        MenuItem resetUserItems = new MenuItemWithText("Clear user items");
        resetUserItems.setOnAction(this::resetUserItems);
        MenuItem resetConfigurationAndUserItems = new MenuItemWithText("Clear user and child items");
        resetConfigurationAndUserItems.setOnAction(this::resetConfigurationAndUserItems);
        toolbar.setContextMenu(new ContextMenu(resetUserItems, resetConfigurationAndUserItems));
        return toolbar;
    }
    
    private void resetUserItems(ActionEvent actionEvent) {
        FxGet.kometPreferences().resetUserPreferences();
        Stage stage = (Stage) this.getTreeItem().getController().getPreferenceTree().getScene().getWindow();
        stage.close();
    } 

    private void resetConfigurationAndUserItems(ActionEvent actionEvent) {
        try {
            IsaacPreferences configurationNode = FxGet.kometConfigurationRootNode().node(getPreferencesNode().absolutePath());
            configurationNode.remove(CHILDREN_NODES);
            configurationNode.sync();
            FxGet.kometPreferences().resetUserPreferences();
            FxGet.kometPreferences().closePreferences();
        } catch (BackingStoreException ex) {
           throw new RuntimeException(ex);
        }
    } 
    
    @Override
    public boolean showRevertAndSave() {
        return false;
    }

 }
