public class RetrievalModelBM25 extends RetrievalModel{
	double k_1;
	double b;
	double k_3;
	
	public String defaultQrySopName () {
		    return new String ("#sum");
	}

	public static void main() {
		// TODO Auto-generated method stub

	}
	public RetrievalModelBM25(double k_1, double b, double k_3) {
		this.k_1 = k_1;
		this.b = b;
		this.k_3 = k_3;
	}

}
