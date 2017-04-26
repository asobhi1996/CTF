package ctf.agent;

import static java.lang.System.out;

import ctf.common.AgentAction;
import ctf.common.AgentEnvironment;
import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

public class srs140430Agent extends Agent {
    Random rand = new Random();
    final int unvisited = 0;
    final int empty = 1;
    final int obstacle = 2;
    final int mybase = 3;
    final int theirbase = 4;
    final int bfsvisited = 5;

    class Board {
	int size;
	int[][] b;

	Point mybasepoint;
	Point theirbasepoint;

	Board(int size, boolean left) {
	    this.size = size;
	    b = new int[size][size];

	    for (int i = 0; i < size; i++) {
		b[0][i] = empty;
		b[size - 1][i] = empty;
	    }

	    if (left) {
		b[0][(size - 1) / 2] = mybase;
		mybasepoint = new Point(0, (size - 1) / 2);
		b[size - 1][(size - 1) / 2] = theirbase;
		theirbasepoint = new Point(size - 1, (size - 1) / 2);
	    } else {
		b[size - 1][(size - 1) / 2] = mybase;
		mybasepoint = new Point(size - 1, (size - 1) / 2);
		b[0][(size - 1) / 2] = theirbase;
		theirbasepoint = new Point(0, (size - 1) / 2);
	    }
	}

	public boolean reachable(Point target, Point from){
	    return target == from || getPath(from, target).size() != 1;
	}
	
	public LinkedList<Point> getPath(Point from, Point to) {

	    int[][] visited = new int[size][size];
	    int[][] cum_cost = new int[size][size];
	    for (int i = 0; i < size; i++)
		for (int j = 0; j < size; j++) cum_cost[i][j] = Integer.MAX_VALUE / 2;
	    for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) visited[i][j] = b[i][j];
	    Point[][] pred = new Point[size][size];

	    LinkedList<Point> Q = new LinkedList<Point>();

	    visited[from.x][from.y] = bfsvisited;
	    cum_cost[from.x][from.y] = 0;
	    pred[from.x][from.y] = null;
	    Q.add(from);

	    while (!Q.isEmpty()) {
		Point min_p = Q.peek();
		int min_cost = cum_cost[min_p.x][min_p.y] + distance(min_p, to);

		for (Point p : Q) {
		    int cost = cum_cost[p.x][p.y] + distance(p, to);
		    if (cost < min_cost) {
			min_p = p;
			min_cost = cost;
		    }
		}
		if (min_p.equals(to)) break;

		Q.remove(min_p);
		visited[min_p.x][min_p.y] = bfsvisited;
		ArrayList<Point> neighbors = neighbors(min_p);

		for (Point neighbor : neighbors) {
		    if (!Q.contains(neighbor) && visited[neighbor.x][neighbor.y] == empty) Q.add(neighbor);

		    int new_cost = cum_cost[min_p.x][min_p.y] + 1 + distance(min_p, to);
		    int old_cost = cum_cost[neighbor.x][neighbor.y] + distance(neighbor, to);

		    if (new_cost < old_cost) {
			cum_cost[neighbor.x][neighbor.y] = cum_cost[min_p.x][min_p.y] + 1;
			pred[neighbor.x][neighbor.y] = min_p;
		    }
		}
	    }

	    LinkedList<Point> path = new LinkedList<>();
	    Point p = to;

	    while (p != null) {
		path.push(p);
		p = pred[p.x][p.y];
	    }
	    return path;
	}

	public int evalPoint(Point point, Point current, Point goal) {
	    //TODO: avoid unvisited nodes with lots of barriers around them
	    int dist_pg = distance(point, goal);
	    int dist_pc = getPath(current, point).size();

	    // count number of unvisited nodes near target node;
	    int unvisited_neighbors = 0;
	    int obstacles_nearby = 0;
	    int radius = 1;
	    for (int i = -radius; i <= radius; i++) {
		for (int j = -radius; j <= radius; j++) {
		    int neighborx = point.x + i;
		    int neighbory = point.y + j;
		    if (neighborx >= size || neighbory >= size) continue;
		    if (neighborx < 0 || neighbory < 0) continue;
		    if (b[neighborx][neighbory] == unvisited) unvisited_neighbors += 1;
		    else if (b[neighborx][neighbory] == obstacle) obstacles_nearby += 1;
		}
	    }

	    return dist_pc + dist_pg - unvisited_neighbors + obstacles_nearby;
	}

	public ArrayList<Point> getReachableUnvisited(Point from) {
	    int[][] visited = new int[size][size];
	    for (int i = 0; i < size; i++) for (int j = 0; j < size; j++) visited[i][j] = b[i][j];

	    LinkedList<Point> Q = new LinkedList<Point>();
	    ArrayList<Point> reachable = new ArrayList<Point>();

	    visited[from.x][from.y] = bfsvisited;
	    Q.add(from);

	    while (!Q.isEmpty()) {
		Point p = Q.pop();
		visited[p.x][p.y] = bfsvisited;
		ArrayList<Point> neighbors = neighbors(p);
		for (Point neighbor : neighbors) {
		    if (visited[neighbor.x][neighbor.y] == unvisited) {
			reachable.add(neighbor);
			visited[neighbor.x][neighbor.y] = bfsvisited;
		    }
		    if (visited[neighbor.x][neighbor.y] == empty && !Q.contains(neighbor)) Q.add(neighbor);
		}
	    }

	    return reachable;
	}

	public ArrayList<Point> neighbors(Point p) {
	    int x = p.x;
	    int y = p.y;
	    ArrayList<Point> neighborz = new ArrayList<>();
	    if (x - 1 >= 0) neighborz.add(new Point(x - 1, y));
	    if (x + 1 < size) neighborz.add(new Point(x + 1, y));
	    if (y - 1 >= 0) neighborz.add(new Point(x, y - 1));
	    if (y + 1 < size) neighborz.add(new Point(x, y + 1));

	    return neighborz;
	}

	public String toString() {
	    StringBuilder s = new StringBuilder();
	    for (int i = size - 1; i >= 0; i--) {
		for (int j = 0; j < size; j++) {
		    s.append(b[j][i]);
		    s.append(' ');
		}
		s.append('\n');
	    }

	    return s.toString();
	}
    }

    static int distance(Point p1, Point p2) {
	return Math.abs(p1.x - p2.x) + Math.abs(p1.y - p2.y);
    }

    static int n_agents = 0;
    static Board board;
    static int size = 0;
    static boolean doneSizing = false;
    static int turn = 0;
    static srs140430Agent oddAgent;
    static srs140430Agent evenAgent;

    boolean topAgent, bottomAgent;
    boolean leftTeam, rightTeam;
    boolean determineSize = false;
    boolean newGame = true;
    int id;
    Point coord;
    Point prev;
    Point goal;
    LinkedList<Point> path;

    public srs140430Agent() {
	n_agents += 1;
	id = n_agents;
	board = null;
	size = 2;
	doneSizing = false;
	turn = 0;

	if (id % 2 == 0) evenAgent = this;
	else oddAgent = this;
    }

    public int getMove(AgentEnvironment env) {
	if (newGame) newGameSetup(env);
	if (bottomAgent) turn += 1;
	if (determineSize) return investigateSize(env);

	if (coord == null) {
	    out.println("COORD IS NULL");
	    if (env.isBaseSouth(0, true)) coord = new Point(homeColumn(), (board.size - 1) / 2 + 1);
	    else coord = new Point(homeColumn(), (board.size - 1) / 2 - 1);
	}
	out.println("*************");
	//out.print(board);

	if (topAgent) {
	    out.println("Top Agent's Move:");
	}

	if (bottomAgent) {
	    out.println("Bottom Agent's Move:");
	}

	checkIfReset(env);
	checkForObstacles(env);
	int move = computeNextMove(env);

	if (isBlocked(move, env)) move = AgentAction.DO_NOTHING;

	prev = coord;
	coord = newCoord(move);
	// out.printf("%s %d%n", coord, move);

	return move;
    }

    int computeNextMove(AgentEnvironment env) {
	if (/*bottomAgent &&*/ goal == null) goal = board.theirbasepoint;
	//if (topAgent && goal == null) goal = new Point(board.mybasepoint.x, board.mybasepoint.y - 1);

	if (env.hasFlag()){
	    goal = board.mybasepoint;
	    path = null;
	}

	if(goal == board.mybasepoint && !env.hasFlag()){
	    goal = board.theirbasepoint;
	    path = null;
	}
	

	if(path != null){
	    Point target = path.peekLast();
	    if(board.b[target.x][target.y] != unvisited){
		out.println("TARGET IS ALREAD VISITED");
		path = null;
	    }
	    else if (coord.equals(path.peek())) {
		out.println("FOLLOWING PATH");
		path.pop();
		if(!path.isEmpty())
		    return actionFromPoints(coord, path.peek());
		else
		    path = null;
	    }
	    else 
		path = null;
	}

	//recompute path
	if (path == null) {
	    out.println("COMPUTING PATH TO NEW TARGET");
	    int min_cost = Integer.MAX_VALUE;
	    Point target = null;
	    if(!board.reachable(goal, coord)){
		for (Point p : board.getReachableUnvisited(coord)) {
		    if (board.evalPoint(p, coord, goal) < min_cost){ target = p;
			min_cost = board.evalPoint(p, coord, goal);
		    }
		}
	    }
	    else{
		out.println("GOAL IS REACHABLE");
		target = goal;
	    }
	    out.printf("NEW TARGET: %s%n",target);
	    path = board.getPath(coord, target);
	}

	out.printf("%s %s%n", coord, path);

	if (coord.equals(path.peek())) {
	    out.println("FOLLOWING PATH");
	    path.pop();
	    if(!path.isEmpty())
		return actionFromPoints(coord, path.peek());
	    else
		path = null;
	}

	out.println("RETURNING RANDOM");

	return rand.nextInt(4);
    }

    int actionFromPoints(Point from, Point to) {
	int diffx = to.x - from.x;
	int diffy = to.y - from.y;

	switch (diffx) {
	case 1:
	    return AgentAction.MOVE_EAST;
	case -1:
	    return AgentAction.MOVE_WEST;
	}

	switch (diffy) {
	case 1:
	    return AgentAction.MOVE_NORTH;
	case -1:
	    return AgentAction.MOVE_SOUTH;
	}

	return AgentAction.DO_NOTHING;
    }

    int xOffset(int move) {
	switch (move) {
	case AgentAction.MOVE_EAST:
	    return 1;
	case AgentAction.MOVE_WEST:
	    return -1;
	default:
	    return 0;
	}
    }

    int yOffset(int move) {
	switch (move) {
	case AgentAction.MOVE_NORTH:
	    return 1;
	case AgentAction.MOVE_SOUTH:
	    return -1;
	default:
	    return 0;
	}
    }

    Point newCoord(int move) {
	return new Point(coord.x + xOffset(move), coord.y + yOffset(move));
    }

    boolean isBlocked(int move, AgentEnvironment env) {
	srs140430Agent other = getOtherAgent();
	boolean flagSafe = !env.hasFlag(AgentEnvironment.ENEMY_TEAM);
	assert other != this;
	Point temp = newCoord(move);

	if (temp.equals(other.coord)) return true;

	switch (move) {
	case AgentAction.MOVE_EAST:
	    assert (temp.equals(other.coord)) == env.isAgentEast(AgentEnvironment.OUR_TEAM, true);
	    return env.isObstacleEastImmediate() || (env.isFlagEast(AgentEnvironment.OUR_TEAM, true) && !env.hasFlag());

	case AgentAction.MOVE_WEST:
	    assert (temp.equals(other.coord)) == env.isAgentWest(AgentEnvironment.OUR_TEAM, true);
	    return env.isObstacleWestImmediate() || (env.isFlagWest(AgentEnvironment.OUR_TEAM, true) && !env.hasFlag());

	case AgentAction.MOVE_NORTH:
	    assert (temp.equals(other.coord)) == env.isAgentNorth(AgentEnvironment.OUR_TEAM, true);
	    return env.isObstacleNorthImmediate() || (env.isFlagNorth(AgentEnvironment.OUR_TEAM, true) && !env.hasFlag());

	case AgentAction.MOVE_SOUTH:
	    assert (temp.equals(other.coord)) == env.isAgentSouth(AgentEnvironment.OUR_TEAM, true);
	    return env.isObstacleSouthImmediate() || (env.isFlagSouth(AgentEnvironment.OUR_TEAM, true) && !env.hasFlag());

	case AgentAction.DO_NOTHING:
	    return false;
	}
	assert false;
	return true;
    }

    void checkForObstacles(AgentEnvironment env) {
	if (coord.x - 1 >= 0) {
	    boolean obst = env.isObstacleWestImmediate();
	    int val = obst ? obstacle : empty;
	    if (board.b[coord.x - 1][coord.y] == unvisited) board.b[coord.x - 1][coord.y] = val;
	}
	if (coord.x + 1 < size) {
	    boolean obst = env.isObstacleEastImmediate();
	    int val = obst ? obstacle : empty;
	    if (board.b[coord.x + 1][coord.y] == unvisited) board.b[coord.x + 1][coord.y] = val;
	}
	if (coord.y - 1 >= 0) {
	    boolean obst = env.isObstacleSouthImmediate();
	    int val = obst ? obstacle : empty;
	    if (board.b[coord.x][coord.y - 1] == unvisited) board.b[coord.x][coord.y - 1] = val;
	}
	if (coord.y + 1 < size) {
	    boolean obst = env.isObstacleNorthImmediate();
	    int val = obst ? obstacle : empty;
	    if (board.b[coord.x][coord.y + 1] == unvisited) board.b[coord.x][coord.y + 1] = val;
	}
    }

    void checkIfReset(AgentEnvironment env) {
	if (onHomeColumn(env)) {
	    if (topAgent && env.isObstacleNorthImmediate())
		coord = new Point(homeColumn(), board.size - 1);
	    else if (bottomAgent && env.isObstacleSouthImmediate()) coord = new Point(homeColumn(), 0);
	}
    }

    void newGameSetup(AgentEnvironment env) {
	if (env.isObstacleSouthImmediate()) bottomAgent = true;
	else topAgent = true;

	leftTeam = env.isBaseEast(1, false);
	rightTeam = !leftTeam;

	newGame = false;
	determineSize = true;
    }

    int investigateSize(AgentEnvironment env) {
	if (topAgent && !env.isFlagSouth(0, true)) {
	    size += 1;
	    return AgentAction.MOVE_SOUTH;
	}
	if (bottomAgent && !env.isFlagNorth(0, true)) {
	    size += 1;
	    return AgentAction.MOVE_NORTH;
	}
	if (!doneSizing) {
	    doneSizing = true;
	    determineSize = false;
	    return AgentAction.DO_NOTHING;
	}

	determineSize = false;
	size += 1;
	board = new Board(size, leftTeam);

	return AgentAction.DO_NOTHING;
    }

    boolean onHomeColumn(AgentEnvironment env) {
	if (leftTeam) return !env.isBaseWest(0, false);

	return !env.isBaseEast(0, false);
    }

    srs140430Agent getOtherAgent() {
	if (id % 2 == 0) return oddAgent;
	else return evenAgent;
    }

    int homeColumn() {
	return leftTeam ? 0 : board.size - 1;
    }
}