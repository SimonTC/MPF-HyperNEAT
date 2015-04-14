package com.stcl.htm.experiments.mario;

import java.util.ArrayList;
import java.util.Random;

import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.Chromosome;

import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.anji.integration.Activator;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.evaluation.HyperNEATFitnessFunction;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

public class MarioFitnessFunction_Incremental extends HyperNEATFitnessFunction {
	private static final String LEVEL_RAND_KEY = "level.rand.key";
	private static final String LEVEL_NUM_TRAINING = "level.num.training";
	private static final String LEVEL_NUM_EVALUATION = "level.num.evaluation";
	private static final String LEVEL_DIFFICULTY = "level.difficulty";
	private static Logger logger = Logger.getLogger(MarioFitnessFunction_Incremental.class);
	private static final long serialVersionUID = 1L;
	private Random rand;
	private int difficulty;
	private int numTrainingLevels;
	private int numEvaluationLevels;
	private int levelLength = 256;
	private String agentName = "Scanner";
	private ArrayList<SimpleMatrix> actions;
	protected ArrayList<String[]> trainingSet, evaluationSet;

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
		long levelRandSeed = 0;
		try{
			levelRandSeed = props.getLongProperty(LEVEL_RAND_KEY);
		} catch (IllegalArgumentException e){
			levelRandSeed = rand.nextLong();
			props.setProperty(LEVEL_RAND_KEY, "" +levelRandSeed);
		}
		
		difficulty = props.getIntProperty(LEVEL_DIFFICULTY, 2);
		numTrainingLevels = props.getIntProperty(LEVEL_NUM_TRAINING, 10);
		numEvaluationLevels = props.getIntProperty(LEVEL_NUM_EVALUATION,5);
		
		Random levelRand = new Random(levelRandSeed);
		trainingSet = createLevelSet(levelRand, numTrainingLevels);
		evaluationSet = createLevelSet(levelRand, numEvaluationLevels);
		createActionMatrix();
	}
	
	protected ArrayList<String[]> createLevelSet(Random levelRand, int numLevels){
		ArrayList<String[]> set = new ArrayList<String[]>();
		String flatNoBlock = "-vis off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String flatBlocks = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withCoins = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg off -lhs off -ltb off";
		String withGaps = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb off";
		String deadEnds = "-vis off -lb on -lca off -lco on -lde on -le off -lf off -lg on -lhs off -ltb off";
		String withTubes = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb on";
		String withFrozenEnemies = "-vis off -lb on -lca off -lco on -lde on -le on -lf off -lg on -lhs off -ltb on -fc on";
		String everything = "-vis off -lb on -lca on -lco on -lde on -lf off -lg on -lhs on -ltb on";
		
		set = new ArrayList<String[]>();
		set.add(createLevels(flatNoBlock,levelRand, numLevels));
		set.add(createLevels(flatBlocks,levelRand, numLevels));
		//set.add(createLevels(withCoins,levelRand, numLevels));
		set.add(createLevels(withGaps,levelRand, numLevels));
		//set.add(createLevels(deadEnds,levelRand, numLevels));
		//set.add(createLevels(withTubes,levelRand, numLevels));
		//set.add(createLevels(withFrozenEnemies,levelRand, numLevels));
		//set.add(createLevels(everything,levelRand, numLevels));
		return set;

	}
	
	/**
	 * 
	 * @param base
	 * @param levelRand
	 * @return
	 */
	protected String[] createLevels(String base, Random levelRand, int numLevels){
		String[] levelParameters = new String[numLevels];
		String s = base + " -ll " + levelLength;
		for (int i = 0; i < numLevels; i++){
			 String param = s + " -ls " + levelRand.nextInt(Integer.MAX_VALUE) + " -ld " + difficulty;
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
		
		logger.debug("Start evaluation on thread " + threadIndex);
		long start_time = System.currentTimeMillis();
		
		HTMNetwork brain = (HTMNetwork) activator;
		
		MPFAgent agent = createAgent(brain);
		
		double fitness = runEvaluation(agent, false, threadIndex);
		if (genotype != null) genotype.setPerformanceValue(fitness);
		
		long end_time = System.currentTimeMillis();
		
		logger.debug("End evaluation on thread " + threadIndex + " Runtime: " + (end_time - start_time));
		return fitness;
	}
	
	protected MPFAgent createAgent(HTMNetwork brain){
		MPFAgent agent = new ScannerAgent("MPF Agent", brain, 1, 1, 7, 7);
		//MPFAgent agent = new EnvironmentAgent("Environment", brain,2,2);
		return agent;
	}
	
	protected double runEvaluation(MPFAgent agent, boolean printInfo, int threadIndex){
		HTMNetwork brain = agent.brain;
		loadActionMatrix(brain);
		
		boolean hasLearnedLevel = true;
		double totalFitness = 0;
		for (int leveltype = 0; leveltype < trainingSet.size() && hasLearnedLevel; leveltype++){
			hasLearnedLevel = false;
		
			//Training
			brain.reset();
			brain.getNetwork().setLearning(true);
			brain.getNetwork().getActionNode().setExplorationChance(0.05);
			for (int level = 0; level < numTrainingLevels; level++){
				String levelParams = trainingSet.get(leveltype)[level];
				runNormalRound(agent, levelParams);
			}
			
			//Evaluation
			brain.reset();
			brain.getNetwork().getActionNode().setExplorationChance(0.0);
			brain.getNetwork().setLearning(false);
			int travelDistance = 0;
			for (int level = 0; level < numEvaluationLevels; level++){
				String levelParams = evaluationSet.get(leveltype)[level];
				int[] ev = runNormalRound(agent, levelParams);
				travelDistance += ev[0];
			}
			
			double fitness = travelDistance / (double) numEvaluationLevels;
			fitness = fitness / (double) levelLength; 
			totalFitness += fitness;
			
			if (equals(fitness, 1)) hasLearnedLevel = true;			
		}
		totalFitness = totalFitness / (double) trainingSet.size();
		return totalFitness;
	}
		
	private boolean equals(double a, double b){
		double e = 0.0001;
		if (a < b - e) return false;
		if (a > b + e) return false;
		return true;
	}
	
	protected int[] runNormalRound(MPFAgent agent, String levelOptions){
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
