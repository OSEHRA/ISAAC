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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.Get;
import sh.isaac.api.component.concept.ConceptSpecification;

import sh.komet.gui.manifold.Manifold;

//~--- classes ----------------------------------------------------------------

/**
 *
 * @author kec
 */
public class ConceptForControlWrapper
         implements ConceptSpecification {
   private final Manifold manifold;
   private final int      conceptSequence;

   //~--- constructors --------------------------------------------------------

   public ConceptForControlWrapper(Manifold manifold, int conceptSequence) {
      if (conceptSequence == Integer.MAX_VALUE) {
         throw new IllegalStateException("Integer.MAX_VALUE for concept sequence.");
      }
      this.manifold        = manifold;
      this.conceptSequence = conceptSequence;
   }

   //~--- methods -------------------------------------------------------------

   @Override
   public String toString() {
      if (this.conceptSequence > 0) {
         Optional<String> description = getPreferedConceptDescriptionText();

         if (description.isPresent()) {
            return description.get();
         }
      }

      return "No description for: " + conceptSequence;
   }

   //~--- get methods ---------------------------------------------------------

   @Override
   public int getConceptSequence() {
      return conceptSequence;
   }

   @Override
   public String getFullySpecifiedConceptDescriptionText() {
      return this.manifold.getFullySpecifiedDescriptionText(this.conceptSequence);
   }

   @Override
   public Optional<String> getPreferedConceptDescriptionText() {
      return Optional.of(manifold.getPreferredDescriptionText(this.conceptSequence));
   }

   @Override
   public List<UUID> getUuidList() {
      return Get.concept(conceptSequence)
                .getUuidList();
   }

   @Override
   public boolean equals(Object obj) {
      if (obj instanceof ConceptSpecification) {
         return this.conceptSequence == ((ConceptSpecification) obj).getConceptSequence();
      }
      return false;
   }

   @Override
   public int hashCode() {
      return Long.hashCode(this.getNid());
   }
}
