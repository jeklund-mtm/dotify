package org.daisy.dotify.formatter.impl;

import org.daisy.dotify.api.formatter.SequenceProperties;

class StaticSequenceEventImpl extends FormatterCoreImpl implements VolumeSequence {
	private final SequenceProperties props;
	private BlockSequence ret;

	
	/**
	 * Creates a new sequence event
	 * @param props
	 */
	public StaticSequenceEventImpl(SequenceProperties props) {
		this.props = props;
		this.ret = null;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 4646831324973203983L;

	public SequenceProperties getSequenceProperties() {
		return props;
	}

	public BlockSequence getBlockSequence(FormatterContext context, DefaultContext c, CrossReferences crh) {
		if (ret!=null) {
			//we can return previous result, because static contents does not depend on context.
			return ret;
		} else {
			BlockSequenceManipulator fsm = new BlockSequenceManipulator(
					context.getMasters().get(getSequenceProperties().getMasterName()), 
					getSequenceProperties().getInitialPageNumber());
			fsm.appendGroup(this);
			ret = fsm.newSequence();
			return ret;
		}
	}

}
