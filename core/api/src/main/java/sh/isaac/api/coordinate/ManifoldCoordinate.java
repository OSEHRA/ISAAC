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



package sh.isaac.api.coordinate;

//~--- JDK imports ------------------------------------------------------------

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import sh.isaac.api.ConceptProxy;
import sh.isaac.api.Edge;
import sh.isaac.api.Get;
import sh.isaac.api.TaxonomySnapshot;
import sh.isaac.api.chronicle.LatestVersion;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.concept.ConceptSpecification;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.ComponentNidVersion;
import sh.isaac.api.component.semantic.version.DescriptionVersion;
import sh.isaac.api.component.semantic.version.LogicGraphVersion;
import sh.isaac.api.logic.LogicalExpression;
import sh.isaac.api.util.time.DateTimeUtil;

import java.util.*;
import java.util.function.Function;

//~--- interfaces -------------------------------------------------------------

/**
 * The Interface ManifoldCoordinate.
 *
 * @author kec
 * TODO consider deprecation/deletion and switch to diagraph coordinate.
 */
public interface ManifoldCoordinate {

    static UUID getManifoldCoordinateUuid(ManifoldCoordinate manifoldCoordinate) {
        ArrayList<UUID> uuidList = new ArrayList<>();
        uuidList.add(manifoldCoordinate.getNavigationCoordinate().getNavigationCoordinateUuid());
        uuidList.add(manifoldCoordinate.getVertexSort().getVertexSortUUID());
        uuidList.add(manifoldCoordinate.getVertexStampFilter().getStampFilterUuid());
        uuidList.add(manifoldCoordinate.getEdgeStampFilter().getStampFilterUuid());
        uuidList.add(manifoldCoordinate.getLanguageStampFilter().getStampFilterUuid());
        uuidList.add(manifoldCoordinate.getLanguageCoordinate().getLanguageCoordinateUuid());
        StringBuilder sb = new StringBuilder(uuidList.toString());
        return UUID.nameUUIDFromBytes(sb.toString().getBytes());
    }

    default String toUserString() {
        StringBuilder sb = new StringBuilder("Manifold coordinate: ");
        sb.append("\nDigraph: ").append(getNavigationCoordinate().toUserString());
        sb.append("\n\nEdge filter:\n").append(getEdgeStampFilter().toUserString());
        sb.append("\n\nLanguage coordinate:\n").append(getLanguageCoordinate().toUserString());
        sb.append("\n\nLanguage filter:\n").append(getLanguageStampFilter().toUserString());
        sb.append("\n\nVertex filter:\n").append(getVertexStampFilter().toUserString());
        sb.append("\n\nVertex sort:\n").append(getVertexSort().getVertexSortName());
        return sb.toString();
    }

    TaxonomySnapshot getDigraphSnapshot();

    ManifoldCoordinateImmutable toManifoldCoordinateImmutable();

    default UUID getManifoldCoordinateUuid() {
        return getManifoldCoordinateUuid(this);
    }

    VertexSort getVertexSort();

    default int[] sortVertexes(int[] vertexConceptNids) {
        return getVertexSort().sortVertexes(vertexConceptNids, toManifoldCoordinateImmutable());
    }

    /**
     * In most cases, this coordinate will be the same object that is returned by {@link #getEdgeStampFilter()},
     * But, it may be a different, depending on the construction - for example, a use case like returning inactive
     * vertexes (concepts) linked by active edges (relationships).
     *
     * This filter is used on the vertexes (source and destination concepts)
     * in digraph operations, while {@link #getEdgeStampFilter()} is used
     * on the edges (relationships) themselves.
     *
     * @return The vertex stamp filter,
     */
    StampFilter getVertexStampFilter();

    /**
     * In most cases, this coordinate will be the same object that is returned by {@link #getVertexStampFilter()},
     * But, it may be a different, depending on the construction - for example, a use case like returning inactive
     * vertexes (concepts) linked by active edges (relationships).
     *
     * This filter is used on the edges (relationships) in digraph operations, while {@link #getVertexStampFilter()}
     * is used on the vertexes (concepts) themselves.
     *
     * @return The edge stamp filter,
     */
    StampFilter getEdgeStampFilter();

    /**
     * In most cases, this coordinate will be the same object that is returned by {@link #getVertexStampFilter()}
     * and {@link #getEdgeStampFilter()}.
     * @return the language stamp filter.
     */
    StampFilter getLanguageStampFilter();

    default LatestVersion<DescriptionVersion> getDescription(
            ConceptSpecification concept) {
        return this.getLanguageCoordinate().getDescription(concept.getNid(), this.getLanguageStampFilter());
    }
    default Optional<String> getDescriptionText(int conceptNid) {
        getLanguageCoordinate().getDescriptionText(conceptNid, this.getLanguageStampFilter());
        LatestVersion<DescriptionVersion> latestVersion = getDescription(conceptNid);
        if (latestVersion.isPresent()) {
            return Optional.of(latestVersion.get().getText());
        }
        return Optional.empty();
    }


    default Optional<String> getDescriptionText(ConceptSpecification concept) {
        return getDescriptionText(concept.getNid());
    }

    default LatestVersion<DescriptionVersion> getDescription(
            int conceptNid) {
        return this.getLanguageCoordinate().getDescription(conceptNid, this.getLanguageStampFilter());
    }


    default LatestVersion<DescriptionVersion> getDescription(
            List<SemanticChronology> descriptionList) {
        return this.getLanguageCoordinate().getDescription(descriptionList, this.getLanguageStampFilter());
    }

    default PremiseType getPremiseType() {
        if (getNavigationCoordinate().getNavigationConceptNids().contains(getLogicCoordinate().getInferredAssemblageNid())) {
            return PremiseType.INFERRED;
        }
        return PremiseType.STATED;
    }

    default NavigationCoordinateImmutable toNavigationCoordinateImmutable() {
        return getNavigationCoordinate().toNavigationCoordinateImmutable();
    }

    NavigationCoordinate getNavigationCoordinate();

    default LogicCoordinate getLogicCoordinate() {
        return getNavigationCoordinate().getLogicCoordinate();
    }

    default LanguageCoordinate getLanguageCoordinate() {
        return getLanguageCoordinate();
    }

    default Optional<String> getFullyQualifiedName(int nid, StampFilter filter) {
        return this.getLanguageCoordinate().getFullyQualifiedNameText(nid, filter);
    }

    default Optional<LogicalExpression> getStatedLogicalExpression(int conceptNid) {
        return getLogicalExpression(conceptNid, PremiseType.STATED);
    }

    default Optional<LogicalExpression> getStatedLogicalExpression(ConceptSpecification concept) {
        return getLogicalExpression(concept.getNid(), PremiseType.STATED);
    }

    default Optional<LogicalExpression> getLogicalExpression(ConceptSpecification concept, PremiseType premiseType) {
        return this.getLogicalExpression(concept.getNid(), premiseType);
    }

    default LatestVersion<LogicGraphVersion> getStatedLogicalDefinition(int conceptNid) {
        return this.getLogicalDefinition(conceptNid, PremiseType.STATED);
    }

    default LatestVersion<LogicGraphVersion> getStatedLogicalDefinition(ConceptSpecification concept) {
        return this.getLogicalDefinition(concept.getNid(), PremiseType.STATED);
    }

    default LatestVersion<LogicGraphVersion> getLogicalDefinition(ConceptSpecification concept, PremiseType premiseType) {
        return this.getLogicalDefinition(concept.getNid(), premiseType);
    }

    default LatestVersion<LogicGraphVersion> getLogicalDefinition(int conceptNid, PremiseType premiseType) {
        return this.getLogicCoordinate().getLogicGraphVersion(conceptNid, premiseType, this.getVertexStampFilter());
    }


    default Optional<String> getFullyQualifiedName(int nid) {
        return this.getLanguageCoordinate().getFullyQualifiedNameText(nid, this.getLanguageStampFilter());
    }

    default String getVertexLabel(int vertexConceptNid) {
        return getVertexSort().getVertexLabel(vertexConceptNid,
                getLanguageCoordinate().toLanguageCoordinateImmutable(),
                getLanguageStampFilter().toStampFilterImmutable());
    }

    default String getVertexLabel(ConceptSpecification vertexConcept) {
        return getVertexLabel(vertexConcept.getNid());
    }

    default String getPreferredDescriptionText(int conceptNid) {
        try {
            return VertexSortPreferredName.getRegularName(conceptNid, getLanguageCoordinate(), getLanguageStampFilter());
        } catch (NoSuchElementException ex) {
            return ex.getLocalizedMessage();
        }
    }

    default String getPreferredDescriptionText(ConceptSpecification concept) {
        return getPreferredDescriptionText(concept.getNid());
    }

    default String getFullyQualifiedDescriptionText(int conceptNid) {
        return VertexSortFullyQualifiedName.getFullyQualifiedName(conceptNid, getLanguageCoordinate(), getLanguageStampFilter());
    }

    default String getFullyQualifiedDescriptionText(ConceptSpecification concept) {
        return getFullyQualifiedDescriptionText(concept.getNid());
    }

    default LatestVersion<DescriptionVersion> getFullyQualifiedDescription(int conceptNid) {
        return getLanguageCoordinate().getFullyQualifiedDescription(conceptNid, getLanguageStampFilter());
    }

    default LatestVersion<DescriptionVersion> getFullyQualifiedDescription(ConceptSpecification concept) {
        return getFullyQualifiedDescription(concept.getNid());
    }


    default LatestVersion<DescriptionVersion> getPreferredDescription(int conceptNid) {
        return getLanguageCoordinate().getPreferredDescription(conceptNid, getLanguageStampFilter());
    }

    default LatestVersion<DescriptionVersion> getPreferredDescription(ConceptSpecification concept) {
        return getPreferredDescription(concept.getNid());
    }


    default OptionalInt getAcceptabilityNid(int descriptionNid, int dialectAssemblageNid) {
        ImmutableIntSet acceptabilityChronologyNids = Get.assemblageService().getSemanticNidsForComponentFromAssemblage(descriptionNid, dialectAssemblageNid);

        for (int acceptabilityChronologyNid: acceptabilityChronologyNids.toArray()) {
            SemanticChronology acceptabilityChronology = Get.assemblageService().getSemanticChronology(acceptabilityChronologyNid);
            LatestVersion<ComponentNidVersion> latestAcceptability = acceptabilityChronology.getLatestVersion(getLanguageStampFilter());
            if (latestAcceptability.isPresent()) {
                return OptionalInt.of(latestAcceptability.get().getComponentNid());
            }
        }
        return OptionalInt.empty();
    }

    default LatestVersion<LogicGraphVersion> getStatedLogicGraphVersion(int conceptNid) {
        return getLogicGraphVersion(conceptNid, PremiseType.STATED);
    }

    default LatestVersion<LogicGraphVersion> getInferredLogicGraphVersion(ConceptSpecification conceptSpecification) {
        return getLogicGraphVersion(conceptSpecification.getNid(), PremiseType.INFERRED);
    }

    default LatestVersion<LogicGraphVersion> getStatedLogicGraphVersion(ConceptSpecification conceptSpecification) {
        return getLogicGraphVersion(conceptSpecification.getNid(), PremiseType.STATED);
    }

    default LatestVersion<LogicGraphVersion> getInferredLogicGraphVersion(int conceptNid) {
        return getLogicGraphVersion(conceptNid, PremiseType.INFERRED);
    }

    default LatestVersion<LogicGraphVersion> getLogicGraphVersion(int conceptNid, PremiseType premiseType) {
        ConceptChronology concept = Get.concept(conceptNid);
        return concept.getLogicalDefinition(getEdgeStampFilter(), premiseType, this.getLogicCoordinate());
    }

    default Optional<LogicalExpression> getInferredLogicalExpression(ConceptSpecification spec) {
        return getLogicCoordinate().getInferredLogicalExpression(spec.getNid(), this.getEdgeStampFilter());
    }

    default Optional<LogicalExpression> getInferredLogicalExpression(int conceptNid) {
        return getLogicCoordinate().getLogicalExpression(conceptNid, PremiseType.INFERRED, this.getEdgeStampFilter());
    }

    default String toFqnConceptString(Object object) {
        return toConceptString(object, this::getFullyQualifiedDescriptionText);
    }

    default String toPreferredConceptString(Object object) {
        return toConceptString(object, this::getPreferredDescriptionText);
    }

    default Optional<LogicalExpression> getLogicalExpression(int conceptNid, PremiseType premiseType) {
        ConceptChronology concept = Get.concept(conceptNid);
        LatestVersion<LogicGraphVersion> logicalDef = concept.getLogicalDefinition(getVertexStampFilter(), premiseType, getLogicCoordinate());
        if (logicalDef.isPresent()) {
            return Optional.of(logicalDef.get().getLogicalExpression());
        }
        return Optional.empty();
    }

    default String toConceptString(Object object, Function<ConceptSpecification,String> toString) {
        if (object == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        if (object instanceof ConceptSpecification) {
            ConceptSpecification conceptSpecification = (ConceptSpecification) object;
            sb.append(toString.apply(conceptSpecification));
        } else if (object instanceof Collection) {
            Collection collection = (Collection) object;
            return toConceptString(collection.toArray(), toString);
        } else if (object.getClass().isArray()) {
            Object[] a = (Object[]) object;
            int iMax = a.length - 1;
            if (iMax == -1) {
                sb.append("[]");
            } else {
                sb.append('[');
                for (int i = 0; ; i++) {
                    sb.append(toConceptString(a[i], toString));
                    if (i == iMax)
                        return sb.append(']').toString();
                    sb.append(", ");
                }
            }
        } else if (object instanceof String) {
            String string = (String) object;
            if (string.indexOf(ConceptProxy.FIELD_SEPARATOR) > -1) {
                ConceptProxy conceptProxy = new ConceptProxy(string);
                sb.append(toConceptString(conceptProxy, toString));
            } else {
                sb.append(string);
            }
        } else if (object instanceof Long) {
            sb.append(DateTimeUtil.format((Long) object));
        } else {
            sb.append(object.toString());
        }
        return sb.toString();
    }



    default int[] getRootNids() {
        return this.getDigraphSnapshot().getRootNids();
    }

    default int[] getChildNids(ConceptSpecification parent) {
        return getChildNids(parent.getNid());
    }
    default int[] getChildNids(int parentNid) {
        return this.getVertexSort().sortVertexes(this.getDigraphSnapshot().getTaxonomyChildConceptNids(parentNid),
                this.toManifoldCoordinateImmutable());
    }

    default boolean isChildOf(ConceptSpecification child, ConceptSpecification parent) {
        return isChildOf(child.getNid(), parent.getNid());
    }
    default boolean isChildOf(int childNid, int parentNid) {
        return this.getDigraphSnapshot().isChildOf(childNid, parentNid);
    }

    default boolean isLeaf(ConceptSpecification concept) {
        return isLeaf(concept.getNid());
    }
    default boolean isLeaf(int nid) {
        return this.getDigraphSnapshot().isLeaf(nid);
    }

    default boolean isKindOf(ConceptSpecification child, ConceptSpecification parent) {
        return isKindOf(child.getNid(), parent.getNid());
    }
    default boolean isKindOf(int childNid, int parentNid) {
        return this.getDigraphSnapshot().isKindOf(childNid, parentNid);
    }

    default  ImmutableIntSet getKindOfNidSet(ConceptSpecification kind) {
        return getKindOfNidSet(kind.getNid());
    }
    default ImmutableIntSet getKindOfNidSet(int kindNid) {
        return this.getDigraphSnapshot().getKindOfConcept(kindNid);
    }

    default boolean isDescendentOf(ConceptSpecification descendant, ConceptSpecification ancestor) {
        return isDescendentOf(descendant.getNid(), ancestor.getNid());
    }
    default boolean isDescendentOf(int descendantNid, int ancestorNid) {
        return this.getDigraphSnapshot().isDescendentOf(descendantNid, ancestorNid);
    }

    default ImmutableCollection<Edge> getParentEdges(int parentNid) {
        return this.getDigraphSnapshot().getTaxonomyParentLinks(parentNid);
    }
    default ImmutableCollection<Edge> getParentEdges(ConceptSpecification parent) {
        return getParentEdges(parent.getNid());
    }

    default ImmutableCollection<Edge> getChildEdges(ConceptSpecification child) {
        return getChildEdges(child.getNid());
    }
    default ImmutableCollection<Edge> getChildEdges(int childNid) {
        return this.getDigraphSnapshot().getTaxonomyChildLinks(childNid);
    }

    default ImmutableCollection<ConceptSpecification> getRoots() {
        return IntLists.immutable.of(getRootNids()).collect(nid -> Get.conceptSpecification(nid));
    }

    default String getPathString() {
        StringBuilder sb = new StringBuilder();
        ConceptSpecification lastPath = this.getVertexStampFilter().getPathConceptForFilter();
        sb.append(this.getPreferredDescriptionText(lastPath));
        ConceptSpecification nextPath = this.getEdgeStampFilter().getPathConceptForFilter();
        if (!nextPath.equals(lastPath)) {
            lastPath = nextPath;
            sb.append(", " + this.getPreferredDescriptionText(lastPath));
        }
        nextPath = this.getLanguageStampFilter().getPathConceptForFilter();
        if (!nextPath.equals(lastPath)) {
            lastPath = nextPath;
            sb.append(", " + this.getPreferredDescriptionText(lastPath));
        }
        return sb.toString();
    }

}
