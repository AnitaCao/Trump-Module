package org.openmrs.module.trumpmodule.policies;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

/**
 * Load a bunch of policies in from files, using a directory pattern:
 * ./policies/[user_uuid]/[policy_uuid]
 * 
 * @author Chris Burnett
 *
 */
public class PolicyFileHandler {
	
	/**
	 * Load policies from files
	 * @param rootDir root directory where policy folders are located
	 * @return a structure mapping user IDs to a list of owned policies
	 * @throws IOException if something goes wrong during loading
	 */
    public static HashMap<String, ArrayList<Policy>> loadPolicies(String rootDir) throws IOException {
       HashMap<String, ArrayList<Policy>> policies = new HashMap<String, ArrayList<Policy>>();

        // loop through directories in 'policies'
        File policyDir = new File(rootDir);
        Iterator<File> dirs = FileUtils.iterateFilesAndDirs(policyDir, DirectoryFileFilter.INSTANCE, TrueFileFilter.INSTANCE);
        dirs.next(); // skip the root directory
        while(dirs.hasNext()) {
        	// for each directory, directory name is the user id
        	File dir = dirs.next();
        	String uid = dir.getName();
            // for each file in the directory, add it to the hashmap under key uid
        	policies.put(uid, new ArrayList<Policy>());
            // by creating a new policy object and setting the file path
        	Iterator<File> policyFiles = FileUtils.iterateFiles(dir, new SuffixFileFilter(".xml"), TrueFileFilter.INSTANCE);
        	while(policyFiles.hasNext()) {
        		File policyFile = policyFiles.next();
        		String pid = policyFile.getName().split("\\.")[0]; // not the .xml part
        		String content = FileUtils.readFileToString(policyFile);
        		Policy policy = new Policy(pid, uid, content);
        		// add the new policy to the hashmap structure
        		policies.get(uid).add(policy);
        	}	
        }
        return policies;
    }
    
    /** 
     * Save policies in a given structure to the file system
     * @param rootDir root directory for policy files
     * @param policies hashmap containing policies
     * @throws IOException
     */
    public static void savePolicies(String rootDir, HashMap<String, ArrayList<Policy>> policies) throws IOException {
    	// try to save each policy
    	for(Entry<String, ArrayList<Policy>> e : policies.entrySet()) {
    		// write the files in the correct directory
    		for(Policy policy : e.getValue()) {
    			File policyDir = new File(rootDir + 
											File.separator + 
											e.getKey() +
											File.separator);
    			File policyFile = new File(policyDir, policy.getId() + ".xml");
    			// make directories if required
    			FileUtils.forceMkdir(policyDir);
    			// write into the file and move on
    			FileUtils.writeStringToFile(policyFile, policy.getContent(), (Charset) null, false);
    		}
    	}
    }

}
