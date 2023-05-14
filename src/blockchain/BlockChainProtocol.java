package blockchain;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import blockchain.messages.RedirectClientRequestMessage;
import blockchain.requests.ClientRequest;
import blockchain.timers.CheckUnhandledRequestsPeriodicTimer;
import blockchain.timers.LeaderSuspectTimer;
import consensus.PBFTProtocol;
import consensus.notifications.CommittedNotification;
import consensus.notifications.ViewChange;
import consensus.requests.ProposeRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.MultithreadedTCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.Crypto;
import utils.SignaturesHelper;

public class BlockChainProtocol extends GenericProtocol {

	private static final String PROTO_NAME = "blockchain";
	private static final short PROTO_ID = 200;

	public static final String ADDRESS_KEY = "address";
	public static final String PORT_KEY = "base_port";
	public static final String INITIAL_MEMBERSHIP_KEY = "initial_membership";

	public static final String PERIOD_CHECK_REQUESTS = "check_requests_timeout";
	public static final String SUSPECT_LEADER_TIMEOUT = "leader_timeout";

	private static final Logger logger = LogManager.getLogger(BlockChainProtocol.class);

	private String cryptoName;
	private KeyStore truststore;
	private PrivateKey key;

	private Map<byte[], Integer> commitNotificationMap = new HashMap<>();
	private Map.Entry<byte[], Integer> popularcommitNotification;

	private final long checkRequestsPeriod;
	private final long leaderTimeout;

	private Host self;
	private int viewNumber;
	private final List<Host> view;
	private boolean leader;
	private List<Block> blockchainList;
	private List<ClientRequest> unhandledRequests;

	public BlockChainProtocol(Properties props) throws NumberFormatException, UnknownHostException {
		super(BlockChainProtocol.PROTO_NAME, BlockChainProtocol.PROTO_ID);

		// Probably the following informations could be provided by a notification
		// emitted by the PBFTProtocol
		// (this should not be interpreted as the unique or canonical solution)
		self = new Host(InetAddress.getByName(props.getProperty(ADDRESS_KEY)),
				Integer.parseInt(props.getProperty(PORT_KEY)));

		viewNumber = 0;
		view = new LinkedList<>();
		this.commitNotificationMap = new HashMap<>();
		this.popularcommitNotification = new HashMap.SimpleEntry<>(new byte[0], 0);
		unhandledRequests = new ArrayList<ClientRequest>();

		//generate blockhain
		blockchainList = new ArrayList<Block>();

		//creation of genesis block(this can be read from a file as a parameter of the protocol in the future)
		List<byte[]> operations = new ArrayList<>();
		operations.add(new byte[] { 0x01, 0x02, 0x03}); //Genesis block has this hard coded operation
		byte[] previousBlockHash = new byte[] { 0x12, 0x34, 0x56, 0x78 }; // Genesis block hard coded previousHash
        int blockNumber = 0; // Genesis block has block number 0
        String replicaIdentity = "Genesis"; 
        byte[] signature = new byte[] { 0x12, 0x34, 0x56 }; // Genesis block hard coded signature

	    Block genesisBlock = new Block(previousBlockHash, blockNumber , operations, replicaIdentity , signature);

		blockchainList.add(genesisBlock); //adding the genesis block as the first element of the blockchain


		// Read timers and timeouts configurations
		checkRequestsPeriod = Long.parseLong(props.getProperty(PERIOD_CHECK_REQUESTS));
		leaderTimeout = Long.parseLong(props.getProperty(SUSPECT_LEADER_TIMEOUT));
	}

	@Override
	public void init(Properties props) throws HandlerRegistrationException, IOException {
		try {
			cryptoName = props.getProperty(Crypto.CRYPTO_NAME_KEY);
			truststore = Crypto.getTruststore(props);
			key = Crypto.getPrivateKey(cryptoName, props);
		} catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException | CertificateException
				| IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		Properties peerProps = new Properties();
		peerProps.put(MultithreadedTCPChannel.ADDRESS_KEY, props.getProperty(ADDRESS_KEY));
		peerProps.setProperty(TCPChannel.PORT_KEY, props.getProperty(PORT_KEY));
		int peerChannel = createChannel(TCPChannel.NAME, peerProps);

        


		





		registerRequestHandler(ClientRequest.REQUEST_ID, this::handleClientRequest);

		registerMessageHandler(peerChannel, RedirectClientRequestMessage.MESSAGE_ID, this::handleRedirectClientRequestMessage);
		registerMessageSerializer(peerChannel, RedirectClientRequestMessage.MESSAGE_ID, RedirectClientRequestMessage.serializer);

		registerTimerHandler(CheckUnhandledRequestsPeriodicTimer.TIMER_ID,
				this::handleCheckUnhandledRequestsPeriodicTimer);
		registerTimerHandler(LeaderSuspectTimer.TIMER_ID, this::handleLeaderSuspectTimer);

		subscribeNotification(ViewChange.NOTIFICATION_ID, this::handleViewChangeNotification);
		subscribeNotification(CommittedNotification.NOTIFICATION_ID, this::handleCommittedNotification);

		setupPeriodicTimer(new CheckUnhandledRequestsPeriodicTimer(), checkRequestsPeriod, checkRequestsPeriod);
	}

	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	/*
	 * ---------------------------------------------- REQUEST HANDLER
	 * -----------------------------------------
	 */
	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */

	public void handleClientRequest(ClientRequest req, short protoID) {
		logger.info("Received a ClientRequest with id: " + req.getRequestId());

		if (this.leader) {

			try {
				// TODO: This is a super over simplification we will handle latter
				// Only one block should be submitted for agreement at a time
				// Also this assumes that a block only contains a single client request
				byte[] request = req.generateByteRepresentation();
				

				

                int lastBlockPosition = blockchainList.size()-1;
				Block lastBlock = blockchainList.get(lastBlockPosition);

				byte[] hashOfPrevious = calculateHash(lastBlock);
				List<byte[]> operationsProposed = new ArrayList<byte[]>();
				operationsProposed.add(request);



				Block blockToPurpose = new Block(hashOfPrevious, lastBlock.getSequenceNumber()+1, operationsProposed, cryptoName);

				byte[] signature = SignaturesHelper.generateSignature(blockToPurpose.toByteArray(), this.key); //sign the block

				blockToPurpose.setSignature(signature); //add signature to the block

				
				

				sendRequest(new ProposeRequest(blockToPurpose, signature), PBFTProtocol.PROTO_ID);

				//log the request in the unhandle request log
				unhandledRequests.add(req);

			} catch (Exception e) {
				e.printStackTrace();
				System.exit(1); // Catastrophic failure!!!
			}
		} else {
			// Redirect this request to the leader via a specialized message

			int remainderofView = viewNumber % view.size();
			int currentPrimary;

			if (remainderofView == 0) {
				currentPrimary = view.size();
			} else {
				currentPrimary = remainderofView;
			}	

			Host leaderHost = view.get(currentPrimary-1); //getting the leader


			logger.info("Redirecting client request to leader");

			sendMessage(new RedirectClientRequestMessage(req.getOperation()), leaderHost);

		}


			    
			
	}

	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	/*
	 * ------------------------------------------- NOTIFICATION HANDLER
	 * ---------------------------------------
	 */
	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */

	public void handleViewChangeNotification(ViewChange vc, short from) {

		logger.info("New view received (" + vc.getViewNumber() + ")");

		// TODO: Should maybe validate this ViewChange :) (check if we received 2f+1 viewChange notifications )

		this.viewNumber = vc.getViewNumber();
		this.view.clear();
		for (int i = 0; i < vc.getView().size(); i++) {
			this.view.add(vc.getView().get(i));
		}
		// TODO: Compute correctly who is the leader and not assume that you are always
		// the leader.
		int nodeId = Integer.parseInt(cryptoName.replace("node", ""));
		int remainderofView = viewNumber % view.size();
		int currentPrimary;

		if (remainderofView == 0) {
			currentPrimary = view.size();
		} else {
			currentPrimary = remainderofView;
		}

		if (nodeId == currentPrimary) {
			this.leader = true;
		} else {
			this.leader = false;
		}

	}

	public void handleCommittedNotification(CommittedNotification cn, short from) {
		// TODO: write this handler

		

			// Verify if the signature of the block is valid
			try {
				if (SignaturesHelper.checkSignature(cn.getBlock().toByteArray(), cn.getSignature(),
						truststore.getCertificate(cn.getBlock().getReplicaIdentity()).getPublicKey())) {

					byte[] mapKey = cn.getSignature();

					if (commitNotificationMap.containsKey(mapKey)) {
						int currValue = commitNotificationMap.get(mapKey);
						commitNotificationMap.put(mapKey, ++currValue);

						if (popularcommitNotification.getValue() < currValue) {
							popularcommitNotification = new HashMap.SimpleEntry<>(mapKey, currValue);
						}
					} else {
						commitNotificationMap.put(mapKey, 1);
						if (popularcommitNotification.getValue() == 0) {
							popularcommitNotification = new HashMap.SimpleEntry<>(mapKey, 1);
						}
					}

					int necessaryCommitNotifications = (view.size() / 3) + 1;
					int currCommitNotifications = commitNotificationMap.get(mapKey);

					logger.info("Reply of block decision received: " + cn.getBlock().getSignature() + " counter: "
							+ currCommitNotifications);

					if (currCommitNotifications >= necessaryCommitNotifications) { // if replica has received f+1 mathcing commit notifications
																					


                        //TODO Remove the request from unhadled requests
						Block blockToAdd = cn.getBlock();
                        
                        for(ClientRequest currentRequest : unhandledRequests){ //if the request was in this block remove that request from the unhadleRequests log
							if(currentRequest.getOperation() == blockToAdd.getOperations().get(0))
							     unhandledRequests.remove(currentRequest);
						}
						
						logger.info("Request of block added with sequence number: " + blockToAdd.getSequenceNumber());

						// add block to the blockchain
						blockchainList.add(blockToAdd);
						logger.info("Block added with sequence number: " + blockToAdd.getSequenceNumber());
					}

				} else {
					logger.warn("Received Commit Notification with invalid block signature.");
				}
			} catch (InvalidKeyException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SignatureException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (KeyStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		
	}

	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	/*
	 * ---------------------------------------------- MESSAGE HANDLER
	 * -----------------------------------------
	 */
	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */

	// TODO: add message handlers (and register them)


	public void handleRedirectClientRequestMessage(RedirectClientRequestMessage msg, Host from, short sourceProto, int channel){

		sendRequest(new ClientRequest(msg.getOperation()), BlockChainProtocol.PROTO_ID);
         
	}

	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	/*
	 * ----------------------------------------------- TIMER HANDLER
	 * ------------------------------------------
	 */
	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */

	public void handleCheckUnhandledRequestsPeriodicTimer(CheckUnhandledRequestsPeriodicTimer t, long timerId) {
		// TODO: write this handler

		logger.info("Checking unhandled requests...");

		// Iterate through the unhandled requests and check their status
		for (ClientRequest request : unhandledRequests) {
			// Check if the request has exceeded the timeout period
			if (System.currentTimeMillis() - request.getTimeStamp() >= checkRequestsPeriod) {
				logger.warn("Request " + request.getRequestId() + " has timed out.");

				// Send messages to all the replicas (start suspect)
				//sendMessage() client request unhandled

				
                
			

				break; // No need to continue checking other requests
			}
		}
	}

	public void handleLeaderSuspectTimer(LeaderSuspectTimer t, long timerId) {
		// TODO: write this handler

		logger.info("Leader suspect timer triggered.");

		// Check if this replica is the leader
		if (leader) {
			logger.warn("Leader suspect timer triggered for the current leader.");

			// TODO: Take appropriate action as the leader initiate view change by sending message to all replicas

		} else {
			logger.info("Leader suspect timer triggered for a non-leader replica.");

			// TODO: Take appropriate action as a non-leader replica (e.g., start an
			// election process)
		}
	}

	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	/*
	 * ----------------------------------------------- APP INTERFACE
	 * ------------------------------------------
	 */
	/*
	 * ----------------------------------------------- -------------
	 * ------------------------------------------
	 */
	public void submitClientOperation(byte[] b) {
		sendRequest(new ClientRequest(b), BlockChainProtocol.PROTO_ID);
	}

	public static byte[] calculateHash(Block block) { //function to hash a block
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] blockBytes = block.toByteArray();
            return digest.digest(blockBytes);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
		return null;
        
    }

}
