/* 
 * Licensed under the Apache License, Version 2.0 (the "License");
 *
 * You may not use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributions from 2013-2017 where performed either by US government 
 * employees, or under US Veterans Health Administration contracts. 
 *
 * US Veterans Health Administration contributions by government employees
 * are work of the U.S. Government and are not subject to copyright
 * protection in the United States. Portions contributed by government 
 * employees are USGovWork (17USC §105). Not subject to copyright. 
 * 
 * Contribution by contractors to the US Veterans Health Administration
 * during this period are contractually contributed under the
 * Apache License, Version 2.0.
 *
 * See: https://www.usa.gov/government-works
 * 
 * Contributions prior to 2013:
 *
 * Copyright (C) International Health Terminology Standards Development Organisation.
 * Licensed under the Apache License, Version 2.0.
 *
 */



package sh.komet.gui.search;

//~--- JDK imports ------------------------------------------------------------

/*
* Copyright 2017 Organizations participating in ISAAC, ISAAC's KOMET, and SOLOR development include the
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
import java.net.URL;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.ResourceBundle;

//~--- non-JDK imports --------------------------------------------------------

import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.SimpleStringProperty;

import javafx.collections.ObservableList;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.scene.Node;

import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.scene.control.cell.TreeItemPropertyValueFactory;
import javafx.scene.layout.*;


//~--- JDK imports ------------------------------------------------------------

import javax.validation.constraints.NotNull;

//~--- non-JDK imports --------------------------------------------------------

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.action.Action;
import org.controlsfx.control.action.ActionGroup;
import org.controlsfx.control.action.ActionUtils;

import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.observable.ObservableSnapshotService;
import sh.isaac.api.observable.sememe.version.ObservableDescriptionVersion;
import sh.isaac.api.query.Clause;
import sh.isaac.api.query.ComponentCollectionTypes;
import sh.isaac.api.query.Or;
import sh.isaac.api.query.ParentClause;
import sh.isaac.api.query.Query;
import sh.isaac.api.query.QueryBuilder;
import sh.isaac.api.query.clauses.DescriptionLuceneMatch;

import sh.komet.gui.action.ConceptAction;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.search.control.LetPropertySheet;
import sh.komet.gui.style.StyleClasses;

import static sh.isaac.api.query.QueryBuilder.DEFAULT_MANIFOLD_COORDINATE_KEY;
import sh.komet.gui.drag.drop.DragDetectedCellEventHandler;
import sh.komet.gui.drag.drop.DragDoneEventHandler;
import sh.komet.gui.table.DescriptionTableCell;
import sh.komet.gui.util.FxGet;

//~--- classes ----------------------------------------------------------------

public class QueryController
         implements ExplorationNode {
   private static final String CLAUSE                = "clause";
   public static final boolean OUTPUT_CSS_STYLE_INFO = false;

   //~--- fields --------------------------------------------------------------

   private final SimpleStringProperty toolTipProperty = new SimpleStringProperty("FLOWR query view");
   @FXML  // ResourceBundle that was given to the FXMLLoader
   private ResourceBundle                                     resources;
   @FXML  // URL location of the FXML file that was given to the FXMLLoader
   private URL                                                location;
   @FXML                                                                         // fx:id="anchorPane"
   private AnchorPane                                         anchorPane;        // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="flowrAccordian"
   private Accordion                                          flowrAccordian;    // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="forPane"
   private TitledPane                                         forPane;           // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="letPane"
   private TitledPane                                         letPane;           // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="orderPane"
   private TitledPane                                         orderPane;         // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="wherePane"
   private TitledPane                                         wherePane;         // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="whereTreeTable"
   private TreeTableView<QueryClause>                         whereTreeTable;    // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="clauseNameColumn"
   private TreeTableColumn<QueryClause, String>               clauseNameColumn;  // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="parameterColumn"
   private TreeTableColumn<QueryClause, String>               parameterColumn;   // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="returnPane"
   private TitledPane                                         returnPane;        // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="executeButton"
   private Button                                             executeButton;     // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="progressBar"
   private ProgressBar                                        progressBar;       // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="cancelButton"
   private Button                                             cancelButton;      // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="resultTable"
   private TableView<ObservableDescriptionVersion>            resultTable;       // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="textColumn"
   private TableColumn<ObservableDescriptionVersion, String>  textColumn;        // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="typeColumn"
   private TableColumn<ObservableDescriptionVersion, Integer> typeColumn;        // Value injected by FXMLLoader
   @FXML                                                                         // fx:id="languageColumn"
   private TableColumn<ObservableDescriptionVersion, Integer> languageColumn;    // Value injected by FXMLLoader
   @FXML
   private RadioButton                                        allComponents;
   @FXML
   private ToggleGroup                                        forGroup;
   @FXML
   private RadioButton                                        allConcepts;
   @FXML
   private RadioButton                                        allDescriptions;
   @FXML
   private RadioButton                                        allSememes;
   @FXML
   private AnchorPane                                         letAnchorPane;
   private TreeItem<QueryClause>                              root;
   private Manifold                                           manifold;

   private LetPropertySheet                                   letPropertySheet;


   

   //~--- methods -------------------------------------------------------------

   void displayResults(NidSet descriptionNids) {
      ObservableList<ObservableDescriptionVersion> tableItems = resultTable.getItems();

      tableItems.clear();

      ObservableSnapshotService snapshot = Get.observableSnapshotService(manifold);

      descriptionNids.stream()
                     .forEach(
                         (nid) -> {
                            LatestVersion<ObservableDescriptionVersion> latestDescription =
                               (LatestVersion<ObservableDescriptionVersion>) snapshot.getObservableSememeVersion(
                                   nid);

                            if (latestDescription.isPresent()) {
                               tableItems.add(latestDescription.get());
                            }
                         });
   }

   @FXML
   void executeQuery(ActionEvent event) {
      QueryBuilder queryBuilder = new QueryBuilder(this.manifold);

      if (allComponents.isSelected()) {
         queryBuilder.from(ComponentCollectionTypes.ALL_COMPONENTS);
      }

      if (allConcepts.isSelected()) {
         queryBuilder.from(ComponentCollectionTypes.ALL_CONCEPTS);
      }

      if (allDescriptions.isSelected()) {
         queryBuilder.from(ComponentCollectionTypes.ALL_SEMEMES);
      }

      if (allSememes.isSelected()) {
         queryBuilder.from(ComponentCollectionTypes.ALL_SEMEMES);
      }

      TreeItem<QueryClause> itemToProcess = this.root;
      Clause                rootClause    = itemToProcess.getValue()
                                                         .getClause();

      queryBuilder.setWhereRoot((ParentClause) rootClause);
      processQueryTreeItem(itemToProcess, queryBuilder);

      Query query = queryBuilder.build();

      rootClause.setEnclosingQuery(query);

      NidSet results = query.compute();

      FxGet.statusMessageService().reportSceneStatus(anchorPane.getScene(), "Query result count: " + results.size());
      displayResults(results);
   }

   @FXML  // This method is called by the FXMLLoader when initialization is complete
   void initialize() {
      assert anchorPane != null: "fx:id=\"anchorPane\" was not injected: check your FXML file 'Query.fxml'.";
      assert flowrAccordian != null: "fx:id=\"flowrAccordian\" was not injected: check your FXML file 'Query.fxml'.";
      assert forPane != null: "fx:id=\"forPane\" was not injected: check your FXML file 'Query.fxml'.";
      assert allComponents != null: "fx:id=\"allComponents\" was not injected: check your FXML file 'Query.fxml'.";
      assert forGroup != null: "fx:id=\"forGroup\" was not injected: check your FXML file 'Query.fxml'.";
      assert allConcepts != null: "fx:id=\"allConcepts\" was not injected: check your FXML file 'Query.fxml'.";
      assert allDescriptions != null: "fx:id=\"allDescriptions\" was not injected: check your FXML file 'Query.fxml'.";
      assert allSememes != null: "fx:id=\"allSememes\" was not injected: check your FXML file 'Query.fxml'.";
      assert letPane != null: "fx:id=\"letPane\" was not injected: check your FXML file 'Query.fxml'.";
      assert orderPane != null: "fx:id=\"orderPane\" was not injected: check your FXML file 'Query.fxml'.";
      assert wherePane != null: "fx:id=\"wherePane\" was not injected: check your FXML file 'Query.fxml'.";
      assert whereTreeTable != null: "fx:id=\"whereTreeTable\" was not injected: check your FXML file 'Query.fxml'.";
      assert clauseNameColumn != null:
             "fx:id=\"clauseNameColumn\" was not injected: check your FXML file 'Query.fxml'.";
      assert parameterColumn != null: "fx:id=\"parameterColumn\" was not injected: check your FXML file 'Query.fxml'.";
      assert returnPane != null: "fx:id=\"returnPane\" was not injected: check your FXML file 'Query.fxml'.";
      assert executeButton != null: "fx:id=\"executeButton\" was not injected: check your FXML file 'Query.fxml'.";
      assert progressBar != null: "fx:id=\"progressBar\" was not injected: check your FXML file 'Query.fxml'.";
      assert cancelButton != null: "fx:id=\"cancelButton\" was not injected: check your FXML file 'Query.fxml'.";
      assert resultTable != null: "fx:id=\"resultTable\" was not injected: check your FXML file 'Query.fxml'.";
      assert textColumn != null: "fx:id=\"textColumn\" was not injected: check your FXML file 'Query.fxml'.";
      assert typeColumn != null: "fx:id=\"typeColumn\" was not injected: check your FXML file 'Query.fxml'.";
      assert languageColumn != null: "fx:id=\"languageColumn\" was not injected: check your FXML file 'Query.fxml'.";
      assert letAnchorPane != null: "fx:id=\"letAnchorPane\" was not injected: check your FXML file 'Query.fxml'.";
      
      textColumn.setCellValueFactory((TableColumn.CellDataFeatures<ObservableDescriptionVersion, String> param) -> param.getValue().textProperty());
      textColumn.setCellFactory((TableColumn<ObservableDescriptionVersion, String> stringText) -> new DescriptionTableCell());
      resultTable.setOnDragDetected(new DragDetectedCellEventHandler());
      resultTable.setOnDragDone(new DragDoneEventHandler());

   }

   private void addChildClause(ActionEvent event, TreeTableRow<QueryClause> rowValue) {
      TreeItem<QueryClause> treeItem = rowValue.getTreeItem();

      System.out.println(event.getSource()
                              .getClass());

      ConceptAction conceptAction = (ConceptAction) ((MenuItem) event.getSource()).getOnAction();
      Clause        clause        = (Clause) conceptAction.getProperties()
                                                          .get(CLAUSE);

      treeItem.getChildren()
              .add(new TreeItem<>(new QueryClause(clause, manifold)));
   }

   private void addSiblingClause(ActionEvent event, TreeTableRow<QueryClause> rowValue) {
      TreeItem<QueryClause> treeItem = rowValue.getTreeItem();

      System.out.println(event.getSource()
                              .getClass());

      ConceptAction conceptAction = (ConceptAction) ((MenuItem) event.getSource()).getOnAction();
      Clause        clause        = (Clause) conceptAction.getProperties()
                                                          .get(CLAUSE);

      treeItem.getParent()
              .getChildren()
              .add(new TreeItem<>(new QueryClause(clause, manifold)));
   }

   private void changeClause(ActionEvent event, TreeTableRow<QueryClause> rowValue) {
      TreeItem<QueryClause> treeItem = rowValue.getTreeItem();

      System.out.println(event.getSource()
                              .getClass());

      ConceptAction conceptAction = (ConceptAction) ((MenuItem) event.getSource()).getOnAction();
      Clause        clause        = (Clause) conceptAction.getProperties()
                                                          .get(CLAUSE);

      treeItem.setValue(new QueryClause(clause, manifold));
   }

   // changeClause->, addSibling->, addChild->,
   private void deleteClause(ActionEvent event, TreeTableRow<QueryClause> rowValue) {
      TreeItem<QueryClause> treeItem = rowValue.getTreeItem();

      treeItem.getParent()
              .getChildren()
              .remove(treeItem);
   }

   private void outputStyleInfo(String prefix, TreeTableCell nodeToStyle) {
      // System.out.println(prefix + " css metadata: " + nodeToStyle.getCssMetaData());
      // System.out.println(prefix + " style: " + nodeToStyle.getStyle());
      System.out.println(prefix + " style classes: " + nodeToStyle.getStyleClass());
   }

   /**
    * Recursive depth-first walk through the tree nodes.
    * @param itemToProcess
    */
   private void processQueryTreeItem(TreeItem<QueryClause> itemToProcess, QueryBuilder queryBuilder) {
      Clause clause = itemToProcess.getValue()
                                   .getClause();

      if (itemToProcess.isLeaf()) {
         String parameter = itemToProcess.getValue().parameter
                                         .getValue();
         int    row       = whereTreeTable.getRow(itemToProcess);

         switch (clause.getClass()
                       .getSimpleName()) {
         case "DescriptionLuceneMatch":
            if (parameter == null) {
               throw new IllegalStateException("Parameter cannot be null for DescriptionLuceneMatch");
            }

            DescriptionLuceneMatch descriptionLuceneMatch = (DescriptionLuceneMatch) clause;
            String                 parameterKey = clause.getClass()
                                                        .getSimpleName() + "-" + queryBuilder.getSequence();

            descriptionLuceneMatch.setLuceneMatchKey(parameterKey);
            queryBuilder.let(parameterKey, parameter);
            descriptionLuceneMatch.setViewCoordinateKey(DEFAULT_MANIFOLD_COORDINATE_KEY);
            break;
         }
      } else {
         ParentClause parent = (ParentClause) clause;

         itemToProcess.getChildren().stream().map((child) -> {
            parent.getChildren()
                    .add(child.getValue()
                            .getClause());
            return child;
         }).forEachOrdered((child) -> {
            processQueryTreeItem(child, queryBuilder);
         });
      }
   }

   private Collection<? extends Action> setupContextMenu(final TreeTableRow<QueryClause> rowValue) {
      // Firstly, create a list of Actions
      ArrayList<Action>           actionList = new ArrayList();
      final TreeItem<QueryClause> treeItem   = rowValue.getTreeItem();

      if (treeItem != null) {
         QueryClause clause = treeItem.getValue();

         if (clause != null) {
            Clause[] siblings     = clause.getClause()
                                          .getAllowedSiblingClauses();
            Clause[] children     = clause.getClause()
                                          .getAllowedChildClauses();
            Clause[] substitution = clause.getClause()
                                          .getAllowedSubstutitionClauses();

            if (siblings.length > 0) {
               ConceptAction[] actions = new ConceptAction[siblings.length];

               for (int i = 0; i < siblings.length; i++) {
                  actions[i] = new ConceptAction(
                      siblings[i],
                          (ActionEvent event) -> {
                             addSiblingClause(event, rowValue);
                          });
                  actions[i].getProperties()
                            .put(CLAUSE, siblings[i]);
               }

               actionList.add(new ActionGroup("add sibling", actions));
            }

            if (children.length > 0) {
               ConceptAction[] actions = new ConceptAction[children.length];

               for (int i = 0; i < children.length; i++) {
                  actions[i] = new ConceptAction(
                      children[i],
                          (ActionEvent event) -> {
                             addChildClause(event, rowValue);
                          });
                  actions[i].getProperties()
                            .put(CLAUSE, children[i]);
               }

               actionList.add(new ActionGroup("add child", actions));
            }

            if (substitution.length > 0) {
               ConceptAction[] actions = new ConceptAction[substitution.length];

               for (int i = 0; i < substitution.length; i++) {
                  actions[i] = new ConceptAction(
                      substitution[i],
                          (ActionEvent event) -> {
                             changeClause(event, rowValue);
                          });
                  actions[i].getProperties()
                            .put(CLAUSE, substitution[i]);
               }

               actionList.add(new ActionGroup("change this clause", actions));
            }

            if ((treeItem.getParent() != this.root) || (this.root.getChildren().size() > 1)) {
               Action deleteAction = new Action(
                                         "delete this clause",
                                             (ActionEvent event) -> {
                                                deleteClause(event, rowValue);
                                             });

               // deleteAction.setGraphic(GlyphFonts.fontAwesome().create('\uf013').color(Color.CORAL).size(28));
               actionList.add(deleteAction);
            }
         }
      }

      return actionList;
   }

   private void updateStyle(@NotNull String item,
                            boolean empty,
                            TreeTableRow<QueryClause> ttr,
                            TreeTableCell nodeToStyle) {
      if (empty) {
         Arrays.stream(StyleClasses.values())
               .forEach(styleClass -> ttr.getStyleClass()
                                         .remove(styleClass.toString()));
      } else {
         if (ttr.getItem() != null) {
            ConceptSpecification clauseConcept = ttr.getItem()
                                                    .getClause()
                                                    .getClauseConcept();

            if (clauseConcept.equals(TermAux.AND_QUERY_CLAUSE)) {
               ttr.getStyleClass()
                  .remove(StyleClasses.OR_CLAUSE.toString());
               ttr.getStyleClass()
                  .add(StyleClasses.AND_CLAUSE.toString());
            } else if (clauseConcept.equals(TermAux.OR_QUERY_CLAUSE)) {
               ttr.getStyleClass()
                  .add(StyleClasses.OR_CLAUSE.toString());
               ttr.getStyleClass()
                  .remove(StyleClasses.AND_CLAUSE.toString());
            }
         }

         TreeItem<QueryClause> rowItem = nodeToStyle.getTreeTableRow()
                                                    .getTreeItem();

         if (rowItem != null) {
            TreeItem<QueryClause> parentItem    = rowItem.getParent();
            ConceptSpecification  parentConcept = parentItem.getValue()
                                                            .getClause()
                                                            .getClauseConcept();

            if (parentConcept.equals(TermAux.AND_QUERY_CLAUSE)) {
               ttr.getStyleClass()
                  .remove(StyleClasses.OR_CLAUSE_CHILD.toString());
               ttr.getStyleClass()
                  .add(StyleClasses.AND_CLAUSE_CHILD.toString());
            } else if (parentConcept.equals(TermAux.OR_QUERY_CLAUSE)) {
               ttr.getStyleClass()
                  .add(StyleClasses.OR_CLAUSE_CHILD.toString());
               ttr.getStyleClass()
                  .remove(StyleClasses.AND_CLAUSE_CHILD.toString());
            }
         }
      }
   }

   //~--- get methods ---------------------------------------------------------

   @Override
   public Manifold getManifold() {
      return this.manifold;
   }

   //~--- set methods ---------------------------------------------------------

   public void setManifold(Manifold manifold) {
      this.manifold = manifold;
      this.root     = new TreeItem<>(new QueryClause(Clause.getRootClause(), manifold));

      TreeItem orTreeItem = new TreeItem<>(new QueryClause(new Or(), manifold));

      orTreeItem.getChildren()
                .add(new TreeItem<>(new QueryClause(new DescriptionLuceneMatch(), manifold)));
      this.root.getChildren()
               .add(orTreeItem);
      orTreeItem.setExpanded(true);
      this.clauseNameColumn.setCellFactory(
          (TreeTableColumn<QueryClause, String> p) -> {
             TreeTableCell<QueryClause, String> cell = new TreeTableCell<QueryClause, String>() {
                @Override
                public void updateItem(String item, boolean empty) {
                   super.updateItem(item, empty);
                   setText(item);

                   TreeTableRow<QueryClause> rowValue = this.tableRowProperty()
                                                            .getValue();

                   updateStyle(item, empty, getTreeTableRow(), this);

                   if ((item != null) && OUTPUT_CSS_STYLE_INFO) {
                      outputStyleInfo("updateItem: " + item, this);
                   }

                   setContextMenu(ActionUtils.createContextMenu(setupContextMenu(rowValue)));
                }
             };

             return cell;
          });

      // Given the data in the row, return the observable value for the column.
      this.clauseNameColumn.setCellValueFactory(
          (TreeTableColumn.CellDataFeatures<QueryClause, String> p) -> p.getValue()
                .getValue().clauseName);
      this.parameterColumn.setCellFactory(TextFieldTreeTableCell.forTreeTableColumn());
      this.parameterColumn.setCellValueFactory(new TreeItemPropertyValueFactory("parameter"));
      this.whereTreeTable.setRoot(root);
      this.textColumn.setCellValueFactory(new PropertyValueFactory("text"));
      this.typeColumn.setCellValueFactory(new PropertyValueFactory("descriptionTypeConceptSequence"));
      this.typeColumn.setCellFactory(
          column -> {
             return new TableCell<ObservableDescriptionVersion, Integer>() {
                @Override
                protected void updateItem(Integer conceptSequence, boolean empty) {
                   super.updateItem(conceptSequence, empty);

                   if ((conceptSequence == null) || empty) {
                      setText(null);
                      setStyle("");
                   } else {
                      setText(manifold.getPreferredDescriptionText(conceptSequence));
                   }
                }
             };
          });
      this.languageColumn.setCellValueFactory(new PropertyValueFactory("languageConceptSequence"));

      // TODO: make concept description cell factory...
      this.languageColumn.setCellFactory(
          column -> {
             return new TableCell<ObservableDescriptionVersion, Integer>() {
                @Override
                protected void updateItem(Integer conceptSequence, boolean empty) {
                   super.updateItem(conceptSequence, empty);

                   if ((conceptSequence == null) || empty) {
                      setText(null);
                      setStyle("");
                   } else {
                      setText(manifold.getPreferredDescriptionText(conceptSequence));
                   }
                }
             };
          });
      resultTable.getSelectionModel()
                 .selectedItemProperty()
                 .addListener(
                     (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                           manifold.setFocusedConceptChronology(
                                   Get.conceptService().getConcept(newSelection.getReferencedComponentNid()));
                        }
                     });

      letPropertySheet = new LetPropertySheet(this.manifold.deepClone());
      this.letAnchorPane.getChildren().add(letPropertySheet.getPropertySheet());
   }

   @Override
   public Node getNode() {
      flowrAccordian.setExpandedPane(wherePane);
      return anchorPane;
   }

   //~--- get methods ---------------------------------------------------------

   @Override
   public ReadOnlyProperty<String> getToolTip() {
      return toolTipProperty;
   }
}

