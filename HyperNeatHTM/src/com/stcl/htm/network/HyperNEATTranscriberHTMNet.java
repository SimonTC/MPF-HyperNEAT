package com.stcl.htm.network;

import java.util.HashMap;
import java.util.Random;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.ejml.simple.SimpleMatrix;
import org.jgapcustomised.*;

import stcl.algo.brain.Network;
import stcl.algo.brain.nodes.ActionNode;
import stcl.algo.brain.nodes.Node;
import stcl.algo.brain.nodes.Sensor;
import stcl.algo.brain.nodes.UnitNode;
import stcl.algo.poolers.SOM;
import stcl.algo.poolers.SpatialPooler;

import com.anji.integration.Activator;
import com.anji.integration.Transcriber;
import com.anji.integration.TranscriberException;
import com.anji.nn.activationfunction.ActivationFunction;
import com.anji.nn.activationfunction.ActivationFunctionFactory;
import com.anji.util.Randomizer;
import com.ojcoleman.ahni.hyperneat.Properties;
import com.ojcoleman.ahni.nn.GridNet;
import com.ojcoleman.ahni.transcriber.HyperNEATTranscriber;
import com.ojcoleman.ahni.util.SuperPoint;

import dk.stcl.core.basic.containers.SomMap;
import dk.stcl.core.basic.containers.SomNode;

/**
 * Constructs a {@link com.ojcoleman.ahni.nn.GridNet} neural network from a chromosome using the hypercube (from HyperNEAT)
 * encoding scheme. An {@link com.anji.integration.ActivatorTranscriber} should be used to construct an instance of this
 * class.
 * 
 * To transcribe the neural network from a {@link Chromosome} a connective pattern producing network (CPPN) is created
 * from the Chromosome, and then this is "queried" to determine the weight of each connection in the neural network. The
 * CPPN is an {@link com.anji.nn.AnjiNet}.
 * 
 * @author Oliver Coleman
 */
public class HyperNEATTranscriberHTMNet extends HyperNEATTranscriber {
	//public static final String HYPERNEAT_ACTIVATION_FUNCTION_KEY = "ann.hyperneat.activation.function";
	
	public static final String HTM_ACTION_VECTOR_LENGTH_KEY = "htm.action.inputlenght";
	public static final String HTM_ACTION_GROUP_MAPSIZE_KEY = "htm.action.mapsize";
	public static final String HTM_ACTION_VOTER_INFLUENCE_EVOLVE_KEY = "htm.action.voter.influence.evolve";
	public static final String HTM_ACTION_DECIDER_BATCH_TRAINING_KEY = "htm.action.decider.batch.training";
	public static final String HTM_ACTION_DECIDER_REACTIONARY_KEY = "htm.action.decider.reactionary";

	private final static Logger logger = Logger.getLogger(HyperNEATTranscriberHTMNet.class);
	
	private Randomizer rand;
	private boolean useActions = true;
	private double explorationChance = 0.05;
	private Properties props;

	public HyperNEATTranscriberHTMNet() {
	}

	public HyperNEATTranscriberHTMNet(Properties props) {
		init(props);
	}

	public void init(Properties props) {
		super.init(props);
		rand = new Randomizer();
		rand.init(props);
		this.props = props;
	}

	/**
	 * @see Transcriber#transcribe(Chromosome)
	 */
	public HTMNetwork transcribe(Chromosome genotype) throws TranscriberException {
		return newHTMBrain(genotype, null);
	}

	public HTMNetwork transcribe(Chromosome genotype, Activator substrate) throws TranscriberException {
		return newHTMBrain(genotype, (HTMNetwork) substrate);
	}

	/**
	 * Create a new neural network from the a genotype.
	 * 
	 * @param genotype chromosome to transcribe
	 * @return phenotype If given this will be updated and returned, if NULL then a new network will be created.
	 * @throws TranscriberException
	 */
	public HTMNetwork newHTMBrain(Chromosome genotype, HTMNetwork phenotype) throws TranscriberException {
		CPPN cppn = new CPPN(genotype);
		
		int connectionRange = this.connectionRange == -1 ? Integer.MAX_VALUE / 4 : this.connectionRange;

		boolean[][][][][][] connectionMatrix;
		int[][][][] parameterMatrix;
		double[][][] bias;
		boolean createNewPhenotype = (phenotype == null);
		Node[][][] nodes;
		int nextFreeID = 0;
		

		Network brainNetwork = new Network();
		nodes = new Node[depth][][];			
		for (int l = 0; l < depth; l++){
			nodes[l] = new Node[height[l]][width[l]];
		}
		
		TreeMap<Integer, Double> votingInfluences = new TreeMap<Integer, Double>();

		// query CPPN for substrate connections and node parameters
		for (int sz = 0; sz < depth; sz++) { //Node in top layer doesn't have any parent
			for (int sy = 0; sy < height[sz]; sy++) {
				for (int sx = 0; sx < width[sz]; sx++) {					
					//If node doesn't exist (no children), we don't need to test for parent
					Node n;
					if (sz == 0){
						//Create node without any children
						n = new Sensor(nextFreeID, sx, sy, sz);
						Sensor s = (Sensor) n;
						s.initialize(1);
						//System.out.println("Added sensor with id " + n.getID());
						brainNetwork.addNode(n);
						nodes[sz][sy][sx] = n;
						nextFreeID++;
					} else {
						n = nodes[sz][sy][sx];
					}
					
					if (n != null){					
						cppn.setSourceCoordinatesFromGridIndices(sx, sy, sz);	
						double[] coordinates = {sx, sy, sz, -1,-1,-1}; //The last three are used for initializing the spatial pooler
						SuperPoint p = new SuperPoint(coordinates);
						cppn.setExtraSourceCoordinates(p);
						
						//Decide on spatial and temporal mapsize of node and initialize it
						if (sz > 0){ //Sensors should not be initialized
							cppn.setTargetCoordinatesFromGridIndices(sx, sy, sz);
							cppn.setExtraTargetCoordinates(p);
							cppn.query();
							int spatialMapSize = (int) Math.round(cppn.getRangedNeuronParam(0, 0));
							int temporalMapSize = (int) Math.round(cppn.getRangedNeuronParam(0, 1));
							int markovOrder = (int) Math.round(cppn.getRangedNeuronParam(0, 2));
							double votingInfluence = (double) cppn.getRangedNeuronParam(0, 3);
							int actionMapSize = props.getIntProperty(HTM_ACTION_GROUP_MAPSIZE_KEY,2);
							UnitNode unitnode = (UnitNode) n;
							int id = nextFreeID++;
							unitnode.setID(id);
							boolean batchTraining = props.getBooleanProperty(HTM_ACTION_DECIDER_BATCH_TRAINING_KEY, false);
							boolean usePrediction = true; //TODO: Is this used? ALso move to property file
							boolean reactionary = props.getBooleanProperty(HTM_ACTION_DECIDER_REACTIONARY_KEY, false);
							unitnode.initialize(rand.getRand(), spatialMapSize, temporalMapSize, markovOrder, actionMapSize * actionMapSize, usePrediction, reactionary, batchTraining);
							brainNetwork.addNode(unitnode);
							votingInfluences.put(id, votingInfluence);
							//System.out.println("Initialized unitnode with id " + unitnode.getID());
							
							//Initialize weights of Spatial pooler
							if (spatialMapSize > 0){
								SpatialPooler pooler = unitnode.getUnit().getSpatialPooler();
								SOM som = pooler.getSOM();
								SomMap map = som.getSomMap();
								int mapHeight = map.getHeight();
								int mapWidth = map.getWidth();
								for (int mapX = 0; mapX < mapWidth; mapX++){
									double x = (double) mapX / mapWidth;
									for (int mapY = 0; mapY < mapHeight; mapY++){
										double y = (double) mapY / mapHeight;
										SomNode node = map.get(mapX, mapY);
										SimpleMatrix vector = node.getVector();
										int vectorLength = vector.getNumElements();
										for (int i = 0; i < vectorLength; i++){
											coordinates[3] = x;
											coordinates[4] = y;
											coordinates[5] = (double) i / vectorLength;
											cppn.setExtraSourceCoordinates(p);
											cppn.setExtraTargetCoordinates(p);
											cppn.query();
											double weight = (double) cppn.getRangedNeuronParam(0, 4);
											vector.set(i, weight);
										}
										node.setVector(vector);
									}
								}								
							}
						}
						
						//Go through all possible parents and find the one with the highest connection weight
						int[] parentCoordinates = new int[3];
						double maxWeight = Double.NEGATIVE_INFINITY;
						boolean noParent = true;
						
						for (int tz = sz + 1; tz < Math.min(depth, sz + connectionRange + 1); tz++){
							for (int ty = Math.max(0, sy - connectionRange); ty < Math.min(height[tz], sy + connectionRange); ty++){
								for (int tx = Math.max(0, sx - connectionRange); tx < Math.min(width[tz], sx + connectionRange); tx++){
									cppn.setTargetCoordinatesFromGridIndices(tx, ty, tz);
									cppn.query();
									double weight = cppn.getRangedWeight();
									if (weight > maxWeight){
										maxWeight = weight;
										int[] tmp = {tz,ty,tx};
										parentCoordinates = tmp;
										noParent = false;
									}
								}
							}						
						}
						
						if (!noParent){ //There is no parent if we are looking at the top node
							//Test if coordinates are within bounds
							Node parent = nodes[parentCoordinates[0]][parentCoordinates[1]][parentCoordinates[2]];
							if (parent == null){
								int z = parentCoordinates[0];
								int y = parentCoordinates[1];
								int x = parentCoordinates[2];
								parent = new UnitNode(-1, x, y, z); //We don't give it its final id yet
								nodes[parentCoordinates[0]][parentCoordinates[1]][parentCoordinates[2]] = parent;
							}							
							parent.addChild(n);
							n.setParent(parent);
						}
					}					
				}
			}
		}
		
		if (useActions){
			int actionVectorLength = props.getIntProperty(HTM_ACTION_VECTOR_LENGTH_KEY,3);
			Sensor actionSensor = new Sensor(nextFreeID++, -1,-1,0); //The action sensor isn't really part of the sensor layer so we give it negative coordinates
			actionSensor.initialize(actionVectorLength);
			ActionNode actionNode = new ActionNode(nextFreeID++);
			actionNode.initialize(rand.getRand(), actionVectorLength, props.getIntProperty(HTM_ACTION_GROUP_MAPSIZE_KEY,2), 0.1, explorationChance); //TODO: Use parameters
			actionSensor.setParent(actionNode);
			actionNode.addChild(actionSensor);
			brainNetwork.addNode(actionNode);
			brainNetwork.addNode(actionSensor);
			if (props.getBooleanProperty(HTM_ACTION_VOTER_INFLUENCE_EVOLVE_KEY, false)){
				actionNode.setInfluenceMap(votingInfluences);
				actionNode.setUpdateVoterInfluence(false);
			}
		}

		if (createNewPhenotype) {
			phenotype = new HTMNetwork(brainNetwork);
			//logger.info("New substrate has input size " + width[0] + "x" + height[0] + " and " + phenotype.getNetwork().getNumUnitNodes() + " active unit nodes.");
		} else {
			phenotype.setNetwork(brainNetwork);
			phenotype.setName("network " + genotype.getId());
		}

		return phenotype;
	}

	/**
	 * @see com.anji.integration.Transcriber#getPhenotypeClass()
	 */
	public Class getPhenotypeClass() {
		return GridNet.class;
	}
}
