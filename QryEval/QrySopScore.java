/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } 

    //  STUDENTS::
    //  Add support for other retrieval models here.
    else if(r instanceof RetrievalModelRankedBoolean) {
    	return this.getScoreRankedBoolean(r);
    }
    else if (r instanceof RetrievalModelBM25) {
        return this.getScoreBM25(r);
    } else if (r instanceof RetrievalModelIndri) {
        return this.getScoreIndri(r);
    }
    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }
  
  /**
   *  getScore for the Unranked retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }
  
  public double getScoreRankedBoolean (RetrievalModel r) throws IOException {
	  if(!this.docIteratorHasMatchCache()) {
		  return 0.0;
	  }else {
		  return ((QryIop)this.args.get(0)).docIteratorGetMatchPosting().tf;
	  }
  }
  
  public double getScoreBM25(RetrievalModel r) throws IOException {
		Qry q = this.args.get(0);
		if (q.docIteratorHasMatch(r)) {
			String field = ((QryIop) q).getField();

			long doc_num = Idx.getNumDocs();
			double doc_len = Idx.getFieldLength(field, q.docIteratorGetMatch());
			double avg_len = Idx.getSumOfFieldLengths(field) / (double) Idx.getDocCount(field);

			
			double tf = ((QryIop) q).docIteratorGetMatchPosting().tf;
			double df = ((QryIop) q).getDf();

			double k_1 = ((RetrievalModelBM25) r).k_1;
			double b = ((RetrievalModelBM25) r).b;
			double k_3 = ((RetrievalModelBM25) r).k_3;
			
			double rsj_weight = Math.max(0, Math.log((doc_num - df + 0.5) / (df + 0.5)));

			double tf_weight = tf / (tf + k_1 * (1 - b + b * doc_len / avg_len));

			double user_weight = (k_3 + 1) * 1 / (k_3 + 1);

			return rsj_weight * tf_weight * user_weight;
		}
		return 0.0;	  
	  
	  
  }
  public double getScoreIndri(RetrievalModel r) throws IOException {
	  	//System.out.println("Get score indri");
		Qry q = this.args.get(0);
		
		//if(q.docIteratorHasMatchCache()) {
		//if (q.docIteratorHasMatch(r)) {
			//System.out.println("matched doc\n");
			String field = ((QryIop) q).getField();

			double lambda = ((RetrievalModelIndri) r).lambda;
			double mu = ((RetrievalModelIndri) r).mu;

			double ctf = ((QryIop) q).getCtf();	
			//double tf = ((QryIop) q).docIteratorGetMatchPosting().tf;
			
			double tf = ((QryIop)q).getInvertedList().postings.get(((QryIop)q).docIteratorIndex).tf;
			//double tf2 = ((QryIop)q).getInvertedList().postings.get(q.docIteratorGetMatch()).tf;
			
			double doc_len = Idx.getFieldLength(field, q.docIteratorGetMatch());
			//double doc_len = Idx.getFieldLength(field, ((QryIop)q).getInvertedList().getDocid(((QryIop)q).docIteratorIndex));
			
			double collection_len = Idx.getSumOfFieldLengths(field);
			
			double mle = ctf / collection_len;
			//double score = (1 - lambda) * (tf + mu * ctf / collection_len) / (doc_len + mu) + lambda * ctf /collection_len;
			double score = (1.0 - lambda) * (tf + mu * mle) / (doc_len + mu) + lambda * mle;
			
			return score;
		//}
		//return 0.0;
		 
		//return ((QryIop)q).getScoreIndri(r);
  }

  
  public double getDefaultScore(RetrievalModel r, long doc_id) throws IOException {
	  	//System.out.println("Get default score indri");

	    Qry q = this.args.get(0);
	    String field = ((QryIop) q).getField();
	    
	    double lambda = ((RetrievalModelIndri) r).lambda;
	    double mu = ((RetrievalModelIndri) r).mu;

	    double ctf = ((QryIop) q).getCtf();
	    
	    // extra smoothing
	    if (Math.abs(ctf - 0) < 1e-9)
             ctf = 0.5;
	    
	    double doc_len = Idx.getFieldLength(field, (int)doc_id);
	    double collection_len = Idx.getSumOfFieldLengths(field);
	    
	    double mle = ctf / collection_len;
	    //double score = (1 - lambda) * (mu * ctf / collection_len) / (doc_len + mu) + lambda * ctf / collection_len;
	    double score = (1.0 - lambda) * (mu * mle) / (doc_len + mu) + lambda * mle;
	    
	    return score;
	  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
