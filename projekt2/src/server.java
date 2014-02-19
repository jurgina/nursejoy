import java.io.*;
import java.net.*;
import java.security.KeyStore;

import javax.net.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;

public class server implements Runnable {
    private ServerSocket serverSocket = null;
    private static int numConnectedClients = 0;
    private static int Doctor = 0, Patient = 1, Nurse = 2, Government = 3;
    
    public server(ServerSocket ss) throws IOException {
        serverSocket = ss;
        newListener();
    }

    public void run() {
        try {
            SSLSocket socket=(SSLSocket)serverSocket.accept();
            newListener();            
            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate)session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
    	    numConnectedClients++;
            System.out.println("client connected");
            System.out.println("client name (cert subject DN field): " + subject); //"CN=XXXXXX"
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
            String[] strings = subject.split("=");
            String[] type = strings[1].split(":");
            String division ="";
          
            int currType = 0;
            String name = type[1];
            String currTypeStr = type[0];
            
            
            switch(type[0]){
	            case "Dr" : 
	            	currType = Doctor;
	            	division = type[2];
	            	break;
	            case "Nurse" : 
	            	currType = Nurse;
	            	division = type[2];
	            	break;
	            case "Mr" : 
	            	currType = Government;
	            	break;
	            case "Patient" : 
	            	currType = Patient;
	            	break;            
            }
            System.out.println("Type = " + currType + " name = " + name);
            
            PrintWriter writer;
            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String clientMsg = null;
            while ((clientMsg = in.readLine()) != null) {
            	System.out.println("Start : ");
            	String[] message = clientMsg.split(" ");
            	if(hasAccess(message[1], message[2], message[0], currType, name, currTypeStr, division)){            		
            		System.out.println("YES YOU CAN");
            		out.println(1);
            		switch(message[0]){
	            		case "r": 
	            			System.out.println("Read:");
	            			String record = getRecord(message[1], message[2]);
	            			System.out.println(record);
	                		out.println(record);
	                		out.println("EOF");
	            			break;
	            		case "c": 
	            			String patient = in.readLine();
	            			/*String personnr = in.readLine();//nej
	            			String admindate = in.readLine();//nej*/
	            			String nurse = in.readLine();
	            			String illness = in.readLine();
	            			in.readLine();	            			
	            			
	            			File f = new File("Patient Files/" + message[1] + "/" + message[2]);
	            			if(f.exists()){
	            				out.println("Record already exist. Try writing to the file instead. ");
	            			}else{
	            				writer = new PrintWriter("Patient Files/" + message[1] + "/" + message[2], "UTF-8");
		            			writer.println(patient);
		            			writer.println("Patient " + message[1]+", Nurse " +nurse + ", Dr " + name);
		            			writer.println(division);
		            			writer.println("Illness:");
		            			writer.println(illness);
		            			writer.close();
		            			out.println("Record created!");
	            			}
	            			break;
	            		case "w": 
	            			String msg = in.readLine();
	            			System.out.println("Message from client : " + msg);
	            			//writer = new PrintWriter("Patient Files/" + message[1] + "/" + message[2], "UTF-8");
	            			try(PrintWriter wr = new PrintWriter(new BufferedWriter(new FileWriter("Patient Files/" + message[1] + "/" + message[2], true)))) {
	            			    wr.println(msg);
	            			}catch (IOException e) {
	            				System.out.println("NOOO");
	            			}
	            			break;
	            		case "d":
	            			File fil = new File("Patient Files/" + message[1] + "/" + message[2]);
	            			fil.delete();
	            			break;
            		}
            	}else{
            		out.println(0);
            	}
            	out.flush();
            	
            	/*
			    String rev = new StringBuilder(clientMsg).reverse().toString();
                System.out.println("received '" + clientMsg + "' from client");
                System.out.print("sending '" + rev + "' to client...");
				out.println(rev);
				out.flush();*/
                System.out.println("done");
			}
			in.close();
			
			out.close();
			socket.close();
    	    numConnectedClients--;
            System.out.println("client disconnected");
            System.out.println(numConnectedClients + " concurrent connection(s)\n");
		} catch (IOException e) {
            System.out.println("Client died: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }

    private String getRecord(String personnr, String date){
    	BufferedReader br = null;
    	String res = "";
		try {
			br = new BufferedReader(new FileReader("Patient Files/" + personnr + "/" + date));
		} catch (FileNotFoundException e) {	
			return "No file found";
		}
    	try {
    	   StringBuilder sb = new StringBuilder();
    	   String line = br.readLine();
    	   while(line != null){
    		   System.out.println("Line = " + line);
    		   sb.append(line + "\n");
    		   line = br.readLine();
    	   }
    	   res = sb.toString();
    	}catch(Exception e){
    		return "Got exception";
    	}
    	return res;
    }
    
    private boolean hasAccess(String personnr, String date, String action, int currType, String name, String currTypeStr, String division) {
    	System.out.println(personnr + date + action + currType + "----" + name +"----" +currTypeStr);
    	
    	if(action.equals("c") && currType == Doctor){
    		if(new File("Patient Files/" + personnr + "/" + date).exists()){
    			return false;
    		}    		
			  return true;
		 }else if(action.equals("d") && currType == Government){
			  return true;
		 }else if(action.equals("r") && currType == Government){
			 return true;
		 }
    	System.out.println("Nope");
    	
    	BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("Patient Files/" + personnr + "/" + date));
		} catch (FileNotFoundException e) {
			return false;
		}
    	try {
    	   StringBuilder sb = new StringBuilder();
    	   String line = br.readLine();
    	   line = br.readLine();
    	   System.out.println(line);
    	   if(line != null){
    		  String[] accessPeople = line.split(",");
    		  for(String s : accessPeople){   
    			  System.out.println(s);
    			  s = s.trim();//trimma den jäveln!
    			  if(s.startsWith(currTypeStr) && s.contains(name)){
    				  System.out.println("all correct");
    				  if(action.equals("r")){
    					  System.out.println("Wants to read");
    					  return true;
    				  }else if(action.equals("w") && currType != Patient && currType != Government){
    					  return true;    					  
    				  }
    			  }
    		  }
    		  line = br.readLine().trim();
    		  if(line.equals(division) && action.equals("r")){
    			  return true;
    		  }    		  
    	   }else{
    		   return false;
    	   }
    	
    	 }catch(Exception e){
    	    	
    	 }finally {
    	    try {
				br.close();
			} catch (IOException e) {
				return false;
			}
    	  }
    	return false;
    }
    
    private void newListener() { (new Thread(this)).start(); } // calls run()

    public static void main(String args[]) {
        System.out.println("\nServer Started\n");
        int port = -1;
        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        String type = "TLS";
        try {
            ServerSocketFactory ssf = getServerSocketFactory(type);
            ServerSocket ss = ssf.createServerSocket(port);
            ((SSLServerSocket)ss).setNeedClientAuth(true); // enables client authentication
            new server(ss);
        } catch (IOException e) {
            System.out.println("Unable to start Server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static ServerSocketFactory getServerSocketFactory(String type) {
        if (type.equals("TLS")) {
            SSLServerSocketFactory ssf = null;
            try { // set up key manager to perform server authentication
                SSLContext ctx = SSLContext.getInstance("TLS");
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                KeyStore ks = KeyStore.getInstance("JKS");
				KeyStore ts = KeyStore.getInstance("JKS");
                char[] password = "password".toCharArray();

                ks.load(new FileInputStream("serverkeystore"), password);  // keystore password (storepass)
                ts.load(new FileInputStream("servertruststore"), password); // truststore password (storepass)
                kmf.init(ks, password); // certificate password (keypass)
                tmf.init(ts);  // possible to use keystore as truststore here
                ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                ssf = ctx.getServerSocketFactory();
                return ssf;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            return ServerSocketFactory.getDefault();
        }
        return null;
    }
}