package ctf.agent;

import java.util.*;
import ctf.common.AgentEnvironment;
import ctf.agent.Agent;
import ctf.common.AgentAction;

public class ars140330Agent extends Agent {
	public static final Integer[] moveList = {AgentAction.MOVE_NORTH,AgentAction.MOVE_SOUTH,AgentAction.MOVE_EAST,AgentAction.MOVE_WEST};
	public static AgentEnvironment inEnvironment;
	public static int boardSize = 1;
	public static int id = 0;
	public static byte[][] map;
	public static int[][] tagged;
	public static boolean[][] mine;
	public static String ourSide;
	public static boolean[] boardSizeKnown, positionKnown;
	public static Position topDefendingPosition, bottomDefendingPosition, topAttackingPosition, bottomAttackingPosition, defenseGoal;
	public static Position enemyFlagPosition, ourFlagPosition;
	public static Set<Position> badPositions = new HashSet<Position>();
	public static ArrayList<Position> defenseMinePositions;
	public static boolean minesPlanted, enemyFlagGoal,defensePositionsFound, initailizedMapArray;
	public static boolean everyThingOk;
	public static int mineColumn;
	public static byte[][] mapCopy;
	public LinkedList<Integer> path; //general purpose path
	public ArrayList<Byte> beginningObstacles; //as we make our intitial path to our flag, see if things on the penultimate column are blocked
	public int agentId; //used to identify the two agents
	public int beginningSteps; //used to calculate boardSize
	public int startingCorner; //0 indicates NorthWest, 1 NorthEast, 2 SouthWest, 3 Southeast
	public Position position; //holds the x,y coordinates of the agent
	public boolean cornerKnown;; //flag for if agent knows its starting corner
	public int moves, offenseMineColumn;
	public boolean initialMapUpdateFlag;
	public boolean onRouteToGoal;
	public Position goal;
	public boolean selfDestruct;
	public int[][] enemyPositions;
	public boolean onPath, offenseMinePlanted, plantMine;
	public Position offenseMine;
	public int counter;
	public boolean attackingInitialBomb;

	public ars140330Agent(){
		moves = 0;
		counter = 0;
		offenseMine = null;
		mapCopy = null;
		onPath = false;
		selfDestruct = false;
		boardSizeKnown = new boolean[2];
		positionKnown = new boolean[2];
		defenseGoal = new Position();
		boardSize = 1;
		this.agentId = id++;
		if (id == 2)
			id = 0;
		boardSizeKnown[this.agentId] = false;
		positionKnown[this.agentId] = false;
		enemyFlagPosition = null;
		ourFlagPosition = null;
		badPositions = new HashSet<Position>();
		map = null;
		path = new LinkedList<Integer>();
		beginningSteps = 0;
		startingCorner = -1;
		position = null;
		cornerKnown = false;
		initailizedMapArray = false;
		beginningSteps = 0;
		startingCorner = -1;
		ourSide = null;
		enemyFlagGoal = true;
		topDefendingPosition = null;
		bottomDefendingPosition = null;
		defensePositionsFound = false;
		beginningObstacles = new ArrayList<Byte>();
		initialMapUpdateFlag = false;
		onRouteToGoal = false;
		goal = new Position();;
		tagged = null;
		mine = null;
		topDefendingPosition = null;
		minesPlanted = false;
		topAttackingPosition = null;
		defenseMinePositions = null;
		enemyPositions = new int[boardSize][boardSize];
		everyThingOk = true;
		mineColumn = 0;
		moves = 0;
		offenseMinePlanted = false;
		minesPlanted = false;
		offenseMineColumn = -2;
		attackingInitialBomb = false;
	}
	private class Position implements Comparable<Position> {
		public int row, column,direction, tagged;
		public double distance, cost;
		public Position parent;
		Position(){
			this.row = -1;
			this.column = -1;
		}
		Position(int row, int column) {
			this.row = row;
			this.column = column;
		}
		Position(Position other) {
			this.row = other.row;
			this.column = other.column;
		}
		public void set(int row, int column){
			this.row = row;
			this.column = column;
		}
		public void set(Position pos) {
			this.row = pos.row;
			this.column = pos.column;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null)
				return false;
			Position pos = (Position) obj;
			if (this.row != pos.row || this.column != pos.column)
				return false;
			return true;
		}
		@Override
	    public int hashCode() {
	    	//Cantor's Pairing Function, maps two ordered numbers to unique number
	        return (int)(.5 *(row + column) *(row + column + 1)) + column;
	    }
	    @Override
	    public int compareTo(Position other){
	        if (this.cost < other.cost)
	        	return -1;
	        else if (this.cost > other.cost)
	        	return 1;
	        else
	        	return 0;
  			}
  		@Override
  		public String toString() { 
    		return "Position: (" + this.row + "," + this.column + ")";
		} 
	}
	public boolean positionChanger(Position position,int direction) {
		if (direction == AgentAction.MOVE_NORTH)
			position.set(position.row-1,position.column);
		else if (direction == AgentAction.MOVE_SOUTH) 
			position.set(position.row+1,position.column);
		else if (direction == AgentAction.MOVE_EAST)
			position.set(position.row,position.column+1);
		else if (direction == AgentAction.MOVE_WEST)
			position.set(position.row,position.column-1);
		if (position.row < 0 || position.row >= boardSize || position.column < 0 || position.column >= boardSize)
			return false;
		else
			return true;
	}
	public Position positionMaker(Position position, int direction){
		int row = position.row;
		int column = position.column;
		if (direction == AgentAction.MOVE_NORTH){
			row -=1;
		}
		else if (direction == AgentAction.MOVE_SOUTH) { 
			row +=1;
		}
		else if (direction == AgentAction.MOVE_EAST) {
			column += 1;
		}
		else if (direction == AgentAction.MOVE_WEST) {
			column -=1;
		}
		if (row < 0 || row >= boardSize || column < 0 || column >= boardSize)
			return null;
		return new Position(row,column);
	}
	public int move(AgentEnvironment inEnvironment) {
		Position[] neighbors = new Position[4];
		Boolean[] valid = {true,true,true,true};
		Collections.shuffle(Arrays.asList(moveList));
		int direction;
		//generate neighbors and check for validity
		for(int i = 0;i<4;i++){
			direction = moveList[i];
			neighbors[direction] = positionMaker(position,direction);
			if (neighbors[direction] == null || !noObstacles(direction,inEnvironment) || badPositions.contains(neighbors[direction]) ||                  !teammateNotInWay(direction,inEnvironment) || !notBabysitting(direction,inEnvironment))
				valid[direction] = false;
		}
		//see if there is at least one valid new position
		int validNeighbors = 0;
		for(int i = 0;i<4;i++){
			direction = moveList[i];
			if(valid[direction]){
				validNeighbors++;
			}
		}
		if (validNeighbors == 0) {
			selfDestruct = true;
			return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
		}
		return AgentAction.DO_NOTHING;
	}
	public boolean notBabysitting(int direction, AgentEnvironment inEnvironment){
		if (!inEnvironment.hasFlag() &&
			(direction == AgentAction.MOVE_NORTH &&
				inEnvironment.isFlagNorth(AgentEnvironment.OUR_TEAM,true) && 
				inEnvironment.isBaseNorth(AgentEnvironment.OUR_TEAM,true)) ||
			(direction == AgentAction.MOVE_SOUTH && 
				inEnvironment.isFlagSouth(AgentEnvironment.OUR_TEAM,true) && 
				inEnvironment.isBaseSouth(AgentEnvironment.OUR_TEAM,true)) ||
			(direction == AgentAction.MOVE_EAST &&
				inEnvironment.isFlagEast(AgentEnvironment.OUR_TEAM,true) &&
				inEnvironment.isBaseEast(AgentEnvironment.OUR_TEAM,true)) ||
			(direction == AgentAction.MOVE_WEST &&
				inEnvironment.isFlagWest(AgentEnvironment.OUR_TEAM,true) &&
				inEnvironment.isBaseWest(AgentEnvironment.OUR_TEAM,true)))
			return false;
		return true;
	}
	public boolean notObstructed(int direction,AgentEnvironment inEnvironment){
		if (!teammateNotInWay(direction,inEnvironment))
			return false;
		//check if not obstructeed by obstacle
		if (!noObstacles(direction,inEnvironment))
			return false;
		//check if we're tryingto move to our base with the flag on it
		if (!notBabysitting(direction,inEnvironment))
			return false;

		return true;
	}
	public boolean noObstacles(int direction,AgentEnvironment inEnvironment){
		boolean obstNorth = inEnvironment.isObstacleNorthImmediate();
		boolean obstSouth = inEnvironment.isObstacleSouthImmediate();
		boolean obstEast = inEnvironment.isObstacleEastImmediate();
		boolean obstWest = inEnvironment.isObstacleWestImmediate();
		if( (direction == AgentAction.MOVE_NORTH && !obstNorth) || (direction == AgentAction.MOVE_SOUTH && !obstSouth) 
			|| (direction == AgentAction.MOVE_EAST && !obstEast) || (direction == AgentAction.MOVE_WEST && !obstWest) || (direction == AgentAction.DO_NOTHING))
			return true;
		return false;
	}
	public boolean teammateNotInWay(int direction,AgentEnvironment inEnvironment) {
		boolean teammateNorth = inEnvironment.isAgentNorth(AgentEnvironment.OUR_TEAM,true);
		boolean teammateSouth = inEnvironment.isAgentSouth(AgentEnvironment.OUR_TEAM,true);
		boolean teammateEast = inEnvironment.isAgentEast(AgentEnvironment.OUR_TEAM,true);
		boolean teammateWest = inEnvironment.isAgentWest(AgentEnvironment.OUR_TEAM,true);
		if( (direction == AgentAction.MOVE_NORTH && !teammateNorth) || (direction == AgentAction.MOVE_SOUTH && !teammateSouth) 
			|| (direction == AgentAction.MOVE_EAST && !teammateEast) || (direction == AgentAction.MOVE_WEST && !teammateWest) || (direction == AgentAction.DO_NOTHING))
			return true;
		return false;
	}
	public void returnToStart() {
		if (agentId == 0)
			defenseGoal = topDefendingPosition;
		if (agentId == 1 && positionKnown[0] && positionKnown[1] && !position.equals(enemyFlagPosition))
			tagged[position.row][position.column]++;
		onPath = false;
		moves = 0;
		offenseMinePlanted = false;
		if (agentId == 1 && mine != null)
			mine[enemyFlagPosition.row][offenseMineColumn] = false;
		if (startingCorner == 0){
			position.row = 0;
			position.column = 0;
		}
		else if (startingCorner == 1){
			position.row = 0;
			position.column = boardSize-1;
		}
		else if (startingCorner == 2){
			position.row = boardSize-1;
			position.column = 0;
		}
		else {
			position.row = boardSize-1;
			position.column = boardSize-1;
		}
		path = new LinkedList<Integer>();
		if (agentId == 1) {
			for (int i = 0;i<boardSize;i++){
				if (ourSide.equals("left")){
					map[i][boardSize-1] = 0;
				}
				else{
					map[i][0] = 0;
				}
			}
			enemyFlagGoal = true;
		}
	}
	public int getNextMoveFromPath(AgentEnvironment inEnvironment){
		Integer direction =(Integer) path.pollFirst();
		if (notObstructed(direction,inEnvironment)){
			positionChanger(position,direction);
			return direction;
		}
		else {
			path.addFirst(direction);
			return AgentAction.DO_NOTHING;
		}
	}
	public int tryNextMoveFromPath(AgentEnvironment inEnvironment){
		Integer direction =(Integer) path.pollFirst();
		if (notObstructed(direction,inEnvironment)){
			positionChanger(position,direction);
			return direction;
		}
		else {
			//teammate is in the way, make a random move
			return -2;
		}
	}
	public void addToBadPoistions(AgentEnvironment inEnvironment){
		if (positionKnown[agentId]) {
			int blocked = 0;
			Position[] neighbors = new Position[4];
			for (int i = 0;i<4;i++) {
				neighbors[i] = positionMaker(position,i);
				if (!noObstacles(i,inEnvironment))
					blocked++;
				if (neighbors[i]!=null && badPositions.contains(neighbors[i]))
					blocked++;
			}
			if (blocked >=3)
				badPositions.add(new Position(position));
		}		
	}
	public double straightLineDistance(Position start, Position end) {
		int x1 = start.row;
		int y1 = start.column;
		int x2 = enemyFlagPosition.row;
		int y2 = enemyFlagPosition.column;
		return Math.sqrt(Math.pow(x1-x2,2)+Math.pow(y1-y2,2));
	}
	public LinkedList<Integer> findPath(Position start, Position end) {
		if (positionKnown[agentId]) {
			start.distance = 0;
			start.direction = -1;
			start.parent = null;
			Set<Position> visited = new HashSet<Position>();
			PriorityQueue<Position> pq = new PriorityQueue<Position>();
			LinkedList<Integer> pathList = new LinkedList<Integer>();
			pq.add(start);
			visited.add(start);
			Position node = new Position();
			while (pq.size() > 0) {
				node = pq.poll();
				if (!inEnvironment.hasFlag() && node.distance > 1 * boardSize){
					return null;
				}
				if (node.equals(end)){
					while (node.parent != null){
						pathList.add(0,node.direction);
						node.distance = 1000;
						node = node.parent;
					}
					return pathList;
				}
				else {
					visited.add(node);
					for(int dir = 0;dir<4;dir++){
						Position neighbor = positionMaker(node,dir);
						if (node.equals(position) && (enemyAgentAdjacent(dir,inEnvironment) || !notObstructed(dir,inEnvironment)))
							neighbor = null;
						if (neighbor != null && inEnvironment.hasFlag() && neighbor.row == ourFlagPosition.row && neighbor.column == ourFlagPosition.column + 2)
							neighbor = null;
						if (neighbor != null && (mine == null || !mine[neighbor.row][neighbor.column]) &&(!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag()) && map[neighbor.row][neighbor.column] == 1  && !visited.contains(neighbor) &&(teammateNotInWay(dir,inEnvironment)||inEnvironment.hasFlag())){
							neighbor.distance = node.distance + 1;
							neighbor.cost = neighbor.distance + straightLineDistance(end,neighbor) + getNumberOfTags(neighbor) + isEnemyColumn(neighbor);
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
	return null;
	}
	public LinkedList<Integer> findPathToClosestUndiscoveredPosition(Position start,Position goal) {
		if (positionKnown[agentId] && initailizedMapArray) {
			start.distance = 0;
			start.direction = -1;
			start.parent = null;
			Set<Position> visited = new HashSet<Position>();
			PriorityQueue<Position> pq = new PriorityQueue<Position>();
			LinkedList<Integer> pathList = new LinkedList<Integer>();
			pq.add(start);
			visited.add(start);
			while (pq.size() > 0) {
				Position node = pq.poll();
				if (node.equals(goal) || map[node.row][node.column] == 0){
					goal = node.parent;
					while (node.parent != null){
						pathList.add(0,node.direction);
						node.distance = 1000;
						node = node.parent;
					}
					return pathList;
				}
				else{
					visited.add(node);
					Collections.shuffle(Arrays.asList(moveList));
					for(int i = 0;i<4;i++){
						int dir = moveList[i];
						Position neighbor = positionMaker(node,dir);
						if (node.equals(position) && enemyAgentAdjacent(dir,inEnvironment) && position.column != enemyFlagPosition.column)
							neighbor = null;
						if (neighbor != null   &&(!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag() || inEnvironment.hasFlag(inEnvironment.ENEMY_TEAM)) && (map[neighbor.row][neighbor.column] == 0 || map[neighbor.row][neighbor.column] == 1) && !visited.contains(neighbor)  && (teammateNotInWay(dir,inEnvironment)||inEnvironment.hasFlag())){
							neighbor.distance = node.distance + 1;
							neighbor.cost = neighbor.distance +  straightLineDistance(goal,neighbor);
							if (agentId == 1) {
								neighbor.cost += moveAwayFromEnemy(neighbor);
								neighbor.cost += getNumberOfTags(neighbor);
								if (!inEnvironment.hasFlag()) {
									neighbor.cost += isEnemyColumn(neighbor);
									if (dir == AgentAction.MOVE_WEST || dir == AgentAction.MOVE_EAST)
										neighbor.cost -= 2;
								}
							}
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
		//could not find a path
		return null;
	}
	public LinkedList<Integer> findPathToDefense(Position start,Position goal) {
		if (positionKnown[agentId] && initailizedMapArray) {
			start.distance = 0;
			start.direction = -1;
			start.parent = null;
			Set<Position> visited = new HashSet<Position>();
			PriorityQueue<Position> pq = new PriorityQueue<Position>();
			LinkedList<Integer> pathList = new LinkedList<Integer>();
			pq.add(start);
			visited.add(start);
			while (pq.size() > 0) {
				Position node = pq.poll();
				if (node.equals(goal) || map[node.row][node.column] == 0){
					while (node.parent != null){
						pathList.add(0,node.direction);
						node.distance = 1000;
						node = node.parent;
					}
					return pathList;
				}
				else{
					visited.add(node);
					Collections.shuffle(Arrays.asList(moveList));
					for(int i = 0;i<4;i++){
						int dir = moveList[i];
						Position neighbor = positionMaker(node,dir);
						if (neighbor != null  && (mine == null || !mine[neighbor.row][neighbor.column]) && (!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag()) && (map[neighbor.row][neighbor.column] == 0 || map[neighbor.row][neighbor.column] == 1) && !visited.contains(neighbor)){
							neighbor.distance = node.distance + 1;
							neighbor.cost = neighbor.distance + 1.5  *straightLineDistance(goal,neighbor);
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
		//could not find a path
		return null;
	}
	public int getNumberOfTags(Position pos){
		return tagged[pos.row][pos.column];
	}
	public int isEnemyColumn(Position pos){
		if (pos.column == enemyFlagPosition.column)
			return (int) -boardSize/3;
		else
			return 0;
	}
	public int calculateNumberOfNewPositions(Position pos){
		int known = 4;
		Position neighbor = new Position();
		for(int i = 0;i<4;i++) {
			neighbor = positionMaker(pos,i);
			if (neighbor!= null && (map[neighbor.row][neighbor.column] == 0 || neighbor.column == enemyFlagPosition.column))
				known--;
		}
		return known;
	}
	public int moveAwayFromEnemy(Position neighbor){
		return enemyPositions[neighbor.row][neighbor.column];

	}

	public void makeEnemyPositionArray(){
		if (positionKnown[agentId]) {
			boolean atLeastOneAbove = inEnvironment.isAgentNorth(inEnvironment.ENEMY_TEAM,false);
			boolean atLeastOneBelow = inEnvironment.isAgentSouth(inEnvironment.ENEMY_TEAM,false);
			boolean atLeastOneWest = inEnvironment.isAgentWest(inEnvironment.ENEMY_TEAM,false);
			boolean atLeastOneEast = inEnvironment.isAgentEast(inEnvironment.ENEMY_TEAM,false);
			enemyPositions = new int[boardSize][boardSize];
			for(int row = 0;row <boardSize;row++){
				for(int col = 0;col<boardSize;col++){
					if (atLeastOneAbove ^ atLeastOneBelow){
						if (atLeastOneAbove && row < position.row){
							enemyPositions[row][col]+=1;
						}
						else if (row > position.row) {
							enemyPositions[row][col] += 1;
						}

					}
					if (atLeastOneWest ^ atLeastOneEast) {
						if (atLeastOneWest & col < position.column)
							enemyPositions[row][col]+=1;
						else if (col > position.column)
							enemyPositions[row][col] += 1;
					}
				}
			}
		}

	}
	public void findCorner(AgentEnvironment inEnvironment) {
		if (!cornerKnown) {
			if (inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate())
				startingCorner = 0;
			else if (inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
				startingCorner = 1;
			else if (inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
				startingCorner = 2;
			else
				startingCorner = 3;
			cornerKnown = true;
			if (inEnvironment.isFlagWest(inEnvironment.ENEMY_TEAM,false))
				ourSide = "right";
			else
				ourSide = "left";
		}
	}
	public int findBoardSize(AgentEnvironment inEnvironment){
		if (!boardSizeKnown[0] || !boardSizeKnown[1]){
			int direction = -2;
			if (boardSizeKnown[agentId])
				return AgentAction.DO_NOTHING;
			else if (inEnvironment.isFlagNorth(inEnvironment.OUR_TEAM,true) || inEnvironment.isFlagSouth(inEnvironment.OUR_TEAM,true)) {
				boardSizeKnown[agentId] = true;
				boardSize += 1;
				return -2;
			}
			else if (inEnvironment.isFlagNorth(inEnvironment.OUR_TEAM,false)){
				direction =  AgentAction.MOVE_NORTH;
			}
			else if (inEnvironment.isFlagSouth(inEnvironment.OUR_TEAM,false)){
				direction =  AgentAction.MOVE_SOUTH;
			}
			if ((ourSide == "left" && inEnvironment.isObstacleEastImmediate()) || (ourSide == "right" && inEnvironment.isObstacleWestImmediate()))
				beginningObstacles.add((byte)2);
			else
				beginningObstacles.add((byte)1);
			boardSize += 1;
			beginningSteps +=1;
			return direction;
		}
		return -2;
	}
	public void checkIfTagged(AgentEnvironment inEnvironment) {
		if (!inEnvironment.hasFlag() && !inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true) && !inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,true)){
			if( (startingCorner == 0 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate()) 
				|| (startingCorner == 1 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
					|| (startingCorner == 2 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
						|| (startingCorner == 3 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleEastImmediate())) {
				returnToStart();
			}
		}
	}
	public void learnAgentAndFlagPositions(AgentEnvironment inEnvironment){
		if (boardSizeKnown[0] && boardSizeKnown[1] && (!positionKnown[0] || !positionKnown[1])){
			if (positionKnown[agentId]){
				;
			}
			else if (startingCorner == 0) {
				position = new Position(beginningSteps,0);
			}
			else if (startingCorner == 1) {
				position = new Position(beginningSteps,boardSize-1);
			}
			else if (startingCorner == 2) {
				position = new Position(boardSize-1 - beginningSteps,0);
			}
			else {
				position = new Position(boardSize-1 - beginningSteps, boardSize-1);
			}
			positionKnown[agentId] = true;
			if (inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true)) {
				ourFlagPosition = positionMaker(position,AgentAction.MOVE_NORTH);
			}
			else
				ourFlagPosition = positionMaker(position,AgentAction.MOVE_SOUTH);
		}
		if (positionKnown[0] && positionKnown[1] && !defensePositionsFound){
			if (ourSide.equals("left")){
				enemyFlagPosition = new Position(ourFlagPosition.row,boardSize-1);
				mineColumn = ourFlagPosition.column+2;
				offenseMineColumn = enemyFlagPosition.column-1;
			}
			else{
				enemyFlagPosition = new Position(ourFlagPosition.row,0);
				mineColumn = ourFlagPosition.column-2;
				offenseMineColumn = enemyFlagPosition.column+1;
			}

			topDefendingPosition = new Position(ourFlagPosition.row-1,ourFlagPosition.column);
			bottomDefendingPosition = new Position(ourFlagPosition.row+2,ourFlagPosition.column);
			topAttackingPosition = new Position(enemyFlagPosition.row-1,enemyFlagPosition.column);
			bottomAttackingPosition= new Position(enemyFlagPosition.row+1,enemyFlagPosition.column);
			defensePositionsFound = true;
			defenseGoal.set(topDefendingPosition);
			goal.set(topAttackingPosition);
		}
	}
	public void createMapArray(){
		if(!initailizedMapArray && boardSizeKnown[0] && boardSizeKnown[1]){
			map = new byte[boardSize][boardSize];
			tagged = new int[boardSize][boardSize];
			for(int i = 0;i<boardSize;i++)
				for(int j = 0;j<boardSize;j++){
					map[i][j] = 0;
					tagged[i][j] = 0;
				}
			for (int i = 0;i<boardSize;i++) {
				if (ourSide.equals("left"))
					map[i][0] = 1;
				else
					map[i][boardSize-1] = 1;
			}
			initailizedMapArray = true;
		}
	}
	public void initialMapUpdate(){
		if (!initialMapUpdateFlag && initailizedMapArray){
			byte key;
			int targetColumn;
			if (ourSide.equals("left"))
				targetColumn = 1;
			else
				targetColumn = boardSize-2;
			for(int i = 0;i<beginningObstacles.size();i++){
				key = (byte) beginningObstacles.get(i);
				if (startingCorner == 0 || startingCorner == 1) {
					map[i][targetColumn] = key;
				}
				else {
					map[boardSize-i-1][targetColumn] = key;
				}
			}
			initialMapUpdateFlag = true;
		}
	}
	public void updateMapArray(AgentEnvironment inEnvironment){
		if(initailizedMapArray && boardSizeKnown[0] && boardSizeKnown[1] && positionKnown[agentId]){
			Position temp = new Position();
			for(int dir = 0;dir<4;dir++){
				temp.set(position);
				if (positionChanger(temp,dir)) {
					if(noObstacles(dir,inEnvironment))
						map[temp.row][temp.column] = 1;
					else 
						map[temp.row][temp.column] = 2;
				}
			}
		}
	}
	public boolean enemyAgentAdjacent(int direction, AgentEnvironment inEnvironment){
		if (direction == AgentAction.MOVE_NORTH && inEnvironment.isAgentNorth(inEnvironment.ENEMY_TEAM,true))
			return true;
		else if (direction == AgentAction.MOVE_SOUTH && inEnvironment.isAgentSouth(inEnvironment.ENEMY_TEAM,true))
			return true;
		else if (direction == AgentAction.MOVE_EAST && inEnvironment.isAgentEast(inEnvironment.ENEMY_TEAM,true))
			return true;
		else if (direction == AgentAction.MOVE_WEST && inEnvironment.isAgentWest(inEnvironment.ENEMY_TEAM,true))
			return true;
		return false;
	}
	public int defend() {
		int direction = -2;
		if (defensePositionsFound && positionKnown[agentId]){
			if (position.equals(topDefendingPosition) && defenseGoal.equals(topDefendingPosition)){
				defenseGoal = bottomDefendingPosition;
			}
			else if (position.equals(bottomDefendingPosition) && defenseGoal.equals(bottomDefendingPosition)){
				defenseGoal = topDefendingPosition;
			}
			//if on path to something, try to go there. If we can't, it means there is a teammate, so we should make any possible move or self destruct
			if (onPath && minesPlanted) {
				if (defenseMinePositions == null)
					calculateDefenseMinePositions();
				if (defenseMinePositions != null) {
					for(Position pos: defenseMinePositions){
						//plant the mine, need a new path
						if (position.equals(pos) && !mine[pos.row][pos.column]) {
							mine[pos.row][pos.column] = true;
							if (areMinesPlanted())
								minesPlanted = true;
							onPath = false;
							return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
						}
					}
				}
				if(path != null && path.peekFirst() == null){
					onPath = false;
					path = findPath(position,defenseGoal);
					if (path == null)
						path = findPathToDefense(position,defenseGoal);
					if (path != null) {
						direction = getNextMoveFromPath(inEnvironment);
						onPath = true;
						return direction;
					}
				}
				else if (notObstructed(path.peek(),inEnvironment)) {
					direction = getNextMoveFromPath(inEnvironment);
					if (direction != -2)
						return direction;
				}
				//make any possible move
				else {
					onPath = false;
					Position neighbor = new Position(position);
					for(int i = 0;i<4;i++){
						neighbor = positionMaker(position,i);
						if (neighbor != null && notObstructed(i,inEnvironment)){
							positionChanger(position,i);
							return i;
						}
					}
					//no possible moves
					selfDestruct = true;
					return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
				}
			}
			//we do not have a current path found
			else {
				if (!minesPlanted) {
					if (defenseMinePositions == null) {
						calculateDefenseMinePositions();
					}
					if (defenseMinePositions != null){
						direction = plantDefenseMine();
						if (direction != -2) {
							onPath = true;
							return direction;
						}
					}
					else {
						path = findPath(position,defenseGoal);
						if (path == null) {
							path = findPathToDefense(position,defenseGoal);
						}
						if (path != null){
							onPath = true;
							direction = tryNextMoveFromPath(inEnvironment);
							if (direction != -2 && teammateNotInWay(direction,inEnvironment))
								return direction;
						}
					}	
				}
				else {
					path = findPath(position,defenseGoal);
					if (path == null) {
						path = findPathToDefense(position,defenseGoal);
					}
					if (path != null) {
						onPath = true;
						direction = tryNextMoveFromPath(inEnvironment);
						if (direction != -2)
							return direction;
					}
				}
			path = findPathToClosestUndiscoveredPosition(position,defenseGoal);
			if (path != null && path.peekFirst() != null)
				return getNextMoveFromPath(inEnvironment);
		}
	}
	return AgentAction.DO_NOTHING;
	}

	public boolean areMinesPlanted(){
		for(int i = 0;i<3;i++) {
			if (!mine[ourFlagPosition.row-1+i][mineColumn] && map[ourFlagPosition.row-1+i][mineColumn] == 1)
				return false;
		}
		return true;
	}
	public int plantDefenseMine(){
		for(Position pos: defenseMinePositions){
			//plant the mine, need a new path
			if (position.equals(pos) && !mine[pos.row][pos.column]) {
				mine[pos.row][pos.column] = true;
				if (areMinesPlanted())
					minesPlanted = true;
				onPath = false;
				return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
			}
			//need to find path to that mine, if found set onPath to true
			if (map[pos.row][pos.column] == 1 && !mine[pos.row][pos.column]) {
				path = findPath(position,pos);
				if (path == null)
					path = findPathToDefense(position,pos);
				if (path != null) {
					onPath = true;
					int mve = tryNextMoveFromPath(inEnvironment); 
					if (mve != -2)
						return mve;
				}
				else{
					//could not find path
					selfDestruct = true;
					onPath = false;
					return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
				}
			}
		}
		//all mines have been planted
		onPath = false;
		minesPlanted = true;
		return -2;
	}
	public boolean calculateDefenseMinePositions(){
		for(int j = 0; j<3;j++){
			if (map[ourFlagPosition.row-1+j][mineColumn] == 0) 
				return false;
		}
		if (defenseMinePositions == null){
			mine = new boolean [boardSize][boardSize];
			mine[ourFlagPosition.row+1][ourFlagPosition.column] = true;
			defenseMinePositions = new ArrayList<Position>();
			if (map[ourFlagPosition.row][ourFlagPosition.column+2] == 1)
				defenseMinePositions.add(new Position(ourFlagPosition.row,ourFlagPosition.column+2));
			for(int i = 0;i<3;i+=2){
				if (map[ourFlagPosition.row-1+i][mineColumn] == 1) {
					int row = ourFlagPosition.row-1+i;
					Position bomb = new Position(row,mineColumn);
					defenseMinePositions.add(bomb);
				}
			}
			return true;
		}
		return false;
	}
	public int attack(){
		for(int i = 0;i<4;i++){
			if(enemyAgentAdjacent(i,inEnvironment)){
				positionChanger(position,i);
				return i;
			}
		}
		return -2;
	}
	public void printMapArray(){
			if (initailizedMapArray) {
				for(int i = 0;i<boardSize;i++){
					for(int j = 0;j<boardSize;j++){
						System.out.printf("%d ",map[i][j]);
					}
				System.out.println();
			}
			System.out.println();
			}
	}
	public int capture(){
		if (inEnvironment.hasFlag()) {
				if (inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true))
					return AgentAction.MOVE_NORTH;
				else if (inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,true))
					return AgentAction.MOVE_WEST;
				else if (inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,true))
					return AgentAction.MOVE_SOUTH;
				else if (inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,true))
					return AgentAction.MOVE_EAST;
			}
			return -2;
		}
	public int getFlag(){
				if (!inEnvironment.hasFlag() && !inEnvironment.hasFlag(inEnvironment.OUR_TEAM) && positionKnown[agentId]){
				int move = -2;
				if (inEnvironment.isFlagNorth(inEnvironment.ENEMY_TEAM,true))
					move = AgentAction.MOVE_NORTH;
				else if (inEnvironment.isFlagWest(inEnvironment.ENEMY_TEAM,true))
					move = AgentAction.MOVE_WEST;
				else if (inEnvironment.isFlagSouth(inEnvironment.ENEMY_TEAM,true))
					move = AgentAction.MOVE_SOUTH;
				else if (inEnvironment.isFlagEast(inEnvironment.ENEMY_TEAM,true))
					move = AgentAction.MOVE_EAST;
				if (move != -2 && notObstructed(move,inEnvironment)){
					positionChanger(position,move);
					return move;
				}
			}
			return -2;

	}

	public boolean justGotEnemyFlag() {
		if (positionKnown[agentId] && position.equals(enemyFlagPosition) && inEnvironment.hasFlag())
			return true;
		else
			return false;

	}
	public boolean knownMineAdjacent(int direction){
		Position potential = new Position(position);
		positionChanger(potential,direction);
		if (potential != null && mine[potential.row][potential.column])
			return true;
		return false;

	}

	public boolean enemyFlagAdjacent(int direction) {
		if (inEnvironment.isFlagNorth(inEnvironment.ENEMY_TEAM,true) && direction == AgentAction.MOVE_NORTH)
			return true;
		else if (inEnvironment.isFlagSouth(inEnvironment.ENEMY_TEAM,true) && direction == AgentAction.MOVE_SOUTH)
			return true;
		else if (inEnvironment.isFlagWest(inEnvironment.ENEMY_TEAM,true) && direction == AgentAction.MOVE_WEST)
			return true;
		else if (inEnvironment.isFlagEast(inEnvironment.ENEMY_TEAM,true) && direction == AgentAction.MOVE_EAST)
			return true;
		return false;
	}

	public boolean ourFlagAdjacent(int direction){
		if (inEnvironment.isFlagNorth(inEnvironment.OUR_TEAM,true) && direction == AgentAction.MOVE_NORTH)
			return true;
		else if (inEnvironment.isFlagSouth(inEnvironment.OUR_TEAM,true) && direction == AgentAction.MOVE_SOUTH)
			return true;
		else if (inEnvironment.isFlagWest(inEnvironment.OUR_TEAM,true) && direction == AgentAction.MOVE_WEST)
			return true;
		else if (inEnvironment.isFlagEast(inEnvironment.OUR_TEAM,true) && direction == AgentAction.MOVE_EAST)
			return true;
		return false;
	}

	public int offense(Position goal){
		int move = -2;
		if (positionKnown[agentId]) {
				//if you're on the path, see if we can get there without making new one
				//otherwise, make a new path
				if (!inEnvironment.hasFlag() && inEnvironment.hasFlag(inEnvironment.ENEMY_TEAM)){
					for (int dir = 0;dir<4;dir++){
						if (enemyAgentAdjacent(dir,inEnvironment) && enemyFlagAdjacent(dir)) {
							positionChanger(position,dir);
							return dir;
						}
					}
				}
				if (onPath) {
					if (path.peekFirst() == null) {
						onPath = false;
						path = findPath(position,goal);
						if (path == null) {
							path = findPathToClosestUndiscoveredPosition(position,goal);
							if (path != null) {
								onPath = true;
							}
						}
						if (path != null && path.peekFirst() != null){
							return getNextMoveFromPath(inEnvironment);
						}
					}

					if (notObstructed(path.peek(),inEnvironment) && !enemyAgentAdjacent(path.peek(),inEnvironment) && (mine == null || !knownMineAdjacent(path.peek()))) {
						move = getNextMoveFromPath(inEnvironment);
						return move;
					}
					else {
						path = findPath(position,goal);
						if (path == null) {
							onPath = false;
							path = findPathToClosestUndiscoveredPosition(position,goal);
							if (path != null) {
								move = getNextMoveFromPath(inEnvironment);
								onPath = true;
								return move;
							}
						}
						else {
							move = getNextMoveFromPath(inEnvironment);
							onPath = true;
							return move;
						}
					}
				}
				else {
					path = findPath(position,goal);
					if (path != null) {
						onPath = true;
						move = getNextMoveFromPath(inEnvironment);
						return move;
					}
					else {
						onPath = false;
						path = findPathToClosestUndiscoveredPosition(position,goal);
						if (path != null) {
							onPath = true;
							move = getNextMoveFromPath(inEnvironment);
							return move;
						}
					}
				}
				//at this point, something is wrong and we should restart
		}
		onPath = false;
		if (!inEnvironment.hasFlag()) {
			selfDestruct = true;
			return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
		}
		else
			return AgentAction.DO_NOTHING;
	}

	public int stopEnemyAgent() {
		int move;
		Position goal = enemyFlagPosition;
		if (moves > boardSize * 2){
			System.out.println("We've stayed at flag too long");
			return -2;
		}
		for (int i = 0;i<4;i++) {
			if (ourFlagAdjacent(i) && enemyAgentAdjacent(i,inEnvironment))
				return i;
		}
		if (position.equals(goal)) {
			System.out.println("increming moves we've stayed at enemy flag");
			moves++;
			System.out.printf("Moves is %d\n,",moves);
			if (!offenseMinePlanted && map[enemyFlagPosition.row][offenseMineColumn] == 1 && !mine[enemyFlagPosition.row][offenseMineColumn]){
				System.out.println("Offense mines have not been planted yet");
				plantMine = true;
				offenseMinePlanted = true;
				mine[enemyFlagPosition.row][offenseMineColumn] = true;
				System.out.println("movingto plant");
				if (inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false)){
					positionChanger(position,AgentAction.MOVE_WEST);
					return AgentAction.MOVE_WEST;
				}
				else{
					positionChanger(position,AgentAction.MOVE_EAST);
					return AgentAction.MOVE_EAST;
				}
			}
			System.out.println("we are at base and defending");
			return AgentAction.DO_NOTHING;
		}
		else
		{
			System.out.println("we are not at base and need to move to it");
			if (inEnvironment.isBaseWest(inEnvironment.ENEMY_TEAM,true)){
				positionChanger(position,AgentAction.MOVE_WEST);
				return AgentAction.MOVE_WEST;
			}
			else if (inEnvironment.isBaseEast(inEnvironment.ENEMY_TEAM,true)) {
				positionChanger(position,AgentAction.MOVE_EAST);
				return AgentAction.MOVE_EAST;
			}
			else return -2;
		}
	}

	public boolean isEverythingOk(){
		if (positionKnown[1]) {
			if (inEnvironment.hasFlag(inEnvironment.ENEMY_TEAM)){
				if (!everyThingOk)
					return false;
				if (inEnvironment.hasFlag() && everyThingOk) {
					int pathLength = path.size();
					if (pathLength < boardSize-1)
						return true;
				}
				goal = new Position(topAttackingPosition);
				path = new LinkedList<Integer>();
				return false;

			}
			return true;
		}
		return true;
	}
	// implements Agent.getMove() interface
	public int getMove(AgentEnvironment inEnvironment) {
		this.inEnvironment = inEnvironment;	
		counter++;	
		int move;
		if (selfDestruct) {
			selfDestruct = false;
			return AgentAction.DO_NOTHING;
		}
		move = capture();
		if  (move != -2)
			return move;
		findCorner(inEnvironment);
		move = findBoardSize(inEnvironment);
		if(move != -2)
			return move;
		checkIfTagged(inEnvironment);
		learnAgentAndFlagPositions(inEnvironment);
		if (plantMine) {
			plantMine = false;
			return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
		}
		addToBadPoistions(inEnvironment);
		createMapArray();
		initialMapUpdate();
		updateMapArray(inEnvironment);
		makeEnemyPositionArray();
		//attacker
		if (agentId == 1){
			if (positionKnown[1]) 
			{
				if (positionKnown[0] && !attackingInitialBomb){
					attackingInitialBomb = true;
					return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
				}
				everyThingOk = isEverythingOk();
				if (everyThingOk) {
					System.out.println("everything is ok");
					if (justGotEnemyFlag())
						enemyFlagGoal = false;
					//see if we can get the flag
					move = getFlag();
					if (move != -2)
						return move;
					if (enemyFlagGoal)
						move = offense(enemyFlagPosition);
					else
						move = offense(ourFlagPosition);
					if (move != -2)
						return move;
				}
				else {
					System.out.println("everything is not ok");
					move = stopEnemyAgent();
					System.out.printf("move number we get from stop enemy %d\n",move);
					if (move == -2){
						System.out.println("Everything not ok but we are going to move on");
						if (inEnvironment.hasFlag())
							move = offense(ourFlagPosition);
						else
							move = offense(enemyFlagPosition);
					}
					System.out.printf("move number we get is %d\n",move);
					return move;
				}
			}
			//in case postions of both agents not ready 
			return AgentAction.DO_NOTHING;
		}
		//this is the defender's actions	
		else {
			moves++;
			move = attack();
			if (move != -2)
				return move;
			if (counter < boardSize *boardSize * 2-5 - boardSize) {
				if (everyThingOk) {
					return defend();
				}
				else return defend();
			}
			else {
				move = offense(enemyFlagPosition);
				return move;
			}
		}
	}
}