package consensus.notifications;

import blockchain.Block;
import pt.unl.fct.di.novasys.babel.generic.ProtoNotification;

public class CommittedNotification extends ProtoNotification {

	public final static short NOTIFICATION_ID = 101;
	
	private final Block block;
	private final byte[] signature;
	
	public CommittedNotification(Block block, byte[] signature) {
		super(CommittedNotification.NOTIFICATION_ID);
		this.block = block;
		this.signature = signature;
	}

	public Block getBlock() {
		return block;
	}

	public byte[] getSignature() {
		return signature;
	}
	
	

}
