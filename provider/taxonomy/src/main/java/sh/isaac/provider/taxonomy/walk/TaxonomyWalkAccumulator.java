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
package sh.isaac.provider.taxonomy.walk;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.component.concept.ConceptChronology;

//~--- classes ----------------------------------------------------------------

/**
 * The Class TaxonomyWalkAccumulator.
 *
 * @author kec
 */
public class TaxonomyWalkAccumulator {
   /** The concepts processed. */
   public int conceptsProcessed = 0;

   /** The connections. */
   public int connections = 0;

   /** The max connections. */
   public int maxConnections = 0;

   /** The min connections. */
   public int minConnections = 0;

   /** The parent connections. */
   public int parentConnections = 0;

   /** The watch concept. */
   ConceptChronology<?> watchConcept = null;

   //~--- methods -------------------------------------------------------------

   /**
    * To string.
    *
    * @return the string
    */
   @Override
   public String toString() {
      return "TaxonomyWalkAccumulator{" + "conceptsProcessed=" + this.conceptsProcessed + ", connections=" +
             this.connections + ", maxConnections=" + this.maxConnections + ", minConnections=" + this.minConnections +
             ", parentConnections=" + this.parentConnections + '}';
   }

   /**
    * Combine.
    *
    * @param u the u
    */
   void combine(TaxonomyWalkAccumulator u) {
      this.conceptsProcessed += u.conceptsProcessed;
      this.connections       += u.connections;
      this.maxConnections    = Math.max(this.maxConnections, u.maxConnections);
      this.minConnections    = Math.max(this.minConnections, u.minConnections);
      this.parentConnections += u.parentConnections;
   }
}
