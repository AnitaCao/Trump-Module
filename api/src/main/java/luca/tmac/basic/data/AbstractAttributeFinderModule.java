package luca.tmac.basic.data;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.balana.attr.AttributeValue;
import org.wso2.balana.attr.BagAttribute;
import org.wso2.balana.attr.StringAttribute;
import org.wso2.balana.cond.EvaluationResult;
import org.wso2.balana.ctx.EvaluationCtx;
import org.wso2.balana.finder.AttributeFinderModule;
import org.wso2.balana.finder.impl.FileBasedPolicyFinderModule;

public abstract class AbstractAttributeFinderModule extends AttributeFinderModule{
	
	private static Log log = LogFactory.getLog(FileBasedPolicyFinderModule.class);
	
	public abstract Set<String> getSupportedCategories();
	
	public abstract Set<String> getSupportedIds();
	
	public abstract EvaluationResult findAttribute(URI attributeType, URI attributeId,
			String issuer, URI category, EvaluationCtx context);
	
	protected BagAttribute getEmptyBag()
	{
		URI type = null;
		try {
			 type = new URI(StringAttribute.identifier);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		ArrayList c = new ArrayList();
		return new BagAttribute(type,c);
	}
	
	protected String getIdFromContext(URI idURI,
			String issuer, URI category, EvaluationCtx context)
	{
		String id = "";
		URI attributeType= null;
		try {
			attributeType = new URI(StringAttribute.identifier);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		//try to get the Subject ID from the request!!!!
		EvaluationResult result = context.getAttribute(attributeType,
				idURI, issuer, category);

		if (result != null && result.getAttributeValue() != null
				&& result.getAttributeValue().isBag()) {
			BagAttribute bagAttribute = (BagAttribute) result
					.getAttributeValue();
			if (bagAttribute.size() > 0) {
				id = ((AttributeValue) bagAttribute.iterator().next())
						.encode();
			} else {
				log.debug("subject not found");
			}
		}
		return id;
	}
	
	public boolean isDesignatorSupported() {
		return true;
	}

}
