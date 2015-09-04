package com.stcl.htm.experiments.rps.evaluationTests;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;

import stcl.algo.util.FileWriter;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class BrainTester implements Runnable{
	private Test[] testers;
	private HTMNetwork brain;
	private boolean collectGameScores;
	private String outputFolder;
	private double[][][] scores;
	private double explorationChance;
	
	public BrainTester(HTMNetwork brain, boolean collectGameScores, String outputFolder, int[][] sequences, int[][] sequences_changed, Properties props) {
		this.brain = brain;
		testers = setupTesters(props, sequences, sequences_changed, collectGameScores, outputFolder);
		this.explorationChance = props.getDoubleProperty(TestSuite.RPS_EXPLORE_CHANCE);
		this.outputFolder = outputFolder;
		this.collectGameScores = collectGameScores;
	}
	
	@Override
	public void run() {

		scores = new double[testers.length][][];
		
		for (int i = 0; i < testers.length; i++){
			Test t = testers[i];
			String testfolder = outputFolder + "/" + t.getName();
			File f = new File(testfolder);
			f.mkdirs();
			scores[i] = t.test(brain, explorationChance, collectGameScores, testfolder);
		}
		try {
			writeResults(scores, outputFolder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new java.util.Date());
		System.out.println(timeStamp + ":  Finished tests for " + outputFolder);
		
	}
	
	private void writeResults(double[][][] results, String resultFolder) throws IOException{
		String headers = "Seq, Normal_prediction, Normal_fitness, Adaption_rules_prediction, Adaption_rules_fitness, Adaption_sequence_prediction, Adaption_sequence_fitness";
		String name = "_results.csv";
		FileWriter writer = new FileWriter(resultFolder + "/" + name);
		writer.openFile(false);
		writer.writeLine(headers);
		int numSequences = results[0].length;
		for (int sequence = 0; sequence < numSequences; sequence++){
			String s = sequence + ",";
			for (int test = 0; test < results.length; test++){
				for (int i = 0; i < 2; i++){
					s += results[test][sequence][i] + ",";
				}
			}
			s = s.substring(0, s.length()-1); //Remove last comma
			writer.writeLine(s);
		}
		
		writer.closeFile();
	}
	
	protected Test[] setupTesters(Properties props, int[][] sequences, int[][] sequences_changed, boolean collectGameScores, String outputFolder){
		Test[] testers = { new Test_Normal(), new Test_Adaption(sequences_changed, false), new Test_Adaption(sequences_changed, true)};
		//Test[] testers = {new Test_Fitness(), new Test_Prediction(), new Test_Speed_Fitness(), new Test_Speed_Prediction(), new Test_Adaption()};
		//Test[] testers = {new Test_Fitness(), new Test_Prediction(), new Test_Speed_Prediction(), new Test_Adaption()};
		for (Test t : testers){
			t.setupTest(props, sequences, collectGameScores, outputFolder);
		}
		return testers;
	}
}
