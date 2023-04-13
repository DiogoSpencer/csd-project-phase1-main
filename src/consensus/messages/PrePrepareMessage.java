package consensus.messages;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedMessageSerializer;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;

public class PrePrepareMessage extends SignedProtoMessage {

	public final static short MESSAGE_ID = 101;
	
	//TODO: Define here the elements of the message
	private final byte[] block;
	private final byte[] signature;
	private final int seqN;
	private final int viewN;
	
	public PrePrepareMessage(byte[] block, byte[] signature, int seqN, int viewN) {
		super(PrePrepareMessage.MESSAGE_ID);
		this.block = block;
		this.signature = signature;
		this.seqN = seqN;
		this.viewN = viewN;
	}

	public static SignedMessageSerializer<PrePrepareMessage> serializer = new SignedMessageSerializer<PrePrepareMessage>() {

		@Override
		public void serializeBody(PrePrepareMessage signedProtoMessage, ByteBuf out) throws IOException {
			// TODO Auto-generated method stub, you should implement this method.
			// Write the length of the block array as a 4-byte integer
			out.writeInt(signedProtoMessage.block.length);

			// Write the block array
			out.writeBytes(signedProtoMessage.block);
	
			// Write the length of the signature array as a 4-byte integer
			out.writeInt(signedProtoMessage.signature.length);
	
			// Write the signature array
			out.writeBytes(signedProtoMessage.signature);

			out.writeInt(signedProtoMessage.seqN);

			out.writeInt(signedProtoMessage.viewN);

		   
			
		}

		@Override
		public PrePrepareMessage deserializeBody(ByteBuf in) throws IOException {
			// TODO Auto-generated method stub, you should implement this method.

            // Read the length of the block array as a 4-byte integer
			int blockLength = in.readInt();

			// Read the block array
			byte[] block = new byte[blockLength];
			in.readBytes(block);
	
			// Read the length of the signature array as a 4-byte integer
			int signatureLength = in.readInt();
	
			// Read the signature array
			byte[] signature = new byte[signatureLength];
			in.readBytes(signature);

			int seqN = in.readInt();
			int viewN = in.readInt();
 
			
			return new PrePrepareMessage(block, signature, seqN, viewN);
		}
		
	};
	
	@Override
	public SignedMessageSerializer<? extends SignedProtoMessage> getSerializer() {
		return PrePrepareMessage.serializer;
	}

}
