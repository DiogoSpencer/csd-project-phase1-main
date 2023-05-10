package consensus.messages;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedMessageSerializer;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;


public class CommitMessage extends SignedProtoMessage{

    public final static short MESSAGE_ID = 103;	

	private String sender;
	private String blockSender;
	private byte[] block;
	private byte[] blockSignature;
	private int viewNumber;
	private int seqNumber;
	
	//TODO: Define here the elements of the message
	
	public CommitMessage(String sender, String blockSender, byte[] b, byte[] sig, int v, int n) {
		super(CommitMessage.MESSAGE_ID);
		this.sender = sender;
		this.blockSender = blockSender;
		this.block = b;
		this.blockSignature = sig;
		this.viewNumber = v;
		this.seqNumber = n;	
	}


	public static final SignedMessageSerializer<CommitMessage> serializer = new SignedMessageSerializer<CommitMessage>() {

		@Override
		public void serializeBody(CommitMessage signedProtoMessage, ByteBuf out) throws IOException {
			out.writeShort(signedProtoMessage.sender.length());
			out.writeBytes(signedProtoMessage.sender.getBytes());
			out.writeShort(signedProtoMessage.blockSender.length());
			out.writeBytes(signedProtoMessage.blockSender.getBytes());
			out.writeInt(signedProtoMessage.block.length);
			out.writeBytes(signedProtoMessage.block);
			out.writeShort(signedProtoMessage.blockSignature.length);
			out.writeBytes(signedProtoMessage.blockSignature);
			out.writeInt(signedProtoMessage.viewNumber);
			out.writeInt(signedProtoMessage.seqNumber);
		}

		@Override
		public CommitMessage deserializeBody(ByteBuf in) throws IOException {
			short s = in.readShort();
			byte[] c = new byte[s];
			in.readBytes(c);
			String sender = new String(c);
			s = in.readShort();
			c = new byte[s];
			in.readBytes(c);
			String blockSender = new String(c);
			int bsize = in.readInt();
			byte[] b = new byte[bsize];
			in.readBytes(b);
			s = in.readShort();
			byte[] sig = new byte[s];
			in.readBytes(sig);
			int v = in.readInt();
			int n = in.readInt();
			
			return new CommitMessage(sender, blockSender, b, sig, v, n);
		}
		
	};
	
	public String getSender() {
		return sender;
	}
	
	public String getBlockSender() {
		return blockSender;
	}
	public byte[] getBlock() {
		return block;
	}

	public byte[] getBlockSignature() {
		return blockSignature;
	}

	public int getViewNumber() {
		return viewNumber;
	}

	public int getSeqNumber() {
		return seqNumber;
	}
	
	@Override
	public SignedMessageSerializer<? extends SignedProtoMessage> getSerializer() {
		return CommitMessage.serializer;
	}

}
