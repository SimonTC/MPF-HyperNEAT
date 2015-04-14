package com.stcl.htm.experiments.mario;

import java.util.ArrayList;
import java.util.Random;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import vikrasim.agents.scanners.MasterScanner;
import vikrasim.agents.scanners.Scanner;
import vikrasim.agents.scanners.gapScanner;
import vikrasim.agents.scanners.MasterScanner.Dir;
import vikrasim.agents.scanners.MasterScanner.ScannerType;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

import ch.idsia.agents.controllers.BasicMarioAIAgent;

public class ScannerAgent extends MPFAgent{
	
	public static final String KEY_AGENT_SCANNER_LENGTH = "mario.agent.scanner.length";
	public static final String KEY_AGENT_SCANNER_HEIGHT = "mario.agent.scanner.heigth";
	
	private ArrayList<MasterScanner> scanners;
	int scannerLength;
	int scannerHeight;
	
	public ScannerAgent(String s, HTMNetwork brain, int zLevelEnemies, int zLevelScene, int scannerLength, int scannerHeight) {
		super(s, brain, zLevelEnemies, zLevelScene);
		
		this.scannerHeight = scannerHeight;
		this.scannerLength = scannerLength;
		
		addScanners(scannerLength, scannerHeight);

	}
	
	public ScannerAgent(){
		super();
	}
	
	@Override
	public void init(Properties props){
		super.init(props);
		this.scannerHeight = props.getIntProperty(KEY_AGENT_SCANNER_HEIGHT, 7);
		this.scannerLength = props.getIntProperty(KEY_AGENT_SCANNER_LENGTH, 7);
		addScanners(scannerLength, scannerHeight);
	}
	
	private void addScanners(int length, int height) {
		this.scanners = new ArrayList<>();

		// Add enemy radar
		scanners.add(new Scanner(length, height, Dir.NE, ScannerType.ENEMY));
		scanners.add(new Scanner(length, height, Dir.NW, ScannerType.ENEMY));
		scanners.add(new Scanner(length, height, Dir.SE, ScannerType.ENEMY));
		scanners.add(new Scanner(length, height, Dir.SW, ScannerType.ENEMY));

		// Add distance scanners
		scanners.add(new Scanner(length, 1, Dir.N, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.S, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.E, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.W, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.NE, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.NW, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.SE, ScannerType.ENVIRONMENT));
		scanners.add(new Scanner(length, 1, Dir.SW, ScannerType.ENVIRONMENT));

		// add gapScanners
		scanners.add(new gapScanner(9, 1, Dir.S, ScannerType.ENVIRONMENT, 1));
		scanners.add(new gapScanner(9, 1, Dir.S, ScannerType.ENVIRONMENT, 3));
	}
	
	@Override
	public boolean[] getAction() {

		SimpleMatrix inputVector = collectInputs();
		
		brain.setInput(inputVector.getMatrix().data);
		
		SimpleMatrix actionVector = collectActionToPerform();
		brain.setAction(actionVector.getMatrix().data);
		
		//Do one step
		brain.step(reward);
		
		//Update the actions done
		double[] chosenAction = brain.getAction();
		for (int i = 0; i < chosenAction.length; i++) {
			action[i] = convertToBoolean(chosenAction[i]);
		}
		
		return action;
	}
	
	private SimpleMatrix collectActionToPerform(){
		//Create action vector
		double[] actionList = new double[action.length];
		for (int i = 0; i < action.length; i++){
			actionList[i] = action[i]? 1.0 : 0.0;
		}
		double[][] actionData = {actionList};
		SimpleMatrix actionVector = new SimpleMatrix(actionData);

		return actionVector;
	}
	
	private SimpleMatrix collectInputs(){
		double[] observations = readSurroundings();
		int numberOfObservations = observations.length;
		double[] inputs = new double[numberOfObservations + 4];
		for (int i = 0; i < numberOfObservations; i++) {
			inputs[i] = observations[i];
		}

		if (inputs[inputs.length - 1] == 0) {
			inputs[inputs.length - 1] = 1;
		} else {
			inputs[inputs.length - 1] = 0;
		}

		if (inputs[inputs.length - 2] == 0) {
			inputs[inputs.length - 2] = 1;
		} else {
			inputs[inputs.length - 2] = 0;
		}

		inputs[numberOfObservations + 0] = convertBooleanToByte(isMarioAbleToJump);
		inputs[numberOfObservations + 1] = convertBooleanToByte(isMarioOnGround);
		inputs[numberOfObservations + 2] = convertBooleanToByte(isMarioAbleToShoot);
		inputs[numberOfObservations + 3] = marioMode;

		
		//Convert inputs to vector
		double[][] data = {inputs};
		SimpleMatrix inputVector = new SimpleMatrix(data);
		
		return inputVector;
	}
	
	private double[] readSurroundings() {

		double[] result = new double[scanners.size()];

		for (int i = 0; i < scanners.size(); i++) {
			MasterScanner s = scanners.get(i);
			result[i] = s.scan(enemies, levelScene);
		}

		return result;
	}

	public HTMNetwork getNetwork(){
		return brain; 
	}
	
	public void setAction(boolean[] action){
		this.action = action;
	}
	
	public void giveReward(double reward){
		this.reward = reward;
	}
	
	protected boolean convertToBoolean(double value) {
		if (value < 0.5)
			return false;
		return true;
	}
	
	protected byte convertBooleanToByte(Boolean b) {
		if (b)
			return 1;
		return 0;
	}

	public void setExplorationChance(double chance){
		brain.getNetwork().getActionNode().setExplorationChance(chance);
	}


	

}
