package openSDC.openSDCDevice.chipOx;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import openSDC.chipOx.ChipOxConnector;
import openSDC.chipOx.ChipOxDevice;
import openSDC.chipOx.ChipOxState.ReadinessState;
import openSDC.openSDCDevice.MyCommandProcessingTask;
import realDeviceConnector.MyLimitAlertInfo;
import realDeviceConnector.RealDevice;
import realDeviceConnector.RealDeviceConnector;
import util.MyLogger;

import com.draeger.medical.biceps.common.model.ContextAssociationStateValue;
import com.draeger.medical.biceps.common.model.CurrentAlertSignal;
import com.draeger.medical.biceps.common.model.EnsembleContextState;
import com.draeger.medical.biceps.common.model.InvocationState;
import com.draeger.medical.biceps.common.model.MeasurementState;
import com.draeger.medical.biceps.common.model.MetricMeasurementState;
import com.draeger.medical.biceps.common.model.NumericMetricState;
import com.draeger.medical.biceps.common.model.NumericValue;
import com.draeger.medical.biceps.common.model.OperationDescriptor;
import com.draeger.medical.biceps.common.model.SetValue;
import com.draeger.medical.biceps.common.model.SignalPresence;
import com.draeger.medical.biceps.common.model.State;
import com.draeger.medical.biceps.device.mdi.interaction.MDICommand;
import com.draeger.medical.biceps.device.mdi.interaction.MDINotification;
import com.draeger.medical.biceps.device.mdi.interaction.notification.AlertStateChanged;
import com.draeger.medical.biceps.device.mdi.interaction.notification.CurrentValueChanged;
import com.draeger.medical.biceps.device.mdi.interaction.notification.OperationStateChangedNotification;
import com.draeger.medical.biceps.device.mdi.interaction.operation.ActivateCommand;
import com.draeger.medical.biceps.device.mdi.interaction.operation.OperationCommand;
import com.draeger.medical.biceps.device.mdi.interaction.operation.SetContextStateCommand;
import com.draeger.medical.biceps.device.mdi.interaction.operation.SetRangeCommand;
import com.draeger.medical.biceps.device.mdi.interaction.operation.SetValueCommand;
import com.draeger.medical.biceps.device.mdib.MDIBOperation;
import com.draeger.medical.biceps.device.mdib.OperationRequest;


/**
 * 
 * @author Parts or complete functionality copied from the openSDC Beta-01 tutorial and / or OR.NET openSDC tutorial workshop 
 * @author Martin Kasparick (University of Rostock - Institute of Applied Microelectronics and Computer Engineering)
 * @note Free for use in OR.NET context. (Please keep the original author(s) information in your projects.)
 */
public class MyChipOxCommandProcessingTask extends MyCommandProcessingTask {
	

	

	public MyChipOxCommandProcessingTask(
			MyChipOxMedicalCommunicationInterface medicalDeviceNodeInterface) 
	{
		this.states = medicalDeviceNodeInterface.getInterfaceStateProvider()
				.getInterfaceState().getInitialMDState();
		
		this.description = medicalDeviceNodeInterface.getInterfaceDescriptionProvider()
				.getInterfaceDescription().getMDIB();
		
	}

	//TODO implement "wake up"
	@Override
	protected void handleCommand(MDICommand command) 
	{
		MyLogger.log.log(Level.FINER, "Start handling a command incoming from a DPWS client.");
		System.err.println("Start handling a command incoming from a DPWS client.");
		
		if ((command instanceof SetValueCommand)
				|| (command instanceof ActivateCommand)
				|| (command instanceof SetRangeCommand))
		{
			OperationCommand operationCommand=(OperationCommand) command;
			MDIBOperation mdibOp = operationCommand.getOperation();
			if (mdibOp!=null)
			{
				OperationRequest op = mdibOp.getOperationRequest();
				if (op!=null)
				{
					try {
						boolean operationHasNotFailed=false;

						//make a notification that we have started to handle the incoming command
						notificationQueue.offer(new OperationStateChangedNotification(mdibOp.getMdsHandle(), mdibOp, InvocationState.STARTED), 
								500, TimeUnit.MILLISECONDS);
						

						
						//now we are looking for the state that fits to the operation
						OperationDescriptor operationDescriptor = op.getOperationDescriptor();
						String target = operationDescriptor.getOperationTarget(); //now we have the handle of the target of the operation (have a look at the xml description of the device)
						System.err.println("The the target handle is: " + target);
						
						//walking through the set of states and looking of the state that fits to the target handle
						for (State state : states.getStates()) {
							//TODO: we can only handle NumericMetricStates here -> implement StringMetric
							if (state.getReferencedDescriptor().equals(target)){
								if( (command instanceof SetValueCommand)  && (state instanceof NumericMetricState)) {
									NumericMetricState nms=(NumericMetricState)state; //cast it to a NumericMetricState
									
									//get the value to set from the request
									SetValue setValueMessage=(SetValue)op.getOperationMessage();
									System.err.println("The setValueMessage contains this value: " + setValueMessage.getValue());
									

									//perform the change request in openSDC by inserting it into the queue
									changeSettingValue(setValueMessage, nms);
									CurrentValueChanged cvcNotification=new CurrentValueChanged(nms);
									operationHasNotFailed |= notificationQueue.offer(cvcNotification);
									
									changeAlertLimitsQuickFix(nms);
									
								} else if(command instanceof SetRangeCommand) {
									
									System.err.println("####### SetRangeCommand received ####");
									
								} else if(command instanceof ActivateCommand) {
									//we do not have any activations currently
									//TODO: implement wake up and sleep
								}

								break; //we have found the state -> break the loop
							}
						}


						//make a notification that we finished handling the request (finished or failed)
						if (operationHasNotFailed)
							notificationQueue.offer(new OperationStateChangedNotification(mdibOp.getMdsHandle(), mdibOp, InvocationState.FINISHED), 
									500, TimeUnit.MILLISECONDS);
						else
							notificationQueue.offer(new OperationStateChangedNotification(mdibOp.getMdsHandle(), mdibOp, InvocationState.FAILED), 
									500, TimeUnit.MILLISECONDS);

					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		if (command instanceof SetContextStateCommand) {
			handleSetContextStateCommand(command);
		}
	}

	
	private void changeSettingValue(SetValue setValueMessage, NumericMetricState nms) {
		NumericValue value =nms.getObservedValue();
		if (value==null){
			value=new NumericValue();
		}
		MeasurementState measurementState=new MeasurementState();
		measurementState.setState(MetricMeasurementState.VALID);
		value.setMeasurementState(measurementState);
		value.setTimeOfObservation(BigInteger.valueOf(System.nanoTime()));
		value.setValue(setValueMessage.getValue());
		nms.setObservedValue(value);
	}
	
	private void changeAlertLimitsQuickFix(NumericMetricState nms){
		
		
		float newThreshold = nms.getObservedValue().getValue().floatValue();
		
		
		
		if(nms.getReferencedDescriptor().equals("handle_pulse_lower_limit")){
			alertChangeHelper("handle_pulse", newThreshold, true);
		}
		else if(nms.getReferencedDescriptor().equals("handle_pulse_upper_limit")){
			alertChangeHelper("handle_pulse", newThreshold, false);
		}
		else if(nms.getReferencedDescriptor().equals("handle_SpO2_lower_limit")){
			alertChangeHelper("handle_SpO2", newThreshold, true);
			
		}
		else if(nms.getReferencedDescriptor().equals("handle_SpO2_upper_limit")){
			alertChangeHelper("handle_SpO2", newThreshold, false);
		}
	}

	private void alertChangeHelper(String metricHandle, float newThreshold, boolean lowerLimit) {
		ChipOxConnector coc = ChipOxConnector.getInstance();
		
		List<MyLimitAlertInfo> mlai = coc.getRealDevice().getMetricLimitAlert(metricHandle);
		
		
		
		if(mlai.size() != 1){
			System.err.println("PROBLEM: with alert limit settings !!! (Size = " + mlai.size() + ")");
		}
		for(MyLimitAlertInfo limitAlertInfo : mlai){ //walk through the list of alerts
			
			coc.getRealDevice().putMetricLimitAlert(metricHandle, mlai);
			
			int currentValue = -1;
			if (metricHandle.equals(ChipOxDevice.HANDLE_PULSE)){
				currentValue = ((ChipOxDevice) coc.getRealDevice()).getChipOxState().getPulse();
			}
			else if (metricHandle.equals(ChipOxDevice.HANDLE_SPO2)){
				currentValue = ((ChipOxDevice) coc.getRealDevice()).getChipOxState().getSpO2();
			}
			
			
			if(lowerLimit){
				limitAlertInfo.setLowerThreshold(newThreshold);
//				if(currentValue < newThreshold){
//					changeAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), PausableActivation.ON);
//				}
//				else {
//					changeAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), PausableActivation.OFF);
//				}
			}
			else { //upper limit
				limitAlertInfo.setUpperThreshold(newThreshold);
//				if(currentValue > newThreshold || currentValue < limitAlertInfo.getLowerThreshold()){
//					changeAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), PausableActivation.ON);
//				}
//				else {
//					changeAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), PausableActivation.OFF);
//				}
			}
			
			if(currentValue > limitAlertInfo.getUpperThreshold() || currentValue < limitAlertInfo.getLowerThreshold()){
				setAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), SignalPresence.ON);
			}
			else {
				setAlertState(limitAlertInfo.getAlertStateDescriptorHandle(), SignalPresence.OFF);
			}
		}
	}

	protected void setAlertState(String alertStateDescriptorHandle, SignalPresence newSignalPresenceValue){	

		//looking for the state we are interested in
		for (State state : states.getStates()) {
			if(state instanceof CurrentAlertSignal && state.getReferencedDescriptor().equals(alertStateDescriptorHandle)){ //are there any alert states?
				CurrentAlertSignal cas = (CurrentAlertSignal) state;
				
				cas.setPresence(newSignalPresenceValue);
				
				//put it into the notification queue (the performs the change of the value and provides an event)
				AlertStateChanged ascNotification = new AlertStateChanged(cas);
				if(notificationQueue != null){
					notificationQueue.offer(ascNotification);
				}
				else{
					System.err.println("No notification queue found!");
				}
					
				System.err.println("New Alert Signal State. Handle (of the state): " + cas.getHandle() + " ### New presence value: " + cas.getPresence() + " ### referenced description handle: " + state.getReferencedDescriptor() + "(MyChipOxCommandProcessingTask.setAlertState() )");
				

				break; //we found the state of interest -> leave the loop
			}
		}//end of state loop
	}

	//TODO
	@Override
	protected void changeReadinessStateCausedBySessionContextChange(
			EnsembleContextState sessionContextState) {
		ChipOxConnector connector = ChipOxConnector.getInstance();
		ChipOxDevice chipOxDevice = (ChipOxDevice) connector.getRealDevice();
		
		if(sessionContextState.getContextAssociation() == ContextAssociationStateValue.ASSOCIATED){
			chipOxDevice.getChipOxState().setSessionAssociated(true);
			((ChipOxDevice) connector.getRealDevice()).getChipOxState().setReadinessState(ReadinessState.READY);
			connector.setNewStringValue("READY", RealDevice.HANDLE_READINESS_STATE_METRIC);
			connector.setNewStringValue("", RealDevice.HANDLE_READINESS_HINT_METRIC, true);
		}
		else{
			chipOxDevice.getChipOxState().setSessionAssociated(false);
			((ChipOxDevice) connector.getRealDevice()).getChipOxState().setReadinessState(ReadinessState.WAITING);
			connector.setNewStringValue("WAITING", RealDevice.HANDLE_READINESS_STATE_METRIC);
			connector.setNewStringValue("WAITING for a Session", RealDevice.HANDLE_READINESS_HINT_METRIC, true);
		}
		//now check other parameters for the state decision 
		chipOxDevice.getChipOxState().evaluateAndPropagateDeviceState();

	}


	public void setNotificationQueue(
			BlockingQueue<MDINotification> notificationQueue) {
		this.notificationQueue = notificationQueue;
		
		//make the notificationQueue accessible for the connector
		RealDeviceConnector.setNotificationQueue(notificationQueue);
	}


}
