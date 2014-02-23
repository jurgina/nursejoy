import java.net.*;
import java.io.*;
import javax.net.ssl.*;
import javax.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.cert.*;

/*
 * This example shows how to set up a key manager to perform client
 * authentication.
 *
 * This program assumes that the client is not inside a firewall.
 * The application can be modified to connect to a server outside
 * the firewall by following SSLSocketClientWithTunneling.java.
 */
public class client {
	// host = args[0]
	// portnr = args[1]
	// client's keystorepassword = args[2]
	// client's truststorepassword = args[3]
	// client's certificatepassword = args[4]
    public static void main(String[] args) throws Exception {
        String host = null;
        int port = -1;
        Console c = System.console();       
        
        //String clientname="";
        
        if (args.length < 2) {
            System.out.println("USAGE: java client host port");
            System.exit(-1);
        }        
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = " + args[i]);
        }        
        try { /* get input parameters */
            host = args[0];
            port = Integer.parseInt(args[1]);  
        } catch (IllegalArgumentException e) {
            System.out.println("USAGE: java client host port");
            System.exit(-1);
        }
        
        try { /* set up a key manager for client authentication */
            SSLSocketFactory factory = null;
            boolean clientConnected = false;
            while(!clientConnected){
            	try {
                	char[] clientkeystorepassword = c.readPassword("Keystore password: ");
                    char[] clienttruststorepassword =c.readPassword("Trusstore password: ");
                    char[] clientcertpassword = c.readPassword("Certificate password: ");
                    
                    KeyStore ks = KeyStore.getInstance("JKS");
                    KeyStore ts = KeyStore.getInstance("JKS");
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
                    SSLContext ctx = SSLContext.getInstance("TLS");
                    ks.load(new FileInputStream("clientkeystore"), clientkeystorepassword);  // keystore password (storepass)
    				ts.load(new FileInputStream("clienttruststore"), clienttruststorepassword); // truststore password (storepass);
    				kmf.init(ks, clientcertpassword); // user password (keypass)
    				tmf.init(ts); // keystore can be used as truststore here
    				ctx.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                    factory = ctx.getSocketFactory();
                    clientConnected = true;
                } catch (Exception e) {
                	clientConnected = false;
                    System.out.println(e.getMessage());
                }
            }           
            
            SSLSocket socket = (SSLSocket)factory.createSocket(host, port);
            //System.out.println("\nsocket before handshake:\n" + socket + "\n");
            
            String[] ss = socket.getSupportedCipherSuites();
            for(String s : ss){
            	System.out.println("Cipher : " + s);
            }
            /*
             * send http request
             *
             * See SSLSocketClient.java for more information about why
             * there is a forced handshake here when using PrintWriters.
             */
            socket.startHandshake();

            SSLSession session = socket.getSession();
            X509Certificate cert = (X509Certificate)session.getPeerCertificateChain()[0];
            String subject = cert.getSubjectDN().getName();
            //System.out.println("certificate name (subject DN field) on certificate received from server:\n" + subject + "\n");
            //System.out.println("socket after handshake:\n" + socket + "\n");
            System.out.println("secure connection established\n\n");
            
            	/*message stuff*/
            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            
			for (;;) {
				boolean validAction = false;
				String action = "";
				while(!validAction){
					System.out.print("Operation (r/w/c/d/q): "); // r=read, w=write, c=create, d=delete, q=quit
					action = read.readLine(); 
					if(action.equals("r") || action.equals("d") || action.equals("w") || action.equals("q") || action.equals("c")){
						validAction = true;
					}
				}
				
				StringBuilder builder = new StringBuilder();
				builder.append(action + " ");
				if (action.equals("q")) {
					break;
				}
				boolean valid = false;
	            boolean validdash = false;
	            String personnbr = "";
				while(!valid || !validdash){
					valid = true;
					validdash = true;
					System.out.print("Patient's personnummer (yymmdd-xxxx): ");  
	                personnbr = read.readLine();	               
	                if(personnbr.length() == 11){
	                	for(int i = 0; i < 11; i++){
	                		if(i == 6){
	                			if(personnbr.charAt(i) != '-'){		                			
		                			validdash = false;	 
	                			}
	                		}else if(!Character.isDigit(personnbr.charAt(i))){	                			
	            				valid = false;
	            			}                		
	                	}
	                }else{
	                	valid = false;
	                }
	                if(!valid || !validdash) System.out.println("Invalid personumber format (yymmdd-xxxx). Please reenter personnumber. ");
				}
                builder.append(personnbr + " ");
                String date = "";
                valid = false;
                while(!valid){
                	valid = true;
                	System.out.print("Admittance date (yyyymmdd): ");
                	date = read.readLine();
                	if(date.length() == 8){
                		for(int i = 0; i < 8; i++){
                			if(!Character.isDigit(date.charAt(i))){
	            				valid = false;
	            			}
                		}
                	}else{
                		valid = false;
                	}
                	if(!valid) System.out.println("Invalid date format (yyyymmdd). Please reenter date. ");                	
                }                
                builder.append(date);
                msg = builder.toString();                
                out.println(msg);
                out.flush();                
                String serveranswer = in.readLine();
                String acc = serveranswer.equals("1") ? "granted" : "denied";
                System.out.println("Received access " + acc + " from server");                
                
                if(serveranswer.equals("1")){ // om servern svara ja
                	switch(action.charAt(0)){
                	
                	case 'r':
                		System.out.println("Record: "); //ta emot stuff 
                		String record = in.readLine();
                		while(!record.equals("EOF")){
                			System.out.println(record);
                			record = in.readLine();
                		}                	               	
                	break;                	
                	case 'w': System.out.println("Enter new information: "); 
	                	msg = read.readLine(); 	                	
	                    out.println(msg);
	                    out.flush();
	                    System.out.println("Information added");                	
	                	break;                	
                	case 'c': 
	                	StringBuilder sb = new StringBuilder();
	                	System.out.println("Enter patient's name: ");
	                	sb.append(read.readLine());
	                	sb.append("\n");                	
	                	System.out.println("Enter the attending nurse's name: ");
	                	sb.append(read.readLine());
	                	sb.append("\n");	                	
	                	System.out.println("Enter patients medical information: ");
	                	sb.append(read.readLine());
	                	sb.append("\n");
	                	msg = sb.toString();	                	
	                    out.println(msg);
	                    out.flush();
	                    String servmsg = in.readLine();
	                    System.out.println(servmsg);  
	                	break;
                	
                	case 'd': System.out.println("The record has been deleted."); break;
                	}
                } 
            }

            in.close();
			out.close();
			read.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
