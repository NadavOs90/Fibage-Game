package Server;
import java.io.*;
import java.net.*;
import java.nio.charset.Charset;

import protocol.*;
import tokenizer.FixedSeparatorMessageTokenizer;
import tokenizer.MessageTokenizer;
import tokenizer.StringMessage;
import tokenizer.TokenizerFactory;


class ConnectionHandler implements Runnable {
	
	private BufferedReader in;
	private PrintWriter out;
	private MessageTokenizer<StringMessage> tokenizer;
	Socket clientSocket;
	private AsyncServerProtocol<StringMessage> protocol;
	private final ProtocolCallback<StringMessage> callback;
	
	public ConnectionHandler(Socket acceptedSocket, AsyncServerProtocol<StringMessage> p, MessageTokenizer<StringMessage> tok)
	{
		in = null;
		out = null;
		clientSocket = acceptedSocket;
		protocol = p;
		tokenizer= tok;
		callback= new ProtocolCallback<StringMessage>(){
			public void sendMessage(StringMessage msg) throws IOException {
				if (msg!=null){
					tokenizer.addBytes(tokenizer.getBytesForMessage(msg));
					while (tokenizer.hasMessage())
						out.println(tokenizer.nextMessage().toString());
				}
			}};
		System.out.println("Accepted connection from client!");
		System.out.println("The client is from: " + acceptedSocket.getInetAddress() + ":" + acceptedSocket.getPort());
	}
	
	public void run()
	{	
		try {
			initialize();
		}
		catch (IOException e) {
			System.out.println("Error in initializing I/O");
		}

		try {
			process();
		} 
		catch (IOException e) {
			System.out.println("Error in I/O");
		} 
		
		System.out.println("Connection closed - bye bye...");
		close();

	}
	
	public void process() throws IOException
	{
		String msg;
		
		while ((msg = in.readLine()) != null)
		{
			StringMessage message= new StringMessage(msg);
			protocol.processMessage(message, callback);
			
			if (protocol.isEnd(message))
			{
				break;
			}
			
		}
	}
	
	// Starts listening
	public void initialize() throws IOException
	{
		// Initialize I/O
		in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream(),"UTF-8"));
		out = new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(),"UTF-8"), true);
		System.out.println("I/O initialized");
	}
	
	// Closes the connection
	public void close()
	{
		try {
			if (in != null)
			{
				in.close();
			}
			if (out != null)
			{
				out.close();
			}
			
			clientSocket.close();
		}
		catch (IOException e)
		{
			System.out.println("Exception in closing I/O");
		}
	}
	
}

class MultipleClientProtocolServer implements Runnable {
	private ServerSocket serverSocket;
	private int listenPort;
	private ServerProtocolFactory<StringMessage> factory;
	private TokenizerFactory<StringMessage> tokenizerMaker;
	
	
	public MultipleClientProtocolServer(int port, ServerProtocolFactory<StringMessage> p)
	{
		serverSocket = null;
		listenPort = port;
		factory = p;
		tokenizerMaker= new TokenizerFactory<StringMessage>() {
            public MessageTokenizer<StringMessage> create() {
                return new FixedSeparatorMessageTokenizer("\n", Charset.forName("UTF-8"));
            }
        };
	}
	
	public void run()
	{
		try {
			serverSocket = new ServerSocket(listenPort);
			System.out.println("Listening...");
		}
		catch (IOException e) {
			System.out.println("Cannot listen on port " + listenPort);
		}
		
		while (true)
		{
			try {
				ConnectionHandler newConnection = new ConnectionHandler(serverSocket.accept(), factory.create(), tokenizerMaker.create());
            new Thread(newConnection).start();
			}
			catch (IOException e)
			{
				System.out.println("Failed to accept on port " + listenPort);
			}
		}
	}
	

	// Closes the connection
	public void close() throws IOException
	{
		serverSocket.close();
	}
	
	public static void main(String[] args) throws IOException
	{
		// Get port
		int port = Integer.decode(args[0]).intValue();
		
		MultipleClientProtocolServer server = new MultipleClientProtocolServer(port, new ServerProtocolFactory<StringMessage>() {
            public AsyncServerProtocol<StringMessage> create() {
                return new EchoProtocol();
            }});
		Thread serverThread = new Thread(server);
		serverThread.start();
		try {
			serverThread.join();
		}
		catch (InterruptedException e)
		{
			System.out.println("Server stopped");
		}
		
		
				
	}
}
