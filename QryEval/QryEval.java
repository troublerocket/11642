/*
 *  Copyright (c) 2019, Carnegie Mellon University.  All Rights Reserved.
 *  Version 3.4.
 *  
 *  Compatible with Lucene 8.1.1.
 */
import java.io.*;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;


import org.apache.lucene.index.*;

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
    //if(parameters.get("fb").equals("true"))
    //System.out.println("true");
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
    
    processQueryFile(parameters.get("queryFilePath"), parameters.get("trecEvalOutputPath"),
    		Integer.parseInt(parameters.get("trecEvalOutputLength")), model);

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
    }

    else {

      throw new IllegalArgumentException
        ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));
    }
      
    return model;
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
  
  private static String expandQuery(ScoreList score_list, RetrievalModelIndri model) throws IOException {
      StringBuilder expandedQuery = new StringBuilder("#wand (");

      Map<String, Double> termScores = new HashMap<>();     // stores score for each term t
      Map<String, Double> termMLE = new HashMap<>();        // stores corpus-specific statistic for term t
      Map<String, List<Integer>> invList = new HashMap<>(); // inverted list for term t

      // The initial query Qoriginal retrieves the top-ranked n documents
      // int docSize = Math.min(fbDocs, referenceRanking.size());
      score_list.truncate((int)model.fbDocs);

      // Extract potential expansion terms from top n documents

      // `Idx.getSumOfFieldLengths("body")` is extremely slow, to boost speed,
      // better to cache it one time rather than compute it every time we call `expandQuery`
      double corpusLen = Idx.getSumOfFieldLengths("body");

      for (int i = 0; i < score_list.size(); i++) {
          int docid = score_list.getDocid(i);
          double docScore = score_list.getDocidScore(i);
          double docLen = Idx.getFieldLength("body", docid);

          // Retrieve the term vector for externalID
          TermVector termVector = new TermVector(docid, "body");

          // Calculate a score for each potential expansion term
          // termVector[0] = null: START FROM 1 !!!
          for (int j = 0; j < termVector.stemsLength(); j++) {
              String term = termVector.stemString(j);

              // Ignore any candidate expansion term that contains a period ('.') or a comma (',')
              if (term == null || term.contains(".") || term.contains(",")) 
            	  continue;

              // Update inverted list for the current term
              if (invList.containsKey(term)) {
                  invList.get(term).add(docid);
              } else {
                  List<Integer> docs = new ArrayList<>();
                  docs.add(docid);
                  invList.put(term, docs);
              }

              double tf = termVector.stemFreq(j);

              // Get/Update corpus-specific statistic (p(t|C) for the current term
              double mle;
              
              if (termMLE.containsKey(term)) {
                  mle = termMLE.get(term);
              } 
              else {
                  double ctf = termVector.totalStemFreq(j);
                  mle = ctf / corpusLen;
                  termMLE.put(term, mle);
              }

              double score = (tf + model.fbMu * mle) / (docLen + model.fbMu);
              double idf = Math.log(1. / mle);
              double weightedScore = score * docScore * idf;

              termScores.put(term, termScores.getOrDefault(term, 0d) + weightedScore);
          }
      }

      /*
       * Now we get m expansion terms from top n docs
       * However, the term score for these m terms is not complete yet
       * because some of the top n docs (referenceRanking) might not contain all the m terms
       * but they still need to contribute to the final term score
       * Therefore, we need to update term score on docs whose tf = 0
       * */

      for (String term : termScores.keySet()) {
          List<Integer> docs = invList.get(term);
          for (int i = 0; i < score_list.size(); i++) {
              int docid = score_list.getDocid(i);
              if (!docs.contains(docid)) {
                  double docScore = score_list.getDocidScore(i);
                  double docLen = Idx.getFieldLength("body", docid);

                  double mle = termMLE.get(term);
                  double score = (0 + model.fbMu * mle) / (docLen + model.fbMu);
                  double idf = Math.log(1. / mle);
                  double weightedScore = score * docScore * idf;

                  termScores.put(term, termScores.get(term) + weightedScore);
              }
          }
      }

      // Sort the term scores by score value, leaving only m top terms in the map
      termScores = MapUtil.sortByDescValue(termScores, (int)model.fbTerms);

      // Use the top m terms to create an expansion query Qlearned
      for (Map.Entry<String, Double> entry : termScores.entrySet()) {
          String term = entry.getKey();
          double score = entry.getValue();
          expandedQuery.append(String.format("%.4f %s ", score, term));
      }

      expandedQuery.append(")");
      return expandedQuery.toString();
	  
  }


}
