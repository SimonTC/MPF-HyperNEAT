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

	protected String topFolder;
	protected String[] genomeNames;
	private String genomeTopFolder;
	private int[][] sequences;
	private int[][] sequences_changed;
	private Properties props;

	private boolean collectScores;
	
	private boolean useHumanStrategy = true;
	

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
			 TestSuite ts = new TestSuite(topFolder, genomeFolder, numSequences, props, collectScores);
			 
			 String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
			 System.out.println(timeStamp + ":  Starting test suite");
			 System.out.println();
			 ts.run(whiteList);
			 
			 timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
			 System.out.println(timeStamp + ":  Finished tests");
		 }
		

	}
	

	
	public TestSuite(String topFolder,  String genomeTopFolder, int numSequences, Properties props, boolean collectGameScores) throws IOException{
		this.genomeTopFolder = genomeTopFolder;
		this.props = props;
		int sequenceLevels = props.getIntProperty(RPS_SEQUENCES_LEVELS_KEY);
		int blockLengthMin = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MIN);
		int blockLengthMax = props.getIntProperty(RPS_SEQUENCES_BLOCKLENGTH_MAX);
		
		int[] sequenceProps = {numSequences, sequenceLevels, blockLengthMin, blockLengthMax};
		setupSequences(sequenceProps);
		this.printSequencesToFile(sequences, topFolder + "/sequences.txt");
		this.printSequencesToFile(sequences_changed, topFolder + "/sequences_changed.txt");
		this.topFolder = topFolder;
		this.collectScores = collectGameScores;
	}
	
	public void run(File[] whitelist) throws IOException, InterruptedException{
		File dir = new File(genomeTopFolder);
		File[] genomeDirectories = dir.listFiles();
		
		for (File genome_dir : genomeDirectories){
			if (genome_dir.isDirectory() && isInFileList(genome_dir, whitelist)){
				String genome_name = genome_dir.getName();
				String resultDir = topFolder + "/results/" + genome_name;
				String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
				System.out.println(timeStamp + ":  Starting tests on genomes from '" + genome_name + "'");
				runTests(genome_dir, resultDir, props, collectScores, sequences, sequences_changed);
				System.out.println();
			}
		}
		
	}
	
	private boolean isInFileList(File f, File[] list){
		for (File file : list){
			if (f.equals(file)) return true;
		}
		return false;
	}
	
	
	private void runTests(File genomeDirectory, String resultDirectoryPath, Properties props, boolean collectGameScores, int[][] sequences, int[][] sequences_changed) throws IOException, InterruptedException{
		boolean simpleBrain = genomeDirectory.getName().contains("Simple Network");
		boolean isRandom = genomeDirectory.getName().contains("Random Player");
		String[] genomeFiles = loadGenomeFiles(genomeDirectory.getAbsolutePath());
		Properties brainProperties;
		Properties propsInBrainTester = new Properties(props);
		ArrayList<Network_DataCollector> brains = new ArrayList<Network_DataCollector>();
		
		for (String genomeFile : genomeFiles){
			
				if (genomeFile.contains("props")){
					brainProperties = new Properties(genomeFile);
					propsInBrainTester.setProperty(RPS_EXPLORE_CHANCE, brainProperties.getProperty(RPS_EXPLORE_CHANCE));				
				} else {
					if (!isRandom){
						Network_DataCollector brain = null;
						if (simpleBrain){
							brain = buildSimpleBrain(genomeFile);
						} else {
							brain = buildBrain(genomeFile);
						}
						brains.add(brain);
					} else {
						brains.add(null);
				}
			
			}
		}
		ExecutorService executor = Executors.newCachedThreadPool();
		
		for (int i = 0; i < brains.size(); i++){
			HTMNetwork network = new HTMNetwork(brains.get(i));
			String outputFolder = resultDirectoryPath + "/Gamescores_genome_" + i;				
			BrainTester bt;
			if (useHumanStrategy){
				bt = new BrainTester_HumanStrategy(network, collectGameScores, outputFolder, sequences, sequences_changed, propsInBrainTester);
			} else {
				bt = new BrainTester(network, collectGameScores, outputFolder, sequences, sequences_changed, propsInBrainTester);
			}
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
	
	private  void setupSequences(int[] sequenceprops){
		int alphabetSize = 3; //Cannot be changed currently
		int numSequences = sequenceprops[0];
		int sequenceLevels = sequenceprops[1];
		int blockLengthMin = sequenceprops[2];
		int blockLengthMax = sequenceprops[3];
		
		Random sequenceRand = new Random();
		SequenceBuilder builder = new SequenceBuilder();
		int [][] sequences = new int[numSequences][];
		int [][] sequences_changed = new int[numSequences][];
		for ( int i = 0; i < numSequences; i++){
			int[] sequence = builder.buildSequence(sequenceRand, sequenceLevels, alphabetSize, blockLengthMin, blockLengthMax);
			sequences[i] = copySequence(sequence);
			int[] sequence_changed = builder.randomizeValues();
			sequences_changed[i] = sequence_changed; //No need to copy this since it wont be changed
		}
		
		this.sequences = sequences;
		this.sequences_changed = sequences_changed;
	}
	
	private int[] copySequence(int[] sequence){
		int[] copy = new int[sequence.length];
		
		for (int i = 0; i < copy.length; i++){
			copy[i] = sequence[i];
		}
		
		return copy;	
	}
	
	private void printSequencesToFile(int[][] sequences, String filepath) throws IOException{
		FileWriter fw = new FileWriter(filepath);
		fw.openFile(false);
		for (int seq = 0; seq < sequences.length; seq++){
			String s = "";
			for (int i : sequences[seq]){
				s += i + " ";
			}
			fw.writeLine(s);
		}
		
		fw.closeFile();
	}

}
