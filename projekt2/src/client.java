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
        char[] clientkeystorepassword = null;
        char[] clienttruststorepassword = null;
        char[] clientcertpassword = null;
        //String clientname="";
        for (int i = 0; i < args.length; i++) {
            System.out.println("args[" + i + "] = " + args[i]);
        }
        if (args.length < 5) {
            System.out.println("USAGE: java client host port clientkeystorepw clienttruststorepw clientcertpassword");
            System.exit(-1);
        }
        try { /* get input parameters */
            host = args[0];
            port = Integer.parseInt(args[1]);
            clientkeystorepassword = args[2].toCharArray();
            clienttruststorepassword = args[3].toCharArray();
            clientcertpassword = args[4].toCharArray();
           // clientname = args[5];
            
            
        } catch (IllegalArgumentException e) {
            System.out.println("USAGE: java client host port clientkeystorepw clienttruststorepw clientcertpassword");
            System.exit(-1);
        }

        try { /* set up a key manager for client authentication */
            SSLSocketFactory factory = null;
            try {
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
            } catch (Exception e) {
                throw new IOException(e.getMessage());
            }
            SSLSocket socket = (SSLSocket)factory.createSocket(host, port);
            System.out.println("\nsocket before handshake:\n" + socket + "\n");

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
            System.out.println("certificate name (subject DN field) on certificate received from server:\n" + subject + "\n");
            System.out.println("socket after handshake:\n" + socket + "\n");
            System.out.println("secure connection established\n\n");
            
            	/*message stuff*/
            BufferedReader read = new BufferedReader(new InputStreamReader(System.in));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String msg;
            
			for (;;) {
				System.out.print("Operation (r/w/c/d/q): "); // r=read, w=write, c=create, d=delete, q=quit
				String action = read.readLine(); 
				StringBuilder builder = new StringBuilder();
				builder.append(action + " ");
				if (action.equals("q")) {
					break;
				}
                System.out.print("Patient's personnummer: ");                
                builder.append(read.readLine() + " ");
                System.out.println("Admittance date (yyyymmdd): ");
                builder.append(read.readLine());
                msg = builder.toString();
                System.out.print("sending '" + msg + "' to server...");
                out.println(msg);
                out.flush();
                System.out.println("done");
                String serveranswer = in.readLine();
                System.out.println("received '" + serveranswer + "' from server\n");
                
                if(serveranswer.equals("1")){ // om servern svara ja
                	switch(action.charAt(0)){
                	
                	case 'r': System.out.println("Here is the record! \n" + in.readLine() ); //ta emot stuff                	
                	break; 
                	
                	case 'w': System.out.println("Write a story"); 
                	msg = read.readLine(); 
                	System.out.print("sending '" + msg + "' to server...");
                    out.println(msg);
                    out.flush();
                    System.out.println("done");                	
                	break;
                	
                	case 'c': 
                	StringBuilder sb = new StringBuilder();
                	System.out.println("Patient's name: ");
                	sb.append(read.readLine());
                	sb.append("\n");
                	System.out.println("Personnummer: ");
                	sb.append(read.readLine());
                	sb.append("\n");
                	System.out.println("Addmittance date: "); //ev nån annanstans
                	sb.append(read.readLine());
                	sb.append("\n");
                	System.out.println("Nurse: ");
                	sb.append(read.readLine());
                	sb.append("\n");
                	//om johan inte fixat his shit så behöver vi lägga in division oxå
                	System.out.println("Illness: ");
                	sb.append(read.readLine());
                	sb.append("\n");
                	msg = sb.toString();
                	System.out.print("sending '" + msg + "' to server...");
                    out.println(msg);
                    out.flush();
                    System.out.println("done");  
                	break;
                	
                	case 'd': System.out.println("The record has been murdered."); break;
                	}
                } else{
                	System.out.println("You don't have the AUTHORITHAI to do that action.");
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
