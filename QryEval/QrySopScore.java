/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;

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
  public double getScoreBM25(int doc_id, String query, String field, double k_1, double b, double k_3) throws IOException {
	  		double score = 0.0;
	  		
	  		String[] qry_terms = QryParser.tokenizeString(query);
			double doc_len = (double)Idx.getFieldLength(field, doc_id);
			double avg_len = Idx.getSumOfFieldLengths(field) / (double) Idx.getDocCount(field);
			TermVector tv = new TermVector(doc_id, field);
			double N = (double)Idx.getNumDocs();
			
			if (tv.positionsLength() == 0)
				return Double.MIN_VALUE;

			for(String term : qry_terms) {
			      int idx = tv.indexOfStem(term);
			      if (idx == -1)
			        continue;
			      
			      double tf = (double)tv.stemFreq(idx);
			      double df = (double)tv.stemDf(idx); 
			      
			      double rsj_weight = Math.max(0, Math.log((N - df + 0.5) / (df + 0.5)));
			      double tf_weight = tf / (tf + k_1 * (1 - b + b * doc_len / avg_len));
			      double user_weight = (k_3 + 1) * 1 / (k_3 + 1);
			      
			      score += rsj_weight * tf_weight * user_weight;
			
			}
			
			return score;

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
  
  public double getScoreIndri(int doc_id, String query, String field, double lambda, double mu) throws IOException {
		
	  		double score = 1.0;
	  		String[] qry_terms = QryParser.tokenizeString(query);
	  		
			double doc_len = Idx.getFieldLength(field, doc_id);
			double collection_len = Idx.getSumOfFieldLengths(field);

			TermVector tv = new TermVector(doc_id, field);
			if (tv.positionsLength() == 0)
				return Double.MIN_VALUE;
			boolean flag = false;
			for(String term : qry_terms) {
				int idx = tv.indexOfStem(term);
			    double tf = 0.0;
			    if (idx != -1) {
			        tf = (double)tv.stemFreq(idx);
			        flag = true;
			      }
				double ctf = (double)Idx.getTotalTermFreq(field, term);
				
				double mle = ctf / collection_len;
				
				double temp_score = (1 - lambda) * (tf + mu * mle) / (doc_len + mu) + lambda * mle;
				
				score *= (Math.pow(temp_score, 1.0 / (double)qry_terms.length));

			}
			if(flag)
				return score;
			else
				return 0.0;

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
  
	public double getOverlapScore(int doc_id, String query, String field) throws IOException {
		
		String[] qry_terms = QryParser.tokenizeString(query);
	    double score = 0.0;
	    TermVector tv = new TermVector(doc_id, field);
	    for (String term : qry_terms) {
	      if (tv.positionsLength() == 0)
	        return Double.MIN_VALUE;
	      int idx = tv.indexOfStem(term);
	      if (idx == -1)
	        continue;
	      double tf = tv.stemFreq(idx);
	      if (tf != 0)
	        score += 1.0;
	    }
	    return score / (double)qry_terms.length;
	}
	
	public double getTfIdfScore(int doc_id, String query, String field) throws IOException{
  		double score = 0.0;
  		
  		String[] qry_terms = QryParser.tokenizeString(query);
		TermVector tv = new TermVector(doc_id, field);
	    double N = (double)Idx.getNumDocs();

		
		if (tv.positionsLength() == 0)
			return Double.MIN_VALUE;

		for(String term : qry_terms) {
		      int idx = tv.indexOfStem(term);
		      if (idx == -1)
		        continue;
		      double tf = (double)tv.stemFreq(idx);
		      double df = (double)tv.stemDf(idx); 
		      double idf = Math.max(0.0, Math.log((N - df + 0.5) / (df + 0.5)));
		      
		      score += tf * idf;
		}
		return score;

	}
	
	public double getAvgtfScore(int doc_id, String query, String field) throws IOException{
  		double score = 0.0;
  		
  		String[] qry_terms = QryParser.tokenizeString(query);
		TermVector tv = new TermVector(doc_id, field);

		if (tv.positionsLength() == 0)
			return Double.MIN_VALUE;

		for(String term : qry_terms) {
		      int idx = tv.indexOfStem(term);
		      if (idx == -1)
		        continue;
		      double tf = (double)tv.stemFreq(idx);
		      
		      score += tf;
		}
		if(qry_terms.length != 0)
			return score / (double)qry_terms.length;
		else
			return 0.0;

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
