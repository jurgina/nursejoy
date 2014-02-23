import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

public class server implements Runnable {
	private ServerSocket serverSocket = null;
	private static int numConnectedClients = 0;
	private static int Doctor = 0, Patient = 1, Nurse = 2, Government = 3;
	private static String logFile = "log.txt";
	private static String logFolder = "Logs";
	
	
	public server(ServerSocket ss) throws IOException {
		serverSocket = ss;
		newListener();
	}

	public void run() {
		try {
			SSLSocket socket = (SSLSocket) serverSocket.accept();	
			newListener();
			String who = socket.getInetAddress().toString();
			writetolog("Client attempts to connect to server", who);			
			SSLSession session = socket.getSession();
			System.out.println("Cipher : " +session.getCipherSuite());
			X509Certificate cert = (X509Certificate) session
					.getPeerCertificateChain()[0];
			String subject = cert.getSubjectDN().getName();
			System.out.println("Protocol used : " + session.getProtocol());
			
			numConnectedClients++;
			System.out.println("client connected");
			/*System.out.println("client name (cert subject DN field): "
					+ subject); // "CN=XXXXXX"*/
			System.out.println(numConnectedClients
					+ " concurrent connection(s)\n");
			String[] strings = subject.split("=");
			String[] type = strings[1].split(":");
			String division = "";
			
			int currType = 0;
			String name = type[1];
			String currTypeStr = type[0];
			
			switch (type[0]) {
			case "Dr":
				currType = Doctor;
				division = type[2];
				break;
			case "Nurse":
				currType = Nurse;
				division = type[2];
				break;
			case "Mr":
				currType = Government;
				break;
			case "Patient":
				currType = Patient;
				break;
			}
			who = type[0] + " " + name + " " + type[2] + " , " + socket.getInetAddress().toString();
			//System.out.println("Type = " + currType + " name = " + name);
			writetolog("Secure connection established", who);
			PrintWriter writer;
			PrintWriter out = null;
			BufferedReader in = null;
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			String clientMsg = null;
			while ((clientMsg = in.readLine()) != null) {
				String[] message = clientMsg.split(" ");
				writetolog("Request from client : " + message[0] + " File " + message[1]+"/"+message[2], who);
				if (hasAccess(message[1], message[2], message[0], currType,
						name, currTypeStr, division)) {
					out.println(1);
					writetolog("Access to file " + message[1]+"/"+message[2] + " and operation " + message[0] + " granted", who);
					System.out.println("Client access approved");
					switch (message[0]) {
					case "r":
						String record = getRecord(message[1], message[2]);
						out.println(record);
						out.println("EOF");
						System.out.println("Record sent to client");
						writetolog("File " + message[1]+"/"+message[2] + " sent to client", who);
						break;
					case "c":
						String patient = in.readLine();
						String nurse = in.readLine();
						String illness = in.readLine();
						in.readLine();
						File f = new File("Patient Files/" + message[1]);
						if (!f.exists()) {
							f.mkdir();
						}
						f = new File("Patient Files/" + message[1] + "/"
								+ message[2]);
						f.createNewFile();
						writer = new PrintWriter("Patient Files/" + message[1]
								+ "/" + message[2], "UTF-8");
						writer.println(patient);
						writer.println("Patient " + message[1] + ", Nurse "
								+ nurse + ", Dr " + name);
						writer.println(division);
						writer.println("Illness:");
						writer.println(illness);
						writer.close();
						out.println("Record created!");
						writetolog("File " + message[1]+"/"+message[2] + " created", who);
						System.out.println("Record created");
						break;
					case "w":
						String msg = in.readLine();
						try (PrintWriter wr = new PrintWriter(
								new BufferedWriter(new FileWriter(
										"Patient Files/" + message[1] + "/"
												+ message[2], true)))) {
							wr.println(msg);
							wr.flush();
						} catch (IOException e) {
						}
						writetolog("Information added to file " + message[1]+"/"+message[2], who);
						System.out.println("Information added");
						break;
					case "d":
						File fil = new File("Patient Files/" + message[1] + "/"
								+ message[2]);
						fil.delete();
						writetolog("File " + message[1]+"/"+message[2] + " deleted", who);
						System.out.println("File deleted");
						break;
					}
				} else {
					out.println(0);
					writetolog("Access to file " + message[1]+"/"+message[2] + " and operation " + message[0] + " denied", who);
					System.out.println("Client access denied");
				}
				out.flush();
			}
			in.close();
			out.close();
			socket.close();
			numConnectedClients--;
			writetolog("Client disconnected", who);
			System.out.println("client disconnected");
			System.out.println(numConnectedClients
					+ " concurrent connection(s)\n");
		} catch (IOException e) {
			System.out.println("Client died: " + e.getMessage());
			writetolog("Client died", e.getMessage());
			e.printStackTrace();
			return;
		}
	}

	private synchronized void writetolog(String logMessage, String who){
		long time = System.currentTimeMillis();
		try{
			//check if Logs folder exists
			File f = new File(logFolder);
			if (!f.exists()) {
				f.mkdir(); //make dir if not exist
			}
			f = new File(logFolder+"/"+logFile);
			if(!f.exists()){
				f.createNewFile(); //create file if not exist
			}
			PrintWriter wr = new PrintWriter(new BufferedWriter(new FileWriter(logFolder+"/"+logFile, true)));			
			
	        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd HH:mm");	        
	        Date resultdate = new Date(time);	        
			wr.println(sdf.format(resultdate).toString() + " , " + who + " , " + logMessage);
			wr.flush();
			System.out.println("Log entry made");
		}catch(Exception e){
			System.out.println("Unnable to write to log. Server terminate for securty reasons");
			System.exit(0);
		}
	}
	
	private synchronized String getRecord(String personnr, String date) {
		BufferedReader br = null;
		String res = "";
		try {
			br = new BufferedReader(new FileReader("Patient Files/" + personnr
					+ "/" + date));
		} catch (FileNotFoundException
				e) {
			return "No file found";
		}
		try {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			while (line != null) {
				sb.append(line + "\n");
				line = br.readLine();
			}
			res = sb.toString();
		} catch (Exception e) {
			return "Got exception";
		}finally{
			try {
				br.close();
			} catch (IOException e) {
				
			}
		}
		return res;
	}

	private boolean hasAccess(String personnr, String date, String action,
			int currType, String name, String currTypeStr, String division) {
		if (action.equals("c") && currType == Doctor) {
			if (new File("Patient Files/" + personnr + "/" + date).exists()) {
				return false;
			}
			return true;
		} else if (action.equals("d") && currType == Government) {
			return true;
		} else if (action.equals("r") && currType == Government) {
			return true;
		}
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("Patient Files/" + personnr
					+ "/" + date));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();
			line = br.readLine();
			if (line != null) {
				String[] accessPeople = line.split(",");
				for (String s : accessPeople) {
					s = s.trim();
					if (s.startsWith(currTypeStr) && s.contains(name)) {
						if (action.equals("r")) {
							return true;
						} else if (action.equals("w") && currType != Patient
								&& currType != Government) {
							return true;
						}
					}
				}
				line = br.readLine().trim();
				if (line.equals(division) && action.equals("r")) {
					return true;
				}
			} else {
				return false;
			}
		} catch (Exception e) {
			return false;
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}

	private void newListener() {
		(new Thread(this)).start();
	}

	public static void main(String args[]) {
		
		int port = -1;
		if (args.length >= 1) {
			port = Integer.parseInt(args[0]);
		}
		String type = "TLS";
		Console c = System.console();
		boolean serverStarted = false;
		while(!serverStarted){
			try {
				char[] password = c.readPassword("Password: ");
				
				ServerSocketFactory ssf = getServerSocketFactory(type, password);
				ServerSocket ss = ssf.createServerSocket(port);
				((SSLServerSocket) ss).setNeedClientAuth(true); // enables client
														// authentication
				String[] protocols = new String[1];
				protocols[0] = "TLSv1";
				
				((SSLServerSocket) ss).setEnabledProtocols(protocols);
				String[] ans = ((SSLServerSocket) ss).getEnabledProtocols();
				for(String s : ans){
					System.out.println(s);
				}
				new server(ss);
				serverStarted = true;
			} catch (Exception e) {
				System.out.println("Unable to start Server: " + e.getMessage());				
				serverStarted = false;
			}
		}		
		System.out.println("\nServer Started\n");
	}

	private static ServerSocketFactory getServerSocketFactory(String type, char[] passwd) throws Exception {
		if (type.equals("TLS")) {
			SSLServerSocketFactory ssf = null;
			 // set up key manager to perform server authentication
				SSLContext ctx = SSLContext.getInstance("TLS");
				KeyManagerFactory kmf = KeyManagerFactory
						.getInstance("SunX509");
				TrustManagerFactory tmf = TrustManagerFactory
						.getInstance("SunX509");
				KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
				//char[] password = "password".toCharArray();
				
				ks.load(new FileInputStream("serverkeystore"), passwd); // keystore
																			// password
																			// (storepass)
				ts.load(new FileInputStream("servertruststore"), passwd); // truststore
																			// password
																			// (storepass)
				kmf.init(ks, passwd); // certificate password (keypass)
				tmf.init(ts); // possible to use keystore as truststore here
				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
				ssf = ctx.getServerSocketFactory();
				return ssf;			
		} else {
			return ServerSocketFactory.getDefault();
		}		
	}
}