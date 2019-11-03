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
public class QryIopWindow extends QryIop{
	
	private int distance;
	

  /**
   *  Distance between adjacent arguments.
   *  @param Distance Distance between adjacent arguments.
   */
	public QryIopWindow(int n) {
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
            
            while(true) {
            	int loc_min = Integer.MAX_VALUE;
            	int loc_max = Integer.MIN_VALUE;
            	int curr_loc;
            	int i;
            	QryIop min_term = null;
            	for(i = 0; i < this.args.size(); i++) {
            		QryIop curr_term = (QryIop)this.getArg(i);
            		if(!curr_term.locIteratorHasMatch())
            			break;
            		else {
            			curr_loc = curr_term.locIteratorGetMatch();
            			if(curr_loc < loc_min) {
            				loc_min = curr_loc;
            				min_term = curr_term;
            			}
            			if(curr_loc > loc_max) {
            				loc_max = curr_loc;
            			}
            		}
            	}//loop all terms
            	
            	if(i == this.args.size()) {
            		if(loc_max-loc_min < this.distance) {
                		postings.add(loc_max);
                		for(Qry q:this.args)
                			((QryIop)q).locIteratorAdvance();
            		}
            		else {
            			min_term.locIteratorAdvance();
            		}
            	}
            	else
            		break;
            }

            
            if(!postings.isEmpty())
            	this.invertedList.appendPosting(doc_id, postings);
            
            this.getArg(0).docIteratorAdvancePast(doc_id);
        	
        }// doc loop
 	
	}

}