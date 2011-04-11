package org.daisy.dotify.system.tasks.layout.page;

import java.io.Closeable;
import java.util.ArrayList;

import org.daisy.dotify.system.tasks.layout.flow.Marker;
import org.daisy.dotify.system.tasks.layout.flow.Row;


/**
 * <p>Breaks a stream of {@link Row} into pages.</p>
 * 
 * <p>The Paginator implementation is responsible for breaking
 * pages when required by the properties of the {@link LayoutMaster}. It
 * is also responsible for placing page dependent items such
 * as headers, footers and footnotes.</p>
 * 
 * <p>The final result is passed on to the {@link PagedMediaWriter}.</p>
 * 
 * @author Joel Håkansson, TPB
 *
 */
public interface Paginator extends Closeable {
	
	/**
	 * Opens for writing to the supplied writer 
	 * @param writer the PagedMediaWriter to use
	 */
	public void open(PagedMediaWriter writer);

	/**
	 * Adds a new sequence of pages
	 * @param master the {@link LayoutMaster} to use for this sequence
	 * @param pagesOffset page offset
	 */
	public void newSequence(LayoutMaster master, int pagesOffset);
	
	/**
	 * Adds a new sequence of pages. Continue page numbering from preceding sequence or zero if there is no preceding section
	 * @param master the {@link LayoutMaster} to use for this sequence
	 */
	public void newSequence(LayoutMaster master);
	
	/**
	 * Explicitly breaks a page
	 */
	public void newPage();
	
	/**
	 * Adds a new row of characters
	 * @param row the row to add
	 */
	public void newRow(Row row);
	
	/**
	 * Inserts markers that cannot be assigned to a row at the current position
	 * @param m ArrayList of Markers to insert at the current position
	 */
	public void insertMarkers(ArrayList<Marker> m);
	
	/**
	 * Gets information about the current page
	 */
	public CurrentPageInfo getPageInfo();

}