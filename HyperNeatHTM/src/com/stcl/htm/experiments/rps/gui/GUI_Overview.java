package com.stcl.htm.experiments.rps.gui;

import org.ejml.simple.SimpleMatrix;

import stcl.algo.brain.Network;
import stcl.graphics.MPFGUI;

public class GUI_Overview implements GUI {

private MPFGUI minorGui;
	
	public void initialize(int observationMatrixSize, int actionMatrixSize, int fps){
		minorGui = new MPFGUI();
		minorGui.initialize(observationMatrixSize, actionMatrixSize, fps);
	}
	
	public void update(Network brain, SimpleMatrix inputNow, SimpleMatrix actionNow, SimpleMatrix prediction, SimpleMatrix actionNext, int step){
		minorGui.update(brain, inputNow, actionNow, prediction, actionNext, step);
	}
	
	public void setSequenceName(String name){
		minorGui.setSequenceName(name);
	}
}
