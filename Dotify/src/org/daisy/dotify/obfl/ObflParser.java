package org.daisy.dotify.obfl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.daisy.dotify.book.VolumeContentFormatter;
import org.daisy.dotify.formatter.BlockProperties;
import org.daisy.dotify.formatter.BlockStruct;
import org.daisy.dotify.formatter.CompoundField;
import org.daisy.dotify.formatter.CurrentPageField;
import org.daisy.dotify.formatter.Field;
import org.daisy.dotify.formatter.Formatter;
import org.daisy.dotify.formatter.FormatterFactory;
import org.daisy.dotify.formatter.FormattingTypes;
import org.daisy.dotify.formatter.LayoutMaster;
import org.daisy.dotify.formatter.Leader;
import org.daisy.dotify.formatter.MarkerReferenceField;
import org.daisy.dotify.formatter.MarkerReferenceField.MarkerSearchDirection;
import org.daisy.dotify.formatter.MarkerReferenceField.MarkerSearchScope;
import org.daisy.dotify.formatter.NumeralField.NumeralStyle;
import org.daisy.dotify.formatter.PageTemplate;
import org.daisy.dotify.formatter.Position;
import org.daisy.dotify.formatter.SequenceProperties;
import org.daisy.dotify.formatter.StringField;
import org.daisy.dotify.formatter.TextProperties;
import org.daisy.dotify.obfl.TocSequenceEvent.TocRange;
import org.daisy.dotify.text.FilterLocale;

/**
 * Provides a parser for OBFL. The parser accepts OBFL input, either
 * as an InputStream or as an XMLEventReader.
 *
 * @author Joel Håkansson
 *
 */
public class ObflParser {

	private HashMap<String, TableOfContents> tocs;
	private HashMap<String, LayoutMaster> masters;
	private Stack<VolumeTemplate> volumeTemplates;

	private FormatterFactory formatterFactory;
	private Formatter formatter;
	
	public ObflParser() {
		this(FormatterFactory.newInstance());
	}
	
	public ObflParser(FormatterFactory formatterFactory) {
		this.formatterFactory = formatterFactory;
	}
	
	public void setFormatterFactory(FormatterFactory formatterFactory) {
		this.formatterFactory = formatterFactory;
	}
	
	public void parse(InputStream stream) throws XMLStreamException, OBFLParserException {
        XMLInputFactory inFactory = XMLInputFactory.newInstance();
		inFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);        
        inFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        inFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        inFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
        parse(inFactory.createXMLEventReader(stream));
	}
	
	public void parse(XMLEventReader input) throws XMLStreamException, OBFLParserException {
		this.formatter = formatterFactory.newFormatter();
		this.tocs = new HashMap<String, TableOfContents>();
		this.masters = new HashMap<String, LayoutMaster>();
		this.volumeTemplates = new Stack<VolumeTemplate>();
		formatter.open();
		XMLEvent event;
		FilterLocale locale = null;
		boolean hyphenate = true;
		while (input.hasNext()) {
			event = input.nextEvent();
			if (equalsStart(event, ObflQName.OBFL)) {
				String loc = getAttr(event, ObflQName.ATTR_XML_LANG);
				if (loc==null) {
					throw new OBFLParserException("Missing xml:lang on root element");
				} else {
					locale = FilterLocale.parse(loc);
				}
				hyphenate = getHyphenate(event, hyphenate);
			} else if (equalsStart(event, ObflQName.LAYOUT_MASTER)) {
				parseLayoutMaster(event, input);
			} else if (equalsStart(event, ObflQName.SEQUENCE)) {
				parseSequence(event, input, locale, hyphenate);
			} else if (equalsStart(event, ObflQName.TABLE_OF_CONTENTS)) {
				parseTableOfContents(event, input, locale, hyphenate);
			} else if (equalsStart(event, ObflQName.VOLUME_TEMPLATE)) {
				parseVolumeTemplate(event, input, locale, hyphenate);
			}
		}
		try {
			input.close();
			formatter.close();
		} catch (IOException e) {
			throw new OBFLParserException(e);
		}
	}

	//TODO: parse page-number-variable
	private void parseLayoutMaster(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		@SuppressWarnings("unchecked")
		Iterator<Attribute> i = event.asStartElement().getAttributes();
		int width = Integer.parseInt(getAttr(event, ObflQName.ATTR_PAGE_WIDTH));
		int height = Integer.parseInt(getAttr(event, ObflQName.ATTR_PAGE_HEIGHT));
		String masterName = getAttr(event, ObflQName.ATTR_NAME);
		LayoutMasterImpl.Builder masterConfig = new LayoutMasterImpl.Builder(width, height);
		while (i.hasNext()) {
			Attribute atts = i.next();
			String name = atts.getName().getLocalPart();
			String value = atts.getValue();
			if (name.equals("inner-margin")) {
				masterConfig.innerMargin(Integer.parseInt(value));
			} else if (name.equals("outer-margin")) {
				masterConfig.outerMargin(Integer.parseInt(value));
			} else if (name.equals("row-spacing")) {
				masterConfig.rowSpacing(Float.parseFloat(value));
			} else if (name.equals("duplex")) {
				masterConfig.duplex(value.equals("true"));
			}
		}
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.TEMPLATE)) {
				masterConfig.addTemplate(parseTemplate(event, input));
			} else if (equalsStart(event, ObflQName.DEFAULT_TEMPLATE)) {
				masterConfig.addTemplate(parseTemplate(event, input));
			} else if (equalsEnd(event, ObflQName.LAYOUT_MASTER)) {
				break;
			}
		}
		formatter.addLayoutMaster(masterName, masterConfig.build());
		masters.put(masterName, masterConfig.build());
	}
	
	private PageTemplate parseTemplate(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		PageTemplateImpl template;
		if (equalsStart(event, ObflQName.TEMPLATE)) {
			template = new PageTemplateImpl(getAttr(event, ObflQName.ATTR_USE_WHEN));
		} else {
			template = new PageTemplateImpl();
		}
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.HEADER)) {
				ArrayList<Field> fields = parseHeaderFooter(event, input);
				if (fields.size()>0) {
					template.addToHeader(fields);
				}
			} else if (equalsStart(event, ObflQName.FOOTER)) {
				ArrayList<Field> fields = parseHeaderFooter(event, input);
				if (fields.size()>0) {
					template.addToFooter(fields);
				}
			} else if (equalsEnd(event, ObflQName.TEMPLATE) || equalsEnd(event, ObflQName.DEFAULT_TEMPLATE)) {
				break;
			}
		}
		return template;
	}
	
	private ArrayList<Field> parseHeaderFooter(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		ArrayList<Field> fields = new ArrayList<Field>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.FIELD)) {
				ArrayList<Field> compound = parseField(event, input);
				if (compound.size()==1) {
					fields.add(compound.get(0));
				} else {
					CompoundField f = new CompoundField();
					f.addAll(compound);
					fields.add(f);
				}
			} else if (equalsEnd(event, ObflQName.HEADER) || equalsEnd(event, ObflQName.FOOTER)) {
				break;
			}
		}
		return fields;
	}
	
	private ArrayList<Field> parseField(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		ArrayList<Field> compound = new ArrayList<Field>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.STRING)) {
				compound.add(new StringField(getAttr(event, "value")));
			} else if (equalsStart(event, ObflQName.EVALUATE)) {
				//FIXME: add variables...
				compound.add(new StringField(new Expression().evaluate(getAttr(event, "expression"))));
			} else if (equalsStart(event, ObflQName.CURRENT_PAGE)) {
				compound.add(new CurrentPageField(NumeralStyle.valueOf(getAttr(event, "style").toUpperCase())));
			} else if (equalsStart(event, ObflQName.MARKER_REFERENCE)) {
				compound.add(
					new MarkerReferenceField(
							getAttr(event, "marker"), 
							MarkerSearchDirection.valueOf(getAttr(event, "direction").toUpperCase()),
							MarkerSearchScope.valueOf(getAttr(event, "scope").toUpperCase())
					)
				);
			} else if (equalsEnd(event, ObflQName.FIELD)) {
				break;
			}
		}
		return compound;
	}
	
	private void parseSequence(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		formatter.newSequence(builder.build());
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.BLOCK)) {
				parseBlock(event, input, locale, hyph);
			}/* else if (equalsStart(event, LEADER)) {
				parseLeader(event, input);
			}*/
			else if (equalsEnd(event, ObflQName.SEQUENCE)) {
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void parseBlock(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		formatter.startBlock(blockBuilder(event.asStartElement().getAttributes()));
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				formatter.addChars(event.asCharacters().getData(), new TextProperties.Builder(locale).hyphenate(hyph).build());
			} else if (equalsStart(event, ObflQName.BLOCK)) {
				parseBlock(event, input, locale, hyph);
			} else if (equalsStart(event, ObflQName.SPAN)) {
				parseSpan(event, input, locale, hyph);
			} else if (equalsStart(event, ObflQName.LEADER)) {
				formatter.insertLeader(parseLeader(event, input));
			} else if (equalsStart(event, ObflQName.MARKER)) {
				formatter.insertMarker(parseMarker(event, input));
			} else if (equalsStart(event, ObflQName.BR)) {
				formatter.newLine();
				scanEmptyElement(input, ObflQName.BR);
			}
			else if (equalsEnd(event, ObflQName.BLOCK)) {
				break;
			}
		}
		formatter.endBlock();
	}
	
	private void parseSpan(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				formatter.addChars(event.asCharacters().getData(), new TextProperties.Builder(locale).hyphenate(hyph).build());
			} else if (equalsStart(event, ObflQName.LEADER)) {
				formatter.insertLeader(parseLeader(event, input));
			} else if (equalsStart(event, ObflQName.MARKER)) {
				formatter.insertMarker(parseMarker(event, input));
			} else if (equalsStart(event, ObflQName.BR)) {
				formatter.newLine();
				scanEmptyElement(input, ObflQName.BR);
			}
			else if (equalsEnd(event, ObflQName.SPAN)) {
				break;
			}
		}
	}
	
	private BlockProperties blockBuilder(Iterator<Attribute> atts) {
		BlockProperties.Builder builder = new BlockProperties.Builder();
		while (atts.hasNext()) {
			Attribute att = atts.next();
			String name = att.getName().getLocalPart();
			if (name.equals("margin-left")) {
				builder.leftMargin(Integer.parseInt(att.getValue()));
			} else if (name.equals("margin-right")) {
				builder.rightMargin(Integer.parseInt(att.getValue()));
			} else if (name.equals("margin-top")) {
				builder.topMargin(Integer.parseInt(att.getValue()));
			} else if (name.equals("margin-bottom")) {
				builder.bottomMargin(Integer.parseInt(att.getValue()));
			} else if (name.equals("text-indent")) {
				builder.textIndent(Integer.parseInt(att.getValue()));
			} else if (name.equals("first-line-indent")) {
				builder.firstLineIndent(Integer.parseInt(att.getValue()));
			} else if (name.equals("list-type")) {
				builder.listType(FormattingTypes.ListStyle.valueOf(att.getValue().toUpperCase()));
			} else if (name.equals("break-before")) {
				builder.breakBefore(FormattingTypes.BreakBefore.valueOf(att.getValue().toUpperCase()));
			} else if (name.equals("keep")) {
				builder.keep(FormattingTypes.Keep.valueOf(att.getValue().toUpperCase()));
			} else if (name.equals("keep-with-next")) {
				builder.keepWithNext(Integer.parseInt(att.getValue()));
			} else if (name.equals("keep-with-previous-sheets")) {
				builder.keepWithPreviousSheets(Integer.parseInt(att.getValue()));
			} else if (name.equals("keep-with-next-sheets")) {
				builder.keepWithNextSheets(Integer.parseInt(att.getValue()));
			} else if (name.equals("block-indent")) {
				builder.blockIndent(Integer.parseInt(att.getValue()));
			} else if (name.equals("id")) {
				builder.identifier(att.getValue());
			}
		}
		return builder.build();
	}
	
	private LeaderEventContents parseLeader(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		LeaderEventContents.Builder builder = new LeaderEventContents.Builder();
		@SuppressWarnings("unchecked")
		Iterator<Attribute> atts = event.asStartElement().getAttributes();
		while (atts.hasNext()) {
			Attribute att = atts.next();
			String name = att.getName().getLocalPart();
			if (name.equals("align")) {
				builder.align(Leader.Alignment.valueOf(att.getValue().toUpperCase()));
			} else if (name.equals("position")) {
				builder.position(Position.parsePosition(att.getValue()));
			} else if (name.equals("pattern")) {
				builder.pattern(att.getValue());
			}
		}
		scanEmptyElement(input, ObflQName.LEADER);
		return new LeaderEventContents(builder);
	}

	private MarkerEventContents parseMarker(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String markerName = getAttr(event, "class");
		String markerValue = getAttr(event, "value");
		return new MarkerEventContents(markerName, markerValue);
	}
	
	private void parseTableOfContents(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String tocName = getAttr(event, ObflQName.ATTR_NAME);
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		TableOfContentsImpl toc = new TableOfContentsImpl();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.TOC_ENTRY)) {
				toc.add(parseTocEntry(event, input, toc, locale, hyph));
			} else if (equalsEnd(event, ObflQName.TABLE_OF_CONTENTS)) {
				break;
			}
		}
		tocs.put(tocName, toc);
	}

	@SuppressWarnings("unchecked")
	private BlockEvent parseTocEntry(XMLEvent event, XMLEventReader input, TableOfContentsImpl toc, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String refId = getAttr(event, "ref-id");
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		String tocId;
		do {
			tocId = ""+((int)Math.round((99999999*Math.random())));
		} while (toc.containsTocID(tocId));
		TocBlockEvent ret = new TocBlockEvent(refId, tocId, blockBuilder(event.asStartElement().getAttributes()));
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				ret.add(new TextContents(event.asCharacters().getData(), new TextProperties.Builder(locale).hyphenate(hyph).build()));
			} else if (equalsStart(event, ObflQName.TOC_ENTRY)) {
				ret.add(parseTocEntry(event, input, toc, locale, hyph));
			} else if (equalsStart(event, ObflQName.LEADER)) {
				ret.add(parseLeader(event, input));
			} else if (equalsStart(event, ObflQName.MARKER)) {
				ret.add(parseMarker(event, input));
			} else if (equalsStart(event, ObflQName.BR)) {
				ret.add(new LineBreak());
				scanEmptyElement(input, ObflQName.BR);
			} else if (equalsStart(event, ObflQName.PAGE_NUMBER)) {
				ret.add(parsePageNumber(event, input));
			} else if (equalsStart(event, ObflQName.ANCHOR)) {
				//TODO: implement
				throw new UnsupportedOperationException("Not implemented");
			} else if (equalsStart(event, ObflQName.EVALUATE)) {
				ret.add(parseEvaluate(event, input));
			}
			else if (equalsEnd(event, ObflQName.TOC_ENTRY)) {
				break;
			}
		}
		return ret;
	}
	
	private PageNumberReferenceEventContents parsePageNumber(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String refId = getAttr(event, "ref-id");
		NumeralStyle style = NumeralStyle.DEFAULT;
		String styleStr = getAttr(event, "style");
		if (styleStr!=null) {
			try {
				style = NumeralStyle.valueOf(styleStr.toUpperCase());
			} catch (Exception e) { }
		}
		scanEmptyElement(input, ObflQName.PAGE_NUMBER);
		return new PageNumberReferenceEventContents(refId, style);
	}
	
	private Evaluate parseEvaluate(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String expr = getAttr(event, "expression");
		scanEmptyElement(input, ObflQName.EVALUATE);
		return new Evaluate(expr);
	}
	
	private void parseVolumeTemplate(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String volumeVar = getAttr(event, "volume-number-variable");
		String volumeCountVar = getAttr(event, "volume-count-variable");
		String useWhen = getAttr(event, ObflQName.ATTR_USE_WHEN);
		String splitterMax = getAttr(event, "sheets-in-volume-max");
		VolumeTemplateImpl template = new VolumeTemplateImpl(volumeVar, volumeCountVar, useWhen, Integer.parseInt(splitterMax));
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.PRE_CONTENT)) {
				template.setPreVolumeContent(parsePreVolumeContent(event, input, template, locale, hyph));
			} else if (equalsStart(event, ObflQName.POST_CONTENT)) {
				template.setPostVolumeContent(parsePostVolumeContent(event, input, locale, hyph));
			} else if (equalsEnd(event, ObflQName.VOLUME_TEMPLATE)) {
				break;
			}
		}
		volumeTemplates.push(template);
	}
	
	private Iterable<VolumeSequenceEvent> parsePreVolumeContent(XMLEvent event, XMLEventReader input, VolumeTemplate template, FilterLocale locale, boolean hyph) throws XMLStreamException {
		ArrayList<VolumeSequenceEvent> ret = new ArrayList<VolumeSequenceEvent>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.SEQUENCE)) {
				ret.add(parseVolumeSequence(event, input, locale, hyph));
			} else if (equalsStart(event, ObflQName.TOC_SEQUENCE)) {
				ret.add(parseTocSequence(event, input, template, locale, hyph));
			} else if (equalsEnd(event, ObflQName.PRE_CONTENT)) {
				break;
			}
		}
		return ret;
	}
	
	private Iterable<VolumeSequenceEvent> parsePostVolumeContent(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		ArrayList<VolumeSequenceEvent> ret = new ArrayList<VolumeSequenceEvent>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.SEQUENCE)) {
				ret.add(parseVolumeSequence(event, input, locale, hyph));
			} else if (equalsEnd(event, ObflQName.POST_CONTENT)) {
				break;
			}
		}
		return ret;
	}

	private VolumeSequenceEvent parseVolumeSequence(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		StaticSequenceEventImpl volSeq = new StaticSequenceEventImpl(builder.build());
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.BLOCK)) {
				volSeq.add(parseBlockEvent(event, input, locale, hyph));
			} else if (equalsEnd(event, ObflQName.SEQUENCE)) {
				break;
			}
		}
		return volSeq;
	}

	private VolumeSequenceEvent parseTocSequence(XMLEvent event, XMLEventReader input, VolumeTemplate template, FilterLocale locale, boolean hyph) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		String tocName = getAttr(event, "toc");
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		TocRange range = TocRange.valueOf(getAttr(event, "range").toUpperCase());
		String condition = getAttr(event, ObflQName.ATTR_USE_WHEN);
		String volEventVar = getAttr(event, "toc-event-volume-number-variable");
		TocSequenceEventImpl tocSequence = new TocSequenceEventImpl(builder.build(), tocName, range, condition, volEventVar, template);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.ON_TOC_START)) {
				String tmp = getAttr(event, ObflQName.ATTR_USE_WHEN);
				tocSequence.addTocStartEvents(parseOnEvent(event, input, ObflQName.ON_TOC_START, locale, hyph), tmp);
			} else if (equalsStart(event, ObflQName.ON_VOLUME_START)) {
				String tmp = getAttr(event, ObflQName.ATTR_USE_WHEN);
				tocSequence.addVolumeStartEvents(parseOnEvent(event, input, ObflQName.ON_VOLUME_START, locale, hyph), tmp);
			} else if (equalsStart(event, ObflQName.ON_VOLUME_END)) {
				String tmp = getAttr(event, ObflQName.ATTR_USE_WHEN);
				tocSequence.addVolumeEndEvents(parseOnEvent(event, input, ObflQName.ON_VOLUME_END, locale, hyph), tmp);
			} else if (equalsStart(event, ObflQName.ON_TOC_END)) {
				String tmp = getAttr(event, ObflQName.ATTR_USE_WHEN);
				tocSequence.addTocEndEvents(parseOnEvent(event, input, ObflQName.ON_TOC_END, locale, hyph), tmp);
			}
			else if (equalsEnd(event, ObflQName.TOC_SEQUENCE)) {
				break;
			}
		}
		return tocSequence;
	}

	private Iterable<BlockEvent> parseOnEvent(XMLEvent event, XMLEventReader input, QName end, FilterLocale locale, boolean hyph) throws XMLStreamException {
		ArrayList<BlockEvent> ret = new ArrayList<BlockEvent>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ObflQName.BLOCK)) {
				ret.add(parseBlockEvent(event, input, locale, hyph));
			} else if (equalsEnd(event, end)) {
				break;
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private BlockEvent parseBlockEvent(XMLEvent event, XMLEventReader input, FilterLocale locale, boolean hyph) throws XMLStreamException {
		BlockEventImpl ret = new BlockEventImpl(blockBuilder(event.asStartElement().getAttributes()));
		locale = getLang(event, locale);
		hyph = getHyphenate(event, hyph);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				ret.add(new TextContents(event.asCharacters().getData(), new TextProperties.Builder(locale).hyphenate(hyph).build()));
			} else if (equalsStart(event, ObflQName.BLOCK)) {
				ret.add(parseBlockEvent(event, input, locale, hyph));
			} else if (equalsStart(event, ObflQName.LEADER)) {
				ret.add(parseLeader(event, input));
			} else if (equalsStart(event, ObflQName.MARKER)) {
				ret.add(parseMarker(event, input));
			} else if (equalsStart(event, ObflQName.BR)) {
				ret.add(new LineBreak());
				scanEmptyElement(input, ObflQName.BR);
			} else if (equalsStart(event, ObflQName.EVALUATE)) {
				ret.add(parseEvaluate(event, input));
			}
			else if (equalsEnd(event, ObflQName.BLOCK)) {
				break;
			}
		}
		return ret;
	}

	private void scanEmptyElement(XMLEventReader input, QName element) throws XMLStreamException {
		XMLEvent event;
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.getEventType()!=XMLStreamConstants.END_ELEMENT) {
				throw new RuntimeException("Unexpected input");
			} else if (equalsEnd(event, element)) {
				break;
			}
		}
	}

	private String getAttr(XMLEvent event, String attr) {
		return getAttr(event, new QName(attr));
	}
	
	private String getAttr(XMLEvent event, QName attr) {
		Attribute ret = event.asStartElement().getAttributeByName(attr);
		if (ret==null) {
			return null;
		} else {
			return ret.getValue();
		}
	}
	
	private FilterLocale getLang(XMLEvent event, FilterLocale locale) {
		String lang = getAttr(event, ObflQName.ATTR_XML_LANG);
		if (lang!=null) {
			if (lang.equals("")) {
				return null;
			} else {
				return FilterLocale.parse(lang);
			}
		}
		return locale;
	}
	
	private boolean getHyphenate(XMLEvent event, boolean hyphenate) {
		String hyph = getAttr(event, ObflQName.ATTR_HYPHENATE);
		if (hyph!=null) {
			return hyph.equals("true");
		}
		return hyphenate;
	}
	
	private boolean equalsStart(XMLEvent event, QName element) {
		return 	event.getEventType()==XMLStreamConstants.START_ELEMENT
				&& event.asStartElement().getName().equals(element);
	}
	
	private boolean equalsEnd(XMLEvent event, QName element) {
		return 	event.getEventType()==XMLStreamConstants.END_ELEMENT 
				&& event.asEndElement().getName().equals(element);
	}
	
	public BlockStruct getBlockStruct() {
		return formatter.getFlowStruct();
	}
	
	public VolumeContentFormatter getVolumeContentFormatter() {
		return new BlockEventHandlerRunner(formatterFactory, masters, tocs, volumeTemplates);
	}

}