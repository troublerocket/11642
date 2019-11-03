/*
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.4.
 *  
 *  Compatible with Lucene 8.1.1.
 */
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;





/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
    "Usage:  java QryEval paramFile\n\n";
  
  private static final String[] TEXT_FIELDS =
      {"body", "url", "keywords", "title"};
  
  //private static int letorFeatureNum = 18;
  
  private static int trecEvalOutputLength;
  
  private static String trecEvalOutputPath;

  
  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.
    
    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    RetrievalModel model = initializeRetrievalModel (parameters);

    if(parameters.containsKey("fb") && parameters.get("fb").equals("true")) {
    	String fbInitialRankingFile = "";
    	//String fbExpansionFile = "";
    	//System.out.println(parameters);
    	if(parameters.containsKey("fbInitialRankingFile")) {
    		fbInitialRankingFile = parameters.get("fbInitialRankingFile");
    	}
    		//System.out.println(parameters.get("fbInitialRankingFile"));
    		((RetrievalModelIndri)model).expansionParams(Boolean.parseBoolean(parameters.get("fb")),
    				Integer.parseInt(parameters.get("fbDocs")), Integer.parseInt(parameters.get("fbTerms")),
    				Integer.parseInt(parameters.get("fbMu")), Double.parseDouble(parameters.get("fbOrigWeight")),
    				fbInitialRankingFile, parameters.get("fbExpansionQueryFile"));
    	
    }

 
  
    //  Perform experiments.
    if(!(model instanceof RetrievalModelLetor)) {
        processQueryFile(parameters.get("queryFilePath"), parameters.get("trecEvalOutputPath"),
        		Integer.parseInt(parameters.get("trecEvalOutputLength")), model);   	
    }
    else {
    	trecEvalOutputLength = Integer.parseInt(parameters.get("trecEvalOutputLength"));
    	trecEvalOutputPath = parameters.get("trecEvalOutputPath");
    	initializeLetor(model);
    }


    //  Clean up.
    
    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @param parameters All of the parameters contained in the parameter file
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
    throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm").toLowerCase();

    if (modelString.equals("unrankedboolean")) {

      model = new RetrievalModelUnrankedBoolean();

      //  If this retrieval model had parameters, they would be
      //  initialized here.

    }

    //  STUDENTS::  Add new retrieval models here.
    else if(modelString.equals("rankedboolean")){
    	
    	model = new RetrievalModelRankedBoolean();
    }
    
    else if (modelString.equals("bm25")) {
        double k_1 = Double.parseDouble(parameters.get("BM25:k_1"));
        double b = Double.parseDouble(parameters.get("BM25:b"));
        double k_3 = Double.parseDouble(parameters.get("BM25:k_3"));
        
        model = new RetrievalModelBM25(k_1, b, k_3);
        
    } else if (modelString.equals("indri")) {
    	
        double mu = Double.parseDouble(parameters.get("Indri:mu"));
        //System.out.println("mu:"+mu);
        double lambda = Double.parseDouble(parameters.get("Indri:lambda"));
        //System.out.println("lambda:"+lambda);

        model = new RetrievalModelIndri(mu, lambda);
    }else if(modelString.equals("letor")) {
    	
    	model = new RetrievalModelLetor(parameters);
    }

    else {

      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }
      
    return model;
  }
  
  public static List<Double> getFeatureVector(int int_id, String query, double k_1, double b, double k_3, double lambda, double mu) throws IOException{
	  List<Double> fv = new ArrayList<>();
	  
	  // f1: spam score for d (read from index)
	  double f1 = Double.parseDouble(Idx.getAttribute("spamScore", int_id));
	  fv.add(f1);
  	
	  // f2: url depth for d(number of '/' in the rawUrl field)
	  String rawUrl = Idx.getAttribute("rawUrl", int_id);
	  double f2 = 0.0;
      for (int i = 0; i < rawUrl.length(); i ++)
          if (rawUrl.charAt(i) == '/')
          	f2 += 1;
      fv.add(f2);
       
      // f3: FromWikipedia score for d (1 if the rawUrl contains "wikipedia.org", otherwise 0)
      double f3 = 0.0;
      if (rawUrl.contains("wikipedia.org"))
      	f3 = 1.0;
      fv.add(f3);
      
      // f4: PageRank score for d (read from index).
      double f4 = Double.parseDouble(Idx.getAttribute("PageRank", int_id));
      fv.add(f4);
      
      QrySopScore sop_score = new QrySopScore();
      
      // f5: BM25 score for <q, d_body>
      double f5 = sop_score.getScoreBM25(int_id, query, "body", k_1, b, k_3);
      fv.add(f5);

      // f6: Indri score for <q, d_body>
      double f6 = sop_score.getScoreIndri(int_id, query, "body", lambda, mu);
      fv.add(f6);
			
      // f7: Term overlap score (also called Coordination Match) for <q, d_body>
      double f7 = sop_score.getOverlapScore(int_id, query, "body");
      fv.add(f7);
		
      // f8: BM25 score for <q, d_title>
      double f8 = sop_score.getScoreBM25(int_id, query, "title", k_1, b, k_3);
      fv.add(f8);

      // f9: Indri score for <q, d_title>
      double f9 = sop_score.getScoreIndri(int_id, query, "title", lambda, mu);
      fv.add(f9);
      
      // f10: Term overlap score (also called Coordination Match) for <q, d_title>
      double f10 = sop_score.getOverlapScore(int_id, query, "title");
      fv.add(f10);
      
      // f11: BM25 score for <q, d_url>
      double f11 = sop_score.getScoreBM25(int_id, query, "url", k_1, b, k_3);
      fv.add(f11);
      
      // f12: Indri score for <q, d_url>
      double f12 = sop_score.getScoreIndri(int_id, query, "url", lambda, mu);
      fv.add(f12);
      
      // f13: Term overlap score (also called Coordination Match) for <q, d_url>
      double f13 = sop_score.getOverlapScore(int_id, query, "url");
      fv.add(f13);
      
      // f14: BM25 score for <q, d_inlink>
      double f14 = sop_score.getScoreBM25(int_id, query, "inlink", k_1, b, k_3);
      fv.add(f14);
      
      // f15: Indri score for <q, d_inlink>
      double f15 = sop_score.getScoreIndri(int_id, query, "inlink", lambda, mu);
      fv.add(f15);
      
      // f16: Term overlap score (also called Coordination Match) for <q, d_inlink>
      double f16 = sop_score.getOverlapScore(int_id, query, "inlink");
      fv.add(f16);
      
      // f17: Your custom feature - avg term frequency = tf / # terms
      double f17 = sop_score.getAvgtfScore(int_id, query, "body");
      fv.add(f17);
      
      // f18: Your custom feature - tf * idf score for <q, d_body>
      double f18 = sop_score.getTfIdfScore(int_id, query, "body");
      fv.add(f18);
      
      return fv;

  }
  
  public static List<Double> fvNormalization(List<Double> fv, double Min[], double Max[]) {
	  for(int i = 0; i < fv.size(); i++) {
  		double min = Min[i];
  		double max = Max[i];
  		double score = fv.get(i);
  		if(score != Double.MIN_VALUE) {
  			if(min != max)
  				fv.set(i, (score - min) / (max - min));
  			else
  				fv.set(i, 0.0);
  		}
  		else
  			fv.set(i, 0.0);
	  }
	  
	  return fv;
	  
  }

  public static void initializeLetor(RetrievalModel model) throws Exception {
	    RetrievalModelLetor letor = (RetrievalModelLetor) model;
	    String trainQuery = letor.trainQuery;
	    String trainQrels = letor.trainQrels;
	    String trainFV = letor.trainFV;
		String featureDisable = letor.featureDisable;
		String svmRankLearn = letor.svmRankLearn;
		String svmRankClassify = letor.svmRankClassify;
		String svmRankModel = letor.svmRankModel;
		double svmParamC = letor.svmParamC;
		String queryFilePath = letor.queryFilePath;
		String testFV = letor.testFV;
		String testDocScore = letor.testDocScore;
		double k_1 = letor.k_1;
		double k_3 = letor.k_3;
		double b = letor.b;
		double mu = letor.mu;
		double lambda = letor.lambda;
	    
	    // read the featureDisable
	    List<Integer> disabledFeatures = new ArrayList<>();
	    if (featureDisable != null) {
	    	for(String f : featureDisable.split(",")) {
	    		disabledFeatures.add(Integer.parseInt(f));
	    	}
	    }
	    
	    // read the trainingQrels file
	    BufferedReader input = new BufferedReader(new FileReader(trainQrels));

	    Map<Integer, List<String>> query_doc_map = new HashMap<>();	// query and relevant documents

	    Map<Map.Entry<Integer, String>, Integer> qrel_map = new HashMap<>(); // relevance judgments
	    
	    String line = null;
	    while((line = input.readLine()) != null) {
	        String[] str = line.split("\\s+");
	        int q_id = Integer.parseInt(str[0]);
	        String externalDocid = str[2];
	        int relScore = Integer.parseInt(str[3]);
	        
	        List<String> doc_list = new ArrayList<>();
	        // clear cache
	        doc_list.clear();
	        
	        if (query_doc_map.containsKey(q_id)) {
	          doc_list = query_doc_map.get(q_id);
	        }
	        doc_list.add(externalDocid);
	        query_doc_map.put(q_id, doc_list);
	        
	        Map.Entry<Integer, String> pair = new AbstractMap.SimpleImmutableEntry<> (q_id, externalDocid);
	        qrel_map.put(pair, relScore);
	    }	   
	    input.close();
	    
	    input = new BufferedReader(new FileReader(trainQuery));
	    //line = null;

	    Map<Integer, String> query_map = new HashMap<>();
	    List<Integer> qid_list = new ArrayList<Integer>(query_map.size());

	    
	    while((line = input.readLine()) != null) {
	    	int idx= line.indexOf(":");
	    	
			int q_id = Integer.parseInt(line.substring(0, idx));
			String query = line.substring(idx + 1);
			
			qid_list.add(q_id);
			query_map.put(q_id, query);
	    	
	    }
	    input.close();
	    
	    //List<Integer> qid_list = new ArrayList<Integer>(query_map.size());
	    // check
	    //qid_list.addAll(query_map.keySet());
	    Collections.sort(qid_list);
	    
	    Map<String, List<Double>> doc_fv = new HashMap<>();
	    for(int q_id : qid_list) {
	    	doc_fv.clear();
	    	String query = query_map.get(q_id);
	    	List<String> ext_docs = query_doc_map.get(q_id);
	    	Collections.sort(ext_docs);
	    	
	        double Min[] = new double[18];
	        double Max[] = new double[18];
	        for (int i = 0; i < 18; i ++) {
	          Min[i] = Double.MAX_VALUE;
	          Max[i] = - Double.MAX_VALUE;
	        }
	        
	        for(String ext_id : ext_docs) {
	        	List<Double> fv = new ArrayList<>();
	        	int int_id = Idx.getInternalDocid(ext_id);
	        	
	        	fv = getFeatureVector(int_id, query, k_1, b ,k_3, lambda, mu);

	        	doc_fv.put(ext_id, fv);
	        	
	            for (int i = 0; i < fv.size(); i ++) {
	            	double score = fv.get(i);
	            	if(score == Double.MIN_VALUE)// invalid score
	            		continue;
	            	Max[i] = Math.max(Max[i], score);
	        		Min[i] = Math.min(Min[i], score);
	            }
	        }
	        
	        // Normalization
	        for(String ext_id : ext_docs) {
	        	
	        	List<Double> fv = doc_fv.get(ext_id);
	        	fv = fvNormalization(fv, Min, Max);
	        	
	            BufferedWriter output = new BufferedWriter(new FileWriter(trainFV, true));
	            Map.Entry<Integer, String> pair = new AbstractMap.SimpleImmutableEntry<> (q_id, ext_id);
	            output.write(qrel_map.get(pair) + " qid:" + q_id);
	            
	            for (int i = 0; i < fv.size(); i ++) {
	                if (disabledFeatures.contains(i + 1))
	                  continue;
	                BigDecimal longlonglong = new BigDecimal(fv.get(i));
	  			  	String outstring = " " + (i + 1) + ":" + longlonglong.toString();
	  			  	output.write(outstring);
	            }
	            output.write(" # " + ext_id + "\n");
	            output.close();

	        } // doc loop 
	    	
	    }// query loop end
	    
	    // call svmrank to train a model
	    Process cmdProc = Runtime.getRuntime().exec(
	            new String[] { svmRankLearn, "-c", String.valueOf(svmParamC), trainFV,
	                svmRankModel});
	    BufferedReader stdoutReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getInputStream()));
	    String l;
	    while ((l = stdoutReader.readLine()) != null) {
	      System.out.println(l);
	    }
	    // consume stderr and print it for debugging purposes
	    BufferedReader stderrReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getErrorStream()));
	    while ((l = stderrReader.readLine()) != null) {
	      System.out.println(l);
	    }

	    int retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
	    
	    // generate testing data for top 100 documents in initial BM25 ranking
	    RetrievalModel bm25 = new RetrievalModelBM25(k_1, b, k_3);
	    input = new BufferedReader(new FileReader(queryFilePath));
	    
	    Map<Integer, ScoreList> initRanking = new HashMap<>();
	    qid_list.clear();
	    line = null;
	    while((line = input.readLine()) != null) {
	    	int idx= line.indexOf(":");
	    	
			int q_id = Integer.parseInt(line.substring(0, idx));
			qid_list.add(q_id);
			String query = line.substring(idx + 1);
			ScoreList results = processQuery(query, bm25);
			initRanking.put(q_id, results);
			
			doc_fv.clear();

			double Min[] = new double[18];
		    double Max[] = new double[18];
		    for (int i = 0; i < 18; i ++) {
		    	Min[i] = Double.MAX_VALUE;
		    	Max[i] = - Double.MAX_VALUE;
		    }
		    for(int j = 0, len = Math.min(results.size(),trecEvalOutputLength); j < len; j++) {
		    	int int_id = results.getDocid(j);// int_id check!!!!!!
		        String ext_id = Idx.getExternalDocid(int_id);
		        List<Double> fv = new ArrayList<>();
		        
		        fv= getFeatureVector(int_id, query, k_1, b, k_3, lambda, mu);
		        
				doc_fv.put(ext_id, fv);
				for (int i = 0; i < fv.size(); i ++) {
	            	double score = fv.get(i);
	            	if(score == Double.MIN_VALUE)// invalid score
	            		continue;
	            	Max[i] = Math.max(Max[i], score);
	        		Min[i] = Math.min(Min[i], score);
	            }
				
		    }// doc loop
		    
		    for(int j = 0, len = Math.min(results.size(),trecEvalOutputLength); j < len; j++) {
		    	int int_id = results.getDocid(j);
		    	String ext_id = Idx.getExternalDocid(int_id);
		    	
	        	List<Double> fv = doc_fv.get(ext_id);
	        	// normalize
	        	fv = fvNormalization(fv, Min, Max);
		    	
		    	BufferedWriter output = new BufferedWriter(new FileWriter(testFV, true));
	            //Map.Entry<Integer, String> pair = new AbstractMap.SimpleImmutableEntry<> (q_id, ext_id);
	            output.write(0 + " qid:" + q_id);
	            
	            for (int i = 0; i < fv.size(); i ++) {
	                if (disabledFeatures.contains(i + 1))
	                  continue;
	                BigDecimal longlonglong = new BigDecimal(fv.get(i));
	  			  	String outstring = " " + (i + 1) + ":" + longlonglong.toString();
	  			  	output.write(outstring);
	            }
	            output.write(" # " + ext_id + "\n");
	            output.close();

		    }// doc loop

		      
	    }// test query loop end

	    input.close();
	    
	    // call svmrank to produce scores for the test data

	    cmdProc = Runtime.getRuntime().exec(
	    		new String[] { svmRankClassify, testFV,
	                    svmRankModel, testDocScore});
	    stdoutReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getInputStream()));
	    String l1;
	    while ((l1 = stdoutReader.readLine()) != null) {
	      System.out.println(l1);
	    }
	    // consume stderr and print it for debugging purposes
	    stderrReader = new BufferedReader(
	            new InputStreamReader(cmdProc.getErrorStream()));
	    while ((l1 = stderrReader.readLine()) != null) {
	      System.out.println(l1);
	    }
	    retValue = cmdProc.waitFor();
	    if (retValue != 0) {
	      throw new Exception("SVM Rank crashed.");
	    }
	    
	    // read in the svmrank scores and re-rank the initial ranking based on the scores
	    input = new BufferedReader(new FileReader(testDocScore));
	    
	    List<Double> svm_scores = new ArrayList<>();
	    line = null;
	    while((line = input.readLine()) != null) {
	    	svm_scores.add(Double.parseDouble(line));
	    }
	    int rank = 0;
	    for (int i = 0; i < qid_list.size(); i++) {
	      int qid = qid_list.get(i);
	      ScoreList results = initRanking.get(qid);
	      for (int j = 0, len = Math.min(results.size(), trecEvalOutputLength); j < len; j++) 
	    	  results.setDocidScore(j, svm_scores.get(rank++));
	      
	      for (int len = Math.min(results.size(), trecEvalOutputLength), j = len; j < results.size(); j ++)
	        results.setDocidScore(j, -Double.MAX_VALUE);
	      results.sort();
	      printResults(String.valueOf(qid), results, trecEvalOutputPath, trecEvalOutputLength);
	    }
 
	  
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   * 
   * @param gc 
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
        + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qryString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qryString, RetrievalModel model)
    throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qryString = defaultOp + "(" + qryString + ")";
    Qry q = QryParser.getQuery (qryString);

    // Show the query that is evaluated
    
    System.out.println("    --> " + q);
    
    if (q != null) {

      ScoreList results = new ScoreList ();
      
      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          //if(Idx.getExternalDocid(docid).equals("GX233-75-15885072"))
          //System.out.println(score);
          results.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
      }

      return results;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath Path to the query file
   *  @param model A retrieval model that will guide matching and scoring
 * @throws Exception 
   */
  static void processQueryFile(String queryFilePath, String trecEvalOutputPath, int length,
                               RetrievalModel model)
      throws Exception {

    BufferedReader input = null;
    //BufferedWriter output = null;
    BufferedWriter expansion = null;
    

    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));


      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {

        printMemoryUsage(false);
        System.out.println("Query " + qLine);
        String[] pair = qLine.split(":");
        ScoreList curr_scorelist = null;

		if (pair.length != 2) {
	          throw new IllegalArgumentException
	            ("Syntax error:  Each line must contain one ':'.");
		}

		String qid = pair[0];
		String query = pair[1];
	
		if(model instanceof RetrievalModelIndri) {
			//System.out.println(((RetrievalModelIndri)model).fb);
			if(((RetrievalModelIndri)model).fb == true) {
				if(((RetrievalModelIndri)model).fbInitialRankingFile.equals("")) {
					curr_scorelist = processQuery(query, model);
					curr_scorelist.sort();
				}
				else {
					Map<String, ScoreList> scorelist_map = readInitialRankingFile(((RetrievalModelIndri)model).fbInitialRankingFile);
					curr_scorelist = scorelist_map.get(qid);
				}
					
				String expandedQuery = expandQuery(curr_scorelist, (RetrievalModelIndri)model);
                //System.out.println(" expanded query " + expandedQuery);
                //System.out.println(" fbExpansionQueryFile " + ((RetrievalModelIndri)model).fbExpansionQueryFile);
				expansion = new BufferedWriter(new FileWriter(((RetrievalModelIndri)model).fbExpansionQueryFile,true));
				expansion.write(qid + ": " + expandedQuery + "\n");
				expansion.close();

                double fbOrigWeight = ((RetrievalModelIndri)model).fbOrigWeight;
                
				
				//System.out.println(qid + ": " + expandedQuery + "\n");
				//expansion.close();
                String originalQuery  = model.defaultQrySopName ()+ "(" + query + ")";
                String combinedQuery = "#wand (" + String.valueOf(fbOrigWeight) + " " +originalQuery + " "
                        + String.valueOf(1 - fbOrigWeight) + " " + expandedQuery + " )";
                
                query = combinedQuery;
                
                System.out.println(" combined query " + qid+ ": "+combinedQuery);
                 //results= processQuery(combined_query, model);
	                
			}
		}
		
        ScoreList results = processQuery(query, model);
        
        if (results != null) { 
          printResults(qid, results, trecEvalOutputPath, length);
          System.out.println();
        }
        
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
      // check close
      if(expansion != null)
    	  expansion.close();

    }
  }
  
  static Map<String, ScoreList> readInitialRankingFile(String fbInitialRankingFile) throws Exception{
      Map<String, ScoreList> scorelist_map = new HashMap<>();
      BufferedReader input = new BufferedReader(new FileReader(fbInitialRankingFile));
      String line = null;
      String prev_qid = null;
      ScoreList scorelist = new ScoreList();

      while ((line = input.readLine()) != null) {
    	  
    	  String[] data = line.split(" ");
    	  
          String query_id = data[0];
          String doc_id = data[2];
          double score = Double.parseDouble(data[4]);
          

          if(query_id.equals(prev_qid)) {
        	  scorelist.add(Idx.getInternalDocid(doc_id), score);
          }
          else {
        	  scorelist.sort();
        	  scorelist_map.put(prev_qid, new ScoreList(scorelist));
        	  scorelist = new ScoreList(); 
        	  scorelist.add(Idx.getInternalDocid(doc_id), score);

          }
          // update previous query id
          prev_qid = query_id;
    	  
      }
      //last query id
	  scorelist.sort();
	  scorelist_map.put(prev_qid, new ScoreList(scorelist));
	  
      input.close();
      return scorelist_map;
      
      

  }
  

  /**
   * Print the query results.
   * 
   * STUDENTS:: 
   * This is not the correct output format. You must change this method so
   * that it outputs in the format specified in the homework page, which is:
   * 
   * QueryID Q0 DocID Rank Score RunID
   * 
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryName, ScoreList result, String path, int length) throws IOException {
	  result.sort();
      BufferedWriter output = new BufferedWriter(new FileWriter(path, true));
	  int output_length;
	  if(result.size() > length)
		  output_length = length;
	  else
		  output_length = result.size();
	  //System.out.println(queryName + ":  ");
	  if (result.size() < 1) {
		  //System.out.println("\tNo results.");
		  String outstring = String.format("%s %s %s %d %f %s\n", queryName, "Q0", "dummy",
				  1, 0.0, "run-1");
		  System.out.println(outstring);
		  output.write(outstring);
	  } 
	  else {
		  for (int i = 0; i < output_length; i++) {
	        //System.out.println("\t" + i + ":  " + Idx.getExternalDocid(result.getDocid(i)) + ", "
	        //    + result.getDocidScore(i));
			  BigDecimal longlonglong = new BigDecimal(result.getDocidScore(i));
			  String outstring = String.format("%s\t%s\t%s\t%d\t%s\t%s\n",queryName, "Q0", 
						Idx.getExternalDocid(result.getDocid(i)), i+1, longlonglong.toString(), "run-1");
			  //String outstring = queryName + " Q0 "+ Idx.getExternalDocid(result.getDocid(i)) + " "
			//		  + (i+1) + " " + result.getDocidScore(i) + " run-1\n";
			  System.out.println(outstring);
			  output.write(outstring);
	      }
	  }
      output.close();

  }
  
  
  

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @param parameterFileName The name of the parameter file
   *  @return The parameters, in &lt;key, value&gt; format.
   *  @throws IllegalArgumentException The parameter file can't be read or doesn't contain required parameters
   *  @throws IOException The parameter file can't be read
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
    throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();
    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
        ("Can't read " + parameterFileName);
    }

    //  Store (all) key/value parameters in a hashmap.

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine();
      String[] pair = line.split ("=");
      parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    //  Confirm that some of the essential parameters are present.
    //  This list is not complete.  It is just intended to catch silly
    //  errors.

    if (! (parameters.containsKey ("indexPath") &&
           parameters.containsKey ("queryFilePath") &&
           parameters.containsKey ("trecEvalOutputPath") &&
           parameters.containsKey ("retrievalAlgorithm"))) {
      throw new IllegalArgumentException
        ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }
  
  // NEED TO COMPLETE
  
  private static String expandQuery(ScoreList scorelist, RetrievalModelIndri model) throws IOException{
      StringBuilder expandedQuery = new StringBuilder("#wand (");
	  Map<String, List<Integer>> term_doc_list = new HashMap<>();
	  Map<String, Double> candidateTermScore = new HashMap<>();
	  
	  scorelist.truncate((int)model.fbDocs);
	  
	  // loop through  docs in scorelist
	  //System.out.println(scorelist.size());
	  //int limit = min(scorelist.size(),(int)model.fbDocs);
	  //System.out.println(limit);
	  for(int i = 0; i < scorelist.size(); i++) {
		  int docid = scorelist.getDocid(i);
		  double docscore = scorelist.getDocidScore(i);
		  // Get the TermVector for this doc
		  TermVector tv = new TermVector(docid, "body");
		  // check 
		  long doc_len = tv.positionsLength();
		  for(int j = 0; j < tv.stemsLength(); j ++) {
			  String term  = tv.stemString(j);
			  if(term == null || term.contains(".") || term.contains(","))
				  continue;
			  if(term_doc_list.containsKey(term)) {
				  //check
				  //term_doc_list.get(term).add(docid);
                  List<Integer> doc_list = term_doc_list.get(term);
                  doc_list.add(docid);
                  term_doc_list.put(term, doc_list);
			  }
			  else {
                  List<Integer> doc_list = new ArrayList<>();
                  doc_list.add(docid);
                  term_doc_list.put(term, doc_list);
			  }
			  double tf = (double)tv.stemFreq(j);
	          double ctf = (double)Idx.getTotalTermFreq("body", term);
	          double collection_len = (double)Idx.getSumOfFieldLengths("body");
	          // check double
	          double mle = ctf / collection_len;
	          double ptd = ( tf + model.fbMu * 1.0 * mle ) / (doc_len + model.fbMu);
	          //System.out.println(ptd);
	          double idf = Math.log(collection_len / ctf);
	          double expansion_score = ptd * idf * docscore;
	          //System.out.println(expansion_score);
	          if(candidateTermScore.containsKey(term)) 
	        	  candidateTermScore.put(term, expansion_score + candidateTermScore.get(term)); 
	          else
	        	  candidateTermScore.put(term, expansion_score); 
		  }
	  }
	  for(String term : candidateTermScore.keySet()) {
		  List<Integer> doc_list = term_doc_list.get(term);
          for (int i = 0; i < scorelist.size(); i++) {
              int docid = scorelist.getDocid(i);
              if(doc_list.contains(docid))
            	  continue;
              double score = scorelist.getDocidScore(i);
              //System.out.println(score);
              double doc_len = Idx.getFieldLength("body", docid);
              //System.out.println(doc_len);
              double ctf = Idx.getTotalTermFreq("body", term);
              //System.out.println(ctf);
              double collection_len = Idx.getSumOfFieldLengths("body");
              //System.out.println(collection_len);
              double mle = ctf / collection_len;
              //System.out.println(mle);
              //System.out.println(model.fbMu);
	          double ptd = ( 0.0 + model.fbMu * 1.0 * mle ) / (doc_len + model.fbMu * 1.0);
	          //System.out.println("EQUALS");
              //System.out.println(ptd);
              double idf =  Math.log(collection_len / ctf);
              double expansion_score = ptd * idf * score;
              //System.out.println(expansion_score);
              candidateTermScore.put(term, expansion_score + candidateTermScore.get(term));
          }  
	  }
	  /*
	  for(String term: candidateTermScore.keySet()) {
		  System.out.println(term);
		  System.out.println(candidateTermScore.get(term));
	  }
	  */
	  List<Map.Entry<String, Double>> list = new ArrayList<>(candidateTermScore.entrySet());
	  list.sort(Map.Entry.comparingByValue());
	  Collections.reverse(list);
	  /*
	  for(String term: candidateTermScore.keySet()) {
		  System.out.println(term);
		  System.out.println(candidateTermScore.get(term));
	  }
	  */
	  
	  int termNum = 0;
	  for (Map.Entry<String, Double> entry : list) {
		  String term = entry.getKey();
		  double score = entry.getValue();
          expandedQuery.append(String.format("%.4f %s ", score, term));
		  termNum += 1;
	      if (termNum >= model.fbTerms)
	        break;
	    }
	  
      expandedQuery.append(")");
      return expandedQuery.toString();

	  
	  
  }
  



}
