/* ------------------------------------------------------------------------- */
/*   Copyright (C) 2017 
                Author:  sroig2013@my.fit.edu
                Florida Tech, Computer Science
   
       This program is free software; you can redistribute it and/or modify
       it under the terms of the GNU Affero General Public License as published by
       the Free Software Foundation; either the current version of the License, or
       (at your option) any later version.
   
      This program is distributed in the hope that it will be useful,
      but WITHOUT ANY WARRANTY; without even the implied warranty of
      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
      GNU General Public License for more details.
  
      You should have received a copy of the GNU Affero General Public License
      along with this program; if not, write to the Free Software
      Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.              */
/* ------------------------------------------------------------------------- */

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;


public class IMAPService {
	private final String mailboxRootDirectory = "Mailbox";
	private String server;
	private int port;
	
	private boolean deleteAfterDownload;
	
	private SSLSocket socket;
	private BufferedReader reader;
	private PrintWriter output;
	
	private ArrayList<EmailFolder> emailFolders;

	public IMAPService(String _server, int _port) {
		File mailboxRootDir = new File (mailboxRootDirectory);
		mailboxRootDir.mkdir();
		
		this.server = _server;
		this.port = _port;
		this.emailFolders = new ArrayList<EmailFolder>();
		this.deleteAfterDownload = false;
		// TODO Auto-generated constructor stub
		try {
			SSLSocketFactory sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
			this.socket = (SSLSocket) sslSocketFactory.createSocket(this.server, this.port);
			
			this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			this.output = new PrintWriter(socket.getOutputStream());			
			System.out.println(parseServerResponse());
			 			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String parseServerResponse() throws IOException {
		// Need a string to store full response and single line
		StringBuilder response = new StringBuilder();
		String line;
		
		while ((line = reader.readLine()) != null) {
			// Add the line to the overall response
			response.append(line + "\n");
			
			// If the reader isn't ready send back the full response
			if (!reader.ready()) {
				return response.toString();		
			}
		}
		
		return response.toString();		
	}
	
	public boolean login(String userName, String password) {
		try {
			// String for logging into the IMAP server
			String login = "a0 login " + userName + " " + password + "\r";
			// Print imap command to local console
			System.out.println(login);
			// Send imap command to the server
			output.println(login);
			output.flush();
			// Listen for server's response
			System.out.println(parseServerResponse());
			//We've made it this far so login was successful
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		// Login failed
		return false;
	}
	
	public void logout () {
		try {
			String logout = "a0 logout" + "\r";
			System.out.println(logout);
			output.println(logout);
			output.flush();
			System.out.println(parseServerResponse());
			reader.close();
			output.close();
			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void buildMailbox () {
		try {
			// Get a list of all the mailboxes
			String listAllFolders = "a0 list \"\" *" + "\r";
			System.out.println(listAllFolders);
			output.println(listAllFolders);
			output.flush();
			String folderList = parseServerResponse();
			
			while (!folderList.contains("a0 OK")) {
				folderList += parseServerResponse();
			}
			
			System.out.println(folderList);

			String[] folders = folderList.split("\n|\"\"");
			folders = Arrays.copyOf(folders, folders.length - 1);
			
			// fetch each email and create a new email and store it in the respective folder
			buildFolderList(folders);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void buildFolderList (String[] folderList) {		
		
		for (String folder : folderList) {
			
			try {
				// Parse the folder string
				String[] folderTokens = folder.split("\"");
				// Create a new folder
				EmailFolder emailFolder = new EmailFolder();
				// Set the name from server response
				emailFolder.name = folderTokens[folderTokens.length - 1];
				
				// Select the folder
				String selectFolder = "a0 select \"" + emailFolder.name + "\"\r";
				System.out.println(selectFolder);
				output.println(selectFolder);
				output.flush();
				
				String selectFolderResponse = parseServerResponse();
				
				while (!selectFolderResponse.contains("a0 OK") && !selectFolderResponse.contains("Unknown Mailbox")) {
					selectFolderResponse += parseServerResponse();
				}
				
				System.out.println(selectFolderResponse);
				
				if (!selectFolderResponse.contains("Unknown Mailbox")) {
					// Get all emails for this folder
					emailFolder.emails = buildEmailList();
				} else {
					emailFolder.emails = new ArrayList<Email>();
				}
				
				// Add all folders to IMAP Service list of folders
				emailFolders.add(emailFolder);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private ArrayList<Email> buildEmailList() {

		try {
			
			// Find all emails
			String fetchEmailList = "a0 fetch 1:* (FLAGS)" + "\r";
			System.out.println(fetchEmailList);
			output.println(fetchEmailList);
			output.flush();
			
			// Get list of emails in the current mailbox
			String emailList = parseServerResponse();
			
			while (!emailList.contains("a0 OK")) {
				emailList += parseServerResponse();
			}
			
			System.out.println(emailList);
			
			// Split up the email list by newlines
			String[] emailTokenIdList = emailList.split("\n");
			
			ArrayList<Integer> emailIds = new ArrayList<Integer>();
			
			for (int i = 0; i < emailTokenIdList.length; i++) {
				if (emailTokenIdList[i].split("\\s+").length > 1){
					if (emailTokenIdList[i].split("\\s+")[1].matches("[-+]?\\d*\\.?\\d+")) {
						if (Integer.parseInt(emailTokenIdList[i].split("\\s+")[1]) > 0)
							emailIds.add(Integer.parseInt(emailTokenIdList[i].split("\\s+")[1]));
					}
				}
			}
			
			ArrayList<Email> emails = new ArrayList<Email>();
			
			for (int emailID : emailIds) {

				// Instantiate email object and use fetch based on ID to grab it's data
				Email email = new Email();
				email.uid = emailID;
				
				// Fetch the email header by it's id
				String fetchEmailSender = "a0 fetch " + email.uid + " body[header]" + "\r";
				output.println(fetchEmailSender);
				output.flush();
				System.out.println(fetchEmailSender);
								
				String headerResponse = parseServerResponse();
				
				while (!headerResponse.contains("a0 OK")) {
					headerResponse += parseServerResponse();
				}
				
				System.out.println(headerResponse);

				String[] headerResponseTokens = headerResponse.split("\n");
				
				email.header = headerResponse;
				
				for (String headerItem : headerResponseTokens) {
					if (headerItem.contains("From")) {
						// Find who it's from
						if (headerItem.contains("<")) {
							String from = headerItem.substring(headerItem.indexOf("<") + 1);
							from = from.substring(0, from.indexOf(">"));
							email.from = from;
						} else {
							email.from = headerItem.split("\\s+")[1];
						}
					}
					if (headerItem.contains("Subject")) {
						// Find email subject
						if (headerItem.indexOf(":") < headerItem.length())
							email.subject = headerItem.substring(headerItem.indexOf(":") + 1, headerItem.length());
						else
							email.subject = "NO SUBJECT";
					}
				}
				
				// Fetch the email header by it's id
				String fetchEmailBody = "a0 fetch " + email.uid + " body[]" + "\r";
				output.println(fetchEmailBody);
				output.flush();
				System.out.println(fetchEmailBody);
				String fetchEmailBodyResponse = parseServerResponse();
			
				// Make sure the message is all there
				while (!fetchEmailBodyResponse.contains("a0 OK Success")) {
					fetchEmailBodyResponse += parseServerResponse();
				} 
				
				System.out.println(fetchEmailBodyResponse);				
				
				email.body = fetchEmailBodyResponse;
				
				// Now that the email is fully built tell it to parse the body elements
				email.parseBodyElements();
				
				emails.add(email);
			}
			
			return emails;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private void deleteFoldersEmails(EmailFolder folder) {
		try {
			// Get a list of all the mailboxes
			String selectFolder = "a0 select \"" + folder.name + "\"\r";
			System.out.println(selectFolder);
			output.println(selectFolder);
			output.flush();
			String selectFolderResponse = parseServerResponse();
			
			while (!selectFolderResponse.contains("a0 OK")) {
				selectFolderResponse += parseServerResponse();
			}
			
			System.out.println(selectFolderResponse);
			
			for (Email email : folder.emails) {
				// Set emails to deleted
				String deleteEmail = "a0 STORE" + email.uid + " +FLAGS \\Deleted\r";
				System.out.println(deleteEmail);
				output.println(deleteEmail);
				output.flush();
				String deleteEmailResponse = parseServerResponse();
				
				while (!deleteEmailResponse.contains("a0 OK")) {
					deleteEmailResponse += parseServerResponse();
				}
				
				System.out.println(deleteEmailResponse);
			}
			
			
			// Expunge deleted emails
			String expungeCommand = "a0 EXPUNGE\r";
			System.out.println(expungeCommand);
			output.println(expungeCommand);
			output.flush();
			String expungeCommandResponse = parseServerResponse();
			
			while (!expungeCommandResponse.contains("a0 OK")) {
				expungeCommandResponse += parseServerResponse();
			}
			
			System.out.println(expungeCommandResponse);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void downloadFoldersEmails(EmailFolder folder) {
		// Create the folder inside main mailbox
		File rootDir = new File (mailboxRootDirectory);
		File folderDir = null;
		String[] folderList = folder.name.split("/");
		
		for (String folderPiece : folderList) {
			folderDir =  new File (rootDir, folderPiece);
			folderDir.mkdir();
			rootDir = new File(folderDir.getPath());
		}
		
		// Create a file for the messages now
		if (folder.emails != null) {
			for (Email email : folder.emails) {
				if (email != null) {
					if (email.from == null)
						email.from = "null";
					if (email.subject == null)
						email.subject = "null";
					String emailDirName = email.fiveDigitId + "_" + email.from + "_" + email.subject;
					emailDirName = emailDirName.replaceAll("[^A-Za-z0-9]", "-");
					File emailDir = new File (folderDir, emailDirName);
					emailDir.mkdir();
					
					try {
						File emailContext = new File (emailDir, "context.txt");
						emailContext.createNewFile();
						Files.write(emailContext.toPath(), email.body.getBytes());
						
						for (BodyElement be : email.bodyElements) {
							File attachmentContext = new File (emailDir, be.fileName);
							attachmentContext.createNewFile();
							// Write base64 encoded information to the attachment
							Files.write(attachmentContext.toPath(), DatatypeConverter.parseBase64Binary(be.data));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		if (deleteAfterDownload) 
			deleteFoldersEmails(folder);
	}
	
	public void downloadFoldersEmails(String directoryName) {
		for (EmailFolder folder : emailFolders) {
			if (folder.name.equals(directoryName)) {
				downloadFoldersEmails(folder);
			}
		}
	}
	
	public void downloadAllFolders() {
		for (EmailFolder folder : emailFolders) {
			downloadFoldersEmails(folder);
		}
	}
	
	public void deleteEmailsAfterDownload() {
		this.deleteAfterDownload = true;
	}
	
	public static void main(String[] args) {
		// Arguments
		String server = "";
		int port = -1;
		String login = "";
		String password = "";
		boolean deleteAfterDownload = false;
		boolean downloadAll = false;
		String[] foldersToDownload = null;
		
		// Set up apache cli
		Options options = new Options();
		
		Option S = new Option("S", true, "Server Name");
		S.setRequired(true);
		options.addOption(S);
		
		Option P = new Option("P", true, "Port Number");
		P.setRequired(true);
		options.addOption(P);
		
		Option l = new Option("l", true, "Login");
		l.setRequired(true);
		options.addOption(l);

		Option p = new Option("p", true, "Password if not on stdin");
		options.addOption(p);

		Option d = new Option("d", false, "Delete after downloading");
		d.setRequired(false);
		options.addOption(d);
		
		Option a = new Option("a", false, "Download from all folders");
		a.setRequired(false);
		options.addOption(a);

		Option f = new Option("f", true, "Download messages from specified folder");
		options.addOption(f);

		CommandLineParser clp = new DefaultParser();
		
		try {
			CommandLine cl = clp.parse(options, args);
			
			if (cl.hasOption("S"))
				server = cl.getOptionValue("S");
			
			if (cl.hasOption("P"))
				port = Integer.parseInt(cl.getOptionValue("P"));
			
			if (cl.hasOption("l"))
				login = cl.getOptionValue("l");
			
			if (cl.hasOption("p"))
				password = cl.getOptionValue("p");
			
			if (cl.hasOption("d"))
				deleteAfterDownload = true;
			
			if (cl.hasOption("a"))
				downloadAll = true;
			else if (cl.hasOption("f"))
				foldersToDownload = cl.getOptionValues("f");
			else {
				showArgMenu(options);
				return;
			}
			
		} catch (Exception e){
			showArgMenu(options);
			return;
		}
		
		if (password.isEmpty()) {
			// Grab p/w of stdin if it's there
			Scanner sc = new Scanner(System.in);
			
			password = sc.nextLine();
			
			sc.close();
		}		
		
		if (!server.isEmpty() && port != -1 && !login.isEmpty() && !password.isEmpty() && (downloadAll || foldersToDownload != null)) {
			//IMAPService imapService = new IMAPService("imap.gmail.com", 993);
			IMAPService imapService = new IMAPService(server, port);
			//imapService.login("kinglibingli@gmail.com", "kingli1bingli");
			imapService.login(login, password);
			imapService.buildMailbox();
			// Check if service should delete emails
			if (deleteAfterDownload)
				imapService.deleteEmailsAfterDownload();
			// Proceed with downloads
			if (downloadAll)
				imapService.downloadAllFolders();
			else {
				for (String folderNames : foldersToDownload) {
					imapService.downloadFoldersEmails(folderNames);	
				}
			}
			
			imapService.logout();
		}
	
	}
	
    private static void showArgMenu (Options options) {
    	HelpFormatter helpFormatter = new HelpFormatter();
    	helpFormatter.printHelp("Gossip P2P Server", options, true);
    }

}
