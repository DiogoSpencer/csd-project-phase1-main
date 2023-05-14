package consensus.messages;

import java.io.IOException;

import blockchain.Block;
import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedMessageSerializer;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;

public class PrePrepareMessage extends SignedProtoMessage {

	public final static short MESSAGE_ID = 101;
	
	private String sender;
	private Block block;
	private byte[] blockSignature;
	private int viewNumber;
	private int seqNumber;
	
	public PrePrepareMessage(String sender, Block b, byte[] sig, int v, int n) {
		super(PrePrepareMessage.MESSAGE_ID);
		this.sender = sender;
		this.block = b;
		this.blockSignature = sig;
		this.viewNumber = v;
		this.seqNumber = n;	
	}

	public static SignedMessageSerializer<PrePrepareMessage> serializer = new SignedMessageSerializer<PrePrepareMessage>() {

		@Override
		public void serializeBody(PrePrepareMessage signedProtoMessage, ByteBuf out) throws IOException {
			out.writeShort(signedProtoMessage.sender.length());
			out.writeBytes(signedProtoMessage.sender.getBytes());
			byte[] blockBytes = signedProtoMessage.block.toByteArray();
			out.writeInt(blockBytes.length);
			out.writeBytes(blockBytes);
			out.writeShort(signedProtoMessage.blockSignature.length);
			out.writeBytes(signedProtoMessage.blockSignature);	
			out.writeInt(signedProtoMessage.viewNumber);
			out.writeInt(signedProtoMessage.seqNumber);
			

			
		}

		@Override
		public PrePrepareMessage deserializeBody(ByteBuf in) throws IOException {
			

			short s = in.readShort();
            byte[] c = new byte[s];
            in.readBytes(c);
            String sender = new String(c);
            int blockBytesLength = in.readInt();
            byte[] blockBytes = new byte[blockBytesLength];
            in.readBytes(blockBytes);
            int signatureLength = in.readShort();
            byte[] sig = new byte[signatureLength];
            in.readBytes(sig);
            int v = in.readInt();
            int n = in.readInt();

            // Create a new Block object from the deserialized byte array
            Block block = Block.fromByteArray(blockBytes);

            return new PrePrepareMessage(sender, block, sig, v, n);
		}
		
	};
	
	public String getSender() {
		return sender;
	}
	
	public Block getBlock() {
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
		return PrePrepareMessage.serializer;
	}

}
