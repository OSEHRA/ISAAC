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
package sh.isaac.solor.direct;

import static sh.isaac.api.logic.LogicalExpressionBuilder.And;
import static sh.isaac.api.logic.LogicalExpressionBuilder.ConceptAssertion;
import static sh.isaac.api.logic.LogicalExpressionBuilder.NecessarySet;
import static sh.isaac.api.logic.LogicalExpressionBuilder.SomeRole;
import static sh.isaac.api.logic.LogicalExpressionBuilder.SufficientSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.eclipse.collections.impl.factory.primitive.IntSets;
import sh.isaac.MetaData;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.DataTarget;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.TaxonomyService;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.collections.NidSet;
import sh.isaac.api.commit.ChangeCheckerMode;
import sh.isaac.api.commit.StampService;
import sh.isaac.api.component.semantic.SemanticBuilder;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.MutableLogicGraphVersion;
import sh.isaac.api.component.semantic.version.brittle.Rf2Relationship;
import sh.isaac.api.coordinate.PremiseType;
import sh.isaac.api.coordinate.StampFilter;
import sh.isaac.api.coordinate.StampFilterImmutable;
import sh.isaac.api.coordinate.StampPosition;
import sh.isaac.api.coordinate.StampPositionImmutable;
import sh.isaac.api.coordinate.StatusSet;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.logic.IsomorphicResults;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.logic.LogicalExpressionBuilder;
import sh.isaac.api.logic.assertions.Assertion;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.api.util.time.DateTimeUtil;
import sh.isaac.model.semantic.version.LogicGraphVersionImpl;

/**
 *
 * @author kec
 */
public class LogicGraphTransformerAndWriter extends TimedTaskWithProgressTracker<Void> {

    /**
     * The never role group set.
     */
    private final NidSet neverRoleGroupSet = new NidSet();

    private final NidSet definingCharacteristicSet = new NidSet();

    private final int isaNid = TermAux.IS_A.getNid();

    private final int legacyImplicationAssemblageNid = TermAux.RF2_LEGACY_RELATIONSHIP_IMPLICATION_ASSEMBLAGE.getNid();

    private final int sufficientDefinition = TermAux.SUFFICIENT_CONCEPT_DEFINITION.getNid();

    private final int primitiveDefinition = TermAux.NECESSARY_BUT_NOT_SUFFICIENT_CONCEPT_DEFINITION.getNid();
    private final int solorOverlayModuleNid = TermAux.SOLOR_OVERLAY_MODULE.getNid();
    private final int statedAssemblageNid = TermAux.EL_PLUS_PLUS_STATED_ASSEMBLAGE.getNid();
    private final int inferredAssemblageNid = TermAux.EL_PLUS_PLUS_INFERRED_ASSEMBLAGE.getNid();
    private final int authorNid = TermAux.USER.getNid();
    private final int developmentPathNid = TermAux.DEVELOPMENT_PATH.getNid();
    private final TaxonomyService taxonomyService;

    {
        this.neverRoleGroupSet.add(TermAux.PART_OF.getNid());
        this.neverRoleGroupSet.add(TermAux.LATERALITY.getNid());
        this.neverRoleGroupSet.add(TermAux.HAS_ACTIVE_INGREDIENT.getNid());
        this.neverRoleGroupSet.add(TermAux.HAS_DOSE_FORM.getNid());

        this.definingCharacteristicSet.add(MetaData.INFERRED_PREMISE_TYPE____SOLOR.getNid());
        this.definingCharacteristicSet.add(MetaData.STATED_PREMISE_TYPE____SOLOR.getNid());
    }

    private final Semaphore writeSemaphore;
    private final List<TransformationGroup> transformationRecords;
    private final List<IndexBuilderService> indexers;
    private final ImportType importType;
    private final Instant commitTime;
    private Transaction transaction;

    /**
     * @param transaction - optional, does NOT commit if transaction if provided, if not provided, creates its own transaction and commits.
     * @param transformationRecords
     * @param writeSemaphore
     * @param importType
     * @param commitTime
     */
    public LogicGraphTransformerAndWriter(Transaction transaction, List<TransformationGroup> transformationRecords,
            Semaphore writeSemaphore, ImportType importType, Instant commitTime) {
        this.transaction = transaction;
        this.transformationRecords = transformationRecords;
        this.writeSemaphore = writeSemaphore;
        this.importType = importType;
        this.commitTime = commitTime;
        this.writeSemaphore.acquireUninterruptibly();
        this.taxonomyService = Get.taxonomyService();
        indexers = LookupService.get().getAllServices(IndexBuilderService.class);
        updateTitle("EL++ transformation");
        updateMessage("");
        addToTotalWork(transformationRecords.size());
        Get.activeTasks().add(this);
    }

    private void index(Chronology chronicle) {
        if (chronicle instanceof SemanticChronology) {
            this.taxonomyService.updateTaxonomy((SemanticChronology) chronicle);
        }
        for (IndexBuilderService indexer : indexers) {
            indexer.indexNow(chronicle);
        }
    }

    @Override
    protected Void call() throws Exception {
        try {
            boolean commitTransaction = this.transaction == null;
            if (commitTransaction) {
                transaction = Get.commitService().newTransaction(Optional.of("LogicGraphTransformer"), ChangeCheckerMode.INACTIVE, false);
            }
            int count = 0;
            for (TransformationGroup transformationGroup : transformationRecords) {
                transformRelationships(transformationGroup.conceptNid, transformationGroup.semanticNids, transformationGroup.getPremiseType());
                if (count % 1000 == 0) {
                    updateMessage("Processing concept: " + Get.conceptDescriptionText(transformationGroup.conceptNid));
                }
                count++;
                completedUnitOfWork();
            }

            if (commitTransaction) {
                transaction.commit("Logic graph transformer").get();
            }
            return null;
        } catch (Throwable t) {
            LOG.error(t.getLocalizedMessage(), t);
            throw t;
        } finally {
            //LOG.info("Releasing semaphore. Permits available: " + this.writeSemaphore.availablePermits());
            this.writeSemaphore.release();
            //LOG.info("Released semaphore. Permits available: " + this.writeSemaphore.availablePermits());
            Get.activeTasks().remove(this);
        }
    }

    private void transformAtTimePath(StampPosition stampPosition, int conceptNid, List<SemanticChronology> relationships, PremiseType premiseType) {

        final LogicalExpressionBuilder logicalExpressionBuilder = Get.logicalExpressionBuilderService()
                .getLogicalExpressionBuilder();
        final ArrayList<Assertion> assertions = new ArrayList<>();
        final HashMap<Integer, ArrayList<Assertion>> groupedAssertions = new HashMap<>();
/*
StatusSet allowedStates,
                         StampPosition stampPosition,
                         ImmutableIntSet authorNids,
                         StampPath stampPath
 */
        StampFilterImmutable stampFilter = StampFilterImmutable.make(StatusSet.ACTIVE_ONLY, stampPosition,
                IntSets.immutable.empty(), IntLists.immutable.empty());
        
        // only process active concepts... TODO... Process all
        if (Get.conceptActiveService().isConceptActive(conceptNid, stampFilter)) {

            // for each relationship, add to assertion or grouped assertions. 
            for (final SemanticChronology rb : relationships) {
                LatestVersion<Rf2Relationship> latestRel = rb.getLatestVersion(stampFilter);
                if (latestRel.isPresent()) {
                    Rf2Relationship relationship = latestRel.get();
    
                    if (definingCharacteristicSet.contains(relationship.getCharacteristicNid())) {
    
                        if (relationship.getRelationshipGroup() == 0) {
    
                            if (isaNid == relationship.getTypeNid()) {
                                assertions.add(ConceptAssertion(relationship.getDestinationNid(),
                                        logicalExpressionBuilder));
                            } else {
                                if (this.neverRoleGroupSet.contains(relationship.getTypeNid())) {
                                    assertions.add(SomeRole(relationship.getTypeNid(),
                                            ConceptAssertion(relationship.getDestinationNid(),
                                                    logicalExpressionBuilder)));
                                } else {
                                    assertions.add(SomeRole(MetaData.ROLE_GROUP____SOLOR.getNid(),
                                            And(SomeRole(relationship.getTypeNid(),
                                                    ConceptAssertion(relationship.getDestinationNid(),
                                                            logicalExpressionBuilder)))));
                                }
                            }
                        } else {
                            ArrayList<Assertion> groupAssertions = groupedAssertions.get(relationship.getRelationshipGroup());
    
                            if (groupAssertions == null) {
                                groupAssertions = new ArrayList<>();
                                groupedAssertions.put(relationship.getRelationshipGroup(), groupAssertions);
                            }
                            groupAssertions.add(SomeRole(relationship.getTypeNid(),
                                    ConceptAssertion(relationship.getDestinationNid(),
                                            logicalExpressionBuilder)));
                        }
                    }
                }
            }
    
            // handle relationship groups
            for (final ArrayList<Assertion> groupAssertions : groupedAssertions.values()) {
                assertions.add(SomeRole(MetaData.ROLE_GROUP____SOLOR.getNid(),
                        And(groupAssertions.toArray(new Assertion[groupAssertions.size()]))));
            }
    
            if (assertions.size() > 0) {
                boolean defined = false; // Change to use list instead of stream...
                Stream<SemanticChronology> implicationChronologyStream = Get.assemblageService()
                        .getSemanticChronologyStreamForComponentFromAssemblage(conceptNid, legacyImplicationAssemblageNid, true);
                List<SemanticChronology> implicationList = implicationChronologyStream.collect(Collectors.toList());
                if (implicationList.size() == 1) {
                    SemanticChronology implicationChronology = implicationList.get(0);
                    LatestVersion<ComponentNidVersion> latestImplication = implicationChronology.getLatestVersion(stampFilter);
                    if (latestImplication.isPresent()) {
                        ComponentNidVersion definitionStatus = latestImplication.get();
                        if (definitionStatus.getComponentNid() == sufficientDefinition) {
                            defined = true;
                        } else if (definitionStatus.getComponentNid() == primitiveDefinition) {
                            defined = false;
                        } else {
                            throw new RuntimeException("Unexpected concept definition status: " + definitionStatus);
                        }
                    } else {
                        StringBuilder builder = new StringBuilder();
                        builder.append("No implication to: ");
                        builder.append(Get.conceptDescriptionText(conceptNid));
                        builder.append("\n");
                        builder.append(Get.concept(conceptNid).toString());
                        LOG.error(builder.toString());
    
                    }
                    if (defined) {
                        SufficientSet(And(assertions.toArray(new Assertion[assertions.size()])));
                    } else {
                        NecessarySet(And(assertions.toArray(new Assertion[assertions.size()])));
                    }
    
                    final LogicalExpression le = logicalExpressionBuilder.build();
                    le.setConceptBeingDefinedNid(conceptNid);
                    if (le.isMeaningful()) {


                    // TODO [graph] what if the modules are different across the graph rels?
                    addLogicGraph(conceptNid,
                            le,
                            premiseType,
                            stampPosition.getTime(),
                            solorOverlayModuleNid, stampFilter);
                } else {
                    LOG.error("expression not meaningful?");
                }
            }

            }
        }

    }
    ConceptProxy tenosynovitisProxy = new ConceptProxy("Tenosynovitis (disorder)", UUID.fromString("51c3117f-245b-3fab-a704-4687d6b55de4"));
    ConceptProxy anatomicalStructureProxy = new ConceptProxy("Anatomical structure (body structure)", UUID.fromString("bcefc7ae-7512-3893-ade1-8eae817b4f0d"));

    /**
     * Transform relationships.
     *
     * @param premiseType  stated or inferred
     */
    private void transformRelationships(int conceptNid, int[] relNids, PremiseType premiseType) {
        updateMessage("Converting " + premiseType + " relationships into logic graphs");

        List<SemanticChronology> relationshipChronologiesForConcept = new ArrayList<>();
        TreeSet<StampPosition> stampPositionsToProcess = new TreeSet<>();
        for (int relNid : relNids) {
            SemanticChronology relationshipChronology = Get.assemblageService().getSemanticChronology(relNid);
            for (int stamp : relationshipChronology.getVersionStampSequences()) {
                StampService stampService = Get.stampService();
                stampPositionsToProcess.add(StampPositionImmutable.make(stampService.getTimeForStamp(stamp), stampService.getPathNidForStamp(stamp)));
            }
            relationshipChronologiesForConcept.add(relationshipChronology);
        }
        for (StampPosition stampPosition : stampPositionsToProcess) {
            // SNOMED released OWL format on "2019-07-31T00:00:00Z", and retired all but is-a relationships
            // in stated and inferred tables... So only use stated and inferred relationships prior to that date. Use the
            // OWL definitions after that date.

            if (stampPosition.getTime() < DateTimeUtil.parseWithZone("2019-07-31T00:00:00Z")) {
                transformAtTimePath(stampPosition, conceptNid, relationshipChronologiesForConcept, premiseType);
            }
        }
    }

    /**
     * Adds the relationship graph.
     *  @param conceptNid the conceptNid
     * @param logicalExpression the logical expression
     * @param premiseType the premise type
     * @param time the time for the commit.
     * @param moduleNid the module
     * @param stampFilter for determining current version if a graph already
     */
    public void addLogicGraph(int conceptNid,
                              LogicalExpression logicalExpression,
                              PremiseType premiseType,
                              long time,
                              int moduleNid, StampFilter stampFilter) {
        if (time == Long.MAX_VALUE) {
            time = commitTime.toEpochMilli();
        }
        int graphAssemblageNid = statedAssemblageNid;
        if (premiseType == PremiseType.INFERRED) {
            graphAssemblageNid = inferredAssemblageNid;
        }

        final SemanticBuilder<? extends SemanticChronology> sb = Get.semanticBuilderService().getLogicalExpressionBuilder(logicalExpression,
                conceptNid,
                graphAssemblageNid);

        UUID nameSpace = TermAux.EL_PLUS_PLUS_STATED_ASSEMBLAGE.getPrimordialUuid();
        if (premiseType == PremiseType.INFERRED) {
            nameSpace = TermAux.EL_PLUS_PLUS_INFERRED_ASSEMBLAGE.getPrimordialUuid();
        }

        // See if a semantic already exists in this assemblage referencing this concept... 
        ImmutableIntSet graphNidsForComponent = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(conceptNid, graphAssemblageNid);
        if (!graphNidsForComponent.isEmpty()) {
//            LOG.info("Existing graph found for: " + Get.conceptDescriptionText(conceptNid));
            if (graphNidsForComponent.size() != 1) {
                throw new IllegalStateException("To many graphs for component: " + Get.conceptDescriptionText(conceptNid));
            }
            SemanticChronology existingGraph = Get.assemblageService().getSemanticChronology(graphNidsForComponent.intIterator().next());
            LatestVersion<LogicGraphVersionImpl> latest = existingGraph.getLatestVersion(stampFilter);
            if (latest.isPresent()) {
                LogicGraphVersionImpl logicGraphLatest = latest.get();
                LogicalExpression latestExpression = logicGraphLatest.getLogicalExpression();
                IsomorphicResults isomorphicResults = logicalExpression.findIsomorphisms(latestExpression);
                if (!isomorphicResults.equivalent()) {
                    int stamp = Get.stampService().getStampSequence(transaction, Status.ACTIVE, time, authorNid, moduleNid, developmentPathNid);
                    final MutableLogicGraphVersion newVersion
                            = existingGraph.createMutableVersion(transaction, stamp);

                    newVersion.setGraphData(logicalExpression.getData(DataTarget.INTERNAL));
                    index(existingGraph);
                    Get.assemblageService().writeSemanticChronology(existingGraph);
                }
//                LOG.info("Isomorphic results: " + isomorphicResults);
            }
        } else {

            // Create UUID from seed and assign SemanticBuilder the value
            final UUID generatedGraphPrimordialUuid = UuidT5Generator.get(nameSpace, Get.concept(conceptNid).getPrimordialUuid().toString());

            sb.setPrimordialUuid(generatedGraphPrimordialUuid);

            final ArrayList<Chronology> builtObjects = new ArrayList<>();
            int stamp = Get.stampService().getStampSequence(Status.ACTIVE, time, authorNid, moduleNid, developmentPathNid);
            final SemanticChronology sci = (SemanticChronology) sb.build(transaction, stamp,
                    builtObjects);
            // There should be no other build objects, so ignore the builtObjects list...

            if (builtObjects.size() != 1) {
                throw new IllegalStateException("More than one build object: " + builtObjects);
            }
            index(sci);
            Get.assemblageService().writeSemanticChronology(sci);

        }

    }

}
