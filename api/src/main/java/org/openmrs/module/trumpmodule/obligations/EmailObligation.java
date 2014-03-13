package org.openmrs.module.trumpmodule.obligations;

import java.util.Date;
import java.util.List;
import java.util.Properties;

//import javax.mail.Address;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;


import luca.data.AttributeQuery;
import luca.tmac.basic.obligations.Obligation;

public class EmailObligation extends Obligation{
	public static String obligationNameXML = "user:send:email";

	public EmailObligation(String pId, Date pStartDate,
			List<AttributeQuery> pParameters) {
		super(pId, pStartDate, pParameters);
	}
	
//public class EmailObligation{
//	
//	public static void main(String[] args){
//		isSatisfied("5","aaa");
//	}
	public String[] readEmails() {
	    // Create all the needed properties 
		Properties props = new Properties();
	    props.setProperty("mail.store.protocol", "imaps");
	    String host = "imap.gmail.com";
	    String username = "anitacao1@gmail.com";
	    String password = "FB529452TBFB";
	    String[] subjects = new String[5];
	    try {
	    	Session session = Session.getInstance(props, null);
	    	Store store = session.getStore();
	    	store.connect(host, username, password);
	            
	        // Get the Inbox folder
	        Folder inbox = store.getFolder("INBOX");
	            
	        // Set the mode to the read-only mode
	        inbox.open(Folder.READ_ONLY);
	       for(int i = inbox.getMessageCount(),j=0; i > inbox.getMessageCount()-5; i--,j++){
	        	Message msg = inbox.getMessage(i);  //get the newest message 
	        	System.out.println("Anita : Reading messages...");
//	        	Address[] in = msg.getFrom();
//	        	for (Address address : in) {
//	        		System.out.println("FROM:" + address.toString());
//	        	}
//	        	Multipart mp = (Multipart) msg.getContent();
//	        	BodyPart bp = mp.getBodyPart(0);
	        	//content = bp.getContent().toString();
	        	subjects[j] = msg.getSubject();
	        	System.out.println("SUBJECT:" + msg.getSubject());
	        }  
	         
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return subjects;
    }
	
	public  boolean checkEmail(String[] subject,String userId,String uuid){
		boolean flag = false;
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
		return flag;
	}
	
	public boolean isSatisfied(String userId,String uuid){
		boolean isSatisfied = false;
		String[] subject = readEmails();
		isSatisfied = checkEmail(subject,userId,uuid);
		return isSatisfied;
	}

}
