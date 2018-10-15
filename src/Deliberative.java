/* import table */
import logist.simulation.Vehicle;

import java.util.HashSet;

import logist.agent.Agent;
import logist.behavior.DeliberativeBehavior;
import logist.plan.Plan;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.task.TaskSet;
import logist.topology.Topology;
import logist.topology.Topology.City;

/**
 * An optimal planner for one vehicle.
 */
@SuppressWarnings("unused")
public class Deliberative implements DeliberativeBehavior {

	enum Algorithm { BFS, ASTAR }
	
	/* Environment */
	Topology topology;
	TaskDistribution td;
	
	/* the properties of the agent */
	Agent agent;
	int capacity;

	/* the planning class */
	Algorithm algorithm;
	
	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {
		this.topology = topology;
		this.td = td;
		this.agent = agent;
		
		// initialize the planner
		int capacity = agent.vehicles().get(0).capacity();
		String algorithmName = agent.readProperty("algorithm", String.class, "BFS");
		
		// Throws IllegalArgumentException if algorithm is unknown
		algorithm = Algorithm.valueOf(algorithmName.toUpperCase());
		
		// ...
	}
	
	@Override
	public Plan plan(Vehicle vehicle, TaskSet tasks) {
		Plan plan;

		//generate tree
		City currentCity = vehicle.getCurrentCity();
		HashSet<Task> carriedTasks = new HashSet<Task>();
		State state = new State(currentCity,tasks,carriedTasks);
		Tree tree = new Tree(state,vehicle.capacity());
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
//			State state = new State(vehicle.getCurrentCity(), tasks, new HashSet<Task>());
//			Tree tree = new Tree(state, vehicle.capacity());
			AstarPlanWithRandomHeuristic astar = new AstarPlanWithRandomHeuristic(tree);
			plan = astar.getPlan();
			break;
		case BFS:
			// ...
			plan = bfsPlan(tree);
			break;
		default:
			throw new AssertionError("Should not happen.");
		}		
		
		//TESTS ARE BEING MADE HERE. SERIOUS STUFF
		State currentState = new State(vehicle.getCurrentCity(), tasks, new HashSet<Task>());
		System.out.println(currentState.getTasksToPickUp());
		return plan;
	}
	
	private Plan naivePlan(Vehicle vehicle, TaskSet tasks) {
		City current = vehicle.getCurrentCity();
		Plan plan = new Plan(current);

		for (Task task : tasks) {
			// move: current city => pickup location
			for (City city : current.pathTo(task.pickupCity))
				plan.appendMove(city);

			plan.appendPickup(task);

			// move: pickup location => delivery location
			for (City city : task.path())
				plan.appendMove(city);

			plan.appendDelivery(task);

			// set current city
			current = task.deliveryCity;
		}
		return plan;
	}
	
	private Plan bfsPlan(Tree _tree){
		BreadthFirstSearch bfs = new BreadthFirstSearch(_tree);
		Plan plan = bfs.getBestPlan();
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		
		if (!carriedTasks.isEmpty()) {
			// This cannot happen for this simple agent, but typically
			// you will need to consider the carriedTasks when the next
			// plan is computed.
		}
	}
}