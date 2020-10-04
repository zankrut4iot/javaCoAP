package org.ws4d.coap.handsOn.Sensors;

import java.io.IOException;

import org.ws4d.coap.core.rest.api.CoapResource;

/**
 * A Thread running in the Background to observe the value of a sensor
 * The class is necessary to set a resources changed flag.
 */
public class SensorValueObserver implements Runnable {

	/** the CoapResource to be notified */
	CoapResource resource;
	
	/** the Sensor to be observed */
	int observedSensor;
	
	/** Time in ms between observations */
	int frequency;
	
	/** Minimal change of value to be indicated */
	float threshold;
	
	/**
	 * Creates a new sensor value observer to notify a CoapResource about a change of the sensor value
	 * @param resource - the CoapResouce to be notified about a changed value
	 * @param sensor - the sensor to be observed
	 * @param frequency - time in ms between observations
	 * @param threshold - the minimal change to be indicated
	 */
    public SensorValueObserver(CoapResource resource, int sensor, int frequency, float threshold) {
		this.resource = resource;
		this.frequency = frequency;
		this.threshold=threshold;
		this.observedSensor = sensor;
	}

	public void run() {
    	float old_value=0, new_value=0;
		while(true){
			try {Thread.sleep(this.frequency);}
			catch (@SuppressWarnings("unused") InterruptedException e) {/*do nothing*/}
			try {
				new_value = DS28Sensor.readDS18B20(this.observedSensor);
			} catch (@SuppressWarnings("unused") IOException e1) {
				// reading broken no change notifications will be sent
				continue;
			}
			if(java.lang.Math.abs(old_value-new_value) >= this.threshold){
				this.resource.changed();
				old_value = new_value;
			}
		}
    }
}