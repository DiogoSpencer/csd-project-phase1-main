package blockchain;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

    public Block(byte[] previousBlockHash, int sequenceNumber, List<byte[]> operations,
            String replicaIdentity) {
        this.previousBlockHash = previousBlockHash;
        this.sequenceNumber = sequenceNumber;
        this.operations = operations;
        this.replicaIdentity = replicaIdentity;
        this.signature = null;
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

    public void setSignature(byte[] signature) {
        this.signature = signature;
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

        // Create a byte buffer with the calculated size
        ByteBuffer buffer = ByteBuffer.allocate(size);

        // Add each field to the byte buffer
        buffer.put(previousBlockHash);
        buffer.putInt(sequenceNumber);
        for (byte[] operation : operations) {
            buffer.put(operation);
        }
        buffer.put(replicaIdentity.getBytes(StandardCharsets.UTF_8));

        // Convert the byte buffer to a byte array and return it
        return buffer.array();
    }

    public static Block fromByteArray(byte[] byteArray) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);

        // Read the fields from the byte buffer
        byte[] previousBlockHash = new byte[32];
        buffer.get(previousBlockHash);
        int sequenceNumber = buffer.getInt();

        List<byte[]> operations = new ArrayList<>();
        while (buffer.hasRemaining()) {
            int operationLength = buffer.getInt();
            byte[] operation = new byte[operationLength];
            buffer.get(operation);
            operations.add(operation);
        }

        int replicaIdentityLength = byteArray.length - buffer.remaining();
        byte[] replicaIdentityBytes = new byte[replicaIdentityLength];
        buffer.get(replicaIdentityBytes);
        String replicaIdentity = new String(replicaIdentityBytes, StandardCharsets.UTF_8);

        // Create and return the Block object
        return new Block(previousBlockHash, sequenceNumber, operations, replicaIdentity);
    }

}
