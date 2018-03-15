package Games;


import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import protocol.ProtocolCallback;
import tokenizer.StringMessage;

public class Room {
	private final String _name;
	private ConcurrentHashMap<String, ProtocolCallback<StringMessage>> _players;
	private GameType _game;
	private boolean _started;

	public Room(String name) {
		_name= name;
		_players= new ConcurrentHashMap<String, ProtocolCallback<StringMessage>>();
		_game= null;
		_started=false;
	}
	
	/**
	 * ends the game
	 */
	public void endGame(){
		_started = false;
	}
	
	public String getName(){
		return _name;
	}
	
	public ConcurrentHashMap<String, ProtocolCallback<StringMessage>> getPlayers(){
		return _players;
	}
	
	/**
	 * 
	 * @return the game that has been started
	 * returns null if no game has started
	 */
	public GameType getGame(){
		return _game;
	}
	
	public void addPlayer(String name, ProtocolCallback<StringMessage> callback){
		if (!_started)
			_players.put(name, callback);
	}
	
	public void removePlayer(String name){
		synchronized (_players){
			_players.remove(name);
		}
		if(_game != null)
			_game.removePlayer(name);
	}

	public boolean containsPlayer(String name) {
		synchronized (_players){
			return _players.containsKey(name);
		}
	}

	/**
	 * initializes and starts the game
	 * @param gameType the game to start
	 */
	public void assignGameType(GameType gameType) {
		_game= gameType;
		_game.addPlayers(_players);
		_started=true;
		_game.play();
	}

	/**
	 * sends a message to a specific player in the room
	 * @param msg the message to send
	 * @param playerName the message receiver 
	 */
	public void sendMessage(String msg, String playerName) {
		try {
			_players.get(playerName).sendMessage(new StringMessage(msg));
		} catch (IOException e) {e.printStackTrace();}
		
	}
	
	/**
	 * 
	 * @return true if a game has started in the room
	 */
	public boolean gameStarted(){
		return _started;
	}

	/**
	 * sends all the players a message except for the sender
	 * @param msg the message to send
	 * @param playerName the senders name
	 */
	public void sendUserMessage(String msg, String playerName) {
		Iterator<String> it = _players.keySet().iterator();
		while(it.hasNext()){
			String player = it.next();
			if(player != playerName)
				sendMessage("USRMSG "+ playerName+ ": " + msg + "\n", player);
		}
	}

}
