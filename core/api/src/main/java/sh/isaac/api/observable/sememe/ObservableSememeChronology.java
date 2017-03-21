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



package sh.isaac.api.observable.sememe;

//~--- non-JDK imports --------------------------------------------------------

import javafx.beans.property.IntegerProperty;

import sh.isaac.api.State;
import sh.isaac.api.component.sememe.SememeObject;
import sh.isaac.api.component.sememe.SememeType;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.observable.ObservableChronology;
import sh.isaac.api.observable.sememe.version.ObservableSememeVersion;

//~--- interfaces -------------------------------------------------------------

/**
 * The Interface ObservableSememeChronology.
 *
 * @author kec
 * @param <V> the value type
 */
public interface ObservableSememeChronology<V extends ObservableSememeVersion>
        extends ObservableChronology<V>, SememeObject {
   /**
    * Assemblage sequence property.
    *
    * @return the integer property
    */
   IntegerProperty assemblageSequenceProperty();

   /**
    * Create a mutable version the specified stampSequence. It is the responsibility of the caller to
    * add persist the chronicle when changes to the mutable version are complete .
    *
    * @param <M> the generic type
    * @param type SememeVersion type
    * @param stampSequence stampSequence that specifies the status, time, author, module, and path of this version.
    * @return the mutable version
    */
   <M extends V> M createMutableVersion(Class<M> type, int stampSequence);

   /**
    * Create a mutable version with Long.MAX_VALUE as the time, indicating
    * the version is uncommitted. It is the responsibility of the caller to
    * add the mutable version to the commit manager when changes are complete
    * prior to committing the component.
    *
    * @param <M> the generic type
    * @param type SememeVersion type
    * @param state state of the created mutable version
    * @param ec edit coordinate to provide the author, module, and path for the mutable version
    * @return the mutable version
    */
   <M extends V> M createMutableVersion(Class<M> type, State state, EditCoordinate ec);

   /**
    * Referenced component nid property.
    *
    * @return the integer property
    */
   IntegerProperty referencedComponentNidProperty();

   /**
    * Sememe sequence property.
    *
    * @return the integer property
    */
   IntegerProperty sememeSequenceProperty();

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the sememe type.
    *
    * @return the sememe type
    */
   SememeType getSememeType();
}
