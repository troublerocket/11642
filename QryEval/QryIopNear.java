/**
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.IOException;
import java.util.ArrayList;

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
        	QryIop first_term = (QryIop)this.getArg(0);
        	int doc_id = first_term.docIteratorGetMatch();
            ArrayList<Integer> postings = new ArrayList<>();
            while(first_term.locIteratorHasMatch()) {//first term match loop
            	int prev_loc = first_term.locIteratorGetMatch();
            	int i;
            	for(i = 1; i < this.args.size(); i++) {
            		QryIop curr_term = (QryIop)this.getArg(i);
            		curr_term.locIteratorAdvancePast(prev_loc);
            		if(!curr_term.locIteratorHasMatch())
            			break;
            		else {
            			int curr_loc = curr_term.locIteratorGetMatch();
            			if(curr_loc - prev_loc > this.distance)
            				break;
            			else
            				prev_loc = curr_loc;
            		}
            		
            	}// loop through all other terms to check match & distance
            	if(i == this.args.size()) {//complete loop through all the terms
            		postings.add(prev_loc);
            		for(Qry q:this.args)
            			((QryIop)q).locIteratorAdvance();
            	}
            	else
            		first_term.locIteratorAdvance();  		
            }// first term loop	
            
            if(!postings.isEmpty())
            	this.invertedList.appendPosting(doc_id, postings);
            
            this.getArg(0).docIteratorAdvancePast(doc_id);
        	
        }// doc loop
 	
	}

}
