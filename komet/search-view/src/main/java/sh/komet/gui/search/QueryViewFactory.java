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
package sh.komet.gui.search;

import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import org.glassfish.hk2.runlevel.RunLevel;
import org.jvnet.hk2.annotations.Service;
import sh.isaac.komet.iconography.Iconography;
import sh.komet.gui.interfaces.ExplorationNode;
import sh.komet.gui.contract.ExplorationNodeFactory;
import sh.komet.gui.manifold.Manifold;

/**
 *
 * @author kec
 */
@Service(name = "FLOWR Search Provider")
@RunLevel(value = 1)
public class QueryViewFactory implements ExplorationNodeFactory {

   @Override
   public ExplorationNode createExplorationNode(Manifold manifold, Consumer<Node> nodeConsumer) {
      try {
         FXMLLoader loader = new FXMLLoader(getClass().getResource("/sh/komet/gui/search/fxml/Query.fxml"));
         loader.load();
         QueryController queryController = loader.getController();
         queryController.setManifold(manifold);
         nodeConsumer.accept(queryController.getNode());
         return queryController;
      } catch (IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   @Override
   public String getMenuText() {
      return "FLOWR Query";
   }

   @Override
   public Node getMenuIcon() {
      return Iconography.FLOWR_SEARCH.getIconographic();
   }
   
}
