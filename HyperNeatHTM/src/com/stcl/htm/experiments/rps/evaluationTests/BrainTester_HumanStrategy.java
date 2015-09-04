package com.stcl.htm.experiments.rps.evaluationTests;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class BrainTester_HumanStrategy extends BrainTester {

	public BrainTester_HumanStrategy(HTMNetwork brain,
			boolean collectGameScores, String outputFolder, int[][] sequences,
			int[][] sequences_changed, Properties props) {
		super(brain, collectGameScores, outputFolder, sequences,
				sequences_changed, props);
	}
	
	protected Test[] setupTesters(Properties props, int[][] sequences, int[][] sequences_changed, boolean collectGameScores, String outputFolder){
		Test[] testers = { new Test_HumanStrategy()};
		
		int[][] mySequences = new int[1][50000];

		for (Test t : testers){
			t.setupTest(props, mySequences, collectGameScores, outputFolder);
		}
		return testers;
	}

}
