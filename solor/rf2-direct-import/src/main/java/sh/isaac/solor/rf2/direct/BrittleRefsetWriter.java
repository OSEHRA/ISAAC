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
package sh.isaac.solor.rf2.direct;

import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoField.INSTANT_SECONDS;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import org.apache.logging.log4j.LogManager;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.Get;
import sh.isaac.api.IdentifierService;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.chronicle.VersionType;
import sh.isaac.api.commit.StampService;
import sh.isaac.api.index.IndexService;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.util.UuidT3Generator;
import sh.isaac.model.semantic.SemanticChronologyImpl;
import sh.isaac.model.semantic.version.ComponentNidVersionImpl;
import sh.isaac.model.semantic.version.LongVersionImpl;
import sh.isaac.model.semantic.version.StringVersionImpl;
import sh.isaac.model.semantic.version.brittle.Int1_Int2_Str3_Str4_Str5_Nid6_Nid7_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Int2_Str3_Str4_Nid5_Nid6_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Int2_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Nid2_Int3_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Nid2_Str3_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Nid2_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Nid1_Str2_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Str1_Str2_Nid3_Nid4_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Str1_Str2_Str3_Str4_Str5_Str6_Str7_VersionImpl;
import sh.isaac.model.semantic.version.brittle.Str1_Str2_VersionImpl;

/**
 *
 * @author kec
 */
public class BrittleRefsetWriter extends TimedTaskWithProgressTracker<Void> {
   private static final int                               REFSET_MEMBER_UUID                    = 0;
   private static final int                               EFFECTIVE_TIME_INDEX            = 1;
   private static final int                               ACTIVE_INDEX                    = 2;  // 0 == false, 1 == true
   private static final int                               MODULE_SCTID_INDEX              = 3;
   private static final int                               ASSEMBLAGE_SCT_ID_INDEX         = 4;
   private static final int                               REFERENCED_CONCEPT_SCT_ID_INDEX = 5;
   private static final int                               VARIABLE_FIELD_START = 5;

   
   private final List<String[]> refsetRecords;
   private final Semaphore writeSemaphore;
   private final List<IndexService> indexers;
   private final ImportStreamType importType;
   private final AssemblageService assemblageService = Get.assemblageService();
   private final IdentifierService identifierService = Get.identifierService();
   private final StampService stampService = Get.stampService();

   public BrittleRefsetWriter(List<String[]> semanticRecords, Semaphore writeSemaphore, String message, 
           ImportStreamType importType) {
      this.refsetRecords = semanticRecords;
      this.writeSemaphore = writeSemaphore;
      this.importType = importType;
      this.writeSemaphore.acquireUninterruptibly();
      indexers = LookupService.get().getAllServices(IndexService.class);
      updateTitle("Importing semantic batch of size: " + semanticRecords.size());
      updateMessage(message);
      addToTotalWork(semanticRecords.size());
      Get.activeTasks().add(this);

   }
   protected static final org.apache.logging.log4j.Logger LOG = LogManager.getLogger();
   private void index(Chronology chronicle) {
      for (IndexService indexer: indexers) {
         try {
            indexer.index(chronicle).get();
         } catch (InterruptedException | ExecutionException ex) {
            LOG.error(ex);
         }
      }
   }
   
   int nidFromSctid(String sctid) {
      return identifierService.getNidForUuids(UuidT3Generator.fromSNOMED(sctid));
   }

   @Override
   protected Void call() throws Exception {
      try {

         int authorNid = TermAux.USER.getNid();
         int pathNid = TermAux.DEVELOPMENT_PATH.getNid();

         for (String[] refsetRecord : refsetRecords) {
            
            
            UUID   elementUuid       = UUID.fromString(refsetRecord[REFSET_MEMBER_UUID]);
            int   elementNid         = identifierService.getNidForUuids(elementUuid);
            int   moduleNid          = nidFromSctid(refsetRecord[MODULE_SCTID_INDEX]);
            int   assemblageNid      = nidFromSctid(refsetRecord[ASSEMBLAGE_SCT_ID_INDEX]);
            Status state               = Status.fromZeroOneToken(refsetRecord[ACTIVE_INDEX]);
            int referencedComponentNid = nidFromSctid(refsetRecord[REFERENCED_CONCEPT_SCT_ID_INDEX]);
            TemporalAccessor accessor = DateTimeFormatter.ISO_INSTANT.parse(Rf2DirectImporter.getIsoInstant(refsetRecord[EFFECTIVE_TIME_INDEX]));
            long time = accessor.getLong(INSTANT_SECONDS) * 1000;
            int versionStamp = stampService.getStampSequence(state, time, authorNid, moduleNid, pathNid);
            
            SemanticChronologyImpl refsetMemberToWrite = new SemanticChronologyImpl(
                                                        this.importType.getSemanticVersionType(),
                                                              elementUuid,
                                                              elementNid,
                                                              assemblageNid,
                                                              referencedComponentNid);
            
            switch (importType) {
               case NID1_NID2_INT3_REFSET:
                  addVersionNID1_NID2_INT3_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_INT2_REFSET:
                  addVersionNID1_INT2_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_INT2_STR3_STR4_NID5_NID6_REFSET:
                  addVersionNID1_INT2_STR3_STR4_NID5_NID6_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_REFSET:
                  addVersionNID1_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case STR1_STR2_NID3_NID4_REFSET:
                  addVersionSTR1_STR2_NID3_NID4_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case STR1_STR2_REFSET:
                  addVersionSTR1_STR2_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case STR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET:
                  addVersionSTR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case MEMBER_REFSET:
                  addVersionMEMBER_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case INT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET:
                  addVersionINT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case STR1_REFSET:
                  addVersionSTR1_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_NID2_REFSET:
                  addVersionNID1_NID2_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_NID2_STR3_REFSET:
                  addVersionNID1_NID2_STR3_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case NID1_STR2_REFSET:
                  addVersionNID1_STR2_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

               case INT1_REFSET:
                  addVersionINT1_REFSET(refsetMemberToWrite, versionStamp, refsetRecord);
                  break;

                  default:
                     throw new UnsupportedOperationException("Can't handle: " + importType);
               
            }

            index(refsetMemberToWrite);
            assemblageService.writeSemanticChronology(refsetMemberToWrite);
            completedUnitOfWork();
         }

         return null;
      } finally {
         this.writeSemaphore.release();
         for (IndexService indexer : indexers) {
            indexer.commitWriter();
         }
         this.done();
         Get.activeTasks().remove(this);
      }
   }

   private void addVersionNID1_NID2_INT3_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Nid2_Int3_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setNid2(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 2]));
      brittleVersion.setInt3(Integer.parseInt(refsetRecord[VARIABLE_FIELD_START + 3]));
   }
   private void addVersionNID1_INT2_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Int2_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setInt2(Integer.parseInt(refsetRecord[VARIABLE_FIELD_START + 2]));
   }

   private void addVersionNID1_INT2_STR3_STR4_NID5_NID6_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Int2_Str3_Str4_Nid5_Nid6_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setInt2(Integer.parseInt(refsetRecord[VARIABLE_FIELD_START + 2]));
      brittleVersion.setStr3(refsetRecord[VARIABLE_FIELD_START + 3]);
      brittleVersion.setStr4(refsetRecord[VARIABLE_FIELD_START + 4]);
      brittleVersion.setNid5(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 5]));
      brittleVersion.setNid6(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 6]));
   }

   private void addVersionNID1_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      ComponentNidVersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setComponentNid(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
   }

   private void addVersionSTR1_STR2_NID3_NID4_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Str1_Str2_Nid3_Nid4_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setStr1(refsetRecord[VARIABLE_FIELD_START + 1]);
      brittleVersion.setStr2(refsetRecord[VARIABLE_FIELD_START + 2]);
      brittleVersion.setNid3(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 3]));
      brittleVersion.setNid4(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 4]));
   }

   private void addVersionSTR1_STR2_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Str1_Str2_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setStr1(refsetRecord[VARIABLE_FIELD_START + 1]);
      brittleVersion.setStr2(refsetRecord[VARIABLE_FIELD_START + 2]);
   }

   private void addVersionSTR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Str1_Str2_Str3_Str4_Str5_Str6_Str7_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setStr1(refsetRecord[VARIABLE_FIELD_START + 1]);
      brittleVersion.setStr2(refsetRecord[VARIABLE_FIELD_START + 2]);
      brittleVersion.setStr3(refsetRecord[VARIABLE_FIELD_START + 3]);
      brittleVersion.setStr4(refsetRecord[VARIABLE_FIELD_START + 4]);
      brittleVersion.setStr5(refsetRecord[VARIABLE_FIELD_START + 5]);
      brittleVersion.setStr6(refsetRecord[VARIABLE_FIELD_START + 6]);
      brittleVersion.setStr7(refsetRecord[VARIABLE_FIELD_START + 7]);
   }

   private void addVersionMEMBER_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      refsetMemberToWrite.createMutableVersion(versionStamp);
   }

   private void addVersionINT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Int1_Int2_Str3_Str4_Str5_Nid6_Nid7_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setInt1(Integer.parseInt(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setInt2(Integer.parseInt(refsetRecord[VARIABLE_FIELD_START + 2]));
      brittleVersion.setStr3(refsetRecord[VARIABLE_FIELD_START + 3]);
      brittleVersion.setStr4(refsetRecord[VARIABLE_FIELD_START + 4]);
      brittleVersion.setStr5(refsetRecord[VARIABLE_FIELD_START + 5]);
      brittleVersion.setNid6(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 6]));
      brittleVersion.setNid7(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 7]));
   }

   private void addVersionSTR1_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      StringVersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setString(refsetRecord[VARIABLE_FIELD_START + 1]);
   }

   private void addVersionNID1_NID2_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Nid2_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setNid2(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 2]));
   }

   private void addVersionNID1_NID2_STR3_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Nid2_Str3_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setNid2(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 2]));
      brittleVersion.setStr3(refsetRecord[VARIABLE_FIELD_START + 3]);
   }

   private void addVersionNID1_STR2_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      Nid1_Str2_VersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setNid1(nidFromSctid(refsetRecord[VARIABLE_FIELD_START + 1]));
      brittleVersion.setStr2(refsetRecord[VARIABLE_FIELD_START + 2]);
   }

   private void addVersionINT1_REFSET(SemanticChronologyImpl refsetMemberToWrite, int versionStamp, String[] refsetRecord) {
      LongVersionImpl brittleVersion = refsetMemberToWrite.createMutableVersion(versionStamp);
      brittleVersion.setLongValue(Long.parseLong(refsetRecord[VARIABLE_FIELD_START + 1]));
   }

   
}
