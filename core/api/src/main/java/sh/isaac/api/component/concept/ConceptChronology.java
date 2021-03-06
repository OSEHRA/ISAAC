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



/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package sh.isaac.api.component.concept;


import java.util.List;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.coordinate.LanguageCoordinate;
import sh.isaac.api.coordinate.LogicCoordinate;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampFilter;
import sh.isaac.api.logic.NodeSemantic;

/**
 * The Interface ConceptChronology.
 *
 * @author kec
 */
public interface ConceptChronology
        extends Chronology, 
                ConceptSpecification {
   /**
    * A test for validating that a concept contains a description. Used
    * to validate concept proxies or concept specs at runtime.
    * @param descriptionText text to match against.
    * @return true if any version of a description matches this text.
    */
   boolean containsDescription(String descriptionText);

   /**
    * A test for validating that a concept contains an active description. Used
    * to validate concept proxies or concept specifications at runtime.
    * @param descriptionText text to match against.
    * @param stampFilter coordinate to determine if description is active.
    * @return true if any version of a description matches this text.
    */
   boolean containsDescription(String descriptionText, StampFilter stampFilter);

   /**
    * Gets the concept description list.
    *
    * @return the concept description list
    */
   List<SemanticChronology> getConceptDescriptionList();

   /**
    * Gets the fully specified description.
    *
    * @param languageCoordinate the language coordinate
    * @param stampFilter the stamp coordinate
    * @return the fully specified description
    */
   LatestVersion<? extends DescriptionVersion> getFullyQualifiedNameDescription(LanguageCoordinate languageCoordinate,
                                                                                StampFilter stampFilter);
   
   /**
    * Gets the fully specified description.
    *
    * @param coordinate the manifold coordinate that specifies both the stamp coordinate and the language 
    * coordinate.
    * @return the fully specified description
    */
   default LatestVersion<? extends DescriptionVersion> getFullySpecifiedDescription(ManifoldCoordinate coordinate) {
      return getFullyQualifiedNameDescription(coordinate.getLanguageCoordinate(), coordinate.getViewStampFilter());
   }

   /**
    * Gets the logical definition.
    *
    * @param stampFilter the stamp coordinate
    * @param premiseType the premise type
    * @param logicCoordinate the logic coordinate
    * @return the logical definition
    */
   LatestVersion<LogicGraphVersion> getLogicalDefinition(StampFilter stampFilter,
                                                         PremiseType premiseType,
                                                         LogicCoordinate logicCoordinate);
   
   default boolean isSufficientlyDefined(StampFilter stampFilter,
                                         LogicCoordinate logicCoordinate) {
      LatestVersion<LogicGraphVersion> latestDefinition = getLogicalDefinition(stampFilter,
         PremiseType.STATED,
         logicCoordinate);
      if (latestDefinition.isPresent()) {
         return latestDefinition.get().getLogicalExpression().contains(NodeSemantic.SUFFICIENT_SET);
      }
      return false;
   }

   /**
    * Return a formatted text report showing chronology of logical definitions
    * for this concept, according to the provided parameters.
    *
    * @param stampFilter specifies the ordering and currency of versions.
    * @param premiseType Stated or inferred premise type
    * @param logicCoordinate specifies the assemblages where the definitions are stored.
    * @return the logical definition chronology report
    */
   String getLogicalDefinitionChronologyReport(StampFilter stampFilter,
                                               PremiseType premiseType,
                                               LogicCoordinate logicCoordinate);

   /**
    * Gets the preferred description.
    *
    * @param languageCoordinate the language coordinate
    * @param stampFilter the stamp coordinate
    * @return the preferred description
    */
   LatestVersion<? extends DescriptionVersion> getPreferredDescription(LanguageCoordinate languageCoordinate,
                                                                       StampFilter stampFilter);


   /**
    * Gets the preferred description.
    *
    * @param coordinate the language coordinate and the stamp coordinate
    * @return the preferred description
    */
   default LatestVersion<? extends DescriptionVersion> getPreferredDescription(ManifoldCoordinate coordinate) {
      return getPreferredDescription(coordinate.getLanguageCoordinate(), coordinate.getViewStampFilter());
   }

   /**
    * 
    * @return a string with more detailed information about all taxonomy information and semantic extensions for the concept. 
    */
   String toLongString();
}

