package uwl.learning;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;

public class QLearningOld {
//	public String[] actions = {"U", "D", "L", "R"};
	public HashMap<String, State> actions = new HashMap<String, State>();
	public String[] actionArray;
	public float alpha = 1.0f;
	public float gamma = 0.9f;
	public State start;
	public HashMap<State, String> world = new HashMap<State, String>();
	public HashMap<State, Float> Q = new HashMap<State, Float>();
	public Random rand = new Random();
	public int height, width;
	
	public float moveP = 0.78f; // Probability to successfully move
//	public float moveP = 1.0f; // Probability to successfully move
	public float pushP = 0.6f; // Probability to successfully push

	public boolean verbose = false;

	public static void main(String[] args) {
		new QLearningOld("world.txt");
	}
	
	public QLearningOld(String filename) {
//		System.out.println((new State(1, 3)).equals(new State(1, 3)));
//		System.out.println((new State(2, 3, "U")).equals(new State(2, 3, "U")));
//		System.out.println((new State(1, 3)).equals(new State(1, 4)));
//		System.out.println((new State(2, 3, "U")).equals(new State(2, 3, "D")));
		
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
		
		// 2000 episodes, reducing epsilon every 1 episodes
		learning(2000, 10);
	}
	
	public void learning(int episodes, int reduction) {
		// Initialize Q matrix
		for (State grid : world.keySet()) {
			for (String action : actionArray) {
				Q.put(new State(grid, action), 0.0f);
			}
		}

		float epsilon = 0.9f;
		ArrayList<Float> rewards = new ArrayList<Float>();
		
		for (int i = 0; i < episodes; i++) {
			if (epsilon <= 0.01f) epsilon = 0.0f;
			else if (i >= reduction) epsilon = 0.9f / (i / reduction);
			
			// Print out current policy
			if (verbose) System.out.println("Episode: " + i + ", Epsilon: " + epsilon);
			if (i % 100 == 0)
				printPolicy(i);
			
			State s = start; // Current state
			float rewardSum = 0.0f;
			
			while (!world.get(s).equals("G")) {
				// Determine action a, chosen epsilon-greedily based on Q(s,a)
				String a = nextAction(s, epsilon);
				// Take action a, Observe next state s', one-step reward r
//				System.out.println("First: " + s);
				StateReward sr = takeAction(s, a);
//				System.out.println("Second: " + s + ", " + a);
//				System.out.println("Next: " + sr.state + ", Reward: " + sr.reward);
//				System.exit(0);
				// Update Q(s,a)
				Q.replace(new State(s, a), Q.get(new State(s, a)) + alpha*(sr.reward + gamma*Q.get(new State(sr.state, maxQ(sr.state))) - Q.get(new State(s, a))));
				s = sr.state;
				rewardSum += sr.reward;
			}
			rewards.add(rewardSum);
		}
		// Print out final policy
		printPolicy(episodes);
	}
	
	public void printPolicy(int episode) {
		System.out.println("\nEpisode: " + episode);
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++)
				System.out.print(nextAction(new State(x, y), 0) + " ");
			System.out.println();
		}
		System.out.println();
	}
	
	public String nextAction(State state, float epsilon) {
		if (rand.nextFloat() < epsilon)
			// Act randomly
			return actionArray[rand.nextInt(actionArray.length)];
		else
			// Act greedily
			return maxQ(state);
	}
	
	public StateReward takeAction(State state, String action) {
		if (world.get(state).equals("G"))
			// In the goal!
			return new StateReward(state, 50 - 1);
//		System.out.println("Original state: " + state);
		State nextState = state.add(actions.get(action));
//		System.out.println("Next state: " + nextState);
		
		String nextGrid = world.get(nextState);
//		System.out.println("World Grid: " + worldGrid);
		if (nextGrid == null) {
			// Unable to leave grid
			return new StateReward(state, -1);
		} else if (nextGrid.equals("B")) {
			// Attempting to push box
			return new StateReward(state, -1);
		} else if (nextGrid.equals("D")) {
			// Attempting to push double box
			return new StateReward(state, -1);
		} else if (nextGrid.equals("O") || nextGrid.equals("G")) {
			// Attempting to move
			if (rand.nextFloat() <= moveP) {
				// Move successful
				if (nextGrid.equals("G"))
					return new StateReward(nextState, 50 - 1);
				else
					return new StateReward(nextState, -1);
			} else
				// Move failed
				return new StateReward(state, -1);
		}
		// Ran into other player or any other failure case
		return new StateReward(state, -1);
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
	
	public void loadWorld(String filename) {
		int x = 0, y = 0;
		Scanner scan;
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
					world.put(new State(x, y), value);
					if (value.equals("1"))
						start = new State(x, y);
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
	
	public class State {
		int x, y;
		String action = null;
		public State(int x, int y) {
			this.x = x; this.y = y;
		}
		public State(int x, int y, String a) {
			this.x = x; this.y = y; action = a;
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
