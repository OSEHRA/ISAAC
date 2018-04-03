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
package sh.komet.progress.view;

import javax.inject.Singleton;
import org.jvnet.hk2.annotations.Service;
import javafx.scene.Node;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.contract.ExplorationNodeFactory;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.manifold.Manifold;
import sh.komet.gui.manifold.Manifold.ManifoldGroup;

/**
 *
 * @author kec
 */
/**
 *
 * @author kec
 */
@Service(name = "Completed task panel factory")
@Singleton
public class TasksCompletedNodeFactory 
         implements ExplorationNodeFactory {
   
   public static final String TITLE_BASE = "Completions";
   public static final String TITLE_BASE_SINGULAR = "Completion";
   @Override
   public ExplorationNode createNode(Manifold manifold) {
      //TODO why does this exist?  Its returning the same thing as the TaskProgressNodeFactory....
      TaskProgressNode taskProgressNode = new TaskProgressNode(manifold);
      return taskProgressNode;
   }

   //~--- get methods ---------------------------------------------------------

   @Override
   public Node getMenuIcon() {
      return Iconography.CHECKERED_FLAG.getIconographic();
   }

   @Override
   public String getMenuText() {
      return TITLE_BASE;
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public PanelPlacement getPanelPlacement() {
      return null;
   }

  /** 
   * {@inheritDoc}
   */
  @Override
  public ManifoldGroup[] getDefaultManifoldGroups() {
     return new ManifoldGroup[] {ManifoldGroup.UNLINKED};
  }
}
