package consensus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Map;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import consensus.messages.CommitMessage;
import consensus.messages.PrePrepareMessage;
import consensus.messages.PrepareMessage;
import consensus.notifications.CommittedNotification;
import consensus.notifications.ViewChange;
import consensus.requests.ProposeRequest;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled.*;
import javafx.util.Pair;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.generic.signed.NoSignaturePresentException;
import pt.unl.fct.di.novasys.babel.generic.signed.SignedProtoMessage;
import pt.unl.fct.di.novasys.babel.generic.signed.InvalidFormatException;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
import pt.unl.fct.di.novasys.babel.generic.signed.InvalidSerializerException;
import pt.unl.fct.di.novasys.babel.generic.ProtoMessage;
import pt.unl.fct.di.novasys.channel.tcp.MultithreadedTCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.TCPChannel;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.InConnectionUp;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionDown;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionFailed;
import pt.unl.fct.di.novasys.channel.tcp.events.OutConnectionUp;
import pt.unl.fct.di.novasys.network.data.Host;
import utils.Crypto;
import utils.SignaturesHelper;

public class PBFTProtocol extends GenericProtocol {

	public static final String PROTO_NAME = "pbft";
	public static final short PROTO_ID = 100;

	public static final String ADDRESS_KEY = "address";
	public static final String PORT_KEY = "base_port";
	public static final String INITIAL_MEMBERSHIP_KEY = "initial_membership";

	private static final Logger logger = LogManager.getLogger(PBFTProtocol.class);

	private String cryptoName;
	private KeyStore truststore;
	private PrivateKey key;

	// TODO: add protocol state (related with the internal operation of the view)
	private Host self;
	private int viewNumber;
	private final List<Host> view;

	private Map<byte[], Integer> prepareMap = new HashMap<>();
	private Map.Entry<byte[], Integer> popularPrepare;

	private Map<byte[], Integer> commitMap = new HashMap<>();
	private Map.Entry<byte[], Integer> popularCommit;

	private int seq;

	public PBFTProtocol(Properties props) throws NumberFormatException, UnknownHostException {
		super(PBFTProtocol.PROTO_NAME, PBFTProtocol.PROTO_ID);

		this.seq = 0;
		this.prepareMap = new HashMap<>();
		this.popularPrepare = new HashMap.SimpleEntry<>(new byte[0], 0);
		this.commitMap = new HashMap<>();
		this.popularCommit = new HashMap.SimpleEntry<>(new byte[0], 0);
		


		self = new Host(InetAddress.getByName(props.getProperty(ADDRESS_KEY)),
				Integer.parseInt(props.getProperty(PORT_KEY)));

		viewNumber = 1;
		view = new LinkedList<>();
		String[] membership = props.getProperty(INITIAL_MEMBERSHIP_KEY).split(",");
		for (String s : membership) {
			String[] tokens = s.split(":");
			view.add(new Host(InetAddress.getByName(tokens[0]), Integer.parseInt(tokens[1])));
		}
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

		// TODO: Must add handlers for requests and messages and register message
		// serializers

		// registerMessageHandler(peerChannel, ProposeRequest.REQUEST_ID, null);

		registerMessageHandler(peerChannel, PrePrepareMessage.MESSAGE_ID, this::handlePrePrepareMessage,
				this::handleMessageFailed);
		registerMessageSerializer(peerChannel, PrePrepareMessage.MESSAGE_ID, PrePrepareMessage.serializer);

		registerMessageHandler(peerChannel, PrepareMessage.MESSAGE_ID, this::handlePrepareMsg,
				this::handleMessageFailed);
		registerMessageSerializer(peerChannel, PrepareMessage.MESSAGE_ID, PrepareMessage.serializer);

		registerMessageHandler(peerChannel, CommitMessage.MESSAGE_ID, this::handleCommitMsg,
				this::handleMessageFailed);
		registerMessageSerializer(peerChannel, CommitMessage.MESSAGE_ID, CommitMessage.serializer);

		registerRequestHandler(ProposeRequest.REQUEST_ID, this::handleProposeRequest);

		registerChannelEventHandler(peerChannel, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
		registerChannelEventHandler(peerChannel, InConnectionUp.EVENT_ID, this::uponInConnectionUp);

		registerChannelEventHandler(peerChannel, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
		registerChannelEventHandler(peerChannel, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);

		registerChannelEventHandler(peerChannel, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);

		logger.info("Standing by to establish connections (10s)");
		

		try {
			Thread.sleep(10 * 1000);
		} catch (InterruptedException e) {
		}

		// TODO: Open connections to all nodes in the (initial) view

		// TODO usar este bloco ou o bloco seguinte?
		// for (Host host : view) {
		// //todo ignorar host se for a view selecionada
		// openConnection(host, peerChannel);
		// logger.info(String.format("Establishing connection to %s:%d",
		// host.getAddress(), host.getPort()));
		// }

		for (Host h : this.view) {
			logger.info("Connecting to " + h);
			openConnection(h);
		}

		// Installing first view
		triggerNotification(new ViewChange(view, viewNumber));
	}

	// TODO: Add event (messages, requests, timers, notifications) handlers of the
	// protocol

	

	private void handleProposeRequest(ProposeRequest request, short from) {
		int nodeId = Integer.parseInt(cryptoName.replace("node", ""));
		if (viewNumber == nodeId) {
			logger.info(String.format("Received a propose request with a block, on %s", cryptoName));
			try {
				// Verify if the signature of the block is valid
				if (SignaturesHelper.checkSignature(request.getBlock(), request.getSignature(),
						truststore.getCertificate(cryptoName).getPublicKey())) {
					PrePrepareMessage ppm = new PrePrepareMessage(cryptoName, request.getBlock(),
							request.getSignature(), viewNumber, ++seq);
					ppm.signMessage(key);

					logger.info("Block signature verified for local entity (" + this.cryptoName + ")");
					logger.info("PROPOSE REQUEST RECEIVES FOLLOWING BLOCK:");
					logger.warn(request.getBlock());
					
					// MessageDigest digest = MessageDigest.getInstance("SHA-256");
					// digest.digest(((byte[]) request));
					// byte[] proposeHash = Objects.hash(ppm);
					// ByteBuf out = new ByteBuf();
					// ByteBuf buf = ByteBuf.buffer(128);
					int ppmCount = 0;
					for (Host h : this.view) {
						if (!h.equals(self)) {

							logger.info("HANDLE PROPOSE REQUEST SENDS FOLLOWING BLOCK:");
							logger.info(ppm.getBlock());
							logger.info(String.format("PPMMMM SENT %d TIMES", ppmCount++));
							// logger.info(ppm.getSerializer().serializeBody(((SignedProtoMessage) ppm), buf));

							// ppm.getSerializer().serializeBody(ppm, out);
							sendMessage(ppm, h);

						}
					}

					// TODO is this signature generation necessary?
					// //Hashing the block
					// MessageDigest digest = MessageDigest.getInstance("SHA-256");
					// byte[] blockHash = digest.digest(request.getBlock());
					// byte[] signature = SignaturesHelper.generateSignature(blockHash, this.key);

					// sendMessage(new PrePrepareMessage(blockHash, signature, seq, viewNumber),
					// self);
				} else {
					logger.warn("Received ProposeRequest with invalid block signature.");
				}
			} catch (InvalidKeyException | SignatureException | NoSuchAlgorithmException | KeyStoreException
					| InvalidSerializerException e) {
				e.printStackTrace();
			}
		}

	}

	private void handlePrePrepareMessage(PrePrepareMessage msg, Host from, short sourceProto, int channel) {
		int seqN = msg.getSeqNumber();
		int viewN = msg.getViewNumber();
		logger.info("Received a PrePrepareMessage from " + msg.getSender() + "<" + from + ">  with view "
				+ " number " + msg.getViewNumber() + " and sequence number " + seqN);

		try {
			PublicKey senderPublicKey = truststore.getCertificate(msg.getSender()).getPublicKey();

			if (this.viewNumber == viewN) {
				if (this.seq < seqN) {
					if (msg.checkSignature(senderPublicKey)) {

						logger.info("Verified the message signature successfully for entity: " + msg.getSender());

						if (SignaturesHelper.checkSignature(msg.getBlock(), msg.getBlockSignature(), senderPublicKey)) {

							logger.info("Verified the block signature successfully for entity: " + msg.getSender());
							logger.info("PREPREPARE MESSAGE RECEIVES FOLLOWING BLOCK:");
							logger.warn(msg.getBlock());

							PrepareMessage pm = new PrepareMessage(cryptoName, msg.getSender(), msg.getBlock(),
									msg.getBlockSignature(), msg.getViewNumber(), msg.getSeqNumber());
							pm.signMessage(key);

							for (Host h : this.view) {
								if (!h.equals(self)) {
									sendMessage(pm, h);
								}
							}
							this.seq = msg.getSeqNumber();
						} else {
							logger.warn("Received PrePrepareMessage from " + msg.getSender() + "<" + from
									+ "> with invalid block signature");
						}
					} else {
						logger.warn("Reveived PrePrepareMessage from " + msg.getSender() + "<" + from
								+ "> with invalid message signature.");
					}
				} else {
					logger.warn("Received PrePrepareMessage from " + msg.getSender() + "<" + from
							+ "> with invalid Sequence Number");
				}
			} else {
				logger.warn("Received PrePrepareMessage from " + msg.getSender() + "<" + from
						+ "> with invalid View Number");
			}
		} catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidFormatException
				| NoSignaturePresentException | InvalidSerializerException | KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void handlePrepareMsg(PrepareMessage msg, Host from, short sourceProto, int channel) {
		int seqN = msg.getSeqNumber();
		int viewN = msg.getViewNumber();



		logger.info("Received a PrepareMessage from " + msg.getSender() + "<" + from + "> containing "
				+ "a block signed by " + msg.getBlockSender() + " with view number " + msg.getViewNumber()
				+ " and sequence number " + msg.getSeqNumber());



				try {
					PublicKey senderPublicKey = truststore.getCertificate(msg.getSender()).getPublicKey();
					PublicKey blockSenderPublicKey = truststore.getCertificate(msg.getBlockSender()).getPublicKey();
		
					if (this.viewNumber == viewN) {
						if (this.seq == seqN) {
							if(msg.checkSignature(senderPublicKey)) {

								logger.info("Verified the message signature successfully for entity: " + msg.getSender());
						
						if(SignaturesHelper.checkSignature(msg.getBlock(), msg.getBlockSignature(), blockSenderPublicKey )) {

							byte[] mapKey = msg.getBlockSignature();
							// TODO remove these logs after fixing issue
							// Commit messages are not being sent because BlockSignature is somehow always different
							logger.warn("BOOOOOOOOOOOOOOOLEAN");
							logger.warn(prepareMap.containsKey(mapKey));
							logger.warn(msg.getBlock());
							logger.warn(msg.getBlockSender());
							logger.warn(msg.getSender());
							if(prepareMap.containsKey(mapKey)) {
								int currValue = prepareMap.get(mapKey);
								prepareMap.put(mapKey, ++currValue);

								if(popularPrepare.getValue() < currValue) {
									popularPrepare = new HashMap.SimpleEntry<>(mapKey, currValue);
								}
							} else {
								prepareMap.put(mapKey, 1);
								if(popularPrepare.getValue() == 0) {
									popularPrepare = new HashMap.SimpleEntry<>(mapKey, 1);
								}
							}

							logger.info("Verified the block signature successfully for entity: " + msg.getSender());
							
							int necessaryPrepares = 2*(view.size()/3) + 1;
							int currPrepares = prepareMap.get(mapKey);


							if(currPrepares >= necessaryPrepares) { //if replica has received 2f+1 mathcing prepares 
								CommitMessage cm = new CommitMessage(cryptoName, msg.getSender(), msg.getBlock(), msg.getBlockSignature(), msg.getViewNumber(), msg.getSeqNumber());
								cm.signMessage(key);

								for(Host h: this.view) {
									if(!h.equals(self)) {
										sendMessage(cm, h);
									}
								}

								logger.info("Sent a commit message from node " + cryptoName + " prepareCounter: <" + currPrepares + ">");
							} 
						} else {
							logger.warn("Received PrepareMessage from " + msg.getSender() + "<" + from
									+ "> with invalid block signature");
						}
					} else {
						logger.warn("Reveived PrepareMessage from " + msg.getSender() + "<" + from
								+ "> with invalid message signature.");
					}
				} else {
					logger.warn("Received PrepareMessage from " + msg.getSender() + "<" + from
							+ "> with invalid Sequence Number");
					logger.warn(String.format("Current sequence number: %d, Received Sequence Number: %d", this.seq, seqN));
				}
			} else {
				logger.warn("Received PrepareMessage from " + msg.getSender() + "<" + from
						+ "> with invalid View Number");
			}
				}catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidFormatException
								| NoSignaturePresentException | InvalidSerializerException | KeyStoreException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} 	




	}



	private void handleCommitMsg(CommitMessage msg, Host from, short sourceProto, int channel) {

			logger.info("Received a CommitMessage from " + msg.getSender() + "<" + from + "> containing "
					+ "a block signed by " + msg.getBlockSender() + " with view number " + msg.getViewNumber()
					+ " and sequence number " + msg.getSeqNumber());




			int seqN = msg.getSeqNumber();
			int viewN = msg.getViewNumber();



			try {
				PublicKey senderPublicKey = truststore.getCertificate(msg.getSender()).getPublicKey();
				PublicKey blockSenderPublicKey = truststore.getCertificate(msg.getBlockSender()).getPublicKey();

				if (this.viewNumber == viewN) {
					if (this.seq == seqN) {
						if(msg.checkSignature(senderPublicKey)) {

							logger.info("Verified the message signature successfully for entity: " + msg.getSender());
					
					if(SignaturesHelper.checkSignature(msg.getBlock(), msg.getBlockSignature(), blockSenderPublicKey )) {


						byte[] mapKey = msg.getBlockSignature();


						if(commitMap.containsKey(mapKey)) {
							int currValue = commitMap.get(mapKey);
							commitMap.put(mapKey, ++currValue);

							if(popularCommit.getValue() < currValue) {
								popularCommit = new HashMap.SimpleEntry<>(mapKey, currValue);
							}
						} else {
							commitMap.put(mapKey, 1);
							if(popularCommit.getValue() == 0) {
								popularCommit = new HashMap.SimpleEntry<>(mapKey, 1);
							}
						}

						//logger.info("Verified the block signature successfully for entity: " + msg.getSender());

						int necessaryCommits = 2*(view.size()/3) + 1;
							int currCommits = commitMap.get(mapKey);


							if(currCommits >= necessaryCommits) { //if replica has received 2f+1 mathcing commits 

								//send reply to the client(blockchain protocol) that the operation has been executed
						        triggerNotification(new CommittedNotification(msg.getBlock(), msg.getBlockSignature()));

								//adicionar reset de maps e seq number e assim aqui acho eu

								logger.info("Sent a commit notification from node " + cryptoName + "to the client with commitCounter: <" + currCommits + ">");
							} 

                        
						 

						


					}else {
								logger.warn("Received CommitMessage from " + msg.getSender() + "<" + from
										+ "> with invalid block signature");
								}

							}else {
								logger.warn("Reveived CommitMessage from " + msg.getSender() + "<" + from
										+ "> with invalid message signature.");
							}
		
						}else {
							logger.warn("Received CommitMessage from " + msg.getSender() + "<" + from
									+ "> with invalid Sequence Number");
							logger.warn(String.format("Current sequence number: %d, Received Sequence Number: %d", this.seq, seqN));
						}
					}else {
						logger.warn("Received CommitMessage from " + msg.getSender() + "<" + from
								+ "> with invalid View Number");
					}
				}catch (InvalidKeyException | NoSuchAlgorithmException | SignatureException | InvalidFormatException
				| NoSignaturePresentException |KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	



		
	}


	private void handleMessageFailed(ProtoMessage msg, Host host, short i, Throwable throwable, int i1) {
		logger.warn("Failed: " + msg + ", to: " + host + ", reason: " + throwable.getMessage());
	}

	/*
	 * --------------------------------------- Connection Manager Functions
	 * -----------------------------------
	 */

	private void uponOutConnectionUp(OutConnectionUp event, int channel) {
		logger.info(event);

	}

	private void uponOutConnectionDown(OutConnectionDown event, int channel) {
		logger.warn(event);
	}

	private void uponOutConnectionFailed(OutConnectionFailed<ProtoMessage> ev, int ch) {
		logger.warn(ev);
		openConnection(ev.getNode());
	}

	private void uponInConnectionUp(InConnectionUp event, int channel) {
		logger.info(event);
	}

	private void uponInConnectionDown(InConnectionDown event, int channel) {
		logger.warn(event);
	}

}
