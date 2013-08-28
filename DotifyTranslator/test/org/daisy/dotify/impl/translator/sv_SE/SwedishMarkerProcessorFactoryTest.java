package org.daisy.dotify.impl.translator.sv_SE;

import static org.junit.Assert.assertEquals;

import org.daisy.dotify.text.FilterLocale;
import org.daisy.dotify.translator.BrailleTranslatorFactory;
import org.daisy.dotify.translator.UnsupportedSpecificationException;
import org.daisy.dotify.translator.attributes.DefaultTextAttribute;
import org.daisy.dotify.translator.attributes.MarkerProcessor;
import org.junit.Test;

public class SwedishMarkerProcessorFactoryTest {
	private final MarkerProcessor processor;

	public SwedishMarkerProcessorFactoryTest() throws UnsupportedSpecificationException {
		processor = new SwedishMarkerProcessorFactory().newMarkerProcessor(FilterLocale.parse("sv-se"), BrailleTranslatorFactory.MODE_UNCONTRACTED);
	}

	@Test
	public void testSub() {
		String text = "H2O";
		DefaultTextAttribute.Builder atts = new DefaultTextAttribute.Builder();
		atts.add(1);
		atts.add(new DefaultTextAttribute.Builder("sub").build(1));
		atts.add(1);
		String actual = processor.processAttributes(atts.build(3), text);
		assertEquals("", "H\u28232O", actual);
	}

	@Test
	public void testSubWithRedundantTextAttributeSuccess() {
		String text = "H2O";
		DefaultTextAttribute.Builder atts = new DefaultTextAttribute.Builder();
		atts.add(1);
		atts.add(new DefaultTextAttribute.Builder("sub").add(new DefaultTextAttribute.Builder().add(1).build(1)).build(1));
		atts.add(1);
		String actual = processor.processAttributes(atts.build(3), text);
		assertEquals("", "H\u28232O", actual);
	}

	@Test
	public void testSubWithRedundantTextAttributeFail() {
		String text = "H2O";
		DefaultTextAttribute.Builder atts = new DefaultTextAttribute.Builder();
		atts.add(1);
		atts.add(new DefaultTextAttribute.Builder("sub").add(new DefaultTextAttribute.Builder().add(new DefaultTextAttribute.Builder("em").build(1)).build(1)).build(1));
		atts.add(1);
		String actual = processor.processAttributes(atts.build(3), text);
		// assert that sub is not added, since the structure is invalid.
		assertEquals("", "H⠠⠄2O", actual);
	}

	@Test
	public void testSup() {
		String text = "3rd";
		DefaultTextAttribute.Builder atts = new DefaultTextAttribute.Builder();
		atts.add(1);
		atts.add(new DefaultTextAttribute.Builder("sup").build(2));
		String actual = processor.processAttributes(atts.build(3), text);
		assertEquals("", "3\u282crd", actual);
	}

}