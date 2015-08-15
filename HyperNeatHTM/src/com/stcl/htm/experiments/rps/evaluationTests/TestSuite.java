package com.stcl.htm.experiments.rps.evaluationTests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import stcl.algo.brain.Network_DataCollector;
import stcl.algo.util.FileWriter;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;
import com.stcl.htm.network.HTMNetwork;
import com.thoughtworks.xstream.security.ExplicitTypePermission;

public class TestSuite implements Runnable{
	
	public static final String RPS_SEQUENCES_LEVELS_KEY = "rps.sequences.levels";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MIN = "rps.sequences.blocklength.min";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MAX = "rps.sequences.blocklength.max";
	public static final String RPS_SEQUENCES_ALPHABET_SIZE = "rps.sequences.alphabet.size";
	public static final String RPS_EXPLORE_CHANCE = "rps.training.explore.chance";
	
	private Test[] testers;
	private String[] genomeFilePaths;
	protected String testFolder;
	protected String[] genomeNames;
	

	private double explorationChance;
	private boolean collectScores;
	

	public static void main(String[] args)  {
		String topFolder = args[0];
		int numSequences = Integer.parseInt(args[1]);
		
		 File dir = new File(topFolder);
		 File[] directoryListing = dir.listFiles();
		 boolean collectScores = Boolean.parseBoolean(args[2]);
		 File[] whiteList = null;
		 if (args.length > 3){
			 whiteList = new File[args.length - 3];
		 }
		 if (whiteList != null){
			 for (int i = 0; i < whiteList.length; i++){
				 File f = new File(topFolder + "/" + args[i+3]);
				 whiteList[i] = f;
			 }
		 } else {
			 whiteList = directoryListing;
		 }
		 		 
		 if (whiteList.length == 0){
			 System.out.println("No files found in directory " + dir.getAbsolutePath());
		 } else {
		 
			 ExecutorService executor = Executors.newFixedThreadPool(4);
			 try {
				 for (int i = 0; i < whiteList.length; i++){
					  File f = whiteList[i];
					  if (f.isDirectory()){
						  String path = f.getAbsolutePath() + "/evaluation";
						  TestSuite ts = new TestSuite(path, numSequences, collectScores);
							executor.execute(ts);
					  }
				  }
				 
				 executor.shutdown();
				 
				 executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
			 	} catch (IOException | InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			 
			 
			
			 System.out.println("Finished tests");
		 }
		

	}
	

	
	public TestSuite(String testFolder,  int numSequences, boolean collectGameScores) throws IOException{
		String propertiesFile = testFolder + "/props.properties";
		Properties props = new Properties(propertiesFile);
		explorationChance = props.getDoubleProperty(RPS_EXPLORE_CHANCE);
		testers = setupTesters(props, numSequences);
		loadGenomeFiles(testFolder + "/genomes");		
		this.testFolder = testFolder;
		this.collectScores = collectGameScores;
	}
	
	@Override
	public void run(){
		try {
			double[][][] results = this.runTest();
			this.writeResults(results, testFolder + "/results");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
		System.out.println(timeStamp + ":  Finished tests in " + testFolder);
	}
	
	
	
	private double[][][] runTest() throws FileNotFoundException{
		double[][][] results = new double[genomeFilePaths.length][][];
		for (int i = 0; i < genomeFilePaths.length; i++){
			String genomeFolder = testFolder + "/results/GameScores_genome_" + i;
			String genome = genomeFilePaths[i];
			Network_DataCollector brain = buildBrain(genome);
			HTMNetwork network = new HTMNetwork(brain);
			results[i] = runTests(network, explorationChance, collectScores, genomeFolder);
		}
		
		return results;		
	}
	
	protected Network_DataCollector buildBrain(String genomeFile){
		Network_DataCollector brain = new Network_DataCollector();
		brain.initialize(genomeFile, new Random());
		return brain;
	}
	
	private void writeResults(double[][][] results, String resultFolder) throws IOException{
		String headers = "Seq, Fitness, Prediction, Speed_Prediction, Adaption";
		for (int i = 0; i < results.length; i++){
			String name = genomeNames[i] +  "_results.csv";
			FileWriter writer = new FileWriter(resultFolder + "/" + name);
			writer.openFile(false);
			writer.writeLine(headers);
			for (int sequence = 0; sequence < results[i][0].length; sequence++){
				writer.write(sequence + ",");
				for (int test = 0; test < results[i].length; test++){
					writer.write(results[i][test][sequence] + ",");
				}
				writer.writeLine("");
			}
			
			writer.closeFile();
			
		}
	}
	
	private void loadGenomeFiles(String parentDirectory){
		  File dir = new File(parentDirectory);
		  File[] directoryListing = dir.listFiles();
		  genomeFilePaths = new String[directoryListing.length];
		  genomeNames = new String[directoryListing.length];
		  for (int i = 0; i < directoryListing.length; i++){
			  File child = directoryListing[i];
			  genomeFilePaths[i] = child.getAbsolutePath();
			  String filename = child.getName();
			  genomeNames[i] = filename.substring(0, filename.length() - 4);
		  }

	}
	
	private double[][] runTests(HTMNetwork brain, double explorationChance, boolean collectGameScores, String mainFolder){
		double[][] scores = new double[testers.length][];
		
		for (int i = 0; i < testers.length; i++){
			Test t = testers[i];
			String testfolder = mainFolder + "/" + t.getName();
			File f = new File(testfolder);
			f.mkdirs();
			scores[i] = t.test(brain, explorationChance, collectGameScores, testfolder);
		}
		return scores;
	}
	
	private Test[] setupTesters(Properties props, int numSequences){
		Test[] testers = { new Test_Prediction()};
		//Test[] testers = {new Test_Fitness(), new Test_Prediction(), new Test_Speed_Fitness(), new Test_Speed_Prediction(), new Test_Adaption()};
		//Test[] testers = {new Test_Fitness(), new Test_Prediction(), new Test_Speed_Prediction(), new Test_Adaption()};
		int[][] sequences = setupSequences(props, numSequences);
		for (Test t : testers){
			t.setupTest(props, sequences);
		}
		return testers;
	}
	
	private  int[][] setupSequences(Properties props, int numSequences){
		int sequenceLevels = props.getIntProperty(RPS_SEQUENCES_LEVELS_KEY);
		int blockLengthMin = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MIN);
		int blockLengthMax = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MAX);
		int alphabetSize = props.getIntProperty(RPS_SEQUENCES_ALPHABET_SIZE, 3); //Currently not in use
		
		Random sequenceRand = new Random();
		SequenceBuilder builder = new SequenceBuilder();
		int [][] sequences = new int[numSequences][];
		for ( int i = 0; i < numSequences; i++){
			sequences[i] = builder.buildSequence(sequenceRand, sequenceLevels, alphabetSize, blockLengthMin, blockLengthMax);
		}
		return sequences;
	}

}
