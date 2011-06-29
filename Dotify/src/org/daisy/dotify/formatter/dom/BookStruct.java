package org.daisy.dotify.formatter.dom;


public interface BookStruct extends CrossReferences {

	/**
	 * Gets static or generated content to be placed first in a new volume
	 * @param volumeNumber the volume number to get content for
	 * @return returns the content
	 */
	public Iterable<PageSequence> getPreVolumeContents(int volumeNumber, VolumeStruct volumeData);
	
	/**
	 * Gets static or generated content to be placed last in a volume
	 * @param volumeNumber the volume number to get content for
	 * @return returns the content
	 */
	public Iterable<PageSequence> getPostVolumeContents(int volumeNumber, VolumeStruct volumeData);
	
	public PageStruct getPageStruct();
	/*
	/**
	 * Gets the breakpoints for volume breaks
	public SplitterData getBreakpoints();
	*/
	

}