/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  The NEAR operator for all retrieval models.  The TERM operator stores
 *  information about a query term, for example "apple" in the query
 *  "#AND (apple pie).  Although it may seem odd to use a query
 *  operator to store a term, doing so makes it easy to build
 *  structured queries with nested query operators.
 *
 */
public class QryIopNear extends QryIop{
	
	private int distance;
	

  /**
   *  Distance between adjacent arguments.
   *  @param Distance Distance between adjacent arguments.
   */
	public QryIopNear(int n) {
		this.distance = n;
	}
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
  /**
   *  Evaluate the query operator; the result is an internal inverted
   *  list that may be accessed via the internal iterators.
   *  @throws IOException Error accessing the Lucene index.
   */	
	protected void evaluate () throws IOException {
	    //  Create an empty inverted list.  If there are only one or zero query arguments,
	    //  this is the final result.
		this.invertedList = new InvList(this.getField());
        if (this.args.size() < 2) {
        	return;
        }

        while(this.docIteratorHasMatchAll(null)) {
        	int doc_id = this.docIteratorGetMatch();
    		this.docIteratorAdvancePast(doc_id);

        }// doc loop

	   
	    
	    
	    
	    
	    
	    
	
	}

}
