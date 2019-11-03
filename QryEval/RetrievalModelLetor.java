import java.util.Map;

public class RetrievalModelLetor extends RetrievalModel{
	
	public String trainQuery, trainQrels, trainFV;
	public String featureDisable;
	public String svmRankLearn, svmRankClassify, svmRankModel;
	public double svmParamC;
	public String testFV, testDocScore;
	public double k_1, k_3, b;
	public double mu, lambda;
	public String queryFilePath;
	
	

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
	
	public String defaultQrySopName() {
		// TODO Auto-generated method stub
		return null;
	}

	public RetrievalModelLetor(Map<String, String> parameters) {
		
		this.trainQuery = parameters.get("letor:trainingQueryFile");
		this.trainQrels = parameters.get("letor:trainingQrelsFile");
		this.trainFV = parameters.get("letor:trainingFeatureVectorsFile");
		this.featureDisable = parameters.get("letor:featureDisable");
		this.svmRankLearn = parameters.get("letor:svmRankLearnPath");
		this.svmRankClassify = parameters.get("letor:svmRankClassifyPath");
		this.svmRankModel = parameters.get("letor:svmRankModelFile");
		this.svmParamC = Double.parseDouble(parameters.get("letor:svmRankParamC")); // trim
		this.testFV = parameters.get("letor:testingFeatureVectorsFile");
		this.testDocScore = parameters.get("letor:testingDocumentScores");
		this.queryFilePath = parameters.get("queryFilePath");
		this.k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
		this.b = Double.parseDouble(parameters.get("BM25:b"));
		this.k_3 = Double.parseDouble(parameters.get("BM25:k_1"));
		this.mu = Double.parseDouble(parameters.get("Indri:mu"));
		this.lambda = Double.parseDouble(parameters.get("Indri:lambda"));		
		
	}
}
