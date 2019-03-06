package SafeHomeManager;

import ConcurrencyManager.ConcurrencyControllerSingleton;
import DeviceManager.DeviceConnectionManagerSingleton;
import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventRegisterRemoveStateChangeDevices;
import EventBusManager.Events.EventSfHmRtnMgrMsg;
import RoutineManager.RoutineManagerSingleton;
import SafetyCheckManager.SafetyCheckerSingleton;
import Utility.*;
import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 10:41 PM
 */
public class SafeHomeManager
{
    RoutineManagerSingleton routineManagerSingleton;
    ConcurrencyControllerSingleton concurrencyControllerSingleton;
    SafetyCheckerSingleton safetyCheckerSingleton;
    DeviceConnectionManagerSingleton deviceConnectionManagerSingleton;

    Thread shutdownHook;
    public void tempFunction_Test_Safety(List<Command> cmdList)
    {
        Routine routine = new Routine("myRoutine", cmdList);
        routine.uniqueRoutineID = 1;
        EventConCtrlSftyCkrMsg eventConCtlrSftyCkrMsg = new EventConCtrlSftyCkrMsg(true, routine);
        EventBusSingleton.getInstance().getEventBus().post(eventConCtlrSftyCkrMsg);
    }

    public SafeHomeManager() throws InterruptedException
    {
        this.shutdownHook = new Thread(this::Dispose, "SafeHomeManager - shutdown hook");
        Runtime.getRuntime().addShutdownHook(this.shutdownHook); // SBA: BestEffort graceful shutdown


        ////////////////////////////////////////////////////////////////////////////////////////
        System.out.println("Initiating Components");
        this.routineManagerSingleton = RoutineManagerSingleton.getInstance();
        this.concurrencyControllerSingleton = ConcurrencyControllerSingleton.getInstance(ConcurrencyControllerType.BASIC);
        this.safetyCheckerSingleton = SafetyCheckerSingleton.getInstance(SafetyCheckerType.PER_ROUTINE);
        this.deviceConnectionManagerSingleton = DeviceConnectionManagerSingleton.getInstance();
        System.out.println("\t\t DONE...");
        ////////////////////////////////////////////////////////////////////////////////////////
        ////////////////////////////////////////////////////////////////////////////////////////
        this.RegisterDevices(SystemParametersSingleton.getInstance().getDeviceInfoList());
        int sleepIntervalMS = 6000;
        System.out.println("Device Registration request sent.... Wait for a while to execute it... Wait time in millisecond = " + sleepIntervalMS);
        Thread.sleep(sleepIntervalMS);
        System.out.println("wake up from sleep");
    }

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////

    private void receiveMsgFromRoutineManager(Routine _routine)
    { // received from Lower layer
        //TODO: add to map
        System.out.println("================================");
        System.out.println(" received from RoutineManager");
        System.out.println(_routine);
        System.out.println("================================");
    }

    public void sendMsgToRoutineManager(Routine _routine)
    {
        EventBusSingleton.getInstance().getEventBus().post(new EventSfHmRtnMgrMsg(true, _routine));
    }
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    @Subscribe
    public void EventSfHmRtnMgrMsg(EventSfHmRtnMgrMsg _eventSfHmRtnMgrMsg)
    {// Event from Lower layer
        if(!_eventSfHmRtnMgrMsg.isFromSfHmToRtnMgr)
        { // incoming msg from RoutineManager
            this.receiveMsgFromRoutineManager(_eventSfHmRtnMgrMsg.routine);
        }
    }
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_ADD/REMOVE/CHANGESTATUS_DEVICES/////////////////////////////

    public void RegisterDevices(List<DeviceInfo> _devInfoList)
    {
        EventRegisterRemoveStateChangeDevices registerDevicesEvent = new EventRegisterRemoveStateChangeDevices(_devInfoList, EventRegisterRemoveStateChangeDevices.DeviceEventType.REGISTER);
        EventBusSingleton.getInstance().getEventBus().post(registerDevicesEvent); // post event
    }

    public void RemoveDevices(List<DeviceInfo> _devInfoList)
    {
        EventRegisterRemoveStateChangeDevices removeDevicesEvent = new EventRegisterRemoveStateChangeDevices(_devInfoList, EventRegisterRemoveStateChangeDevices.DeviceEventType.REMOVE);
        EventBusSingleton.getInstance().getEventBus().post(removeDevicesEvent); // post event
    }

    public void turnOnOffdevice(String _deviceName, boolean _isOn)
    {
        DeviceInfo deviceInfo = SystemParametersSingleton.getInstance().getDeviceInfo(_deviceName);

        if(null == deviceInfo)
        {
            System.out.println("Device : " + _deviceName + " NOT FOUND...");
        }
        else
        {
            EventRegisterRemoveStateChangeDevices.DeviceEventType deviceEventType = (_isOn)?
                    EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_ON : EventRegisterRemoveStateChangeDevices.DeviceEventType.TURN_OFF;

            EventRegisterRemoveStateChangeDevices onOffDevicesEvent = new EventRegisterRemoveStateChangeDevices(deviceInfo, deviceEventType);
            EventBusSingleton.getInstance().getEventBus().post(onOffDevicesEvent); // post event
        }
    }
    ////////////////////////////////_ADD/REMOVE/CHANGESTATUS_DEVICES/////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public void Dispose()
    {
        Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        System.out.println("Disposing SafeHomeManager");
        routineManagerSingleton.Dispose();
        concurrencyControllerSingleton.Dispose();
        safetyCheckerSingleton.Dispose();
        deviceConnectionManagerSingleton.Dispose();

        EventBusSingleton.getInstance().Dispose(); // SBA: WARNING: always Dispose it last. Otherwise it might break other not-Disposed-yet components

    }
}
