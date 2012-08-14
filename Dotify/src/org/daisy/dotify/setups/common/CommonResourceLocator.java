package org.daisy.dotify.setups.common;

import java.net.URL;

import org.daisy.dotify.system.AbstractResourceLocator;
import org.daisy.dotify.system.ResourceLocatorException;

/**
 * Provides a method to find resources relative to this class 
 * @author Joel Håkansson
 *
 */
public class CommonResourceLocator extends AbstractResourceLocator {
	public enum CommonResourceIdentifier {
		OBFL_WHITESPACE_NORMALIZER_XSLT;
	}
	
	/**
	 * Gets a resource by identifier. It is preferred to use this method 
	 * rather than get a resource by string, since the internal structure
	 * of this package should be considered opaque to users of this class.
	 * @param identifier the identifier of the resource to get.
	 * @return returns the URL to the resource
	 */
	public URL getResourceByIdentifier(CommonResourceIdentifier identifier) {
		try {
			switch (identifier) {
				case OBFL_WHITESPACE_NORMALIZER_XSLT: return getResource("xslt/flow-whitespace-normalizer.xsl");
				default: return null;
			}
		} catch (ResourceLocatorException e) {
			throw new RuntimeException("Could not locate resource by enum identifier. This is a coding error.", e);
		}
	}

}
