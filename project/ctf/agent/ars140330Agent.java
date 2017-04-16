package ctf.agent;

import java.util.*;
import ctf.common.AgentEnvironment;
import ctf.agent.Agent;
import ctf.common.AgentAction;

public class ars140330Agent extends Agent {
	public static int boardSize = 1;
	public static int id = 0;
	public static String ourSide;
	public static boolean[] boardSizeKnown = {false,false};
	public static boolean[] positionKnown = {false,false};
	public static Position[] positionArray = {null,null};
	public static Position enemyFlagPosition;
	public static Position ourFlagPosition;
	public static Set<Position> badPositions = new HashSet<Position>();
	public static Set<Position> discoveredPositions = new HashSet<Position>();
	public static boolean[][] noObstacle;
	public ArrayList<Position> positionTabu;
	public Stack<Integer> homePath;
	public int agentId;
	public int beginningSteps; //used to calculate boardSize
	public int startingCorner; //0 indicates NorthWest, 1 NorthEast, 2 SouthWest, 3 Southeast
	public Position position; //holds the x,y coordinates of the agent
	public boolean cornerKnown;; //flag for if agent knows its starting corner
	public boolean goingHome;
	public boolean initailizedObstacleArray = false;
	public static final int TABU_SIZE_LIMIT = 1;
	public static final Integer[] moveList = {AgentAction.MOVE_NORTH,AgentAction.MOVE_SOUTH,AgentAction.MOVE_EAST,AgentAction.MOVE_WEST};

	//Constructor
	public ars140330Agent(){
		this.agentId = id++;
		if (id == 2)
			id = 0;
		boardSizeKnown[this.agentId] = false;
		positionKnown[this.agentId] = false;
		positionArray[agentId] = null;
		positionTabu = new ArrayList<Position>();
		homePath = new Stack<Integer>();
		homePath.push(AgentAction.DO_NOTHING);
		goingHome = false;
		cornerKnown = false;
		beginningSteps = 0;
		startingCorner = -1;
		ourSide = null;

	}

	//private class Position to hold row,column coordinates
	private class Position implements Comparable<Position> {
		public int row, column,distance,direction;
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
			this.distance = 1000;
		}
		public void set(int row, int column){
			this.row = row;
			this.column = column;
			this.distance = 1000;
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
	}
	public void positionChanger(Position position,int direction) {
		if (direction == AgentAction.MOVE_NORTH)
			position.set(position.row-1,position.column);
		else if (direction == AgentAction.MOVE_SOUTH) 
			position.set(position.row+1,position.column);
		else if (direction == AgentAction.MOVE_EAST)
			position.set(position.row,position.column+1);
		else if (direction == AgentAction.MOVE_WEST)
			position.set(position.row,position.column-1);
	}

	public Position positionMaker(Position position, int direction){
		Position newPosition;
		if (direction == AgentAction.MOVE_NORTH)
			newPosition = new Position(position.row-1,position.column);
		else if (direction == AgentAction.MOVE_SOUTH) 
			newPosition =  new Position(position.row+1,position.column);
		else if (direction == AgentAction.MOVE_EAST)
			newPosition =  new Position(position.row,position.column+1);
		else if (direction == AgentAction.MOVE_WEST)
			newPosition =  new Position(position.row,position.column-1);
		else 
			return position;
		if (newPosition.row < 0 || newPosition.row >= boardSize || newPosition.column < 0 || newPosition.column >= boardSize)
			return null;
		return newPosition;
	}

	public int move(AgentEnvironment inEnvironment) {
		Position[] neighbors = new Position[4];
		Boolean[] valid = {true,true,true,true};
		Collections.shuffle(Arrays.asList(moveList));
		int direction;
		for(int i = 0;i<4;i++){
			direction = moveList[i];
			neighbors[direction] = positionMaker(position,direction);
			if(neighbors[direction] == null || neighbors[direction].row < 0 || neighbors[direction].row >= boardSize || neighbors[direction].column < 0 || neighbors[direction].column >= boardSize)
				valid[direction] = false;
			if (!validMove(direction,inEnvironment) || badPositions.contains(neighbors[direction]) 
				|| !teammateNotInWay(direction,inEnvironment) || !notBabysitting(direction,inEnvironment) || !checkTabu(neighbors[direction]))
				valid[direction] = false;
		}
		int validNeighbors = 0;
		for(int i = 0;i<4;i++){
			direction = moveList[i];
			if(valid[direction]){
				validNeighbors++;
			}
		}
		if (validNeighbors == 0) {
			if (positionTabu.size() > 0)
				positionTabu.remove(positionTabu.size()-1);
			return AgentAction.DO_NOTHING;
		}
		//make undiscovered move
		for(int i = 0;i<4;i++){
			direction = moveList[i];
			Position neighbor = neighbors[direction];
			if(valid[direction] && !discoveredPositions.contains(neighbor)) {
				discoveredPositions.add(neighbor);
				if (positionTabu.size() > TABU_SIZE_LIMIT)
					positionTabu.remove(TABU_SIZE_LIMIT);
				positionTabu.add(0,neighbor);
				position.set(neighbor.row,neighbor.column);
				positionArray[agentId] = position;
				homePath.push(direction);
				return direction;
			}
		}
		//make random move
		direction = -1;
		while (direction == -1) {
			double roll = Math.random();
			if (roll < .25 && valid[0]) {
					direction = AgentAction.MOVE_NORTH;
				}
			else if (roll >= .25 && roll < .5 && valid[1]) {
					direction = AgentAction.MOVE_SOUTH;
				}
			else if (roll >=.5 && roll < .75 && valid[3]) {
					direction = AgentAction.MOVE_WEST;
				}
			else
				if (valid[2]){
					direction = AgentAction.MOVE_EAST;
				}
		}

		if (positionTabu.size() > TABU_SIZE_LIMIT)
			positionTabu.remove(TABU_SIZE_LIMIT);
		positionTabu.add(0,neighbors[direction]);
		position = neighbors[direction];
		positionArray[agentId] = position;
		homePath.push(direction);
		return direction;

	}

	public boolean checkTabu(Position candidate) {
		for (Position position: positionTabu){
			if(position.equals(candidate)) {
				return false;
			}
		}
		return true;
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
	public boolean notObstructed(int direction,AgentEnvironment inEnvironment,Position candidatePosition){
		if (!teammateNotInWay(candidatePosition))
			return false;
		//check if not obstructeed by obstacle
		if (!validMove(direction,inEnvironment))
			return false;
		//check if we're tryingto move to our base with the flag on it
		if (!notBabysitting(direction,inEnvironment))
			return false;

		return true;
	}

	public boolean validMove(int direction,AgentEnvironment inEnvironment){
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

	public boolean teammateNotInWay(Position candidatePosition) {
		int otherAgent;
		if (agentId == 1)
			otherAgent = 0;
		else 
			otherAgent = 1;
		if (positionKnown[otherAgent] && positionArray[otherAgent].equals(candidatePosition)){
			return false;
		}
		return true;
	}

	public void returnToStart() {
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
		positionArray[agentId] = position;
		while(!homePath.empty())
			homePath.pop();
		homePath.push(AgentAction.DO_NOTHING);
		goingHome = false;
	}

	public int getNextMoveFromHomeStack(AgentEnvironment inEnvironment) {
		int toDirection = -1;
		int returnDirection = -1;
		if(!homePath.empty()) {
			toDirection = (int)homePath.pop();
			if (toDirection == AgentAction.MOVE_NORTH)
				returnDirection = AgentAction.MOVE_SOUTH;
			else if (toDirection == AgentAction.MOVE_SOUTH)
				returnDirection = AgentAction.MOVE_NORTH;
			else if (toDirection == AgentAction.MOVE_EAST)
				returnDirection = AgentAction.MOVE_WEST;
			else if (toDirection == AgentAction.MOVE_WEST)
				returnDirection = AgentAction.MOVE_EAST;
			else
				;
			Position candidate = positionMaker(position,returnDirection);
			if (candidate != null && teammateNotInWay(returnDirection,inEnvironment)){
				position.set(candidate.row,candidate.column);
				positionArray[agentId] = candidate;
				return returnDirection;
			}
			else {
				homePath.push(toDirection);
				return AgentAction.DO_NOTHING;
			}
		}
		return AgentAction.DO_NOTHING;
	}
	public void addMoveToHomePath(int direction){
		if(direction != AgentAction.DO_NOTHING && direction != AgentAction.PLANT_HYPERDEADLY_PROXIMITY_MINE){
			if(!homePath.empty()){
				int lastDirection = (int)homePath.peek();
				int complementDirection;
				if (direction == AgentAction.MOVE_NORTH)
					complementDirection = AgentAction.MOVE_SOUTH;
				else if (direction == AgentAction.MOVE_SOUTH)
					complementDirection = AgentAction.MOVE_NORTH;
				else if (direction == AgentAction.MOVE_EAST)
					complementDirection = AgentAction.MOVE_WEST;
				else if (direction == AgentAction.MOVE_WEST)
					complementDirection = AgentAction.MOVE_EAST;
				else
					complementDirection = -2;
				if (lastDirection != complementDirection){
					homePath.push(direction);
				}
				else{
					homePath.pop();
				}
			}
		}

	}

	public void addToBadPoistions(AgentEnvironment inEnvironment){
		int blocked = 0;
		Position[] neighbors = {positionMaker(position,AgentAction.MOVE_NORTH),positionMaker(position,AgentAction.MOVE_SOUTH),
			positionMaker(position,AgentAction.MOVE_EAST),positionMaker(position,AgentAction.MOVE_WEST)};
		for (int i = 0;i<4;i++){
			if (!validMove(i,inEnvironment))
				blocked++;
			if (neighbors[i]!=null && badPositions.contains(neighbors[i]))
				blocked++;
		}
		if (blocked >=3)
			badPositions.add(position);


	}

	public Position pathListFromEndToStart(Position start, Position end) {
		start.distance = 0;
		start.parent = null;
		Set<Position> visited = new HashSet<Position>();
		PriorityQueue<Position> pq = new PriorityQueue<Position>();
		pq.add(start);
		visited.add(start);
		while (pq.size() > 0) {
			Position node = pq.poll();
			if (node.equals(end)){
				return node;
			}
			else{
				visited.add(node);
				Position[] neighbors = {positionMaker(position,AgentAction.MOVE_NORTH),positionMaker(position,AgentAction.MOVE_SOUTH),
				positionMaker(position,AgentAction.MOVE_EAST),positionMaker(position,AgentAction.MOVE_WEST)};
				for(int dir = 0;dir<4;dir++){
					Position neighbor = neighbors[dir];
					if (neighbor != null && noObstacle[neighbor.row][neighbor.column] && !visited.contains(neighbor)){
						neighbor.distance = node.distance + 1;
						neighbor.parent = node;
						neighbor.direction = dir;
						pq.add(neighbor);
					}

				}
			}
		}
		return null;
	}
	// implements Agent.getMove() interface
	public int getMove(AgentEnvironment inEnvironment) {
		// booleans describing direction of goal
		// goal is either enemy flag, or our base
		//find what the starting corner is
		//if(positionKnown[agentId] && (position.row > boardSize-1 || position.column > boardSize-1))
		//	System.exit(0);
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
			if (inEnvironment.isFlagWest(inEnvironment.OUR_TEAM,false))
				ourSide = "right";
			else
				ourSide = "left";
		}
		//code to find the dimension of the board
		if (!boardSizeKnown[0] || !boardSizeKnown[1]){
			if (boardSizeKnown[agentId])
				return AgentAction.DO_NOTHING;
			if (inEnvironment.isFlagNorth(inEnvironment.OUR_TEAM,true) || inEnvironment.isFlagSouth(inEnvironment.OUR_TEAM,true)) {
				boardSizeKnown[agentId] = true;
				boardSize += 1;
			}
			else if (inEnvironment.isFlagNorth(inEnvironment.OUR_TEAM,false)){
				boardSize += 1;
				beginningSteps +=1;
				return AgentAction.MOVE_NORTH;
			}
			else if (inEnvironment.isFlagSouth(inEnvironment.OUR_TEAM,false)){
				boardSize += 1;
				beginningSteps += 1;
				return AgentAction.MOVE_SOUTH;
			}
		}

		//add positions to valid and bad position sets
		if(positionKnown[agentId])
			addToBadPoistions(inEnvironment);
		//once both agents have reached the flag, calculate their starting indexes
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
			positionArray[agentId] = position;
			if (inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true)) {
				ourFlagPosition = positionMaker(position,AgentAction.MOVE_NORTH);
			}
			else
				ourFlagPosition = positionMaker(position,AgentAction.MOVE_SOUTH);
		}
		if (positionKnown[agentId]){
			if (ourSide.equals("left"))
				enemyFlagPosition = new Position(ourFlagPosition.row,boardSize-1);
			else
				enemyFlagPosition = new Position(ourFlagPosition.row,0);
		}


		//anything after this point will know the dimensions of the board and the position of each agent

		//initailize the obstacle boolean array
		//noObstacle[x][y] = true if there is no obstacle at position (X,Y), false otherwise
		if(positionKnown[agentId]){
			if (!discoveredPositions.contains(position)){
				discoveredPositions.add(position);
			}
		}

		if(!initailizedObstacleArray && boardSizeKnown[0] && boardSizeKnown[1]){
			noObstacle = new boolean[boardSize][boardSize];
			if (ourSide.equals("left")){
				for (int i = 0;i<boardSize;i++)
					noObstacle[i][0] = true;
			}
			else {
				for (int i = 0;i<boardSize;i++)
					noObstacle[i][boardSize-1] = true;
			}
			initailizedObstacleArray = true;
			Position temp;
			for(int dir = 0;dir<4;dir++){
				if (validMove(dir,inEnvironment)){
					temp = positionMaker(position,dir);
					if (temp != null){
						noObstacle[temp.row][temp.column] = true;
					}
				}
			}
		}

		if(initailizedObstacleArray && boardSizeKnown[0] && boardSizeKnown[1]){
			Position temp;
			for(int dir = 0;dir<4;dir++){
				if (validMove(dir,inEnvironment)){
					temp = positionMaker(position,dir);
					if (temp != null){
						noObstacle[temp.row][temp.column] = true;
					}
				}
			}

		}
		//check if the agent has just been tagged
		//see if its in its starting corner and to the left/right of its base
		if (!inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true) && !inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,true)){
			if( (startingCorner == 0 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate()) 
				|| (startingCorner == 1 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
					|| (startingCorner == 2 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
						|| (startingCorner == 3 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleEastImmediate())) {
				this.returnToStart();
			}
		}
		//see if we can go straight up or down to home
		if (inEnvironment.hasFlag()) {
			if (inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true)
			 || (inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false)))
				return AgentAction.MOVE_NORTH;
			else if (inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,true))
				return AgentAction.MOVE_WEST;
			else if (inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,true)
				|| (inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false)))
				return AgentAction.MOVE_SOUTH;
			else if (inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,true))
				return AgentAction.MOVE_EAST;
		}
		//see if the flag is immedietly next to us and go there
		if (!inEnvironment.hasFlag() && !inEnvironment.hasFlag(inEnvironment.OUR_TEAM) && positionKnown[agentId]){
			int direction = -2;
			if (inEnvironment.isFlagNorth(inEnvironment.ENEMY_TEAM,true)
				|| (inEnvironment.isFlagNorth(inEnvironment.ENEMY_TEAM,false) && position.column == enemyFlagPosition.column))
				direction = AgentAction.MOVE_NORTH;
			else if (inEnvironment.isFlagWest(inEnvironment.ENEMY_TEAM,true))
				direction = AgentAction.MOVE_WEST;
			else if (inEnvironment.isFlagSouth(inEnvironment.ENEMY_TEAM,true)
				|| (inEnvironment.isFlagSouth(inEnvironment.ENEMY_TEAM,false) && position.column == enemyFlagPosition.column))
				direction = AgentAction.MOVE_SOUTH;
			else if (inEnvironment.isFlagEast(inEnvironment.ENEMY_TEAM,true))
				direction = AgentAction.MOVE_EAST;
			if (direction != -2 && teammateNotInWay(direction,inEnvironment)){
				positionChanger(position,direction);
				positionArray[agentId] = position;
				addMoveToHomePath(direction);
				return direction;
			}
		}
		//we have just gotten the flag
		if (positionKnown[agentId] && position.equals(enemyFlagPosition) && inEnvironment.hasFlag()) {
			goingHome = true;
		}
		//if we are on our way back
		if (goingHome) {
			int move = getNextMoveFromHomeStack(inEnvironment);
			if (!homePath.empty()){
				return move;
			}
			else{
				goingHome = false;
			}
		}
		//make random possible move that is not in tabu list or a "bad" spot
		if (positionKnown[0] && positionKnown[1]){
			return move(inEnvironment);
		}
		else //wait until we know both positions 
			{
				return AgentAction.DO_NOTHING;
			}
		}
	}