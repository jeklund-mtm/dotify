package org.daisy.dotify.translator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.spi.ServiceRegistry;

import org.daisy.dotify.text.FilterLocale;

/**
 * Provides a braille translator factory maker. This class will look for 
 * implementations of the BrailleTranslatorFactory interface using the
 * services API. It will return the first implementation that matches the
 * requested specification.
 * 
 * <p>This class can be overridden by extending it and adding a reference
 * to the new implementation to the services API. This class will then
 * choose the new implementation when a new instance is requested.</p>
 * 
 * @author Joel Håkansson
 *
 */
public class BrailleTranslatorFactoryMaker implements BrailleTranslatorFactory {
	private final List<BrailleTranslatorFactory> factories;
	private final Map<String, BrailleTranslatorFactory> map;
	private final Logger logger;

	private BrailleTranslatorFactoryMaker() {
		logger = Logger.getLogger(BrailleTranslatorFactoryMaker.class.getCanonicalName());
		factories = new ArrayList<BrailleTranslatorFactory>();
		Iterator<BrailleTranslatorFactory> i = ServiceRegistry.lookupProviders(BrailleTranslatorFactory.class);
		while (i.hasNext()) {
			factories.add(i.next());
		}
		this.map = new HashMap<String, BrailleTranslatorFactory>();
	}

	/**
	 * Creates a new instance of braille translator factory maker.
	 * @return returns a new braille translator factory maker.
	 */
	public static BrailleTranslatorFactoryMaker newInstance() {
		Iterator<BrailleTranslatorFactoryMaker> i = ServiceRegistry.lookupProviders(BrailleTranslatorFactoryMaker.class);
		while (i.hasNext()) {
			return i.next();
		}
		return new BrailleTranslatorFactoryMaker();
	}
	
	private static String toKey(FilterLocale locale, String grade) {
		return locale.toString() + "(" + grade + ")";
	}
	
	public boolean supportsSpecification(FilterLocale locale, String grade) {
		return map.get(toKey(locale, grade))!=null;
	}
	
	/**
	 * Gets a factory for the given specification.
	 * 
	 * @param locale the locale for the factory
	 * @param grade the grade for the factory
	 * @return returns a braille translator factory
	 * @throws UnsupportedSpecificationException if the specification is not supported
	 */
	public BrailleTranslatorFactory getFactory(FilterLocale locale, String grade) throws UnsupportedSpecificationException {
		BrailleTranslatorFactory template = map.get(toKey(locale, grade));
		if (template==null) {
			for (BrailleTranslatorFactory h : factories) {
				if (h.supportsSpecification(locale, grade)) {
					logger.fine("Found a factory for " + locale + " (" + h.getClass() + ")");
					map.put(toKey(locale, grade), h);
					template = h;
					break;
				}
			}
		}
		if (template==null) {
			throw new UnsupportedSpecificationException("Cannot locate a factory for " + toKey(locale, grade));
		}
		return template;
	}
	
	public BrailleTranslator newTranslator(FilterLocale locale, String grade) throws UnsupportedSpecificationException {
		return getFactory(locale, grade).newTranslator(locale, grade);
	}
}