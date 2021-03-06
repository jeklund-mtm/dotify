package org.daisy.dotify.impl.input.epub;

import java.util.HashSet;
import java.util.Set;

import org.daisy.dotify.input.InputManager;
import org.daisy.dotify.input.InputManagerFactory;
import org.daisy.dotify.text.FilterLocale;

public class Epub3InputManagerFactory implements InputManagerFactory {
	private final Set<String> supportedLocales;
	private final Set<String> supportedFileFormats;

	public Epub3InputManagerFactory() {
		this.supportedFileFormats = new HashSet<String>();
		supportedFileFormats.add("epub");
		this.supportedLocales = new HashSet<String>();
		supportedLocales.add("sv-SE");
		supportedLocales.add("sv");
		supportedLocales.add("en");
		supportedLocales.add("en-US");
	}

	public Set<String> listSupportedLocales() {
		return supportedLocales;
	}

	public Set<String> listSupportedFileFormats() {
		return supportedFileFormats;
	}

	public boolean supportsSpecification(FilterLocale locale, String fileFormat) {
		return supportedFileFormats.contains(fileFormat);
	}

	public InputManager newInputManager(FilterLocale locale, String fileFormat) {
		if (supportsSpecification(locale, fileFormat)) {
			return new Epub3InputManager();
		}
		return null;
	}

}
