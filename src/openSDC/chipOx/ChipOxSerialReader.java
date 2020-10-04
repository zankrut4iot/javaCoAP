package openSDC.chipOx;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

import util.MyLogger;

public class ChipOxSerialReader implements Runnable 
{
	
	private ChipOxState chipOxState;
    private InputStream in;
    private ChipOxConnector connector;

      
    public ChipOxSerialReader (InputStream in)
    {
        this.in = in;
        
        
        this.connector = ChipOxConnector.getInstance();
        this.chipOxState= ((ChipOxDevice) connector.getRealDevice()).getChipOxState();
    }
    

    
	

	public void run(){


    	ChipOxFrame frame = new ChipOxFrame();
    	
//    	boolean startBitRead = false;
        byte[] buffer = new byte[1024];
        int len = -1;
        try
        {
            while ( ( len = this.in.read(buffer)) > -1 )
            {
                //System.out.print(new String(buffer,0,len));
				for(int i = 0; i < len; i++){
					
//					//display ChipOx output
//					System.out.print(Integer.toHexString(buffer[i] & 0xFF) + " ");
//					if( (buffer[i] & 0xFF) == 0xA8 ){
//						if(startBitRead){
//							System.out.println("");
//							startBitRead = !startBitRead;
//						}
//						else{
//							startBitRead = !startBitRead;
//						}
//					}
					
					//handle ChipOx output
					evaluateReadByte(buffer, i, frame);
					

					
					
				}
            }
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }		
	}
	
    private void evaluateReadByte(byte[] buffer, int i, ChipOxFrame frame){
		switch(frame.ndx){
    		case 0: //start flag expected (but it can also be a stop flag)
    			if( (buffer[i] & 0xFF) != 0xA8 ){ //this is not a start / stop flag
    				frame.reset();
    				return;
    			}
    			else{ //start or stop flag found
    				frame.startStopRead++;
    			}
    			break;
    		case 1:
    			// three options are possible
    			// 1. channel id -> only 127 = 0x7F is a relevant channel id
    			// 2. another start/stop flag -> so this is the real start flag
    			// 3. something else -> wait for a new start flag
    			
    			switch((buffer[i] & 0xFF)){
    				case 0x7F: //correct channel id read (option 1.)
	    				frame.channelId=buffer[i];
	    				break;
    				case 0xA8: //this is a start flag (option 2.)
    					if(frame.startStopRead == 1 ){ //one start/stop flag has been read before
    						//so this is the real start flag -> the start flag read before was a stop flag -> ignore it
    						frame.ndx=0; //reset this pointer
    					}
    					else{ //something going wrong
    	    				frame.reset();
    	    				return;
    					}
    					break;
    				default: //this is not the correct channel id or a start/stop flag
	    				frame.reset();
	    				return;		    				
    			}
    			break;

    		case 2: //data value identifier expected
    			if( ((buffer[i] & 0xFF) != 0x01) // SpO2
    					&& ((buffer[i] & 0xFF) != 0x02) //pulse 
    					&& ((buffer[i] & 0xFF) != 0x08) //state
    					&& ((buffer[i] & 0xFF) != 0x04) //Plethysmogram
    					&& ((buffer[i] & 0xFF) != 0x51) //RT-Datachannel
    					){ 
    				//this is not one of the expected identifiers 
    				frame.reset();
    				return;
    			}
				else{
					frame.identifier = buffer[i];
					break;
				}
			default: //	read data (and checksum)		
				if((buffer[i] & 0xFF) == 0xA8){ //stop flag found
					//calculate checksum
					byte[] checksum = new byte[2];
					int startChecksum = 0;
					//0x01 (SpO2) and 0x04 (Plethysmogram) have 1 byte payload
					if ( ((frame.identifier & 0xFF) == 0x01) || ((frame.identifier & 0xFF) == 0x04) ){
						byte[] tmpData = {frame.channelId, frame.identifier, frame.data[0]};
						checksum = frame.calcChecksum(tmpData, tmpData.length);
						startChecksum = 1;
					}
					//0x02 (pulse) and 0x08 (state) have 2 byte payload 
					else if ( ((frame.identifier & 0xFF) == 0x02) || ((frame.identifier & 0xFF) == 0x08) ){
						byte[] tmpData = {frame.channelId, frame.identifier, frame.data[0], frame.data[1]};
						checksum = frame.calcChecksum(tmpData, tmpData.length);
						startChecksum = 2;
					}
					//0x51 (RT-Datachannel) have 10 times 1 byte payload (this can be configured!!!)
					else if ( ((frame.identifier & 0xFF) == 0x51) ){
						byte[] tmpData = new byte[12];
						tmpData[0] = frame.channelId;
						tmpData[1] = frame.identifier;
						for(int j = 0; j < 10; j++){
							tmpData[j+2] = frame.data[j];
						}
						checksum = frame.calcChecksum(tmpData, tmpData.length);
						startChecksum = 10;
					}
					
					//compare calculated and read checksum
					if( ((frame.data[startChecksum] & 0xFF) != (checksum[0] & 0xFF)) || ((frame.data[startChecksum+1] & 0xFF) != (checksum[1] & 0xFF)) ){
						System.err.println("*** Problem: Bad Checksum! ***");
						System.err.println("read checksum: " + frame.data[startChecksum] + " " + frame.data[startChecksum+1]
								+ " --- calculated checksum: " + checksum[0] + " " + checksum[1] );
					}
					else{ //checksum correct
						if((frame.identifier & 0xFF) == 0x01){
							//check whether it is a new value
							int newValue = (int)frame.data[0] & 0xFF;
							if(chipOxState.getSpO2() != newValue){							
								//some logging output
								MyLogger.log.log(Level.FINE, "Read Input is a NEW value (SpO2): " + newValue + "; handle: " + ChipOxDevice.HANDLE_SPO2);
								
								chipOxState.setSpO2WithOSCPPropagation(newValue);
							}
							
							
							MyLogger.log.log(Level.FINER, "*** SpO2: " + chipOxState.getSpO2());
						}
						else if((frame.identifier & 0xFF) == 0x02){
							short tmp = (short) ((frame.data[0] & 0xFF) << 8);
							tmp += (frame.data[1] & 0xFF); 
							
							int newValue = (int)tmp & 0xFFFF;
							if(chipOxState.getPulse() != newValue){
								//some logging output
								MyLogger.log.log(Level.FINE, "Read Input is a NEW value (Pulse): " + newValue + "; handle: " + ChipOxDevice.HANDLE_PULSE);
								
								chipOxState.setPulseWithOSCPPropagation(newValue);
							}
							
							MyLogger.log.log(Level.FINER, "*** Pulse: " + chipOxState.getPulse());
							

						}
						else if((frame.identifier & 0xFF) == 0x08){ //state value
							/**
							 * Bit order of state value:
							 * data[1] byte: Bit7, Bit6, ..., Bit0
							 * data[0] byte: Bit15, Bit14, ..., Bit8
							 */
							short tmp = 0;
							tmp = (short) ((frame.data[0] & 0xFF) << 8);
							tmp += (frame.data[1] & 0xFF); 
							
							MyLogger.log.log(Level.FINE, "Status Information: " + writeByteBinary(frame.data[0]) + " " + writeByteBinary(frame.data[1]));
							
							//do we have new data?
							if(chipOxState.getDeviceState() != tmp){
								chipOxState.setDeviceState(tmp);
								chipOxState.evaluateAndPropagateDeviceState();
								chipOxState.evaluateAndPropagateDeviceStateAlerts();
							}
							
							if(tmp != 0){
														
								if((frame.data[1] & 0x01) == 1 ){ //Bit 0: "Sensor is off"
									MyLogger.log.log(Level.FINER, "### Sensor is off!");
								}								 
								if((frame.data[1] & 0x02) == 2 ){ //Bit 1: "Finger is out"
									MyLogger.log.log(Level.FINER, "### Finger removed from sensor!");
								}
								if((frame.data[1] & 0x04) == 4 ){ //Bit 2: "Pulse wave detected"
									MyLogger.log.log(Level.FINER, "### Pulse wave detected!");
								}
								if((frame.data[1] & 0x08) == 8 ){ //Bit 3: "Searching for pulse"
									MyLogger.log.log(Level.FINER, "### Searching for pulse!");
								}
								if((frame.data[1] & 0x10) == 16 ){ //Bit 4: "Pulse search takes too long"
									MyLogger.log.log(Level.FINER, "### Pulse search takes too long (pulse cannot be found within 15 s)!");
								}
								if((frame.data[1] & 0x20) == 32 ){ //Bit 5: "Low pulsation strength (low AC/DC ratio)"
									MyLogger.log.log(Level.FINER, "### Low pulsation strength (low AC/DC ratio)!");
								}
								if((frame.data[1] & 0x40) == 64 ){ //Bit 6: "Low signal (low AC and low DC signals)"
									MyLogger.log.log(Level.FINER, "### Low signal (low AC and low DC signals)!");
								}
								if((frame.data[1] & 0x80) == 128 ){ //Bit 7: "Too much ambient light"
									MyLogger.log.log(Level.FINER, "### Too much ambient light!");
								}
								
								if((frame.data[0] & 0x01) == 1 ){ //Bit 8: "Too many disturbances"
									MyLogger.log.log(Level.FINER, "### Too many disturbances!");
								}								 
								if((frame.data[0] & 0x02) == 2 ){ //Bit 9: "Many motion artifacts"
									MyLogger.log.log(Level.FINER, "### Many motion artifacts!");
								}
								if((frame.data[0] & 0x04) == 4 ){ //Bit 10: "Sensor defective"
									chipOxState.setDefect(true);
									MyLogger.log.log(Level.FINER, "### Sensor defective");
								}
								if((frame.data[0] & 0x08) == 8 ){ //Bit 11: "Power supply outside of tolerance"
									MyLogger.log.log(Level.FINER, "### Power supply outside of tolerance!");
								}
								if((frame.data[0] & 0x10) == 16 ){ //Bit 12: "Operating temperature outside of tolerance"
									MyLogger.log.log(Level.FINER, "### Operating temperature outside of tolerance!");
								}
								if((frame.data[0] & 0x20) == 32 ){ //Bit 13: "Wrong sensor"
									MyLogger.log.log(Level.FINER, "### Wrong sensor!");
								}
								if((frame.data[0] & 0x40) == 64 ){ //Bit 14: "Vital parameter data outside of measurement range"
									MyLogger.log.log(Level.FINER, "### Vital parameter data outside of measurement range!");
								}
								if((frame.data[0] & 0x80) == 128 ){ //Bit 15: "-"
									MyLogger.log.log(Level.FINER, "### This state should not happen!!!");
								}

							}
							
						}
						else if((frame.identifier & 0xFF) == 0x04){ //Plethysmogram
							int newValue = (int)frame.data[0] & 0xFF;
							MyLogger.log.log(Level.FINE, "read Plethysmogram Value (from 0x04 Channel): " + newValue);

							

							//put them into waveform queue 
//							connector.pushStreamValues(new double[]{newValue});
						}
						//RT-Datachannel -> we use this only for Plethysmogram data
						else if((frame.identifier & 0xFF) == 0x51){
							
//							for(int j = 0; j < 10; j++){
//								int newValue = (int)frame.data[j] & 0xFF;
//																
//								System.err.print(newValue + " ");
//							}
//							System.err.println("");
							
							MyLogger.log.log(Level.FINER, "read Plethysmogram Value (from 0x51 RT-Channel)");

							//covert from byte[] to double[]
							double[] tmpDataDouble = new double[10];
							for(int j = 0; j < 10; j++){
								tmpDataDouble[j] = ((int)frame.data[j] & 0xFF);
								MyLogger.log.log(Level.FINEST, "read Plethysmogram Value (from 0x51 RT-Channel): " + ((int)frame.data[j] & 0xFF));
							}
							
							if(!chipOxState.useCoap()){
								connector.pushStreamValues(tmpDataDouble);
							}
						}
						
//						we don't need this anymore !?!
//						//now we now that we got data for this device 
//						//-> can change the readiness metric (concept from Lübeck)
//						if(!connector.isReadinessStateInitialized()){
//							chipOxState.setReadinessState(ReadinessState.WAITING);
//							connector.setNewStringValue("WAITING", KSDevice.HANDLE_READINESS_STATE_METRIC);
//							connector.setNewStringValue("WAITING for a Session", KSDevice.HANDLE_READINESS_HINT_METRIC);
//							connector.setReadinessStateInitialized(true);
//						}
					}

						
					frame.reset();
					return;
				}
				//read byte is NOT a stop flag
				else{
					//grab payload -> look for stuffing algorithm control byte (0xA9)
					if( (buffer[i] & 0xFF) == 0xA9 ){ //received control byte -> have to destuff next byte
						frame.stuffing = true;
						return;
					}
					
					if(frame.stuffing){ //last byte was a control byte -> now destuff
						frame.stuffing = false;
						byte tmp = (byte) (buffer[i] & 0xFF);
						tmp = (byte) (tmp | 0x20);
						frame.data[frame.ndx - 3] = tmp;
					}
					else {
						frame.data[frame.ndx - 3] = buffer[i]; //put buffer content into the frame
					}
				}
    			
    	}//end of switch(frame.ndx)
    	frame.ndx++; //increment index pointer
    }
    
    /**
     * Produces an 8 bit binary string from a given byte 
     * @param input byte input data
     * @return 8 bit string representing the 
     */
    private String writeByteBinary(byte input){
    	
    	String ret = ""; //output string    	
    	String tmp = Integer.toBinaryString(input);

    	for(int i = 8; i > tmp.length(); i--){
    		ret = "0" + ret;
    	}
    	ret = ret + tmp;
    	
    	return ret;
    }

}
