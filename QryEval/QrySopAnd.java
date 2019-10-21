/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The AND operator for all retrieval models.
 */
public class QrySopAnd extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchAll(r);
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
    else if(r instanceof RetrievalModelRankedBoolean) {
    	return this.getScoreRankedBoolean(r);
    }
    else if (r instanceof RetrievalModelIndri) {
        return this.getScoreIndri(r);
    } 

    //  STUDENTS::
    //  Add support for other retrieval models here.

    else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the AND operator.");
    }
  }
  
  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }
  /**
   *  getScore for the RankedBoolean retrieval model: Max score among all the query terms.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
    	double min = Double.MAX_VALUE;
    	for (Qry q:this.args) {
    		if(q.docIteratorHasMatch(r)) {
    			if(q.docIteratorGetMatch() == this.docIteratorGetMatch()) {
    				double curr_score = ((QrySop)q).getScore(r);
    				if(curr_score < min) {
    					min = curr_score;
    				}
    			}
    		}
    	}
    	return min;
    }
  }
  
  
  private double getScoreIndri(RetrievalModel r) throws IOException {

      int docid = this.docIteratorGetMatch();
      double score = 1.0;

      for (Qry q : this.args) {
    	  
          if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docid) {
        	  
              //score *= ((QrySop) q).getScore(r);
        	  score *= Math.pow(((QrySop)q).getScore(r), 1.0/this.args.size());
          }
          
          else {
        	  
              //score *= ((QrySop) q).getDefaultScore(r, docid);
        	  score *= Math.pow(((QrySop) q).getDefaultScore(r, docid), 1.0/this.args.size());

          }
          

      }
      
      //if(Idx.getExternalDocid(docid).equals("GX233-75-15885072"))
      //System.out.println(score);
      //System.out.println("score:"+Math.pow(score, 1.0 / this.args.size()));
      //return Math.pow(score, 1.0 / this.args.size());
      return score;

  }


  public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

      double score = 1.0;

      for ( Qry q : this.args ) {
          //score *= ((QrySop) q).getDefaultScore(r, docid);
    	  score *= Math.pow(((QrySop) q).getDefaultScore(r, docid), 1.0/this.args.size());
      }

      //return Math.pow(score, 1.0 / this.args.size());
      return score;
  }

}
