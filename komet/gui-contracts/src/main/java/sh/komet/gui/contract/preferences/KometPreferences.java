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
package sh.komet.gui.contract.preferences;

import javafx.collections.ObservableList;
import javafx.stage.Stage;
import org.jvnet.hk2.annotations.Contract;

import java.util.Optional;
import java.util.UUID;

/**
 *
 * @author kec
 */
@Contract
public interface KometPreferences {
    Stage showPreferences();
    void loadPreferences();
    void reloadPreferences();
    void resetUserPreferences();
    void closePreferences();

    ObservableList<AttachmentItem> getAttachmentItems();
    ObservableList<ConfigurationPreference> getConfigurationPreferences();
    ObservableList<LogicItem> getLogicItems();
    ObservableList<SynchronizationItem> getSynchronizationItems();
    ObservableList<GraphConfigurationItem> getGraphConfigurationItems();
    ObservableList<UserPreferenceItems> getUserPreferenceItems();
    ObservableList<WindowPreferences> getWindowPreferenceItems();
    WindowsParentPreferences getWindowParentPreferences();
    ObservableList<PersonaItem> getPersonaPreferences();
    default Optional<PersonaItem> getPersona(UUID personaUuid) {
        for (PersonaItem personaItem: getPersonaPreferences()) {
            if (personaItem.getPersonaUuid().equals(personaUuid)) {
                return Optional.of(personaItem);
            }
        }
        return Optional.empty();
    }

    void updatePreferencesTitle(UUID preference, String title);

}
