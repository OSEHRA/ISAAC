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
package sh.isaac.komet.gui.graphview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import javafx.application.Platform;
import org.apache.mahout.math.Arrays;
import sh.isaac.api.Get;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.komet.gui.util.FxGet;

/**
 *
 * @author kec
 */
public class ShowConceptInGraphTask extends TimedTaskWithProgressTracker<Void> {

    private final MultiParentGraphViewController multiParentGraphView;
    private final UUID conceptUUID;

    public ShowConceptInGraphTask(MultiParentGraphViewController multiParentGraphView, UUID conceptUUID) {
        this.multiParentGraphView = multiParentGraphView;
        this.conceptUUID = conceptUUID;
        Get.activeTasks().add(this);
        String conceptDescription
                = Get.conceptDescriptionText(Get.identifierService().getNidForUuids(conceptUUID));
        updateTitle("Expanding taxonomy to: " + conceptDescription);
    }

    @Override
    protected Void call()
            throws Exception {
        try {
            // await() init() completion.
            LOG.debug("Looking for concept {} in tree", Get.conceptDescriptionText(Get.identifierService().getNidForUuids(conceptUUID)));
            FxGet.statusMessageService().reportStatus("Expanding taxonomy to: " + 
                            multiParentGraphView.getManifoldCoordinate().getPreferredDescriptionText(Get.identifierService().getNidForUuids(conceptUUID)));

            final ArrayList<UUID> pathToRoot = new ArrayList<>();

            pathToRoot.add(conceptUUID);

            // Walk up taxonomy to origin until no parent found.
            UUID current = conceptUUID;

            while (true) {
                Optional<? extends ConceptChronology> conceptOptional = Get.conceptService()
                        .getOptionalConcept(current);

                if (!conceptOptional.isPresent()) {
                    // Must be a "pending concept".
                    // Not handled yet.
                    FxGet.statusMessageService().reportStatus("Concept is not yet committed: " + 
                            Get.conceptDescriptionText(Get.identifierService().getNidForUuids(conceptUUID)));
                    return null;
                }

                ConceptChronology concept = conceptOptional.get();

                // Look for an IS_A relationship to origin.
                boolean found = false;

                for (int parent : multiParentGraphView.getNavigator().getParentNids(concept.getNid())) {
                    current = Get.identifierService()
                            .getUuidPrimordialForNid(parent);
                    pathToRoot.add(current);
                    found = true;
                    break;
                }

                // No parent IS_A relationship found, stop looking.
                if (!found) {
                    if (concept.getNid() != TermAux.SOLOR_ROOT.getNid()) {
                        FxGet.statusMessageService().reportStatus("No parents for concept: " + 
                            multiParentGraphView.getManifoldCoordinate().getPreferredDescriptionText(concept));
                    }
                    break;
                }
            }

            Collections.reverse(pathToRoot);


            LOG.debug("Calculated root path {}", Arrays.toString(pathToRoot.toArray()));
            Platform.runLater(() -> {
                this.multiParentGraphView.expandAndSelect(pathToRoot);
            });
        } finally {
            Get.activeTasks().remove(this);
        }
        return null;
    }
}
