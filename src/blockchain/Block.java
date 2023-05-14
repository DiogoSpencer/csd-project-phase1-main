package blockchain;

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


}
