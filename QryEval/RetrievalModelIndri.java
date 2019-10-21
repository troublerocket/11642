public class RetrievalModelIndri extends RetrievalModel{
	public double mu;
	public double lambda;
	
	// fb parameters
	public boolean fb = false;
	public long fbDocs;
	public long fbTerms;
	public long fbMu;
	public double fbOrigWeight;
	public String fbInitialRankingFile = "";
	public String fbExpansionQueryFile = "";
	
	public String defaultQrySopName () {
	    return new String ("#and");
	}
	public RetrievalModelIndri(double mu, double lambda) {
		this.mu = mu;
		this.lambda = lambda;
	}
	
	public void expansionParams(boolean fb, int fbDocs, int fbTerms, int fbMu,
            double fbOrigWeight, String fbInitialRankingFile, String fbExpansionQueryFile) {
	this.fb = fb;
	this.fbDocs = fbDocs;
	this.fbTerms = fbTerms;
	this.fbMu = fbMu;
	this.fbOrigWeight = fbOrigWeight;
	this.fbInitialRankingFile = fbInitialRankingFile;
	this.fbExpansionQueryFile = fbExpansionQueryFile;
}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
