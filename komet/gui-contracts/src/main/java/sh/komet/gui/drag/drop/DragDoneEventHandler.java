/*
 * Copyright 2017 Organizations participating in ISAAC, ISAAC's DragAndDrop, and SOLOR development include the 
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
package sh.komet.gui.drag.drop;

import javafx.event.EventHandler;
import javafx.scene.input.DragEvent;

/**
 * {@link DragDoneEventHandler}
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class DragDoneEventHandler implements EventHandler<DragEvent> {

   /**
    * @param event
    * @see javafx.event.EventHandler#handle(javafx.event.Event)
    */
   @Override
   public void handle(DragEvent event) {
      DragRegistry.dragComplete();
   }
}
