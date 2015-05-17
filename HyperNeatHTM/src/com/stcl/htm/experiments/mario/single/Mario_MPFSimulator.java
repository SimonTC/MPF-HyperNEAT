package com.stcl.htm.experiments.mario.single;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import vikrasim.agents.GapAgent;
import vikrasim.agents.MPFAgent;
import vikrasim.agents.MasterAgent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

public class Mario_MPFSimulator {

	static ArrayList<SimpleMatrix> actions;
	private static boolean writeInfo = false;
	private static int counter;

	public static void main(String[] args) throws IOException {
		// Viktors levels
		String flatNoBlock = "-vis off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String flatBlocks = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withCoins = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withGaps = "-vis on -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb off";
		String deadEnds = "-vis off -lb on -lca off -lco on -lde on -le off -lf off -lg on -lhs off -ltb off";
		String withTubes = "-vis on -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb on";
		String withFrozenEnemies = "-vis off -lb on -lca off -lco on -lde on -le on -lf off -lg on -lhs off -ltb on -fc on";
		String everything = "-vis off -lb on -lca on -lco on -lde on -lf off -lg on -lhs on -ltb on";

		// Write parameters to use in simulation
		String learningOptions = flatNoBlock;
		// options = options + " -ls 2 -ld 2 -z on";
		System.out.print(learningOptions);

		String outputFile = "D:/Users/Simon/Documents/Experiments/HTM/mario/No_Evo";
		// Create new agent
		String file = "D:\\Users\\Simon\\Dropbox\\ITU\\AI\\Mario\\Exam\\Org - disabled -4.txt";
		MasterAgent agentGAP = new GapAgent("ThisRocks", file, 1, 1, 7, 7);
		agentGAP.createBrain();
		MPFAgent agentMPF = new MPFAgent("MPF", 1, 1, 7, 7, new Random(1234), true);
		Network_DataCollector agentBrain = agentMPF.getNetwork();
		createActionMatrix();
		loadActionMatrix(agentBrain);
		
		
		if (writeInfo) agentBrain.initializeWriters(outputFile, false);
		if (writeInfo)agentBrain.closeFiles();
		//Run 5 learning rounds
		
		agentBrain.setUsePrediction(true);
		
		System.out.println();
		System.out.println("Model weigths before learning in unit: " + agentBrain.getUnitNodes().get(0).getID());
		
		agentBrain.getUnitNodes().get(0).getUnit().getSpatialPooler().printModelWeigths();
		System.out.println();

		counter = 0;
		learningOptions = learningOptions.replace("-vis off", "-vis on");
		for (int i = 0; i < 100; i++){
			//writeInfo = (i % 10 == 0) ;
			if (writeInfo)agentBrain.openFiles(true);
			System.out.println("Starting learning run " + i);
			int[] results = runLearningRound(agentMPF, learningOptions, agentGAP);
			System.out.println("Distance traveled: " + results[0]);
			agentBrain.newEpisode();
			//agentBrain.newEpisode(calculateInternaleward(10));
			if (writeInfo)agentBrain.closeFiles();
		}
		
		//agentBrain.getActionNode().printActionModels();
		System.out.println();
		System.out.println("Model weigths after learning:");
		agentBrain.getUnitNodes().get(0).getUnit().getSpatialPooler().printModelWeigths();
		System.out.println();
		
		
		agentBrain.setLearning(false);
		agentBrain.getActionNode().setExplorationChance(0);
		learningOptions = learningOptions.replace("-vis off", "-vis on");
		//learningOptions = learningOptions + " -ls 5 -ld 2 -z on";
		int[] results = runNormalRound(agentMPF, learningOptions);
		System.out.println("Evaluation");
		System.out.println("Distance traveled: " + results[0]);
		System.exit(0);
		
		/*
		//learningOptions = learningOptions.replace("-vis on", "-vis off");
		//Run training
		agentBrain.setUsePrediction(true);
		
		int trainingEpisodes = 500;
		for (int i = 0; i < trainingEpisodes; i++){
			int levelSeed = 5;
			if (writeInfo)agentBrain.openFiles(true);
			//learningOptions = learningOptions.replace("-vis off", "-vis on");
			agentBrain.getActionNode().setExplorationChance(1 - (double)i / trainingEpisodes);
			int[] results = runNormalRound(agentMPF, learningOptions + " -ls " + levelSeed + " -ld 2");
			
			//agentBrain.newEpisode(calculateInternaleward(lastReward));
			agentBrain.newEpisode();
			System.out.println("Normal run, " + i);
			System.out.println("Distance traveled: " + results[0]);
			//agentBrain.getUnitNodes().get(0).getUnit().getSpatialPooler().printModelWeigths();
			if (writeInfo)agentBrain.closeFiles();
		}
		
		System.out.println();
		System.out.println("Model weigths after training:");
		agentBrain.getUnitNodes().get(0).getUnit().getSpatialPooler().printModelWeigths();
		System.out.println();
		
		agentBrain.setLearning(false);
		agentBrain.getActionNode().setExplorationChance(0);
		//learningOptions = learningOptions.replace("-vis off", "-vis on");
		learningOptions = learningOptions + " -ls 5 -ld 2 -z on";
		int[] results = runNormalRound(agentMPF, learningOptions);
		System.out.println("Evaluation");
		System.out.println("Distance traveled: " + results[0]);
		System.exit(0);
		*/
		agentBrain.closeFiles();
	}
	
	private static int[] runNormalRound(MPFAgent agent, String levelOptions){
		double stepPoint = 1.0 / 256.0;
		int distanceNow = 0;
		int distanceBefore = 0;
		
		Environment environment = new MarioEnvironment();
		environment.reset(levelOptions);

		boolean[] action = {false,false,false,false,false,false}; //Choose random action
		
		int count = 0;
		boolean[] nextAction = action;
		int[] ev = null;
		do {			
			environment.performAction(action);	
			environment.tick(); // Execute one tick in the game //STC
			if (count % 1 == 0){
				ev = environment.getEvaluationInfoAsInts();
				distanceNow = ev[0];
				double reward = calculateReward(ev, distanceNow, distanceBefore);
				agent.giveReward(reward);
				agent.integrateObservation(environment);
				action = agent.getAction();
				//System.out.println("Distance moved: " + (distanceNow - distanceBefore) + " Reward: " + reward);
				distanceBefore = distanceNow;
			} 
			count++;
			
		} while (!environment.isLevelFinished());
		
		return ev;
	}
	
	private static int[] runLearningRound(MPFAgent agent, String levelOptions, MasterAgent teacher){
		Environment environment = new MarioEnvironment();
		environment.setAgent(teacher);
		environment.reset(levelOptions);		
		
		int distanceNow = 0;
		int distanceBefore = 0;
		boolean[] action = null;
		while (!environment.isLevelFinished()) {	
			environment.tick(); // Execute one tick in the game //STC
			int[] ev = environment.getEvaluationInfoAsInts();
			distanceNow = ev[0];
			double reward = calculateReward(ev, distanceNow, distanceBefore);
			agent.giveReward(reward);
			teacher.integrateObservation(environment);
			agent.integrateObservation(environment);
			agent.getAction();
			action = teacher.getAction();
			agent.setAction(action);
			distanceBefore = distanceNow;
			environment.performAction(action);				
		}
		
		int[] ev = environment.getEvaluationInfoAsInts();
		return ev;
	}
	
	private static double calculateReward(int[] environment, int distanceNow, int distanceBefore){
		double reward = 0;
		/*
		int marioStatus = environment[8];
		if (marioStatus == 0){
			reward = -1; //Dead
		} else if (marioStatus == 1){
			reward = 1; //Won
		*/
		if (distanceNow == 256){
			reward = 10;
		} else {		
			//Give reward based on how far Mario has moved
			double stepPoint = 0.001;// 1.0 / 256.0;
			reward = distanceNow - distanceBefore;
			reward = reward * stepPoint - 0.001;
		}
		return reward;
	}
	
	
	
	/*
	private static int[] runLearningRound(MPFAgent pupil, MasterAgent teacher, String levelOptions){
		Environment environment = new MarioEnvironment();
		environment.reset(levelOptions);
		double stepPoint = 1.0 / 256.0;
		boolean[] action = {false,false,false,false,false,false}; //Choose random action
		int distanceNow = 0;
		int distanceBefore = 0;
		while (!environment.isLevelFinished()) {			
			increaseCount();
			double reward = distanceNow - distanceBefore;
			reward = reward * stepPoint - stepPoint;
			double internalReward = reward - 0.001;//calculateInternaleward(reward);
			pupil.giveReward(internalReward);
			pupil.integrateObservation(environment);
			teacher.integrateObservation(environment);
			action = teacher.getAction();
			pupil.setAction(action);
			pupil.getAction();
			environment.performAction(action);
			environment.tick(); // Execute one tick in the game //STC
			int[] ev = environment.getEvaluationInfoAsInts();
			if (distanceNow > distanceBefore){
				distanceBefore = distanceNow;
			}
			distanceNow = ev[0];							
		}
		int[] ev = environment.getEvaluationInfoAsInts();
		double lastReward = 0;
		int marioStatus = ev[8];
		if (marioStatus == 0) lastReward = -10; //Dead
		if (marioStatus == 1) lastReward = 10; //Won
		if (marioStatus == 2) lastReward = 0; //Time out

		pupil.giveReward(lastReward);
		pupil.integrateObservation(environment);
		pupil.getAction();
	
		return ev;
	}
*/
	
	private static double externalRewardNow, externalRewardBefore, internalRewardBefore;
	private static double calculateInternaleward(double externalReward){
		double maxReward = 100;
		double alpha = 0.1;
		double externalRewardNow = externalReward;
		
		double exponentialWeightedMovingAverage = (externalRewardNow - externalRewardBefore) / maxReward;
		
		double internalReward = alpha * exponentialWeightedMovingAverage + (1-alpha) * internalRewardBefore;
		
		internalRewardBefore = internalReward;
		externalRewardBefore = externalRewardNow;
		
		return internalReward;
		
		//return externalReward;

	}
	
	/**
	 * Create all combinations f data that are possible to perform
	 */
	private static void createActionMatrix(){
		double[][][] actionData = {
				{{1,0,0,1,1,0}},
				{{1,0,0,0,1,0}},
				{{1,0,0,1,0,0}},
				{{1,0,0,0,0,0}},
				{{0,1,0,1,1,0}},
				{{0,1,0,0,1,0}},
				{{0,1,0,1,0,0}},
				{{0,1,0,0,0,0}},
				{{0,0,1,1,1,0}},
				{{0,0,1,0,1,0}},
				{{0,0,1,1,0,0}},
				{{0,0,1,0,0,0}},
				{{0,0,0,1,1,1}},
				{{0,0,0,0,1,1}},
				{{0,0,0,1,0,1}},
				{{0,0,0,0,0,1}}};
		
		actions = new ArrayList<SimpleMatrix>();
		for (double[][] dataMatrix : actionData){
			SimpleMatrix m = new SimpleMatrix(dataMatrix);
			actions.add(m);
		}		
	}
	
	private static void increaseCount(){
		counter++;
		/*
		if(counter == 31000){
			System.out.println();
		}
		*/
	}
	
	protected static void loadActionMatrix(Network brain){
		brain.getActionNode().setPossibleActions(actions);
	}

}
