import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class IMAPApp {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket) sslSocketFactory.createSocket("imap.gmail.com", 993);
			
			BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			PrintWriter output = new PrintWriter(socket.getOutputStream());
			
			String response = input.readLine();
			
			System.out.println(response);
			 
			socket.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
