package org.wso2.balana.finder.impl;

import java.util.Set;

import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.PolicyFinderResult;

/**
 * This is file based policy repository. Policies can be inside the directory in
 * a file system. Then you can set directory location using
 * "org.wso2.balana.PolicyDirectory" JAVA property
 */
public class FileBasedPolicyIdFinderModule extends FileBasedPolicyFinderModule {

	public FileBasedPolicyIdFinderModule(Set<String> set) {
		super(set);
	}

	@Override
	public boolean isRequestSupported() {
		return false;
	}

	@Override
	public PolicyFinderResult findPolicy(EvaluationCtx context) {
		return new PolicyFinderResult();
	}
}
