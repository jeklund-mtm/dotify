package org.daisy.dotify.text;


/**
 * Breaks a paragraph of text into rows. It is assumed that all 
 * preferred break points are supplied with the input String.
 * As a consequence, non standard hyphenation is not supported.
 * 
 * Soft hyphen (0x00ad), dash (0x002d) and space are used to
 * determine an appropriate break point. Soft hyphens are
 * removed in the result.
 * @author Joel Håkansson, TPB
 *
 */
public class BreakPointHandler {
	private final static char SOFT_HYPHEN = '\u00ad';
	private final static char ZERO_WIDTH_SPACE = '\u200b';
	private final static char DASH = '-';
	private final static char SPACE = ' ';
	private String charsStr;
	
	/**
	 * Create a new BreakPointHandler. All preferred break points 
	 * must be in supplied with the input String, represented by 
	 * hyphen 0x2d, soft hyphen 0xad or space 0x20.
	 * @param str the paragraph to break into rows. 
	 */
	public BreakPointHandler(String str) {
		if (str==null) {
			throw new NullPointerException("Input string cannot be null.");
		}
		this.charsStr = str;
	}

	/**
	 * Gets the next row from this BreakPointHandler
	 * @param breakPoint the desired breakpoint for this row
	 * @return returns the next BreakPoint
	 */
	public BreakPoint nextRow(int breakPoint, boolean force) {
		return doNextRow(breakPoint, force, false);
	}
	
	/**
	 * Tries to break the row at the breakpoint but does not modify the buffer.
	 * @param breakPoint
	 * @return
	 */
	public BreakPoint tryNextRow(int breakPoint) {
		return doNextRow(breakPoint, false, true);
	}
	
	private BreakPoint doNextRow(int breakPoint, boolean force, boolean test) {
		if (charsStr.length()==0) {
			// pretty simple...
			return new BreakPoint("", "", false);
		}
		String head;
		boolean hard = false;
		String tail;
		assert charsStr.length()==charsStr.codePointCount(0, charsStr.length());
		if (charsStr.length()<=breakPoint) {
			head = charsStr;
			tail = "";
		} else if (breakPoint<=0) {
			head = "";
			tail = charsStr;
		} else {
			int strPos = -1;
			int len = 0;
			for (char c : charsStr.toCharArray()) {
				strPos++;
				switch (c) {
					case SOFT_HYPHEN: case ZERO_WIDTH_SPACE:
						break;
					default:
						len++;
				}
				if (len>=breakPoint) {
					break;
				}
			}
			assert strPos<charsStr.length();
			
			int tailStart;
			
			/*if (strPos>=charsStr.length()-1) {
				head = charsStr.substring(0, strPos);
				System.out.println(head);
				tailStart = strPos;
			} else */
			// check next character to see if it can be removed.
			if (strPos==charsStr.length()-1) {
				head = charsStr.substring(0, strPos+1);
				tailStart = strPos+1;
			} else if (charsStr.charAt(strPos+1)==SPACE) {
				head = charsStr.substring(0, strPos+2); // strPos+1
				tailStart = strPos+2;
			} else { // back up
				int i=strPos;
whileLoop:		while (i>=0) {
					switch (charsStr.charAt(i)) {
						case SPACE: case DASH: case SOFT_HYPHEN: case ZERO_WIDTH_SPACE:
							break whileLoop;
					}
					i--;
				}
				if (i<0) { // no breakpoint found, break hard 
					if (force) {
						hard = true;
						head = charsStr.substring(0, strPos+1);
						tailStart = strPos+1;
					} else {
						head = "";
						tailStart = 0;
					}
				} else if (charsStr.charAt(i)==SPACE) { // don't ignore space at breakpoint
					head = charsStr.substring(0, i+1); //i
					tailStart = i+1;
				} else if (charsStr.charAt(i)==SOFT_HYPHEN) { // convert soft hyphen to hard hyphen 
					head = charsStr.substring(0, i) + DASH;
					tailStart = i+1;
				}  else if (charsStr.charAt(i)==ZERO_WIDTH_SPACE) { // ignore zero width space 
					head = charsStr.substring(0, i);
					tailStart = i+1;
				} else if (charsStr.charAt(i)==DASH && charsStr.length()>1 && charsStr.charAt(i-1)==SPACE) {
					// if hyphen is preceded by space, back up one more
					head = charsStr.substring(0, i);
					tailStart = i;
				} else {
					head = charsStr.substring(0, i+1);
					tailStart = i+1;
				}
			}
			if (charsStr.length()>tailStart) {
				tail = charsStr.substring(tailStart);
			} else {
				tail = "";
			}
			head = head.replaceAll("\\s+\\z", "");
		}
		assert (tail.length()<=charsStr.length());
		//trim leading whitespace in tail
		tail = tail.replaceAll("\\A\\s+", "");
		head = finalize(head);
		if (!test) {
			charsStr = tail;
		}
		
		return new BreakPoint(head, tail, hard);
	}

	public int countRemaining() {
		if (charsStr==null) {
			return 0;
		}
		return getRemaining().length();
	}
	
	public String getRemaining() {
		return finalize(charsStr);
	}
	
	private String finalize(String str) {
		StringBuilder sb = new StringBuilder();
		for (char c : str.toCharArray()) {
			switch (c) {
				case SOFT_HYPHEN: case ZERO_WIDTH_SPACE:
					// remove from output
					break;
				default:
					sb.append(c);
			}
		}
		return sb.toString();
		/*
		return str.replaceAll(""+SOFT_HYPHEN, "").replaceAll(""+ZERO_WIDTH_SPACE, "");*/
	}
	/**
	 * Does this BreakPointHandler has any text left to break into rows 
	 * @return returns true if this BreakPointHandler has any text left to break into rows
	 */
	public boolean hasNext() {
		return (charsStr!=null && charsStr.length()>0);
	}

}