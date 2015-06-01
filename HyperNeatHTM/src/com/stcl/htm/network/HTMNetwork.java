package com.stcl.htm.network;

import java.awt.Graphics2D;
import java.io.Serializable;
import java.util.ArrayList;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import stcl.algo.brain.nodes.Sensor;

import com.anji.integration.Activator;

public class HTMNetwork  implements Activator, Serializable {

	private static final long serialVersionUID = 1L;

	private Network network;
	
	/**
	 * base XML tag
	 */
	public final static String XML_TAG = "HTM network";
	
	private String name;
	private double reward; 
	
	
	
	public HTMNetwork(){
		this.network = new Network();
	}
	
	public HTMNetwork(Network network){
		this.network = network;
	}
		
	/**
	 * Activate the whole network once.
	 * Call setInput() and setAction() before calling this
	 * @param reward
	 */
	public void step(double reward){
		network.step(reward);
	}
	
	/**
	 * Set the values of the input sensors
	 * @param stimuli
	 */
	public void setInput(double[] stimuli){
		ArrayList<Sensor> sensors = network.getSensors();
		 
		for (int i = 0; i < stimuli.length; i++){
			Sensor s = sensors.get(i);
			s.setInput(stimuli[i]);
		}

	}
	
	public void setAction(double[] action){
		ArrayList<Sensor> sensors = network.getSensors();
		Sensor actionSensor = sensors.get(sensors.size()-1);
		SimpleMatrix input = new SimpleMatrix(1, action.length, true, action);
		actionSensor.setInput(input);
	}
	
	public double[] getOutput(){
		ArrayList<Sensor> sensors = network.getSensors();
		double[] output = new double[sensors.size()];
		for (int i = 0; i < sensors.size()-1; i++){ //Substracting one because we don't want the action
			Sensor s = sensors.get(i);
			output[i] = s.getFeedbackOutput().get(0);
		}
		return output;
	}
	
	public double[] getAction(){
		ArrayList<Sensor> sensors = network.getSensors();
		Sensor actionSensor = sensors.get(sensors.size()-1);
		double[] output = actionSensor.getFeedbackOutput().getMatrix().data;
		return output;
	}
	
	@Override
	public String getXmlRootTag() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getXmld() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object next() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public double[] next(double[] stimuli) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] nextSequence(double[][] stimuli) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] next(double[][] stimuli) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][][] nextSequence(double[][][] stimuli) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toXml() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void reset() {
		network.flush();
	}

	@Override
	public String getName() {
		return name;
	}
	
	public void setName(String name){
		this.name = name;
	}

	@Override
	public double getMinResponse() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double getMaxResponse() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int[] getInputDimension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int[] getOutputDimension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getInputCount() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	@Override
	public int getOutputCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean render(Graphics2D g, int width, int height, int nodeSize) {		
		
		width -= nodeSize * 2;
		height -= nodeSize * 2;
		
		
		
		
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public String toString(){
		return network.toString();
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRecurrent() {
		return false;
	}
		
	public Network getNetwork(){
		return network;
	}
	
	public void setNetwork(Network network){
		this.network = network;
	}

}
