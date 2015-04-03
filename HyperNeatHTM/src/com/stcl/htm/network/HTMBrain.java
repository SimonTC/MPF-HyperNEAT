package com.stcl.htm.network;

import java.awt.Graphics2D;

import stcl.algo.brain.Network;

import com.anji.integration.Activator;

public class HTMBrain  implements Activator {

	private boolean[][][][][][] connectionMatrix;
	private int[][][][] parameterMatrix;
	private Network network;
	
	private String name;
	
	HTMBrain(){
		this.network = new Network();
	}
	
	HTMBrain(Network network){
		this.network = network;
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
		// TODO Auto-generated method stub

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
	public boolean render(Graphics2D g, int width, int height, int neuronSize) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isRecurrent() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public boolean[][][][][][] getConnectionMatrix(){
		return this.connectionMatrix;
	}
	
	public int[][][][] getParameterMatrix(){
		return this.parameterMatrix;
	}
	
	public Network getNetwork(){
		return network;
	}
	
	public void setNetwork(Network network){
		this.network = network;
	}

}
