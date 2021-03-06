package sh.isaac.komet.preferences.coordinate;

import javafx.beans.property.SimpleStringProperty;
import sh.isaac.MetaData;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.coordinate.Coordinates;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LanguageCoordinateImmutable;
import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.observable.coordinate.ObservableLanguageCoordinate;
import sh.isaac.api.preferences.IsaacPreferences;
import sh.isaac.komet.preferences.AbstractPreferences;
import sh.isaac.model.observable.coordinate.ObservableLanguageCoordinateImpl;
import sh.komet.gui.contract.preferences.KometPreferencesController;
import sh.komet.gui.contract.preferences.PreferenceGroup;
import sh.komet.gui.control.property.wrapper.PropertySheetTextWrapper;
import sh.komet.gui.control.concept.PropertySheetConceptListWrapper;
import sh.komet.gui.control.concept.PropertySheetItemConceptWrapper;
import sh.komet.gui.control.property.ViewProperties;
import sh.komet.gui.util.FxGet;
import sh.isaac.api.util.UuidStringKey;

import java.util.Optional;
import java.util.UUID;
import java.util.prefs.BackingStoreException;

public class LanguageCoordinateItemPanel extends AbstractPreferences {

    public enum Keys {
        LANGUAGE_COORDINATE_DATA
    }

    private final SimpleStringProperty nameProperty
            = new SimpleStringProperty(this, MetaData.LANGUAGE_COORDINATE_NAME____SOLOR.toExternalString());

    private final ObservableLanguageCoordinate languageCoordinateItem;

    private final UuidStringKey itemKey;


    public LanguageCoordinateItemPanel(LanguageCoordinate languageCoordinate, String coordinateName, IsaacPreferences preferencesNode, ViewProperties viewProperties, KometPreferencesController kpc) {
        super(preferencesNode, coordinateName, viewProperties, kpc);
         this.languageCoordinateItem = new ObservableLanguageCoordinateImpl(languageCoordinate.toLanguageCoordinateImmutable());
        setup(viewProperties);
        this.itemKey = new UuidStringKey(UUID.fromString(preferencesNode.name()), nameProperty.get());
        FxGet.languageCoordinates().put(itemKey, languageCoordinateItem);
    }


    /**
     * Constructure used via reflection when reading preferences.
     * @param preferencesNode
     * @param viewProperties
     * @param kpc
     */
    public LanguageCoordinateItemPanel(IsaacPreferences preferencesNode, ViewProperties viewProperties, KometPreferencesController kpc) {
        super(preferencesNode, preferencesNode.get(PreferenceGroup.Keys.GROUP_NAME).get(), viewProperties, kpc);
        Optional<byte[]> optionalBytes = preferencesNode.getByteArray(Keys.LANGUAGE_COORDINATE_DATA);
        if (optionalBytes.isPresent()) {
            ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(optionalBytes.get());
            LanguageCoordinateImmutable languageCoordinate = LanguageCoordinateImmutable.make(buffer);
            this.languageCoordinateItem = new ObservableLanguageCoordinateImpl(languageCoordinate);
        } else {
            setGroupName("US English");
            LanguageCoordinateImmutable languageCoordinate = Coordinates.Language.UsEnglishRegularName();
            this.languageCoordinateItem = new ObservableLanguageCoordinateImpl(languageCoordinate);
        }
        setup(viewProperties);
        this.itemKey = new UuidStringKey(UUID.fromString(preferencesNode.name()), nameProperty.get());
        FxGet.languageCoordinates().put(itemKey, languageCoordinateItem);
    }

    private void setup(ViewProperties viewProperties) {

        nameProperty.set(groupNameProperty().get());
        getItemList().add(new PropertySheetTextWrapper(viewProperties.getManifoldCoordinate(), nameProperty));
        nameProperty.addListener((observable, oldValue, newValue) -> {
            groupNameProperty().set(newValue);
            FxGet.languageCoordinates().remove(itemKey);
            itemKey.updateString(newValue);
            FxGet.languageCoordinates().put(itemKey, languageCoordinateItem);

        });

        getItemList().add(new PropertySheetItemConceptWrapper(viewProperties.getManifoldCoordinate(), languageCoordinateItem.languageConceptProperty(),
                TermAux.ENGLISH_LANGUAGE.getNid(),TermAux.SPANISH_LANGUAGE.getNid()));
        getItemList().add(new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), languageCoordinateItem.dialectAssemblagePreferenceListProperty()));
        getItemList().add(new PropertySheetConceptListWrapper(viewProperties.getManifoldCoordinate(), languageCoordinateItem.descriptionTypePreferenceListProperty()));
        revert();
        save();
    }

    @Override
    protected void saveFields() throws BackingStoreException {
        getPreferencesNode().put(PreferenceGroup.Keys.GROUP_NAME, this.nameProperty.get());
        ByteArrayDataBuffer buff = new ByteArrayDataBuffer();
        this.languageCoordinateItem.getValue().marshal(buff);
        buff.trimToSize();
        getPreferencesNode().putByteArray(Keys.LANGUAGE_COORDINATE_DATA, buff.getData());
    }

    @Override
    protected void revertFields() throws BackingStoreException {
        this.nameProperty.set(getPreferencesNode().get(PreferenceGroup.Keys.GROUP_NAME, getGroupName()));

        this.nameProperty.set(getPreferencesNode().get(PreferenceGroup.Keys.GROUP_NAME, getGroupName()));
        ByteArrayDataBuffer revertbuffer = new ByteArrayDataBuffer();
        this.languageCoordinateItem.getValue().marshal(revertbuffer);
        revertbuffer.trimToSize();
        byte[] data = getPreferencesNode().getByteArray(Keys.LANGUAGE_COORDINATE_DATA, revertbuffer.getData());

        ByteArrayDataBuffer buffer = new ByteArrayDataBuffer(data);
        LanguageCoordinate languageCoordinate = LanguageCoordinateImmutable.make(buffer);
        if (!languageCoordinate.getLanguageCoordinateUuid().equals(this.languageCoordinateItem.getLanguageCoordinateUuid())) {

            this.languageCoordinateItem.languageConceptProperty().setValue(languageCoordinate.getLanguageConcept());

            this.languageCoordinateItem.dialectAssemblagePreferenceListProperty().setAll(languageCoordinate.getDialectAssemblageSpecPreferenceList());

            this.languageCoordinateItem.descriptionTypePreferenceListProperty().setAll(languageCoordinate.getDescriptionTypeSpecPreferenceList());

            this.languageCoordinateItem.modulePreferenceListForLanguageProperty().setAll(languageCoordinate.getModuleSpecPreferenceListForLanguage());

            // TODO: handle next priority language coordinate.
            //LanguageCoordinate nextPriorityLanguageCoordinate = null;

        }
    }
}