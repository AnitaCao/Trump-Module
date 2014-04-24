package luca.tmac.basic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import luca.data.DataHandler;
import luca.tmac.basic.data.AbstractAttributeFinderModule;
import luca.tmac.basic.data.impl.PermissionAttributeFinderModule;

import luca.tmac.basic.data.impl.TaskAttributeFinderModule;
import luca.tmac.basic.data.impl.TeamAttributeFinderModule;

import org.wso2.balana.Balana;
import org.wso2.balana.PDP;
import org.wso2.balana.PDPConfig;
import org.wso2.balana.finder.AttributeFinder;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.PolicyFinder;
import org.wso2.balana.finder.PolicyFinderModule;
import org.wso2.balana.finder.impl.CurrentEnvModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyIdFinderModule;

public class TmacPDP {
	private Balana balana;
	private PDP mainPDP = null;
	DataHandler dh = null;
	
	private List<AttributeFinderModule> finderModules;
	AttributeFinder attributeFinder;
	PDPConfig pdpConfig;
	PolicyFinder pFinder;
	



	public TmacPDP(DataHandler pDh, String top_level_policy_dir,
			String others_policy_dir) {

		balana = Balana.getInstance();

		dh = pDh;
		finderModules = new ArrayList<AttributeFinderModule>();
		
		pdpConfig = balana.getPdpConfig();
		attributeFinder = pdpConfig.getAttributeFinder();

		Set<PolicyFinderModule> pFinderModules = new HashSet<PolicyFinderModule>();

		// instantiate top-level policyFinderModule
		Set<String> topLevelDirs = new HashSet<String>();
		topLevelDirs.add(top_level_policy_dir);
		pFinderModules.add(new FileBasedPolicyFinderModule(topLevelDirs));

		// instantiate other policies policyFinderModule
		Set<String> otherDirs = new HashSet<String>();
		otherDirs.add(others_policy_dir);
		pFinderModules.add(new FileBasedPolicyIdFinderModule(otherDirs));

	
		pFinder = new PolicyFinder();
		pFinder.setModules(pFinderModules);

		// registering new attribute finder. so default PDPConfig is needed to
		// change
		//OpenmrsSubjectAttributeFinderModule is for find the attributes from openmrs not from the xml file
	
		finderModules.add(new PermissionAttributeFinderModule(dh));
		finderModules.add(new TaskAttributeFinderModule(dh));
		finderModules.add(new TeamAttributeFinderModule(dh));
		//finderModules.add(new RiskAttributeFinderModule(dh,
		//		new StandardTrustCalculator(), new StandardRiskCalculator(),new StandardBudgetCalculator()));
		finderModules.add(new CurrentEnvModule());
		
	}

	public void addFinderModule(AbstractAttributeFinderModule s) {
		finderModules.add(s);
	}

	public void createPDP(){
		finderModules.addAll(attributeFinder.getModules());
		attributeFinder.setModules(finderModules);
		mainPDP = new PDP(new PDPConfig(attributeFinder,pFinder,null,true));
	}
	public String evaluate(String request) {
		return mainPDP.evaluate(request);
	}


}
