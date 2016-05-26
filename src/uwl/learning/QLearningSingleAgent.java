package uwl.learning;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

import uwl.learning.QLearningSimple.State;
import uwl.learning.QLearningWithEpisodes.StateReward;

public class QLearningSingleAgent {
	//	public String[] actions = {"U", "D", "L", "R"};
	public HashMap<String, State> actions = new HashMap<String, State>();
	public String[] actionArray;
	public float alpha = 1.0f;
	public float gamma = 0.9f;
	public State start1;
	public HashMap<State, String> world = new HashMap<State, String>();
	public HashMap<State, String> currentWorld = new HashMap<State, String>();
	public Random rand = new Random();
	public int height, width;

	public Agent agent1;

	// public float moveP = 0.78f; // Probability to successfully move
	public float moveP = 1.0f; // Probability to successfully move
	public float pushP = 0.8f; // Probability to successfully push
	public int boxReward = 100;

	public boolean verbose = false;
	
	public int boxsInGoal = 0;

	public static void main(String[] args) {
		new QLearningSingleAgent("world.txt");
	}

	public QLearningSingleAgent(String filename) {
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

		// learningEpisodes(1000000, 500000, 10000);
		// 2000 episodes, reducing epsilon every 10 episodes
		learning(10000000);
	}

	public void learning(int iterations) {
		State s1 = start1; // Current state
		// float epsilon = 0.05f;
		float epsilon = 0.9f;
		float rewardSum = 0.0f;
		// Reset current world
		currentWorld = (HashMap<State, String>) world.clone();
		// Determine reduction rate
		float threshold = 0.005f;
		float reduction = threshold * iterations / 0.9f;

		// Loop for several movement iterations.
		for (int iteration = 0; iteration < iterations; iteration++) {
			// Reduce epsilon
			if (epsilon <= threshold) epsilon = 0.0f;
			else if (iteration >= reduction) epsilon = 0.9f / (iteration / reduction);

			// Print out current policy
			if (iteration % 5000 == 0) {
				if (verbose) 
					System.out.println("Iteration: " + iteration + ", Epsilon: " + epsilon);
				printPolicy(iteration, rewardSum);
			}

			// Determine actions, chosen epsilon-greedily based on Q(s,a)
			String a1 = agent1.nextAction(s1, epsilon);
			// Take action a, Observe next state s', one-step reward r
			StateReward[] sr = takeAction(s1, a1);
			// Update Q(s,a)
			agent1.updateQ(s1, a1, sr[0]);
			s1 = sr[0].state;
			rewardSum += sr[0].reward;
			//if (iteration % 10 == 0)
			//printWorld();
		}

		// Print out final policy
		printPolicy(iterations, rewardSum);
	}

	private float calculateAverage(ArrayList<Float> vals) {
		Float sum = 0.0f;
		if(!vals.isEmpty()) {
			for (Float val : vals) {
				sum += val;
			}
			return sum / vals.size();
		}
		return sum;
	}

	public void learningEpisodes(int episodes, int iterations, int reduction) {
		// float epsilon = 0.05f;
		float epsilon = 0.9f;
		ArrayList<Float> rewards = new ArrayList<Float>();

		for (int i = 0; i < episodes; i++) {
			// if (epsilon <= 0.1f) epsilon = 0.0f;
			// else 
			if (i >= reduction) epsilon = 0.9f / (i / reduction);

			// Print out current policy
			// Print out current policy
			if (i % 1000 == 0) {
				if (verbose) 
					System.out.println("Episode: " + i + ", Epsilon: " + epsilon);
				printPolicy(i, calculateAverage(rewards));
			}

			State s1 = start1; // Current state
			float rewardSum = 0.0f;
			// Reset current world
			currentWorld = (HashMap<State, String>) world.clone();
			boxsInGoal = 0;

			// while (iterations < ) {
			// Loop for several movement iterations.
			while (boxsInGoal < 2) {
//			for (int iteration = 0; iteration < iterations; iteration++) {
				// Determine actions, chosen epsilon-greedily based on Q(s,a)
				String a1 = agent1.nextAction(s1, epsilon);
				// Take action a, Observe next state s', one-step reward r
				StateReward[] sr = takeAction(s1, a1);
				// Update Q(s,a)
				agent1.updateQ(s1, a1, sr[0]);
				s1 = sr[0].state;
				rewardSum += sr[0].reward;
			}
			// System.exit(0);
			rewards.add(rewardSum);
		}
		// Print out final policy
		printPolicy(episodes, calculateAverage(rewards));
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

	public void printPolicy(int iteration, float rewardSum) {
		System.out.println("\nIteration: " + iteration + ", " + "Reward sum: " + rewardSum);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				System.out.print(agent1.nextAction(new State(x, y), 0) + " ");
			System.out.print("  |  ");
			for (int x = 0; x < width; x++)
				System.out.print(currentWorld.get(new State(x, y)) + " ");
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

	// This is part of the environment, with rewards and states being sent back to each agent.
	public StateReward[] takeAction(State state1, String action1) {
		float p1 = rand.nextFloat();
		String currentGrid1 = currentWorld.get(state1);

		State nextState1 = state1.add(actions.get(action1));

		String nextGrid1 = currentWorld.get(nextState1);

		StateReward sr[] = new StateReward[2];
		sr[0] = new StateReward(state1, -1); 

		if (nextGrid1 != null && nextGrid1.contains("B") && p1 <= pushP && action1.equals("U")) {
			// Push successful
			// Check that the box can actually move into another grid
			State nextBoxState1 = nextState1.add(actions.get(action1));
			String nextBoxGrid1 = currentWorld.get(nextBoxState1);
			// Make sure box can move
			if (nextBoxGrid1 == null)
				return sr;

			// Verify box does not push into the left, right, or bottom grids (box would be stuck)
			if (nextBoxState1.x > 0 && nextBoxState1.x < width - 1 && nextBoxState1.y < height - 1) {
				if (nextBoxGrid1.equals("O")) {
					// The box can be moved successfully!
					// Move box to next grid
					currentWorld.replace(nextBoxState1, currentWorld.get(nextState1));
					// Move agent to grid where box was
					moveAgent(state1, nextState1);
					sr[0] = new StateReward(nextState1, -1);
				} else if (nextBoxGrid1.equals("G")) {
					boxsInGoal++;
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
					// Move box into goal only if the box can teleport back to original location.
					if (origBoxGrid.equals("O")) {
						// Box can move back to starting point
						currentWorld.replace(origBoxState, currentWorld.get(nextState1));
						// Move agent to grid where box was
						moveAgent(state1, nextState1);
						sr[0] = new StateReward(nextState1, boxReward);
					} //else
					// Box cannot teleport back to beginning location. Move box into goal grid
					//currentWorld.replace(nextBoxState1, currentWorld.get(nextState1));
				}
			}
		} 

		if (nextGrid1 != null && (nextGrid1.equals("O") || nextGrid1.equals("G"))) {
			// Move agent to next grid
			moveAgent(state1, nextState1);
			sr[0] = new StateReward(nextState1, -1);
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
