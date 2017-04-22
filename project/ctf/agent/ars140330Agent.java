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
	public static Set<Position> discoveredPositions = new HashSet<Position>();
	public static ArrayList<Position> defenseMinePositions;
	public static boolean minesPlanted, enemyFlagGoal,defensePositionsFound, initailizedMapArray;

	public LinkedList<Integer> path; //general purpose path
	public ArrayList<Byte> beginningObstacles; //as we make our intitial path to our flag, see if things on the penultimate column are blocked
	public int agentId; //used to identify the two agents
	public int beginningSteps; //used to calculate boardSize
	public int startingCorner; //0 indicates NorthWest, 1 NorthEast, 2 SouthWest, 3 Southeast
	public Position position; //holds the x,y coordinates of the agent
	public boolean cornerKnown;; //flag for if agent knows its starting corner
	public int moves;
	public boolean initialMapUpdateFlag;
	public boolean onRouteToGoal;
	public Position goal;
	public boolean selfDestruct;
	public int[][] enemyPositions;

	public ars140330Agent(){
		moves = 0;
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
		discoveredPositions = new HashSet<Position>();
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
		goal = null;
		tagged = null;
		mine = null;
		topDefendingPosition = null;
		minesPlanted = false;
		topAttackingPosition = null;
		defenseMinePositions = null;
		enemyPositions = new int[boardSize][boardSize];
	}
	private class Position implements Comparable<Position> {
		public int row, column,direction, tagged;
		public double distance;
		public Position parent;
		Position(){
			this.row = -1;
			this.column = -1;
			this.distance = 10000;
		}
		Position(int row, int column) {
			this.row = row;
			this.column = column;
			this.distance = 10000;
		}
		Position(Position other) {
			this.row = other.row;
			this.column = other.column;
		}
		public void set(int row, int column){
			this.row = row;
			this.column = column;
			this.distance = 1000;
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
	        if (this.distance < other.distance)
	        	return -1;
	        else if (this.distance > other.distance)
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
	public void plantMine(){
		mine[position.row][position.column] = true;
	}
	public int move(AgentEnvironment inEnvironment) {
		if (agentId == 1)
			System.out.println("test");	
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
			System.out.println("No neighbors");
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
						if (neighbor != null && (mine == null || !mine[neighbor.row][neighbor.column])  &&(!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag()) && map[neighbor.row][neighbor.column] == 1  && !visited.contains(neighbor) &&(teammateNotInWay(dir,inEnvironment)||inEnvironment.hasFlag())){
							neighbor.distance = node.distance + straightLineDistance(end,neighbor) + getNumberOfTags(neighbor);
							if (agentId == 1)
								neighbor.distance+= moveAwayFromEnemy(neighbor);
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
	System.out.println("Failed to find a path to end");
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
						if (neighbor != null && (!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag()) && (map[neighbor.row][neighbor.column] == 0 || map[neighbor.row][neighbor.column] == 1) && !visited.contains(neighbor)  && (teammateNotInWay(dir,inEnvironment)||inEnvironment.hasFlag())){
							neighbor.distance = node.distance + straightLineDistance(goal,neighbor) + calculateNumberOfNewPositions(neighbor) + isEnemyColumn(neighbor) + getNumberOfTags(neighbor);
							if (agentId == 1)
								neighbor.distance += moveAwayFromEnemy(neighbor);
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
		System.out.println("Failed to find a path to new node");
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
				if (!node.equals(start) && map[node.row][node.column] == 0){
					goal = node;
					System.out.println(goal);
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
						if (node.equals(position))
							neighbor = null;
						if (neighbor != null  && (mine == null || !mine[neighbor.row][neighbor.column]) && (!neighbor.equals(ourFlagPosition) || inEnvironment.hasFlag()) && (map[neighbor.row][neighbor.column] == 0 || map[neighbor.row][neighbor.column] == 1) && !visited.contains(neighbor)){
							neighbor.distance = node.distance + straightLineDistance(goal,neighbor);
							neighbor.parent = node;
							neighbor.direction = dir;
							pq.add(neighbor);
						}
					}
				}
			}
		}
		System.out.println("Failed to find a path to new defense  node");
		//could not find a path
		return null;
	}
	public int getNumberOfTags(Position pos){
		return tagged[pos.row][pos.column];
	}
	public int isEnemyColumn(Position pos){
		if (pos.column == enemyFlagPosition.column)
			return -3;
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
							enemyPositions[row][col]+=3;
						}
						else if (row > position.row) {
							enemyPositions[row][col] += 3;
						}

					}
					if (atLeastOneWest ^ atLeastOneEast) {
						if (atLeastOneWest & col < position.column)
							enemyPositions[row][col]+=3;
						else if (col > position.column)
							enemyPositions[row][col] += 3;
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
			if (positionKnown[agentId])
				;
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
			if (ourSide.equals("left"))
				enemyFlagPosition = new Position(ourFlagPosition.row,boardSize-1);
			else
				enemyFlagPosition = new Position(ourFlagPosition.row,0);

			topDefendingPosition = new Position(ourFlagPosition.row-1,ourFlagPosition.column);
			bottomDefendingPosition = new Position(ourFlagPosition.row+1,ourFlagPosition.column);
			topAttackingPosition = new Position(enemyFlagPosition.row-1,enemyFlagPosition.column);
			bottomAttackingPosition= new Position(enemyFlagPosition.row+1,enemyFlagPosition.column);
			defensePositionsFound = true;
			defenseGoal.set(topDefendingPosition);		}
	}
	public void addToDiscoveredPositions(){
		if(positionKnown[agentId]){
			if (!discoveredPositions.contains(position)){
				discoveredPositions.add(position);
			}
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
		if (defensePositionsFound && positionKnown[agentId]){
			if (position.equals(topDefendingPosition) && defenseGoal.equals(topDefendingPosition)){
				defenseGoal = bottomDefendingPosition;
			}
			else if (position.equals(bottomDefendingPosition) && defenseGoal.equals(bottomDefendingPosition)){
				defenseGoal = topDefendingPosition;
			}
			if (!minesPlanted) {
				if (defenseMinePositions == null) {
					calculateDefenseMinePositions();
				}
				if (!minesPlanted && defenseMinePositions != null){
					int direction = plantDefenseMine();
					if (direction != -2)
						return direction;
				}
				path = findPathToClosestUndiscoveredPosition(position,defenseGoal);
				if (path != null){
					int direction = tryNextMoveFromPath(inEnvironment);
					if (direction != -2 && teammateNotInWay(direction,inEnvironment))
						return direction;
				}
			}
			path = findPath(position,defenseGoal);
			if (path == null) 
				path = findPathToDefense(position,defenseGoal);
			if (path != null) {
				int direction = tryNextMoveFromPath(inEnvironment);
				if (direction != -2 && teammateNotInWay(direction,inEnvironment))
					return direction;
			}
		}
		return -2;
	}
	public int plantDefenseMine(){
		for(Position pos: defenseMinePositions){
			if (position.equals(pos) && !mine[pos.row][pos.column]) {
				mine[pos.row][pos.column] = true;
				return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
			}
			if (map[pos.row][pos.column] == 1 && !mine[pos.row][pos.column]) {
				path = findPath(position,pos);
				if (path == null)
					path = findPathToClosestUndiscoveredPosition(position,enemyFlagPosition);
				if (path != null) {
					int mve = tryNextMoveFromPath(inEnvironment); 
					if (mve != -2)
						return mve;
				}
				else{
					selfDestruct = true;
					return AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE;
				}
			}
		}
		minesPlanted = true;
		return -2;
	}
	public boolean calculateDefenseMinePositions(){
		for(int j = 0; j<3;j++){
			if (map[ourFlagPosition.row-1+j][ourFlagPosition.column+2] == 0) 
				return false;
		}
		if (defenseMinePositions == null){
			printMapArray();
			mine = new boolean [boardSize][boardSize];
			defenseMinePositions = new ArrayList<Position>();
			for(int i = 0;i<3;i++){
				if (map[ourFlagPosition.row-1+i][ourFlagPosition.column+2] == 1) {
					int column = ourFlagPosition.column +2;
					int row = ourFlagPosition.row-1+i;
					Position bomb = new Position(row,column);
					defenseMinePositions.add(bomb);
				}
			}
			for (Position k: defenseMinePositions)
				System.out.println(k);
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
	// implements Agent.getMove() interface
	public int getMove(AgentEnvironment inEnvironment) {
		this.inEnvironment = inEnvironment;		
		int move;
		if (mine != null &&  moves % 20 == 0){
			mine[ourFlagPosition.row][ourFlagPosition.column+2] = false;
			minesPlanted = false;
		}
		if (selfDestruct)
		{
			selfDestruct = false;
			return AgentAction.DO_NOTHING;
		}
		moves++;
		move = capture();
		if  (move != -2)
			return move;
		findCorner(inEnvironment);
		move = findBoardSize(inEnvironment);
		if(move != -2)
			return move;
		checkIfTagged(inEnvironment);
		learnAgentAndFlagPositions(inEnvironment);
		addToBadPoistions(inEnvironment);
		addToDiscoveredPositions();
		createMapArray();
		initialMapUpdate();
		updateMapArray(inEnvironment);
		makeEnemyPositionArray();
		//attacker
		if (agentId == 1){
			if (!inEnvironment.hasFlag() && !inEnvironment.hasFlag(inEnvironment.OUR_TEAM) && positionKnown[agentId]){
				move = -2;
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
			//we have just gotten the flag
			if (positionKnown[agentId] && position.equals(enemyFlagPosition) && inEnvironment.hasFlag()) {
				enemyFlagGoal = false;
			}
			//our goal is to reach the enemy flag
			//find a path to the closest new position to the enemy flag
			if (enemyFlagGoal){
				path = findPathToClosestUndiscoveredPosition(position,enemyFlagPosition);
				if (path != null && path.size() > 0) {
					move = getNextMoveFromPath(inEnvironment);
					return move;
				}	
			}
			//our goal is to reach our flag
			//find the path to our flag
			else {
				path = findPath(position,ourFlagPosition);
				if (path != null && path.size() > 0) {
					move = getNextMoveFromPath(inEnvironment);
					return move;
				}
				else{
					path = findPathToClosestUndiscoveredPosition(position,ourFlagPosition);
					move = tryNextMoveFromPath(inEnvironment);
					if (move != -2)
						return AgentAction.DO_NOTHING;
				}
			} 
			return AgentAction.DO_NOTHING;
		}	
		else {
			move = attack();
			if (move != -2)
				return move;
			if (!inEnvironment.hasFlag(inEnvironment.ENEMY_TEAM)) {
				move = defend();
				if (move != -2)
					return move;
			}
			return defend();
			//if (positionKnown[0] && positionKnown[1]){
			//	printMapArray();
		//		return move(inEnvironment);
		//	}
		//	return AgentAction.DO_NOTHING;
		}
	}
}