package org.daisy.dotify.input;

import static org.junit.Assert.assertTrue;

import org.daisy.dotify.text.FilterLocale;
import org.junit.Test;

public class InputManagerFactoryMakerTest {

	@Test
	public void testFactoryExists() {
		//Setup
		InputManagerFactoryMaker factory = InputManagerFactoryMaker.newInstance();
		
		//Test
		assertTrue(factory != null);
	}
	
	@Test
	public void testGetFactoryForEnglish() {
		//Setup
		InputManagerFactoryMaker factory = InputManagerFactoryMaker.newInstance();
		FilterLocale locale = FilterLocale.parse("en-US");
		
		//Test
		assertTrue(factory.getFactory(locale, "xml")!=null);
	}

}
