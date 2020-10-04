package openSDC.chipOx;

import java.util.List;
import java.util.logging.Level;

import CoAP.CoAPServer;

import com.draeger.medical.biceps.common.model.PausableActivation;
import com.draeger.medical.biceps.common.model.SignalPresence;

import realDeviceConnector.RealDevice;
import util.MyLogger;
import util.Util;

public class ChipOxState {
	
	/// indicates whether CoAP should be used 
	private boolean useCoap = false;
	
	/// current SpO2 value
	private int spO2; 
	/// current pulse value
	private int pulse; 
	/// defect flag: signs that the state information sent device defect last time
	private Boolean defect;
	
	/// state information, like finger out, sensor off, ...
	private short deviceState;
	
	/// current readiness state
	private ReadinessState readinessState;
	
	private PausableActivation currentDefectiveAlSig;
	private PausableActivation currentSensorOffAlSig;
	private PausableActivation currentFingerOutAlSig;
	
	/**
	 * Enum type for readiness state values
	 */
	public enum ReadinessState {READY, BUSY, NEEDS_ACTION, WAITING, BROKEN};
	
	private boolean sessionAssociated; 
	
	private static ChipOxState chipOxState = null;
	
	private ChipOxState(){
		reset();
	}
	
	public static ChipOxState getInstance(){
		if(chipOxState == null){
			chipOxState = new ChipOxState();
		}
		return chipOxState;
	}
	
	
	
	
	public void evaluateAndPropagateDeviceStateAlerts(){
		if(useCoap){
			//FIXME
			return;
		}
		//convert device state back to byte[2] array
		byte[] devState = {0,0};
		devState[1] = (byte)(deviceState & 0xff);
		devState[0] = (byte)((deviceState >> 8) & 0xff);
		
		ChipOxConnector connector = ChipOxConnector.getInstance();
		
		List<ChipOxStateAlertInfo> stateAlertInfoList = ((ChipOxDevice) connector.getRealDevice()).getStateAlertInfoList();
		
		for( ChipOxStateAlertInfo stateAlertInfo : stateAlertInfoList ){
			//sensor state has the info but currently alert signal is NOT ON -> turn it ON
			if(( (devState[stateAlertInfo.getByteIndex()] & stateAlertInfo.getMask()) != 0 )
					&& (stateAlertInfo.getCurrentAlSig() != PausableActivation.ON) ){
				connector.changeAlertSignalStateActivationState(stateAlertInfo.getAlSigDescriptorHandle(), SignalPresence.ON);
				stateAlertInfo.setCurrentAlSig(PausableActivation.ON);
			}
			//sensor state has the NO info but currently alert signal is NOT OFF -> turn it OFF
			else if(( (devState[stateAlertInfo.getByteIndex()] & stateAlertInfo.getMask()) == 0 )
					&& (stateAlertInfo.getCurrentAlSig() != PausableActivation.OFF) ){
				connector.changeAlertSignalStateActivationState(stateAlertInfo.getAlSigDescriptorHandle(), SignalPresence.OFF);
				stateAlertInfo.setCurrentAlSig(PausableActivation.OFF);
			}
		}
	}
	
	public ReadinessState evaluateAndPropagateDeviceState(){
		if(useCoap){
			//FIXME
			return ReadinessState.BROKEN;
		}
		
		//convert device state back to byte[2] array
		byte[] devState = {0,0};
		devState[1] = (byte)(deviceState & 0xff);
		devState[0] = (byte)((deviceState >> 8) & 0xff);
		
		ChipOxConnector connector = ChipOxConnector.getInstance();
		
		MyLogger.log.log(Level.FINER, "Evaluate and propagate the device state.");
		
		if(sessionAssociated && deviceState == 0){
			connector.setNewStringValue("READY", RealDevice.HANDLE_READINESS_STATE_METRIC);
			connector.setNewStringValue("", RealDevice.HANDLE_READINESS_HINT_METRIC);
			
			this.readinessState = ReadinessState.READY;
			return ReadinessState.READY;
		}
		//check sensor defect
		else if((devState[0] & 0x04) == 4 ){ //Bit 10: "Sensor defective"
			//update metrics if it is a new state
			if(this.readinessState != ReadinessState.BROKEN){
				connector.setNewStringValue("BROKEN", RealDevice.HANDLE_READINESS_STATE_METRIC);
				connector.setNewStringValue("Sensor defective!", RealDevice.HANDLE_READINESS_HINT_METRIC);
			}
			
			this.readinessState = ReadinessState.BROKEN;
			return ReadinessState.BROKEN;
		}
		//WATING beats NEEDS_ACTION and BUSY (in my case), thus if device has no associated session the state is WAITING
		else if (!sessionAssociated){
			
			connector.setNewStringValue("WAITING", RealDevice.HANDLE_READINESS_STATE_METRIC);
			connector.setNewStringValue("WAITING for a Session", RealDevice.HANDLE_READINESS_HINT_METRIC);
			
			this.readinessState = ReadinessState.WAITING;
			return ReadinessState.WAITING;
		}
		//check sensor BUSY because of serching pulse
		else if((devState[1] & 0x08) == 8 ){ //Bit 3: "Searching for pulse"
			//update metrics if it is a new state
			if(this.readinessState != ReadinessState.BUSY){
				connector.setNewStringValue("BUSY", RealDevice.HANDLE_READINESS_STATE_METRIC);
				connector.setNewStringValue("Searching for pulse", RealDevice.HANDLE_READINESS_HINT_METRIC);
			}
						
			this.readinessState = ReadinessState.BUSY;
			return ReadinessState.BUSY;
		}
		//let's handle the information leading to NEEDS_ACTION
		else {
			String readinessHint = "";
			String separator = "; ";
			
			if((devState[1] & 0x01) == 1 ){ //Bit 0: "Sensor is off"
				readinessHint = Util.append(readinessHint, "Sensor is off", separator);
			}								 
			if((devState[1] & 0x02) == 2 ){ //Bit 1: "Finger is out"
				readinessHint = Util.append(readinessHint, "Finger removed from sensor", separator);
			}
			if((devState[1] & 0x04) == 4 ){ //Bit 2: "Pulse wave detected"
				readinessHint = Util.append(readinessHint, "Pulse wave detected", separator);
			}
			//Bit 3 -> Searching for pulse -> BUSY
			if((devState[1] & 0x10) == 16 ){ //Bit 4: "Pulse search takes too long"
				readinessHint = Util.append(readinessHint,
						"Pulse search takes too long (pulse cannot be found within 15 s) -> turn finger clip on an off again",
						separator);
			}
			if((devState[1] & 0x20) == 32 ){ //Bit 5: "Low pulsation strength (low AC/DC ratio)"
				readinessHint = Util.append(readinessHint, "Low pulsation strength (low AC/DC ratio)", separator);
			}
			if((devState[1] & 0x40) == 64 ){ //Bit 6: "Low signal (low AC and low DC signals)"
				readinessHint = Util.append(readinessHint, "Low signal (low AC and low DC signals)", separator);
			}
			if((devState[1] & 0x80) == 128 ){ //Bit 7: "Too much ambient light"
				readinessHint = Util.append(readinessHint, "Too much ambient light", separator);
			}
			
			if((devState[0] & 0x01) == 1 ){ //Bit 8: "Too many disturbances"
				readinessHint = Util.append(readinessHint, "Too many disturbances", separator);
			}								 
			if((devState[0] & 0x02) == 2 ){ //Bit 9: "Many motion artifacts"
				readinessHint = Util.append(readinessHint, "Many motion artifacts", separator);
			}
			//Bit 10 -> sensor defective -> BROKEN
			if((devState[0] & 0x08) == 8 ){ //Bit 11: "Power supply outside of tolerance"
				readinessHint = Util.append(readinessHint, "Power supply outside of tolerance", separator);
			}
			if((devState[0] & 0x10) == 16 ){ //Bit 12: "Operating temperature outside of tolerance"
				readinessHint = Util.append(readinessHint, "Operating temperature outside of tolerance", separator);
			}
			if((devState[0] & 0x20) == 32 ){ //Bit 13: "Wrong sensor"
				readinessHint = Util.append(readinessHint, "Wrong sensor", separator);
			}
			if((devState[0] & 0x40) == 64 ){ //Bit 14: "Vital parameter data outside of measurement range"
				readinessHint = Util.append(readinessHint, "Vital parameter data outside of measurement range", separator);
			}
			
			

			this.readinessState = ReadinessState.NEEDS_ACTION;
			
			connector.setNewStringValue(this.readinessState.toString(), RealDevice.HANDLE_READINESS_STATE_METRIC);
			connector.setNewStringValue(readinessHint, RealDevice.HANDLE_READINESS_HINT_METRIC);
			
			return ReadinessState.NEEDS_ACTION;
		}
	}
	
	public void reset(){
		spO2 = 1;
		pulse = 1;
		defect = false;
		deviceState = -1;
		readinessState = ReadinessState.BROKEN;
		sessionAssociated = false;
		
		currentDefectiveAlSig = PausableActivation.PAUSED;
		currentFingerOutAlSig = PausableActivation.PAUSED;
		currentSensorOffAlSig = PausableActivation.PAUSED;
	}
	
	/**
	 * method sets the the value and propagates the new value into the OSCP world
	 * @param newSpO2Value
	 */
	public void setSpO2WithOSCPPropagation(int newSpO2Value){
		setSpO2(newSpO2Value);
		
		if(!useCoap){
			oscpPropagationOfNewValue(newSpO2Value, ChipOxDevice.HANDLE_SPO2);
		}
		else{
			coapPropagationOfNewValue(newSpO2Value, ChipOxDevice.HANDLE_SPO2);
		}
	}
	
	/**
	 * method sets the the value and propagates the new value into the OSCP world
	 * @param newPulseValue
	 */
	public void setPulseWithOSCPPropagation(int newPulseValue){
		setPulse(newPulseValue);
		
		if(!useCoap){
			oscpPropagationOfNewValue(newPulseValue, ChipOxDevice.HANDLE_PULSE);
		}
		else{
			coapPropagationOfNewValue(newPulseValue, ChipOxDevice.HANDLE_PULSE);
		}
	}
	
	/**
	 * Method propagates a value to the given descriptor handle into the OSCP world
	 * @param newValue
	 * @param descriptorHandle
	 */
	public void oscpPropagationOfNewValue(int newValue, String descriptorHandle){
		if(useCoap){
			MyLogger.log.severe("Tried to call oscpPropagationOfNewValue when CoAP has been configured!");
			return;
		}
		ChipOxConnector connector = ChipOxConnector.getInstance();
		
		//handle the new value
		connector.setNewValue(newValue, descriptorHandle);
		
		//handle alerts for this metric
		if(connector.isDoAlertHandling()){
			connector.handleLimitAlert(descriptorHandle, newValue);
		}
	}
	
	/**
	 * Method propagates a value to the given descriptor handle into the CoAP world
	 * @param newValue
	 * @param descriptorHandle
	 */
	public void coapPropagationOfNewValue(int newValue, String descriptorHandle){

		CoAPServer coapServer =CoAPServer.getInstance();
		
		//handle the new value
		coapServer.propagateNewValue(descriptorHandle);
		
		//handle alerts for this metric
		//FIXME
	}
	
	public Boolean getDefect() {
		return defect;
	}

	public void setDefect(Boolean defect) {
		this.defect = defect;
	}

	public int getSpO2() {
		return spO2;
	}
	public void setSpO2(int spO2) {
		this.spO2 = spO2;
	}
	public int getPulse() {
		return pulse;
	}
	public void setPulse(int pulse) {
		this.pulse = pulse;
	}

	public short getDeviceState() {
		return deviceState;
	}

	public void setDeviceState(short deviceState) {
		this.deviceState = deviceState;
	}

	public ReadinessState getReadinessState() {
		return readinessState;
	}

	public void setReadinessState(ReadinessState readinessState) {
		this.readinessState = readinessState;
	}

	public boolean isSessionAssociated() {
		return sessionAssociated;
	}

	public void setSessionAssociated(boolean sessionAssociated) {
		this.sessionAssociated = sessionAssociated;
	}

	public boolean useCoap() {
		return useCoap;
	}

	public void setUseCoap(boolean useCoap) {
		this.useCoap = useCoap;
	}
}
