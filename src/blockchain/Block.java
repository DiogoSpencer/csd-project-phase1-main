package blockchain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Block {
    
    private byte[] previousBlockHash;
    private int sequenceNumber;
    private List<byte[]> operations;
    private String replicaIdentity;
    private byte[] signature;



    public Block(byte[] previousBlockHash, int sequenceNumber, List<byte[]> operations,
                 String replicaIdentity, byte[] signature) {
        this.previousBlockHash = previousBlockHash;
        this.sequenceNumber = sequenceNumber;
        this.operations = operations;
        this.replicaIdentity = replicaIdentity;
        this.signature = signature;
    }

    public byte[] getPreviousBlockHash() {
        return previousBlockHash;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public List<byte[]> getOperations() {
        return operations;
    }

    public String getReplicaIdentity() {
        return replicaIdentity;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] toByteArray() {
        int size = 0;
        
        // Calculate the size of the byte array
        size += previousBlockHash.length;
        size += Integer.BYTES; // Size of the sequenceNumber (4 bytes)
        
        // Calculate the size of all operations combined
        for (byte[] operation : operations) {
            size += operation.length;
        }
        
        size += replicaIdentity.getBytes(StandardCharsets.UTF_8).length;
        size += signature.length;
        
        // Create a byte buffer with the calculated size
        ByteBuffer buffer = ByteBuffer.allocate(size);
        
        // Add each field to the byte buffer
        buffer.put(previousBlockHash);
        buffer.putInt(sequenceNumber);
        for (byte[] operation : operations) {
            buffer.put(operation);
        }
        buffer.put(replicaIdentity.getBytes(StandardCharsets.UTF_8));
        buffer.put(signature);
        
        // Convert the byte buffer to a byte array and return it
        return buffer.array();
    }


}
