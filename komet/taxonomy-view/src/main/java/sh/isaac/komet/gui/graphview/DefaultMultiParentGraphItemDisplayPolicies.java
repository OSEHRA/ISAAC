/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sh.isaac.komet.gui.graphview;

import javafx.scene.Node;
import sh.isaac.api.Get;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.coordinate.StampFilterImmutable;
import sh.isaac.api.coordinate.StatusSet;
import sh.isaac.api.observable.coordinate.ObservableManifoldCoordinate;
import sh.isaac.komet.iconography.Iconography;

import java.util.EnumSet;

/**
 * DefaultMultiParentGraphItemDisplayPolicies
 *
 * @author kec
 * @author <a href="mailto:joel.kniaz@gmail.com">Joel Kniaz</a>
 *
 */
public class DefaultMultiParentGraphItemDisplayPolicies implements MultiParentGraphItemDisplayPolicies {
   private final ObservableManifoldCoordinate observableManifoldCoordinate;

   public DefaultMultiParentGraphItemDisplayPolicies(ObservableManifoldCoordinate observableManifoldCoordinate) {
      this.observableManifoldCoordinate = observableManifoldCoordinate;
   }
   
   
   @Override
   public Node computeGraphic(MultiParentGraphItem item) {
        if (item.isRoot()) {
            // TODO get dynamic icons from Assemblages.
            if (item.getConceptNid() == TermAux.PRIMORDIAL_PATH.getNid()) {
                return Iconography.SOURCE_BRANCH_1.getIconographic();
            }
            return Iconography.TAXONOMY_ROOT_ICON.getIconographic();
        } 
       
       if (item.getTypeNid() != TermAux.IS_A.getNid()) {
           // TODO get dynamic icons from Assemblages.
           if (item.getTypeNid() == TermAux.PATH_ORIGIN_ASSEMBLAGE.getNid()) {
               return Iconography.SOURCE_BRANCH_1.getIconographic();
           }
          return Iconography.ALERT_CONFIRM.getIconographic();
       } 
       
       if (item.isDefined() && (item.isMultiParent() || item.getMultiParentDepth() > 0)) {
         if (item.isSecondaryParentOpened()) {
            return Iconography.TAXONOMY_DEFINED_MULTIPARENT_OPEN.getIconographic();
         } else {
            return Iconography.TAXONOMY_DEFINED_MULTIPARENT_CLOSED.getIconographic();
         }
      } else if (!item.isDefined() && (item.isMultiParent() || item.getMultiParentDepth() > 0)) {
         if (item.isSecondaryParentOpened()) {
            return Iconography.TAXONOMY_PRIMITIVE_MULTIPARENT_OPEN.getIconographic();
         } else {
            return Iconography.TAXONOMY_PRIMITIVE_MULTIPARENT_CLOSED.getIconographic();
         }
      } else if (item.isDefined() && !item.isMultiParent()) {
         return Iconography.TAXONOMY_DEFINED_SINGLE_PARENT.getIconographic();
      }
      return Iconography.TAXONOMY_PRIMITIVE_SINGLE_PARENT.getIconographic();
   }

   @Override
   public boolean shouldDisplay(MultiParentGraphItem treeItem) {
       if (treeItem.isRoot()) {
           return true;
       }
      int conceptNid = treeItem.getConceptNid();
       StampFilterImmutable vertexStampFilter = observableManifoldCoordinate.getVertexStampFilter().toStampFilterImmutable();
       StatusSet allowedStates = vertexStampFilter.getAllowedStates();
           EnumSet<Status> states = Get.conceptActiveService().getConceptStates(conceptNid, vertexStampFilter);
           for (Status state: states) {
               if (allowedStates.contains(state)) {
                   return true;
               }
           }
           return false;
   }
}
