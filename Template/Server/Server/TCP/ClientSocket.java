package Server.TCP;

import java.io.*;
import java.net.*;

public class ClientSocket {
	private String host;
	private int port;
	private Socket socket;
	private PrintWriter outToServer;
	private BufferedReader inFromServer;

	public ClientSocket(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public void connect() {
		try {
			boolean first = true;
			while (true) {
				try {
					socket = new Socket(host, port);
					outToServer = new PrintWriter(socket.getOutputStream(), true);
					// open an input string to an server
					inFromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					System.out.println("Connecting to host:" + host + ", port:" + port);
					break;
				} catch (IOException e) {
					if (first) {
						System.out.println("Waiting for host:" + host + ", port:" + port);
						first = false;
					}
				}
				Thread.sleep(500);
			}
		} catch (Exception e) {
			System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception\n" + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}

	public String process(String message) throws IOException {
		// send the user's input via the output stream to the server
		// receive the server's result via the input stream from the server
		// establish a new connection before finishing
		outToServer.println(message);
		String line = inFromServer.readLine();
		String res = line;
		// while (line != null) {
		// 	if (res.length() == 0)
		// 		res = line;
		// 	else
		// 		res = "\n" + line;
		// }
		connect();
		System.out.println("result: " + res); // print the server result to the user\
		return res;
	}

	public void stopClient() {
		try {
			inFromServer.close();
			outToServer.close();
			socket.close();
		} catch (Exception e) {
			System.err.println((char) 27 + "[31;1mMiddleware exception: cannot disconnect" + (char) 27
					+ "[0mUncaught exception: " + e.toString());
		}
	}
}
