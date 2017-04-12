package ctf.agent;

import java.util.ArrayList;
import ctf.common.AgentEnvironment;
import ctf.agent.Agent;
import ctf.common.AgentAction;

public class ars140330Agent extends Agent {
	public static int boardSize = 1;
	public static int id = 0;
	public static boolean[] boardSizeKnown = {false,false};
	public static boolean[] positionKnown = {false,false};
	public ArrayList<Position> positionTabu;
	public int agentId;
	public int beginningSteps = 0; //used to calculate boardSize
	public int startingCorner; //0 indicates NorthWest, 1 NorthEast, 2 SouthWest, 3 Southeast
	public Position position; //holds the x,y coordinates of the agent
	public boolean cornerKnown = false; //flag for if agent knows its starting corner

	//Constructor
	public ars140330Agent(){
		this.agentId = id++;
		if (id == 2)
			id = 0;
		boardSizeKnown[this.agentId] = false;
		positionTabu = new ArrayList<Position>();

	}

	//private class Position to hold row,column coordinates
	private class Position {
			public int row, column;
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
		}
	public void positionChanger(Position position,int direction) {
		if (agentId == 1)
			System.out.printf("Current position (2) (%d,%d)\n",position.row,position.column);
		if (direction == AgentAction.MOVE_NORTH)
			position.set(position.row+1,position.column);
		else if (direction == AgentAction.MOVE_SOUTH) 
			position.set(position.row-1,position.column);
		else if (direction == AgentAction.MOVE_EAST)
			position.set(position.row,position.column+1);
		else 
			position.set(position.row,position.column-1);
		if (agentId == 1)
			System.out.printf("Position after proposed change (3) (%d,%d)\n",position.row,position.column);
	}
	public int move(int direction,AgentEnvironment inEnvironment){
		if (!positionKnown[agentId])
			return direction;
		System.out.printf("Attempting to move to %d at Position (%d,%d)",direction,position.row,position.column);
		Position candidatePosition = new Position(position);
		boolean obstNorth = inEnvironment.isObstacleNorthImmediate();
		boolean obstSouth = inEnvironment.isObstacleSouthImmediate();
		boolean obstEast = inEnvironment.isObstacleEastImmediate();
		boolean obstWest = inEnvironment.isObstacleWestImmediate();
		int action = AgentAction.DO_NOTHING;
		if (agentId == 1)
			System.out.printf("Current position (1) (%d,%d)\n",position.row,position.column);


		if (direction == AgentAction.MOVE_NORTH && !obstNorth){
			action = AgentAction.MOVE_NORTH;
			positionChanger(candidatePosition,action);
		}
		else if (direction == AgentAction.MOVE_WEST&& !obstWest){
			action = AgentAction.MOVE_WEST;
			positionChanger(candidatePosition,action);
		}
		else if (direction == AgentAction.MOVE_EAST && !obstEast){
			action = AgentAction.MOVE_EAST;
			positionChanger(candidatePosition,action);
		}
		else if (direction == AgentAction.MOVE_SOUTH && !obstSouth){
			action = AgentAction.MOVE_SOUTH;
			positionChanger(candidatePosition,action);
		}
		action =tryMove(candidatePosition,action);
		if (agentId == 1){
			System.out.printf("Action is to move %d to (%d,%d)",action,candidatePosition.row,candidatePosition.column);
		}
		if (action == -1){
			while (action != -1){
				int roll = makeRandomPossibleMove();
				if (validMove(roll,inEnvironment)) {
					candidatePosition.set(position.row,position.column);
					positionChanger(candidatePosition,roll);
					action = tryMove(candidatePosition,roll);
				}
			}
		}
		if (positionTabu.size() != 0 && positionTabu.size() <=3)
			positionTabu.remove(0);
		if (action != AgentAction.DO_NOTHING)
			positionTabu.add(position);
		return action;
	}

	public int tryMove(Position candidate, int action){
		if (checkTabu(candidate)){
			position = candidate;
			return action;
		}
		return -1;
	}
	public boolean checkTabu(Position candidate) {
		for (Position position: positionTabu){
			if(position == candidate) {
				return false;
			}
		}
		return true;
	}

	public int makeRandomPossibleMove()	{
		double roll = Math.random();
		if (roll < .2)
			return AgentAction.MOVE_NORTH;
		else if (roll >= .2 && roll < .4)
			return AgentAction.MOVE_SOUTH;
		else if (roll >=.4 && roll < .6)
			return AgentAction.MOVE_WEST;
		else if (roll >= .6 && roll < .8)
			return AgentAction.MOVE_EAST;
		else
			return AgentAction.DO_NOTHING;
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
	}


	// implements Agent.getMove() interface
	public int getMove( AgentEnvironment inEnvironment ) {
		
		// booleans describing direction of goal
		// goal is either enemy flag, or our base

		boolean goalNorth;
		boolean goalSouth;
		boolean goalEast;
		boolean goalWest;
		boolean justTagged = false;
		//find what the starting corner is
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
			if (agentId == 1)
				System.out.println("Agent 1 is  at  position (" + position.row + ',' + position.column);
		}
			//System.out.println("Agent 1 is  at  position (" + position.row + ',' + position.column);
		//anything after this point will know the dimensions of the board and the position of each agent

		//check if the agent has just been tagged
		//see if its in its starting corner and to the left/right of its base
		if (!inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseNorth(inEnvironment.OUR_TEAM,true) && !inEnvironment.isBaseSouth(inEnvironment.OUR_TEAM,true)){
			if( (startingCorner == 0 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate()) 
				|| (startingCorner == 1 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
					|| (startingCorner == 2 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
						|| (startingCorner == 3 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleEastImmediate())) {
				justTagged = true;
				System.out.println("Just tagged");
				this.returnToStart();
			}
		}
		if( !inEnvironment.hasFlag() ) {
			// make goal the enemy flag
			goalNorth = inEnvironment.isFlagNorth( 
				inEnvironment.ENEMY_TEAM, false );
		
			goalSouth = inEnvironment.isFlagSouth( 
				inEnvironment.ENEMY_TEAM, false );
		
			goalEast = inEnvironment.isFlagEast( 
				inEnvironment.ENEMY_TEAM, false );
		
			goalWest = inEnvironment.isFlagWest( 
				inEnvironment.ENEMY_TEAM, false );
			}
		else {
			// we have enemy flag.
			// make goal our base
			goalNorth = inEnvironment.isBaseNorth( 
				inEnvironment.OUR_TEAM, false );
		
			goalSouth = inEnvironment.isBaseSouth( 
				inEnvironment.OUR_TEAM, false );
		
			goalEast = inEnvironment.isBaseEast( 
				inEnvironment.OUR_TEAM, false );
		
			goalWest = inEnvironment.isBaseWest( 
				inEnvironment.OUR_TEAM, false );
			}
		
		// now we have direction booleans for our goal	
		
		// check for immediate obstacles blocking our path		
		boolean obstNorth = inEnvironment.isObstacleNorthImmediate();
		boolean obstSouth = inEnvironment.isObstacleSouthImmediate();
		boolean obstEast = inEnvironment.isObstacleEastImmediate();
		boolean obstWest = inEnvironment.isObstacleWestImmediate();
		
		
		// if the goal is north only, and we're not blocked
		if( goalNorth && ! goalEast && ! goalWest && !obstNorth ) {
			// move north
			return move(AgentAction.MOVE_NORTH,inEnvironment);
			}
			
		// if goal both north and east
		if( goalNorth && goalEast ) {
			// pick north or east for move with 50/50 chance
			if( Math.random() < 0.5 && !obstNorth ) {
				return move(AgentAction.MOVE_NORTH,inEnvironment);
				}
			if( !obstEast ) {	
				return move(AgentAction.MOVE_EAST,inEnvironment);
				}
			if( !obstNorth ) {	
				return move(AgentAction.MOVE_NORTH,inEnvironment);
				}
			}	
			
		// if goal both north and west	
		if( goalNorth && goalWest ) {
			// pick north or west for move with 50/50 chance
			if( Math.random() < 0.5 && !obstNorth ) {
				return move(AgentAction.MOVE_NORTH,inEnvironment);
				}
			if( !obstWest ) {	
				return move(AgentAction.MOVE_WEST,inEnvironment);
				}
			if( !obstNorth ) {	
				return move(AgentAction.MOVE_NORTH,inEnvironment);
				}	
			}
		
		// if the goal is south only, and we're not blocked
		if( goalSouth && ! goalEast && ! goalWest && !obstSouth ) {
			// move south
			return move(AgentAction.MOVE_SOUTH,inEnvironment);
			}
		
		// do same for southeast and southwest as for north versions	
		if( goalSouth && goalEast ) {
			if( Math.random() < 0.5 && !obstSouth ) {
				return move(AgentAction.MOVE_SOUTH,inEnvironment);
				}
			if( !obstEast ) {
				return move(AgentAction.MOVE_EAST,inEnvironment);
				}
			if( !obstSouth ) {
				return move(AgentAction.MOVE_SOUTH,inEnvironment);
				}
			}
				
		if( goalSouth && goalWest && !obstSouth ) {
			if( Math.random() < 0.5 ) {
				return move(AgentAction.MOVE_SOUTH,inEnvironment);
				}
			if( !obstWest ) {
				return move(AgentAction.MOVE_WEST,inEnvironment);
				}
			if( !obstSouth ) {
				return move(AgentAction.MOVE_SOUTH,inEnvironment);
				}
			}
		
		// if the goal is east only, and we're not blocked
		if( goalEast && !obstEast ) {
			return move(AgentAction.MOVE_EAST,inEnvironment);
			}
		
		// if the goal is west only, and we're not blocked	
		if( goalWest && !obstWest ) {
			return move(AgentAction.MOVE_WEST,inEnvironment);
			}	
		
		// otherwise, make any unblocked move

		if( !obstNorth ) {
			return move(AgentAction.MOVE_NORTH,inEnvironment);
			}
		else if( !obstSouth ) {
			return move(AgentAction.MOVE_SOUTH,inEnvironment);
			}
		else if( !obstEast ) {
			return move(AgentAction.MOVE_EAST,inEnvironment);
			}
		else if( !obstWest ) {
			return move(AgentAction.MOVE_WEST,inEnvironment);
			}	
		else {
			// completely blocked!	
			return AgentAction.DO_NOTHING;
			}	
		}

	}