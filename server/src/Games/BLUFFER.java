package Games;

import java.io.BufferedReader;
import java.io.FileReader;
import com.google.gson.Gson;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import protocol.ProtocolCallback;
import tokenizer.StringMessage;

/**
 * the bluffer game implementation.
 *
 */
public class BLUFFER implements GameType {
	
	private question[] questions;		//the question database.
	private LinkedList<String> _players;		//the players playing the game
	private ConcurrentHashMap<String,Integer> _score;		//the final scores of all the players
	private ConcurrentHashMap<String,Integer> _roundScore;		//the score for a current round
	private ConcurrentHashMap<String,String> answerPerPlayer;		//the answer that each player answered during the current round
	private ConcurrentHashMap<String,Boolean> _playerAlreadyAnswered;		//flags if a player has picked an answer already during the current round, gets true if his answer is correct.
	private String[] answers;		//randomly positions the answers of the current round
	private int amountOfAns;		//the amount of players who have submitted their answers
	private String currAnswer;		//the correct answer for the current round
	private Room _room;				//the room playing this game
	private int numQuesAsked;		//the amount of questions that have been asked
	private int[] randomIndex;		//an array to help randomize the indexes of the players answers

	/**
	 * constructor
	 * @param room the room that started the game
	 */
	public BLUFFER(Room room) {
		_players = new LinkedList<String>();
		_score = new ConcurrentHashMap<String,Integer>();
		currAnswer="";
		_room=room;
		numQuesAsked=0;
		Gson gson = new Gson();
		JsonObject o = null;
		BufferedReader jsonFile = null;
		try{
			jsonFile = new BufferedReader(new FileReader("bluffer.json"));
			o = gson.fromJson(jsonFile, JsonObject.class);
			jsonFile.close();
		} catch (IOException e){} 
		questions = o.getQuestion();
	}
	
	/**
	 * 
	 * @return the final scores of all the players
	 */
	public String gameSummary(){
		String ans = "";
		Iterator<String> it = _players.iterator();
		while(it.hasNext()){
			String playerName = it.next();
			ans += playerName + ": " + _score.get(playerName).toString() + "pts, ";
		}
		ans= ans.substring(0,ans.length()-2);
		_room.endGame();
		return ans;
	}
	
	/**
	 * picks a random question from the database 
	 * initializes all the current round fields
	 * sends the question to the players
	 */
	public void askQuestion(){
		if(numQuesAsked<3){
			answers= new String[_players.size()+1];
			randomIndex= new int[_players.size()+1];
			for (int i=0; i<randomIndex.length; i++)
				randomIndex[i]=i;
			_playerAlreadyAnswered = new ConcurrentHashMap<String,Boolean>();
			_roundScore = new ConcurrentHashMap<String,Integer>();
			answerPerPlayer= new ConcurrentHashMap<String,String>(_players.size());
			for(int i=0; i<answers.length; i++)
				answers[i] = null;
			int random = (int) (Math.random() * questions.length);
			amountOfAns = 0;
			while (questions[random] == null)
				random = (int) (Math.random() * questions.length);
			String questionTxt= questions[random].getQuestionText();
			currAnswer= questions[random].getRealAnswer();
			questions[random] = null;
			Iterator<String> it = _players.iterator();
			while(it.hasNext()){
				String playerName = it.next();
				_room.sendMessage("ASKTXT "+questionTxt+"\n", playerName);
			}
			numQuesAsked++;
		}
		else{
			Iterator<String> it = _players.iterator();
			while(it.hasNext()){
				String playerName = it.next();
					_room.sendMessage("GAMEMSG Summary: "+gameSummary()+"\n", playerName);
			}
		}
	}
	
	/**
	 * adds the response of the player to the relevant databases
	 * sends all the players the choices, when all players have submitted their responses
	 * @param	res the player response
	 * @param	the players nick name
	 */
	public synchronized void addResponse(String res, String player){
		
		if (answerPerPlayer.containsValue(player)){
			_room.sendMessage("GAMEMSG Error you have already answered \n", player);
			return;
		}
		if (res.equals(currAnswer)){
			_room.sendMessage("GAMEMSG Congratulations you responded the correct answer!! please add a new false response \n", player);
			return;
		}
		answerPerPlayer.put(res, player);
		int random=(int) (Math.random() * randomIndex.length);
		answers[randomIndex[random]] = res;
		randomIndex[random]=-1;
		int index=0;
		int[] temp=new int[randomIndex.length-1];
		for (int i=0; i<randomIndex.length; i++){
			if (randomIndex[i]!=-1){
				temp[index]=randomIndex[i];
				index++;
			}
		}
		randomIndex=temp;
		amountOfAns++;
		if (amountOfAns >= _players.size()){
			for (int i=0; i<answers.length; i++)
				if (answers[i]==null)
					answers[i]=currAnswer;
			String ans = "";
			for (int i=0; i<answers.length; i++)
				ans += i + "." + answers[i] + " ";
			Iterator<String> it = _players.iterator();
			while(it.hasNext()){
				String playerName = it.next();
					_room.sendMessage("ASKCHOICES " + ans + "\n", playerName);
			}
		}
	}
		
	@Override
	public void addPlayers(ConcurrentHashMap<String, ProtocolCallback<StringMessage>> players) {
		Iterator<String> it = players.keySet().iterator();
		while(it.hasNext()){
			String temp= it.next();
			_players.add(temp);
			_score.put(temp, 0);
		}
	}
	
	/**
	 * starts the next round
	 */
	@Override
	public void play() {
		askQuestion();
	}

	/**
	 * add the player's choice to the database
	 * sends all the players the round result, after all the choices have been submitted
	 */
	@Override
	public synchronized void addChoice(String choice, String playerName) {
		int choiceNum = Integer.parseInt(choice);
		if (_playerAlreadyAnswered.containsKey(playerName)){
			_room.sendMessage("GAMEMSG Error cant make more then one choice\n", playerName);
			return;
		}
		if(choiceNum<0 || choiceNum >= answers.length){
			_room.sendMessage("GAMEMSG Error Incorrect choice\n", playerName);
			return;
		}
		else{
			_playerAlreadyAnswered.put(playerName, false);
			if(!_roundScore.containsKey(playerName))
				_roundScore.put(playerName,0);
			if(answers[choiceNum] == currAnswer){
				_playerAlreadyAnswered.put(playerName, true);
				if(_roundScore.containsKey(playerName))
					_roundScore.put(playerName, _roundScore.get(playerName).intValue()+10);
				else
					_roundScore.put(playerName,10);
				_score.put(playerName, _score.get(playerName).intValue()+10);
			}
			else{
				if(_roundScore.containsKey(answerPerPlayer.get(answers[choiceNum])))
						_roundScore.put(answerPerPlayer.get(answers[choiceNum]), _roundScore.get(answerPerPlayer.get(answers[choiceNum])).intValue()+5);
				else
					_roundScore.put(answerPerPlayer.get(answers[choiceNum]),5);
				_score.put(answerPerPlayer.get(answers[choiceNum]), _score.get(answerPerPlayer.get(answers[choiceNum])).intValue()+5);
			}
		}
		if(_playerAlreadyAnswered.size() == _players.size()){
			Iterator<String> it = _players.iterator();
			while(it.hasNext()){
				String player = it.next();
				_room.sendMessage("GAMEMSG The correct answer is: " + currAnswer + "\n", player);
				if(_playerAlreadyAnswered.get(player).booleanValue())
					_room.sendMessage("GAMEMSG correct! +" + _roundScore.get(player) + "pts\n", player);
				else _room.sendMessage("GAMEMSG wrong! +" + _roundScore.get(player) + "pts\n", player);
			}
			play();
		}	
	}
	
	public boolean allAnswered(){
		return amountOfAns == _players.size();
	}

	@Override
	public void removePlayer(String name) {
		_players.remove(name);
		if (_playerAlreadyAnswered.containsKey(name))
			amountOfAns--;
	}
	
	/**
	 * the class for the questions in the bluffer questions database
	 *
	 */
	public static class question{
		private String questionText;
		private String realAnswer;
		
		public String getRealAnswer() {
			return realAnswer;
		}

		public String getQuestionText() {
			return questionText;
		}
	}
}
