package consensus.messages;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedMessageSerializer;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;

public class PrePrepareMessage extends SignedProtoMessage {

	public final static short MESSAGE_ID = 101;
	
	private String sender;
	private byte[] block;
	private byte[] blockSignature;
	private int viewNumber;
	private int seqNumber;
	
	public PrePrepareMessage(String sender, byte[] b, byte[] sig, int v, int n) {
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
			System.out.println(signedProtoMessage.sender.length());
			out.writeBytes(signedProtoMessage.sender.getBytes());
			System.out.println(signedProtoMessage.sender.getBytes());
			out.writeInt(signedProtoMessage.block.length);
			System.out.println(signedProtoMessage.block.length);
			out.writeBytes(signedProtoMessage.block);
			System.out.println(signedProtoMessage.block);
			out.writeShort(signedProtoMessage.blockSignature.length);
			System.out.println(signedProtoMessage.blockSignature.length);
			out.writeBytes(signedProtoMessage.blockSignature);	
			System.out.println(signedProtoMessage.blockSignature);
			out.writeInt(signedProtoMessage.viewNumber);
			System.out.println(signedProtoMessage.viewNumber);
			out.writeInt(signedProtoMessage.seqNumber);
			System.out.println(signedProtoMessage.seqNumber);

			System.out.println("SERIALIZING PPM");
			System.out.println(out.array());
		}

		@Override
		public PrePrepareMessage deserializeBody(ByteBuf in) throws IOException {
			System.out.println("DESERIALIZING PPM");
			System.out.println(in.array());

			short s = in.readShort();
			System.out.println(s);
			byte[] c = new byte[s];
			in.readBytes(c);
			System.out.println(c);
			String sender = new String(c);
			int bsize = in.readInt();
			System.out.println(bsize);
			byte[] b = new byte[bsize];
			in.readBytes(b);
			System.out.println(b);
			s = in.readShort();
			System.out.println(s);
			byte[] sig = new byte[s];
			in.readBytes(sig);
			System.out.println(sig);
			int v = in.readInt();
			System.out.println(v);
			int n = in.readInt();
			System.out.println(n);
			
			return new PrePrepareMessage(sender, b, sig, v, n);
		}
		
	};
	
	public String getSender() {
		return sender;
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
		return PrePrepareMessage.serializer;
	}

}
