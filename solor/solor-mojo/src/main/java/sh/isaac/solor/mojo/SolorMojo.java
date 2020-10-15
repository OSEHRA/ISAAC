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
package sh.isaac.solor.mojo;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Future;

import javafx.concurrent.Task;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import sh.isaac.api.ConfigurationService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.ConfigurationService.BuildMode;
import sh.isaac.api.classifier.ClassifierResults;
import sh.isaac.api.commit.ChangeCheckerMode;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.coordinate.ManifoldCoordinate;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.solor.direct.*;
import sh.isaac.solor.direct.rxnorm.RxNormDirectImporter;

/**
 * @author kec
 */
@Mojo(
        name = "solor-import",
        defaultPhase = LifecyclePhase.PROCESS_RESOURCES
)
public class SolorMojo extends AbstractMojo {

    /**
     * Location of the folder that contains the files to import.
     */
    @Parameter(required = false, defaultValue = "${project.build.directory}/data")
    private String importFolderLocation;

    /**
     * This value, if present, is passed in to
     * {@link ConfigurationService#setDataStoreFolderPath(Path)}
     *
     * @parameter
     * @optional
     */
    @Parameter(required = false)
    private File dataStoreLocation;

    @Parameter(required = true)
    private String importType;

    @Parameter(required = false, defaultValue = "false")
    private boolean transform;

    public SolorMojo() {
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Get.configurationService().setDBBuildMode(BuildMode.DB);


        try {
            Get.configurationService().setIBDFImportPathFolderPath(new File(importFolderLocation).toPath());

            if (this.dataStoreLocation != null) {
                Get.configurationService().setDataStoreFolderPath(dataStoreLocation.toPath());
            }

            getLog().info("  Setup AppContext, data store location = " + Get.configurationService().getDataStoreFolderPath().toFile().getCanonicalPath());
            LookupService.startupIsaac();
            Transaction transaction = Get.commitService().newTransaction(Optional.empty(), ChangeCheckerMode.INACTIVE, false);
            LookupService.getService(IndexBuilderService.class, "semantic index").setEnabled(true);
            DirectImporter rf2Importer = new DirectImporter(transaction, ImportType.valueOf(importType));
            getLog().info("  Importing RF2 files.");
            rf2Importer.run();
            LookupService.syncAll();

            RxNormDirectImporter rxNormDirectImporter = new RxNormDirectImporter(transaction);
            getLog().info("  Importing RxNorm files.");
            rxNormDirectImporter.run();
            LookupService.syncAll();

            getLog().info("Convert LOINC expressions...");
            LoincExpressionToConcept convertLoinc = new LoincExpressionToConcept(transaction);
            Future<?> convertLoincTask = Get.executor().submit(convertLoinc);
            convertLoincTask.get();

            getLog().info("Adding navigation concepts...");
            //TODO change how we get the edit coordinates.
            ManifoldCoordinate coordinate = Get.coordinateFactory().createDefaultStatedManifoldCoordinate();
            EditCoordinate editCoordinate = Get.coordinateFactory().createDefaultUserSolorOverlayEditCoordinate();
            LoincExpressionToNavConcepts addNavigationConcepts = new LoincExpressionToNavConcepts(transaction, coordinate);
            Future<?> addNavigationConceptsTask = Get.executor().submit(addNavigationConcepts);
            addNavigationConceptsTask.get();

            LoincDirectImporter loincImporter = new LoincDirectImporter(transaction);
            getLog().info("  Importing LOINC files.");
            loincImporter.run();
            LookupService.syncAll();
            if (transform) {

                getLog().info("  Transforming RF2 relationships to SOLOR.");
                Rf2RelationshipTransformer transformer = new Rf2RelationshipTransformer(transaction, ImportType.valueOf(importType));
                Future<?> transformTask = Get.executor().submit(transformer);
                transformTask.get();

                getLog().info(" Converting SNOMED OWL expressions...");
                Rf2OwlTransformer rf2OwlTransformer = new Rf2OwlTransformer(ImportType.parseFromString(importType));
                Future<?> rf2OwlTransformTask = Get.executor().submit(rf2OwlTransformer);
                rf2OwlTransformTask.get();

                getLog().info("Classifying new content...");
                Task<ClassifierResults> classifierResultsTask
                        = Get.logicService().getClassifierService(coordinate.toManifoldCoordinateImmutable()).classify();
                ClassifierResults classifierResults = classifierResultsTask.get();
                getLog().info(classifierResults.toString());
            }
            transaction.commit("Solor mojo");
            Get.startIndexTask().get();
            LookupService.syncAll();  //This should be unnecessary....
            LookupService.shutdownIsaac();
        } catch (Throwable throwable) {
            throw new MojoFailureException("solor-import failed", throwable);
        }
    }
}
