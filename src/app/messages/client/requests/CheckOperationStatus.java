package app.messages.client.requests;

import java.io.IOException;
import java.util.UUID;

import io.netty.buffer.ByteBuf;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.network.ISerializer;

public class CheckOperationStatus extends ProtoMessage {
	
	public final static short MESSAGE_ID = 308;
	
	private UUID rID;

	
	public CheckOperationStatus(UUID rID) {
		super(CheckOperationStatus.MESSAGE_ID);
		this.rID = rID;

	}
	
	public UUID getrID() {
		return rID;
	}

	public void setrID(UUID rID) {
		this.rID = rID;
	}


	public static ISerializer<CheckOperationStatus> serializer = new ISerializer<CheckOperationStatus>() {

		@Override
		public void serialize(CheckOperationStatus t, ByteBuf out) throws IOException {
			out.writeLong(t.rID.getMostSignificantBits());
			out.writeLong(t.rID.getLeastSignificantBits());
		}

		@Override
		public CheckOperationStatus deserialize(ByteBuf in) throws IOException {
			long msb = in.readLong();
			long lsb = in.readLong();
			
			return new CheckOperationStatus(new UUID(msb,lsb));
		
		}
	};

}
