package com.stcl.htm.experiments.rps;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction;
import com.stcl.htm.experiments.rps.rewardfunctions.RewardFunction_Standard;

public class RPSFitnessFunction_Speed extends RPSFitnessFunction_HTM {
	public static final String RPS_SPEED_PREDICTION_THRESHOLD_KEY = "rps.speed.predictionthreshold";
	public static final String RPS_SPEED_FITNESS_THRESHOLD_KEY = "rps.speed.fitnessthreshold";
	public static final String RPS_SPEED_AVERAGEOVER_KEY = "rps.speed.averageover";
	private double predictionThreshold, fitnessThreshold;
	private int averageOver;
	
	public void init(Properties props) {
		super.init(props);
		predictionThreshold = props.getDoubleProperty(RPS_SPEED_PREDICTION_THRESHOLD_KEY, 0.9);
		fitnessThreshold = props.getDoubleProperty(RPS_SPEED_FITNESS_THRESHOLD_KEY, 0.9);
		averageOver = props.getIntProperty(RPS_SPEED_AVERAGEOVER_KEY, 5);
	}
	
	@Override
	protected RPS setupEvaluator(){
		RewardFunction[] functions = {new RewardFunction_Standard()};
		RPS_Speed eval = new RPS_Speed(possibleInputs, sequences, functions, numExperimentsPerSequence, trainingIterations, evaluationIterations, predictionThreshold, fitnessThreshold, averageOver);
		return eval;
	}

}
