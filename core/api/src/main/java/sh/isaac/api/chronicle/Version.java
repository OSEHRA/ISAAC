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
package sh.isaac.api.chronicle;

import java.util.UUID;

import sh.isaac.api.Get;
import sh.isaac.api.commit.IdentifiedStampedVersion;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.transaction.Transaction;

/**
 * @author kec
 */
public interface Version extends MutableStampedVersion, IdentifiedStampedVersion {

    default int getNid() {
        return getChronology().getNid();
    }
    /**
     * Gets the chronology.
     *
     * @return the chronology
     */
    Chronology getChronology();

    VersionType getSemanticType();

    /**
     * Create a analog version with Long.MAX_VALUE as the time, indicating
     * the version is uncommitted. It is the responsibility of the caller to
     * add the mutable version to the commit manager when changes are complete
     * prior to committing the component. Values for all properties except author,
     * time, and path (which are provided by the edit coordinate) will be copied
     * from this version.
     *
     * @param <V> the mutable version type
     * @param mc  edit coordinate to provide the author and path for the mutable version
     * @return the mutable version
     * @deprecated use the make analog with the transaction instead.
     */
    default <V extends Version> V makeAnalog(ManifoldCoordinate mc) {
        final int stampSequence = Get.stampService()
                .getStampSequence(this.getStatus(),
                        Long.MAX_VALUE,
                        mc.getEditCoordinate().getAuthorNidForChanges(),
                        mc.getModuleNidForAnalog(this),
                        mc.getPathNidForAnalog());
        return setupAnalog(stampSequence);
    }

    /**
     * Create a analog version with Long.MAX_VALUE as the time, indicating
     * the version is uncommitted. It is the responsibility of the caller to
     * add the mutable version to the commit manager when changes are complete
     * prior to committing the component. Values for all properties except time,
     * author, and path will be copied from this version.
     *
     * @param <V>         the mutable version type
     * @param mc   the manifold coordinate to determine author, module, path for the analog.
     * @param transaction the transaction that will govern persistence of the analog.
     * @return the mutable version
     */
    default <V extends Version> V makeAnalog(Transaction transaction, ManifoldCoordinate mc) {
        final int stampSequence = Get.stampService()
                .getStampSequence(transaction,
                        this.getStatus(),
                        Long.MAX_VALUE,
                        mc.getEditCoordinate().getAuthorNidForChanges(),
                        mc.getModuleNidForAnalog(this),
                        mc.getPathNidForAnalog());
        return setupAnalog(stampSequence);
    }

    /**
     * Values for all properties except time,
     * author, and path will be copied from this version.
     *
     * @param stampSequence
     * @param <V>
     * @return the mutable version
     */
    <V extends Version> V setupAnalog(int stampSequence);

    /**
     * Adds the additional uuids.
     *
     * @param uuids the uuid
     */
    public void addAdditionalUuids(UUID... uuids);

    /**
     * DeepEquals considers all fields, not just the stamp and the assumptions that the commit manager will not allow
     * more one version for a given stamp. This extra consideration is necessary to support uncommitted versions, that
     * may change in a multi-user environment, including that an individual author may make changes on more than one path
     * at a time.
     *
     * @param other the object to compare.
     * @return true if all fields are equal, otherwise false.
     */
    boolean deepEquals(Object other);

}
