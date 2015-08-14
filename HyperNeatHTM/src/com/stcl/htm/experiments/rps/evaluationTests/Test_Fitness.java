package com.stcl.htm.experiments.rps.evaluationTests;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class Test_Fitness extends Test_Prediction {

	@Override
	public double[] test(HTMNetwork brain, double explorationChance, boolean collectGameScores, String scoreFolderName) {
		double[] results = new double[numSequences];
		evaluator.run(brain, explorationChance, collectGameScores, scoreFolderName);
		double[][] sequenceScores = evaluator.getSequenceScores();
		for (int i = 0; i < numSequences; i++){
			results[i] = sequenceScores[i][1];
		}
		return results;
	}
	
	
	protected void setName(){
		this.testName = "Fitness";
	}

}
