package tokenizer;

public class StringMessage implements Message<StringMessage> {
	private final String message;
	
	public StringMessage(String message){
		this.message=message;
	}

	public String getMessage(){
		return message;
	}
	
	@Override
	public String toString() {
		return message;
	}
	
	@Override
	public boolean equals(Object other) {
		return message.equals(other);
	}
	
	/**
	 * extracts the command from the message
	 * @return the command
	 */
	public String getCommand(){
		if (message.contains(" "))
			return message.substring(0,message.indexOf(" "));
		return message;
	}
	
	/**
	 * extracts the parameters from the message
	 * @return the parameters
	 * returns "null" if the parameter is empty
	 */
	public String getParam(){
		if (message.indexOf(" ")!=-1)
			return message.substring(message.indexOf(" ")+1);
		else
			return "null";
	}
}
