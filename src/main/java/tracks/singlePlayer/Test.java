package tracks.singlePlayer;

import java.util.Random;

import core.logging.Logger;
import tools.Utils;
import tracks.ArcadeMachine;

/**
 * Created with IntelliJ IDEA. User: Diego Date: 04/10/13 Time: 16:29 This is a
 * Java port from Tom Schaul's VGDL - https://github.com/schaul/py-vgdl
 */
public class Test {

    public static void main(String[] args) {
		//Load available games
		String spGamesCollection =  "examples/all_games_sp.csv";
		String[][] games = Utils.readGames(spGamesCollection);

		//Game settings
		boolean visuals = true;
		int seed = new Random().nextInt();

		// FOR TEST PURPOSE ONLY
		//int levelIdx = 5; // level names from 0 to 4 (game_lvlN.txt).
		//int seed = 0;

		// Game and level to play
		int gameIdx = 11;
		int levelIdx = 0; // level names from 0 to 4 (game_lvlN.txt).
		String gameName = games[gameIdx][1];
		String game = games[gameIdx][0];
		String level1 = game.replace(gameName, gameName + "_lvl" + levelIdx);

		// Controller name
		String controller = "controller.PlanningAgent";

		String recordActionsFile = null;// "actions_" + games[gameIdx] + "_lvl"
						// + levelIdx + "_" + seed + ".txt";
						// where to record the actions
						// executed. null if not to save.

		// 1. This starts a game, in a level, played by a human.
		//ArcadeMachine.playOneGame(game, level1, recordActionsFile, seed);

		// 2. This plays a game in a level by the controller.
		ArcadeMachine.runOneGame(game, level1, visuals, controller, recordActionsFile, seed, 0);
    }
}
