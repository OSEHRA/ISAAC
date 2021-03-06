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
package sh.isaac.api.query;

//~--- JDK imports ------------------------------------------------------------


import sh.isaac.api.Get;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.concept.ConceptVersion;
import sh.isaac.api.coordinate.StampFilter;

import java.util.HashMap;
import java.util.Map;

//~--- non-JDK imports --------------------------------------------------------

//~--- classes ----------------------------------------------------------------
/**
 * Returns components that are in the incoming For set and not in the set
 * returned from the computation of the clauses that are descendents of the
 * <code>Not</code> clause.
 *
 * @author kec
 */
public class Not
        extends ParentClause {

    /**
     * The for set.
     */
    NidSet forSet;

    /**
     * The not set.
     */
    NidSet notSet;
    private LetItemKey stampCoordinateKey;

    //~--- constructors --------------------------------------------------------
    /**
     * Default no arg constructor for Jaxb.
     */
    public Not() {
        super();
    }

    /**
     * Instantiates a new not.
     *
     * @param enclosingQuery the enclosing query
     * @param child the child
     * @param stampCoordinateKey
     */
    public Not(Query enclosingQuery, Clause child, LetItemKey stampCoordinateKey) {
        super(enclosingQuery, child);
        this.stampCoordinateKey = stampCoordinateKey;
    }

    //~--- methods -------------------------------------------------------------
    @Override
    public void resetResults() {
        this.forSet = null;
        this.notSet = null;
    }
    /**
     * Compute components.
     *
     * @param incomingComponents the incoming components
     * @return the nid set
     */
    @Override
    public Map<ConceptSpecification, NidSet> computeComponents(Map<ConceptSpecification, NidSet> incomingComponents) {
        this.forSet = this.enclosingQuery.For().get(this.getAssemblageForIteration());
        assert this.forSet != null;

        final NidSet activeSet = new NidSet();
        StampFilter stampFilter = (StampFilter) getEnclosingQuery().getLetDeclarations().get(stampCoordinateKey);

        Get.conceptService().getConceptChronologyStream(incomingComponents.get(this.getAssemblageForIteration()), false).forEach((ConceptChronology cc) -> {
            final LatestVersion<ConceptVersion> latestVersion
                    = cc.getLatestVersion(stampFilter);

            if (latestVersion.isPresent()) {
                activeSet.add(cc.getNid());
            }
        });
        getChildren().stream().forEach((c) -> {
            this.notSet.or(c.computeComponents(incomingComponents).get(c.getAssemblageForIteration()));
            setAssemblageForIteration(c.getAssemblageForIteration());
        });
        this.forSet = NidSet.of(activeSet);
        this.forSet.andNot(this.notSet);
        HashMap<ConceptSpecification, NidSet> resultsMap = new HashMap<>(incomingComponents);
        resultsMap.put(this.getAssemblageForIteration(), this.forSet);
        return resultsMap;
    }

    /**
     * Compute possible components.
     *
     * @param incomingPossibleComponents the incoming possible components
     * @return the nid set
     */
    @Override
    public Map<ConceptSpecification, NidSet> computePossibleComponents(Map<ConceptSpecification, NidSet> incomingPossibleComponents) {
        this.notSet = new NidSet();

        for (final Clause c : getChildren()) {
            for (final ClauseComputeType cp : c.getComputePhases()) {
                switch (cp) {
                    case PRE_ITERATION:
                        this.notSet.or(c.computePossibleComponents(incomingPossibleComponents).get(c.getAssemblageForIteration()));
                        break;

                    case ITERATION:
                        c.computePossibleComponents(incomingPossibleComponents);
                        break;

                    case POST_ITERATION:
                        c.computePossibleComponents(incomingPossibleComponents);
                        break;
                }
            }
            setAssemblageForIteration(c.getAssemblageForIteration());

        }

        return incomingPossibleComponents;
    }

    //~--- get methods ---------------------------------------------------------
    @Override
    public ClauseSemantic getClauseSemantic() {
        return ClauseSemantic.NOT;
    }

    /**
     * Gets the where clause.
     *
     * @return the where clause
     */
    @Override
    public WhereClause getWhereClause() {
        final WhereClause whereClause = new WhereClause();

        whereClause.setSemantic(ClauseSemantic.NOT);

        for (final Clause clause : getChildren()) {
            whereClause.getChildren()
                    .add(clause.getWhereClause());
        }

        return whereClause;
    }

    @Override
    public Clause[] getAllowedSubstutitionClauses() {
        return getParentClauses();
    }

    @Override
    public Clause[] getAllowedChildClauses() {
        return getAllClauses();
    }

    @Override
    public Clause[] getAllowedSiblingClauses() {
        return getAllClauses();
    }

}
