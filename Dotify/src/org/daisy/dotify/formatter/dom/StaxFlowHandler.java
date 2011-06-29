package org.daisy.dotify.formatter.dom;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.XMLEvent;

import org.daisy.dotify.formatter.Expression;
import org.daisy.dotify.formatter.Formatter;
import org.daisy.dotify.formatter.FormatterFactory;
import org.daisy.dotify.formatter.Position;
import org.daisy.dotify.formatter.dom.MarkerReferenceField.MarkerSearchDirection;
import org.daisy.dotify.formatter.dom.MarkerReferenceField.MarkerSearchScope;
import org.daisy.dotify.formatter.dom.NumeralField.NumeralStyle;
import org.daisy.dotify.formatter.dom.TocSequenceEvent.TocRange;
import org.daisy.dotify.formatter.impl.DefaultSequenceEvent;

public class StaxFlowHandler {
	private final static QName LAYOUT_MASTER = new QName("layout-master");
	private final static QName TEMPLATE = new QName("template");
	private final static QName DEFAULT_TEMPLATE = new QName("default-template");
	private final static QName HEADER = new QName("header");
	private final static QName FOOTER = new QName("footer");
	private final static QName FIELD = new QName("field");
	private final static QName STRING = new QName("string");
	private final static QName EVALUATE = new QName("evaluate");
	private final static QName CURRENT_PAGE = new QName("current-page");
	private final static QName MARKER_REFERENCE = new QName("marker-reference");
	private final static QName BLOCK = new QName("block");
	private final static QName TOC_ENTRY = new QName("toc-entry");
	private final static QName LEADER = new QName("leader");
	private final static QName MARKER = new QName("marker");
	private final static QName ANCHOR = new QName("anchor");
	private final static QName BR = new QName("br");
	private final static QName PAGE_NUMBER = new QName("page-number");
	
	private final static QName SEQUENCE = new QName("sequence");
	private final static QName VOLUME_TEMPLATE = new QName("volume-template");
	private final static QName PRE_CONTENT = new QName("pre-content");
	private final static QName POST_CONTENT = new QName("post-content");
	private final static QName TOC_SEQUENCE = new QName("toc-sequence");
	private final static QName ON_TOC_START = new QName("on-toc-start");
	private final static QName ON_VOLUME_START = new QName("on-volume-start");
	private final static QName ON_VOLUME_END = new QName("on-volume-end");
	private final static QName ON_TOC_END = new QName("on-toc-end");
	
	private final static QName TABLE_OF_CONTENTS = new QName("table-of-contents");
	
	private final static QName ATTR_PAGE_WIDTH = new QName("page-width");
	private final static QName ATTR_PAGE_HEIGHT = new QName("page-height");
	private final static QName ATTR_NAME = new QName("name");
	
	private final Formatter flow;
	private final HashMap<String, TableOfContents> tocs;
	private final HashMap<String, LayoutMaster> masters;
	private final Stack<VolumeTemplate> volumeTemplates;
	
	public StaxFlowHandler(FormatterFactory flow) {
		this.flow = flow.newFormatter();
		this.tocs = new HashMap<String, TableOfContents>();
		this.masters = new HashMap<String, LayoutMaster>();
		this.volumeTemplates = new Stack<VolumeTemplate>();
	}
	
	public void parse(XMLEventReader input) throws XMLStreamException {
		flow.open();
		XMLEvent event;
		while (input.hasNext()) {
			event = input.nextEvent();
			if (equalsStart(event, LAYOUT_MASTER)) {
				parseLayoutMaster(event, input);
			} else if (equalsStart(event, SEQUENCE)) {
				parseSequence(event, input);
			} else if (equalsStart(event, TABLE_OF_CONTENTS)) {
				parseTableOfContents(event, input);
			} else if (equalsStart(event, VOLUME_TEMPLATE)) {
				parseVolumeTemplate(event, input);
			}
		}
		try {
			flow.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	//TODO: parse page-number-variable
	private void parseLayoutMaster(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		@SuppressWarnings("unchecked")
		Iterator<Attribute> i = event.asStartElement().getAttributes();
		int width = Integer.parseInt(getAttr(event, ATTR_PAGE_WIDTH));
		int height = Integer.parseInt(getAttr(event, ATTR_PAGE_HEIGHT));
		String masterName = getAttr(event, ATTR_NAME);
		ConfigurableLayoutMaster.Builder masterConfig = new ConfigurableLayoutMaster.Builder(width, height);
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
			if (equalsStart(event, TEMPLATE)) {
				masterConfig.addTemplate(parseTemplate(event, input));
			} else if (equalsStart(event, DEFAULT_TEMPLATE)) {
				masterConfig.addTemplate(parseTemplate(event, input));
			} else if (equalsEnd(event, LAYOUT_MASTER)) {
				break;
			}
		}
		flow.addLayoutMaster(masterName, masterConfig.build());
		masters.put(masterName, masterConfig.build());
	}
	
	private PageTemplate parseTemplate(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		DefaultPageTemplate template;
		if (equalsStart(event, TEMPLATE)) {
			template = new DefaultPageTemplate(getAttr(event, "use-when"));
		} else {
			template = new DefaultPageTemplate();
		}
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, HEADER)) {
				ArrayList<Object> fields = parseHeaderFooter(event, input);
				if (fields.size()>0) {
					template.addToHeader(fields);
				}
			} else if (equalsStart(event, FOOTER)) {
				ArrayList<Object> fields = parseHeaderFooter(event, input);
				if (fields.size()>0) {
					template.addToFooter(fields);
				}
			} else if (equalsEnd(event, TEMPLATE) || equalsEnd(event, DEFAULT_TEMPLATE)) {
				break;
			}
		}
		return template;
	}
	
	private ArrayList<Object> parseHeaderFooter(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		ArrayList<Object> fields = new ArrayList<Object>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, FIELD)) {
				ArrayList<Object> compound = parseField(event, input);
				if (compound.size()==1) {
					fields.add(compound.get(0));
				} else {
					CompoundField f = new CompoundField();
					f.addAll(compound);
					fields.add(f);
				}
			} else if (equalsEnd(event, HEADER) || equalsEnd(event, FOOTER)) {
				break;
			}
		}
		return fields;
	}
	
	private ArrayList<Object> parseField(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		ArrayList<Object> compound = new ArrayList<Object>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, STRING)) {
				compound.add(getAttr(event, "value"));
			} else if (equalsStart(event, EVALUATE)) {
				//FIXME: add variables...
				compound.add(new Expression().evaluate(getAttr(event, "expression")));
			} else if (equalsStart(event, CURRENT_PAGE)) {
				compound.add(new CurrentPageField(NumeralStyle.valueOf(getAttr(event, "style").toUpperCase())));
			} else if (equalsStart(event, MARKER_REFERENCE)) {
				compound.add(
					new MarkerReferenceField(
							getAttr(event, "marker"), 
							MarkerSearchDirection.valueOf(getAttr(event, "direction").toUpperCase()),
							MarkerSearchScope.valueOf(getAttr(event, "scope").toUpperCase())
					)
				);
			} else if (equalsEnd(event, FIELD)) {
				break;
			}
		}
		return compound;
	}
	
	private void parseSequence(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		flow.newSequence(builder.build());
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, BLOCK)) {
				parseBlock(event, input);
			}/* else if (equalsStart(event, LEADER)) {
				parseLeader(event, input);
			}*/
			else if (equalsEnd(event, SEQUENCE)) {
				break;
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	private void parseBlock(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		flow.startBlock(blockBuilder(event.asStartElement().getAttributes()));
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				flow.addChars(event.asCharacters().getData());
			} else if (equalsStart(event, BLOCK)) {
				parseBlock(event, input);
			} else if (equalsStart(event, LEADER)) {
				flow.insertLeader(parseLeader(event, input));
			} else if (equalsStart(event, MARKER)) {
				flow.insertMarker(parseMarker(event, input));
			} else if (equalsStart(event, BR)) {
				flow.newLine();
				scanEmptyElement(input, BR);
			}
			else if (equalsEnd(event, BLOCK)) {
				break;
			}
		}
		flow.endBlock();
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
			} else if (name.equals("block-indent")) {
				builder.blockIndent(Integer.parseInt(att.getValue()));
			} else if (name.equals("id")) {
				builder.identifier(att.getValue());
			}
		}
		return builder.build();
	}
	
	private void parseFloat(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		
	}
	
	private Leader parseLeader(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		Leader.Builder builder = new Leader.Builder();
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
		scanEmptyElement(input, LEADER);
		return builder.build();
	}

	private Marker parseMarker(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String markerName = getAttr(event, "class");
		String markerValue = getAttr(event, "value");
		return new Marker(markerName, markerValue);
	}
	
	private void parseTableOfContents(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String tocName = getAttr(event, "name");
		TableOfContents toc = new TableOfContents();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, TOC_ENTRY)) {
				toc.add(parseTocEntry(event, input, toc));
			} else if (equalsEnd(event, TABLE_OF_CONTENTS)) {
				break;
			}
		}
		tocs.put(tocName, toc);
	}

	@SuppressWarnings("unchecked")
	private BlockEvent parseTocEntry(XMLEvent event, XMLEventReader input, TableOfContents toc) throws XMLStreamException {
		String refId = getAttr(event, "ref-id");
		String tocId;
		do {
			tocId = ""+((int)Math.round((99999999*Math.random())));
		} while (toc.containsTocID(tocId));
		TocEventImpl ret = new TocEventImpl(refId, tocId, blockBuilder(event.asStartElement().getAttributes()));
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				ret.add(new TextContents(event.asCharacters().getData()));
			} else if (equalsStart(event, TOC_ENTRY)) {
				ret.add(parseTocEntry(event, input, toc));
			} else if (equalsStart(event, LEADER)) {
				ret.add(parseLeader(event, input));
			} else if (equalsStart(event, MARKER)) {
				ret.add(parseMarker(event, input));
			} else if (equalsStart(event, BR)) {
				ret.add(new LineBreak());
				scanEmptyElement(input, BR);
			} else if (equalsStart(event, PAGE_NUMBER)) {
				ret.add(parsePageNumber(event, input));
			} else if (equalsStart(event, ANCHOR)) {
				//TODO: implement
				throw new UnsupportedOperationException("Not implemented");
			} else if (equalsStart(event, EVALUATE)) {
				ret.add(parseEvaluate(event, input));
			}
			else if (equalsEnd(event, TOC_ENTRY)) {
				break;
			}
		}
		return ret;
	}
	
	private PageNumberReference parsePageNumber(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String refId = getAttr(event, "ref-id");
		scanEmptyElement(input, PAGE_NUMBER);
		return new PageNumberReference(refId);
	}
	
	private Evaluate parseEvaluate(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String expr = getAttr(event, "expression");
		scanEmptyElement(input, EVALUATE);
		return new Evaluate(expr);
	}
	
	private void parseVolumeTemplate(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String volumeVar = getAttr(event, "volume-number-variable");
		String volumeCountVar = getAttr(event, "volume-count-variable");
		String useWhen = getAttr(event, "use-when");
		DefaultVolumeTemplate template = new DefaultVolumeTemplate(volumeVar, volumeCountVar, useWhen);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, PRE_CONTENT)) {
				template.setPreVolumeContent(parsePreVolumeContent(event, input, template));
			} else if (equalsStart(event, POST_CONTENT)) {
				template.setPostVolumeContent(parsePostVolumeContent(event, input));
			} else if (equalsEnd(event, VOLUME_TEMPLATE)) {
				break;
			}
		}
		volumeTemplates.push(template);
	}
	
	private Iterable<VolumeSequence> parsePreVolumeContent(XMLEvent event, XMLEventReader input, VolumeTemplate template) throws XMLStreamException {
		ArrayList<VolumeSequence> ret = new ArrayList<VolumeSequence>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, SEQUENCE)) {
				ret.add(parseVolumeSequence(event, input));
			} else if (equalsStart(event, TOC_SEQUENCE)) {
				ret.add(parseTocSequence(event, input, template));
			} else if (equalsEnd(event, PRE_CONTENT)) {
				break;
			}
		}
		return ret;
	}
	
	private Iterable<VolumeSequence> parsePostVolumeContent(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		ArrayList<VolumeSequence> ret = new ArrayList<VolumeSequence>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, SEQUENCE)) {
				ret.add(parseVolumeSequence(event, input));
			} else if (equalsEnd(event, POST_CONTENT)) {
				break;
			}
		}
		return ret;
	}

	private VolumeSequence parseVolumeSequence(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		DefaultSequenceEvent volSeq = new DefaultSequenceEvent(builder.build());
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, BLOCK)) {
				volSeq.add(parseBlockEvent(event, input));
			} else if (equalsEnd(event, SEQUENCE)) {
				break;
			}
		}
		return volSeq;
	}

	private VolumeSequence parseTocSequence(XMLEvent event, XMLEventReader input, VolumeTemplate template) throws XMLStreamException {
		String masterName = getAttr(event, "master");
		String tocName = getAttr(event, "toc");
		SequenceProperties.Builder builder = new SequenceProperties.Builder(masterName);
		String initialPageNumber = getAttr(event, "initial-page-number");
		if (initialPageNumber!=null) {
			builder.initialPageNumber(Integer.parseInt(initialPageNumber));
		}
		TocRange range = TocRange.valueOf(getAttr(event, "range").toUpperCase());
		String condition = getAttr(event, "use-when");
		String volEventVar = getAttr(event, "toc-event-volume-number-variable");
		TocSequenceEventImpl tocSequence = new TocSequenceEventImpl(builder.build(), tocName, range, condition, volEventVar, template);
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, ON_TOC_START)) {
				String tmp = getAttr(event, "use-when");
				tocSequence.addTocStartEvents(parseOnEvent(event, input, ON_TOC_START), tmp);
			} else if (equalsStart(event, ON_VOLUME_START)) {
				String tmp = getAttr(event, "use-when");
				tocSequence.addVolumeStartEvents(parseOnEvent(event, input, ON_VOLUME_START), tmp);
			} else if (equalsStart(event, ON_VOLUME_END)) {
				String tmp = getAttr(event, "use-when");
				tocSequence.addVolumeEndEvents(parseOnEvent(event, input, ON_VOLUME_END), tmp);
			} else if (equalsStart(event, ON_TOC_END)) {
				String tmp = getAttr(event, "use-when");
				tocSequence.addTocEndEvents(parseOnEvent(event, input, ON_TOC_END), tmp);
			}
			else if (equalsEnd(event, TOC_SEQUENCE)) {
				break;
			}
		}
		return tocSequence;
	}

	private Iterable<BlockEvent> parseOnEvent(XMLEvent event, XMLEventReader input, QName end) throws XMLStreamException {
		ArrayList<BlockEvent> ret = new ArrayList<BlockEvent>();
		while (input.hasNext()) {
			event=input.nextEvent();
			if (equalsStart(event, BLOCK)) {
				ret.add(parseBlockEvent(event, input));
			} else if (equalsEnd(event, end)) {
				break;
			}
		}
		return ret;
	}
	
	@SuppressWarnings("unchecked")
	private BlockEvent parseBlockEvent(XMLEvent event, XMLEventReader input) throws XMLStreamException {
		BlockEventImpl ret = new BlockEventImpl(blockBuilder(event.asStartElement().getAttributes()));
		while (input.hasNext()) {
			event=input.nextEvent();
			if (event.isCharacters()) {
				ret.add(new TextContents(event.asCharacters().getData()));
			} else if (equalsStart(event, BLOCK)) {
				ret.add(parseBlockEvent(event, input));
			} else if (equalsStart(event, LEADER)) {
				ret.add(parseLeader(event, input));
			} else if (equalsStart(event, MARKER)) {
				ret.add(parseMarker(event, input));
			} else if (equalsStart(event, BR)) {
				ret.add(new LineBreak());
				scanEmptyElement(input, BR);
			} else if (equalsStart(event, EVALUATE)) {
				ret.add(parseEvaluate(event, input));
			}
			else if (equalsEnd(event, BLOCK)) {
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
	
	private boolean equalsStart(XMLEvent event, QName element) {
		return 	event.getEventType()==XMLStreamConstants.START_ELEMENT
				&& event.asStartElement().getName().equals(element);
	}
	
	private boolean equalsEnd(XMLEvent event, QName element) {
		return 	event.getEventType()==XMLStreamConstants.END_ELEMENT 
				&& event.asEndElement().getName().equals(element);
	}
	
	public Map<String, TableOfContents> getTocs() {
		return tocs;
	}
	
	public Iterable<VolumeTemplate> getVolumeTemplates() {
		return volumeTemplates;
	}
	
	public Map<String, LayoutMaster> getMasters() {
		return masters;
	}
	
	public BlockStruct getBlockStruct() {
		return flow.getFlowStruct();
	}

}