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



package sh.isaac.provider.query.lucene;

//~--- JDK imports ------------------------------------------------------------

/**
 * Copyright Notice
 *
 * This is a work of the U.S. Government and is not subject to copyright
 * protection in the United States. Foreign copyrights may apply.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import java.nio.file.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

//~--- non-JDK imports --------------------------------------------------------

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.IntField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.LogByteSizeMergePolicy;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TrackingIndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ControlledRealTimeReopenThread;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ReferenceManager;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;

import sh.isaac.api.ConfigurationService;
import sh.isaac.api.Get;
import sh.isaac.api.LookupService;
import sh.isaac.api.SystemStatusService;
import sh.isaac.api.chronicle.ObjectChronology;
import sh.isaac.api.commit.ChronologyChangeListener;
import sh.isaac.api.commit.CommitRecord;
import sh.isaac.api.component.concept.ConceptChronology;
import sh.isaac.api.component.sememe.SememeChronology;
import sh.isaac.api.component.sememe.version.SememeVersion;
import sh.isaac.api.identity.StampedVersion;
import sh.isaac.api.index.ComponentSearchResult;
import sh.isaac.api.index.ConceptSearchResult;
import sh.isaac.api.index.IndexServiceBI;
import sh.isaac.api.index.IndexedGenerationCallable;
import sh.isaac.api.index.SearchResult;
import sh.isaac.api.util.NamedThreadFactory;
import sh.isaac.api.util.UuidT5Generator;
import sh.isaac.api.util.WorkExecutors;
import sh.isaac.provider.query.lucene.indexers.DescriptionIndexer;
import sh.isaac.provider.query.lucene.indexers.SememeIndexer;
import sh.isaac.provider.query.lucene.indexers.TopDocsFilteredCollector;
import sh.isaac.utility.Frills;

//~--- classes ----------------------------------------------------------------

//See example for help with the Controlled Real-time indexing...

/**
 * The Class LuceneIndexer.
 */
//http://stackoverflow.com/questions/17993960/lucene-4-4-0-new-controlledrealtimereopenthread-sample-usage?answertab=votes#tab-top
public abstract class LuceneIndexer
         implements IndexServiceBI {
   /** The Constant DEFAULT_LUCENE_FOLDER. */
   public static final String DEFAULT_LUCENE_FOLDER = "lucene";

   /** The Constant log. */
   private static final Logger log = LogManager.getLogger();

   /** The Constant luceneVersion. */
   public static final Version luceneVersion = Version.LUCENE_4_10_3;

   /** The Constant unindexedFuture. */
   private static final UnindexedFuture unindexedFuture = new UnindexedFuture();

   // don't need to analyze this - and even though it is an integer, we index it as a string, as that is faster when we are only doing

   /** The Constant FIELD_SEMEME_ASSEMBLAGE_SEQUENCE. */
   // exact matches.
   protected static final String FIELD_SEMEME_ASSEMBLAGE_SEQUENCE = "_sememe_type_sequence_" +
                                                                    PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER;

   /** The Constant FIELD_COMPONENT_NID. */

   // this isn't indexed
   public static final String FIELD_COMPONENT_NID = "_component_nid_";

   /** The Constant FIELD_TYPE_INT_STORED_NOT_INDEXED. */
   protected static final FieldType FIELD_TYPE_INT_STORED_NOT_INDEXED;

   //~--- static initializers -------------------------------------------------

   static {
      FIELD_TYPE_INT_STORED_NOT_INDEXED = new FieldType();
      FIELD_TYPE_INT_STORED_NOT_INDEXED.setNumericType(FieldType.NumericType.INT);
      FIELD_TYPE_INT_STORED_NOT_INDEXED.setIndexed(false);
      FIELD_TYPE_INT_STORED_NOT_INDEXED.setStored(true);
      FIELD_TYPE_INT_STORED_NOT_INDEXED.setTokenized(false);
      FIELD_TYPE_INT_STORED_NOT_INDEXED.freeze();
   }

   //~--- fields --------------------------------------------------------------

   /** The index folder. */
   private File indexFolder = null;

   /** The indexed component statistics. */
   private final HashMap<String, AtomicInteger> indexedComponentStatistics = new HashMap<>();

   /** The indexed component statistics block. */
   private final Semaphore indexedComponentStatisticsBlock = new Semaphore(1);

   /** The component nid latch. */
   private final ConcurrentHashMap<Integer, IndexedGenerationCallable> componentNidLatch = new ConcurrentHashMap<>();

   /** The enabled. */
   private boolean enabled = true;

   /** The db build mode. */
   private Boolean dbBuildMode = null;

   /** The database validity. */
   private DatabaseValidity databaseValidity = DatabaseValidity.NOT_SET;

   /** The change listener ref. */
   private ChronologyChangeListener changeListenerRef;

   /** The lucene writer service. */
   protected final ExecutorService luceneWriterService;

   /** The lucene writer future checker service. */
   protected ExecutorService luceneWriterFutureCheckerService;

   /** The reopen thread. */
   private final ControlledRealTimeReopenThread<IndexSearcher> reopenThread;

   /** The tracking index writer. */
   private final TrackingIndexWriter trackingIndexWriter;

   /** The searcher manager. */
   private final ReferenceManager<IndexSearcher> searcherManager;

   /** The index name. */
   private final String indexName;

   //~--- constructors --------------------------------------------------------

   /**
    * Instantiates a new lucene indexer.
    *
    * @param indexName the index name
    * @throws IOException Signals that an I/O exception has occurred.
    */
   protected LuceneIndexer(String indexName)
            throws IOException {
      try {
         this.indexName          = indexName;
         this.luceneWriterService = LookupService.getService(WorkExecutors.class)
               .getIOExecutor();
         this.luceneWriterFutureCheckerService = Executors.newFixedThreadPool(1,
               new NamedThreadFactory(indexName + " Lucene future checker", false));

         final Path searchFolder     = LookupService.getService(ConfigurationService.class)
                                                    .getSearchFolderPath();
         final File luceneRootFolder = new File(searchFolder.toFile(), DEFAULT_LUCENE_FOLDER);

         luceneRootFolder.mkdirs();
         this.indexFolder = new File(luceneRootFolder, indexName);

         if (!this.indexFolder.exists()) {
            this.databaseValidity = DatabaseValidity.MISSING_DIRECTORY;
            log.info("Index folder missing: " + this.indexFolder.getAbsolutePath());
         } else if (this.indexFolder.list().length > 0) {
            this.databaseValidity = DatabaseValidity.POPULATED_DIRECTORY;
         }

         this.indexFolder.mkdirs();
         log.info("Index: " + this.indexFolder.getAbsolutePath());

         final Directory indexDirectory =
            new MMapDirectory(this.indexFolder);  // switch over to MMapDirectory - in theory - this gives us back some

         // room on the JDK stack, letting the OS directly manage the caching of the index files - and more importantly, gives us a huge
         // performance boost during any operation that tries to do multi-threaded reads of the index (like the SOLOR rules processing) because
         // the default value of SimpleFSDirectory is a huge bottleneck.
         indexDirectory.clearLock("write.lock");

         final IndexWriterConfig config = new IndexWriterConfig(luceneVersion, new PerFieldAnalyzer());

         config.setRAMBufferSizeMB(256);

         final MergePolicy mergePolicy = new LogByteSizeMergePolicy();

         config.setMergePolicy(mergePolicy);
         config.setSimilarity(new ShortTextSimilarity());

         final IndexWriter indexWriter = new IndexWriter(indexDirectory, config);

         this.trackingIndexWriter = new TrackingIndexWriter(indexWriter);

         final boolean applyAllDeletes = false;

         this.searcherManager = new SearcherManager(indexWriter, applyAllDeletes, null);

         // [3]: Create the ControlledRealTimeReopenThread that reopens the index periodically taking into
         // account the changes made to the index and tracked by the TrackingIndexWriter instance
         // The index is refreshed every 60sc when nobody is waiting
         // and every 100 millis whenever is someone waiting (see search method)
         // (see http://lucene.apache.org/core/4_3_0/core/org/apache/lucene/search/NRTManagerReopenThread.html)
         this.reopenThread = new ControlledRealTimeReopenThread<>(this.trackingIndexWriter,
               this.searcherManager,
               60.00,
               0.1);
         this.startThread();

         // Register for commits:
         log.info("Registering indexer " + getIndexerName() + " for commits");
         this.changeListenerRef = new ChronologyChangeListener() {
            @Override
            public void handleCommit(CommitRecord commitRecord) {
               if (LuceneIndexer.this.dbBuildMode == null) {
                  LuceneIndexer.this.dbBuildMode = Get.configurationService()
                        .inDBBuildMode();
               }

               if (LuceneIndexer.this.dbBuildMode) {
                  log.debug("Ignore commit due to db build mode");
                  return;
               }

               final int size = commitRecord.getSememesInCommit()
                                            .size();

               if (size < 100) {
                  log.info("submitting sememes " + commitRecord.getSememesInCommit().toString() + " to indexer " +
                           getIndexerName() + " due to commit");
               } else {
                  log.info("submitting " + size + " sememes to indexer " + getIndexerName() + " due to commit");
               }

               commitRecord.getSememesInCommit().stream().forEach(sememeId -> {
                                       final SememeChronology<?> sc = Get.sememeService()
                                                                         .getSememe(sememeId);

                                       index(sc);
                                    });
            }
            @Override
            public void handleChange(SememeChronology<? extends SememeVersion<?>> sc) {
               // noop
            }
            @Override
            public void handleChange(ConceptChronology<? extends StampedVersion> cc) {
               // noop
            }
            @Override
            public UUID getListenerUuid() {
               return UuidT5Generator.get(getIndexerName());
            }
         };
         Get.commitService()
            .addChangeListener(this.changeListenerRef);
      } catch (final Exception e) {
         LookupService.getService(SystemStatusService.class)
                      .notifyServiceConfigurationFailure(indexName, e);
         throw e;
      }
   }

   //~--- methods -------------------------------------------------------------

   /**
    * Clear database validity value.
    */
   @Override
   public void clearDatabaseValidityValue() {
      // Reset to enforce analysis
      this.databaseValidity = DatabaseValidity.NOT_SET;
   }

   /**
    * Clear index.
    */
   @Override
   public final void clearIndex() {
      try {
         this.trackingIndexWriter.deleteAll();
      } catch (final IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   /**
    * Clear indexed statistics.
    */
   @Override
   public void clearIndexedStatistics() {
      this.indexedComponentStatistics.clear();
   }

   /**
    * Close writer.
    */
   @Override
   public final void closeWriter() {
      try {
         this.reopenThread.close();

         // We don't shutdown the writer service we are using, because it is the core isaac thread pool.
         // waiting for the future checker service is sufficient to ensure that all write operations are complete.
         this.luceneWriterFutureCheckerService.shutdown();
         this.luceneWriterFutureCheckerService.awaitTermination(15, TimeUnit.MINUTES);
         this.trackingIndexWriter.getIndexWriter()
                                 .close();
      } catch (IOException | InterruptedException ex) {
         throw new RuntimeException(ex);
      }
   }

   /**
    * Commit writer.
    */
   @Override
   public final void commitWriter() {
      try {
         this.trackingIndexWriter.getIndexWriter()
                                 .commit();
         this.searcherManager.maybeRefreshBlocking();
      } catch (final IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   /**
    * Force merge.
    */
   @Override
   public void forceMerge() {
      try {
         this.trackingIndexWriter.getIndexWriter()
                                 .forceMerge(1);
         this.searcherManager.maybeRefreshBlocking();
      } catch (final IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   /**
    * Index.
    *
    * @param chronicle the chronicle
    * @return the future
    */
   @Override
   public final Future<Long> index(ObjectChronology<?> chronicle) {
      return index((() -> new AddDocument(chronicle)), (() -> indexChronicle(chronicle)), chronicle.getNid());
   }

   /**
    * Merge results on concept.
    *
    * @param searchResult the search result
    * @return the list
    */
   @Override
   public List<ConceptSearchResult> mergeResultsOnConcept(List<SearchResult> searchResult) {
      final HashMap<Integer, ConceptSearchResult> merged = new HashMap<>();
      final List<ConceptSearchResult>             result = new ArrayList<>();

      for (final SearchResult sr: searchResult) {
         final int conSequence = Frills.findConcept(sr.getNid());

         if (conSequence < 0) {
            log.error("Failed to find a concept that references nid " + sr.getNid());
         } else if (merged.containsKey(conSequence)) {
            merged.get(conSequence)
                  .merge(sr);
         } else {
            final ConceptSearchResult csr = new ConceptSearchResult(conSequence, sr.getNid(), sr.getScore());

            merged.put(conSequence, csr);
            result.add(csr);
         }
      }

      return result;
   }

   /**
    * Query index with no specified target generation of the index.
    *
    * Calls {@link #query(String, Integer, int, long)} with the semeneConceptSequence set to null and
    * the targetGeneration field set to Long.MIN_VALUE
    *
    * @param query The query to apply.
    * @param sizeLimit The maximum size of the result list.
    * @return a List of {@code SearchResult} that contains the nid of the
    * component that matched, and the score of that match relative to other matches.
    */
   @Override
   public final List<SearchResult> query(String query, int sizeLimit) {
      return query(query, null, sizeLimit, Long.MIN_VALUE);
   }

   /**
    *
    * Calls {@link #query(String, boolean, Integer, int, long)} with the prefixSearch field set to false.
    *
    * @param query The query to apply.
    * @param semeneConceptSequence optional - The concept seqeuence of the sememe that you wish to search within.  If null,
    * searches all indexed content.  This would be set to the concept sequence of {@link MetaData#ENGLISH_DESCRIPTION_ASSEMBLAGE}
    * or the concept sequence {@link MetaData#SCTID} for example.
    * @param sizeLimit The maximum size of the result list.
    * @param targetGeneration target generation that must be included in the search or Long.MIN_VALUE if there is no
    * need to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any
    * in-progress indexing operations are completed - and then use the latest index.
    * @return a List of {@code SearchResult} that contains the nid of the component that matched, and the score of
    * that match relative to other matches.
    */
   @Override
   public final List<SearchResult> query(String query,
         Integer[] semeneConceptSequence,
         int sizeLimit,
         Long targetGeneration) {
      return query(query, false, semeneConceptSequence, sizeLimit, targetGeneration);
   }

   /**
    * A generic query API that handles most common cases.  The cases handled for various component property types
    * are detailed below.
    *
    * NOTE - subclasses of LuceneIndexer may have other query(...) methods that allow for more specific and or complex
    * queries.  Specifically both {@link SememeIndexer} and {@link DescriptionIndexer} have their own
    * query(...) methods which allow for more advanced queries.
    *
    * @param query The query to apply.
    * @param prefixSearch if true, utilize a search algorithm that is optimized for prefix searching, such as the searching
    * that would be done to implement a type-ahead style search.  Does not use the Lucene Query parser.  Every term (or token)
    * that is part of the query string will be required to be found in the result.
    *
    * Note, it is useful to NOT trim the text of the query before it is sent in - if the last word of the query has a
    * space character following it, that word will be required as a complete term.  If the last word of the query does not
    * have a space character following it, that word will be required as a prefix match only.
    *
    * For example:
    * The query "family test" will return results that contain 'Family Testudinidae'
    * The query "family test " will not match on  'Testudinidae', so that will be excluded.
    * @param sememeConceptSequence the sememe concept sequence
    * @param sizeLimit The maximum size of the result list.
    * @param targetGeneration target generation that must be included in the search or Long.MIN_VALUE if there is no need
    * to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any in progress
    * indexing operations are completed - and then use the latest index.
    * @return a List of {@link SearchResult} that contains the nid of the component that matched, and the score of that match relative
    * to other matches.
    */
   @Override
   public abstract List<SearchResult> query(String query,
         boolean prefixSearch,
         Integer[] sememeConceptSequence,
         int sizeLimit,
         Long targetGeneration);

   /**
    * Report indexed items.
    *
    * @return the hash map
    */
   @Override
   public HashMap<String, Integer> reportIndexedItems() {
      final HashMap<String, Integer> result = new HashMap<>();

      this.indexedComponentStatistics.forEach((name, value) -> {
               result.put(name, value.get());
            });
      return result;
   }

   /**
    * Adds the fields.
    *
    * @param chronicle the chronicle
    * @param doc the doc
    */
   protected abstract void addFields(ObjectChronology<?> chronicle, Document doc);

   /**
    * Builds the prefix query.
    *
    * @param searchString the search string
    * @param field the field
    * @param analyzer the analyzer
    * @return the query
    * @throws IOException Signals that an I/O exception has occurred.
    */
   protected Query buildPrefixQuery(String searchString, String field, Analyzer analyzer)
            throws IOException {
      final TokenStream tokenStream;
      final List<String>  terms;
      try (StringReader textReader = new StringReader(searchString)) {
         tokenStream = analyzer.tokenStream(field, textReader);
         tokenStream.reset();
         terms = new ArrayList<>();
         final CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
         while (tokenStream.incrementToken()) {
            terms.add(charTermAttribute.toString());
         }
      }
      tokenStream.close();
      analyzer.close();

      final BooleanQuery bq = new BooleanQuery();

      if ((terms.size() > 0) &&!searchString.endsWith(" ")) {
         final String last = terms.remove(terms.size() - 1);

         bq.add(new PrefixQuery((new Term(field, last))), Occur.MUST);
      }

      terms.stream().forEach((s) -> {
                       bq.add(new TermQuery(new Term(field, s)), Occur.MUST);
                    });
      return bq;
   }

   /**
    * Create a query that will match on the specified text using either the WhitespaceAnalyzer or the StandardAnalyzer.
    * Uses the Lucene Query Parser if prefixSearch is false, otherwise, uses a custom prefix algorithm.
    * See {@link LuceneIndexer#query(String, boolean, Integer, int, Long)} for details on the prefix search algorithm.
    *
    * @param query the query
    * @param field the field
    * @param prefixSearch the prefix search
    * @return the query
    */
   protected Query buildTokenizedStringQuery(String query, String field, boolean prefixSearch) {
      try {
         final BooleanQuery bq = new BooleanQuery();

         if (prefixSearch) {
            bq.add(buildPrefixQuery(query, field, new PerFieldAnalyzer()), Occur.SHOULD);
            bq.add(buildPrefixQuery(query, field + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER, new PerFieldAnalyzer()),
                   Occur.SHOULD);
         } else {
            final QueryParser qp1 = new QueryParser(field, new PerFieldAnalyzer());

            qp1.setAllowLeadingWildcard(true);
            bq.add(qp1.parse(query), Occur.SHOULD);

            final QueryParser qp2 = new QueryParser(field + PerFieldAnalyzer.WHITE_SPACE_FIELD_MARKER,
                                                    new PerFieldAnalyzer());

            qp2.setAllowLeadingWildcard(true);
            bq.add(qp2.parse(query), Occur.SHOULD);
         }

         final BooleanQuery wrap = new BooleanQuery();

         wrap.add(bq, Occur.MUST);
         return wrap;
      } catch (IOException | ParseException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Increment indexed item count.
    *
    * @param name the name
    */
   protected void incrementIndexedItemCount(String name) {
      AtomicInteger temp = this.indexedComponentStatistics.get(name);

      if (temp == null) {
         try {
            this.indexedComponentStatisticsBlock.acquireUninterruptibly();
            temp = this.indexedComponentStatistics.get(name);

            if (temp == null) {
               temp = new AtomicInteger(0);
               this.indexedComponentStatistics.put(name, temp);
            }
         } finally {
            this.indexedComponentStatisticsBlock.release();
         }
      }

      temp.incrementAndGet();
   }

   /**
    * Index chronicle.
    *
    * @param chronicle the chronicle
    * @return true, if successful
    */
   protected abstract boolean indexChronicle(ObjectChronology<?> chronicle);

   /**
    * Release latch.
    *
    * @param latchNid the latch nid
    * @param indexGeneration the index generation
    */
   protected void releaseLatch(int latchNid, long indexGeneration) {
      final IndexedGenerationCallable latch = this.componentNidLatch.remove(latchNid);

      if (latch != null) {
         latch.setIndexGeneration(indexGeneration);
      }
   }

   /**
    * Restrict to sememe.
    *
    * @param query the query
    * @param sememeConceptSequence the sememe concept sequence
    * @return the query
    */
   protected Query restrictToSememe(Query query, Integer[] sememeConceptSequence) {
      final ArrayList<Integer> nullSafe = new ArrayList<>();

      if (sememeConceptSequence != null) {
         for (final Integer i: sememeConceptSequence) {
            if (i != null) {
               nullSafe.add(i);
            }
         }
      }

      if (nullSafe.size() > 0) {
         final BooleanQuery outerWrap = new BooleanQuery();

         outerWrap.add(query, Occur.MUST);

         final BooleanQuery wrap = new BooleanQuery();

         // or together the sememeConceptSequences, but require at least one of them to match.
         for (final int i: nullSafe) {
            wrap.add(new TermQuery(new Term(FIELD_SEMEME_ASSEMBLAGE_SEQUENCE, i + "")), Occur.SHOULD);
         }

         outerWrap.add(wrap, Occur.MUST);
         return outerWrap;
      } else {
         return query;
      }
   }

   /**
    * Subclasses may call this method with much more specific queries than this generic class is capable of constructing.
    *
    * @param q - the query
    * @param sizeLimit - how many results to return (at most)
    * @param targetGeneration - target generation that must be included in the search or Long.MIN_VALUE if there is no need
    * to wait for a target generation.  Long.MAX_VALUE can be passed in to force this query to wait until any in progress
    * indexing operations are completed - and then use the latest index.
    * @param filter - an optional filter on results - if provided, the filter should expect nids, and can return true, if
    * the nid should be allowed in the result, false otherwise.  Note that this may cause large performance slowdowns, depending
    * on the implementation of your filter
    * @return the list
    */
   protected final List<SearchResult> search(Query q, int sizeLimit, Long targetGeneration, Predicate<Integer> filter) {
      try {
         if ((targetGeneration != null) && (targetGeneration != Long.MIN_VALUE)) {
            if (targetGeneration == Long.MAX_VALUE) {
               this.searcherManager.maybeRefreshBlocking();
            } else {
               try {
                  this.reopenThread.waitForGeneration(targetGeneration);
               } catch (final InterruptedException e) {
                  throw new RuntimeException(e);
               }
            }
         }

         final IndexSearcher searcher = this.searcherManager.acquire();

         try {
            log.debug("Running query: {}", q.toString());

            // Since the index carries some duplicates by design, which we will remove - get a few extra results up front.
            // so we are more likely to come up with the requested number of results
            final long limitWithExtras = sizeLimit + (long) (sizeLimit * 0.25d);
            final int  adjustedLimit   = ((limitWithExtras > Integer.MAX_VALUE) ? sizeLimit
                  : (int) limitWithExtras);
            TopDocs    topDocs;

            if (filter != null) {
               final TopDocsFilteredCollector tdf = new TopDocsFilteredCollector(adjustedLimit, q, searcher, filter);

               searcher.search(q, tdf);
               topDocs = tdf.getTopDocs();
            } else {
               topDocs = searcher.search(q, adjustedLimit);
            }

            final List<SearchResult> results               = new ArrayList<>(topDocs.totalHits);
            final HashSet<Integer>   includedComponentNids = new HashSet<>();

            for (final ScoreDoc hit: topDocs.scoreDocs) {
               log.debug("Hit: {} Score: {}", new Object[] { hit.doc, hit.score });

               final Document doc          = searcher.doc(hit.doc);
               final int      componentNid = doc.getField(FIELD_COMPONENT_NID)
                                                .numericValue()
                                                .intValue();

               if (includedComponentNids.contains(componentNid)) {
                  continue;
               } else {
                  includedComponentNids.add(componentNid);
                  results.add(new ComponentSearchResult(componentNid, hit.score));

                  if (results.size() == sizeLimit) {
                     break;
                  }
               }
            }

            log.debug("Returning {} results from query", results.size());
            return results;
         } finally {
            this.searcherManager.release(searcher);
         }
      } catch (final IOException ex) {
         throw new RuntimeException(ex);
      }
   }

   /**
    * Index.
    *
    * @param documentSupplier the document supplier
    * @param indexChronicle the index chronicle
    * @param chronicleNid the chronicle nid
    * @return the future
    */
   private Future<Long> index(Supplier<AddDocument> documentSupplier,
                              BooleanSupplier indexChronicle,
                              int chronicleNid) {
      if (!this.enabled) {
         releaseLatch(chronicleNid, Long.MIN_VALUE);
         return null;
      }

      if (indexChronicle.getAsBoolean()) {
         final Future<Long> future = this.luceneWriterService.submit(documentSupplier.get());

         this.luceneWriterFutureCheckerService.execute(new FutureChecker(future));
         return future;
      } else {
         releaseLatch(chronicleNid, Long.MIN_VALUE);
      }

      return unindexedFuture;
   }

   /**
    * Start me.
    */
   @PostConstruct
   private void startMe() {
      log.info("Starting " + getIndexerName() + " post-construct");
   }

   /**
    * Start thread.
    */
   private void startThread() {
      this.reopenThread.setName("Lucene " + this.indexName + " Reopen Thread");
      this.reopenThread.setPriority(Math.min(Thread.currentThread()
            .getPriority() + 2, Thread.MAX_PRIORITY));
      this.reopenThread.setDaemon(true);
      this.reopenThread.start();
   }

   /**
    * Stop me.
    */
   @PreDestroy
   private void stopMe() {
      log.info("Stopping " + getIndexerName() + " pre-destroy. ");
      commitWriter();
      closeWriter();
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the database folder.
    *
    * @return the database folder
    */
   @Override
   public Path getDatabaseFolder() {
      return this.indexFolder.toPath();
   }

   /**
    * Gets the database validity status.
    *
    * @return the database validity status
    */
   @Override
   public DatabaseValidity getDatabaseValidityStatus() {
      return this.databaseValidity;
   }

   /**
    * Checks if enabled.
    *
    * @return true, if enabled
    */
   @Override
   public boolean isEnabled() {
      return this.enabled;
   }

   //~--- set methods ---------------------------------------------------------

   /**
    * Sets the enabled.
    *
    * @param enabled the new enabled
    */
   @Override
   public void setEnabled(boolean enabled) {
      this.enabled = enabled;
   }

   //~--- get methods ---------------------------------------------------------

   /**
    * Gets the indexed generation callable.
    *
    * @param nid for the component that the caller wished to wait until it's document is added to the index.
    * @return a {@link IndexedGenerationCallable} object that will block until this indexer has added the
    * document to the index. The {@link IndexedGenerationCallable#call()} method on the object will return the
    * index generation that contains the document, which can be used in search calls to make sure the generation
    * is available to the searcher.
    */
   @Override
   public IndexedGenerationCallable getIndexedGenerationCallable(int nid) {
      final IndexedGenerationCallable indexedLatch         = new IndexedGenerationCallable();
      final IndexedGenerationCallable existingIndexedLatch = this.componentNidLatch.putIfAbsent(nid, indexedLatch);

      if (existingIndexedLatch != null) {
         return existingIndexedLatch;
      }

      return indexedLatch;
   }

   /**
    * Gets the indexer folder.
    *
    * @return the indexer folder
    */
   @Override
   public File getIndexerFolder() {
      return this.indexFolder;
   }

   /**
    * Gets the indexer name.
    *
    * @return the indexer name
    */
   @Override
   public String getIndexerName() {
      return this.indexName;
   }

   //~--- inner classes -------------------------------------------------------

   /**
    * The Class AddDocument.
    */
   private class AddDocument
            implements Callable<Long> {
      /** The chronicle. */
      ObjectChronology<?> chronicle = null;

      //~--- constructors -----------------------------------------------------

      /**
       * Instantiates a new adds the document.
       *
       * @param chronicle the chronicle
       */
      public AddDocument(ObjectChronology<?> chronicle) {
         this.chronicle = chronicle;
      }

      //~--- methods ----------------------------------------------------------

      /**
       * Call.
       *
       * @return the long
       * @throws Exception the exception
       */
      @Override
      public Long call()
               throws Exception {
         final Document doc = new Document();

         doc.add(new IntField(FIELD_COMPONENT_NID,
                              this.chronicle.getNid(),
                              LuceneIndexer.FIELD_TYPE_INT_STORED_NOT_INDEXED));
         addFields(this.chronicle, doc);

         // Note that the addDocument operation could cause duplicate documents to be
         // added to the index if a new luceneVersion is added after initial index
         // creation. It does this to avoid the performance penalty of
         // finding and deleting documents prior to inserting a new one.
         //
         // At this point, the number of duplicates should be
         // small, and we are willing to accept a small number of duplicates
         // because the new versions are additive (we don't allow deletion of content)
         // so the search results will be the same. Duplicates can be removed
         // by regenerating the index.
         final long indexGeneration = LuceneIndexer.this.trackingIndexWriter.addDocument(doc);

         releaseLatch(getNid(), indexGeneration);
         return indexGeneration;
      }

      //~--- get methods ------------------------------------------------------

      /**
       * Gets the nid.
       *
       * @return the nid
       */
      public int getNid() {
         return this.chronicle.getNid();
      }
   }


   /**
    * Class to ensure that any exceptions associated with indexingFutures are properly logged.
    */
   private static class FutureChecker
            implements Runnable {
      /** The future. */
      Future<Long> future;

      //~--- constructors -----------------------------------------------------

      /**
       * Instantiates a new future checker.
       *
       * @param future the future
       */
      public FutureChecker(Future<Long> future) {
         this.future = future;
      }

      //~--- methods ----------------------------------------------------------

      /**
       * Run.
       */
      @Override
      public void run() {
         try {
            this.future.get();
         } catch (InterruptedException | ExecutionException ex) {
            log.fatal("Unexpected error in future checker!", ex);
         }
      }
   }


   /**
    * The Class UnindexedFuture.
    */
   private static class UnindexedFuture
            implements Future<Long> {
      /**
       * Cancel.
       *
       * @param mayInterruptIfRunning the may interrupt if running
       * @return true, if successful
       */
      @Override
      public boolean cancel(boolean mayInterruptIfRunning) {
         return false;
      }

      //~--- get methods ------------------------------------------------------

      /**
       * Checks if cancelled.
       *
       * @return true, if cancelled
       */
      @Override
      public boolean isCancelled() {
         return false;
      }

      /**
       * Checks if done.
       *
       * @return true, if done
       */
      @Override
      public boolean isDone() {
         return true;
      }

      /**
       * Gets the.
       *
       * @return the long
       * @throws InterruptedException the interrupted exception
       * @throws ExecutionException the execution exception
       */
      @Override
      public Long get()
               throws InterruptedException, ExecutionException {
         return Long.MIN_VALUE;
      }

      /**
       * Gets the.
       *
       * @param timeout the timeout
       * @param unit the unit
       * @return the long
       * @throws InterruptedException the interrupted exception
       * @throws ExecutionException the execution exception
       * @throws TimeoutException the timeout exception
       */
      @Override
      public Long get(long timeout, TimeUnit unit)
               throws InterruptedException, ExecutionException, TimeoutException {
         return Long.MIN_VALUE;
      }
   }
}
