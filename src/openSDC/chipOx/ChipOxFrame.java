package openSDC.chipOx;

/**
 * Data structure to represent a data frame from the ChipOx device. 
 */
public class ChipOxFrame{
	private int bufferSize = 42; ///buffer size TODO
	public boolean done; /// complete frame received
	public int ndx; /// index of frame element (0 -> start flag is expected; 1 -> start flag read + channel id expected; ...)
	public int startStopRead; /// start stop flag detection
	public byte channelId; /// channel id (@see ChipOx protocol -> relevant is 127 = 0x7F)
	public byte identifier; /// data value identifier (@see ChipOx protocol -> 0x01 = SpO2; 0x02 = pulse)
	public byte[] data; ///data buffer
	public boolean stuffing; /// a control byte has been received -> destuff next byte 
	
	public ChipOxFrame(){
		reset();
	}
	
	/**
	 * Reset all attributes to their default values.
	 */
	public void reset(){
		done = false;
		ndx = 0;
		startStopRead = 0;
		channelId = 0;
		identifier = (byte) 0xFF;
		data = new byte[bufferSize];
		stuffing = false;
	}
	
    /**
     * Calculate the checksum of the ChipOx frame (calculation adopted from the c implementation for the raven board)
     * 
     * @param data the checksum is calculated over this data (including the channel id, identifier and value
     * @param len length of the data
     * @return checksum (array of two bytes)
     */
    public byte[] calcChecksum(byte[] data, int len){
		byte[] ret = {(byte)0x00, (byte)0x00};
		
		short checksum = 0; //checksum (16 bit value)
		byte checksumHigh = 0; //high byte of the checksum (8 bit value)
		byte checksumLow = 0; //low byte of the checksum (8 bit value)
		byte databyte = 0; //current data byte
		
		for (int i = 0; i < len; i++) {
			databyte = data[i];
			checksum = (short) (checksumLow & 0xFF); //& 0xFF because java does not know unsigned 
			checksum += (short) (databyte & 0xFF);
			
			checksumLow = (byte) checksum;
			checksumHigh += (checksum >> 8) + (checksumLow ^ databyte);
		}
		
		ret[0] = checksumHigh;
		ret[1] = checksumLow;			
		
    	return ret;
    }
}