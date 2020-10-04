package org.ws4d.coap.handsOn.Sensors;

import java.util.Locale;
import openSDC.chipOx.*;

public class PulseOximeterValues {
	
	
	private static ChipOxConnector chipOxConnect;
	private static ChipOxState chipOxState;
	public static int ReadSpO2(){
		int readSpO2 = 0;
		chipOxConnect = ChipOxConnector.getInstance();
		
		
		chipOxState = ChipOxState.getInstance();
		if(chipOxConnect.openSerialPort()){
			readSpO2 = chipOxState.getSpO2();
		}
		
		return readSpO2;
		
	}
	
	
}
