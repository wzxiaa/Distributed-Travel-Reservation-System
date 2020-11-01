package Server.TCP;

import java.util.*;

import java.io.*;
import java.net.*;
import Server.Common.*;

public class ServerSocketThread extends Thread {
	Socket socket;
	TCPMiddleware manager;

	public ServerSocketThread(Socket socket, TCPMiddleware manager) {
		this.socket = socket;
		this.manager = manager;
	}

	public void run() {
		try {
			BufferedReader inFromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter outToClient = new PrintWriter(socket.getOutputStream(), true);
			String message = null;
			while ((message = inFromClient.readLine()) != null) {
				System.out.println("message: " + message);
				Vector<String> arguments = new Vector<String>();
				arguments = parse(message);
				Command cmd = Command.fromString((String) arguments.elementAt(0));
				if (arguments.size() == 0) {
					outToClient.println("");
					inFromClient.close();
					outToClient.close();
					socket.close();
					return;
				}

				String result = manager.executeCommand(cmd, arguments, message);

				outToClient.println(result);
				inFromClient.close();
				outToClient.close();
				socket.close();
			}
		} catch (IOException e) {
			System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception" + e.toString());
		}
	}

	public static Vector<String> parse(String command) {
		if(command.charAt(0) == '[' && command.charAt(command.length()-1) == ']'){
			command = command.substring(1, command.length()-1);
		}
		Vector<String> arguments = new Vector<String>();
		StringTokenizer tokenizer = new StringTokenizer(command, ",");
		String argument = "";
		while (tokenizer.hasMoreTokens()) {
			argument = tokenizer.nextToken();
			argument = argument.trim();
			arguments.add(argument);
		}
		return arguments;
	}

}
