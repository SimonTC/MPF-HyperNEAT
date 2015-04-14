package com.stcl.htm.experiments.mario;

import com.ojcoleman.ahni.hyperneat.Configurable;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.stcl.htm.network.HTMNetwork;

import ch.idsia.agents.controllers.BasicMarioAIAgent;

public abstract class MPFAgent extends BasicMarioAIAgent implements Configurable{

	protected HTMNetwork brain;
	protected double reward;
	private static final String KEY_AGENT_ZLEVEL_ENEMIES = "mario.agent.zlevel.enemies";
	private static final String KEY_AGENT_ZLEVEL_SCENE = "mario.agent.zlevel.scene";
	
	public MPFAgent(){
		super("MPF Agent");
	}
		
	public MPFAgent(String s, HTMNetwork brain, int zLevelEnemies, int zLevelScene) {
		super(s);
		this.brain = brain;
		this.zLevelEnemies = zLevelEnemies;
		this.zLevelScene = zLevelScene;
	}
	
	public void init(Properties props){
		zLevelEnemies = props.getIntProperty(KEY_AGENT_ZLEVEL_ENEMIES, 1);
		zLevelScene = props.getIntProperty(KEY_AGENT_ZLEVEL_SCENE, 1);
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
	
	public void setBrain(HTMNetwork brain){
		this.brain = brain;
	}

}
