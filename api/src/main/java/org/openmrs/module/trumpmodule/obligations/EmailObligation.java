package org.openmrs.module.trumpmodule.obligations;

import java.util.Date;
import java.util.List;
import java.util.Properties;



import java.util.UUID;

//import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;

import luca.data.AttributeQuery;
import luca.tmac.basic.obligations.ObligationImpl;

public class EmailObligation extends ObligationImpl {


	public EmailObligation(String actionName, String userId, Date pStartDate,
			List<AttributeQuery> pParameters) {
		super(actionName,userId, pStartDate, pParameters);
		setObUUID(UUID.randomUUID().toString());
	}
	
	public String[] readEmails() {
	    // Create all the needed properties 
		Properties props = new Properties();
	    props.setProperty("mail.store.protocol", "imaps");
	    String host = "imap.gmail.com";
	    String username = "anitacao1@gmail.com";
	    String password = "273488140";
	    String[] subjects = new String[5];
	    try {
	    	Session session = Session.getInstance(props, null);
	    	Store store = session.getStore();
	    	if(!store.isConnected()){
	    		store.connect(host, username, password);
	    	}
	            
	        // Get the Inbox folder
	        Folder inbox = store.getFolder("INBOX");
	            
	        // Set the mode to the read-only mode
	        inbox.open(Folder.READ_ONLY);
	       for(int i = inbox.getMessageCount(),j=0; i > inbox.getMessageCount()-5; i--,j++){
	        	Message msg = inbox.getMessage(i);  //get the newest message 
	        	System.out.println("Anita : Reading messages...");
	        	subjects[j] = msg.getSubject();
	        	System.out.println("SUBJECT:" + msg.getSubject());
	        }  
	       store.close();
	         
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return subjects;
    }
	
	public  boolean checkEmail(String[] subject,String userId,String uuid){
		boolean flag = false;
		if(subject.length>0){
			for(int i = 0;i<subject.length;i++){
				String[] args = null;
				args = subject[i].split(";");
				if(args.length >= 2){
					String a = args[0];
					String b = args[1];
					String userIdString = "["+userId;
					String obligationString = uuid+"]";
			
					if(a.equalsIgnoreCase(userIdString)
						&& b.equalsIgnoreCase(obligationString)){
					return true;
					}
				}
			}
		}
		return flag;
	}
	
	public boolean isSatisfied(String userId,String uuid){
		boolean isSatisfied = false;
		String[] subject = readEmails();
		isSatisfied = checkEmail(subject,userId,uuid);
		return isSatisfied;
	}
}
