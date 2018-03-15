package Games;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import protocol.ProtocolCallback;
import tokenizer.StringMessage;

/**
 * a singleton class for all the games
 * contains all the games, rooms and players
 *
 */
public class Game {
	private ConcurrentHashMap<ProtocolCallback<StringMessage>, String> _nicks;
	private ConcurrentHashMap<String,Room> _rooms;
	private LinkedList<String> _supportedGames;
	
	private static class SingletoneHolder{
		private static Game instance = new Game();
	}
	
	private Game(){
		_nicks= new ConcurrentHashMap<ProtocolCallback<StringMessage>, String>();
		_rooms= new ConcurrentHashMap<String,Room>();
		_supportedGames= new LinkedList<String>();
		addGame("BLUFFER");
	}
	
	public static Game getInstance(){
		return SingletoneHolder.instance;
	}
	
	public boolean hasPlayer(String name){
		synchronized (_nicks){
			return _nicks.containsValue(name);
		}
	}
	
	public boolean hasPlayer(ProtocolCallback<StringMessage> callback){
		return _nicks.containsKey(callback);
	}
	
	public void addPlayer(String name, ProtocolCallback<StringMessage> callback){
		synchronized (_nicks){
			_nicks.put(callback, name);
		}
	}
	
	public void removePlayer(ProtocolCallback<StringMessage> callback){
		synchronized (_nicks){
			_nicks.remove(callback);
		}
	}

	/**
	 * attempts to join the player to a specific room
	 * if the room doesn't exist, creates a new room with the specified name
	 * @param roomName the room name to join or create
	 * @param callback	the unique object assigned to the player
	 * @return true if the room existed 
	 */
	public boolean join(String roomName, ProtocolCallback<StringMessage> callback) {
		String playerName=_nicks.get(callback);
		if (getRoomGame(callback)!=null)
			getRoomGame(callback).removePlayer(playerName);
		synchronized (_rooms){
			if (_rooms.containsKey(roomName)){
				_rooms.get(roomName).addPlayer(playerName, callback);
				return true;
			}
			Room room= new Room(roomName);
			room.addPlayer(playerName, callback);
			_rooms.put(roomName, room);
			return false;
		}
	}
	
	/**
	 * check if a game has started in the specific room
	 * @param roomName the name of the room to check
	 * @return true if a game has started in the room
	 */
	public boolean gameStarted(String roomName){
		if(_rooms.containsKey(roomName))
			return _rooms.get(roomName).gameStarted();
		return false;
	}
	
	/**
	 * adds games to the playable games list
	 * @param game the game name to add
	 */
	public void addGame(String game){
		if (!_supportedGames.contains(game))
			_supportedGames.add(game);
	}
	
	/**
	 * removes games from the playable games list
	 * @param game	the game name to remove
	 */
	public void removeGame(String game){
		if (_supportedGames.contains(game))
			_supportedGames.remove(game);
	}

	/**
	 * 
	 * @return the list of playable games
	 */
	public String getGamesList() {
		String ans="";
		for (int i=0; i<_supportedGames.size(); i++)
			ans=ans+_supportedGames.get(i)+" ";
		return ans;
	}
	
	/**
	 * 
	 * @param game the game name to start
	 * @param callback the object associated with the player to find the relevant room 
	 * @return true if the game is playable
	 */
	public boolean gameStart(String game, ProtocolCallback<StringMessage> callback){
		if (!_supportedGames.contains(game))
			return false;
		else
			switch (game){
			case "BLUFFER":
				Room temp=getRoomGame(callback);
				if (temp!=null){
					temp.assignGameType(new BLUFFER(temp));
				}
				break;
		}
		return true;
			
	}
	
	/**
	 * gets the room that the player joined
	 * @param callback the unique object that is assigned to the relevant player
	 * @return the room the player joined
	 */
	public Room getRoomGame(ProtocolCallback<StringMessage> callback){
		Iterator<Room> it=_rooms.values().iterator();
		while (it.hasNext()){
			Room temp= it.next();
			if (temp.containsPlayer(_nicks.get(callback)))
				return temp;
		}
		return null;
	}
	
	public String getPlayerName(ProtocolCallback<StringMessage> callback){
		return _nicks.get(callback);
	}
		
}
