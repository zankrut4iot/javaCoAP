package org.ws4d.coap.handsOn.multicastDiscovery;

import java.net.InetAddress;

import org.ws4d.coap.core.CoapClient;
import org.ws4d.coap.core.CoapConstants;
import org.ws4d.coap.core.connection.BasicCoapChannelManager;
import org.ws4d.coap.core.connection.api.CoapChannelManager;
import org.ws4d.coap.core.connection.api.CoapClientChannel;
import org.ws4d.coap.core.enumerations.CoapRequestCode;
import org.ws4d.coap.core.messages.api.CoapRequest;
import org.ws4d.coap.core.messages.api.CoapResponse;

public class MulticastDiscoveryClient implements CoapClient {

	private CoapChannelManager channelManager;
	private CoapClientChannel clientChannel;
	
	public static void main(String[] args) {
		MulticastDiscoveryClient coapClient = new MulticastDiscoveryClient();
		System.out.println("=== START Multicast Discovery Client ===");
		
		/* *************************************************************************************************** */
		/* Use multicast address                                                                               */
		/* *************************************************************************************************** */
		// coapClient.start(CoapConstants.COAP_ALL_NODES_IPV4_MC_ADDR, CoapConstants.COAP_DEFAULT_PORT);
		 coapClient.start(CoapConstants.COAP_ALL_NODES_IPV6_LL_MC_ADDR, CoapConstants.COAP_DEFAULT_PORT);
		// coapClient.start(CoapConstants.COAP_ALL_NODES_IPV6_SL_MC_ADDR, CoapConstants.COAP_DEFAULT_PORT);
	}

	public void start(String serverAddress, int serverPort) {
		this.channelManager = BasicCoapChannelManager.getInstance();
		this.clientChannel = null;
		try {
			this.clientChannel = this.channelManager.connect(this, InetAddress.getByName(serverAddress), serverPort);
			if (this.clientChannel == null) {
				System.err.println("Connect failed: clientChannel in null!");
				System.exit(-1);
			}
		} catch (Exception e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(-1);
		}


		/* *************************************************************************************************** */
		/* Create request to /.well-known/core resource                                                        */
		/* *************************************************************************************************** */
		
		// set reliability and request method
		CoapRequest request = this.clientChannel.createRequest(false, CoapRequestCode.GET);
		
		// set resource path
		request.setUriPath("/.well-known/core");
		
		// add a token to match outgoing multicast message and incoming unicast messages
		request.setToken("MCToken".getBytes());
		
		// send the request
		this.clientChannel.sendMessage(request);
	}

	/* *************************************************************************************************** */
	/* Implement callback methods                                                                          */
	/* *************************************************************************************************** */
	@Override
	public void onConnectionFailed(CoapClientChannel channel, boolean notReachable, boolean resetByServer) {
		System.err.println("Connection Failed");
		System.exit(-1);
	}

	@Override
	public void onResponse(CoapClientChannel channel, CoapResponse response) {
		if (response.getPayload() != null) {
			System.out.println("Response: " + response.toString() + " (" + new String(response.getPayload()) + ")");
		} else {
			System.out.println("Response: " + response.toString());
		}
	}

	@Override
	public void onMCResponse(CoapClientChannel channel, CoapResponse response, InetAddress srcAddress, int srcPort) {
		if (response.getPayload() != null) {
			System.out.println("Response from "+ srcAddress.toString() + ":" + srcPort + " - " + response.toString() + " (" + new String(response.getPayload()) + ")");
		} else {
			System.out.println("Response from "+ srcAddress.toString() + ":" + srcPort + " - " + response.toString());
		}
	}
}
