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
package sh.isaac.api.query.clauses;

import sh.isaac.api.Get;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.query.*;
import sh.isaac.api.query.properties.ReferencedComponentClause;

import java.util.EnumSet;
import java.util.Map;

/**
 *
 * @author kec
 */
public class ReferencedComponentIs
        extends LeafClause implements ReferencedComponentClause {

   /** The concept spec key. */
   LetItemKey referencedComponentSpecKey;


   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new refset contains concept.
    */
   public ReferencedComponentIs() {}

   /**
    * Instantiates a new assemblage contains concept.
    *
    * @param enclosingQuery the enclosing query
    * @param conceptSpecKey the concept spec key
    */
   public ReferencedComponentIs(Query enclosingQuery,
                                LetItemKey conceptSpecKey) {
      super(enclosingQuery);
      this.referencedComponentSpecKey    = conceptSpecKey;
   }

   //~--- methods -------------------------------------------------------------

   /**
    * Compute possible components.
    *
    * @param incomingPossibleComponents the incoming possible components
    * @return the nid set
    */
   @Override
   public Map<ConceptSpecification, NidSet> computePossibleComponents(Map<ConceptSpecification, NidSet> incomingPossibleComponents) {
    ConceptSpecification conceptSpec = (ConceptSpecification) this.enclosingQuery.getLetDeclarations().get(referencedComponentSpecKey);

    int conceptNid = conceptSpec.getNid();
    NidSet possibleComponents = incomingPossibleComponents.get(getAssemblageForIteration());
    for (int nid: possibleComponents.asArray()) {
        SemanticChronology sc = Get.assemblageService().getSemanticChronology(nid);
        if (sc.getReferencedComponentNid() != conceptNid) {
            possibleComponents.remove(nid);
        }
    }
    return incomingPossibleComponents;
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the compute phases.
    *
    * @return the compute phases
    */
   @Override
   public EnumSet<ClauseComputeType> getComputePhases() {
      return PRE_ITERATION;
   }


    @Override
    public ClauseSemantic getClauseSemantic() {
        return ClauseSemantic.REFERENCED_COMPONENT_IS;
    }
   

   /**
    * Gets the where clause.
    *
    * @return the where clause
    */
   @Override
   public WhereClause getWhereClause() {
      final WhereClause whereClause = new WhereClause();

      whereClause.setSemantic(ClauseSemantic.REFERENCED_COMPONENT_IS);
      whereClause.getLetKeys()
                 .add(this.referencedComponentSpecKey);
      return whereClause;
   }

    @Override
    public LetItemKey getReferencedComponentSpecKey() {
        return referencedComponentSpecKey;
    }

    @Override
    public void setReferencedComponentSpecKey(LetItemKey referencedComponentSpecKey) {
        this.referencedComponentSpecKey = referencedComponentSpecKey;
    }
   
}

