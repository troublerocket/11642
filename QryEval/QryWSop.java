import java.util.*;

public abstract class QryWSop extends QrySop{
	
	ArrayList<Double> weights = new ArrayList<Double>();
	
    public void setWeights(ArrayList<Double> weights) {
        this.weights = weights;
        
    }
    public void addWeight(double weight) {
        weights.add(weight);
    }
 
    public double sumWeight() {
        double sum = 0.0;
        for (Double w : weights) 
        	sum += w;
        return sum;
    }
    
    
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
