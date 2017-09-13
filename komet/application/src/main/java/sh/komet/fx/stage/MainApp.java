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
package sh.komet.fx.stage;

//~--- JDK imports ------------------------------------------------------------
import java.net.MalformedURLException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.UUID;

//~--- non-JDK imports --------------------------------------------------------
import javafx.application.Application;
import javafx.application.Platform;

import javafx.event.EventHandler;

import javafx.fxml.FXMLLoader;

import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import static javafx.application.Application.launch;

import de.codecentric.centerdevice.javafxsvg.SvgImageLoaderFactory;

import sh.isaac.api.LookupService;
import sh.isaac.komet.iconography.Iconography;

import sh.komet.gui.util.FxGet;

import static sh.isaac.api.constants.Constants.DATA_STORE_ROOT_LOCATION_PROPERTY;
import static sh.isaac.api.constants.Constants.USER_CSS_LOCATION_PROPERTY;

//~--- classes ----------------------------------------------------------------
public class MainApp
        extends Application {
// TODO add TaskProgressView
// http://dlsc.com/2014/10/13/new-custom-control-taskprogressview/
// http://fxexperience.com/controlsfx/features/   

   public static final String SPLASH_IMAGE = "prism-splash.png";

   //~--- methods -------------------------------------------------------------
   /**
    * The main() method is ignored in correctly deployed JavaFX application. main() serves only as fallback in case the
    * application can not be launched through deployment artifacts, e.g., in IDEs with limited FX support. NetBeans
    * ignores main().
    *
    * @param args the command line arguments
    */
   public static void main(String[] args) {
      launch(args);
   }

   // Create drop label for identified components
   // Create walker panel
   // grow & shrink icons for tabs & tab panels...
   // for each tab group, add a + control to create new tabs...
   @Override
   public void start(Stage stage)
           throws Exception {
      // TODO have SvgImageLoaderFactory autoinstall as part of a HK2 service.
      SvgImageLoaderFactory.install();

      if (Files.exists(Paths.get("target", "data", "meta-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "target/data/meta-db.data");
      } else if (Files.exists(Paths.get("target", "data", "solor-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "target/data/solor-db.data");
      } else if (Files.exists(Paths.get("data", "meta-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "data/meta-db.data");
      } else if (Files.exists(Paths.get("data", "solor-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "data/solor-db.data");
      } else if (Files.exists(Paths.get("meta-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "meta-db.data");
      } else if (Files.exists(Paths.get("solor-db.data"))) {
         System.setProperty(DATA_STORE_ROOT_LOCATION_PROPERTY, "solor-db.data");
      } else {
         throw new UnsupportedOperationException(
                 "Can't find data directory... Working dir: " + System.getProperty("user.dir"));
      }

      // /Users/kec/isaac/semiotic-history/isaac/komet/css/src/main/resources/user.css
      if (setPropertyIfFileExists(
              USER_CSS_LOCATION_PROPERTY,
              Paths.get(
                      "/Users",
                      "kec",
                      "isaac",
                      "semiotic-history",
                      "isaac",
                      "komet",
                      "css",
                      "src",
                      "main",
                      "resources",
                      "user.css"))) {
      } else if (setPropertyIfFileExists(USER_CSS_LOCATION_PROPERTY, Paths.get("target", "data", "user.css"))) {
      } else if (setPropertyIfFileExists(USER_CSS_LOCATION_PROPERTY, Paths.get("data", "user.css"))) {
      } else if (setPropertyIfFileExists(USER_CSS_LOCATION_PROPERTY, Paths.get("user.css"))) {
      } else {
         throw new UnsupportedOperationException(
                 "Can't find user.css file... Working dir: " + System.getProperty("user.dir"));
      }

      LookupService.startupIsaac();

      FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/KometStageScene.fxml"));
      Parent root = loader.load();
      KometStageController controller = loader.getController();

      root.setId(UUID.randomUUID()
              .toString());

      Scene scene = new Scene(root);

      // GraphController.setSceneForControllers(scene);
      scene.getStylesheets()
              .add(System.getProperty(USER_CSS_LOCATION_PROPERTY));
      scene.getStylesheets()
              .add(Iconography.getStyleSheetStringUrl());

      // SNAPSHOT
      // Chronology
      // Reflector
      //
      // Logic, Language, Dialect, Chronology,
      // LILAC Reflector (LOGIC,
      // COLLD Reflector: Chronology of Logic, Language, and Dialect : COLLAD
      // COLLDAE Chronology of Logic, Langugage, Dialect, and Extension
      // CHILLDE
      // Knowledge, Language, Dialect, Chronology
      // KOLDAC
      stage.setTitle("KOMET Reflector");
      stage.setScene(scene);
      FxGet.statusMessageService()
              .addScene(scene, controller::reportStatus);
      stage.show();
      stage.setOnCloseRequest(this::handleShutdown);

      // ScenicView.show(scene);
   }

   private void handleShutdown(WindowEvent e) {
      LookupService.shutdownIsaac();
      LookupService.shutdownSystem();
      Platform.exit();
      System.exit(0);

   }
   //~--- set methods ---------------------------------------------------------

   /**
    *
    * @return true if the file existed, and the property was set.
    */
   private boolean setPropertyIfFileExists(String property, Path filePath)
           throws MalformedURLException {
      if (Files.exists(filePath)) {
         System.setProperty(property, filePath.toUri()
                 .toURL()
                 .toString());
         return true;
      }

      return false;
   }
}