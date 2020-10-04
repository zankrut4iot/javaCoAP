package openSDC.openSDCDevice.chipOx;

import openSDC.chipOx.ChipOxConnector;
import openSDC.chipOx.ChipOxSimulation;
import openSDC.chipOx.ChipOxState;

import org.ws4d.java.util.Log;

import util.chipOx.MyJSAPChipOx;

import CoAP.CoAPServer;

import com.draeger.medical.biceps.device.BICEPSDeviceApplication;
import com.draeger.medical.biceps.device.mdi.MedicalDeviceCommunicationInterface;
import com.martiansoftware.jsap.JSAPResult;

/**
 * My openSDC Device.
 * 
 * @author Parts or complete functionality copied from the openSDC Beta-01 tutorial and / or OR.NET openSDC tutorial workshop 
 * @author Martin Kasparick (University of Rostock - Institute of Applied Microelectronics and Computer Engineering)
 * @note Free for use in OR.NET context. (Please keep the original author(s) information in your projects.)
 */
public class ChipOxOpenSDCDeviceApp {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		//let's handle the call arguments
		//we use JSAP lib
		MyJSAPChipOx jsap = new MyJSAPChipOx();
		
		//now parse the arguments
		JSAPResult config = jsap.parse(args);
		
		if (!config.success()) {
			jsap.displayUsage(config);
		}
		
		//do we use CoAP or MDPWS?
		boolean useCoap = false;
		if (config.getBoolean(MyJSAPChipOx.USE_COAP)){
			ChipOxState.getInstance().setUseCoap(true);
			useCoap = true;
		}
		
		//the following parts should be generic for MDPWS and CoAP (hopefully I worked fine ;-) )
		//Initialize the connector
		ChipOxConnector connector = ChipOxConnector.getInstance();
		if(config.getBoolean(MyJSAPChipOx.WITHOUT_ALERTS)){
			connector.setDoAlertHandling(false);
		}
			
		//now separate between MDPWS and CoAP

		if(!useCoap){
			
			//make the generic initializations
			jsap.initializeGenericParameters(config);
					
			Log.setLogLevel(Log.DEBUG_LEVEL_INFO);
			new BICEPSDeviceApplication(args) {
				MedicalDeviceCommunicationInterface medicalDeviceComInterface=new MyChipOxMedicalCommunicationInterface();
				
				protected MedicalDeviceCommunicationInterface getMedicalDeviceCommunicationInterface() {
					return medicalDeviceComInterface;
				}
				
				protected int getConfigurationId() {
					return 1;
				}
			};
	
	
	
			BICEPSDeviceApplication.getApplication().run();
			
			//initialize values
			connector.initMetricValues();
			connector.initLimitAlertConfigMetrics();
			
		}
		else{ //useCoAP
			//FIXME
			Thread coapServerThread = new Thread(CoAPServer.getInstance());
	    	coapServerThread.start();
		}
		
		//the following parts should be generic for MDPWS and CoAP again (hopefully I worked fine ;-) )
			

		//open serial connection for GPIO (RxTx)
		if(!config.getBoolean(MyJSAPChipOx.RUN_ON_PC)){
			connector.openSerialPort();
		}
		else{
			ChipOxSimulation simulation = new ChipOxSimulation();
			simulation.run();
//			while(true){
//				try {
//					Thread.sleep(100);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
		}


	}

}
