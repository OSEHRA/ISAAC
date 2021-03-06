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



package sh.isaac.api;

import org.eclipse.collections.api.collection.ImmutableCollection;
import org.eclipse.collections.api.set.primitive.ImmutableIntSet;
import sh.isaac.api.navigation.Navigator;
import sh.isaac.api.tree.Tree;

/**
 * The Interface TaxonomySnapshot.
 *
 * @author kec
 */
public interface TaxonomySnapshot extends Navigator {

   /**
    * 
    * @param conceptNid concept to test if it is a leaf node
    * @return true if the node is a leaf (it has no children)
    */
   boolean isLeaf(int conceptNid);
   
   /**
    * true if child is any level descendent of parent, or child == parent
    *
    * @param childConceptNid the child id
    * @param parentConceptNid the parent id
    * @return true, if kind of
    */
   default boolean isKindOf(int childConceptNid, int parentConceptNid) {
      if (childConceptNid == parentConceptNid) {
         return true;
      }
      return isDescendentOf(childConceptNid, parentConceptNid);
   }
   
   /**
    * same as {@link #isKindOf(int, int)}, except doesn't allow descendentConceptNid == parentConceptNid
    *
    * @param descendantConceptNid the descendant id
    * @param ancestorConceptNid the parent id
    * @return true, if descendent of
    */
   boolean isDescendentOf(int descendantConceptNid, int ancestorConceptNid);

   /**
    * Gets the kind of nid set.
    *
    * @param rootConceptNid the root id
    * @return the kind of nid set
    */
   ImmutableIntSet getKindOfConcept(int rootConceptNid);

   /**
    * Gets the taxonomy child nids.
    *
    * @param parentConceptNid the parent id
    * @return the taxonomy child concept nids
    */
   int[] getTaxonomyChildConceptNids(int parentConceptNid);

   /**
    * Gets the taxonomy parent nids.
    *
    * @param childConceptNid the child id
    * @return the taxonomy parent nids
    */
   int[] getTaxonomyParentConceptNids(int childConceptNid);
   
   /**
    * For circumstances where there is more than one type of relationship in the taxonomy. 
    * @param parentConceptNid
    * @return an Iterable of all the parent taxonomy links. 
    */
   ImmutableCollection<Edge> getTaxonomyParentLinks(int parentConceptNid);

   /**
    * For circumstances where there is more than one type of relationship in the taxonomy. 
    * @param childConceptNid
    * @return an Iterable of all the child taxonomy links. 
    */
   ImmutableCollection<Edge> getTaxonomyChildLinks(int childConceptNid);
   
   /**
    * Gets the taxonomy tree.
    *
    * @return the taxonomy tree
    */
   Tree getTaxonomyTree();

   @Override
   default int[] getParentNids(int childNid) {
      return getTaxonomyParentConceptNids(childNid);
   }

   @Override
   default int[] getChildNids(int parentNid) {
      return getTaxonomyChildConceptNids(parentNid);
   }

   @Override
   default ImmutableCollection<Edge> getParentLinks(int childNid) {
      return getTaxonomyParentLinks(childNid);
   }

   @Override
   default ImmutableCollection<Edge> getChildLinks(int parentNid) {
      return getTaxonomyChildLinks(parentNid);
   }
}

