package org.ws4d.coap.handsOn;


import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.ws4d.coap.core.enumerations.CoapMediaType;
import org.ws4d.coap.core.rest.BasicCoapResource;
import org.ws4d.coap.core.rest.CoapData;
import org.ws4d.coap.handsOn.Sensors.DS28Sensor;
import org.ws4d.coap.handsOn.Sensors.SensorValueObserver;

public class TemperatureSensorResource extends BasicCoapResource {
	
	private static double MAX_TEMP = 30.0;
	private static double MIN_TEMP = 20.0;
	DS28Sensor ReadTemp = new DS28Sensor();

	private TemperatureSensorResource(String path, byte[] value, CoapMediaType mediaType) {
		// 		resource path,		initial value,		media type
		super(	path, 				value, 				mediaType);
		
		//disallow POST, PUT and DELETE
		this.setDeletable(false);
    	this.setPostable(false);
    	this.setPutable(false);
    	
    	// add some meta Information (optional)
    	this.setResourceType("Temperature");
    	this.setInterfaceDescription("GET only");
    	new Thread(new SensorValueObserver(this, 0, 250, 0.25f)).start();
	}
    
    public TemperatureSensorResource(){
    	// 		resource path,		initial value,		media type
    	this(	"/temperature/Sensor", 	"0.0".getBytes(), 	CoapMediaType.text_plain);
    }

    @Override
    public synchronized CoapData get(List<CoapMediaType> mediaTypesAccepted) {
    	//double randomTemperature = MIN_TEMP + ( Math.random() * (MAX_TEMP-MIN_TEMP) );
    	double  senseTemperature = 10.0;
		try {
			senseTemperature = DS28Sensor.readDS18B20(0);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	String result = String.format(Locale.US, "%1$,.1f", senseTemperature);
    	return new CoapData(result.getBytes(), CoapMediaType.text_plain);
    }
    
    @Override
    public synchronized CoapData get(List<String> query, List<CoapMediaType> mediaTypesAccepted) {
    	/* we just ignore query parameters*/
    	return get(mediaTypesAccepted);
    }
}
