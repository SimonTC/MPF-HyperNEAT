package com.stcl.htm.experiments.mario;

import com.stcl.htm.network.HTMNetwork;

import ch.idsia.agents.controllers.BasicMarioAIAgent;

public abstract class MPFAgent extends BasicMarioAIAgent {

	protected HTMNetwork brain;
	protected double reward;
	
	public MPFAgent(String s, HTMNetwork brain) {
		super(s);
		this.brain = brain;
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
