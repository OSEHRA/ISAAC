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
package sh.isaac.model.observable.version;

//~--- non-JDK imports --------------------------------------------------------
import java.util.List;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyProperty;
import javafx.beans.property.StringProperty;
import sh.isaac.api.Get;
import sh.isaac.api.chronicle.Version;

import sh.isaac.api.observable.sememe.ObservableSememeChronology;
import sh.isaac.model.observable.CommitAwareIntegerProperty;
import sh.isaac.model.observable.CommitAwareStringProperty;
import sh.isaac.model.observable.ObservableFields;
import sh.isaac.model.sememe.version.DescriptionVersionImpl;
import sh.isaac.api.component.sememe.version.DescriptionVersion;
import sh.isaac.api.coordinate.EditCoordinate;
import sh.isaac.api.observable.sememe.version.ObservableDescriptionVersion;
import sh.isaac.model.observable.ObservableChronologyImpl;

//~--- classes ----------------------------------------------------------------
/**
 * The Class ObservableDescriptionVersionImpl.
 *
 * @author kec
 */
public class ObservableDescriptionVersionImpl
        extends ObservableSememeVersionImpl
        implements ObservableDescriptionVersion {

   /**
    * The case significance concept sequence property.
    */
   IntegerProperty caseSignificanceConceptSequenceProperty;

   /**
    * The language concept sequence property.
    */
   IntegerProperty languageConceptSequenceProperty;

   /**
    * The text property.
    */
   StringProperty textProperty;

   /**
    * The description type concept sequence property.
    */
   IntegerProperty descriptionTypeConceptSequenceProperty;

   //~--- constructors --------------------------------------------------------
   /**
    * Instantiates a new observable description impl.
    *
    * @param stampedVersion the stamped version
    * @param chronology the chronology
    */
   public ObservableDescriptionVersionImpl(DescriptionVersion stampedVersion,
           ObservableSememeChronology chronology) {
      super(stampedVersion,
              chronology);
   }

   @Override
   public <V extends Version> V makeAnalog(EditCoordinate ec) {
      DescriptionVersion newVersion = this.stampedVersion.makeAnalog(ec);
      ObservableDescriptionVersionImpl newObservableVersion
              = new ObservableDescriptionVersionImpl(newVersion, (ObservableSememeChronology) chronology);
      ((ObservableChronologyImpl) chronology).getObservableVersionList().add(newObservableVersion);
      return (V) newObservableVersion;
   }

   //~--- methods -------------------------------------------------------------
   /**
    * Case significance concept sequence property.
    *
    * @return the integer property
    */
   @Override
   public IntegerProperty caseSignificanceConceptSequenceProperty() {
      if (this.caseSignificanceConceptSequenceProperty == null) {
         this.caseSignificanceConceptSequenceProperty = new CommitAwareIntegerProperty(this,
                 ObservableFields.CASE_SIGNIFICANCE_CONCEPT_SEQUENCE_FOR_DESCRIPTION.toExternalString(),
                 getCaseSignificanceConceptSequence());
         this.caseSignificanceConceptSequenceProperty.addListener((observable, oldValue, newValue) -> {
            ((DescriptionVersionImpl) this.stampedVersion).setCaseSignificanceConceptSequence(newValue.intValue());
         });
      }

      return this.caseSignificanceConceptSequenceProperty;
   }

   /**
    * Description type concept sequence property.
    *
    * @return the integer property
    */
   @Override
   public IntegerProperty descriptionTypeConceptSequenceProperty() {
      if (this.descriptionTypeConceptSequenceProperty == null) {
         this.descriptionTypeConceptSequenceProperty = new CommitAwareIntegerProperty(this,
                 ObservableFields.DESCRIPTION_TYPE_FOR_DESCRIPTION.toExternalString(),
                 getDescriptionTypeConceptSequence());
         this.descriptionTypeConceptSequenceProperty.addListener((observable, oldValue, newValue) -> {
            ((DescriptionVersionImpl) this.stampedVersion).setDescriptionTypeConceptSequence(newValue.intValue());
         });
      }

      return this.descriptionTypeConceptSequenceProperty;
   }

   /**
    * Language concept sequence property.
    *
    * @return the integer property
    */
   @Override
   public IntegerProperty languageConceptSequenceProperty() {
      if (this.languageConceptSequenceProperty == null) {
         this.languageConceptSequenceProperty = new CommitAwareIntegerProperty(this,
                 ObservableFields.LANGUAGE_CONCEPT_SEQUENCE_FOR_DESCRIPTION.toExternalString(),
                 getLanguageConceptSequence());
         this.languageConceptSequenceProperty.addListener((observable, oldValue, newValue) -> {
            ((DescriptionVersionImpl) this.stampedVersion).setLanguageConceptSequence(newValue.intValue());
         });
      }

      return this.languageConceptSequenceProperty;
   }

   /**
    * Text property.
    *
    * @return the string property
    */
   @Override
   public StringProperty textProperty() {
      if (this.textProperty == null) {
         this.textProperty = new CommitAwareStringProperty(this,
                 ObservableFields.TEXT_FOR_DESCRIPTION.toExternalString(),
                 getText());
         this.textProperty.addListener((observable, oldValue, newValue) -> {
            ((DescriptionVersionImpl) this.stampedVersion).setText(newValue);
         });
      }

      return this.textProperty;
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the case significance concept sequence.
    *
    * @return the case significance concept sequence
    */
   @Override
   public int getCaseSignificanceConceptSequence() {
      if (this.caseSignificanceConceptSequenceProperty != null) {
         return this.caseSignificanceConceptSequenceProperty.get();
      }

      return ((DescriptionVersionImpl) this.stampedVersion).getCaseSignificanceConceptSequence();
   }

   //~--- set methods ---------------------------------------------------------
   /**
    * Sets the case significance concept sequence.
    *
    * @param caseSignificanceConceptSequence the new case significance concept sequence
    */
   @Override
   public void setCaseSignificanceConceptSequence(int caseSignificanceConceptSequence) {
      if (this.caseSignificanceConceptSequenceProperty != null) {
         this.caseSignificanceConceptSequenceProperty.set(caseSignificanceConceptSequence);
      }
      ((DescriptionVersionImpl) this.stampedVersion).setCaseSignificanceConceptSequence(
                 caseSignificanceConceptSequence);
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the description type concept sequence.
    *
    * @return the description type concept sequence
    */
   @Override
   public int getDescriptionTypeConceptSequence() {
      if (this.descriptionTypeConceptSequenceProperty != null) {
         return this.descriptionTypeConceptSequenceProperty.get();
      }

      return ((DescriptionVersionImpl) this.stampedVersion).getDescriptionTypeConceptSequence();
   }

   //~--- set methods ---------------------------------------------------------
   /**
    * Sets the description type concept sequence.
    *
    * @param descriptionTypeConceptSequence the new description type concept sequence
    */
   @Override
   public void setDescriptionTypeConceptSequence(int descriptionTypeConceptSequence) {
      if (this.descriptionTypeConceptSequenceProperty != null) {
         this.descriptionTypeConceptSequenceProperty.set(descriptionTypeConceptSequence);
      }
      ((DescriptionVersionImpl) this.stampedVersion).setDescriptionTypeConceptSequence(descriptionTypeConceptSequence);
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the language concept sequence.
    *
    * @return the language concept sequence
    */
   @Override
   public int getLanguageConceptSequence() {
      if (this.languageConceptSequenceProperty != null) {
         return this.languageConceptSequenceProperty.get();
      }

      return ((DescriptionVersionImpl) this.stampedVersion).getLanguageConceptSequence();
   }

   //~--- set methods ---------------------------------------------------------
   /**
    * Sets the language concept sequence.
    *
    * @param languageConceptSequence the new language concept sequence
    */
   @Override
   public void setLanguageConceptSequence(int languageConceptSequence) {
      if (this.languageConceptSequenceProperty != null) {
         this.languageConceptSequenceProperty.set(languageConceptSequence);
      }
      ((DescriptionVersionImpl) this.stampedVersion).setLanguageConceptSequence(languageConceptSequence);
   }

   //~--- get methods ---------------------------------------------------------
   /**
    * Gets the text.
    *
    * @return the text
    */
   @Override
   public String getText() {
      if (this.textProperty != null) {
         return this.textProperty.get();
      }

      return ((DescriptionVersionImpl) this.stampedVersion).getText();
   }

   //~--- set methods ---------------------------------------------------------
   /**
    * Sets the text.
    *
    * @param text the new text
    */
   @Override
   public void setText(String text) {
      if (this.textProperty != null) {
         this.textProperty.set(text);
      }
      ((DescriptionVersionImpl) this.stampedVersion).setText(text);
   }

   @Override
   public String toString() {
      return "ObservableDescriptionImpl{text:" + getText() + ", case: " + Get.conceptDescriptionText(getCaseSignificanceConceptSequence())
              + ", language:" + Get.conceptDescriptionText(getLanguageConceptSequence())
              + ", type: " + Get.conceptDescriptionText(getDescriptionTypeConceptSequence())
              + " " + Get.stampService().describeStampSequence(getStampSequence())
              + '}';
   }

   @Override
   public List<ReadOnlyProperty<?>> getProperties() {
      List<ReadOnlyProperty<?>> properties = super.getProperties();
      properties.add(textProperty());
      properties.add(languageConceptSequenceProperty());
      properties.add(descriptionTypeConceptSequenceProperty());
      properties.add(caseSignificanceConceptSequenceProperty());
      return properties;
   }

}