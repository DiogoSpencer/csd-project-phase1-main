package consensus;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import consensus.messages.PrePrepareMessage;
import consensus.notifications.CommittedNotification;
import consensus.notifications.ViewChange;
import consensus.requests.ProposeRequest;
import pt.unl.fct.di.novasys.babel.core.GenericProtocol;
import pt.unl.fct.di.novasys.babel.exceptions.HandlerRegistrationException;
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
	
	//TODO: add protocol state (related with the internal operation of the view)
	private Host self;
	private int viewNumber;
	private final List<Host> view;
	private int seq;
	
	
	public PBFTProtocol(Properties props) throws NumberFormatException, UnknownHostException {
		super(PBFTProtocol.PROTO_NAME, PBFTProtocol.PROTO_ID);
	
		this.seq = 0;

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
		
		// TODO: Must add handlers for requests and messages and register message serializers

		
        //registerMessageHandler(peerChannel, ProposeRequest.REQUEST_ID, null);

		registerRequestHandler(ProposeRequest.REQUEST_ID, this::handleProposeRequest);
		
		registerChannelEventHandler(peerChannel, InConnectionDown.EVENT_ID, this::uponInConnectionDown);
        registerChannelEventHandler(peerChannel, InConnectionUp.EVENT_ID, this::uponInConnectionUp);
        registerChannelEventHandler(peerChannel, OutConnectionDown.EVENT_ID, this::uponOutConnectionDown);
        registerChannelEventHandler(peerChannel, OutConnectionUp.EVENT_ID, this::uponOutConnectionUp);
        registerChannelEventHandler(peerChannel, OutConnectionFailed.EVENT_ID, this::uponOutConnectionFailed);
		
        
		logger.info("Standing by to extablish connections (10s)");
		
		try { Thread.sleep(10 * 1000); } catch (InterruptedException e) { }
		
		// TODO: Open connections to all nodes in the (initial) view 
	/* 	for (Host host : view) {
			openConnection(host);
		}
		*/
		
		//Installing first view
		triggerNotification(new ViewChange(view, viewNumber));
	}
	
	//TODO: Add event (messages, requests, timers, notifications) handlers of the protocol

	private void uponPropose(ProposeRequest msg, Host from){
		//sendMessage(msg, channel);

	}

	private void handleProposeRequest(ProposeRequest request, short from) {
         //teste
		logger.info("Teste: " + from);
		
		String identifier = String.valueOf(from);
		
		Enumeration<String> teste;
		try {
			teste = truststore.aliases();
			while(teste.hasMoreElements()){
				String element = teste.nextElement();
				logger.info(element);
			}
	
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		


		

		//Verify if the signature is 
	/* 	try {
			SignaturesHelper.checkSignature(request.getBlock(), request.getSignature(), truststore.getCertificate(identifier).getPublicKey());
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

		*/



		sendMessage(new PrePrepareMessage(request.getBlock(), request.getSignature()), self);
        
		//triggerNotification(new CommittedNotification(request.getBlock(),request.getSignature()));
		
    }
	
	/* --------------------------------------- Connection Manager Functions ----------------------------------- */
	
    private void uponOutConnectionUp(OutConnectionUp event, int channel) {
        logger.info(event);
		//sendMessage(new ProposeRequest(null, null), event.getNode());
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
