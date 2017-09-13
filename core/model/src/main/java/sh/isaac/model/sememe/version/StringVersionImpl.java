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



package sh.isaac.model.sememe.version;

//~--- non-JDK imports --------------------------------------------------------

import sh.isaac.api.Get;
import sh.isaac.api.chronicle.Version;
import sh.isaac.api.component.sememe.SememeChronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.externalizable.ByteArrayDataBuffer;
import sh.isaac.api.component.sememe.version.MutableStringVersion;
import sh.isaac.api.component.sememe.version.StringVersion;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.model.sememe.SememeChronologyImpl;

//~--- classes ----------------------------------------------------------------

/**
 * The Class StringVersionImpl.
 *
 * @author kec
 */
public class StringVersionImpl
        extends SememeVersionImpl
         implements StringVersion, MutableStringVersion {
   /** The string. */
   private String string = null;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new string sememe impl.
    *
    * @param container the container
    * @param stampSequence the stamp sequence
    * @param versionSequence the version sequence
    */
   public StringVersionImpl(SememeChronology container, int stampSequence, short versionSequence) {
      super(container, stampSequence, versionSequence);
   }

   /**
    * Instantiates a new string sememe impl.
    *
    * @param container the container
    * @param stampSequence the stamp sequence
    * @param versionSequence the version sequence
    * @param data the data
    */
   public StringVersionImpl(SememeChronology container,
                           int stampSequence,
                           short versionSequence,
                           ByteArrayDataBuffer data) {
      super(container, stampSequence, versionSequence);
      this.string = data.readUTF();
   }
   
   private StringVersionImpl(StringVersionImpl other, int stampSequence, short versionSequence) {
      super(other.getChronology(), stampSequence, versionSequence);
      this.string = other.getString();
   }

   @Override
   public <V extends Version> V makeAnalog(EditCoordinate ec) {
      final int stampSequence = Get.stampService()
                                   .getStampSequence(
                                       this.getState(),
                                       Long.MAX_VALUE,
                                       ec.getAuthorSequence(),
                                       this.getModuleSequence(),
                                       ec.getPathSequence());
      SememeChronologyImpl chronologyImpl = (SememeChronologyImpl) this.chronicle;
      final StringVersionImpl newVersion = new StringVersionImpl(this, stampSequence, 
              chronologyImpl.nextVersionSequence());

      chronologyImpl.addVersion(newVersion);
      return (V) newVersion;   
   }

   //~--- methods -------------------------------------------------------------

   /**
    * To string.
    *
    * @return the string
    */
   @Override
   public String toString() {
      final StringBuilder sb = new StringBuilder();

      sb.append("{String≤");
      sb.append(this.string);
      toString(sb);
      sb.append("≥S}");
      return sb.toString();
   }

   /**
    * Write version data.
    *
    * @param data the data
    */
   @Override
   protected void writeVersionData(ByteArrayDataBuffer data) {
      super.writeVersionData(data);
      data.putUTF(this.string);
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the sememe type.
    *
    * @return the sememe type
    */
   @Override
   public VersionType getSememeType() {
      return VersionType.STRING;
   }

   /**
    * Gets the string.
    *
    * @return the string
    */
   @Override
   public String getString() {
      return this.string;
   }

   //~--- set methods ---------------------------------------------------------

   /**
    * Sets the string.
    *
    * @param string the new string
    */
   @Override
   public void setString(String string) {
      if (this.string != null) {
         checkUncommitted();
      }

      this.string = string;
   }
   
}
