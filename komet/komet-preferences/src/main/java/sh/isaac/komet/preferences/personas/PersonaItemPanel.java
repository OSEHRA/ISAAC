package sh.isaac.komet.preferences.personas;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import sh.isaac.MetaData;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.isaac.api.preferences.TransientPreferences;
import sh.isaac.komet.preferences.AbstractPreferences;
import sh.komet.gui.contract.preferences.KometPreferencesController;
import sh.komet.gui.contract.preferences.PreferenceGroup;
import sh.isaac.komet.preferences.window.WindowPreferencePanel;
import sh.komet.gui.contract.preferences.PersonaItem;
import sh.komet.gui.contract.preferences.WindowPreferences;
import sh.komet.gui.control.property.wrapper.PropertySheetBooleanWrapper;
import sh.komet.gui.control.property.wrapper.PropertySheetTextWrapper;
import sh.komet.gui.control.concept.PropertySheetConceptListWrapper;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.util.FxGet;
import sh.komet.gui.util.PersonaChangeListeners;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.BackingStoreException;

import static sh.komet.gui.contract.preferences.PreferenceGroup.Keys.GROUP_NAME;
import static sh.isaac.komet.preferences.personas.PersonasPanel.DEFAULT_PERSONA_NAME;

/**
 * 2019-07-22
 * aks8m - https://github.com/aks8m
 */
public class PersonaItemPanel extends AbstractPreferences implements PersonaItem {

    public enum Keys {
        PERSONA_UUID,
        NAME_FOR_INSTANCE,
        ENABLE_LEFT_PANE,
        ENABLE_CENTER_PANE,
        ENABLE_RIGHT_PANE,
        LEFT_PANE_DEFAULTS,
        CENTER_PANE_DEFAULTS,
        RIGHT_PANE_DEFAULTS,
        LEFT_PANE_OPTIONS,
        CENTER_PANE_OPTIONS,
        RIGHT_PANE_OPTIONS,
    };

    //TODO Dan Notes - This is pretty much an abomination of the entire notion of using HK2 and extendability.  No idea why on earth we are now hard coding UUIDs and concepts for 
    //implementations of GUI widgets.... its rather insane.  None of this belongs here....  If you want to have a default set for a particular persona (and don't mind breaking
    //the modular portions) then just link to them via code... this makes NO sense at all.  Massively brittle, terrible to extend / use / do anything with. We really _dont_ need
    //concepts for every single last thing in the GUI... especially if we are no longer allowing it to be modular, since you could just code to the classes... instead of this round
    //about brittle mess of code to hard coded UUIDs to Concepts in metadata and back to code
    private static final ConceptSpecification[] standardTabFactoryList = new ConceptSpecification[] {
            MetaData.ACTIVITIES_PANEL____SOLOR,
            MetaData.ASSEMBLAGE_PANEL____SOLOR,
            MetaData.COMPLETION_PANEL____SOLOR,
            MetaData.COMPONENT_LIST_PANEL____SOLOR,
            MetaData.CONCEPT_BUILDER_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_SEARCH_LINKED_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_TAXONOMY_LINKED_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_LIST_VIEW_LINKED_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_NEW_CONCEPT_LINKED_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_CLASSIFICATION_RESULTS_LINKED_PANEL____SOLOR,
            MetaData.CONCEPT_DETAILS_TREE_TABLE____SOLOR,
            MetaData.SIMPLE_SEARCH_PANEL____SOLOR,
            MetaData.EXTENDED_SEARCH_PANEL____SOLOR,
            MetaData.FLWOR_QUERY_PANEL____SOLOR,
            MetaData.TAXONOMY_PANEL____SOLOR,
            MetaData.CLASSIFICATION_RESULTS_PANEL____SOLOR,
            MetaData.COMPOSITE_ACTION_PANEL____SOLOR,
            MetaData.TRANSACTION_LIST_PANEL____SOLOR
    };

    private UUID personaUuid;

    private final SimpleStringProperty nameProperty
            = new SimpleStringProperty(this, MetaData.PERSONA_NAME____SOLOR.toExternalString());

    private final SimpleStringProperty instanceNameProperty
            = new SimpleStringProperty(this, MetaData.PERSONA_INSTANCE_NAME____SOLOR.toExternalString());

    private final SimpleBooleanProperty enableLeftPaneProperty = new SimpleBooleanProperty(this, MetaData.ENABLE_LEFT_PANE____SOLOR.toExternalString(), false);
    private final SimpleBooleanProperty enableCenterPaneProperty = new SimpleBooleanProperty(this, MetaData.ENABLE_CENTER_PANE____SOLOR.toExternalString(), false);
    private final SimpleBooleanProperty enableRightPaneProperty = new SimpleBooleanProperty(this, MetaData.ENABLE_RIGHT_PANE____SOLOR.toExternalString(), false);

    private final SimpleListProperty<ConceptSpecification> leftPaneDefaultsProperty =
            new SimpleListProperty<>(this, MetaData.LEFT_PANE_DAFAULTS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper leftPaneDefaultsWrapper;

    private final SimpleListProperty<ConceptSpecification> leftPaneOptionsProperty =
            new SimpleListProperty<>(this, MetaData.LEFT_PANE_OPTIONS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper leftPaneOptionsWrapper;

    private final SimpleListProperty<ConceptSpecification> centerPaneDefaultsProperty =
            new SimpleListProperty<>(this, MetaData.CENTER_PANE_DEFAULTS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper centerPaneDefaultsWrapper;

    private final SimpleListProperty<ConceptSpecification> centerPaneOptionsProperty =
            new SimpleListProperty<>(this, MetaData.CENTER_PANE_OPTIONS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper centerPaneOptionsWrapper;

    private final SimpleListProperty<ConceptSpecification> rightPaneDefaultsProperty =
            new SimpleListProperty<>(this, MetaData.RIGHT_PANE_DEFAULTS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper rightPaneDefaultsWrapper;

    private final SimpleListProperty<ConceptSpecification> rightPaneOptionsProperty =
            new SimpleListProperty<>(this, MetaData.RIGHT_PANE_OPTIONS____SOLOR.toExternalString(), FXCollections.observableArrayList());
    private final PropertySheetConceptListWrapper rightPaneOptionsWrapper;

    public PersonaItemPanel(IsaacPreferences preferencesNode, ViewProperties viewProperties, KometPreferencesController kpc,
                            String instanceName,
                            ConceptSpecification[] leftPanelDefaults,
                            ConceptSpecification[] centerPanelDefaults,
                            ConceptSpecification[] rightPanelDefaults) {
        this(preferencesNode, viewProperties, kpc);
        instanceNameProperty.set(instanceName);
        leftPaneDefaultsProperty.setAll(leftPanelDefaults);
        centerPaneDefaultsProperty.setAll(centerPanelDefaults);
        rightPaneDefaultsProperty.setAll(rightPanelDefaults);
        save();
    }

    public PersonaItemPanel(IsaacPreferences preferencesNode, ViewProperties viewProperties, KometPreferencesController kpc) {
        super(preferencesNode, getGroupName(preferencesNode), viewProperties, kpc);
        // The GROUP_NAME is the name of this window that will show up in the preferences panel,
        // We want the name of the group to be the same as name of the window, and will link the
        // properties here.
        this.setPersonaUuid(UUID.fromString(preferencesNode.name()));
        nameProperty.set(groupNameProperty().get());
        nameProperty.addListener((observable, oldValue, newValue) -> {
            groupNameProperty().set(newValue);
        });
        this.leftPaneOptionsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), leftPaneOptionsProperty);
        this.centerPaneOptionsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), centerPaneOptionsProperty);
        this.rightPaneOptionsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), rightPaneOptionsProperty);

        this.leftPaneDefaultsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), leftPaneDefaultsProperty);
        this.centerPaneDefaultsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), centerPaneDefaultsProperty);
        this.rightPaneDefaultsWrapper = new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), rightPaneDefaultsProperty);

        revertFields();
        save();
        getItemList().add(new PropertySheetTextWrapper(viewProperties.getManifoldCoordinate(), nameProperty));
        getItemList().add(new PropertySheetTextWrapper(viewProperties.getManifoldCoordinate(), instanceNameProperty));
        getItemList().add(new PropertySheetBooleanWrapper(viewProperties.getManifoldCoordinate(), enableLeftPaneProperty));
        getItemList().add(leftPaneDefaultsWrapper);
        getItemList().add(leftPaneOptionsWrapper);
        getItemList().add(new PropertySheetBooleanWrapper(viewProperties.getManifoldCoordinate(), enableCenterPaneProperty));
        getItemList().add(centerPaneDefaultsWrapper);
        getItemList().add(centerPaneOptionsWrapper);
        getItemList().add(new PropertySheetBooleanWrapper(viewProperties.getManifoldCoordinate(), enableRightPaneProperty));
        getItemList().add(rightPaneDefaultsWrapper);
        getItemList().add(rightPaneOptionsWrapper);
    }

    @Override
    protected void saveFields() throws BackingStoreException {
        getPreferencesNode().put(Keys.PERSONA_UUID, this.personaUuid.toString());
        getPreferencesNode().put(PreferenceGroup.Keys.GROUP_NAME, this.nameProperty.get());
        getPreferencesNode().put(Keys.NAME_FOR_INSTANCE, this.instanceNameProperty.get());
        getPreferencesNode().putBoolean(Keys.ENABLE_LEFT_PANE, this.enableLeftPaneProperty.get());
        getPreferencesNode().putConceptList(Keys.LEFT_PANE_DEFAULTS, this.leftPaneDefaultsProperty);
        getPreferencesNode().putConceptList(Keys.LEFT_PANE_OPTIONS, this.leftPaneOptionsProperty);
        getPreferencesNode().putBoolean(Keys.ENABLE_CENTER_PANE, this.enableCenterPaneProperty.get());
        getPreferencesNode().putConceptList(Keys.CENTER_PANE_DEFAULTS, this.centerPaneDefaultsProperty);
        getPreferencesNode().putConceptList(Keys.CENTER_PANE_OPTIONS, this.centerPaneOptionsProperty);
        getPreferencesNode().putBoolean(Keys.ENABLE_RIGHT_PANE, this.enableRightPaneProperty.get());
        getPreferencesNode().putConceptList(Keys.RIGHT_PANE_OPTIONS, this.rightPaneOptionsProperty);
        getPreferencesNode().putConceptList(Keys.RIGHT_PANE_DEFAULTS, this.rightPaneDefaultsProperty);
        PersonaChangeListeners.firePersonaChanged(this, true);
    }
    @Override
    protected void revertFields(){
        this.personaUuid = UUID.fromString(getPreferencesNode().get(Keys.PERSONA_UUID, this.personaUuid.toString()));
        this.nameProperty.set(getPreferencesNode().get(PreferenceGroup.Keys.GROUP_NAME, getGroupName()));
        this.instanceNameProperty.set(getPreferencesNode().get(Keys.NAME_FOR_INSTANCE, "Komet"));
        this.enableLeftPaneProperty.set(getPreferencesNode().getBoolean(Keys.ENABLE_LEFT_PANE, true));
        this.leftPaneDefaultsProperty.setAll(getPreferencesNode().getConceptList(Keys.LEFT_PANE_DEFAULTS,
                List.of(MetaData.TAXONOMY_PANEL____SOLOR)));
        this.leftPaneOptionsProperty.setAll(getPreferencesNode().getConceptList(Keys.LEFT_PANE_OPTIONS,
                List.of(standardTabFactoryList)));

        this.enableCenterPaneProperty.set(getPreferencesNode().getBoolean(Keys.ENABLE_CENTER_PANE, true));
        this.centerPaneDefaultsProperty.setAll(getPreferencesNode().getConceptList(Keys.CENTER_PANE_DEFAULTS,
                List.of(MetaData.CONCEPT_DETAILS_TAXONOMY_LINKED_PANEL____SOLOR, MetaData.CONCEPT_DETAILS_SEARCH_LINKED_PANEL____SOLOR, MetaData.CONCEPT_DETAILS_PANEL____SOLOR)));
        this.centerPaneOptionsProperty.setAll(getPreferencesNode().getConceptList(Keys.CENTER_PANE_OPTIONS,
                List.of(standardTabFactoryList)));

        this.enableRightPaneProperty.set(getPreferencesNode().getBoolean(Keys.ENABLE_RIGHT_PANE, true));
        this.rightPaneDefaultsProperty.setAll(getPreferencesNode().getConceptList(Keys.RIGHT_PANE_DEFAULTS,
                List.of(MetaData.ACTIVITIES_PANEL____SOLOR,
                        MetaData.SIMPLE_SEARCH_PANEL____SOLOR)));
        this.rightPaneOptionsProperty.setAll(getPreferencesNode().getConceptList(Keys.RIGHT_PANE_OPTIONS,
                List.of(standardTabFactoryList)));
        PersonaChangeListeners.firePersonaChanged(this, true);
    }

    @Override
    public boolean showDelete() {
        return true;
    }

    @Override
    protected void deleteSelf(ActionEvent event) {
        PersonaChangeListeners.firePersonaChanged(this, false);
        super.deleteSelf(event);
    }

    @Override
    public SimpleStringProperty nameProperty() {
        return nameProperty;
    }

    @Override
    public SimpleStringProperty instanceNameProperty() {
        return instanceNameProperty;
    }

    @Override
    public SimpleBooleanProperty enableLeftPaneProperty() {
        return enableLeftPaneProperty;
    }

    @Override
    public SimpleBooleanProperty enableCenterPaneProperty() {
        return enableCenterPaneProperty;
    }

    @Override
    public SimpleBooleanProperty enableRightPaneProperty() {
        return enableRightPaneProperty;
    }

    @Override
    public SimpleListProperty<ConceptSpecification> leftPaneOptionsProperty() {
        return leftPaneOptionsProperty;
    }


    @Override
    public SimpleListProperty<ConceptSpecification> centerPaneOptionsProperty() {
        return centerPaneOptionsProperty;
    }


    @Override
    public SimpleListProperty<ConceptSpecification> rightPaneOptionProperty() {
        return rightPaneOptionsProperty;
    }

    @Override
    public SimpleListProperty<ConceptSpecification> leftPaneDefaultsProperty() {
        return leftPaneDefaultsProperty;
    }
    @Override
    public SimpleListProperty<ConceptSpecification> centerPaneDefaultsProperty() {
        return centerPaneDefaultsProperty;
    }

    @Override
    public SimpleListProperty<ConceptSpecification> rightPaneDefaultsProperty() {
        return rightPaneDefaultsProperty;
    }

    @Override
    public UUID getPersonaUuid() {
        return personaUuid;
    }

    @Override
    public void setPersonaUuid(UUID personaUuid) {
        this.personaUuid = personaUuid;
    }

    @Override
    public WindowPreferences createNewWindowPreferences() {
        UUID windowUuid = UUID.randomUUID();
        String windowName = WindowPreferencePanel.getWindowName(instanceNameProperty.get(), windowUuid.toString());
        IsaacPreferences windowPreferencesNode = FxGet.kometPreferences().getWindowParentPreferences().addWindow(windowName, windowUuid);


        return new WindowPreferencePanel(windowPreferencesNode,
                getViewProperties(),
                this.kpc, this);
    }

    public static WindowPreferences createNewDefaultWindowPreferences(IsaacPreferences windowPreferencesNode,
                                                                      ViewProperties viewProperties, KometPreferencesController kpc) {

        TransientPreferences transientPersonaPreferences = new TransientPreferences(PersonasPanel.DEFAULT_PERSONA_UUID);
        transientPersonaPreferences.put(GROUP_NAME, DEFAULT_PERSONA_NAME);
        PersonaItemPanel persona = new PersonaItemPanel(transientPersonaPreferences, viewProperties, kpc);

        String windowName = WindowPreferencePanel.getWindowName(persona.instanceNameProperty().get(), windowPreferencesNode.name());
        windowPreferencesNode.put(GROUP_NAME, windowName);
        return new WindowPreferencePanel(windowPreferencesNode,
                viewProperties,
                kpc, persona);

    }

    @Override
    public Set<ConceptSpecification> getAllowedOptionsForPane(int paneIndex) {
        switch (paneIndex) {
            case 0:
                return new HashSet<ConceptSpecification>(leftPaneOptionsProperty);
            case 1:
                return new HashSet<ConceptSpecification>(centerPaneOptionsProperty);
            case 2:
                return new HashSet<ConceptSpecification>(rightPaneOptionsProperty);
            default:
                throw new ArrayIndexOutOfBoundsException("Pane index is: " + paneIndex);
        }
    }

    @Override
    public List<ConceptSpecification> getDefaultItemsForPane(int paneIndex) {
        switch (paneIndex) {
            case 0:
                return leftPaneDefaultsProperty;
            case 1:
                return centerPaneDefaultsProperty;
            case 2:
                return rightPaneDefaultsProperty;
            default:
                throw new ArrayIndexOutOfBoundsException("Pane index is: " + paneIndex);
        }
    }

    @Override
    public boolean isPaneEnabled(int paneIndex) {
        switch (paneIndex) {
            case 0:
                return enableLeftPaneProperty.get();
            case 1:
                return enableCenterPaneProperty.get();
            case 2:
                return enableRightPaneProperty.get();
            default:
                return false;
        }
    }
}
