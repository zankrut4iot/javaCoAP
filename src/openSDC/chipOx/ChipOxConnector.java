package openSDC.chipOx;

import gnu.io.CommPort;
import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import realDeviceConnector.MyLimitAlertInfo;
import realDeviceConnector.RealDeviceConnector;

/**
 * The connection between the openSDC and the KS world is done here.
 * 
 * The main loop of the thread handles the incoming traffic from the OSCBM via a socket.
 * 
 * @author Martin Kasparick (University of Rostock - Institute of Applied Microelectronics and Computer Engineering)
 * @note Free for use in OR.NET context. (Please keep the original author(s) information in your projects.)
 */
public class ChipOxConnector extends RealDeviceConnector {
	
	private int baudRate = 9600; /// baud rate for UART communication (9600 is ChipOx default value)
	private int dataBits = SerialPort.DATABITS_8; 
	private int stopBits = SerialPort.STOPBITS_1;
	private int parity = SerialPort.PARITY_NONE;
	private static final String portName = "/dev/ttyAMA0"; /// /dev/ttyAMA0 is the default port for UART at Raspberry Pi

	private SerialPort serialPort; ///serial port (UART)


	private static ChipOxConnector chipOxConnection;
	
	
	/**
	 * file that contains the BICEPS XML description of the device
	 */
	private File xmlDescriptionFile = null;
	

	/**
	 * if false, than no alerts will be handled. default = true;
	 */
	private boolean doAlertHandling = true;

	private ChipOxConnector(){
		// Exists only to defeat instantiation.	
	}


	
	public static synchronized ChipOxConnector getInstance(){
		if (chipOxConnection == null){
			chipOxConnection = new ChipOxConnector();	
			
			//initialize the device (this will also load the XML description file of the device)
			chipOxConnection.setRealDevice(ChipOxDevice.getInstance());
			
			
			System.setProperty("gnu.io.rxtx.SerialPorts", portName); //some magic ^^ TODO: document it :-P
			
			
			//initialize the alerts
			List<MyLimitAlertInfo> mlaiList = new ArrayList<MyLimitAlertInfo>();
			MyLimitAlertInfo mlai = new MyLimitAlertInfo(ChipOxDevice.HANDLE_ALSIG_PULSE, ChipOxDevice.PULSE_LOWER, ChipOxDevice.PULSE_UPPER, null);
			mlaiList.add(mlai);
			chipOxConnection.getRealDevice().putMetricLimitAlert(ChipOxDevice.HANDLE_PULSE, mlaiList);
			
			mlaiList = new ArrayList<MyLimitAlertInfo>();
			mlai = new MyLimitAlertInfo(ChipOxDevice.HANDLE_ALSIG_SPO2, ChipOxDevice.SPO2_LOWER, ChipOxDevice.SPO2_UPPER, null);
			mlaiList.add(mlai);
			chipOxConnection.getRealDevice().putMetricLimitAlert(ChipOxDevice.HANDLE_SPO2, mlaiList);
			
//			chipOxConnection.initObservedNumericValues();
		}
		return chipOxConnection;
	}
	
	public void initLimitAlertConfigMetrics(){
		chipOxConnection.setNewValue(ChipOxDevice.PULSE_LOWER, ChipOxDevice.HANDLE_PULSE_LOWER);
		chipOxConnection.setNewValue(ChipOxDevice.PULSE_UPPER, ChipOxDevice.HANDLE_PULSE_UPPER);
		chipOxConnection.setNewValue(ChipOxDevice.SPO2_LOWER, ChipOxDevice.HANDLE_SPO2_LOWER);
		chipOxConnection.setNewValue(ChipOxDevice.SPO2_UPPER, ChipOxDevice.HANDLE_SPO2_UPPER);
	}
	

	public boolean openSerialPort(){
		return openSerialPort(portName);
	}


	/**
	 * Open a connection to a serial port. Here: UART
	 * @param portName name of the port to connect with (for UART communication at Raspberry Pi: /dev/ttyAMA0) 
	 * @return 
	 */
	private boolean openSerialPort(String port){
	
		try {
			CommPortIdentifier portIdentifier = CommPortIdentifier.getPortIdentifier(port);
	        if ( portIdentifier.isCurrentlyOwned() )
	        {
	            System.err.println("###### Error: Port " + port + " is currently in use");
	        }
	        else{
	        	CommPort commPort = portIdentifier.open(this.getClass().getName(),2000);
	        	
	        	if ( commPort instanceof SerialPort){
	        		serialPort = (SerialPort) commPort;
					serialPort.setSerialPortParams(baudRate,dataBits,stopBits,parity);

					InputStream in = serialPort.getInputStream();
					(new Thread(new ChipOxSerialReader(in))).start();

	        	}
	        	else{
	        		System.err.println("###### Error: Port " + port + " is not a serial port");
	        	}
	        }
		} catch (NoSuchPortException e) {
			System.err.println("###### Error: Port " + port + " not found");
			System.out.println("####");
			System.out.println(e.getMessage());
			System.out.println("####");
			e.printStackTrace();
		} catch (PortInUseException e) {
			System.err.println("###### Error: Port " + port + " is currently in use");
			e.printStackTrace();
		} catch (UnsupportedCommOperationException e) {
			System.err.println("###### Error: Port " + port + " could not be configured");
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("###### Error: Port " + port + " Problem!");
			e.printStackTrace();
		}
		return true;
	}


//	/**
//	 * Method handles the alert for a given metric descriptor handle according to a given value
//	 * @param handle
//	 * @param readInputValue
//	 */
//	public void handleLimitAlert(String handle, float readInputValue){
//		List<MyLimitAlertInfo> alertLimitInfos = getMetricLimitAlert(handle);
//		if(alertLimitInfos != null){ //there is an alert for this metric
//			for(MyLimitAlertInfo alertLimitInfo : alertLimitInfos){ //walk through the list of alerts
//				if(readInputValue < alertLimitInfo.getLowerThreshold() || readInputValue > alertLimitInfo.getUpperThreshold()){
//					setNewAlertSignalState(alertLimitInfo, LatchablePausableActivation.ON);
//				}
//				else{
//					setNewAlertSignalState(alertLimitInfo, LatchablePausableActivation.OFF);
//				}
//			}
//		}//end of alert found for this metric
//	}
	
//	/**
//	 * Method handles the alert for a given metric descriptor handle according to a given value
//	 * @param handle
//	 * @param readInputValue
//	 */
//	public void handleAlertOld(String handle, float readInputValue){
//		List<MyAlertInfo> alertInfos = getMetricAlert(handle);
//		if(alertInfos != null){ //there is an alert for this metric
//			for(MyAlertInfo alertInfo : alertInfos){ //walk through the list of alerts
//				if(alertInfo.isUpperThreshold()){ //if it is an upper threshold check whether value > threshold
//					if(readInputValue > alertInfo.getThreshold()){
//						setNewAlertSignalState(alertInfo, LatchablePausableActivation.ON);
//					}
//					else{
//						setNewAlertSignalState(alertInfo, LatchablePausableActivation.OFF);
//					}
//				}
//				else{ //it is an untercut threshold -> check value < threshold
//					if(readInputValue < alertInfo.getThreshold()){
//						setNewAlertSignalState(alertInfo, LatchablePausableActivation.ON);
//					}
//					else{
//						setNewAlertSignalState(alertInfo, LatchablePausableActivation.OFF);
//					}
//				}
//			}
//		}//end of alert found for this metric
//	}
	

	
//	/**
//	 * Methode checks whether the new state is really new and calls the changeAlertState method if necessary 
//	 * @param alertInfo
//	 * @param newState
//	 */
//	private void setNewAlertSignalStateOld(MyAlertInfo alertInfo, LatchablePausableActivation newState){
//		//check whether the new alert signal state is really new
//		if(alertInfo.getCurrentAlertSignalState() != newState){
//			alertInfo.setCurrentAlertSignalState(newState); //set new state in internal data structure 
//			changeAlertState(alertInfo.getAlertStateDescriptorHandle(), newState); //set new state in data model 
//		}
//		
//	}
	


	public File getXmlDescriptionFile() {
		return xmlDescriptionFile;
	}



	public void setXmlDescriptionFile(File xmlDescriptionFile) {
		this.xmlDescriptionFile = xmlDescriptionFile;
	}





	public boolean isDoAlertHandling() {
		return doAlertHandling;
	}



	public void setDoAlertHandling(boolean noAlertHandling) {
		this.doAlertHandling = noAlertHandling;
	}



	


}
