package com.stcl.htm.experiments.mario;

import org.ejml.simple.SimpleMatrix;

import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

import ch.idsia.agents.controllers.BasicMarioAIAgent;

public class EnvironmentAgent extends MPFAgent {
	
	public EnvironmentAgent(String s, HTMNetwork brain, int zLevelEnemies, int zLevelScene) {
		super(s, brain, zLevelEnemies, zLevelScene);
	}
	
	public EnvironmentAgent(){
		super();
	}
	
	@Override
	public void init(Properties props) {
		super.init(props);		
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
		SimpleMatrix m = new SimpleMatrix(levelScene.length, levelScene[0].length);
		for (int row = 0; row < levelScene.length; row++){
			for (int col = 0; col < levelScene[row].length; col++){
				m.set(row, col, levelScene[row][col]);
			}
		}
		return m;
	}

	
	

}
