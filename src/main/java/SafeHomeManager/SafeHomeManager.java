package SafeHomeManager;

import ConcurrencyManager.ConcurrencyControllerSingleton;
import DeviceManager.DeviceConnectionManagerSingleton;
import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventRegisterRemoveDevices;
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
        this.RegisterDevices(SystemParametersSingleton.getInstance().getDeviceList());
        int sleepIntervalMS = 6000;
        System.out.println("Device Registration request sent.... Wait for a while to execute it... Wait time in millisecond = " + sleepIntervalMS);
        Thread.sleep(sleepIntervalMS);
        System.out.println("wake up from sleep");


        //Routine routine1 = SystemParametersSingleton.getInstance().getRoutine("routine1");
        //this.sendMsgToRoutineManager(routine1);

        Routine routine3 = SystemParametersSingleton.getInstance().getRoutine("routine3");
        this.sendMsgToRoutineManager(routine3);
        ////////////////////////////////////////////////////////////////////////////////////////

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

    private void sendMsgToRoutineManager(Routine _routine)
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
    ////////////////////////////////_ADD/REMOVE_DEVICES//////////////////////////////////////////

    public void RegisterDevices(List<DeviceInfo> _devInfoList)
    {
        EventRegisterRemoveDevices registerDevicesEvent = new EventRegisterRemoveDevices(_devInfoList, true);
        EventBusSingleton.getInstance().getEventBus().post(registerDevicesEvent); // post event
    }

    public void RemoveDevices(List<DeviceInfo> _devInfoList)
    {
        EventRegisterRemoveDevices removeDevicesEvent = new EventRegisterRemoveDevices(_devInfoList, false);
        EventBusSingleton.getInstance().getEventBus().post(removeDevicesEvent); // post event
    }

    ////////////////////////////////_ADD/REMOVE_DEVICES//////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public void Dispose()
    {
        Runtime.getRuntime().removeShutdownHook(this.shutdownHook);
        System.out.println("Disposing SafeHomeManager");
        concurrencyControllerSingleton.Dispose();
        safetyCheckerSingleton.Dispose();
        deviceConnectionManagerSingleton.Dispose();

        EventBusSingleton.getInstance().Dispose(); // SBA: WARNING: always Dispose it last. Otherwise it might break other not-Disposed-yet components

    }
}
