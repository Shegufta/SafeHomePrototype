package DeviceManager;

import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnector;
import DeviceManager.DeviceDrivers.DeviceFactory.DeviceConnectorFactory;
import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlDevMngrMsg;
import EventBusManager.Events.EventDeviceStatusChange;
import EventBusManager.Events.EventRegisterRemoveStateChangeDevices;
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
    private class ParallelExecutor implements  Runnable {

        DeviceConnectionManagerSingleton parentClass;
        Routine routine;
        public ParallelExecutor(DeviceConnectionManagerSingleton _parentClass, Routine _routine)
        {
            this.parentClass = _parentClass;
            this.routine = _routine;

        }

        public void run() {
            List<RoutineExecutionRecipe> executionRecipeList = this.routine.getRoutineExecuteRecipe();

            for(int I = 0 ; I < executionRecipeList.size() ; I++)
            {
                RoutineExecutionRecipe recipe = executionRecipeList.get(I);

                if(0 < recipe.sleepMilliSecBeforeExecutingCmd)
                {
                    try
                    {
                        Thread.sleep(recipe.sleepMilliSecBeforeExecutingCmd);
                    }
                    catch(InterruptedException ex)
                    {
                        System.out.println(ex.toString());
                        System.exit(1);
                    }
                }

                DeviceInfo devInfo = recipe.deviceInfo;
                DeviceStatus targetStatus = recipe.targetStatus;
                DeviceStatus afterExecutionStatus = this.parentClass.executeCommand(devInfo, targetStatus, false); // Execute the command
                //System.out.println("\t\t ++++++++++++++++++++++++++++  devName = " + devInfo.getDevName() + " | TargetStatus = " + targetStatus.name() + " afterEx =  " + afterExecutionStatus.name() );
                System.out.println("\t\t\t ####-subCmd " + recipe);
            }

            this.parentClass.sendMsgToConCtrl(routine);
        }
    }

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



    private synchronized DeviceStatus executeCommand(DeviceInfo devInfo, DeviceStatus _targetStatus, Boolean _isManualOverride)
    {
        assert((DeviceStatus.OFF == _targetStatus) ||
                (DeviceStatus.ON == _targetStatus) ||
                ( (DeviceStatus.TIMEOUT == _targetStatus) && (devInfo.getDevType() == DeviceType.DUMMY_DEVICE) )
        );

        if((DeviceStatus.TIMEOUT == _targetStatus) && (devInfo.getDevType() != DeviceType.DUMMY_DEVICE))
        {
            System.out.println("\ninside DeviceConnectionManagerSingleton::executeCommand(...)");
            System.out.println("Fatal Error: Can set TIMEOUT only in DUMMY DEVICE...... Current device type is : " + devInfo.getDevType().name());
            System.exit(1);
        }

        String devName = devInfo.getDevName();

        DeviceStatus currentStatus = DeviceStatus.COMMAND_NOT_EXECUTED_YET;
        switch (_targetStatus)
        {
            case ON:
            {
                currentStatus = this.sendCommandToDevice(devName, DeviceCommandType.TURN_ON);
                break;
            }
            case OFF:
            {
                currentStatus = this.sendCommandToDevice(devName, DeviceCommandType.TURN_OFF);
                break;
            }
            case TIMEOUT:
            {
                // NOTE: this option is only for DUMMY_DEVICE
                assert(devInfo.getDevType() == DeviceType.DUMMY_DEVICE);

                currentStatus = this.sendCommandToDevice(devName, DeviceCommandType.TIMEOUT);
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
            //If it is a DUMMY_DEVICE and _targetStatus = TIMEOUT, then do not update devNameDevTrackerMap
            //If devNameDevTrackerMapis updated to TIMEOUT, then the "device state change" detector
            // wont be able to detect the simulated change (ON, OFF, UNPLUG) of the DUMMY_DEVICE

            //if successful, update devNameDevTrackerMap. Otherwise the periodicStatusAndFailureChecker will consider it as external change.
            if (!_isManualOverride) // TIMEOUT can be set as targetStatus only for DUMMY_DEVICE
            {
                // if it is not a ManualOverride (i.e. sending command from SafeHomeManager, then update the local status-table
                // However, in the case of a ManualOverride, let the Periodic Status Tracker track down the manually changed status
                devNameDevTrackerMap.get(devName).UpdateCurrentStatus(currentStatus);
            }
        }
        else
        {
            // SBA: DO NOT update the devNameDevTrackerMap. Let the failure detector detect the failure.
            assert(currentStatus == DeviceStatus.TIMEOUT);
        }

        return currentStatus;
    }

    private synchronized void executeRoutine(Routine routine)
    {
        System.out.println("\t\t ####-T Creating Thread For Rtn " + routine.uniqueRoutineID);

        ParallelExecutor parallelExecutor = new ParallelExecutor(this, routine);

        Thread thread = new Thread(parallelExecutor);
        thread.start();

        /*
        for(int index = 0; index < routine.commandList.size() ; ++index)
        {//If a MUST command fails, stop execution. For BestEffort -> don't care

            DeviceStatus targetStatus = routine.commandList.get(index).targetStatus;
            DeviceInfo devInfo = routine.commandList.get(index).deviceInfo;

            DeviceStatus afterExecutionStatus = this.executeCommand(devInfo, targetStatus, false); // Execute the command

            System.out.println("\t\t devName = " + devInfo.getDevName() + " | TargetStatus = " + targetStatus.name() + " afterEx =  " + afterExecutionStatus.name() );
        }

        this.sendMsgToConCtrl(routine);

        */
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    public void sendMsgToConCtrl(Routine executedRoutine)
    {//send MSG to upper layer

        EventConCtrlDevMngrMsg executionResult = new EventConCtrlDevMngrMsg(
                false,
                executedRoutine);

        EventBusSingleton.getInstance().getEventBus().post(executionResult);
    }

    private void receiveMsgFromConCtrl(Routine routine)
    {//
        System.out.println("\t ####-Rtn -t:" + System.currentTimeMillis() + " | RoutineID:" + routine.uniqueRoutineID + " | " +
                routine);
        this.executeRoutine(routine);
    }
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    @Subscribe
    public void EventCheckerDeviceMsg(EventConCtrlDevMngrMsg _eventConCtrlDevMngrMsg)
    {
        // Event from Upper layer

        if(_eventConCtrlDevMngrMsg.isFromConCtlToDevMngr)
        {
            this.receiveMsgFromConCtrl(_eventConCtrlDevMngrMsg.routine);
        }
    }

    @Subscribe
    public void EventRegisterRemoveDevices(EventRegisterRemoveStateChangeDevices _registerDevicesEvent)
    {
        if(!isDisposed)
        {
            switch (_registerDevicesEvent.deviceEventType)
            {
                case REGISTER:
                {
                    this.registerDevices(_registerDevicesEvent.devInfoList);
                    break;
                }
                case REMOVE:
                {
                    this.removeDevices(_registerDevicesEvent.devInfoList);
                    break;
                }
                case TURN_ON:
                case TURN_OFF:
                case UNPLUG:
                {
                    DeviceStatus deviceStatus = DeviceStatus.ON;
                    if(_registerDevicesEvent.deviceEventType == EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON)
                    {
                        deviceStatus = DeviceStatus.ON;
                    }
                    else if(_registerDevicesEvent.deviceEventType == EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_OFF)
                    {
                        deviceStatus = DeviceStatus.OFF;
                    }
                    else
                        deviceStatus = DeviceStatus.TIMEOUT; // This is only for DUMMY_DEVICE. UNPLUG is equivalent to TIMEOUT

                    for (DeviceInfo devInfo : _registerDevicesEvent.devInfoList)
                    {
                        DeviceStatus afterExecutionStatus = this.executeCommand(devInfo, deviceStatus, true); // Execute the command
                        //this is a BestEffort service. can ignore afterExecutionStatus
                    }

                    break;
                }
            }
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
            case TIMEOUT:
            {// only for DUMMY_DEVICES... you can set the status TIMEOUT. NOT APPLICABLE FOR OTHER DEVICES
                devStatus = devConnector.simulateTIMEOUTonlyIn_DUMMY_DEVICE();
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
