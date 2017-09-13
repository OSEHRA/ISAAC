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
package sh.komet.gui.control;

//~--- JDK imports ------------------------------------------------------------
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.application.Platform;

//~--- non-JDK imports --------------------------------------------------------
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.text.Text;

import org.apache.mahout.math.map.OpenIntIntHashMap;
import org.controlsfx.control.PropertySheet;

import sh.isaac.api.Get;
import sh.isaac.api.State;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.component.sememe.SememeChronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.sememe.version.ComponentNidVersion;
import sh.isaac.api.component.sememe.version.DescriptionVersion;
import sh.isaac.api.component.sememe.version.LogicGraphVersion;
import sh.isaac.api.component.sememe.version.LongVersion;
import sh.isaac.api.component.sememe.version.SememeVersion;
import sh.isaac.api.component.sememe.version.StringVersion;
import sh.isaac.api.observable.ObservableCategorizedVersion;
import sh.isaac.api.observable.ObservableVersion;
import sh.isaac.komet.iconography.Iconography;

import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.state.ExpandAction;
import sh.komet.gui.style.PseudoClasses;
import sh.komet.gui.style.StyleClasses;

import static sh.komet.gui.style.StyleClasses.ADD_ATTACHMENT;
import sh.komet.gui.util.FxGet;

//~--- classes ----------------------------------------------------------------
/**
 *
 * @author kec
 */
public abstract class BadgedVersionPanel
        extends Pane {

   public static final int FIRST_COLUMN_WIDTH = 32;

   //~--- fields --------------------------------------------------------------
   protected final int badgeWidth = 25;
   protected final ArrayList<Node> badges = new ArrayList<>();
   protected int columns = 10;
   protected final Text componentText = new Text();
   protected final Text componentType = new Text();
   protected final MenuButton editControl = new MenuButton("", Iconography.EDIT_PENCIL.getIconographic());
   protected final MenuButton addAttachmentControl = new MenuButton("", Iconography.combine(Iconography.PLUS, Iconography.PAPERCLIP));
   protected final ExpandControl expandControl = new ExpandControl();
   protected final GridPane gridpane = new GridPane();
   protected final SimpleBooleanProperty isConcept = new SimpleBooleanProperty(false);
   protected final SimpleBooleanProperty isContradiction = new SimpleBooleanProperty(false);
   protected final SimpleBooleanProperty isDescription = new SimpleBooleanProperty(false);
   protected final SimpleBooleanProperty isInactive = new SimpleBooleanProperty(false);
   protected final SimpleBooleanProperty isLogicalDefinition = new SimpleBooleanProperty(false);
   protected final int rowHeight = 25;
   protected final StampControl stampControl = new StampControl();
   protected int wrappingWidth = 300;
   protected final ObservableList<ComponentPanel> extensionPanels = FXCollections.observableArrayList();
   protected final ObservableList<VersionPanel> versionPanels = FXCollections.observableArrayList();
   protected final CheckBox revertCheckBox = new CheckBox();
   private final ObservableCategorizedVersion categorizedVersion;
   private final Manifold manifold;
   protected int rows;
   private Optional<PropertySheetMenuItem> optionalPropertySheetMenuItem = Optional.empty();
   private final Button cancelButton = new Button("Cancel");
   private final Button commitButton = new Button("Commit");

   //~--- initializers --------------------------------------------------------
   {
      isDescription.addListener(this::pseudoStateChanged);
      isInactive.addListener(this::pseudoStateChanged);
      isConcept.addListener(this::pseudoStateChanged);
      isLogicalDefinition.addListener(this::pseudoStateChanged);
      isContradiction.addListener(this::pseudoStateChanged);
   }

   //~--- constructors --------------------------------------------------------
   public BadgedVersionPanel(Manifold manifold,
           ObservableCategorizedVersion categorizedVersion,
           OpenIntIntHashMap stampOrderHashMap) {
      this.manifold = manifold;
      this.categorizedVersion = categorizedVersion;
      isInactive.set(categorizedVersion.getState() == State.INACTIVE);
      expandControl.expandActionProperty()
              .addListener(this::expand);
      this.getChildren()
              .add(gridpane);
      componentType.getStyleClass()
              .add(StyleClasses.COMPONENT_VERSION_WHAT_CELL.toString());
      componentText.getStyleClass()
              .add(StyleClasses.COMPONENT_TEXT.toString());
      componentText.setWrappingWidth(wrappingWidth);
      componentText.layoutBoundsProperty()
              .addListener(this::textLayoutChanged);
      componentText.layoutBoundsProperty().addListener(this::debugTextLayoutListener);
      isInactive.set(this.categorizedVersion.getState() != State.ACTIVE);
      this.stampControl.setStampedVersion(
              categorizedVersion.getStampSequence(),
              manifold,
              stampOrderHashMap.get(categorizedVersion.getStampSequence()));
      badges.add(this.stampControl);
      this.widthProperty()
              .addListener(this::widthChanged);

      ObservableVersion observableVersion = categorizedVersion.getObservableVersion();

      addAttachmentControl.getStyleClass()
              .setAll(ADD_ATTACHMENT.toString());
      addAttachmentControl.getItems().addAll(getAttachmentMenuItems());
      addAttachmentControl.setVisible(!addAttachmentControl.getItems().isEmpty());
      editControl.getStyleClass()
              .setAll(StyleClasses.EDIT_COMPONENT_BUTTON.toString());
      editControl.getItems().addAll(getEditMenuItems());
      editControl.setVisible(!editControl.getItems().isEmpty());
      
      cancelButton.getStyleClass()
              .add(StyleClasses.CANCEL_BUTTON.toString());
      cancelButton.setOnAction(this::cancel);
      commitButton.getStyleClass()
              .add(StyleClasses.COMMIT_BUTTON.toString());
      commitButton.setOnAction(this::commit);
      cancelButton.setVisible(false);
      commitButton.setVisible(false);

      if (observableVersion instanceof DescriptionVersion) {
         isDescription.set(true);
         setupDescription((DescriptionVersion) observableVersion);
      } else if (observableVersion instanceof ConceptVersion) {
         isConcept.set(true);
         setupConcept((ConceptVersion) observableVersion);
      } else if (observableVersion instanceof LogicGraphVersion) {
         isLogicalDefinition.set(true);
         setupDef((LogicGraphVersion) observableVersion);
      } else {
         setupOther(observableVersion);
      }
   }
   
   private void cancel(ActionEvent event) {
      System.out.println("cancel");
      if (optionalPropertySheetMenuItem.isPresent()) {
         PropertySheetMenuItem item = optionalPropertySheetMenuItem.get();
         item.cancel();
         cancelButton.setVisible(false);
         commitButton.setVisible(false);
         gridpane.getChildren().remove(item.getPropertySheet());
         optionalPropertySheetMenuItem = Optional.empty();
         pseudoClassStateChanged(PseudoClasses.UNCOMMITTED_PSEUDO_CLASS, false);
         editControl.getItems().setAll(getEditMenuItems());
         editControl.setVisible(!editControl.getItems().isEmpty());
        redoLayout();
      } 
   }
   
   private void commit(ActionEvent event) {
     System.out.println("commit");
      if (optionalPropertySheetMenuItem.isPresent()) {
         PropertySheetMenuItem item = optionalPropertySheetMenuItem.get();
         item.commit();
         cancelButton.setVisible(false);
         commitButton.setVisible(false);
         gridpane.getChildren().remove(item.getPropertySheet());
         optionalPropertySheetMenuItem = Optional.empty();
         pseudoClassStateChanged(PseudoClasses.UNCOMMITTED_PSEUDO_CLASS, false);
         editControl.getItems().setAll(getEditMenuItems());
         editControl.setVisible(!editControl.getItems().isEmpty());
         redoLayout();
      }
    }

   public void debugTextLayoutListener(ObservableValue<? extends Bounds> bounds, Bounds oldBounds, Bounds newBounds) {
      if (this.getParent() != null && componentText.getText().startsWith("SNOMED CT has been")) {
         System.out.println("SCT has been layout: " + newBounds + "\n panel bounds: " + this.getLayoutBounds());
         if (newBounds.getHeight() >= this.getLayoutBounds().getHeight()) {
            this.setMinHeight(newBounds.getHeight());
            this.setPrefHeight(newBounds.getHeight());
            this.setHeight(newBounds.getHeight());
            Platform.runLater(() -> this.getParent().requestLayout());
            System.out.println("Requested layout ");
         }
      }
   }
   //~--- methods -------------------------------------------------------------

   public final List<MenuItem> getAttachmentMenuItems() {
      return FxGet.rulesDrivenKometService().getAttachmentMenuItems(manifold, this.categorizedVersion, (t) -> {
         throw new UnsupportedOperationException();
      });
   }

   public final List<MenuItem> getEditMenuItems() {
      return FxGet.rulesDrivenKometService().getEditMenuItems(manifold, this.categorizedVersion, (propertySheetMenuItem) -> {
         addEditingPropertySheet(propertySheetMenuItem);
      });
   }

   private void addEditingPropertySheet(PropertySheetMenuItem propertySheetMenuItem) {
      pseudoClassStateChanged(PseudoClasses.UNCOMMITTED_PSEUDO_CLASS, true);
      editControl.setVisible(false);
      cancelButton.setVisible(true);
      commitButton.setVisible(true);
      this.optionalPropertySheetMenuItem = Optional.of(propertySheetMenuItem);
      redoLayout();
   }

   public void doExpandAllAction(ExpandAction action) {
      expandControl.setExpandAction(action);
      extensionPanels.forEach((panel) -> panel.doExpandAllAction(action));
   }

   protected abstract void addExtras();

   protected final void expand(ObservableValue<? extends ExpandAction> observable,
           ExpandAction oldValue,
           ExpandAction newValue) {
      redoLayout();
   }

   protected final void setupConcept(ConceptVersion conceptVersion) {
      if (isLatestPanel()) {
         componentType.setText("Concept");
         componentText.setText(
                 "\n" + conceptVersion.getState() + " in " + getManifold().getPreferredDescriptionText(
                 conceptVersion.getModuleSequence()) + " on " + getManifold().getPreferredDescriptionText(
                 conceptVersion.getPathSequence()));
      } else {
         componentType.setText("");
         componentText.setText(
                 conceptVersion.getState() + " in " + getManifold().getPreferredDescriptionText(
                 conceptVersion.getModuleSequence()) + " on " + getManifold().getPreferredDescriptionText(
                 conceptVersion.getPathSequence()));
      }
   }

   protected final void setupDef(LogicGraphVersion logicGraphVersion) {
      if (isLatestPanel()) {
         componentType.setText("DEF");

         if (getManifold().getLogicCoordinate()
                 .getInferredAssemblageSequence() == logicGraphVersion.getAssemblageSequence()) {
            badges.add(Iconography.SETTINGS_GEAR.getIconographic());
         } else if (getManifold().getLogicCoordinate()
                 .getStatedAssemblageSequence() == logicGraphVersion.getAssemblageSequence()) {
            badges.add(Iconography.ICON_EXPORT.getIconographic());
         }
      } else {
         componentType.setText("");
      }

      componentText.setText(logicGraphVersion.getLogicalExpression()
              .toSimpleString());
   }

   protected final void setupDescription(DescriptionVersion description) {
      componentText.setText(description.getText());

      if (isLatestPanel()) {
         int descriptionType = description.getDescriptionTypeConceptSequence();

         if (descriptionType == TermAux.FULLY_SPECIFIED_DESCRIPTION_TYPE.getConceptSequence()) {
            componentType.setText("FSN");
         } else if (descriptionType == TermAux.SYNONYM_DESCRIPTION_TYPE.getConceptSequence()) {
            componentType.setText("SYN");
         } else if (descriptionType == TermAux.DEFINITION_DESCRIPTION_TYPE.getConceptSequence()) {
            componentType.setText("DEF");
         } else {
            componentType.setText(getManifold().getPreferredDescriptionText(descriptionType));
         }
      } else {
         componentType.setText("");
      }

      if (description.getCaseSignificanceConceptSequence() == TermAux.DESCRIPTION_CASE_SENSITIVE.getConceptSequence()) {
         badges.add(Iconography.CASE_SENSITIVE.getIconographic());
      } else if (description.getCaseSignificanceConceptSequence()
              == TermAux.DESCRIPTION_INITIAL_CHARACTER_SENSITIVE.getConceptSequence()) {
         // TODO get iconographic for initial character sensitive
         badges.add(Iconography.CASE_SENSITIVE.getIconographic());
      } else if (description.getCaseSignificanceConceptSequence()
              == TermAux.DESCRIPTION_NOT_CASE_SENSITIVE.getConceptSequence()) {
         badges.add(Iconography.CASE_SENSITIVE_NOT.getIconographic());
      }
   }

   protected final void setupOther(Version version) {
      if (version instanceof SememeVersion) {
         SememeVersion sememeVersion = (SememeVersion) version;
         VersionType sememeType = sememeVersion.getChronology()
                 .getSememeType();

         componentType.setText(sememeType.toString());

         switch (sememeType) {
            case STRING:
               if (isLatestPanel()) {
                  componentType.setText("STR");
               } else {
                  componentType.setText("");
               }

               componentText.setText(
                       getManifold().getPreferredDescriptionText(
                               sememeVersion.getAssemblageSequence()) + "\n" + ((StringVersion) sememeVersion).getString());
               break;

            case COMPONENT_NID:
               if (isLatestPanel()) {
                  componentType.setText("REF");
               } else {
                  componentType.setText("");
               }

               int nid = ((ComponentNidVersion) sememeVersion).getComponentNid();

               switch (Get.identifierService()
                       .getChronologyTypeForNid(nid)) {
                  case CONCEPT:
                     componentText.setText(
                             getManifold().getPreferredDescriptionText(
                                     sememeVersion.getAssemblageSequence()) + "\n" + getManifold().getPreferredDescriptionText(nid));
                     break;

                  case SEMEME:
                     SememeChronology sc = Get.assemblageService()
                             .getSememe(nid);

                     componentText.setText(
                             getManifold().getPreferredDescriptionText(
                                     sememeVersion.getAssemblageSequence()) + "\nReferences: " + sc.getSememeType().toString());
                     break;

                  case UNKNOWN_NID:
                  default:
                     componentText.setText(
                             getManifold().getPreferredDescriptionText(
                                     sememeVersion.getAssemblageSequence()) + "\nReferences:"
                             + Get.identifierService().getChronologyTypeForNid(
                                     nid).toString());
               }

               break;

            case LOGIC_GRAPH:
               if (isLatestPanel()) {
                  componentType.setText("DEF");
               } else {
                  componentType.setText("");
               }

               componentText.setText(((LogicGraphVersion) sememeVersion).getLogicalExpression()
                       .toString());
               break;

            case LONG:
               if (isLatestPanel()) {
                  componentType.setText("INT");
               } else {
                  componentType.setText("");
               }

               componentText.setText(Long.toString(((LongVersion) sememeVersion).getLongValue()));
               break;

            case MEMBER:
               componentText.setText(
                       getManifold().getPreferredDescriptionText(sememeVersion.getAssemblageSequence()) + "\nMember");
               break;

            case DYNAMIC:
            case UNKNOWN:
            case DESCRIPTION:
            default:
               throw new UnsupportedOperationException("Can't handle: " + sememeType);
         }
      } else {
         componentText.setText(version.getClass()
                 .getSimpleName());
      }
   }

   protected void textLayoutChanged(ObservableValue<? extends Bounds> bounds, Bounds oldBounds, Bounds newBounds) {
      redoLayout();
   }

   protected void widthChanged(ObservableValue<? extends Number> observableWidth, Number oldWidth, Number newWidth) {
      redoLayout();
   }

   private void pseudoStateChanged(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
      if (observable == isDescription) {
         this.pseudoClassStateChanged(PseudoClasses.DESCRIPTION_PSEUDO_CLASS, newValue);
      } else if (observable == isInactive) {
         this.pseudoClassStateChanged(PseudoClasses.INACTIVE_PSEUDO_CLASS, newValue);
      } else if (observable == isConcept) {
         this.pseudoClassStateChanged(PseudoClasses.CONCEPT_PSEUDO_CLASS, newValue);
      } else if (observable == isLogicalDefinition) {
         this.pseudoClassStateChanged(PseudoClasses.LOGICAL_DEFINITION_PSEUDO_CLASS, newValue);
      } else if (observable == isContradiction) {
         this.pseudoClassStateChanged(PseudoClasses.CONTRADICTED_PSEUDO_CLASS, newValue);
      }
   }

   private void redoLayout() {
      if (getParent() != null) {
         getParent().applyCss();
         getParent().layout();
      }
      double doubleRows = componentText.boundsInLocalProperty()
              .get()
              .getHeight() / rowHeight;
      int rowsOfText = (int) doubleRows + 1;
      gridpane.getRowConstraints()
              .clear();

      gridpane.setMinWidth(layoutBoundsProperty().get()
              .getWidth());
      gridpane.setPrefWidth(layoutBoundsProperty().get()
              .getWidth());
      gridpane.setMaxWidth(layoutBoundsProperty().get()
              .getWidth());
      setupColumns();
      wrappingWidth = (int) (layoutBoundsProperty().get()
              .getWidth() - (5 * badgeWidth));
      if (componentText.getWrappingWidth() != wrappingWidth) {
         componentText.setWrappingWidth(wrappingWidth);
         // will call redoLayout, so should not continue to layout...
      } else {
         
         gridpane.getChildren()
                 .remove(expandControl);
         GridPane.setConstraints(expandControl, 0, 0, 1, 1, HPos.CENTER, VPos.TOP, Priority.NEVER, Priority.NEVER);
         gridpane.getChildren()
                 .add(expandControl);  // next is 1
         gridpane.getChildren()
                 .remove(componentType);
         GridPane.setConstraints(componentType, 1, 0, 2, 1, HPos.LEFT, VPos.TOP, Priority.NEVER, Priority.NEVER);
         gridpane.getChildren()
                 .add(componentType);  // next is 3
         gridpane.getChildren()
                 .remove(addAttachmentControl);
         GridPane.setConstraints(addAttachmentControl,
                 columns,
                 1,
                 2,
                 1,
                 HPos.RIGHT,
                 VPos.CENTER,
                 Priority.SOMETIMES,
                 Priority.NEVER,
                 new Insets(0, 4, 1, 0));
         gridpane.getChildren()
                 .add(addAttachmentControl);
// edit control         
         gridpane.getChildren()
                 .remove(editControl);
         GridPane.setConstraints(
                 editControl,
                 columns,
                 0,
                 2,
                 1,
                 HPos.RIGHT,
                 VPos.TOP,
                 Priority.SOMETIMES,
                 Priority.NEVER,
                 new Insets(1, 4, 0, 0));
         gridpane.getChildren()
                 .add(editControl);
// commitButton         
         
         gridpane.getChildren()
                 .remove(commitButton);
         GridPane.setConstraints(
                 commitButton,
                 columns-3,
                 0,
                 4,
                 1,
                 HPos.RIGHT,
                 VPos.TOP,
                 Priority.SOMETIMES,
                 Priority.NEVER,
                 new Insets(1, 4, 0, 0));
         gridpane.getChildren()
                 .add(commitButton);
                 
//         
// cancelButton         
         
         gridpane.getChildren()
                 .remove(cancelButton);
         GridPane.setConstraints(
                 cancelButton,
                 columns-6,
                 0,
                 3,
                 1,
                 HPos.RIGHT,
                 VPos.TOP,
                 Priority.SOMETIMES,
                 Priority.NEVER,
                 new Insets(1, 4, 0, 0));
         gridpane.getChildren()
                 .add(cancelButton);
                 
//         
         int gridRow = 0;
         if (optionalPropertySheetMenuItem.isPresent()) {
            PropertySheetMenuItem propertySheetMenuItem = optionalPropertySheetMenuItem.get();
            PropertySheet propertySheet = propertySheetMenuItem.getPropertySheet();
            gridpane.getChildren()
                    .remove(propertySheet);
            gridRow = 1;
            gridpane.getRowConstraints()
                    .add(new RowConstraints(rowHeight));  // add row zero...
            RowConstraints propertyRowConstraints = new RowConstraints();
            propertyRowConstraints.setVgrow(Priority.NEVER);

            gridpane.getRowConstraints()
                    .add(propertyRowConstraints);  // add row one...

            GridPane.setConstraints(
                    propertySheet,
                    0,
                    gridRow++,
                    columns - 1,
                    1,
                    HPos.LEFT,
                    VPos.TOP,
                    Priority.ALWAYS,
                    Priority.NEVER);

            gridpane.getChildren()
                    .add(propertySheet);
         }

         componentText.getLayoutBounds()
                 .getHeight();
         gridpane.getChildren()
                 .remove(componentText);
         GridPane.setConstraints(
                 componentText,
                 3,
                 gridRow++,
                 columns - 4,
                 (int) rowsOfText,
                 HPos.LEFT,
                 VPos.TOP,
                 Priority.ALWAYS,
                 Priority.NEVER);
         gridpane.getChildren()
                 .add(componentText);
         gridpane.getRowConstraints()
                 .add(new RowConstraints(rowHeight));

         boolean firstBadgeAdded = false;

         for (int i = 0; i < badges.size();) {
            for (int row = gridRow; i < badges.size(); row++) {
               this.rows = row;
               gridpane.getRowConstraints()
                       .add(new RowConstraints(rowHeight));

               if (row + 1 <= rowsOfText) {
                  for (int column = 0; (column < 3) && (i < badges.size()); column++) {
                     if (firstBadgeAdded && (column == 0)) {
                        column = 1;
                        firstBadgeAdded = true;
                     }

                     setupBadge(badges.get(i++), column, row);
                  }
               } else {
                  for (int column = 0; (column < columns) && (i < badges.size()); column++) {
                     if (firstBadgeAdded && (column == 0)) {
                        column = 1;
                        firstBadgeAdded = true;
                     }

                     setupBadge(badges.get(i++), column, row);
                  }
               }
            }
         }

         addExtras();
      }

   }

   private void setupBadge(Node badge, int column, int row) {
      gridpane.getChildren()
              .remove(badge);
      GridPane.setConstraints(
              badge,
              column,
              row,
              1,
              1,
              HPos.CENTER,
              VPos.CENTER,
              Priority.NEVER,
              Priority.NEVER,
              new Insets(2));
      gridpane.getChildren()
              .add(badge);

      if (!badge.getStyleClass()
              .contains(StyleClasses.COMPONENT_BADGE.toString())) {
         badge.getStyleClass()
                 .add(StyleClasses.COMPONENT_BADGE.toString());
      }
   }

   private void setupColumns() {
      if (this.getParent() != null) {
         this.columns = (int) (getLayoutBounds().getWidth() / badgeWidth) - 1;

         if (this.columns < 6) {
            this.columns = 6;
         }

         gridpane.getColumnConstraints()
                 .clear();

         for (int i = 0; i < this.columns; i++) {
            if (i == 0) {
               gridpane.getColumnConstraints()
                       .add(new ColumnConstraints(FIRST_COLUMN_WIDTH));
            } else {
               gridpane.getColumnConstraints()
                       .add(new ColumnConstraints(badgeWidth));
            }
         }
      }
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * @return the categorizedVersion
    */
   public final ObservableCategorizedVersion getCategorizedVersion() {
      return categorizedVersion;
   }

   @Override
   public final ObservableList<Node> getChildren() {
      return super.getChildren();
   }

   public int getColumns() {
      return columns;
   }

   protected abstract boolean isLatestPanel();

   /**
    * @return the manifold
    */
   public Manifold getManifold() {
      return manifold;
   }

   public int getRows() {
      return rows;
   }
}