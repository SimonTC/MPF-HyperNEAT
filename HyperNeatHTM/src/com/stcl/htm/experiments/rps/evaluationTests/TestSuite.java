package com.stcl.htm.experiments.rps.evaluationTests;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

public class TestSuite {
	
	public static final String RPS_SEQUENCES_LEVELS_KEY = "rps.sequences.levels";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MIN = "rps.sequences.blocklength.min";
	public static final String RPS_SEQUENCES_BLOCKLENGTH_MAX = "rps.sequences.blocklength.max";
	public static final String RPS_SEQUENCES_ALPHABET_SIZE = "rps.sequences.alphabet.size";
	public static final String RPS_EXPLORE_CHANCE = "rps.training.explore.chance";
	
	public static final String RPS_EVALUATION_ITERATIONS_KEY = "rps.evaluation.iterations";
	public static final String RPS_TRAINING_ITERATIONS_KEY = "rps.training.iterations";
	public static final String RPS_SEQUENCES_ITERATIONS_KEY = "rps.sequences.iterations";
	public static final String RPS_NOISE_MAGNITUDE = "rps.noise.magnitude";

	protected String testFolder;
	protected String[] genomeNames;
	private String genomeTopFolder;
	private int[][] sequences;
	private Properties props;

	private boolean collectScores;
	

	public static void main(String[] args) throws IOException, InterruptedException  {
		String topFolder = args[0];
		int numSequences = Integer.parseInt(args[1]);
		boolean collectScores = Boolean.parseBoolean(args[2]);
		
		String genomeFolder = topFolder + "/genomes";
		
		 File dir = new File(genomeFolder);
		 File[] directoryListing = dir.listFiles();
		 
		 File[] whiteList = null;
		 if (args.length > 3){
			 whiteList = new File[args.length - 3];
		 }
		 if (whiteList != null){
			 for (int i = 0; i < whiteList.length; i++){
				 File f = new File(genomeFolder + "/" + args[i+3]);
				 whiteList[i] = f;
			 }
		 } else {
			 whiteList = directoryListing;
		 }
		 		 
		 if (whiteList.length == 0){
			 System.out.println("No genome files found in directory " + dir.getAbsolutePath());
		 } else {
			 Properties props = new Properties(topFolder + "/props.properties");
			 TestSuite ts = new TestSuite(topFolder, genomeFolder, numSequences, props.getIntProperty(RPS_SEQUENCES_LEVELS_KEY), props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MIN), props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MAX), collectScores);
			 ts.run(whiteList);
			
			 System.out.println("Finished tests");
		 }
		

	}
	

	
	public TestSuite(String testFolder,  String genomeTopFolder, int numSequences, int sequenceLevels, int blockLengthMin, int blockLengthMax, boolean collectGameScores) throws IOException{
		this.genomeTopFolder = genomeTopFolder;
		props = createExpProperties();
		int[] sequenceProps = {numSequences, sequenceLevels, blockLengthMin, blockLengthMax};
		sequences = setupSequences(sequenceProps);
		this.testFolder = testFolder;
		this.collectScores = collectGameScores;
	}
	
	private Properties createExpProperties(){
		Properties props = new Properties();
		props.setProperty(RPS_NOISE_MAGNITUDE, "0.1");
		props.setProperty(RPS_SEQUENCES_ITERATIONS_KEY, "5");
		props.setProperty(RPS_TRAINING_ITERATIONS_KEY, "1000");
		props.setProperty(RPS_EVALUATION_ITERATIONS_KEY, "100");
		
		return props;
		
	}
	
	public void run(File[] whitelist) throws IOException, InterruptedException{
		File dir = new File(genomeTopFolder);
		File[] genomeDirectories = dir.listFiles();
		
		for (File genome_dir : genomeDirectories){
			if (genome_dir.isDirectory() && isInFileList(genome_dir, whitelist)){
				runTests(genome_dir, props, collectScores, sequences);
			}
		}
		
	}
	
	private boolean isInFileList(File f, File[] list){
		for (File file : list){
			if (f.equals(file)) return true;
		}
		return false;
	}
	
	
	private void runTests(File genomeDirectory, Properties props, boolean collectGameScores, int[][] sequences) throws IOException, InterruptedException{
		boolean simpleBrain = genomeDirectory.getName().contains("Simple Network");
		String[] genomeFiles = loadGenomeFiles(genomeDirectory.getAbsolutePath());
		Properties brainProperties;
		Properties propsInBrainTester = new Properties(props);
		ArrayList<Network_DataCollector> brains = new ArrayList<Network_DataCollector>();
		
		for (String genomeFile : genomeFiles){
			if (genomeFile.contains("props")){
				brainProperties = new Properties(genomeFile);
				propsInBrainTester.setProperty(RPS_EXPLORE_CHANCE, brainProperties.getProperty(RPS_EXPLORE_CHANCE));				
			} else {
				Network_DataCollector brain = null;
				if (simpleBrain){
					brain = buildSimpleBrain(genomeFile);
				} else {
					brain = buildBrain(genomeFile);
				}
				brains.add(brain);
			}
		}
		ExecutorService executor = Executors.newCachedThreadPool();
		
		for (int i = 0; i < brains.size(); i++){
			HTMNetwork network = new HTMNetwork(brains.get(i));
			String outputFolder = genomeDirectory.getAbsolutePath() + "/Gamescores_genome_" + i;
			
			BrainTester bt = new BrainTester(network, collectGameScores, outputFolder, sequences, propsInBrainTester);
			executor.execute(bt);
		}
		
		executor.shutdown();
		 
		executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
		
	}
	
	
	private Network_DataCollector buildSimpleBrain(String genomeFile){
		Network_DataCollector brain = null;
		try {
			brain = new Network_DataCollector(genomeFile, new Random());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return brain;
	}
	
	private Network_DataCollector buildBrain(String genomeFile){
		Network_DataCollector brain = new Network_DataCollector();
		brain.initialize(genomeFile, new Random(), true);
		return brain;
	}
	
	private String[] loadGenomeFiles(String parentDirectory){
		  File dir = new File(parentDirectory);
		  File[] directoryListing = dir.listFiles();
		  String[] genomeFilePaths = new String[directoryListing.length];
		  for (int i = 0; i < directoryListing.length; i++){
			  File child = directoryListing[i];
			  genomeFilePaths[i] = child.getAbsolutePath();
		  }
		  return genomeFilePaths;

	}
	
	private  int[][] setupSequences(int[] sequenceprops){
		int alphabetSize = 3; //Cannot be changed currently
		int numSequences = sequenceprops[0];
		int sequenceLevels = sequenceprops[1];
		int blockLengthMin = sequenceprops[2];
		int blockLengthMax = sequenceprops[3];
		
		Random sequenceRand = new Random();
		SequenceBuilder builder = new SequenceBuilder();
		int [][] sequences = new int[numSequences][];
		for ( int i = 0; i < numSequences; i++){
			sequences[i] = builder.buildSequence(sequenceRand, sequenceLevels, alphabetSize, blockLengthMin, blockLengthMax);
		}
		return sequences;
	}

}
