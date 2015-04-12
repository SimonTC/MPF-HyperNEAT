package com.stcl.htm.experiments.mario;

import org.ejml.simple.SimpleMatrix;

import com.stcl.htm.network.HTMNetwork;

import ch.idsia.agents.controllers.BasicMarioAIAgent;

public class EnvironmentAgent extends BasicMarioAIAgent {
	private HTMNetwork brain;
	private double reward;
	
	public EnvironmentAgent(String s, HTMNetwork brain) {
		super(s);
		this.brain = brain;
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
		return null;
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
