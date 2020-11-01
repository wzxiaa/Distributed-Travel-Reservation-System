package Server.TCP;

import java.net.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import Server.Common.*;
import Server.Interface.*;

import java.io.*;
import java.util.logging.Logger;

public class TCPMiddleware extends ResourceManager {
	private static String m_serverName = "Middleware";
	private static String m_rmiPrefix = "group_24_";
	private static String server_host_car;
	private static String server_host_room;
	private static String server_host_flight;

	private static ServerSocket serverSocket = null;
	private static TCPMiddleware socketMiddleware = null;

	private static int middleware_port = 3024;
	private static int threads = 10;

	private static int server_port_car = 4124;
	private static int server_port_room = 4224;
	private static int server_port_flight = 4324;
	private static ExecutorService executor = null;

	private static ClientSocket flightRM = null;
	private static ClientSocket carRM = null;
	private static ClientSocket roomRM = null;
	private static final Logger logger = Logger.getLogger(TCPResourceManager.class.getName());

	private static String s_serverName = "Server";
	// TODO: ADD YOUR GROUP NUMBER TO COMPLETE
	private static String s_rmiPrefix = "group_24_";

	public static void main(String[] args) {
		if (args.length > 0) {
			server_host_car=args[0];
			server_host_room=args[1];
			server_host_flight=args[2];
			s_serverName = args[3];
		}

		try {
			socketMiddleware = new TCPMiddleware(s_serverName);

			// Middleware must not block when it is waiting for the ResourceManagers to
			// execute a request.
			// Runtime.getRuntime().addShutdownHook(new Thread() {
			// public void run() {
			// try {
			// flightRM.stopClient();
			// carRM.stopClient();
			// roomRM.stopClient();
			// serverSocket.close();
			// System.out.println("'" + s_serverName + "' resource manager unbound");
			// } catch (Exception e) {
			// System.err.println(
			// (char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught
			// exception " + e.toString());
			// e.printStackTrace();
			// }
			// }
			// });

			System.out.println((char) 27 + "[31;Middleware starting to get input...");
			// while (true) {
			// serverSocket = new ServerSocket(middleware_port);
			// ServerSocketThread sthread = new ServerSocketThread(serverSocket.accept(),
			// socketMiddleware);
			// sthread.start();
			// }
			socketMiddleware.start(middleware_port);
		} catch (Exception e) {
			System.err.println(
					(char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception " + e.toString());
		}
	}

	public TCPMiddleware(String p_name) {
		super(p_name);
		flightRM = new ClientSocket(server_host_flight, server_port_flight);
		carRM = new ClientSocket(server_host_car, server_port_car);
		roomRM = new ClientSocket(server_host_room, server_port_room);

		flightRM.connect();
		carRM.connect();
		roomRM.connect();

		this.executor = Executors.newFixedThreadPool(threads);
		logger.info("TCPResourceManager " + p_name + " initialized.");
		logger.info("TCPResourceManager " + p_name + " has a thread pool of " + threads + " threads.");
	}

	public void start(int port) {
		try {
			serverSocket = new ServerSocket(port);
			logger.info("TCPResourceManager " + s_serverName + " binded to port " + port);
			while (true) {
				executor.submit(new Handler(serverSocket.accept()));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static class Handler implements Runnable {
		private Socket socket;
		private PrintWriter outToClient;
		private BufferedReader inFromClient;

		public Handler(Socket socket) {
			this.socket = socket;
			logger.info("Connected by " + this.socket.getRemoteSocketAddress().toString());
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

					String result = executeCommand(cmd, arguments, message);

					outToClient.println(result);
					inFromClient.close();
					outToClient.close();
					socket.close();
				}
			} catch (IOException e) {
				System.err.println((char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception "
						+ e.toString());
			}
		}
	}

	public static String executeCommand(Command cmd, Vector<String> arguments, String message) {
		try {
			switch (cmd) {
			case AddFlight: {
				try {
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to execute command: AddFlight";
				}
			}
			case AddCars: {
				try {
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to execute command: AddCars";
				}
			}
			case AddRooms: {
				try {
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to execute command: AddCars";
				}
			}
			case AddCustomer: {
				try {
					String xid = (String) arguments.elementAt(1);
					int cid = Integer.parseInt(xid +
					String.valueOf(Calendar.getInstance().get(Calendar.MILLISECOND)) +
					String.valueOf(Math.round(Math.random() * 100 + 1)));
					message = String.format("AddCustomerID,%s,%d",xid,cid);
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
						
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return String.valueOf(cid);
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to addCustomer to room server";
				}
			}
			case AddCustomerID: {
				try {
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
						
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to addCustomer to room server";
				}
			}
			case DeleteFlight: {
				try {
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}
				} catch (Exception e) {
					return "Failed to execute command: DeleteFlight";
				}
			}
			case DeleteCars: {
				try {
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
				} catch (Exception e) {
					return "Failed to execute command: DeleteCars";
				}
			}
			case DeleteRooms: {
				try {
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
				} catch (Exception e) {
					return "Failed to execute command: DeleteRooms";
				}
			}
			case DeleteCustomer: {
				try {
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
						
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to execute command: DeleteCustomer";
				}
			}
			case QueryFlight: {
				try {
					String res = flightRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					flightRM.connect();
					return flightRM.process(message);
				}
			}
			case QueryCars: {
				try {
					String res = carRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					carRM.connect();
					return carRM.process(message);
				}
			}
			case QueryRooms: {
				try {
					String res = roomRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					roomRM.connect();
					return roomRM.process(message);
				}
			}
			case QueryCustomer: {
				// TODO: handle this case
				try {
					synchronized (roomRM) {
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
						
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
					synchronized (carRM) {
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}

				} catch (Exception e) {
					return "Failed to execute command: QueryCustomer";
				}
			}
			case QueryFlightPrice: {
				try {
					String res = flightRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					flightRM.connect();
					return flightRM.process(message);
				}
			}
			case QueryCarsPrice: {
				try {
					String res = carRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					carRM.connect();
					return carRM.process(message);
				}
			}
			case QueryRoomsPrice: {
				try {
					String res = roomRM.process(message);
					if (res.equals(""))
						throw new IOException();
					return res;
				} catch (IOException e) {
					roomRM.connect();
					return roomRM.process(message);
				}
			}
			case ReserveFlight: {
				try {
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}
				} catch (Exception e) {
					return "Failed to execute command: ReserveFlight";
				}
			}
			case ReserveCar: {
				try{
					synchronized(carRM){
						try {
							String res = carRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							carRM.connect();
							return carRM.process(message);
						}
					}
					
				} catch (Exception e) {
					return "Failed to execute command: ReserveCar";
				}
			}
			case ReserveRoom: {
				try{
					synchronized(roomRM){
						try {
							String res = roomRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							roomRM.connect();
							return roomRM.process(message);
						}
					}
					
				} catch (Exception e) {
					return "Failed to execute command: ReserveRoom";
				}
			}
			case Bundle: {
				// TODO: implement bundle

				if (arguments.size() < 7) {
				System.err.println((char)27 + "[31;1mCommand exception: " + (char)27 +
				"[0mBundle command expects at least 7 arguments. Location \"help\" or \"help,<CommandName>\"");
				break;
				}

				try {
					synchronized (flightRM) {
						try {
							String res = flightRM.process(message);
							if (res.equals(""))
								throw new IOException();
							return res;
						} catch (IOException e) {
							flightRM.connect();
							return flightRM.process(message);
						}
					}
				} catch (Exception e) {
					return "Failed to execute command: DeleteRooms";
				}
			}
			}
		} catch (Exception e) {
			System.out.println(
					(char) 27 + "[31;1mMiddleware exception: " + (char) 27 + "[0mUncaught exception " + e.toString());
			e.printStackTrace(); 
		}
		return "";
	}

	public static Vector<String> parse(String command) {
		if (command.charAt(0) == '[' && command.charAt(command.length() - 1) == ']') {
			command = command.substring(1, command.length() - 1);
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
