package sh.isaac.model.observable.version.brittle;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.component.semantic.version.SemanticVersion;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.observable.ObservableVersion;
import sh.isaac.api.observable.semantic.ObservableSemanticChronology;
import sh.isaac.api.observable.semantic.version.brittle.Observable_Nid1_Long2_Version;
import sh.isaac.model.observable.ObservableFields;
import sh.isaac.model.observable.commitaware.CommitAwareIntegerProperty;
import sh.isaac.model.observable.commitaware.CommitAwareLongProperty;
import sh.isaac.model.observable.version.ObservableAbstractSemanticVersionImpl;
import sh.isaac.model.semantic.SemanticChronologyImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Long2_VersionImpl;

/**
 *
 * @author kec
 */
public class Observable_Nid1_Long2_VersionImpl
        extends ObservableAbstractSemanticVersionImpl
        implements Observable_Nid1_Long2_Version {
    IntegerProperty nid1Property;
    LongProperty long2Property;

    public Observable_Nid1_Long2_VersionImpl(SemanticVersion stampedVersion, ObservableSemanticChronology chronology) {
        super(stampedVersion, chronology);
    }

    public Observable_Nid1_Long2_VersionImpl(Observable_Nid1_Long2_VersionImpl versionToClone, ObservableSemanticChronology chronology) {
        super(versionToClone, chronology);
        setNid1(versionToClone.getNid1());
        setLong2(versionToClone.getLong2());
    }
    public Observable_Nid1_Long2_VersionImpl(UUID primordialUuid, UUID referencedComponentUuid, int assemblageNid) {
        super(VersionType.Nid1_Long2, primordialUuid, referencedComponentUuid, assemblageNid);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <V extends ObservableVersion> V makeAutonomousAnalog(ManifoldCoordinate mc) {
        Observable_Nid1_Int2_VersionImpl analog = new Observable_Nid1_Int2_VersionImpl(this, getChronology());
        copyLocalFields(analog);
        analog.setModuleNid(mc.getModuleNidForAnalog(this));
        analog.setAuthorNid(mc.getAuthorNidForChanges());
        analog.setPathNid(mc.getPathNidForAnalog());
        return (V) analog;
    }

    @Override
    public LongProperty long2Property() {
        if (this.stampedVersionProperty == null  && this.long2Property == null) {
            this.long2Property = new CommitAwareLongProperty(this, ObservableFields.LONG2.toExternalString(),
                    0);
        }
        if (this.long2Property == null) {
            this.long2Property = new CommitAwareLongProperty(this, ObservableFields.LONG2.toExternalString(), getLong2());
            this.long2Property.addListener(
                    (observable, oldValue, newValue) -> {
                        getNid1_Long2_Version().setLong2(newValue.intValue());
                    });
        }

        return this.long2Property;
    }

    @Override
    public IntegerProperty nid1Property() {
        if (this.stampedVersionProperty == null  && this.nid1Property == null) {
            this.nid1Property = new CommitAwareIntegerProperty(this, ObservableFields.NID1.toExternalString(),
                    0);
        }
        if (this.nid1Property == null) {
            this.nid1Property = new CommitAwareIntegerProperty(this, ObservableFields.NID1.toExternalString(), getNid1());
            this.nid1Property.addListener(
                    (observable, oldValue, newValue) -> {
                        getNid1_Long2_Version().setNid1(newValue.intValue());
                    });
        }

        return this.nid1Property;
    }

    @Override
    public long getLong2() {
        if (this.long2Property != null) {
            return this.long2Property.get();
        }

        return getNid1_Long2_Version().getLong2();
    }

    @Override
    public final void setLong2(long value) {
        if (this.stampedVersionProperty == null) {
            this.long2Property();
        }
        if (this.long2Property != null) {
            this.long2Property.set(value);
        }

        if (this.stampedVersionProperty != null) {
            getNid1_Long2_Version().setLong2(value);
        }
    }

    @Override
    public int getNid1() {
        if (this.nid1Property != null) {
            return this.nid1Property.get();
        }

        return getNid1_Long2_Version().getNid1();
    }

    @Override
    public final void setNid1(int nid) {
        if (this.stampedVersionProperty == null) {
            this.nid1Property();
        }
        if (this.nid1Property != null) {
            this.nid1Property.set(nid);
        }

        if (this.stampedVersionProperty != null) {
            getNid1_Long2_Version().setNid1(nid);
        }
    }

    private Nid1_Long2_VersionImpl getNid1_Long2_Version() {
        return (Nid1_Long2_VersionImpl) this.stampedVersionProperty.get();
    }

    @Override
    public List<ReadOnlyProperty<?>> getProperties() {
        List<ReadOnlyProperty<?>> properties = super.getProperties();

        properties.add(nid1Property());
        properties.add(long2Property());
        return properties;
    }

    @Override
    protected List<Property<?>> getEditableProperties3() {
        List<Property<?>> properties = new ArrayList<>();
        properties.add(nid1Property());
        properties.add(long2Property());
        return properties;
    }

    @Override
    protected void copyLocalFields(SemanticVersion analog) {
        if (analog instanceof Observable_Nid1_Long2_VersionImpl) {
            Observable_Nid1_Long2_VersionImpl observableAnalog = (Observable_Nid1_Long2_VersionImpl) analog;
            observableAnalog.setNid1(this.getNid1());
            observableAnalog.setLong2(this.getLong2());
        } else if (analog instanceof Nid1_Long2_VersionImpl) {
            Nid1_Long2_VersionImpl simpleAnalog = (Nid1_Long2_VersionImpl) analog;
            simpleAnalog.setNid1(this.getNid1());
            simpleAnalog.setLong2(this.getLong2());
        } else {
            throw new IllegalStateException("Can't handle class: " + analog.getClass());
        }
    }

    @Override
    public Chronology createChronologyForCommit(int stampSequence) {
        SemanticChronologyImpl sc = new SemanticChronologyImpl(versionType, getPrimordialUuid(), getAssemblageNid(), this.getReferencedComponentNid());
        Nid1_Long2_VersionImpl newVersion = new Nid1_Long2_VersionImpl(sc, stampSequence);
        copyLocalFields(newVersion);
        sc.addVersion(newVersion);
        return sc;
    }

    @Override
    protected void updateVersion() {
        if (this.nid1Property != null &&
                this.nid1Property.get() != ((Nid1_Long2_VersionImpl) this.stampedVersionProperty.get()).getNid1()) {
            this.nid1Property.set(((Nid1_Long2_VersionImpl) this.stampedVersionProperty.get()).getNid1());
        }
        if (this.long2Property != null &&
                this.long2Property.get() != ((Nid1_Long2_VersionImpl) this.stampedVersionProperty.get()).getLong2()) {
            this.long2Property.set(((Nid1_Long2_VersionImpl) this.stampedVersionProperty.get()).getLong2());
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V extends Version> V makeAnalog(int stampSequence) {
        Nid1_Long2_VersionImpl newVersion = getOptionalStampedVersion().get().makeAnalog(stampSequence);
        Observable_Nid1_Long2_VersionImpl newObservableVersion = new Observable_Nid1_Long2_VersionImpl(newVersion, getChronology());
        getChronology().getVersionList().add(newObservableVersion);
        return (V) newObservableVersion;
    }
}
