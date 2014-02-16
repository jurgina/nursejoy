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
            String[] type = strings[1].split(" ");
            
            /*******************************LÄGG TILL ATT HÄMTA DIVISION!!!! JAAA*************************************************/
            int currType = 0;
            String name = "";
            String currTypeStr = type[0];
            for(int i = 1; i < type.length; i++){
            	name += type[i] + " ";
            }
            name = name.trim();
            switch(type[0]){
	            case "Dr" : 
	            	currType = Doctor;
	            	break;
	            case "Nurse" : 
	            	currType = Nurse;
	            	break;
	            case "Mr" : 
	            	currType = Government;
	            	break;
	            case "Patient" : 
	            	currType = Patient;
	            	break;            
            }
            System.out.println("Type = " + currType + " name = " + name);
            
            
            PrintWriter out = null;
            BufferedReader in = null;
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String clientMsg = null;
            while ((clientMsg = in.readLine()) != null) {
            	String[] message = clientMsg.split(" ");
            	if(hasAccess(message[1], message[2], message[0], currType, name, currTypeStr)){            		
            		System.out.println("YES YOU CAN");
            		out.println(1);            		
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
                System.out.println("done\n");
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

    private boolean hasAccess(String personnr, String date, String action, int currType, String name, String currTypeStr) {
    	if(action.equals("c") && currType == Doctor){
			  return true;
		 }else if(action.equals("d") && currType == Government){
			  return true;
		 }else if(action.equals("r") && currType == Government){
			 return true;
		 }
    	
    	
    	BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader("Patient Files/" + personnr + "/" + date));
		} catch (FileNotFoundException e) {
			return false;
		}
    	try {
    	   StringBuilder sb = new StringBuilder();
    	   String line = br.readLine();
    	   if(line != null){
    		  String[] accessPeople = line.split(",");    		  
    		  for(String s : accessPeople){    			  
    			  if(s.startsWith(currTypeStr) && s.contains(name)){
    				  if(action.equals("r")){
    					  return true;
    				  }else if(action.equals("w") && currType != Patient && currType != Government){
    					  return true;    					  
    				  }
    			  }   			  
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