package org.ws4d.coap.handsOn;

import java.util.List;
import java.util.Locale;

import org.ws4d.coap.core.enumerations.CoapMediaType;
import org.ws4d.coap.core.rest.BasicCoapResource;
import org.ws4d.coap.core.rest.CoapData;

public class TemperatureFakeResource extends BasicCoapResource {
	
	private static double MAX_TEMP = 30.0;
	private static double MIN_TEMP = 20.0;

	private TemperatureFakeResource(String path, byte[] value, CoapMediaType mediaType) {
		// 		resource path,		initial value,		media type
		super(	path, 				value, 				mediaType);
		
		//disallow POST, PUT and DELETE
		this.setDeletable(false);
    	this.setPostable(false);
    	this.setPutable(false);
    	
    	// add some meta Information (optional)
    	this.setResourceType("Temperature");
    	this.setInterfaceDescription("GET only");
	}
    
    public TemperatureFakeResource(){
    	// 		resource path,		initial value,		media type
    	this(	"/temperature/fake", 	"0.0".getBytes(), 	CoapMediaType.text_plain);
    }

    @Override
    public synchronized CoapData get(List<CoapMediaType> mediaTypesAccepted) {
    	double randomTemperature = MIN_TEMP + ( Math.random() * (MAX_TEMP-MIN_TEMP) );
    	String result = String.format(Locale.US, "%1$,.1f", randomTemperature);
    	return new CoapData(result.getBytes(), CoapMediaType.text_plain);
    }
    
    @Override
    public synchronized CoapData get(List<String> query, List<CoapMediaType> mediaTypesAccepted) {
    	/* we just ignore query parameters*/
    	return get(mediaTypesAccepted);
    }
}
