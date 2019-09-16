package controller; //The package name is the same as the username in the web.

// Imports related to GVGAI
import core.player.AbstractPlayer;
import core.game.StateObservation;
import core.game.SerializableStateObservation;
import core.game.GameDescription;
import core.vgdl.VGDLRegistry;
import tools.ElapsedCpuTimer;
import ontology.Types;

import java.util.ArrayList;

import parsing.Parser;

import java.util.Random;

public class Agent extends AbstractPlayer {

    protected Random randomGenerator;

    //Constructor. It must return in 1 second maximum.
    public Agent(StateObservation so, ElapsedCpuTimer elapsedTimer)
    {
        randomGenerator = new Random();
    }

    //Act function. Called every game step, it must return an action in 40 ms maximum.
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {

        //Get the available actions in this game.
        ArrayList<Types.ACTIONS> actions = stateObs.getAvailableActions();
        //System.out.println(stateObs.getObservationGrid()[6][6].get(0));

        System.out.println(Parser.<String, ArrayList<String>>parseJSONFile("correspondence.json").get("A").get(0));
        System.out.println(VGDLRegistry.GetInstance().getRegisteredSpriteKey(10));
        Parser.parseStateObservation(stateObs);

        //Determine an index randomly and get the action to return.
        int index = randomGenerator.nextInt(actions.size());
        Types.ACTIONS action = actions.get(index);

        //Return the action.
        return action;
    }
}