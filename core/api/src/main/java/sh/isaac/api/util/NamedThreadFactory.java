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

import java.util.concurrent.ThreadFactory;

//~--- classes ----------------------------------------------------------------

/**
 * A factory for creating NamedThread objects.
 *
 * @author <a href="mailto:daniel.armbrust.list@gmail.com">Dan Armbrust</a>
 */
public class NamedThreadFactory
         implements ThreadFactory {
   /** The thread group. */
   private ThreadGroup threadGroup = null;

   /** The thread name prefix. */
   private String threadNamePrefix = null;

   /** The thread priority. */
   private final int threadPriority;

   /** The daemon. */
   private final boolean daemon;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new named thread factory.
    *
    * @param daemon the daemon
    */
   public NamedThreadFactory(boolean daemon) {
      this(null, null, Thread.NORM_PRIORITY, daemon);
   }

   /**
    * Instantiates a new named thread factory.
    *
    * @param threadNamePrefix optional
    * @param daemon the daemon
    */
   public NamedThreadFactory(String threadNamePrefix, boolean daemon) {
      this(null, threadNamePrefix, Thread.NORM_PRIORITY, daemon);
   }

   /**
    * Instantiates a new named thread factory.
    *
    * @param threadGroup optional
    * @param threadNamePrefix optional
    */
   public NamedThreadFactory(ThreadGroup threadGroup, String threadNamePrefix) {
      this(threadGroup, threadNamePrefix, Thread.NORM_PRIORITY, true);
   }

   /**
    * Instantiates a new named thread factory.
    *
    * @param threadGroup optional
    * @param threadNamePrefix optional
    * @param threadPriority the thread priority
    * @param daemon the daemon
    */
   public NamedThreadFactory(ThreadGroup threadGroup, String threadNamePrefix, int threadPriority, boolean daemon) {
      super();
      this.threadGroup      = threadGroup;
      this.threadNamePrefix = threadNamePrefix;
      this.threadPriority   = threadPriority;
      this.daemon           = daemon;

      if ((threadGroup != null) && (threadGroup.getMaxPriority() < threadPriority)) {
         threadGroup.setMaxPriority(threadPriority);
      }
   }

   //~--- methods -------------------------------------------------------------

   /**
    * New thread.
    *
    * @param r the r
    * @return the thread
    */
   @Override
   public Thread newThread(Runnable r) {
      final Thread t = (this.threadGroup == null) ? new Thread(r)
            : new Thread(this.threadGroup, r);

      t.setName(((this.threadNamePrefix == null) ? ""
            : this.threadNamePrefix + " ") + t.getId());
      t.setPriority(this.threadPriority);
      t.setDaemon(this.daemon);
      return t;
   }
}

