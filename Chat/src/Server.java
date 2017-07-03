
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONArray;
import org.json.JSONObject;

public class Server {
	public static ArrayList<Socket> SocketList = new ArrayList<Socket>();
	public static JSONObject LogChat = new JSONObject();
	public static int chat_length = 1;
	public static final int N_THREADS = 5;
	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws IOException {
		final int port = 6969;
		//ExecutorService executorService = Executors.newFixedThreadPool(N_THREADS);
		System.out.println("Server waiting for connection on port " + port);
		ServerSocket serverSockets = new ServerSocket(port);
		SocketList.clear();
		while (true) {
			Socket clientSocket = null;
			try {
				
				clientSocket = serverSockets.accept();
				clientSocket.setSoTimeout(3000);
				System.out.println("Recieved connection from " + clientSocket.getInetAddress() + " on port "
						+ clientSocket.getPort());
				if (!SocketList.contains(clientSocket))
						SocketList.add(clientSocket);
				
				for (Socket socket : SocketList) {
					ReceiveFromClientThread receive_from_client = new ReceiveFromClientThread(socket);
					// //Thread receive_thread = new Thread(receive_from_client);
					receive_from_client.start();
				}
				
				// }

				// send_client.run(clientSocket);

			} catch (Exception e) {
				// TODO: handle exception
				System.out.println("closed socket from server\n");
			}
			// for (Socket socket : SocketList) {

			// clientSocket.close();
		}
	}
}

class ReceiveFromClientThread extends Thread {
	Socket clientSocket = null;
	BufferedReader brBufferedReader = null;
	StringBuilder mess_build = null;
	public ReceiveFromClientThread(Socket clientSocket) {
		this.clientSocket = clientSocket;
	}// end constructor

	public void run() {
		try {
			brBufferedReader = new BufferedReader(new InputStreamReader(this.clientSocket.getInputStream()));
			int length = 0;
											
			while (true) {
				String request = brBufferedReader.readLine();
				if (request == null || request.length() == 0) {
					mess_build = new StringBuilder();
					for (int i = 0; i < length; i++) {
						mess_build.append((char) brBufferedReader.read());
					}
					if (!mess_build.toString().trim().equals(""))
						
					System.out.println("MESS: " + mess_build.toString());
					if (mess_build.toString().length() > 1 && Server.chat_length > 0){
						for (Socket socket : Server.SocketList) {
							SendMessToClient(socket, mess_build.toString());
						}
						Server.chat_length = mess_build.toString().length();
						System.out.println("chat length : " + Server.chat_length);
					}
					break;
				} else if (request.startsWith("GET /")) {

					String path = request.substring(4, request.indexOf("HTTP"));
					System.out.println("get section: " + path + "#");
					if (path.trim().equals("/")) {
						sendFileIndex(clientSocket);
					}
					else if (path.trim().equals("/chat?")){
						System.out.println("chat length : " + Server.chat_length);
						if (mess_build.toString().length() > 1 && Server.chat_length > 0){
							for (Socket socket : Server.SocketList) {
								SendMessToClient(socket, mess_build.toString());
							}
							Server.chat_length = mess_build.toString().length();
							System.out.println("chat length : " + Server.chat_length);
						}
					}

				} else if (request.startsWith("POST /")) {
					System.out.println("post section : " + request + "@");

				} else if (request.startsWith("Content-Length:")) {
					length = Integer.parseInt(request.substring(15, request.length()).trim());
					System.out.println("content-length : " + length);
//				} else if (request.startsWith("{") && request.endsWith("}")) {
//					System.out.println("mess section :" + request);
//					String mess = request.substring(1, request.indexOf("}"));
////					for (Socket socket : Server.SocketList) {
////						SendMessToClient(socket, mess);
////					}
					
				}

				System.out.println("From Client: " + request);// print
			}

		}

		catch (Exception ex) {
			Server.SocketList.remove(clientSocket);
			System.out.println("Closed socket from client: " +ex.getMessage());
		}
		finally {
			//clientSocket.shutdownInput();
			//clientSocket.shutdownOutput();
			//clientSocket.close();
			Server.SocketList.remove(clientSocket);
		}
	}

	public void sendFileIndex(Socket socket) {
		try {
			// SendFileIndex();
			String path = "index/index.html";
			InputStream file_in = new FileInputStream(path);
			OutputStream file_out = socket.getOutputStream();
			PrintStream print_file_out = new PrintStream(file_out);
			print_file_out.print("HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n" + "Date: " + new Date() + "\r\n\r\n");
			FileTransfert(file_in, file_out);
			print_file_out.flush();
			file_out.flush();
			//file_out.close();
			//Server.SocketList.remove(socket);
			socket.shutdownOutput();
			//file_in.close();
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("Please enter something to send back to client..");

		}
	}

	public void SendMessToClient(Socket socket, String mess) {

		try {
			String username="";
			String message="";
			String mess_cut[] = mess.substring(1, mess.indexOf("}")).split(",");
			for (String string : mess_cut) {
				System.out.println("aloalo: "+string);
			}
			username = mess_cut[0].split(":")[1];
			if(mess_cut.length < 2){
			message = mess_cut[1].split(":")[1];
			}
			else {
				String join = "";
				for (int i = 1; i < mess_cut.length;i++){
					if (i < mess_cut.length-1)
						join += mess_cut[i] + ",";
					else 
						join += mess_cut[i];
				}
				message = join.split(":")[1];
			}
			Date date = new Date();
			String to_date = date.getHours() + ":" + date.getMinutes();
			JSONObject mess_chat = new JSONObject();
			mess_chat.put("username", username);
			mess_chat.put("mess", message);
			mess_chat.put("date", to_date);
			
			//JSONArray array = new JSONArray();
			//array.put(mess_chat);
			
			Server.LogChat.append("chat", mess_chat);
			System.out.println("message is: " + Server.SocketList.size() + "\n" +Server.LogChat);
			OutputStream mess_out = socket.getOutputStream();
			PrintStream print_mess_out = new PrintStream(mess_out);
			print_mess_out.print("HTTP/1.1 200 OK\r\n" + "Content-Type: text/plain\r\n" + "Date: " + new Date() + "\r\n\r\n");
			print_mess_out.print(Server.LogChat);
			print_mess_out.flush();
			mess_out.flush();
			socket.shutdownOutput();
			//socket.close();
			//Server.SocketList.remove(socket);
		} catch (Exception e) {
			// TODO: handle exception
			System.out.println("closed socket from send data\n");
			Server.SocketList.remove(socket);
		}

	}

	public void FileTransfert(InputStream file_in, OutputStream file_out) {
		byte[] buf = new byte[99999];
		int line = 0;
		try {
			while ((line = file_in.read(buf)) != -1) {
				file_out.write(buf, 0, line);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("closed socket from send index\n");
		}
	}
}
