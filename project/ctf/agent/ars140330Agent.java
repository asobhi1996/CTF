package ctf.agent;


import ctf.common.AgentEnvironment;
import ctf.agent.Agent;

import ctf.common.AgentAction;

public class ars140330Agent extends Agent {
	public static int boardSize = 1;
	public static int id = 0;
	public static boolean[] boardSizeKnown = {false,false};
	public static boolean[] positionKnown = {false,false};
	public int agentId;
	public int beginningSteps = 0;
	public int startingCorner; //0 indicates NorthWest, 1 NorthEast, 2 SouthWest, 3 Southeast
	public Position position;
	public Position startPosition;
	public boolean cornerKnown = false;
	public ars140330Agent(){
		this.agentId = id++;
		if (id == 2)
			id = 0;
		boardSizeKnown[this.agentId] = false;

	}
	private class Position {
			int row, column;
			Position(int row, int column) {
				this.row = row;
				this.column = column;
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
				startPosition = new Position(0,0);
				position = new Position(beginningSteps,0);
			}
			else if (startingCorner == 1) {
				startPosition = new Position(0,boardSize - 1);
				position = new Position(beginningSteps,boardSize-1);
			}
			else if (startingCorner == 2) {
				startPosition = new Position(boardSize - 1,0);
				position = new Position(boardSize-1 - beginningSteps,0);
			}
			else {
				startPosition = new Position(boardSize - 1,boardSize - 1);
				position = new Position(boardSize-1 - beginningSteps, boardSize-1);
			}
			positionKnown[agentId] = true;
		}
		//anything after this point will know the dimensions of the board and the position of each agent

		//check if the agent has just been tagged
		//see if its in its starting corner and to the left/right of its base
		boolean justTagged = false;
		if (!inEnvironment.isBaseWest(inEnvironment.OUR_TEAM,false) && !inEnvironment.isBaseEast(inEnvironment.OUR_TEAM,false)){
			if( (startingCorner == 0 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleWestImmediate()) 
				|| (startingCorner == 1 && inEnvironment.isObstacleNorthImmediate() && inEnvironment.isObstacleEastImmediate())
					|| (startingCorner == 2 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleWestImmediate())
						|| (startingCorner == 3 && inEnvironment.isObstacleSouthImmediate() && inEnvironment.isObstacleEastImmediate()))
				justTagged = true;
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
			return AgentAction.MOVE_NORTH;
			}
			
		// if goal both north and east
		if( goalNorth && goalEast ) {
			// pick north or east for move with 50/50 chance
			if( Math.random() < 0.5 && !obstNorth ) {
				return AgentAction.MOVE_NORTH;
				}
			if( !obstEast ) {	
				return AgentAction.MOVE_EAST;
				}
			if( !obstNorth ) {	
				return AgentAction.MOVE_NORTH;
				}
			}	
			
		// if goal both north and west	
		if( goalNorth && goalWest ) {
			// pick north or west for move with 50/50 chance
			if( Math.random() < 0.5 && !obstNorth ) {
				return AgentAction.MOVE_NORTH;
				}
			if( !obstWest ) {	
				return AgentAction.MOVE_WEST;
				}
			if( !obstNorth ) {	
				return AgentAction.MOVE_NORTH;
				}	
			}
		
		// if the goal is south only, and we're not blocked
		if( goalSouth && ! goalEast && ! goalWest && !obstSouth ) {
			// move south
			return AgentAction.MOVE_SOUTH;
			}
		
		// do same for southeast and southwest as for north versions	
		if( goalSouth && goalEast ) {
			if( Math.random() < 0.5 && !obstSouth ) {
				return AgentAction.MOVE_SOUTH;
				}
			if( !obstEast ) {
				return AgentAction.MOVE_EAST;
				}
			if( !obstSouth ) {
				return AgentAction.MOVE_SOUTH;
				}
			}
				
		if( goalSouth && goalWest && !obstSouth ) {
			if( Math.random() < 0.5 ) {
				return AgentAction.MOVE_SOUTH;
				}
			if( !obstWest ) {
				return AgentAction.MOVE_WEST;
				}
			if( !obstSouth ) {
				return AgentAction.MOVE_SOUTH;
				}
			}
		
		// if the goal is east only, and we're not blocked
		if( goalEast && !obstEast ) {
			return AgentAction.MOVE_EAST;
			}
		
		// if the goal is west only, and we're not blocked	
		if( goalWest && !obstWest ) {
			return AgentAction.MOVE_WEST;
			}	
		
		// otherwise, make any unblocked move
		if( !obstNorth ) {
			return AgentAction.MOVE_NORTH;
			}
		else if( !obstSouth ) {
			return AgentAction.MOVE_SOUTH;
			}
		else if( !obstEast ) {
			return AgentAction.MOVE_EAST;
			}
		else if( !obstWest ) {
			return AgentAction.MOVE_WEST;
			}	
		else {
			// completely blocked!
			return AgentAction.DO_NOTHING;
			}	
		}

	}