package org.daisy.dotify.formatter.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.daisy.dotify.api.formatter.FallbackRule;
import org.daisy.dotify.api.formatter.PageAreaProperties;
import org.daisy.dotify.api.formatter.RenameFallbackRule;

class PageSequenceBuilder extends PageSequence {
	private final Map<String, PageImpl> pageReferences;
	private final FormatterContext context;
	private final BlockSequence seq;
	private final CrossReferenceHandler crh;
	private int pagesOffset;
	private int keepNextSheets;
	private PageImpl nextPage;
	private List<RowImpl> before;
	private List<RowImpl> after;

	PageSequenceBuilder(CrossReferenceHandler crh, BlockSequence seq, Map<String, PageImpl> pageReferences, FormatterContext context) {
		super(seq.getLayoutMaster());
		this.pageReferences = pageReferences;
		this.context = context;
		this.seq = seq;
		this.crh = crh;
		reset();
	}
	
	private void reset() {
		this.keepNextSheets = 0;
		this.nextPage = null;
		this.before = new ArrayList<RowImpl>();
		this.after = new ArrayList<RowImpl>();
	}

	private List<RowImpl> getBefore() {
		return before;
	}

	private List<RowImpl> getAfter() {
		return after;
	}

	private void newPage() {
		if (nextPage!=null) {
			pages.push(nextPage);
			nextPage = null;
		} else {
			pages.push(new PageImpl(master, context, this, pages.size()+pagesOffset, before, after));
		}
		if (keepNextSheets>0) {
			currentPage().setAllowsVolumeBreak(false);
		}
		if (!getLayoutMaster().duplex() || getPageCount()%2==0) {
			if (keepNextSheets>0) {
				keepNextSheets--;
			}
		}
	}

	private void newPageOnRow() {
		if (nextPage!=null) {
			//if new page is already in buffer, flush it.
			newPage();
		}
		nextPage = new PageImpl(master, context, this, pages.size()+pagesOffset, before, after);
	}

	private void setKeepWithPreviousSheets(int value) {
		currentPage().setKeepWithPreviousSheets(value);
	}

	private void setKeepWithNextSheets(int value) {
		keepNextSheets = Math.max(value, keepNextSheets);
		if (keepNextSheets>0) {
			currentPage().setAllowsVolumeBreak(false);
		}
	}

	public int getPageNumberOffset() {
		return pagesOffset;
	}

	
	private PageImpl currentPage() {
		if (nextPage!=null) {
			return nextPage;
		} else {
			return pages.peek();
		}
	}
	
	int currentPageNumber() {
		return currentPage().getPageIndex()+1;
	}

	/**
	 * Space used, in rows
	 * 
	 * @return
	 */
	private int spaceUsedOnPage(int offs) {
		return currentPage().spaceUsedOnPage(offs);
	}

	private void newRow(RowImpl row, List<RowImpl> block) {
		if (nextPage != null || currentPage().spaceNeeded(block) + currentPage().spaceNeeded() + 1 > currentPage().getFlowHeight()) {
			newPage();
		}
		currentPage().newRow(row);
		currentPage().addToPageArea(block);
	}

	private void newRow(RowImpl row) {
		if (spaceUsedOnPage(1) > currentPage().getFlowHeight() || nextPage != null) {
			newPage();
		}
		currentPage().newRow(row);
	}

	private void insertIdentifier(String id) {
		crh.setPageNumber(id, currentPage().getPageIndex() + 1);
		currentPage().addIdentifier(id);
		if (pageReferences.put(id, currentPage())!=null) {
			throw new IllegalArgumentException("Identifier not unique: " + id);
		}
	}

	boolean paginate(int pagesOffset, CrossReferences refs, DefaultContext rcontext) throws PaginatorException  {
		if (seq.getInitialPageNumber()!=null) {
			this.pagesOffset = seq.getInitialPageNumber() - 1;
		} else {
			this.pagesOffset = pagesOffset;
		}
		newPage();

		ContentCollectionImpl c = null;
		PageAreaProperties pa = seq.getLayoutMaster().getPageArea();
		if (pa!=null) {
			c = context.getCollections().get(pa.getCollectionId());
		}
		PageAreaBuilderImpl pab = seq.getLayoutMaster().getPageAreaBuilder();
		if (pab !=null) {
			//Assumes before is static
			for (Block b : pab.getBeforeArea()) {
				b.setContext(seq.getLayoutMaster().getFlowWidth(), refs, rcontext, context);
				for (RowImpl r : b.getBlockContentManager()) {
					getBefore().add(r);
				}
			}

			//Assumes after is static
			for (Block b : pab.getAfterArea()) {
				b.setContext(seq.getLayoutMaster().getFlowWidth(), refs, rcontext, context);
				for (RowImpl r : b.getBlockContentManager()) {
					getAfter().add(r);
				}
			}
		}


		//update context
		for (Block g : seq) {
			g.setContext(seq.getLayoutMaster().getFlowWidth(), refs, rcontext, context);
		}

		//layout
		for (Block g : seq) {
			BlockDataContext bd = new BlockDataContext(g);
			//FIXME: this assumes that row spacing is equal to 1
			if (bd.rdm.countPreContentRows()+bd.rdm.countPostContentRows()>=currentPage().getFlowHeight()) {
				throw new PaginatorException("Group margins too large to fit on an empty page.");
			}

			//Start new page if needed
			bd.startNewPageIfNeeded(seq);
			bd.addVerticalSpace();

			addRows(bd.rdm.getPreContentRows());

			currentPage().addMarkers(bd.rdm.getGroupMarkers());
			if (bd.rdm.getRowCount()==0 && !"".equals(g.getIdentifier())) {
				insertIdentifier(g.getIdentifier());
			}

			setKeepWithNextSheets(g.getKeepWithNextSheets());
			if (!bd.addRows(seq.getLayoutMaster(), refs, rcontext, c)) {
				//reassign collection
				if (pa!=null) {
					int i = 0;
					for (FallbackRule r : pa.getFallbackRules()) {
						i++;
						if (r instanceof RenameFallbackRule) {
							c = context.getCollections().remove(r.applyToCollection());
							if (context.getCollections().put(((RenameFallbackRule)r).getToCollection(), c)!=null) {
								throw new PaginatorException("Fallback id already in use:" + ((RenameFallbackRule)r).getToCollection());
							}							
						} else {
							throw new PaginatorException("Unknown fallback rule: " + r);
						}
					}
					if (i==0) {
						throw new PaginatorException("Failed to fit collection '" + pa.getCollectionId() + "' within the page-area boundaries, and no fallback was defined.");
					}

				}
				//restart formatting
				return false;
			}
			setKeepWithPreviousSheets(g.getKeepWithPreviousSheets());

			addRows(bd.rdm.getPostContentRows());

			//FIXME: this assumes that row spacing is equal to 1
			if (bd.rdm.countSkippablePostContentRows() > currentPage().getFlowHeight() - spaceUsedOnPage(1)) {
				newPageOnRow();
			} else {
				addRows(bd.rdm.getSkippablePostContentRows());
			}
			//gi++;
		}
		return true;
	}

	private void addRows(Iterable<RowImpl> rows) {
		for (RowImpl r : rows) {
			newRow(r);
		}
	}

	/**
	 * Provides the block in the context of the supplied parameters.
	 */
	private class BlockDataContext {
		private final Block block;
		private final BlockContentManager rdm;

		private BlockDataContext(Block block) {
			this.block = block;
			this.rdm = block.getBlockContentManager();
		}

		private void addVerticalSpace() {
			if (block.getVerticalPosition() != null) {			
				int blockSpace = rdm.getRowCount() + block.getSpaceBefore() + block.getSpaceAfter();
				int pos = block.getVerticalPosition().getPosition().makeAbsolute(currentPage().getFlowHeight());
				int t = pos - spaceUsedOnPage(0);
				if (t > 0) {
					int advance = 0;
					switch (block.getVerticalPosition().getAlignment()) {
					case BEFORE:
						advance = t - blockSpace;
						break;
					case CENTER:
						advance = t - blockSpace / 2;
						break;
					case AFTER:
						advance = t;
						break;
					}
					for (int i = 0; i < Math.floor(advance / getLayoutMaster().getRowSpacing()); i++) {
						newRow(new RowImpl("", rdm.getLeftMarginParent(), rdm.getRightMarginParent()));
					}
				}
			}
		}

		private void startNewPageIfNeeded(BlockSequence seq) {
			boolean hasContent = spaceUsedOnPage(0) > 0;
			switch (block.getBreakBeforeType()) {
			case PAGE:
				if (hasContent) {
					newPage();
				}
				break;
			case AUTO:default:;
			}
			switch (block.getKeepType()) {
			case ALL:
				int keepHeight = seq.getKeepHeight(block);
				//FIXME: this assumes that row spacing is equal to 1
				if (hasContent && keepHeight > currentPage().getFlowHeight() - spaceUsedOnPage(0) && keepHeight <= currentPage().getFlowHeight()) {
					newPage();
				}
				break;
			case AUTO:
				break;
			default:;
			}
			//FIXME: this assumes that row spacing is equal to 1
			if (block.getSpaceBefore() > currentPage().getFlowHeight() - spaceUsedOnPage(1)) {
				newPageOnRow();
			}
		}


		private boolean addRows(LayoutMaster master, CrossReferences refs, DefaultContext rcontext, ContentCollectionImpl c) {
			boolean first = true;
			for (RowImpl row : rdm) {
				if (master.getPageArea()!=null && c!=null) {
					ArrayList<RowImpl> blk = new ArrayList<RowImpl>();
					for (String a : row.getAnchors()) {
						if (!currentPage().getAnchors().contains(a)) {
							//page doesn't already contains these blocks
							for (Block b : c.getBlocks(a)) {
								b.setContext(master.getFlowWidth(), refs, rcontext, context);
							}
							for (Block b : c.getBlocks(a)) {
								for (RowImpl r : b.getBlockContentManager().getPreContentRows()) {
									blk.add(r);
								}
								for (RowImpl r : b.getBlockContentManager()) {
									blk.add(r);
								}
								for (RowImpl r : b.getBlockContentManager().getPostContentRows()) {
									blk.add(r);
								}
								for (RowImpl r : b.getBlockContentManager().getSkippablePostContentRows()) {
									blk.add(r);
								}
							}
						}
					}
					if (blk.size()>0) {
						newRow(row, blk);
						//The text volume is reduced if row spacing increased
						if (currentPage().pageAreaSpaceNeeded() > master.getPageArea().getMaxHeight()) {
							return false;
						}
					} else {
						newRow(row);
					}
				} else {
					newRow(row);
				}
				if (first) {
					first = false;
					if (!"".equals(block.getIdentifier())) {
						insertIdentifier(block.getIdentifier());
					}
				}
			}
			return true;
		}
	}
}
