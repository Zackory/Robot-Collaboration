package uwl.learning;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class QLearningWithEpisodes {
	//	public String[] actions = {"U", "D", "L", "R"};
	public HashMap<String, State> actions = new HashMap<String, State>();
	public String[] actionArray;
	public float alpha = 1.0f;
	public float gamma = 0.9f;
	public State start1;
	public State start2;
	public HashMap<State, String> world = new HashMap<State, String>();
	public HashMap<State, String> currentWorld = new HashMap<State, String>();
	public Random rand = new Random();
	public int height, width;

	public Agent agent1, agent2;

	// public float moveP = 0.78f; // Probability to successfully move
	public float moveP = 1.0f; // Probability to successfully move
	public float pushP = 0.8f; // Probability to successfully push
	public int boxReward = 5000;

	public boolean verbose = false;

	public static void main(String[] args) {
		new QLearningWithEpisodes("world.txt");
	}

	public QLearningWithEpisodes(String filename) {
		// Load world map
		if (verbose) System.out.println("Loading world");
		loadWorld(filename);
		if (verbose) System.out.println("World loaded. Width: " + width + ", Height: " + height);

		// Load actions
		actions.put("U", new State(0, -1));
		actions.put("D", new State(0, 1));
		actions.put("L", new State(-1, 0));
		actions.put("R", new State(1, 0));
		actionArray = actions.keySet().toArray(new String[0]);

		// Create agents
		agent1 = new Agent();
		agent2 = new Agent();

		// 2000 episodes, reducing epsilon every 10 episodes
		learning(2000, 50);
	}

	public void learning(int episodes, int reduction) {
		float epsilon = 0.9f;
		ArrayList<Float> rewards = new ArrayList<Float>();

		for (int i = 0; i < episodes; i++) {
			if (epsilon <= 0.1f) epsilon = 0.0f;
			else if (i >= reduction) epsilon = 0.9f / (i / reduction);

			// Print out current policy
			if (verbose) System.out.println("Episode: " + i + ", Epsilon: " + epsilon);
			if (i % 250 == 0)
				printPolicy(i);

			State s1 = start1; // Current state
			State s2 = start2; // Current state
			float rewardSum = 0.0f;
			// Reset current world
			currentWorld = (HashMap<State, String>) world.clone();

			// while (iterations < ) {
			// Loop for several movement iterations.
			for (int iteration = 0; iteration < 1000; iteration++) {
				// TODO check if all boxes are stuck. If so, exit early.
				// Determine actions, chosen epsilon-greedily based on Q(s,a)
				String a1 = agent1.nextAction(s1, epsilon);
				String a2 = agent2.nextAction(s2, epsilon);
				// Take action a, Observe next state s', one-step reward r
				StateReward[] sr = takeAction(s1, a1, s2, a2);
				// Update Q(s,a)
				agent1.updateQ(s1, a1, sr[0]);
				agent2.updateQ(s2, a2, sr[1]);
				s1 = sr[0].state;
				s2 = sr[1].state;
				rewardSum += sr[0].reward + sr[1].reward;
				// if (iteration % 10 == 0)
				// printWorld();
			}
			// System.exit(0);
			rewards.add(rewardSum);
		}
		// Print out final policy
		printPolicy(episodes);
	}

	// Pass in the current state of the agent. This will return the updated state if valid 
	// (in the world), otherwise it returns the provided state (currentState)
	public State updateState(Agent agent, State currentState) {
		State nextState1 = currentState.add(actions.get(agent.nextAction(currentState, 0)));
		String nextGrid1 = currentWorld.get(nextState1);
		if (nextGrid1 != null)
			return nextState1;
		return null;
	}

	public void printPolicy(int episode) {
		System.out.println("\nEpisode: " + episode);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				System.out.print(agent1.nextAction(new State(x, y), 0) + " ");
			System.out.print("  |  ");
			for (int x = 0; x < width; x++)
				System.out.print(agent2.nextAction(new State(x, y), 0) + " ");
			System.out.println();
		}
		System.out.println();
	}

	public void printWorld() {
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				System.out.print(currentWorld.get(new State(x, y)) + " ");
			System.out.println();
		}
		System.out.println();
	}

	// TODO ------- 
	// Prevent boxes from being pushed into left, bottom, and right walls 
	// ---------

	// This is part of the environment, with rewards and states being sent back to each agent.
	public StateReward[] takeAction(State state1, String action1, State state2, String action2) {
		float p1 = rand.nextFloat();
		float p2 = rand.nextFloat();
		String currentGrid1 = currentWorld.get(state1);
		String currentGrid2 = currentWorld.get(state2);

		State nextState1 = state1.add(actions.get(action1));
		State nextState2 = state2.add(actions.get(action2));

		String nextGrid1 = currentWorld.get(nextState1);
		String nextGrid2 = currentWorld.get(nextState2);

		StateReward sr[] = new StateReward[2];
		sr[0] = new StateReward(state1, -1); 
		sr[1] = new StateReward(state2, -1);

		if (nextGrid1 == nextGrid2)
			// Robots attempting to move into same grid (or both returned grids are null)
			return sr;

		/*if (nextGrid1.contains("B") && p1 < pushP && nextGrid2.contains("B") && p2 < pushP) {
			// Both agents are successfully pushing a box. Make sure no collisions occurs
			State nextBoxState = nextState1.add(actions.get(action1));
			String nextBoxGrid = currentWorld.get(nextBoxState);

		} else if (nextGrid1.contains("B") && p1 < pushP) {
			// First agent is successfully pushing a box.

		} else if (nextGrid2.contains("B") && p2 < pushP) {
			// Second agent is successfully pushing a box.

		}*/

		if ((nextGrid1 != null && nextGrid1.contains("B") && p1 <= pushP) || 
				(nextGrid2 != null && nextGrid2.contains("B") && p2 <= pushP)) {
			// Push successful
			// Check that the box can actually move into another grid
			State nextBoxState1 = nextState1.add(actions.get(action1));
			String nextBoxGrid1 = currentWorld.get(nextBoxState1);
			State nextBoxState2 = nextState2.add(actions.get(action2));
			String nextBoxGrid2 = currentWorld.get(nextBoxState2);
			// Make sure at least one box can move
			if (nextBoxGrid1 == null && nextBoxGrid2 == null)
				return sr;
			// Make sure box is not pushed into same grid that other agent is moving into
			if (nextBoxGrid1 == nextGrid2 || nextBoxGrid2 == nextGrid1)
				// Agent attempting to push box into grid that the other agent is moving into.
				return sr;
			// Make sure box is not pushed into same grid other agent is pushing a box into
			if (nextGrid1 != null && nextGrid2 != null && nextGrid1.contains("B") && p1 <= pushP && 
					nextGrid2.contains("B") && p2 < pushP && nextBoxGrid1 == nextBoxGrid2)
				// Both agents attempting to push boxes into same grid. Both will fail.
				return sr;
			// Handle case in which one agent attempts to push box into the other who doesn't move.
			if ((nextBoxGrid1 == currentGrid2 && nextGrid2 == null) || (nextBoxGrid2 == currentGrid1 && nextGrid1 == null))
				return sr;

			// Check if first agent is pushing a box
			if (nextGrid1 != null && nextBoxGrid1 != null && nextGrid1.contains("B") && p1 <= pushP) {
				// Verify box does not push into the left, right, or bottom grids (box would be stuck)
				if (nextBoxState1.x > 0 && nextBoxState1.x < width - 1 && nextBoxState1.y < height - 1) {
					if (nextBoxGrid1.equals("O")) {
						// The box can be moved successfully!
						// Move box to next grid
						currentWorld.replace(nextBoxState1, currentWorld.get(nextState1));
						// Move agent to grid where box was
						moveAgent(state1, nextState1);
						sr[0] = new StateReward(nextState1, 0); // TODO trying non-negative reward for pushing box
					} else if (nextBoxGrid1.equals("G")) {
						// Box has reached the goal. 
						// Move box back to its beginning location (if it's not already preoccupied)
						State origBoxState = null;
						// Find original location for box
						for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
								if (world.get(new State(x, y)).equals(nextGrid1))
									origBoxState = new State(x, y);
						if (origBoxState == null) {
							System.err.println("No original state found for Box: " + nextGrid1);
							System.exit(-1);
						}
						String origBoxGrid = currentWorld.get(origBoxState);
						int reward = 0;
						// TODO make sure origBoxGrid does not collide with current Agent 2's location if Agent 2 doesn't move.
						if (origBoxGrid.equals("O") && origBoxGrid != nextGrid2 && origBoxGrid != nextBoxGrid2 && origBoxGrid != currentGrid2) {
							// Box can move back to starting point
							currentWorld.replace(origBoxState, currentWorld.get(nextState1));
							reward += boxReward;
						} else
							// Box cannot teleport back to beginning location. Move box into goal grid
							currentWorld.replace(nextBoxState1, currentWorld.get(nextState1));
						// Move agent to grid where box was
						moveAgent(state1, nextState1);
						sr[0] = new StateReward(nextState1, reward);
					}
				}
			}

			// Check if second agent is pushing a box
			if (nextGrid2 != null && nextBoxGrid2 != null && nextGrid2.contains("B") && p2 <= pushP) {
				// Verify box does not push into the left, right, or bottom grids (box would be stuck)
				if (nextBoxState2.x > 0 && nextBoxState2.x < width - 1 && nextBoxState2.y < height - 1) {
					if (nextBoxGrid2.equals("O")) {
						// The box can be moved successfully!
						// Move box to next grid
						currentWorld.replace(nextBoxState2, currentWorld.get(nextState2));
						// Move agent to grid where box was
						moveAgent(state2, nextState2);
						sr[1] = new StateReward(nextState2, 0); // TODO trying non-negative reward for pushing box
					} else if (nextBoxGrid2.equals("G")) {
						// Box has reached the goal. 
						// Move box back to its beginning location (if it's not already preoccupied)
						State origBoxState = null;
						// Find original location for box
						for (int y = 0; y < height; y++)
							for (int x = 0; x < width; x++)
								if (world.get(new State(x, y)).equals(nextGrid2))
									origBoxState = new State(x, y);
						if (origBoxState == null) {
							System.err.println("No original state found for Box: " + nextGrid2);
							System.exit(-1);
						}
						String origBoxGrid = currentWorld.get(origBoxState);
						int reward = 0;
						// TODO make sure origBoxGrid does not collide with current Agent 2's location if Agent 2 doesn't move.
						if (origBoxGrid.equals("O") && origBoxGrid != nextGrid1 && origBoxGrid != nextBoxGrid1 && origBoxGrid != currentGrid1) {
							// Box can move back to starting point
							currentWorld.replace(origBoxState, currentWorld.get(nextState2));
							reward += boxReward;
						} else
							// Box cannot teleport back to beginning location. Move box into goal grid
							currentWorld.replace(nextBoxState2, currentWorld.get(nextState2));
						// Move agent to grid where box was
						moveAgent(state2, nextState2);
						sr[1] = new StateReward(nextState2, reward);
					}
				}
			}

			// Agent is attempting to push box into grid of other agent while the other agent is moving to another grid (both can move)
			if (nextBoxGrid1 == currentGrid2 && sr[1].state != state2) {
				// Verify box does not push into the left, right, or bottom grids (box would be stuck)
				if (nextBoxState1.x > 0 && nextBoxState1.x < width - 1 && nextBoxState1.y < height - 1) {
					// Move box to next grid
					currentWorld.replace(nextBoxState1, currentWorld.get(nextState1));
					// Move agent to grid where box was
					moveAgent(state1, nextState1);
					sr[0] = new StateReward(nextState1, 0); // TODO trying non-negative reward for pushing box
				}
			} else if (nextBoxGrid2 == currentGrid1 && sr[0].state != state1) {
				// Verify box does not push into the left, right, or bottom grids (box would be stuck)
				if (nextBoxState2.x > 0 && nextBoxState2.x < width - 1 && nextBoxState2.y < height - 1) {
					// Move box to next grid
					currentWorld.replace(nextBoxState2, currentWorld.get(nextState2));
					// Move agent to grid where box was
					moveAgent(state2, nextState2);
					sr[1] = new StateReward(nextState2, 0); // TODO trying non-negative reward for pushing box
				}
			}
		} 

		if (nextGrid1 != null && (nextGrid1.equals("O") || nextGrid1.equals("G"))) {
			// Move agent to next grid
			moveAgent(state1, nextState1);
			sr[0] = new StateReward(nextState1, -1);
		} 
		if (nextGrid2 != null && (nextGrid2.equals("O") || nextGrid2.equals("G"))) {
			// Move agent to next grid
			moveAgent(state2, nextState2);
			sr[1] = new StateReward(nextState2, -1);
		} 


		// Check if agent is attempting to move into grid of other agent while the other agent is moving to another grid (both can move)
		if (nextGrid1 == currentGrid2 && sr[1].state != state2) {
			// Move agent to next grid
			moveAgent(state1, nextState1);
			sr[0] = new StateReward(nextState1, -1);
		}
		if (nextGrid2 == currentGrid1 && sr[0].state != state1) {
			// Move agent to next grid
			moveAgent(state2, nextState2);
			sr[1] = new StateReward(nextState2, -1);
		}

		if (nextGrid1 != null && nextGrid2 != null && nextGrid1.equals("D") && nextGrid2.equals("D")) {
			// Both agents attempting to push double box
			// TODO make sure they are pushing in same direction
		} 

		// Ran into other player or any other failure case
		return sr;
	}

	public void moveAgent(State currentState, State nextState) {
		currentWorld.replace(nextState, currentWorld.get(currentState));
		// Free up current grid
		if (world.get(currentState).equals("G"))
			currentWorld.replace(currentState, "G");
		else
			currentWorld.replace(currentState, "O");
	}

	public void loadWorld(String filename) {
		int x = 0, y = 0;
		Scanner scan;
		int box = 0;
		try {
			scan = new Scanner(new File(filename));
			while (scan.hasNextLine()) {
				// Scan each row
				x = 0;
				Scanner s = new Scanner(scan.nextLine());
				while (s.hasNext()) {
					// Scan each column
					String value = s.next();
					if (verbose) System.out.printf("%d, %d, %s\n", x, y, value);
					if (value.equals("B")) {
						value += box;
						box++;
					}
					world.put(new State(x, y), value);
					if (value.equals("1"))
						start1 = new State(x, y);
					else if (value.equals("2"))
						start2 = new State(x, y);
					x++;
				}
				y++;
				s.close();
			}
			scan.close();
			// Set world height and width
			height = y;
			width = x;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public class Agent {
		public HashMap<State, Float> Q = new HashMap<State, Float>();

		public Agent() {
			// Initialize Q matrix
			for (State grid : world.keySet()) {
				for (String action : actionArray) {
					Q.put(new State(grid, action), 0.0f);
				}
			}
		}

		public String nextAction(State state, float epsilon) {
			if (rand.nextFloat() < epsilon)
				// Act randomly
				return actionArray[rand.nextInt(actionArray.length)];
			else
				// Act greedily
				return maxQ(state);
		}

		public void updateQ(State s, String a, StateReward sr) {
			// Current state s, current action a, new state sr.state, reward sr.reward
			Q.replace(new State(s, a), Q.get(new State(s, a)) + alpha*(sr.reward + gamma*Q.get(new State(sr.state, maxQ(sr.state))) - Q.get(new State(s, a))));
		}

		public String maxQ(State state) {
			// Find the action which has the largest Q value at given state
			float v = -Float.MAX_VALUE;
			String action = null;
			for (String a: actionArray) {
				Float f = Q.get(new State(state, a));
				if (f != null && f > v) {
					v = f;
					action = a;
				}
			}
			return action;
		}
	}

	public class State {
		public int x, y;
		public String action = null;
		public State(int x, int y) {
			this.x = x; this.y = y;
		}
		public State(State s, String a) {
			x = s.x; y = s.y; action = a;
		}

		public State add(State s) {
			return new State(x + s.x, y + s.y);
		}

		@Override
		public int hashCode() {
			return x + 10000*y + (action != null ? action.hashCode() : 0);
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			if (obj == this)
				return true;
			if (!(obj instanceof State))
				return false;
			State s = (State) obj;

			return x == s.x && y == s.y && ((action == null && s.action == null) || (action != null && s.action != null && action.equals(s.action)));
		}

		@Override
		public String toString() {
			return String.format("%d, %d, %s", x, y, action);
		}
	}

	public class StateReward {
		State state;
		float reward;
		public StateReward(State s, float r) {
			state = s; reward = r;
		}
	}
}
