package com.stcl.htm.experiments.rps.gui;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import stcl.graphics.MPFGUI;

public interface GUI {

	
	public void initialize(int observationMatrixSize, int actionMatrixSize, int fps);
	
	public void update(Network brain, SimpleMatrix inputNow, SimpleMatrix actionNow, SimpleMatrix prediction, SimpleMatrix actionNext, int step);
	
	public void setSequenceName(String name);

}
