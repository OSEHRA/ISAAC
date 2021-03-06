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



package sh.isaac.api.util;

//~--- JDK imports ------------------------------------------------------------

import java.io.File;
import java.io.IOException;

import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

//~--- classes ----------------------------------------------------------------

/**
 * {@link RecursiveDelete}.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class RecursiveDelete {
   /**
    * Delete.
    *
    * @param file the file
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public static void delete(File file)
            throws IOException {
      delete(file.toPath());
   }

   /**
    * Delete.
    *
    * @param path the path
    * @throws IOException Signals that an I/O exception has occurred.
    */
   public static void delete(Path path)
            throws IOException {
      if (path.toFile()
              .isDirectory()) {
         Files.walkFileTree(path,
                            new SimpleFileVisitor<Path>() {
                               @Override
                               public FileVisitResult visitFile(Path file,
                                     BasicFileAttributes attrs)
                                        throws IOException {
                                  Files.delete(file);
                                  return FileVisitResult.CONTINUE;
                               }
                               @Override
                               public FileVisitResult postVisitDirectory(Path dir, IOException exc)
                                        throws IOException {
                                  Files.delete(dir);
                                  return FileVisitResult.CONTINUE;
                               }
                            });
      } else if (path.toFile()
                     .isFile()) {
         Files.delete(path);
      }
   }
}

