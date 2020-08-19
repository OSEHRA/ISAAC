package sh.komet.gui.control.badged;

import static sh.komet.gui.util.FxUtils.setupHeaderPanel;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.mahout.math.map.OpenIntIntHashMap;
import javafx.scene.Node;
import javafx.scene.layout.AnchorPane;
import sh.isaac.api.Status;
import sh.isaac.api.chronicle.CategorizedVersion;
import sh.isaac.api.chronicle.CategorizedVersions;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.observable.ObservableCategorizedVersion;
import sh.isaac.api.observable.ObservableChronology;
import sh.isaac.api.observable.ObservableVersion;
import sh.komet.gui.control.PropertySheetMenuItem;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.style.StyleClasses;

public class ComponentPaneModel extends BadgedVersionPaneModel {
    private static final Logger LOG = LogManager.getLogger();
    private final CategorizedVersions<CategorizedVersion> categorizedVersions;
    private final AnchorPane extensionHeaderPanel = setupHeaderPanel("Attachments:");
    private final AnchorPane versionHeaderPanel = setupHeaderPanel("Change history:");

    //~--- constructors --------------------------------------------------------
    public ComponentPaneModel(Manifold manifold, ObservableCategorizedVersion categorizedVersion,
                              OpenIntIntHashMap stampOrderHashMap,
                              HashMap<String, AtomicBoolean> disclosureStateMap) {
        super(manifold, categorizedVersion, stampOrderHashMap, disclosureStateMap);

        this.categorizedVersions = categorizedVersion.getCategorizedVersions();

        // gridpane.gridLinesVisibleProperty().set(true);
        getBadgedPane().getStyleClass()
                .add(StyleClasses.COMPONENT_PANEL.toString());

        ObservableVersion observableVersion = getCategorizedVersion().getObservableVersion();

        isContradiction.set(this.categorizedVersions.getLatestVersion()
                .isContradicted());

        if (!this.categorizedVersions.getUncommittedVersions().isEmpty()) {
            if (this.categorizedVersions.getUncommittedVersions().size() > 1) {
                LOG.error("Error: Can't handle more than one uncommitted version in this editor...");
            }
            ObservableCategorizedVersion uncommittedVersion = (ObservableCategorizedVersion) this.categorizedVersions.getUncommittedVersions().get(0);
            Optional<PropertySheetMenuItem> propertySheetMenuItem = uncommittedVersion.getUserObject(PROPERTY_SHEET_ATTACHMENT);
            if (propertySheetMenuItem.isPresent()) {
                this.addEditingPropertySheet(propertySheetMenuItem.get());
            } else {
                LOG.error("Warn: No property sheet editor for this uncommitted version...\n       " + uncommittedVersion.getPrimordialUuid()
                        + "\n       " + uncommittedVersion + "\nWill treat uncommitted as a historic version. ");
                versionPanes.add(new VersionPaneModel(manifold, uncommittedVersion, stampOrderHashMap,
                        getDisclosureStateMap()));
            }
        }

        if (this.categorizedVersions.getLatestVersion()
                .isContradicted()) {
            this.categorizedVersions.getLatestVersion()
                    .contradictions()
                    .forEach(
                            (contradiction) -> {
                                if (contradiction.getStampSequence() != -1) {
                                    versionPanes.add(new VersionPaneModel(manifold, (ObservableCategorizedVersion) contradiction, stampOrderHashMap,
                                            getDisclosureStateMap()));
                                }
                            });
        }

        this.categorizedVersions.getHistoricVersions()
                .forEach(
                        (historicVersion) -> {
                            if (historicVersion.getStampSequence() != -1 && historicVersion.getStatus() != Status.CANCELED) {
                                versionPanes.add(new VersionPaneModel(manifold, (ObservableCategorizedVersion) historicVersion, stampOrderHashMap,
                                        getDisclosureStateMap()));
                            }
                        });
        observableVersion.getChronology()
                .getObservableSemanticList()
                .forEach(
                        (osc) -> {
                            switch (osc.getVersionType()) {
                                case DESCRIPTION:
                                case LOGIC_GRAPH:
                                case RF2_RELATIONSHIP:
                                    break;  // Ignore, description and logic graph where already added as an independent panel

                                default:
                                    addChronology(osc, stampOrderHashMap);
                            }
                        });
        expandControl.setVisible(!versionPanes.isEmpty() || !extensionPaneModels.isEmpty());
    }

    public static boolean isSemanticTypeSupported(Chronology chronology) {
        return isSemanticTypeSupported(chronology.getVersionType());
    }
    public static boolean isSemanticTypeSupported(VersionType semanticType) {
        switch (semanticType) {
            case STRING:
            case COMPONENT_NID:
            case LOGIC_GRAPH:
            case LONG:
            case MEMBER:
            case CONCEPT:
            case DESCRIPTION:
            case Nid1_Int2:
            case Nid1_Long2:
            case IMAGE:
            case DYNAMIC:
                return true;

            case RF2_RELATIONSHIP:
                return false;

            default:
                //may consider supporting more types in the future.
                return false;
        }
    }

    //~--- methods -------------------------------------------------------------
    @Override
    public void addExtras() {
        switch (expandControl.getExpandAction()) {
            case HIDE_CHILDREN:
                if (!versionPanes.isEmpty()) {
                    getBadgedPane().getChildren()
                            .remove(versionHeaderPanel);
                }
                versionPanes.forEach(
                        (paneModel) -> {
                            getBadgedPane().getChildren()
                                    .remove(paneModel.getBadgedPane());
                        });
                if (!extensionPaneModels.isEmpty()) {
                    getBadgedPane().getChildren()
                            .remove(extensionHeaderPanel);
                }
                extensionPaneModels.forEach(
                        (paneModel) -> {
                            getBadgedPane().getChildren()
                                    .remove(paneModel.getBadgedPane());
                        });
                break;

            case SHOW_CHILDREN:
                if (!versionPanes.isEmpty()) {
                    addVersionPane(versionHeaderPanel);
                }
                // Add most recent first, and add oldest last... Reverse of the sort.
                for (int i = versionPanes.size() - 1; i > -1; i--) {
                    this.addVersionPane(versionPanes.get(i));
                 }
                if (!extensionPaneModels.isEmpty()) {
                    addAttachmentPane(extensionHeaderPanel);
                }
                extensionPaneModels.forEach(this::addAttachmentPane);
                break;

            default:
                throw new UnsupportedOperationException("am Can't handle: " + expandControl.getExpandAction());
        }
    }

    private void addChronology(ObservableChronology observableChronology, OpenIntIntHashMap stampOrderHashMap) {
        if (isSemanticTypeSupported(observableChronology)) {
            CategorizedVersions<ObservableCategorizedVersion> oscCategorizedVersions
                    = observableChronology.getCategorizedVersions(
                    getManifold().getStampFilter());

            if (oscCategorizedVersions.getLatestVersion()
                    .isPresent()) {
                ComponentPaneModel newPanel = new ComponentPaneModel(getManifold(),
                        oscCategorizedVersions.getLatestVersion().get(), stampOrderHashMap, getDisclosureStateMap());

                extensionPaneModels.add(newPanel);
            } else if (!oscCategorizedVersions.getUncommittedVersions().isEmpty()) {
                ComponentPaneModel newPanel = new ComponentPaneModel(getManifold(),
                        oscCategorizedVersions.getUncommittedVersions().get(0), stampOrderHashMap, getDisclosureStateMap());
                extensionPaneModels.add(newPanel);
            }
        }
    }
    private void addVersionPane(VersionPaneModel versionPane) {
        addVersionPane(versionPane.getBadgedPane());
    }

    private void addAttachmentPane(ComponentPaneModel componentPane) {
        componentPane.wrapAttachmentPane();
        addVersionPane(componentPane.getBadgedPane());
    }

    private void addVersionPane(Node panel) {
        getBadgedPane().getChildren()
                .remove(panel);
        getBadgedPane().getChildren()
                .add(panel);
    }

    private void addAttachmentPane(Node panel) {
        getBadgedPane().getChildren()
                .remove(panel);
        getBadgedPane().getChildren()
                .add(panel);
    }

    @Override
    protected boolean isLatestPanel() {
        return true;
    }
}
