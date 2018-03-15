package protocol;

import java.io.IOException;

import Games.Game;
import tokenizer.StringMessage;

/**
 * a simple implementation of the server protocol interface
 */
public class EchoProtocol implements AsyncServerProtocol<StringMessage> {

	private boolean _shouldClose = false;
	private boolean _connectionTerminated = false;
	

	/**
	 * processes a message<BR>
	 * this simple interface prints the message to the screen, then composes a simple
	 * reply and sends it back to the client
	 *
	 * @param msg the message to process
	 * @param callback a unique object associated to the client, used to communicate with the server
	 * @return the reply that should be sent to the client, or null if no reply needed
	 */
	@Override
	public void processMessage(StringMessage msg, ProtocolCallback<StringMessage> callback) {        
		String result="UNIDENTIFIED";
		String optional="";
		boolean msgProcessed = true;
		switch (msg.getCommand()){
		
		case "NICK":
			if (Game.getInstance().hasPlayer(msg.getParam())){ 				//checks if the requested nick name is already in use.
				optional="nick already in use.";
				result="REJECTED";
				break;
			}
			if (Game.getInstance().hasPlayer(callback)){					//checks if the client already has a nick name
				result= "REJECTED";
				optional ="Nick allready created";
				break;
			}
			Game.getInstance().addPlayer(msg.getParam(), callback);
			result="ACCEPTED";
			break;
			
		case "JOIN":
			if (!Game.getInstance().hasPlayer(callback)){					//checks if the client has a registered nick name.
				result= "REJECTED";
				optional ="Nick not created";
				break;
			}
			if (Game.getInstance().getRoomGame(callback)!=null && Game.getInstance().getRoomGame(callback).gameStarted()){
				result= "REJECTED";
				optional ="A game has already started in your room " + msg.getParam();
				break;
			}
			if(Game.getInstance().gameStarted(msg.getParam())){				//checks if a game has already started in the room
				result= "REJECTED";
				optional ="A game has already started in room " + msg.getParam();
				break;
			}
			if (Game.getInstance().join(msg.getParam(), callback))
				optional="Joined existing room "+msg.getParam();
			else
				optional="Created and joined new room "+msg.getParam();
			result="ACCEPTED";
			break;
			
		case "LISTGAMES":
			optional=Game.getInstance().getGamesList();
			result="ACCEPTED";
			break;
			
		case "STARTGAME":
			if (!Game.getInstance().hasPlayer(callback)){					//checks if the client has a registered nick name.
				result= "REJECTED";
				optional ="Nick not created";
				break;
			}
			if (Game.getInstance().getRoomGame(callback)==null){				//checks if the client has joined a room.
				result= "REJECTED";
				optional ="Hasn't Joined a room";
				break;
			}
			try {
				callback.sendMessage(new StringMessage("SYSMSG " + msg.getCommand() +" ACCEPTED\n"));
			} catch (IOException e) {e.printStackTrace();}
			msgProcessed = false;
			if (Game.getInstance().gameStart(msg.getParam(), callback))
				break;
			else{
				msgProcessed =  true;
				result="REJECTED";
				optional="UNABLE TO START GAME";
			}
			break;
			
		case "TXTRESP":
			if (!Game.getInstance().hasPlayer(callback)){					//checks if the client has a registered nick name.
				result= "REJECTED";
				optional ="Nick not created";
				break;
			}
			if (Game.getInstance().getRoomGame(callback)==null){			//checks if the client has joined a room.
				result= "REJECTED";
				optional ="Hasn't Joined a room";
				break;
			}
			if (!Game.getInstance().getRoomGame(callback).gameStarted()){		//checks if the game started
				result= "REJECTED";
				optional ="No game has started in this room";
				break;
			}
			msgProcessed = false;
			try {
				callback.sendMessage(new StringMessage("SYSMSG " + msg.getCommand() +" ACCEPTED\n"));
			} catch (IOException e) {e.printStackTrace();}
			Game.getInstance().getRoomGame(callback).getGame().addResponse(msg.getParam().toLowerCase(), Game.getInstance().getPlayerName(callback));
			break;
			
		case "SELECTRESP":
			if (!Game.getInstance().hasPlayer(callback)){					//checks if the client has a registered nick name.
				result= "REJECTED";
				optional ="Nick not created";
				break;
			}
			if (Game.getInstance().getRoomGame(callback)==null){			//checks if the client has joined a room.
				result= "REJECTED";
				optional ="Hasn't Joined a room";
				break;
			}
			if (!Game.getInstance().getRoomGame(callback).gameStarted()){	//checks if a game has been started already. 
				result= "REJECTED";
				optional ="No game has started in this room";
				break;
			}
			if (!Game.getInstance().getRoomGame(callback).getGame().allAnswered()){		//checks if the client can send his choice.
				result= "REJECTED";
				optional ="No available choices yet";
				break;
			}
			try{Integer.parseInt(msg.getParam());}					//checks if the client's choice is convertible to integer
			catch (NumberFormatException e){
				result= "REJECTED";
				optional ="Your choise is not numeric";
				break;
			}
			msgProcessed = false;
			try {
				callback.sendMessage(new StringMessage("SYSMSG " + msg.getCommand() +" ACCEPTED\n"));
			} catch (IOException e) {e.printStackTrace();}
			Game.getInstance().getRoomGame(callback).getGame().addChoice(msg.getParam(), Game.getInstance().getPlayerName(callback));
			break;
			
		case "QUIT":
			if (!Game.getInstance().hasPlayer(callback)){			//checks if the player has a nick
				connectionTerminated();
				result= "ACCEPTED";
				break;
			}
			if (Game.getInstance().getRoomGame(callback) == null){	// checks if the player joined a room
				Game.getInstance().removePlayer(callback);
				connectionTerminated();
				result= "ACCEPTED";
				break;
			}
			else
				if (!Game.getInstance().getRoomGame(callback).gameStarted()){ //checks if the player is in the middle of a game
					Game.getInstance().getRoomGame(callback).removePlayer(Game.getInstance().getPlayerName(callback));
					Game.getInstance().removePlayer(callback);
					connectionTerminated();
					result= "ACCEPTED";
					break;
				}
			result= "REJECTED";
			optional= "can't quit in the middle of a game";
			break;
			
		case "MSG":
			if (!Game.getInstance().hasPlayer(callback)){						//checks if the client has a registered nick name.
				result= "REJECTED";
				optional ="Nick not created";
				break;
			}
			if (Game.getInstance().getRoomGame(callback)==null){				//checks if the client has joined a room.
				result= "REJECTED";
				optional ="Hasn't Joined a room";
				break;
			}
			Game.getInstance().getRoomGame(callback).sendUserMessage(msg.getParam(), Game.getInstance().getPlayerName(callback));
			result = "ACCEPTED";
			break;
		}
		if (msgProcessed)
			try {
				callback.sendMessage(new StringMessage("SYSMSG " + msg.getCommand() + " " + result + " " + optional + "\n"));
			} catch (IOException e) {e.printStackTrace();}
	}

	/**
	 * detetmine whether the given message is the termination message
	 *
	 * @param msg the message to examine
	 * @return whether the command is 'QUIT'
	 */
	@Override
	public boolean isEnd(StringMessage msg) {
		return msg.getCommand().equals("QUIT");
	}

	/**
	 * Is the protocol in a closing state?.
	 * When a protocol is in a closing state, it's handler should write out all pending data, 
	 * and close the connection.
	 * @return true if the protocol is in closing state.
	 */
	@Override
	public boolean shouldClose() {
		return this._shouldClose;
	}

	/**
	 * Indicate to the protocol that the client disconnected.
	 */
	@Override
	public void connectionTerminated() {
		this._connectionTerminated = true;
	}

}
