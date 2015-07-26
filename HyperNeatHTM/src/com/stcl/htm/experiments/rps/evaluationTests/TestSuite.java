package com.stcl.htm.experiments.rps.evaluationTests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Random;

import stcl.algo.brain.Network_DataCollector;
import stcl.algo.util.FileWriter;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.sequencecreation.SequenceBuilder;
import com.stcl.htm.network.HTMNetwork;

public class TestSuite {
	
	public static final String RPS_SEQUENCES_LEVELS_KEY = "rps.sequences.levels";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MIN = "rps.sequences.blocklength.min";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MAX = "rps.sequences.blocklength.max";
	public static final String RPS_SEQUENCES_ALPHABET_SIZE = "rps.sequences.alphabet.size";
	
	private Test[] testers;
	private File[] genomeFiles;
	private String[] genomeFilePaths;
	

	public static void main(String[] args) {
		String testFolder = args[0];
		int numSequences = Integer.parseInt(args[1]);
		String propertiesFile = testFolder + "/props.properties";
		
		try {
			TestSuite ts = new TestSuite(testFolder, propertiesFile, numSequences);
			double[][][] results = ts.run();
			ts.writeResults(results, testFolder + "/Results");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
	
	public TestSuite(String testFolder, String propertiesFile, int numSequences) throws IOException{
		Properties props = new Properties(propertiesFile);
		testers = setupTesters(props, numSequences);
		loadGenomeFiles(testFolder + "/Genomes");		
	}
	
	public double[][][] run() throws FileNotFoundException{
		double[][][] results = new double[genomeFilePaths.length][][];
		
		for (int i = 0; i < genomeFilePaths.length; i++){
			String genome = genomeFilePaths[i];
			Network_DataCollector brain = new Network_DataCollector(genome, new Random());
			HTMNetwork network = new HTMNetwork(brain);
			results[i] = runTests(network);
		}
		
		return results;		
	}
	
	public void writeResults(double[][][] results, String resultFolder) throws IOException{
		String headers = "Seq, Fitness, Prediction, Speed_Fitness, Speed_Prediction, Adaption";
		for (int i = 0; i < results.length; i++){
			String filename = genomeFiles[i].getName();
			filename = filename.substring(0, filename.length() - 4) + "_results.csv";
			FileWriter writer = new FileWriter(resultFolder + "/" + filename);
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
		  genomeFiles = directoryListing;
		  genomeFilePaths = new String[directoryListing.length];
		  for (int i = 0; i < directoryListing.length; i++){
			  File child = directoryListing[i];
			  genomeFilePaths[i] = child.getAbsolutePath();
		  }

	}
	
	private double[][] runTests(HTMNetwork brain){
		double[][] scores = new double[testers.length][];
		
		for (int i = 0; i < testers.length; i++){
			Test t = testers[i];
			scores[i] = t.test(brain);
		}
		return scores;
	}
	
	private Test[] setupTesters(Properties props, int numSequences){
		Test[] testers = {new Test_Fitness(), new Test_Prediction(), new Test_Speed_Fitness(), new Test_Speed_Prediction(), new Test_Adaption()};
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
