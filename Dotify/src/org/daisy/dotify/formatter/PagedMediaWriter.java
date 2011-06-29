package org.daisy.dotify.formatter;

import java.io.Closeable;
import java.io.OutputStream;




/**
 * <p>PagedMediaWriter is an interface for writing to a paged media.</p>
 * 
 * <p>An implementation of PagedMediaWriter is to render 
 * a result created by e.g. a {@link Paginator} implementation by
 * adding the tokens and properties that are specific for a particular 
 * output format.</p>
 * 
 * <p>The PagedMediaWriter must not alter the page appearance.
 * For example, an implementation of PagedMediaWriter must not break
 * a page unless instructed via {@link #newPage()}.</p>
 * 
 * @author Joel Håkansson, TPB
 */
public interface PagedMediaWriter extends Closeable {
	
	/**
	 * Inserts a new volume in the output format,
	 * if applicable
	 */
	public void newVolume(SectionProperties props);

	/**
	 * Insert a new section in the output format, 
	 * if applicable
	 * @param props the SectionProperties for this section
	 */
	public void newSection(SectionProperties props);

	/**
	 *  Inserts a new page in the output format,
	 *  if applicable
	 */
	public void newPage();

	/**
	 * Add a new row to the current page
	 * @param row the characters to put on the row
	 */
	public void newRow(CharSequence row);
	
	/**
	 * Add a new empty row to the current page  
	 */
	public void newRow();

	/**
	 * Open the PagedMediaWriter for writing
	 * @param os The underlying OutputStream for the PagedMediaWriter
	 * @throws PagedMediaWriterException throws an PagedMediaWriterException if the PagedMediaWriter could not be opened
	 */
	public void open(OutputStream os) throws PagedMediaWriterException;

}