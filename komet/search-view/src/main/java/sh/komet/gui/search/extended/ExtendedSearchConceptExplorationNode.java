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
package sh.komet.gui.search.extended;

import java.util.Optional;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.query.CompositeQueryResult;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.control.property.ActivityFeed;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.interfaces.ConceptExplorationNode;
import sh.komet.gui.interfaces.ExplorationNodeAbstract;
import sh.komet.gui.search.simple.SimpleSearchViewFactory;

/**
 *
 * @author kec
 */
public class ExtendedSearchConceptExplorationNode extends ExplorationNodeAbstract implements ConceptExplorationNode {

    {
        titleProperty.setValue("Extended search");
        toolTipProperty.setValue("Extended search panel. ");
        menuIconProperty.setValue(Iconography.TARGET.getIconographic());
    }

    private final ExtendedSearchViewController controller;
    private final SimpleObjectProperty<ConceptSpecification> conceptSpecification = new SimpleObjectProperty<>();

    public ExtendedSearchConceptExplorationNode(ExtendedSearchViewController controller, ViewProperties viewProperties) {
        super(viewProperties);
        this.controller = controller;
        controller.getSearchResults().getSelectionModel().selectedItemProperty().addListener(this::selectedSearchResultChanged);
    }

    @Override
    public Node getMenuIconGraphic() {
        return Iconography.TARGET.getIconographic();
    }

    @Override
    public void revertPreferences() {

    }

    @Override
    public void savePreferences() {

    }

    @Override
    public Optional<Node> getTitleNode() {
        return Optional.empty();
    }

    @Override
    public Node getNode() {
        return controller.getRoot();
    }

    private void selectedSearchResultChanged(ObservableValue<? extends CompositeQueryResult> observable, CompositeQueryResult oldValue, CompositeQueryResult newValue) {
        if (newValue != null) {
            conceptSpecification.setValue(newValue.getContainingConcept());
        } else {
            conceptSpecification.set(null);
        }
    }

    @Override
    public ReadOnlyObjectProperty<ConceptSpecification> selectedConceptSpecification() {
        return conceptSpecification;
    }

    @Override
    public void focusOnInput() {
        controller.getSearchText().requestFocus();
        controller.getSearchText().selectAll();
    }

    @Override
    public void focusOnResults() {
        controller.getSearchResults().requestFocus();
        controller.getSearchResults().getSelectionModel().selectFirst();
    }


    @Override
    public void close() {
        // nothing to do...
    }

    @Override
    public boolean canClose() {
        return true;
    }

    @Override
    public ActivityFeed getActivityFeed() {
        throw new UnsupportedOperationException();
    }


}

