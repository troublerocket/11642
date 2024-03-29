/** 
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;

/**
 *  Idx manages and provides access to Lucene indexes and auxiliary
 *  data structures.
 *  <p>
 *  Most homework assignments only require a single index.  However,
 *  several distinct indexes can be open simultaneously (e.g., for
 *  federated search).  The Idx class designates one index the
 *  <i>current</i> index.  All requests are satisfied from the current
 *  index.  setCurrentIndex changes the current index.
 *  </p>
 */
public class Idx {

  //  --------------- Constants and variables ---------------------

  /**
   *  The Lucene index that is considered the current index.
   */
  public static IndexReader INDEXREADER=null;

  private static HashMap<String,IndexReader> openIndexReaders =
    new HashMap<String,IndexReader> ();
  private static String externalIdField = new String ("externalId");

  //  --------------- Methods ---------------------------------------

  /**
   *  Get the specified attribute from the specified document.
   *  @param attributeName Name of attribute
   *  @param docid The internal docid in the lucene index.
   *  @return the attribute value
   *  @throws IOException Error accessing the Lucene index.
   */
  public static String getAttribute (String attributeName, int docid)
    throws IOException {

    Document d = Idx.INDEXREADER.document (docid);
    return d.get (attributeName);
  }

  /**
   *  Get the number of documents that contain the specified field.
   *  @param fieldName the field name
   *  @return the number of documents that contain the field
   *  @throws IOException Error accessing the Lucene index.
   */
  public static int getDocCount (String fieldName)
    throws IOException {
    return Idx.INDEXREADER.getDocCount (fieldName);
  }

  /**
   *  Get the external document id for a document specified by an
   *  internal document id.
   *  @param internalId The internal document id of the document.
   *  @return the external document id
   *  @throws IOException Error accessing the Lucene index.
   */
  public static String getExternalDocid(int internalId) throws IOException {
    Document d = Idx.INDEXREADER.document(internalId);
    String externalId = d.get(externalIdField);
    return externalId;
  }

  /**
   *  Get the length of the specified field in the specified document.
   *  @param fieldName Name of field to access lengths.
   *  @param docid The internal docid in the Lucene index.
   *  @return the length of the field, including stopword positions.
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getFieldLength (String fieldName, int docid)
    throws IOException {

    LeafReaderContext leafContext = getLeafReaderContext (Idx.INDEXREADER, docid);
    int leafDocid = docid - leafContext.docBase;
    LeafReader leafReader = leafContext.reader ();
    NumericDocValues norms = leafReader.getNormValues (fieldName);
    long fieldLength = 0;
	    
    if (norms != null) {
      if (norms.advanceExact (leafDocid)) {
	fieldLength = norms.longValue();
      }
    }
	    
    return fieldLength;
  }

  /**
   * Get the internal document id for a document specified by its
   * external id, e.g. clueweb09-enwp00-88-09710. If no such document
   * exists, throw an exception.
   * @param externalId The external docid in the Lucene index.
   * @return iternal docid.
   * @throws Exception Could not read the internal document id from the index.
   */
  public static int getInternalDocid(String externalId)
    throws Exception {

    LeafReaderContext leafContext =
      getLeafReaderContext (Idx.INDEXREADER, externalId);

    if (leafContext == null) 
      throw new Exception ("External id " + externalId + " not found.");

    Term term = new Term (externalIdField, externalId);
    LeafReader leafReader = leafContext.reader();
    PostingsEnum postings = leafReader.postings (term);

    if (postings.nextDoc() != DocIdSetIterator.NO_MORE_DOCS) {
      int internalId = leafContext.docBase + postings.docID();
      return (internalId);
    };

    throw new Exception ("External id should exist, but isn't found.");
  }

  /**
   *  Get the context of the LeafReader that contains the specified document.
   *  @param reader An IndexReader (probably a CompositeReader)
   *  @param docid An external document id
   *  @return the LeafReaderContext that contains the document, or null
   *  @throws IOException Error accessing the Lucene index.
   */
  private static LeafReaderContext getLeafReaderContext (IndexReader reader, String docid)
    throws Exception, IOException {

    Term term = new Term (externalIdField, docid);

    if (reader.docFreq (term) > 1)
      throw new Exception ("Multiple matches for external id " + docid);

    for (LeafReaderContext leafContext : reader.leaves()) {
      LeafReader leafReader = leafContext.reader();
      if (leafReader.postings (term) != null) {
	return (leafContext);
      };
    };
	
    return null;
  }

  /**
   *  Get the context of the LeafReader that contains the specified document.
   *  @param reader An IndexReader (probably a CompositeReader)
   *  @param docid An internal document id
   *  @return the LeafReaderContext that contains the document, or null
   */
  private static LeafReaderContext getLeafReaderContext (IndexReader reader, long docid) {

    LeafReader leafReader = null;

    for (LeafReaderContext leafContext : reader.leaves()) {
      leafReader = leafContext.reader();
      int minDocid = leafContext.docBase;
      int maxDocid = leafContext.docBase + leafReader.numDocs();
      if ((docid >= minDocid) && (docid < maxDocid)) {
	return leafContext;
      };
    };

    return null;
  }

  /**
   *  Get the total number of documents in the corpus.
   *  @return The total number of documents.
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getNumDocs () throws IOException {
    return Idx.INDEXREADER.numDocs();
  }

  /**
   *  Get the total number of term occurrences contained in all
   *  instances of the specified field in the corpus (e.g., add up the
   *  lengths of every TITLE field in the corpus).
   *  @param fieldName The field name.
   *  @return The total number of term occurrence
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getSumOfFieldLengths (String fieldName)
    throws IOException {
    return Idx.INDEXREADER.getSumTotalTermFreq (fieldName);
  }


  /**
   *  Get the collection term frequency (ctf) of a term in
   *  a field (e.g., the total number of times the term 'apple'
   *  occurs in title fields.
   *  @param fieldName The field name.
   *  @param term The term.
   *  @return The total number of term occurrence
   *  @throws IOException Error accessing the Lucene index.
   */
  public static long getTotalTermFreq (String fieldName, String term)
    throws IOException {
    return INDEXREADER.totalTermFreq (new Term (fieldName, new BytesRef (term)));
  }


  /**
   *  Open a Lucene index.
   *  @param indexPath A directory that contains a Lucene index.
   *  @throws IllegalArgumentException Unable to open the index.
   *  @throws IOException Error accessing the index.
   */
  public static void open (String indexPath)
    throws IllegalArgumentException, IOException {

    IndexReader indexReader;

    //  Open the Lucene index

    indexReader = 
      DirectoryReader.open (FSDirectory.open (Paths.get (indexPath)));
  
    if (indexReader == null) {
      throw new IllegalArgumentException ("Unable to open the index.");
    }
  
    //  Keep track of the open indexes.

    openIndexReaders.put (indexPath, indexReader);

    //  The current index defaults to the first open index.

    if (Idx.INDEXREADER == null) {
      Idx.INDEXREADER = indexReader;
    }
  }

  /**
   *  Change the current index to another open Lucene index.
   *  @param indexPath A directory that contains an open Lucene index.
   *  @throws IllegalArgumentException The specified index isn't open.
   */
  public static void setCurrentIndex (String indexPath)
    throws IllegalArgumentException {

    IndexReader indexReader = openIndexReaders.get (indexPath);

    if (indexReader == null) {
      throw new IllegalArgumentException (
        "An index must be open before it can be the current index");
    }

    Idx.INDEXREADER = indexReader;
  }
}
