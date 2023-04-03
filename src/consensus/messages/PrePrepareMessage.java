package consensus.messages;

import java.io.IOException;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedMessageSerializer;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;

public class PrePrepareMessage extends SignedProtoMessage {

	private final static short MESSAGE_ID = 101;
	
	//TODO: Define here the elements of the message
	private final byte[] block;
	private final byte[] signature;
	
	public PrePrepareMessage(byte[] block, byte[] signature) {
		super(PrePrepareMessage.MESSAGE_ID);
		this.block = block;
		this.signature = signature;
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
 
			
			return new PrePrepareMessage(block, signature);
		}
		
	};
	
	@Override
	public SignedMessageSerializer<? extends SignedProtoMessage> getSerializer() {
		return PrePrepareMessage.serializer;
	}

}
