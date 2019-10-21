import java.io.*;

public class QrySopWSum extends QryWSop {

	public boolean docIteratorHasMatch (RetrievalModel r) {
	    return this.docIteratorHasMatchMin(r);
	  }

	public double getScore(RetrievalModel r) throws IOException{
		if (r instanceof RetrievalModelIndri) {
	        return this.getScoreIndri (r);
	      }
	      //  STUDENTS::
	      //  Add support for other retrieval models here.
		else {
		  throw new IllegalArgumentException
	          (r.getClass().getName() + " doesn't support the WSUM operator.");
	      }
		
	}

	public double getScoreIndri(RetrievalModel r) throws IOException{
	
		double score = 0.0;
		int docid = this.docIteratorGetMatch();
		
	    for (int i = 0; i < this.args.size(); i++) {
	    	
	        QrySop q = (QrySop)this.args.get(i);
	
	        if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docid ) {
	        	score += q.getScore(r) * weights.get(i) / this.sumWeight();
	        } 
	        else {
	        	score += q.getDefaultScore(r, docid) * weights.get(i) / this.sumWeight();
	        }
	
	    }
	
	    return score;
		
	}

	public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

	    double score = 0.0;
	
	    for ( int i = 0; i < this.args.size(); i++ ) {
	        QrySop q = (QrySop) this.args.get(i);
	        score += q.getDefaultScore(r, docid) * weights.get(i) / this.sumWeight();
	    }
	
	    return score;
	
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub
	
	}
}