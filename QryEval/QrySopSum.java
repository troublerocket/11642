import java.io.*;

public class QrySopSum  extends QrySop{
	
	
    public boolean docIteratorHasMatch(RetrievalModel r) {
        return this.docIteratorHasMatchMin(r);
    }

    public double getScore(RetrievalModel r) throws IOException {
    	
        if (r instanceof RetrievalModelBM25) {
            return this.getScoreBM25 (r);
        } 
        else {
            throw new IllegalArgumentException
              (r.getClass().getName() + " doesn't support the SUM operator.");
        }

    }


    public double getScoreBM25(RetrievalModel r) throws IOException {
        if (!this.docIteratorHasMatch(r) ) 
        	return 0.0;
        int docid = this.docIteratorGetMatch();
        double score = 0.0;
        for ( Qry q : args ) {
            if ( q.docIteratorHasMatch(r) && q.docIteratorGetMatch() == docid )
                score += ((QrySop) q).getScore(r);
        }
        return score;
    }
    

    public double getDefaultScore(RetrievalModel r, long docid) throws IOException {
    	
        return 0.0;
    }

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
