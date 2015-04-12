package com.stcl.htm.experiments.mario;

import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import vikrasim.agents.MPFAgent;
import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class MarioFitnessFunction_Incremental extends HyperNEATFitnessFunction {

	private Random rand;
	private int numLevels = 10; //TODO: Take from parameter
	private int difficulty = 2; //TODO: Should grow the better the agents are
	private int numTrainingLevels = 10;
	private int numEvaluationLevels = 5;
	private int levelLength = 256;
	private String agentName = "Scanner";
	private ArrayList<SimpleMatrix> actions;
	private ArrayList<String[]> levels;

	/**
	 * See <a href=" {@docRoot} /params.htm" target="anji_params">Parameter Details </a> for specific property settings.
	 * 
	 * @param props configuration parameters
	 */
	public void init(Properties props) {
		super.init(props);
		Randomizer randomizer = new Randomizer();
		randomizer.init(props);
		rand = randomizer.getRand();
	}
	
	@Override
	public void initialiseEvaluation() {
		createTrainingSet();
		createActionMatrix();
	}
	
	private void createTrainingSet(){
		String flatNoBlock = "-vis off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String flatBlocks = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withCoins = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withGaps = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb off";
		String deadEnds = "-vis off -lb on -lca off -lco on -lde on -le off -lf off -lg on -lhs off -ltb off";
		String withTubes = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb on";
		String withFrozenEnemies = "-vis off -lb on -lca off -lco on -lde on -le on -lf off -lg on -lhs off -ltb on -fc on";
		String everything = "-vis off -lb on -lca on -lco on -lde on -lf off -lg on -lhs on -ltb on";
		
		levels = new ArrayList<String[]>();
		levels.add(createLevels(flatNoBlock));
		levels.add(createLevels(flatBlocks));
		//levels.add(createLevels(withCoins));
		levels.add(createLevels(withGaps));
		//levels.add(createLevels(deadEnds));
		//levels.add(createLevels(withTubes));
		//levels.add(createLevels(withFrozenEnemies));
		//levels.add(createLevels(everything));

	}
	
	private String[] createLevels(String base){
		String[] levelParameters = new String[numLevels];
		String s = base + " -ll " + levelLength;
		for (int i = 0; i < numLevels; i++){
			 String param = s + " -ls " + rand.nextInt(100);
			levelParameters[i] = param;
		}
		return levelParameters;
	}
	
	/**
	 * Create all combinations f data that are possible to perform
	 */
	private void createActionMatrix(){
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
	
	private void loadActionMatrix(HTMNetwork brain){
		brain.getNetwork().getActionNode().setPossibleActions(actions);
	}
	
	protected double evaluate(Chromosome genotype, Activator activator, int threadIndex) {
		HTMNetwork brain = (HTMNetwork) activator;
		
		loadActionMatrix(brain);
		
		ScannerAgent agent = new ScannerAgent("Scanner", brain, 1, 1, 7, 7);
		
		boolean hasLearnedLevel = true;
		double totalFitness = 0;
		for (int leveltype = 0; leveltype < levels.size() && hasLearnedLevel; leveltype++){
			String[] levelParameters = levels.get(leveltype);
			hasLearnedLevel = false;
		
			//Training
			brain.reset();
			brain.getNetwork().setLearning(true);
			brain.getNetwork().getActionNode().setExplorationChance(0.05);
			for (int level = 0; level < numTrainingLevels; level++){
				String levelParams = levelParameters[rand.nextInt(levelParameters.length)];
				runNormalRound(agent, levelParams);
			}
			
			//Evaluation
			brain.reset();
			brain.getNetwork().getActionNode().setExplorationChance(0.0);
			brain.getNetwork().setLearning(false);
			int travelDistance = 0;
			for (int level = 0; level < numEvaluationLevels; level++){
				String levelParams = levelParameters[rand.nextInt(levelParameters.length)];
				int[] ev = runNormalRound(agent, levelParams);
				travelDistance += ev[0];
			}
			
			double fitness = travelDistance / (double) numEvaluationLevels;
			fitness = fitness / (double) levelLength; 
			totalFitness += fitness;
			
			if (equals(fitness, 1)) hasLearnedLevel = true;			
		}
		totalFitness = totalFitness / (double) levels.size();
		genotype.setPerformanceValue(totalFitness);
		return totalFitness;
	}
	
	private boolean equals(double a, double b){
		double e = 0.0001;
		if (a < b - e) return false;
		if (a > b + e) return false;
		return true;
	}
	
	private int[] runNormalRound(ScannerAgent agent, String levelOptions){
		Environment environment = new MarioEnvironment();
		environment.setAgent(agent);
		environment.reset(levelOptions);
		
		
		int distanceNow = 0;
		int distanceBefore = 0;
		boolean[] action = null;
		while (!environment.isLevelFinished()) {			
			environment.tick(); // Execute one tick in the game //STC
			int[] ev = environment.getEvaluationInfoAsInts();
			distanceNow = ev[0];
			double reward = distanceNow - distanceBefore;
			reward = reward - 0.5; //Punish it for not moving
			agent.giveReward(reward);
			agent.integrateObservation(environment);
			action = agent.getAction();
			distanceBefore = distanceNow;
			environment.performAction(action);				
		}
		
		int[] ev = environment.getEvaluationInfoAsInts();
		return ev;
	}

}
