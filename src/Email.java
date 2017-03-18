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

import java.util.ArrayList;
import java.util.Arrays;

public class Email {
	
	int uid;
	String fiveDigitId;
	String header;
	String from;
	String subject;
	String body;
	ArrayList<BodyElement> bodyElements;

	public Email() {
		// TODO Auto-generated constructor stub
		this.fiveDigitId = String.format("%05d", Counter.getNextID());
		this.bodyElements = new ArrayList<BodyElement>();
	}
	
	public void parseBodyElements () {
		if (this.body.isEmpty())
			return;
		
		// Look for the boundary id
		if (this.body.contains("boundary=")) {
			// Parse out the boundary id
			String boundary = this.body.substring(this.body.indexOf("boundary=") +1, this.body.length());
			boundary = boundary.substring(boundary.indexOf("=") + 1, boundary.indexOf("\n"));
			
			// Split the body based on the boundary
			String[] bodyElementTokens = this.body.split("--" + boundary);
			
			// Get rid of the header from the body
			bodyElementTokens = Arrays.copyOfRange(bodyElementTokens, 1, bodyElementTokens.length);
				
			// Loop through each body element
			for (String bodyElement : bodyElementTokens) {
				if (bodyElement.contains("Content")) {
					String[] bodyElementContentTokens = bodyElement.split("\n\n");
					
					// Check attachment for base64 encoding
					if (bodyElementContentTokens[0].contains("base64")) {
						BodyElement be = new BodyElement();
						
						// Check for a filename
						if (bodyElementContentTokens[0].contains("name")) {
							String fileName = bodyElementContentTokens[0].substring(bodyElementContentTokens[0].indexOf("name=") + 6, bodyElementContentTokens[0].length());
							fileName = fileName.substring(0, fileName.indexOf("\""));
							be.fileName = fileName;
						} else {
							be.fileName = "NO_NAME_" + Counter.getNextAttachmentID() + ".txt"  ;
						}
						// Data will be second portion of this array
						be.data = bodyElementContentTokens[1];
						
						this.bodyElements.add(be);
					}
				}
			}
		}
	}

}
