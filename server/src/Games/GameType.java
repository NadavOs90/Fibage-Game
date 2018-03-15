package Games;

import java.util.concurrent.ConcurrentHashMap;

import protocol.ProtocolCallback;
import tokenizer.StringMessage;

/**
 * general interface that all supported games must implement
 *
 */
public interface GameType {

	void addPlayers(ConcurrentHashMap<String, ProtocolCallback<StringMessage>> _players);
	void addResponse(String res, String player);
	void play();
	void addChoice(String param, String playerName);
	boolean allAnswered();
	void removePlayer(String name);

}
