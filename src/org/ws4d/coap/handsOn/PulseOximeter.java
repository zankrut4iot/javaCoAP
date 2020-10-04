package org.ws4d.coap.handsOn;

import java.util.List;
import java.util.Locale;
import java.lang.*;
import org.ws4d.coap.core.enumerations.CoapMediaType;
import org.ws4d.coap.core.rest.BasicCoapResource;
import org.ws4d.coap.core.rest.CoapData;
import org.ws4d.coap.handsOn.Sensors.PulseOximeterValues;

public class PulseOximeter extends BasicCoapResource {
	
	
	private PulseOximeter(String path, byte[] value, CoapMediaType mediaType) {
		// 		resource path,		initial value,		media type
		super(	path, 				value, 				mediaType);
		
		//disallow POST, PUT and DELETE
		this.setDeletable(false);
    	this.setPostable(false);
    	this.setPutable(false);
    	
    	// add some meta Information (optional)
    	this.setResourceType("PulseOximeter");
    	this.setInterfaceDescription("GET only");
	}
    
    public PulseOximeter(){
    	// 		resource path,		initial value,		media type
    	this(	"/PulseOximeter", 	"0.0".getBytes(), 	CoapMediaType.text_plain);
    }

    @Override
    public synchronized CoapData get(List<CoapMediaType> mediaTypesAccepted) {
    	int valueSpO2 = PulseOximeterValues.ReadSpO2();
    	String result = valueSpO2+"%";
    	//String result = String.format(Locale.US, "%1$,.1f", combine);
    	return new CoapData(result.getBytes(), CoapMediaType.text_plain);
    }
    
    @Override
    public synchronized CoapData get(List<String> query, List<CoapMediaType> mediaTypesAccepted) {
    	/* we just ignore query parameters*/
    	return get(mediaTypesAccepted);
    }
}
