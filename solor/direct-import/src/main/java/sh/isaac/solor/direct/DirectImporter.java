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
package sh.isaac.solor.direct;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import sh.isaac.api.AssemblageService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.Status;
import sh.isaac.api.bootstrap.TermAux;
import sh.isaac.api.chronicle.Chronology;
import sh.isaac.api.component.semantic.SemanticChronology;
import sh.isaac.api.component.semantic.version.dynamic.DynamicColumnInfo;
import sh.isaac.api.component.semantic.version.dynamic.DynamicDataType;
import sh.isaac.api.component.semantic.version.dynamic.DynamicUtility;
import sh.isaac.api.coordinate.WriteCoordinate;
import sh.isaac.api.coordinate.WriteCoordinateImpl;
import sh.isaac.api.index.IndexBuilderService;
import sh.isaac.api.progress.PersistTaskResult;
import sh.isaac.api.task.TimedTaskWithProgressTracker;
import sh.isaac.api.transaction.Transaction;
import sh.isaac.api.util.UuidT3Generator;
import sh.isaac.model.semantic.DynamicUsageDescriptionImpl;
import sh.isaac.solor.ContentProvider;
import sh.isaac.solor.ContentStreamProvider;
import sh.isaac.solor.direct.clinvar.ClinvarImporter;
import sh.isaac.solor.direct.cvx.CVXImporter;
import sh.isaac.solor.direct.livd.LIVDImporter;
import sh.isaac.solor.direct.srf.SRFImporter;

/**
 * Loader code to convert RF2 format fileCount into the ISAAC format.
 */
public class DirectImporter
        extends TimedTaskWithProgressTracker<Void>
        implements PersistTaskResult {

    // TODO consider replacing readSemaphore with TaskCountManager
    private static final int WRITE_PERMITS = Runtime.getRuntime()
            .availableProcessors() * 2;

    public static HashSet<String> watchTokens = new HashSet<>();

    public static Boolean importDynamic = false;

    /**
     * The date format parser.
     */
    protected static final SimpleDateFormat DATE_PARSER = new SimpleDateFormat("yyyyMMdd");

    protected final Semaphore writeSemaphore = new Semaphore(WRITE_PERMITS);

    protected final ImportType importType;

    protected final List<ContentProvider> entriesToImport;
    protected File importDirectory;
    private HashMap<String, ArrayList<MutablePair<DynamicColumnInfo, Integer>>> refsetColumnInfo = null;  //refset SCTID to column information from the refset spec
    private final Transaction transaction;
    public DirectImporter(Transaction transaction, ImportType importType) {
        this.transaction = transaction;
        this.importType = importType;
        this.entriesToImport = null;
        this.importDirectory = Get.configurationService().getIBDFImportPath().toFile();
//        watchTokens.add("782587007");
//        watchTokens.add("89587004"); // Removal of foreign body from abdominal cavity (procedure)
//        watchTokens.add("84971000000100"); // PBCL flag true (attribute)
//        watchTokens.add("123101000000107"); // PBCL flag true: report, request, level, test (qualifier value)

        updateTitle("Importing from from" + importDirectory.getAbsolutePath());
        Get.activeTasks()
                .add(this);
    }

    public DirectImporter(Transaction transaction, ImportType importType, List<ContentProvider> entriesToImport) {
        this.transaction = transaction;
        this.importType = importType;
        this.entriesToImport = preProcessEntries(entriesToImport);
//        File importDirectory = Get.configurationService().getIBDFImportPath().toFile();
//        watchTokens.add("89587004"); // Removal of foreign body from abdominal cavity (procedure)
//        watchTokens.add("84971000000100"); // PBCL flag true (attribute)
//        watchTokens.add("123101000000107"); // PBCL flag true: report, request, level, test (qualifier value)

        updateTitle("Importing from from provided entries");
        Get.activeTasks()
                .add(this);
    }

    public DirectImporter(Transaction transaction, ImportType importType, File importDirectory) {
        this.transaction = transaction;
        this.importType = importType;
        this.entriesToImport = null;
        this.importDirectory = importDirectory;

        updateTitle("Importing from " + importDirectory.getAbsolutePath());
        Get.activeTasks()
                .add(this);
    }

    //~--- methods -------------------------------------------------------------
    /**
     * Execute.
     *
     * @throws java.lang.Exception
     */
    @Override
    public Void call()
            throws Exception {
        try {
            final long time = System.currentTimeMillis();
            boolean solorReleaseFormat = false;

            if (this.entriesToImport != null) {
                ArrayList<ImportSpecification> specificationsToImport = new ArrayList<>();

                for (ContentProvider entry : this.entriesToImport) {
                    processEntry(entry, specificationsToImport, solorReleaseFormat);
                }
                doRf2Import(specificationsToImport, time);
            } else {
                File importDirectory = this.importDirectory == null ? Get.configurationService().getIBDFImportPath().toFile() : this.importDirectory;

                LOG.info("Importing from: " + importDirectory.getAbsolutePath());

                int fileCount = loadDatabase(importDirectory, time, solorReleaseFormat);

                if (fileCount == 0) {
                    LOG.info("Import from: " + importDirectory.getAbsolutePath() + " failed.");
                }
            }

            return null;
        } finally {
            Get.taxonomyService().notifyTaxonomyListenersToRefresh();
            Get.activeTasks()
                    .remove(this);
        }
    }

    @Override
    protected void running() {
        super.running();
    }

    /**
     * If the item they passed us is a zip file, we need to look inside the zip
     * file.
     *
     * @param entriesToImport
     * @return TODO maybe merge "loadDatabase" into this? I need code that
     * handles paths, not files, since I may be passing in a nested zip already.
     */
    private List<ContentProvider> preProcessEntries(List<ContentProvider> entriesToImport) {
        try {
            ArrayList<ContentProvider> results = new ArrayList<>();
            for (ContentProvider cp : entriesToImport) {
                if (cp.getStreamSourceName().toLowerCase().endsWith(".zip")) {
                    StringBuilder importPrefixRegex = new StringBuilder();
                    importPrefixRegex.append("([a-z/0-9_]*)?(rf2release/)?"); //ignore parent directories
                    switch (importType) {
                        case FULL:
                            importPrefixRegex.append("(full/)"); //prefixes to match
                            break;
                        case SNAPSHOT:
                        case SNAPSHOT_ACTIVE_ONLY:
                            importPrefixRegex.append("(snapshot/)"); //prefixes to match
                            break;
                        default:
                            throw new RuntimeException("Unsupported import type");
                    }
                    importPrefixRegex.append("[a-z/0-9_\\.\\-]*"); //allow all match child directories

                    try (ZipInputStream zis = new ZipInputStream(cp.get().get(), Charset.forName("UTF-8"))) {
                        ZipEntry nestedEntry = zis.getNextEntry();
                        while (nestedEntry != null) {
                            if (!nestedEntry.getName().toUpperCase().contains("__MACOSX") && !nestedEntry.getName().contains("._")
                                    && nestedEntry.getName().toLowerCase().matches(importPrefixRegex.toString())) {

                                byte[] temp = IOUtils.toByteArray(zis);

                                if (temp.length < (500 * 1024 * 1024)) {  //if more than 500 MB, pass on a ref to unzip the stream again.  Otherwise, cache 
                                    //We have to cache these unzipped bytes, as otherwise, 
                                    //the import is terribly slow, because the java zip API only provides stream access
                                    //to nested files, and when you try to unzip from a stream, it can't jump ahead whe you 
                                    //call next entry, so you end up re-extracting the entire file for each file, which more 
                                    //that triples the load times.
                                    results.add(new ContentProvider(cp.getStreamSourceName() + ":" + nestedEntry.getName(), () -> temp));
                                    LOG.debug("Caching unzipped content - " + results.get(results.size() - 1).getStreamSourceName());
                                } else {
                                    //Code to reopen the zip and find the same zip entry from the previous provider (which is quite expensive)
                                    final String thisName = nestedEntry.getName();
                                    results.add(new ContentProvider(cp.getStreamSourceName() + ":" + nestedEntry.getName(), () -> {
                                        try {
                                            try (ZipInputStream zisInternal = new ZipInputStream(cp.get().get(), Charset.forName("UTF-8"))) {
                                                ZipEntry nestedEntryInternal = zisInternal.getNextEntry();
                                                while (nestedEntryInternal != null) {
                                                    if (nestedEntryInternal.getName().equals(thisName)) {
                                                        return IOUtils.toByteArray(zisInternal);
                                                    }
                                                    nestedEntryInternal = zisInternal.getNextEntry();
                                                }
                                                throw new RuntimeException("Couldn't refind expected entry??");
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }));
                                    LOG.debug(results.get(results.size() - 1).getStreamSourceName() + " too large for cache, will to unzip again");
                                }
                            }
                            nestedEntry = zis.getNextEntry();
                        }
                    }
                } else {
                    results.add(cp);
                }
            }
            return results;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Load database.
     *
     * @param contentDirectory the zip file
     * @throws Exception the exception
     */
    private int loadDatabase(File contentDirectory, long time, boolean solorReleaseFormat)
            throws Exception {
        List<Path> zipFiles = Files.walk(contentDirectory.toPath())
                .filter(
                        p -> p.toString().toLowerCase().endsWith(".zip")
                        && (p.toString().toUpperCase().contains("SNOMEDCT")
                        || p.toString().toLowerCase().contains("sct")))
                .collect(Collectors.toList());
        ArrayList<ImportSpecification> specificationsToImport = new ArrayList<>();
        StringBuilder importPrefixRegex = new StringBuilder();
        importPrefixRegex.append("([a-z/0-9_\\-]*)?(rf2release/)?"); //ignore parent directories
        switch (importType) {
            case FULL:
                importPrefixRegex.append("(full/)"); //prefixes to match
                break;
            case SNAPSHOT:
            case SNAPSHOT_ACTIVE_ONLY:
                importPrefixRegex.append("(snapshot/)"); //prefixes to match
                break;
            default:
                throw new RuntimeException("Unsupported import type");
        }
        importPrefixRegex.append("[a-z/0-9_\\.\\-]*"); //allow all match child directories
        for (Path zipFilePath : zipFiles) {
            try (ZipFile zipFile = new ZipFile(zipFilePath.toFile(), Charset.forName("UTF-8"))) {
                Enumeration<? extends ZipEntry> entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    if (!entry.isDirectory()) {
                        if (!entry.getName().contains("__MACOSX") && !entry.getName().toLowerCase().contains("resources")) {
                            String entryName = entry.getName().toLowerCase();
                            if (entryName.matches(importPrefixRegex.toString())) {
                                processEntry(new ContentProvider(zipFilePath.toFile(), entry), specificationsToImport, solorReleaseFormat);
                            }
                        }
                    }
                }
            }
        }

        return doRf2Import(specificationsToImport, time);
    }

    protected int doRf2Import(ArrayList<ImportSpecification> specificationsToImport, final long time) throws ExecutionException, IOException, UnsupportedOperationException, InterruptedException {
        int fileCount = 0;
        Collections.sort(specificationsToImport);
        StringBuilder builder = new StringBuilder();
        builder.append("Importing the following zip entries: \n");
        for (ImportSpecification spec : specificationsToImport) {
            builder.append("     ").append(spec.streamType);
            builder.append(": ").append(spec.contentProvider.getStreamSourceName()).append("\n");
        }

        HashMap<String, UUID> createdColumnConcepts = new HashMap<>();

        LOG.info(builder.toString());

        addToTotalWork(specificationsToImport.size());

        for (ImportSpecification importSpecification : specificationsToImport) {
            String message = "Importing " + trimZipName(importSpecification.contentProvider.getStreamSourceName());
            updateMessage(message);
            LOG.info("\n\n" + message + "\n");
            if (message.toLowerCase().contains("loinc.csv")) {
                LOG.info("About to import loinc...");
            }

            try (ContentStreamProvider csp = importSpecification.contentProvider.get()) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(csp.get(), Charset.forName("UTF-8")))) {
                    fileCount++;

                    switch (importSpecification.streamType) {
                        case ALTERNATIVE_IDENTIFIER:
                            readAlternativeIdentifiers(br, importSpecification);
                            break;

                        case CONCEPT:
                            readConcepts(br, importSpecification);
                            break;

                        case DESCRIPTION:
                            readDescriptions(br, importSpecification);
                            break;

                        case DIALECT:
                            readDialect(br, importSpecification);
                            break;

                        case INFERRED_RELATIONSHIP:
                            readInferredRelationships(br, importSpecification);
                            break;

                        case STATED_RELATIONSHIP:
                            readStatedRelationships(br, importSpecification);
                            break;

                        //TODO Dan notes, none of these refset importer patterns is properly annotating the created refset assemblage concept
                        //with the metadata that should be placed on the refset definition concept.  That said, I'm not going to fix it, because
                        //all of this code should simply be thrown away, as the 'Dynamic' import mode already handles this properly.
                        // set the variable 'importDynamic' to true, and all of your problems with this missing metadata go away :)
                        case INT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET:
                            readINT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET(br, importSpecification);
                            break;
                        case INT1_REFSET:
                            readINT1_REFSET(br, importSpecification);
                            break;
                        case MEMBER_REFSET:
                            readMEMBER_REFSET(br, importSpecification);
                            break;
                        case NID1_INT2_REFSET:
                            readNID1_INT2_REFSET(br, importSpecification);
                            break;
                        case NID1_INT2_STR3_STR4_NID5_NID6_REFSET:
                            readNID1_INT2_STR3_STR4_NID5_NID6_REFSET(br, importSpecification);
                            break;
                        case NID1_NID2_INT3_REFSET:
                            readNID1_NID2_INT3_REFSET(br, importSpecification);
                            break;
                        case NID1_NID2_REFSET:
                            readNID1_NID2_REFSET(br, importSpecification);
                            break;
                        case NID1_NID2_STR3_REFSET:
                            readNID1_NID2_STR3_REFSET(br, importSpecification);
                            break;
                        case NID1_REFSET:
                            readNID1_REFSET(br, importSpecification);
                            break;
                        case NID1_STR2_REFSET:
                            readNID1_STR2_REFSET(br, importSpecification);
                            break;
                        case STR1_REFSET:
                            readSTR1_REFSET(br, importSpecification);
                            break;
                        case STR1_STR2_NID3_NID4_REFSET:
                            readSTR1_STR2_NID3_NID4_REFSET(br, importSpecification);
                            break;
                        case STR1_STR2_REFSET:
                            readSTR1_STR2_REFSET(br, importSpecification);
                            break;
                        case STR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET:
                            readSTR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET(br, importSpecification);
                            break;
                        case STR1_NID2_NID3_NID4_REFSET:
                            readSTR1_NID2_NID3_NID4_REFSET(br, importSpecification);
                            break;
                        case STR1_STR2_NID3_NID4_NID5_REFSET:
                            readSTR1_STR2_NID3_NID4_NID5_REFSET(br, importSpecification);
                            break;
                        case DYNAMIC:
                            read_DYNAMIC_REFSET(br, importSpecification, createdColumnConcepts);
                            break;

                        case RXNORM_CONSO:
                            readRXNORM_CONSO(br, importSpecification);
                            break;

                        case LOINC:
                            throw new IllegalStateException("LOINC file is not an RF2 file...");

                        case CLINVAR:
                            // throw new IllegalStateException("CLINVAR file is not an RF2 file...");
                            ClinvarImporter clinvarImporter = new ClinvarImporter(this.writeSemaphore, WRITE_PERMITS);
                            clinvarImporter.runImport(br);
                            break;

                        case CVX:
                            // throw new IllegalStateException("CVX file is not an RF2 file...");
                            CVXImporter cvxImporter = new CVXImporter(this.writeSemaphore, WRITE_PERMITS);
                            cvxImporter.runImport(csp.get());

                            break;
                        case LIVD:
                            // throw new IllegalStateException("LIVD file is not an RF2 file...");
                            LIVDImporter livdImporter = new LIVDImporter(this.transaction, this.writeSemaphore, WRITE_PERMITS);
                            livdImporter.runImport(csp.get());
                            break;

                        case SRF_CONCEPT:
                        case SRF_DESCRIPTION:
                        case SRF_STATED_RELATIONSHIP:
                        case SRF_INFERRED_RELATIONSHIP:
                        case SRF_INT1_ASSEMBLAGE:
                        case SRF_NID1_ASSEMBLAGE:
                        case SRF_STR1_ASSEMBLAGE:
                        case SRF_MEMBER_ASSEMBLAGE:
                        case SRF_NID1_INT2_ASSEMBLAGE:
                        case SRF_NID1_NID2_ASSEMBLAGE:
                        case SRF_NID1_STR2_ASSEMBLAGE:
                        case SRF_STR1_STR2_ASSEMBLAGE:
                        case SRF_NID1_NID2_INT3_ASSEMBLAGE:
                        case SRF_NID1_NID2_STR3_ASSEMBLAGE:
                        case SRF_STR1_NID2_NID3_NID4_ASSEMBLAGE:
                        case SRF_STR1_STR2_NID3_NID4_ASSEMBLAGE:
                        case SRF_STR1_STR2_NID3_NID4_NID5_ASSEMBLAGE:
                        case SRF_NID1_INT2_STR3_STR4_NID5_NID6_ASSEMBLAGE:
                        case SRF_INT1_INT2_STR3_STR4_STR5_NID6_NID7_ASSEMBLAGE:
                        case SRF_STR1_STR2_STR3_STR4_STR5_STR6_STR7_ASSEMBLAGE:

                            SRFImporter.RunImport(br, importSpecification, this.writeSemaphore, WRITE_PERMITS, importType);
                            break;

                        default:
                            throw new UnsupportedOperationException("Can't handle: " + importSpecification.streamType);
                    }
                }
            } catch (Exception e) {
                LOG.error("Unexpected error", e);
                throw new RuntimeException("Bad:", e);
            }
            completedUnitOfWork();
        }
        LookupService.syncAll();
        LOG.info("Loaded " + fileCount + " files in " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
        return fileCount;
    }

    protected void processEntry(ContentProvider contentProvider, ArrayList<ImportSpecification> entriesToImport1,
                                boolean solorReleaseFormat) {
        String entryName = contentProvider.getStreamSourceName().toLowerCase();

        if (entryName.contains("sct2_concept_")) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.CONCEPT, solorReleaseFormat));
        } else if ((entryName.contains("sct2_description_") || entryName.contains("sct2_textdefinition_"))) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.DESCRIPTION, solorReleaseFormat));
        } else if (entryName.contains("der2_crefset_") && entryName.contains("language")) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.DIALECT, solorReleaseFormat));
        } else if (entryName.contains("sct2_identifier_")) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.ALTERNATIVE_IDENTIFIER, solorReleaseFormat));
        } else if (entryName.contains("sct2_relationship_")) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.INFERRED_RELATIONSHIP, solorReleaseFormat));
        } else if (entryName.contains("sct2_statedrelationship_")) {
            entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.STATED_RELATIONSHIP, solorReleaseFormat));
        } else if (entryName.contains("refset_")) {
            if (importDynamic) {
                entriesToImport1.add(new ImportSpecification(contentProvider, ImportStreamType.DYNAMIC, entryName, solorReleaseFormat));
            } else {
                if (entryName.contains("_ccirefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_NID2_INT3_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_cirefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_INT2_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_cissccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_INT2_STR3_STR4_NID5_NID6_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_crefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_ssccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_STR2_NID3_NID4_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_ssrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_STR2_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_sssssssrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_refset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.MEMBER_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_iisssccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.INT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_srefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_ccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_NID2_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_ccsrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_NID2_STR3_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_csrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.NID1_STR2_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_irefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.INT1_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_scccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_NID2_NID3_NID4_REFSET, solorReleaseFormat));
                } else if (entryName.contains("_sscccrefset")) {
                    entriesToImport1.add(new ImportSpecification(
                            contentProvider,
                            ImportStreamType.STR1_STR2_NID3_NID4_NID5_REFSET, solorReleaseFormat));
                } else {
                    LOG.info("Ignoring: " + contentProvider.getStreamSourceName());
                }
            }
        } else if (entryName.toUpperCase().endsWith("RXNCONSO.RRF")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.RXNORM_CONSO, solorReleaseFormat));
        } else if (entryName.toUpperCase().endsWith("LOINC.CSV")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.LOINC, solorReleaseFormat));
        } else if (entryName.toLowerCase().endsWith(".xlsx") && entryName.toLowerCase().contains("cvx")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.CVX, solorReleaseFormat));
        } else if (entryName.toLowerCase().endsWith(".xlsx") && entryName.toLowerCase().contains("livd")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.LIVD, solorReleaseFormat));
        }else if (entryName.toLowerCase().contains("variant")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.CLINVAR, solorReleaseFormat));
        } else if (entryName.contains("solor_concept")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_CONCEPT, solorReleaseFormat));
        }else if (entryName.contains("solor_description")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_DESCRIPTION, solorReleaseFormat));
        }else if (entryName.contains("solor_relationship")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_INFERRED_RELATIONSHIP, solorReleaseFormat));
        }else if (entryName.contains("solor_statedrelationship")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STATED_RELATIONSHIP, solorReleaseFormat));
        }else if (entryName.contains("assemblage_cci")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_NID2_INT3_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_ci")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_INT2_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_cisscc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_INT2_STR3_STR4_NID5_NID6_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_c")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_sscc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_STR2_NID3_NID4_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_ss")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_STR2_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_sssssss")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_STR2_STR3_STR4_STR5_STR6_STR7_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_ ")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_MEMBER_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_iissscc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_INT1_INT2_STR3_STR4_STR5_NID6_NID7_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_s")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_cc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_NID2_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_ccs")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_NID2_STR3_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_cs")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_NID1_STR2_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_i")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_INT1_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_sccc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_NID2_NID3_NID4_ASSEMBLAGE, solorReleaseFormat));
        } else if (entryName.contains("assemblage_ssccc")) {
            entriesToImport1.add(new ImportSpecification(
                    contentProvider,
                    ImportStreamType.SRF_STR1_STR2_NID3_NID4_NID5_ASSEMBLAGE, solorReleaseFormat));
        } else {
            LOG.info("Ignoring: " + contentProvider.getStreamSourceName());
        }
    }

    private void readRXNORM_CONSO(BufferedReader br,
            ImportSpecification importSpecification) throws IOException {
        updateMessage("Importing RxNorm data...");
        long commitTime = System.currentTimeMillis();
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        // RRF has no header row br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                RxNormWriter rxNormWriter = new RxNormWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing RxNorm records from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()), commitTime);

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(rxNormWriter);
            }
        }
        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            RxNormWriter rxNormWriter = new RxNormWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing RxNorm records from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()), commitTime);

            Get.executor()
                    .submit(rxNormWriter);
        }

        updateMessage("Waiting for RxNorm file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);

        updateMessage("Synchronizing indexes...");
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readINT1_INT2_STR3_STR4_STR5_NID6_NID7_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing iissscc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing iissscc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);

    }

    protected String[] checkWatchTokensAndSplit(String rowString, ImportSpecification importSpecification) {
        String[] columns;
        if (importSpecification.streamType == ImportStreamType.RXNORM_CONSO) {
            columns = rowString.split("\\|");
        } else {

            columns = rowString.split("\t");
            if (!watchTokens.isEmpty()) {
                int watchCount = 0;
                for (String column : columns) {
                    if (watchTokens.contains(column)) {
                        watchCount++;
                        if (watchCount > 0 && watchCount <= 3) {
                            LOG.info("Found watch tokens in: "
                                    + importSpecification.contentProvider.getStreamSourceName()
                                    + " \n" + rowString);
                        }
                    }

                }
            }
        }
        return columns;
    }

    private void readINT1_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing i semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing i semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readMEMBER_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_INT2_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing ci semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing ci semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_INT2_STR3_STR4_NID5_NID6_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing cisscc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing cisscc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_NID2_INT3_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing cci semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing cci semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_NID2_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing iissscc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing iissscc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_NID2_STR3_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing ccs semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing ccs semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing c semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing c semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readNID1_STR2_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing cs semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing cs semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing s semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing s semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_STR2_NID3_NID4_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);
            if (columns[4].equals("705112009") && (columns[7].equals("712561002") || columns[7].equals("704318007"))) {
                String[] newColumns = new String[columns.length];
                newColumns[0] = columns[0]; // id, a uuid
                newColumns[1] = "20180131"; // effective time
                newColumns[2] = columns[2]; // active
                newColumns[3] = TermAux.SOLOR_OVERLAY_MODULE.getPrimordialUuid().toString(); // moduleId
                newColumns[4] = columns[4]; // refsetId
                newColumns[5] = columns[5]; // referenced component id
                newColumns[6] = columns[6]; // mapTarget
                newColumns[7] = columns[7].replaceAll("704318007", "370130000").replaceAll("712561002", "739029001"); // attributeId
                newColumns[8] = columns[8]; // correlationId
                newColumns[9] = columns[9]; // contentOriginId
                columnsToWrite.add(newColumns);
            }

            if (columns[4].equals("705112009") && ConceptWriter.CONCEPT_REPLACEMENT_MAP_20180731.containsKey(columns[7])) {
                String[] newColumns = new String[columns.length];
                newColumns[0] = columns[0]; // id, a uuid
                newColumns[1] = "20180731"; // effective time
                newColumns[2] = columns[2]; // active
                newColumns[3] = TermAux.SOLOR_OVERLAY_MODULE.getPrimordialUuid().toString(); // moduleId
                newColumns[4] = columns[4]; // refsetId
                newColumns[5] = columns[5]; // referenced component id
                newColumns[6] = columns[6]; // mapTarget
                newColumns[7] = columns[7];
                for (Entry<String, String> entry : ConceptWriter.CONCEPT_REPLACEMENT_MAP_20180731.entrySet()) {
                    newColumns[7] = newColumns[7].replaceAll(entry.getKey(), entry.getValue());
                }
                newColumns[8] = columns[8]; // correlationId
                newColumns[9] = columns[9]; // contentOriginId
                columnsToWrite.add(newColumns);
            }

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing sscc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing sscc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_STR2_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing ss semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing ss semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_STR2_STR3_STR4_STR5_STR6_STR7_REFSET(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing sssssss semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing sssssss semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_NID2_NID3_NID4_REFSET(BufferedReader br,
            ImportSpecification importSpecification) throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing sccc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing sccc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readSTR1_STR2_NID3_NID4_NID5_REFSET(BufferedReader br,
            ImportSpecification importSpecification) throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);
            if (columns[4].equals("705110001") && (columns[7].contains("712561002") || columns[7].contains("704318007"))) {
                String[] newColumns = new String[columns.length];
                newColumns[0] = columns[0]; // id, a uuid
                newColumns[1] = "20180131"; // effective time
                newColumns[2] = columns[2]; // active
                newColumns[3] = TermAux.SOLOR_OVERLAY_MODULE.getPrimordialUuid().toString(); // moduleId
                newColumns[4] = columns[4]; // refsetId
                newColumns[5] = columns[5]; // referenced component id
                newColumns[6] = columns[6]; // mapTarget
                newColumns[7] = columns[7].replaceAll("704318007", "370130000").replaceAll("712561002", "739029001"); // expression
                newColumns[8] = columns[8]; // definitionStatusId
                newColumns[9] = columns[9]; // correlationId
                newColumns[10] = columns[10]; // contentOriginId
                columnsToWrite.add(newColumns);
            }

            if (columns[4].equals("705112009") && ConceptWriter.CONCEPT_REPLACEMENT_MAP_20180731.containsKey(columns[7])) {
                String[] newColumns = new String[columns.length];
                newColumns[0] = columns[0]; // id, a uuid
                newColumns[1] = "20180731"; // effective time
                newColumns[2] = columns[2]; // active
                newColumns[3] = TermAux.SOLOR_OVERLAY_MODULE.getPrimordialUuid().toString(); // moduleId
                newColumns[4] = columns[4]; // refsetId
                newColumns[5] = columns[5]; // referenced component id
                newColumns[6] = columns[6]; // mapTarget
                newColumns[7] = columns[7];
                for (Entry<String, String> entry : ConceptWriter.CONCEPT_REPLACEMENT_MAP_20180731.entrySet()) {
                    newColumns[7] = newColumns[7].replaceAll(entry.getKey(), entry.getValue());
                }
                newColumns[8] = columns[8]; // correlationId
                newColumns[9] = columns[9]; // contentOriginId
                columnsToWrite.add(newColumns);
            }

            if (columnsToWrite.size() == writeSize) {
                BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing ssccc semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            BrittleRefsetWriter writer = new BrittleRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing ssccc semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);
            Get.executor()
                    .submit(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void read_DYNAMIC_REFSET(BufferedReader br,
            ImportSpecification importSpecification, HashMap<String, UUID> createdColumnConcepts)
            throws IOException {
        AssemblageService assemblageService = Get.assemblageService();
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        br.readLine();  //skip header row
        //String[] headerRow = checkWatchTokensAndSplit(br.readLine(), importSpecification);

        if (refsetColumnInfo == null) {
            /*
             * If things have been sorted properly, the first time this method is called, it will be with the
             * "Reference set descriptor reference set (foundation metadata concept)" (900000000000456007)
             * refset. We must process this file first, to know how to process the rest of the refsets.
             * 
             * the refset descriptor file shouldn't be too huge, so set a mark on the stream, allowing us to back
             * it up and read it again, when we actually process it into the DB below.
             * 
             * (we can't process it on the fly below, because we need to read it first, to know how to process itself, as it is
             * self describing...)
             */

            LOG.info("Reading refset descriptors");
            /*
             * columns we care about are 6, 7 and 8: attributeDescription attributeType attributeOrder
             * attributeDescription is an SCTID column, which provides the concept to use as the column header concept
             * attributeType is an sctid columns, which provides the datatype of the column
             * 
             * attributeOrder is an integer column, which is 0 indexed starting at the referencedComponent columns (example)
             * 
             * id  effectiveTime  active  moduleId  refsetId  referencedComponentId  attributeDescription  attributeType  attributeOrder
             *                                                0                      1                     2              3
             * 00  01             02      03        04        05                     06                    07             08
             * 
             * The DynamicRefsetWriter already has hard-coded logic to handle columns 00 thru 05, as these are present in every refset.
             * So, we only care about 06 on, which is the {@link DynamicRefsetWriter#VARIABLE_FIELD_START} constant, which will match up
             * with the '1' in the attribute order column....
             */
            if (!(importSpecification.contentProvider.getStreamSourceName().toLowerCase().contains("refset/metadata/der2_ccirefset_") && 
                     importSpecification.contentProvider.getStreamSourceName().toLowerCase().contains("refsetdescriptor"))) {
                //This isn't fatal, if its an extension that doesn't define any new types of refsets
                LOG.info("der2_ccirefset_refsetdescriptor is missing or not sorted to the top of the refsets!");
                refsetColumnInfo = new HashMap<>();
            }
            else {
                /*
                 * Per the RF2 spec:
                 * Creation of Reference set descriptor data is mandatory when creating a new reference set in the International
                 * Release or in a National Extension .
                 * 
                 * TODO need to handle ancestor refset spec lookups....
                 * 
                 * Creation of a Reference set descriptor is optional when creating a reference set in another Extension. If a descriptor
                 * is not created, the descriptor of the closest ancestor of the reference set is used when validating reference set
                 * member records.
                 */
                br.mark(100000);  //this should be big enough, if not, we should fail on reset
                //Configure a hashmap of refsetId -> ArrayList<Pair<DynamicColumnInfo, Integer>>
                refsetColumnInfo = new HashMap<>();
                String rowString;
                while ((rowString = br.readLine()) != null) {
                    String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);
                    String refsetId = columns[5].trim();  //we actually want the referencedComponentId, not the refsetId, because this is the refset that is being described.
                    int adjustedColumnNumber = Integer.parseInt(columns[8]) - 1;
                    UUID columnHeaderConcept = UuidT3Generator.fromSNOMED(columns[6]);
                    Status refsetState = Status.fromZeroOneToken(columns[2]);
                    int effectiveTime = Integer.parseInt(columns[1]);
                    
                    if (importType == ImportType.SNAPSHOT_ACTIVE_ONLY && refsetState != Status.ACTIVE) {
                        continue;
                    }
    
                    ArrayList<MutablePair<DynamicColumnInfo, Integer>> refsetColumns = refsetColumnInfo.get(refsetId);
                    if (refsetColumns == null) {
                        refsetColumns = new ArrayList<>();
                        refsetColumnInfo.put(refsetId, refsetColumns);
                    }
    
                    if (adjustedColumnNumber < 0) {
                        continue;  //We don't need this one, as it should always be referencedComponentId when processing the refset descriptor file
                    }
    
                    //TODO I can't figure out if/where the RF2 spec specifies whether columns can be optional or required.... default to optional for now.
                    DynamicColumnInfo newDCI = new DynamicColumnInfo(adjustedColumnNumber, columnHeaderConcept,
                            DynamicDataType.translateSCTIDMetadata(columns[7]), null, false);
                    boolean found = false;
                    for (MutablePair<DynamicColumnInfo, Integer> existing : refsetColumns) {
                        if (existing.getKey().getColumnOrder() == newDCI.getColumnOrder()) {
                            //redefining a column... just keep the newer definition.
                            if (effectiveTime > existing.getValue()) {
                                LOG.info("Column was redefined from {} to {} in refset {}", existing.getKey(), newDCI, refsetId);
                                existing.setLeft(newDCI);
                            }
                            else {
                                LOG.info("Column was redefined from {} to {} in refset {}", newDCI, existing.getKey(), refsetId);
                            }
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        refsetColumns.add(new MutablePair<DynamicColumnInfo, Integer>(newDCI, effectiveTime));
                    }
                }
                //At this point, we should have a hash, of how every single refset should be configured.  
                //sort the column info and sanity check....
                for (Entry<String, ArrayList<MutablePair<DynamicColumnInfo, Integer>>> dci : refsetColumnInfo.entrySet()) {
                    ArrayList<DynamicColumnInfo> temp = new ArrayList<>(dci.getValue().size());
                    for (MutablePair<DynamicColumnInfo, Integer> pair : dci.getValue()) {
                        temp.add(pair.getKey());
                    }
                    Collections.sort(temp);
                    for (int i = 0; i < temp.size(); i++) {
                        if (temp.get(i).getColumnOrder() != i) {
                            throw new RuntimeException("Misconfiguration for refset " + dci.getKey() + " no info for column " + i);
                        }
                    }
                }
                br.reset();  //back the stream up, and actually process the refset now.
            }

            //Use the metadata we just read, and properly annotate the concepts as dynamic semantics in our system.
            for (Entry<String, ArrayList<MutablePair<DynamicColumnInfo, Integer>>> refsetDescriptors : refsetColumnInfo.entrySet())
            {
                //refset descriptors are SCTID to colInfo
                // TODO would like to add a second parent to this concept into the metadata tree, but I don't think it knows how to
                // merge logic graphs well yet....
                //TODO this should be done with an edit coordinate the same as the refset concept, I suppose... I don't know how to find 
                //the coords of the refset concept that was specified during _this_ import, however...

                //See if we already know the SCTID / refset config due to a dependency preload (we should)
                int nid;
                try {
                    nid = Get.identifierService().getNidForUuids(UuidT3Generator.fromSNOMED(refsetDescriptors.getKey()));
                }
                catch (NoSuchElementException e1) {
                    //we have no knowledge of the concept this refset is defining... log error, continue.
                    LOG.error("Cannot determine nid for refset descriptor {}, probably an inactive concept that wasn't loaded?  Skipping", refsetDescriptors.getKey());
                    continue;
                }
                int[] assemblageStamps = Get.concept(nid).getVersionStampSequences();
                Arrays.sort(assemblageStamps);
                int stampSequence = assemblageStamps[assemblageStamps.length - 1];  //use the largest (newest) stamp on the concept, 
                //since we probably just loaded the concept....
                
                WriteCoordinate wc = new WriteCoordinateImpl(this.transaction, stampSequence);

                //TODO we need special handling for mapset conversion into our native mapset type
                SemanticChronology[] items = LookupService.getService(DynamicUtility.class).configureConceptAsDynamicSemantic(
                    wc,
                    nid,
                    "DynamicDefinition for refset " + Get.conceptDescriptionText(nid),
                    refsetDescriptors.getValue().stream().map(MutablePair::getLeft).toArray(DynamicColumnInfo[]::new), null, null, false);

                for (Chronology c : items) {
                    assemblageService.writeSemanticChronology((SemanticChronology)c);
                    for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
                        indexer.indexNow(c);
                    }
                }
                //Reindex all descriptions on this concept, in case it it outside the metadata tree, and wouldn't otherwise be flagged as a potential
                //metadata concept (which it is, now that it defines a semantic)
                for (SemanticChronology sc : Get.assemblageService().getDescriptionsForComponent(nid)) {
                    for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
                        indexer.indexNow(sc);
                    }
                }
                LOG.info("Refset Config for sctid {}: {}", refsetDescriptors.getKey(), DynamicUsageDescriptionImpl.read(nid).toString());
            }
            try
            {
                //make sure it is readable for future calls
                LookupService.syncAll();
            }
            catch (Exception e)
            {
                throw new RuntimeException("unexpected", e);
            }
        }

        //Process the refset file itself...
        int dataCount = 0;
        String rowString;
        ArrayList<DynamicRefsetWriter> writers = new ArrayList<>();
        while ((rowString = br.readLine()) != null) {
            dataCount++;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);
            if (dataCount == 1) {
                //Another sanity check - the header-row length beyond column 5 should match the column definitions...
                ArrayList<MutablePair<DynamicColumnInfo, Integer>> dci = refsetColumnInfo.get(columns[DynamicRefsetWriter.ASSEMBLAGE_SCT_ID_INDEX]);
                if (dci != null && dci.size() != columns.length - DynamicRefsetWriter.VARIABLE_FIELD_START) {
                    throw new RuntimeException("Header information in " + importSpecification.contentProvider.getStreamSourceName()
                            + " does not match specification from the der2_ccirefset_refsetdescriptor file ");
                }
                //dci being null isn't always fatal, if the refset is an extension that adds to an existing refset, for example.
            }
            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                DynamicRefsetWriter writer = new DynamicRefsetWriter(columnsToWrite, this.writeSemaphore,
                        "Processing dynamic semantics from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()), importSpecification, importType, transaction);
                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(writer);
                writers.add(writer);
            }
        }
        if (dataCount == 0) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            DynamicRefsetWriter writer = new DynamicRefsetWriter(columnsToWrite, this.writeSemaphore,
                    "Processing dynamic semantics from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()), importSpecification, importType, transaction);
            Get.executor()
                    .submit(writer);
            writers.add(writer);
        }

        updateMessage("Waiting for refset file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        int skipped = 0;
        for (DynamicRefsetWriter writer : writers) {
            try {
                skipped += writer.get();
            } catch (Exception e) {
                LOG.error("Unexpected failure", e);
            }
        }
        LOG.info("Read {} rows of data, and skipped {}", dataCount, skipped);
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readAlternativeIdentifiers(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        String rowString;

        br.readLine();  // discard header row
        LOG.warn("Alternative identifiers not yet supported.");
        while ((rowString = br.readLine()) != null) {
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);
        }
    }

    private void readConcepts(BufferedReader br, ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        String rowString;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        
        br.readLine();  // discard header row
        int lineCount = 1;
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            lineCount++;
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                ConceptWriter conceptWriter = new ConceptWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing concepts from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()), importType,
                        importSpecification.isSolorReleaseFormat());

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(conceptWriter);
            }
        }
        LOG.info("Concept linecount: " + lineCount + " in: " + importSpecification.contentProvider.getStreamSourceName());
        if (empty) {
            LOG.info("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            ConceptWriter conceptWriter = new ConceptWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing concepts from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()), importType,
                    importSpecification.isSolorReleaseFormat());

            Get.executor()
                    .submit(conceptWriter);
        }

        updateMessage("Waiting for concept file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readDescriptions(BufferedReader br, ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                DescriptionWriter descriptionWriter = new DescriptionWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing descriptions from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()), importType);

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(descriptionWriter);
            }
        }
        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            DescriptionWriter descriptionWriter = new DescriptionWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing descriptions from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()), importType);

            Get.executor()
                    .submit(descriptionWriter);
        }

        updateMessage("Waiting for description file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);

        updateMessage("Synchronizing indexes...");
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readDialect(BufferedReader br, ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                DialectWriter dialectWriter = new DialectWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing dialect from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()), importType);

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(dialectWriter);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            DialectWriter dialectWriter = new DialectWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing dialect from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()), importType);

            Get.executor()
                    .submit(dialectWriter);
        }

        updateMessage("Waiting for dialect file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        updateMessage("Synchronizing indexes...");
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readInferredRelationships(BufferedReader br,
            ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                Rf2RelationshipWriter relWriter = new Rf2RelationshipWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing inferred rels from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(relWriter);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            Rf2RelationshipWriter relWriter = new Rf2RelationshipWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing inferred rels from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);

            Get.executor()
                    .submit(relWriter);
        }

        updateMessage("Waiting for inferred relationship file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        updateMessage("Synchronizing indexes...");
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    private void readStatedRelationships(BufferedReader br, ImportSpecification importSpecification)
            throws IOException {
        final int writeSize = 102400;
        ArrayList<String[]> columnsToWrite = new ArrayList<>(writeSize);
        String rowString;

        br.readLine();  // discard header row
        boolean empty = true;
        while ((rowString = br.readLine()) != null) {
            empty = false;
            String[] columns = checkWatchTokensAndSplit(rowString, importSpecification);

            columnsToWrite.add(columns);

            if (columnsToWrite.size() == writeSize) {
                Rf2RelationshipWriter relWriter = new Rf2RelationshipWriter(
                        columnsToWrite,
                        this.writeSemaphore,
                        "Processing stated rels from: " + trimZipName(
                                importSpecification.contentProvider.getStreamSourceName()),
                        importSpecification, importType);

                columnsToWrite = new ArrayList<>(writeSize);
                Get.executor()
                        .submit(relWriter);
            }
        }

        if (empty) {
            LOG.warn("No data in file: " + importSpecification.contentProvider.getStreamSourceName());
        }
        if (!columnsToWrite.isEmpty()) {
            Rf2RelationshipWriter relWriter = new Rf2RelationshipWriter(
                    columnsToWrite,
                    this.writeSemaphore,
                    "Finishing stated rels from: " + trimZipName(
                            importSpecification.contentProvider.getStreamSourceName()),
                    importSpecification, importType);

            Get.executor()
                    .submit(relWriter);
        }

        updateMessage("Waiting for stated relationship file completion...");
        this.writeSemaphore.acquireUninterruptibly(WRITE_PERMITS);
        updateMessage("Synchronizing indexes...");
        for (IndexBuilderService indexer : LookupService.get().getAllServices(IndexBuilderService.class)) {
            try {
                indexer.sync().get();
            } catch (Exception e) {
                LOG.error("problem calling sync on index", e);
            }
        }
        this.writeSemaphore.release(WRITE_PERMITS);
    }

    public static String trimZipName(String zipName) {
        int index = zipName.lastIndexOf("/");

        return zipName.substring(index + 1);
    }

    //~--- get methods ---------------------------------------------------------
    public static String getIsoInstant(String basicIsoDate) {
        if (basicIsoDate.contains("-")) {
            return basicIsoDate;
        }
        // From basicIsoDate: '20111203'
        StringBuilder isoInstantBuilder = new StringBuilder();

        // To IsoInstant: '2011-12-03T00:00:00Z'
        isoInstantBuilder.append(basicIsoDate.substring(0, 4));
        isoInstantBuilder.append("-");
        isoInstantBuilder.append(basicIsoDate.substring(4, 6));
        isoInstantBuilder.append("-");
        isoInstantBuilder.append(basicIsoDate.substring(6, 8));
        isoInstantBuilder.append("T00:00:00Z");
        return isoInstantBuilder.toString();
    }

}
