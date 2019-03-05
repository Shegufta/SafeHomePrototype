package DeviceManager;

import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnector;
import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnectorFactory;
import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventSftyCkrDevMngrMsg;
import EventBusManager.Events.EventDeviceStatusChange;
import EventBusManager.Events.EventRegisterRemoveDevices;
import Utility.*;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 8:33 PM
 */
public class DeviceConnectionManagerSingleton
{
    private class DeviceTracker
    {
        public DeviceConnector deviceConnector;
        public Long lastStatusUpdateMS;
        public DeviceStatus currentStatus;
        public String devName;

        public void UpdateCurrentStatus(DeviceStatus _currentStatus)
        {
            this.currentStatus = _currentStatus;
            this.lastStatusUpdateMS = System.currentTimeMillis();
        }

        public DeviceTracker(String _devName, DeviceConnector _deviceConnector)
        {
            this.devName = _devName;
            this.deviceConnector = _deviceConnector;
            this.lastStatusUpdateMS = 0L;
            this.currentStatus = DeviceStatus.UNKNOWN;
        }

        public void Dispose()
        {
            this.deviceConnector.Dispose();
        }

        public void ResetConnectionBestEffort()
        {
            this.deviceConnector.ResetConnectionBestEffort();
        }
    }

    private HashMap<String, DeviceTracker> devNameDevTrackerMap;
    private static DeviceConnectionManagerSingleton singleton;
    private Boolean isDisposed;

    private ScheduledFuture<?> scheduledFuture;
    private ScheduledExecutorService scheduledExecutorService;


    private DeviceConnectionManagerSingleton()
    {
        this.devNameDevTrackerMap = new HashMap<>();
        this.isDisposed = false;

        this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

        EventBusSingleton.getInstance().getEventBus().register(this); // Register to event bus

        this.scheduledFuture = this.scheduledExecutorService.scheduleAtFixedRate(
                ()-> {this.periodicStatusAndFailureChecker();},
                0,
                SystemParametersSingleton.getInstance().heartBeatIntervalMS,
                TimeUnit.MILLISECONDS);
    }

    public static synchronized DeviceConnectionManagerSingleton getInstance()
    {
        if(null == DeviceConnectionManagerSingleton.singleton)
        {
            DeviceConnectionManagerSingleton.singleton = new DeviceConnectionManagerSingleton();
        }

        if(DeviceConnectionManagerSingleton.singleton.isDisposed)
            return null;


        return DeviceConnectionManagerSingleton.singleton;
    }

    private synchronized DeviceConnector getConnection(String _devName)
    {
        if(this.isDisposed || !this.devNameDevTrackerMap.containsKey(_devName))
        {
            return null;
        }

        DeviceTracker devTracker = this.devNameDevTrackerMap.get(_devName);

        return devTracker.deviceConnector;
    }



    private synchronized DeviceStatus executeCommand(String _devName, DeviceStatus _targetStatus)
    {
        assert((DeviceStatus.OFF == _targetStatus) || (DeviceStatus.ON == _targetStatus));

        DeviceStatus currentStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        switch (_targetStatus)
        {
            case ON:
            {
                currentStatus = this.sendCommandToDevice(_devName, DeviceCommandType.TURN_ON);
                break;
            }
            case OFF:
            {
                currentStatus = this.sendCommandToDevice(_devName, DeviceCommandType.TURN_OFF);
                break;
            }
            default:
            {
                System.out.println("Invalid _targetStatus : " + _targetStatus.name());
                assert(false);
                System.exit(1);// if assert is turned off, System.exit will close the program
            }
        }

        if(currentStatus == _targetStatus)
        {
            //if successful, update devNameDevTrackerMap. Otherwise the periodicStatusAndFailureChecker will consider it as external change.
            devNameDevTrackerMap.get(_devName).UpdateCurrentStatus(currentStatus);
        }
        else
        {
            // SBA: DO NOT update the devNameDevTrackerMap. Let the failure detector detect the failure.
            assert(currentStatus == DeviceStatus.TIMEOUT);
        }

        return currentStatus;
    }

    private synchronized void executeRoutine(Routine routine, Routine rollBackFormula)
    {
        for(Command command : routine.commandList)
        {
            //This field should be filled up by the Safety checker.
            //This information will be used during rollback.
            assert(command.beforeExecutionStatus != DeviceStatus.COMMAND_NOT_EXECUTED_YET);
            assert(command.afterExecutionStatus == DeviceStatus.COMMAND_NOT_EXECUTED_YET);
        }

        System.out.println("Lower Layer: " + routine);

        for(int index = 0; index < routine.commandList.size() ; ++index)
        {//If a MUST command fails, stop execution. For BestEffort -> don't care

            DeviceStatus targetStatus = routine.commandList.get(index).targetStatus;
            String devName = routine.commandList.get(index).deviceInfo.getDevName();

            DeviceStatus afterExecutionStatus = this.executeCommand(devName, targetStatus); // Execute the command
            routine.commandList.get(index).afterExecutionStatus = afterExecutionStatus; //Update the execution result

            System.out.println("\t\t devName = " + devName + " | TargetStatus = " + targetStatus.name() + " afterEx =  " + afterExecutionStatus.name() );

            if( afterExecutionStatus != targetStatus)
            {// If cannot set the device's status

                if(CommandPriority.MUST == routine.commandList.get(index).commandPriority)
                {
                    int rollBackIndex = index;

                    if(1 == routine.commandList.size())
                    {
                        // Is it a per-command or Per routine?
                        // If per-command, then size of the commandList will be 1
                        // To avoid confusion, if routine.commandList.size() == 1, then
                        // start rollback from furthest side of the rollback.command list.
                        // It will cover both per-routine check (if there is only a single command)
                        // and per-command check (only a single command C_n and rollback of (C_0 to C_n)

                        rollBackIndex = rollBackFormula.commandList.size() - 1;
                    }
                    System.out.println(" \t\t      rollBackIndex = " + rollBackIndex);

                    for(; 0 <= rollBackIndex; --rollBackIndex)
                    {
                        DeviceStatus rollBackStatus = rollBackFormula.commandList.get(rollBackIndex).targetStatus;
                        String rollBackDevName = rollBackFormula.commandList.get(rollBackIndex).deviceInfo.getDevName();
                        afterExecutionStatus = this.executeCommand(rollBackDevName, rollBackStatus); //Try to rollback to its previous stage
                        rollBackFormula.commandList.get(rollBackIndex).afterExecutionStatus = afterExecutionStatus; //Update the current "after execution status"
                    }

                    System.out.println(" \t\t      afterRollBack " + rollBackFormula );

                    break; //cancel further execution

                }
                else
                {
                    //SBA: BEST_EFFORT command failed. For now we dont care about the best_effort command
                }
            }
        }

        this.sendMsgToSafetyChecker(routine, rollBackFormula);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    public void sendMsgToSafetyChecker(Routine executedRoutine, Routine executedRollBackFormula)
    {//send MSG to upper layer

        EventSftyCkrDevMngrMsg executionResult = new EventSftyCkrDevMngrMsg(
                false,
                executedRoutine,
                executedRollBackFormula);

        EventBusSingleton.getInstance().getEventBus().post(executionResult);
    }

    private void receiveMsgFromSafetyChecker(Routine routine, Routine rollBackFormula)
    {//
        this.executeRoutine(routine, rollBackFormula);
    }
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    @Subscribe
    public void EventCheckerDeviceMsg(EventSftyCkrDevMngrMsg _eventSftyCkrDevMngrMsg)
    {
        // Event from Upper layer

        if(_eventSftyCkrDevMngrMsg.isFromCheckerToDevice)
        {
            this.receiveMsgFromSafetyChecker(_eventSftyCkrDevMngrMsg.routine, _eventSftyCkrDevMngrMsg.rollBackFormula);
        }
    }

    @Subscribe
    public void EventRegisterRemoveDevices(EventRegisterRemoveDevices _registerDevicesEvent)
    {
        if(_registerDevicesEvent.isRegisterEvent)
        {
            this.registerDevices(_registerDevicesEvent.devInfoList);
        }
        else
        {
            this.removeDevices(_registerDevicesEvent.devInfoList);
        }
    }
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_ADD/REMOVE_DEVICES//////////////////////////////////////////
    private synchronized void registerDevices(List<DeviceInfo> _devInfoList)
    {
        String devName = "";

        for(DeviceInfo devInfo : _devInfoList)
        {

            devName = devInfo.getDevName();

            if(this.devNameDevTrackerMap.containsKey(devName))
            {
                System.out.println("Device already registered: => " + devInfo);
                assert(false); // Assert does not work in some IDE (e.g. in Intellij you have to explicitly set some flags to turn assert ON). Hence I am forcing the system to exit using System.exit(1)
                System.exit(1);
            }

            System.out.println("registering " + devInfo);

            this.devNameDevTrackerMap.put(devName, new DeviceTracker(devName, DeviceConnectorFactory.createDeviceConnector(devInfo)));
            System.out.println("\t registration complete....");
        }
    }

    private synchronized void removeDevices(List<DeviceInfo> _devInfoList)
    {
        if(!isDisposed)
        {
            for(DeviceInfo _devInfo : _devInfoList)
            {
                if(this.devNameDevTrackerMap.containsKey(_devInfo.getDevName()))
                {
                    this.devNameDevTrackerMap.get(_devInfo.getDevName()).Dispose();
                    this.devNameDevTrackerMap.remove(_devInfo.getDevName());
                }
            }
        }
        for(DeviceInfo devInfo : _devInfoList)
        {
            System.out.println("Device Removed : " + devInfo.getDevName());
        }

        EventBusSingleton.getInstance().getEventBus().post( new EventDeviceStatusChange(_devInfoList, DeviceStatus.REMOVED) ); // post to EventBus

    }

    ////////////////////////////////_ADD/REMOVE_DEVICES//////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////_PERIODIC_EXECUTION_///////////////////////////////////////////
    private synchronized void periodicStatusAndFailureChecker()
    {
        Map<String, DeviceStatus> devNameChangedStatusMap = new HashMap<>();

        for (Map.Entry<String, DeviceTracker> nameDevConnTuple : this.devNameDevTrackerMap.entrySet())
        {
            String devName = nameDevConnTuple.getValue().devName;
            DeviceStatus previousStatus = nameDevConnTuple.getValue().currentStatus;
            DeviceStatus currentStatus = this.sendCommandToDevice(devName, DeviceCommandType.GET_STATUS);
            nameDevConnTuple.getValue().UpdateCurrentStatus(currentStatus);

            if(previousStatus != currentStatus)
            {
                devNameChangedStatusMap.put(devName, currentStatus);
            }
        }

        if(0 < devNameChangedStatusMap.size())
        {
            //Notify only if there is a change
            EventBusSingleton.getInstance().getEventBus().post( new EventDeviceStatusChange(devNameChangedStatusMap) ); // post to EventBus
        }
        else
        {
            //System.out.println("No Change...");
        }
    }
    //////////////////////////////_PERIODIC_EXECUTION_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////



    /////////////////////////////////////////////////////////////////////////////////////////////
    ///////////////////////_PHYSICAL_DEVICE_LEVEL_COMMUNICATION_/////////////////////////////////
    private synchronized DeviceStatus sendCommandToDevice(String _devName, DeviceCommandType _deviceCommandType)
    {
        if(isDisposed)
            return null;

        DeviceStatus devStatus = DeviceStatus.TIMEOUT;

        DeviceConnector devConnector = this.getConnection(_devName);

        if(null == devConnector)
        {
            //this device was never registered or has been removed
            return DeviceStatus.TIMEOUT; // this is equivalent to TIMEOUT
        }

        switch (_deviceCommandType)
        {
            case TURN_ON:
            {
                devStatus = devConnector.turnON();
                break;
            }
            case TURN_OFF:
            {
                devStatus = devConnector.turnOFF();
                break;
            }
            case GET_STATUS:
            {
                devStatus = devConnector.getCurrentStatus();
                break;
            }
            default:
            {
                System.out.println("Unsupported Message Type: " + _deviceCommandType.name());
                assert (false);
                System.exit(1);
            }
        }

        if(DeviceStatus.TIMEOUT == devStatus)
        {// something is wrong... ! try to reset the connection
            this.devNameDevTrackerMap.get(_devName).ResetConnectionBestEffort();
        }

        return devStatus;
    }
    ///////////////////////_PHYSICAL_DEVICE_LEVEL_COMMUNICATION_/////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void Dispose()
    {
        this.isDisposed = true;

        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus

        this.scheduledFuture.cancel(false);
        this.scheduledExecutorService.shutdownNow();

        for (Map.Entry<String, DeviceTracker> nameDevConnTuple : this.devNameDevTrackerMap.entrySet())
        {
            nameDevConnTuple.getValue().Dispose();
        }

        this.devNameDevTrackerMap.clear();
        this.devNameDevTrackerMap = null;
    }
}
