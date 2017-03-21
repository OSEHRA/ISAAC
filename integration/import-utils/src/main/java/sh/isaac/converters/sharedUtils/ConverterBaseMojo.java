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



package sh.isaac.converters.sharedUtils;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;

import java.util.List;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import sh.isaac.api.Get;
import sh.isaac.converters.sharedUtils.stats.ConverterUUID;

//~--- classes ----------------------------------------------------------------

/**
 *
 * {@link ConverterBaseMojo}
 *
 * Base mojo class with shared parameters for reuse by terminology specific converters.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public abstract class ConverterBaseMojo
        extends AbstractMojo {
   /** Location to write the output file. */
   @Parameter(
      required     = true,
      defaultValue = "${project.build.directory}"
   )
   protected File outputDirectory;

   /**
    * Location of the input source file(s).  May be a file or a directory, depending on the specific loader.
    * Usually a directory.
    */
   @Parameter(required = true)
   protected File inputFileLocation;

   /** Output artifactId. */
   @Parameter(
      required     = true,
      defaultValue = "${project.artifactId}"
   )
   protected String converterOutputArtifactId;

   /** Loader version number. */
   @Parameter(
      required     = true,
      defaultValue = "${loader.version}"
   )
   protected String converterVersion;

   /** Converter result version number. */
   @Parameter(
      required     = true,
      defaultValue = "${project.version}"
   )
   protected String converterOutputArtifactVersion;

   /** Converter result classifier. */
   @Parameter(
      required     = false,
      defaultValue = "${resultArtifactClassifier}"
   )
   protected String converterOutputArtifactClassifier;

   /** Converter source artifact version. */
   @Parameter(
      required     = true,
      defaultValue = "${sourceData.version}"
   )
   protected String converterSourceArtifactVersion;

   /** Set '-Dsdp' (skipUUIDDebugPublish) on the command line, to prevent the publishing of the debug UUID map (it will still be created, and written to a file)  At the moment, this param is never used in code - it is just used as a pom trigger (but documented here). */
   @Parameter(
      required     = false,
      defaultValue = "${sdp}"
   )
   private String createDebugUUIDMapSkipPublish;

   /**
    * Set '-DskipUUIDDebug' on the command line, to disable the in memory UUID Debug map entirely (this disables UUID duplicate detection, but
    * significantly cuts the required RAM overhead to run a loader).
    */
   @Parameter(
      required     = false,
      defaultValue = "${skipUUIDDebug}"
   )
   private String createDebugUUIDMap;

   /**
    * An optional list of annotation type names which should be skipped during this transformation.
    */
   @Parameter(required = false)
   protected List<String> annotationSkipList;

   /**
    * An optional list of description type names which should be skipped during this transformation.
    */
   @Parameter(required = false)
   protected List<String> descriptionSkipList;

   /**
    * An optional list of id type names which should be skipped during this transformation.
    */
   @Parameter(required = false)
   protected List<String> idSkipList;

   /**
    * An optional list of refset names which should be skipped during this transformation.
    */
   @Parameter(required = false)
   protected List<String> refsetSkipList;

   /**
    * An optional list of relationship names which should be skipped during this transformation.
    */
   @Parameter(required = false)
   protected List<String> relationshipSkipList;

   /** The import util. */
   protected IBDFCreationUtility importUtil;

   //~--- methods -------------------------------------------------------------

   /**
    * Execute.
    *
    * @throws MojoExecutionException the mojo execution exception
    */
   @Override
   public void execute()
            throws MojoExecutionException {
      Get.configurationService()
         .setBootstrapMode();
      ConverterUUID.disableUUIDMap = (((this.createDebugUUIDMap == null) ||
                                        (this.createDebugUUIDMap.length() == 0)) ? false
            : Boolean.parseBoolean(this.createDebugUUIDMap));

      if (ConverterUUID.disableUUIDMap) {
         ConsoleUtil.println("The UUID Debug map is disabled - this also prevents duplicate ID detection");
      }

      // Set up the output
      if (!this.outputDirectory.exists()) {
         this.outputDirectory.mkdirs();
      }

      checkSkipListSupport();
   }

   /**
    * Supports annotation skip list.
    *
    * @return true, if successful
    */

   // Individual loaders need to override the methods below, if they wish to support the various skiplists
   protected boolean supportsAnnotationSkipList() {
      throw new UnsupportedOperationException("This loader does not support an annotation skip list");
   }

   /**
    * Supports description skip list.
    *
    * @return true, if successful
    */
   protected boolean supportsDescriptionSkipList() {
      throw new UnsupportedOperationException("This loader does not support a description skip list");
   }

   /**
    * Supports id skip list.
    *
    * @return true, if successful
    */
   protected boolean supportsIdSkipList() {
      throw new UnsupportedOperationException("This loader does not support an id skip list");
   }

   /**
    * Supports refset skip list.
    *
    * @return true, if successful
    */
   protected boolean supportsRefsetSkipList() {
      throw new UnsupportedOperationException("This loader does not support a refset skip list");
   }

   /**
    * Supports relationship skip list.
    *
    * @return true, if successful
    */
   protected boolean supportsRelationshipSkipList() {
      throw new UnsupportedOperationException("This loader does not support a relationsihp skip list");
   }

   /**
    * Check skip list support.
    */
   private void checkSkipListSupport() {
      if (notEmpty(this.annotationSkipList)) {
         supportsAnnotationSkipList();
      }

      if (notEmpty(this.idSkipList)) {
         supportsIdSkipList();
      }

      if (notEmpty(this.refsetSkipList)) {
         supportsRefsetSkipList();
      }

      if (notEmpty(this.relationshipSkipList)) {
         supportsRelationshipSkipList();
      }

      if (notEmpty(this.descriptionSkipList)) {
         supportsDescriptionSkipList();
      }
   }

   /**
    * Not empty.
    *
    * @param item the item
    * @return true, if successful
    */
   private boolean notEmpty(List<String> item) {
      if ((item != null) && (item.size() > 0)) {
         return true;
      }

      return false;
   }
}
