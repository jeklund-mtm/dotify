package org.daisy.dotify.system.tasks.layout.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

import org.daisy.dotify.formatter.BlockProperties;
import org.daisy.dotify.formatter.BlockStruct;
import org.daisy.dotify.formatter.Formatter;
import org.daisy.dotify.formatter.FormattingTypes;
import org.daisy.dotify.formatter.Leader;
import org.daisy.dotify.formatter.Marker;
import org.daisy.dotify.formatter.Row;
import org.daisy.dotify.formatter.SequenceProperties;
import org.daisy.dotify.formatter.SpanProperties;
import org.daisy.dotify.system.tasks.layout.page.LayoutMaster;
import org.daisy.dotify.system.tasks.layout.text.FilterFactory;
import org.daisy.dotify.system.tasks.layout.utils.BlockHandler;
import org.daisy.dotify.system.tools.StateObject;


/**
 * Breaks flow into rows, page related block properties are left to next step
 * @author Joel Håkansson, TPB
 */
public class FormatterImpl implements Formatter {
	private int leftMargin;
	private int rightMargin;
	private final BlockStructImpl flowStruct;
	private final Stack<BlockProperties> context;
	private boolean firstRow;
	private final StateObject state;

	private final BlockHandler bh;

	// TODO: fix recursive keep problem
	// TODO: Implement SpanProperites
	// TODO: Implement floating elements
	/**
	 * Create a new flow
	 * @param filtersFactory the filters factory to use
	 */
	public FormatterImpl(FilterFactory filtersFactory) {
		//this.filters = builder.filtersFactory.getDefault();
		this.context = new Stack<BlockProperties>();
		this.leftMargin = 0;
		this.rightMargin = 0;
		this.flowStruct = new BlockStructImpl(); //masters
		this.bh = new BlockHandler(filtersFactory.getDefault());
		this.state = new StateObject();
	}

	public void open() {
		state.assertUnopened();
		state.open();
	}

	public void addLayoutMaster(String name, LayoutMaster master) {
		flowStruct.addLayoutMaster(name, master);
	}

	//TODO Handle SpanProperites
	public void addChars(CharSequence c, SpanProperties p) {
		state.assertOpen();
		addChars(c);
	}

	// Using BlockHandler
	public void addChars(CharSequence c) {
		state.assertOpen();
		assert context.size()!=0;
		if (context.size()==0) return;
		bh.setBlockProperties(context.peek());
		bh.setWidth(flowStruct.getLayoutMaster(flowStruct.getCurrentSequence().getSequenceProperties().getMasterName()).getFlowWidth() - rightMargin);
		ArrayList<Row> ret;
		if (firstRow) {
			ret = bh.layoutBlock(c, leftMargin, flowStruct.getLayoutMaster(flowStruct.getCurrentSequence().getSequenceProperties().getMasterName()));
			firstRow = false;
		} else {
			Row r = flowStruct.getCurrentSequence().getCurrentGroup().pop();
			ret = bh.appendBlock(c, leftMargin, r, flowStruct.getLayoutMaster(flowStruct.getCurrentSequence().getSequenceProperties().getMasterName()));
		}
		for (Row r : ret) {
			flowStruct.getCurrentSequence().getCurrentGroup().push(r);
		}
	}
	// END Using BlockHandler

	public void insertMarker(Marker m) {
		state.assertOpen();
		flowStruct.getCurrentSequence().getCurrentGroup().addMarker(m);
	}

	public void startBlock(BlockProperties p) {
		state.assertOpen();
		assert bh.getCurrentLeader() == null;
		if (context.size()>0) {
			bh.addToBlockIndent(context.peek().getBlockIndent());
			if (context.peek().getListType()!=FormattingTypes.ListStyle.NONE) {
				String listLabel;
				switch (context.peek().getListType()) {
				case OL:
					listLabel = context.peek().nextListNumber()+""; break;
				case UL:
					listLabel = "•";
					break;
				case PL: default:
					listLabel = "";
				}
				bh.setListItem(listLabel, context.peek().getListType());
			}
		}
		BlockImpl c = flowStruct.getCurrentSequence().newFlowGroup();
		c.addSpaceBefore(p.getTopMargin());
		c.setBreakBeforeType(p.getBreakBeforeType());
		c.setKeepType(p.getKeepType());
		c.setKeepWithNext(p.getKeepWithNext());
		c.setIdentifier(p.getIdentifier());
		context.push(p);
		leftMargin += p.getLeftMargin();
		rightMargin += p.getRightMargin();
		firstRow = true;
	}
	
	public void endBlock() {
		state.assertOpen();
		/*if (currentLeader!=null) {
			addChars("");
		}*/
		// BlockHandler
		if (bh.getCurrentLeader()!=null || bh.getListItem()!=null) {
			addChars("");
		}
		// BlockHandler
		BlockProperties p = context.pop();
		flowStruct.getCurrentSequence().getCurrentGroup().addSpaceAfter(p.getBottomMargin());
		if (context.size()>0) {
			BlockImpl c = flowStruct.getCurrentSequence().newFlowGroup();
			c.setKeepType(context.peek().getKeepType());
			c.setKeepWithNext(context.peek().getKeepWithNext());
			bh.subtractFromBlockIndent(context.peek().getBlockIndent());
		}
		leftMargin -= p.getLeftMargin();
		rightMargin -= p.getRightMargin();
		firstRow = true;
	}

	public void newSequence(SequenceProperties p) {
		state.assertOpen();
		flowStruct.newSequence(p);
	}

	public void insertLeader(Leader leader) {
		state.assertOpen();
		//currentLeader = leader;
		if (bh.getCurrentLeader()!=null) {
			addChars("");
		}
		bh.setCurrentLeader(leader);
	}
	
	public void newLine() {
		state.assertOpen();
		Row r = new Row("");
		r.setLeftMargin(leftMargin + context.peek().getTextIndent());
		flowStruct.getCurrentSequence().getCurrentGroup().push(r);
	}

	/**
	 * Gets the resulting data structure
	 * @return returns the data structure
	 * @throws IllegalStateException if not closed 
	 */
	public BlockStruct getFlowStruct() {
		state.assertClosed();
		return flowStruct;
	}
	
	public void close() throws IOException {
		if (state.isClosed()) {
			return;
		}
		state.assertOpen();
		state.close();
	}

	public void endFloat() {
		state.assertOpen();
		// TODO Auto-generated method stub
		
	}

	public void insertAnchor(String ref) {
		state.assertOpen();
		// TODO Auto-generated method stub
		
	}

	public void startFloat(String id) {
		state.assertOpen();
		// TODO Auto-generated method stub
		
	}
}
