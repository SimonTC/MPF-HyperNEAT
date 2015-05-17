package com.stcl.htm.experiments.mario.single;

import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import ch.idsia.benchmark.mario.environments.Environment;
import ch.idsia.benchmark.mario.environments.MarioEnvironment;

import com.stcl.htm.experiments.mario.MPFAgent;
import com.stcl.htm.experiments.mario.ScannerAgent;
import com.stcl.htm.network.HTMNetwork;

import stcl.algo.brain.Network;
import stcl.algo.brain.Network_DataCollector;
import stcl.algo.brain.nodes.ActionNode;
import stcl.algo.brain.nodes.Sensor;
import stcl.algo.brain.nodes.UnitNode;
import vikrasim.agents.GapAgent;
import vikrasim.agents.MasterAgent;

public class Mario_Reactionary {
	
	private final String outputFile = "D:/Users/Simon/Documents/Experiments/HTM/mario/No_Evo";
	private Network_DataCollector agentBrain;
	
	private Random rand = new Random(1234);
	private MasterAgent teacher;
	private MPFAgent agent;

	String flatNoBlock = "-vis off -lb off -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
	String flatBlocks = "-vis off -lb on -lca off -lco off -lde off -le off -lf off -lg off -lhs off -ltb off";
	String withCoins = "-vis off -lb on -lca off -lco on -lde off -le off -lf off -lg off -lhs off -ltb off";
	String withGaps = "-vis on -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb off";
	String deadEnds = "-vis off -lb on -lca off -lco on -lde on -le off -lf off -lg on -lhs off -ltb off";
	String withTubes = "-vis on -lb on -lca off -lco on -lde off -le off -lf off -lg on -lhs off -ltb on";
	String withFrozenEnemies = "-vis off -lb on -lca off -lco on -lde on -le on -lf off -lg on -lhs off -ltb on -fc on";
	String everything = "-vis off -lb on -lca on -lco on -lde on -lf off -lg on -lhs on -ltb on";
	
	
	public static void main(String[] args) {
		Mario_Reactionary mr = new Mario_Reactionary();
		mr.run();

	}
	
	public void run(){
		String learningOptions = flatNoBlock;
		setup(false);
		System.out.println("Starting teaching");
		doTeaching(teacher, agent, 100, 0, learningOptions);
		System.out.println();
		System.out.println("Starting training");
		//doTraining(agent, 100, 0, learningOptions);
		System.out.println();
		System.out.println("Starting evaluation");
		doEvaluation(agent, 100, 0, learningOptions);
		
	}
	
	private void doTeaching(MasterAgent teacher, MPFAgent pupil, int episodes, int saveEvery, String learningOptions){
		boolean writeInfo = false;
		for (int i = 0; i < episodes; i++){
			if (saveEvery > 0){
				writeInfo = (i % saveEvery == 0);
			}
			if (writeInfo)agentBrain.openFiles(true);
			int[] results = runRound(pupil, learningOptions, teacher);
			System.out.println("Teaching run " + i + " - Distance traveled: " + results[0]);
			pupil.newEpisode(); 
			if (writeInfo)agentBrain.closeFiles();
		}
	}
	
	private void doTraining(MPFAgent pupil, int episodes, int saveEvery, String learningOptions){
		boolean writeInfo = false;
		for (int i = 0; i < episodes; i++){
			pupil.setExplorationChance(1 - (double) i / episodes);    
			if (saveEvery > 0){
				writeInfo = (i % saveEvery == 0);
			}
			if (writeInfo)agentBrain.openFiles(true);
			int[] results = runRound(pupil, learningOptions, null);
			System.out.println("Training run " + i + " - Distance traveled: " + results[0]);
			pupil.newEpisode(); 
			if (writeInfo)agentBrain.closeFiles();
		}
	}
	
	private void doEvaluation(MPFAgent pupil, int episodes, int saveEvery, String learningOptions){
		pupil.setExplorationChance(0);
		pupil.getNetwork().getNetwork().setLearning(false);
		boolean writeInfo = false;
		for (int i = 0; i < episodes; i++){
			if (saveEvery > 0){
				writeInfo = (i % saveEvery == 0);
			}
			if (writeInfo)agentBrain.openFiles(true);
			int[] results = runRound(pupil, learningOptions, null);
			System.out.println("Evaluation run " + i + " - Distance traveled: " + results[0]);
			pupil.newEpisode(); 
			if (writeInfo)agentBrain.closeFiles();
		}
	}
	
	protected int[] runRound(MPFAgent agent, String levelOptions, MasterAgent teacher){
		Environment environment = new MarioEnvironment();
		if (teacher != null){
			environment.setAgent(teacher);
		} else {
			environment.setAgent(agent);
		}
		
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
			if (teacher != null) teacher.integrateObservation(environment);
			agent.integrateObservation(environment);
			action = agent.getAction();
			if (teacher != null) {
				action = teacher.getAction();
				agent.setAction(action);
			} 			
			distanceBefore = distanceNow;
			environment.performAction(action);				
		}
		
		int[] ev = environment.getEvaluationInfoAsInts();
		return ev;
	}
	
	private static double calculateReward(int[] environment, int distanceNow, int distanceBefore){
		double reward = 0;
		
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
	
	private void setup(boolean writeInfo){
		teacher = createTeacher();
		agent = createPupil();
		
		if (writeInfo){			
			agentBrain = (Network_DataCollector) agent.getNetwork().getNetwork();
			agentBrain.initializeWriters(outputFile, false);
			agentBrain.closeFiles();
		}		
	}
	
	private MasterAgent createTeacher(){
		String file = "D:\\Users\\Simon\\Dropbox\\ITU\\AI\\Mario\\Exam\\Org - disabled -4.txt";
		MasterAgent agentGAP = new GapAgent("ThisRocks", file, 1, 1, 7, 7);
		agentGAP.createBrain();
		
		return agentGAP;
	}
	
	private MPFAgent createPupil(){
		Network_DataCollector network = buildNetwork(6, 18, true);
		HTMNetwork brain = new HTMNetwork(network);
		MPFAgent agent = new ScannerAgent("Scanner", brain, 1, 1, 7, 7);
		return agent;
		
	}
	
	private Network_DataCollector buildNetwork(int actionVectorLength, int inputLength, boolean offlineLearning){
		Network_DataCollector network = new Network_DataCollector();

		//Create top node
		UnitNode topNode = new UnitNode(0,0,0,3);
		
		//Create node that combines input and action
		UnitNode combiner = new UnitNode(1, 0,0,2);		
		combiner.setParent(topNode);
		
		//Create node that pools input
		UnitNode inputPooler = new UnitNode(2, 0,0,1);
		inputPooler.setParent(combiner);
		
		//Create action sensor
		Sensor actionSensor = new Sensor(3, -1,0,0);
		actionSensor.initialize(actionVectorLength);

		//Create action node
		ActionNode actionNode = new ActionNode(4);
		int actionMapSize = 4;
		int numActions = actionMapSize * actionMapSize;
		actionNode.initialize(rand, actionVectorLength, actionMapSize, 0.1, 0.05);
		actionSensor.setParent(actionNode);
				
		//Initialize unit nodes
		//Input pooler
		int spatialMapSize_input = 5;
		int temporalMapSize_input = 3;
		int markovOrder_input = 3;
		inputPooler.initialize(rand, inputLength, spatialMapSize_input, temporalMapSize_input, 0.1, markovOrder_input, numActions, offlineLearning);
	
		//Combiner
		
		int ffInputLength_combiner = inputPooler.getFeedforwardOutputVectorLength();
		int spatialMapSize_combiner = 5;
		int temporalMapSize_combiner = 3;
		int markovOrder_combiner = 3;
		combiner.initialize(rand, ffInputLength_combiner, spatialMapSize_combiner, temporalMapSize_combiner, 0.1, markovOrder_combiner, numActions, offlineLearning);
	
		//top node
		int ffInputLength_top = combiner.getFeedforwardOutputVectorLength();
		int spatialMapSize_top = 5;
		int temporalMapSize_top = 3;
		int markovOrder_top = 3;
		topNode.initialize(rand, ffInputLength_top, spatialMapSize_top, temporalMapSize_top, 0.1, markovOrder_top, numActions, offlineLearning);
		
		//Create input sensors
		int id = 5;
		for (int i = 0; i < inputLength; i++){
			Sensor inputSensor= new Sensor(id++,i,0,0);
			inputSensor.initialize(1);
			inputSensor.setParent(inputPooler);
			inputPooler.addChild(inputSensor);
			network.addNode(inputSensor);
		}		
		
		
		//Add children - Needs to be done in reverse order of creation to make sure that input length calculation is correct
		actionNode.addChild(actionSensor);
		combiner.addChild(inputPooler);
		topNode.addChild(combiner);
		
		//Add nodes to brain		
		network.addNode(actionSensor);
		network.addNode(inputPooler);
		network.addNode(combiner);
		network.addNode(topNode);
		network.addNode(actionNode);
				
		return network; 
	}
	
	/**
	 * Create all combinations f data that are possible to perform
	 */
	private ArrayList<SimpleMatrix> createActionMatrix(){
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
		
		ArrayList<SimpleMatrix> actions = new ArrayList<SimpleMatrix>();
		for (double[][] dataMatrix : actionData){
			SimpleMatrix m = new SimpleMatrix(dataMatrix);
			actions.add(m);
		}		
		
		return actions;
	}
	
	protected void loadActionMatrix(Network brain, ArrayList<SimpleMatrix> actions){
		brain.getActionNode().setPossibleActions(actions);
	}

}
