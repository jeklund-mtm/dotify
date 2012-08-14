package org.daisy.dotify.setups.en_US;

import java.util.ArrayList;

import org.daisy.dotify.formatter.writers.TextMediaWriter;
import org.daisy.dotify.system.InternalTask;
import org.daisy.dotify.system.LayoutEngineTask;
import org.daisy.dotify.system.RunParameters;
import org.daisy.dotify.system.TaskSystem;
import org.daisy.dotify.system.TaskSystemException;
import org.daisy.dotify.text.FilterFactory;
import org.daisy.dotify.text.FilterLocale;
import org.daisy.dotify.text.RegexFilter;
import org.daisy.dotify.text.StringFilter;


/**
 * <p>Transforms a DTBook 2005-3 into text format.
 * The input DTBook should be hyphenated (using SOFT HYPHEN U+00AD) at all
 * breakpoints prior to conversion.</p>
 * @author Joel Håkansson, TPB
 *
 */
public class DefaultTextSystem implements TaskSystem {
	private final String name;
	
	public DefaultTextSystem(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}

	public ArrayList<InternalTask> compile(RunParameters p) throws TaskSystemException {
		
		ArrayList<InternalTask> setup = new ArrayList<InternalTask>();

		// Layout FLOW as text
		MyFilterFactory factory = new MyFilterFactory();
		TextMediaWriter paged = new TextMediaWriter(p.getProperties(), "UTF-8");

		setup.add(new LayoutEngineTask("OBFL to Text converter", factory, FilterLocale.parse("en"), paged));

		return setup;
	}
	
	private static class MyFilterFactory implements FilterFactory {
		public StringFilter newStringFilter(FilterLocale target) {
			// remove zero width space
			return new RegexFilter("\\u200B", "");
		}
	}

}