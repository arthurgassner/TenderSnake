
/* import table */
import logist.simulation.Vehicle;

import java.util.ArrayList;
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

	enum Algorithm {
		BFS, ASTAR
	}

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
		
		//This is super ugly TODO make it pretty
		ArrayList<Task> a = new ArrayList<Task>();
		for (Object task : vehicle.getCurrentTasks().toArray()) {
			a.add((Task) task);
		}
		HashSet<Task> carriedTasks = new HashSet<Task>();
		carriedTasks.addAll(a);
		State state = new State(currentCity,tasks,carriedTasks);
		Tree tree = null;
		
		// Compute the plan with the selected algorithm.
		switch (algorithm) {
		case ASTAR:
			tree = new Tree(state,vehicle.capacity(), false);
			AstarPlanWithUnderestimatingHeuristic astar = new AstarPlanWithUnderestimatingHeuristic(tree);
			astar.computePlan();
			plan = astar.getPlan();
			System.out.println(plan);
			break;
		case BFS:
			tree = new Tree(state,vehicle.capacity(), true);
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
		Plan plan = null;
		BreadthFirstSearch bfs = new BreadthFirstSearch(_tree);
		boolean planFound = bfs.determineMasterPlan();
		if(planFound){
			plan = bfs.getBestPlan();
		}
			
		return plan;
	}

	@Override
	public void planCancelled(TaskSet carriedTasks) {
		// Nothing needs to be done here
	}
}