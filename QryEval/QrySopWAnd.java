import java.io.*;
import java.util.*;

public class QrySopWAnd extends QryWSop {

	public boolean docIteratorHasMatch (RetrievalModel r) {
        if (r instanceof RetrievalModelIndri)
            return this.docIteratorHasMatchMin (r);
        else
            return this.docIteratorHasMatchAll (r);		  
    }
	
	public double getScore(RetrievalModel r) throws IOException{
		if (r instanceof RetrievalModelIndri) {
	        return this.getScoreIndri (r);
	      }
	      //  STUDENTS::
	      //  Add support for other retrieval models here.
		else {
		  throw new IllegalArgumentException
	          (r.getClass().getName() + " doesn't support the WAND operator.");
	      }
		
	}
	
	public double getScoreIndri(RetrievalModel r) throws IOException{
		
		double score = 1.0;
		int docid = this.docIteratorGetMatch();
		
        for (int i = 0; i < this.args.size(); i++) {
        	
            QrySop q = (QrySop)this.args.get(i);

            if (q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docid ) {
                score *= Math.pow(q.getScore(r), weights.get(i) / this.sumWeight());
            } 
            else {
                score *= Math.pow(q.getDefaultScore(r, docid), weights.get(i) / this.sumWeight());
            }

        }

        return score;
		
	}
	
    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {

        double score = 1.0;

        for ( int i = 0; i < this.args.size(); i++ ) {
            QrySop q = (QrySop) this.args.get(i);
            score *= Math.pow(q.getDefaultScore(r, docid), weights.get(i) / this.sumWeight());
        }

        return score;

    }
	
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
