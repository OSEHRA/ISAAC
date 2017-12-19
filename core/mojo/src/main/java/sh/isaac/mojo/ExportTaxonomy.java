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



/*
* To change this license header, choose License Headers in Project Properties.
* To change this template file, choose Tools | Templates
* and open the template in the editor.
 */
package sh.isaac.mojo;

//~--- JDK imports ------------------------------------------------------------

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.util.Optional;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import sh.isaac.api.Get;
import sh.isaac.api.IsaacTaxonomy;
import sh.isaac.api.LookupService;
import sh.isaac.api.constants.MetadataConceptConstant;
import sh.isaac.api.constants.ModuleProvidedConstants;

//~--- classes ----------------------------------------------------------------

/**
 * The Class ExportTaxonomy.
 *
 * @author kec
 */
@Mojo(name = "export-taxonomy")
public class ExportTaxonomy
        extends AbstractMojo {
   /** The binding package. */
   @Parameter(required = true)
   private String bindingPackage;

   /** The binding class. */
   @Parameter(required = true)
   private String bindingClass;

   /** The build directory. */
   @Parameter(
      required     = true,
      defaultValue = "${project.build.directory}"
   )
   File buildDirectory;

   //~--- methods -------------------------------------------------------------

   /**
    * Execute.
    *
    * @throws MojoExecutionException the mojo execution exception
    * @throws MojoFailureException the mojo failure exception
    */
   @Override
   public void execute()
            throws MojoExecutionException, MojoFailureException {
      try {
         Get.configurationService()
            .setBootstrapMode();
         Get.configurationService()
            .setDBBuildMode();

         final IsaacTaxonomy taxonomy = LookupService.get()
                                                     .getService(IsaacTaxonomy.class);

         // Read in the MetadataConceptConstant constant objects
         // TODO: this step adds the metadata constant to the last concept on the parent stack... 
         // WHich is not always what you want, and subject to change if the IsaacTaxonomy class changes. 
         // Need to modify 
         for (final ModuleProvidedConstants mpc: LookupService.get()
               .getAllServices(ModuleProvidedConstants.class)) {
            getLog().info("Adding metadata constants from " + mpc.getClass().getName());

            int count = 0;

            for (final MetadataConceptConstant mc: mpc.getConstantsToCreate()) {
               taxonomy.createConcept(mc);
               count++;
            }

            getLog().info("Created " + count + " concepts (+ their children)");
         }

         final File          javaDir  = new File(this.buildDirectory, "src/generated");

         javaDir.mkdirs();

         final File metadataDirectory = new File(this.buildDirectory, "generated-resources");

         metadataDirectory.mkdirs();

         final File   metadataXmlDataFile  = new File(metadataDirectory, taxonomy.getClass().getSimpleName() + ".xml");
         final String bindingFileDirectory = this.bindingPackage.concat(".")
                                                                .concat(this.bindingClass)
                                                                .replace('.', '/');

         // Write out the java binding file before we read in the MetadataConceptConstant objects, as these already come from classes
         // and I don't want to have duplicate constants in the system
         final File bindingFile = new File(javaDir, bindingFileDirectory + ".java");

         bindingFile.getParentFile()
                    .mkdirs();
                 
         try (Writer javaWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(bindingFile), "UTF-8"));) {
            taxonomy.exportJavaBinding(javaWriter, this.bindingPackage, this.bindingClass);
         }

         // Now write out the other files, so they have all of the constants.
         try (DataOutputStream xmlData =
               new DataOutputStream(new BufferedOutputStream(new FileOutputStream(metadataXmlDataFile)));
            FileWriter yamlFile = new FileWriter(new File(metadataDirectory.getAbsolutePath(),
                                                          taxonomy.getClass().getSimpleName() + ".yaml"));) {
            taxonomy.exportYamlBinding(yamlFile, this.bindingPackage, this.bindingClass);
         }

         final Path ibdfPath = Paths.get(metadataDirectory.getAbsolutePath(),
                                         taxonomy.getClass()
                                               .getSimpleName() + ".ibdf");
         final Path jsonPath = Paths.get(metadataDirectory.getAbsolutePath(),
                                         taxonomy.getClass()
                                               .getSimpleName() + ".json");

         taxonomy.export(Optional.of(jsonPath), Optional.of(ibdfPath));
      } catch (final Throwable ex) {
         throw new MojoExecutionException(ex.getLocalizedMessage(), ex);
      }
   }
}

