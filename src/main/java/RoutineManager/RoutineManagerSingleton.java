package RoutineManager;

import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventRtnMgrConCtrlMsg;
import EventBusManager.Events.EventSfHmRtnMgrMsg;
import Measurement.MeasurementSingleton;
import Measurement.MeasurementType;
import Utility.Routine;
import Utility.SystemParametersSingleton;
import com.google.common.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 1:13 AM
 */
public class RoutineManagerSingleton
{
    private Boolean isDisposed;
    private Integer uniqueRoutineID;
    private HashMap<Integer, RoutineHandler> routineIDroutineMgrMap;
    private static RoutineManagerSingleton singleton;

    public RoutineManagerSingleton()
    {
        this.isDisposed = false;
        this.uniqueRoutineID = 0;
        this.routineIDroutineMgrMap = new HashMap<>();
        EventBusSingleton.getInstance().getEventBus().register(this); // register to event bus
    }

    public static synchronized RoutineManagerSingleton getInstance()
    {
        if(null == RoutineManagerSingleton.singleton)
        {
            RoutineManagerSingleton.singleton = new RoutineManagerSingleton();
        }

        if(RoutineManagerSingleton.singleton.isDisposed)
            return null;

        return RoutineManagerSingleton.singleton;
    }

    private synchronized Integer getUniqueRoutineID()
    {
        synchronized (this.uniqueRoutineID)
        {
            return ++this.uniqueRoutineID;
        }
    }


    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////

    private void receiveMsgFromSafeHomeManager(final Routine _routine)
    {
        assert(_routine.isCmmandListINITIALIZED == false);

        Routine copiedRoutine = _routine.getDeepCopy(); // Create a Deep copy

        // Register the routine
        copiedRoutine.initializeCommandList();
        copiedRoutine.uniqueRoutineID = getUniqueRoutineID();
        copiedRoutine.registrationTime = System.currentTimeMillis();

        this.routineIDroutineMgrMap.put(copiedRoutine.uniqueRoutineID, new RoutineHandler(copiedRoutine));
    }

    public void sendMsgToSafeHomeManager(Routine _routine)
    {//send MSG to upper layer
        EventBusSingleton.getInstance().getEventBus().post(new EventSfHmRtnMgrMsg(false, _routine));
    }

    private void receiveMsgFromConcurrencyController(Routine _routine)
    {
        if (this.routineIDroutineMgrMap.containsKey(_routine.uniqueRoutineID)) {
            this.routineIDroutineMgrMap.get(_routine.uniqueRoutineID).executionResult(_routine);
            MeasurementSingleton.getInstance().AddResult(MeasurementType.WAIT_TIME, _routine.getStartDelay());
            MeasurementSingleton.getInstance().AddResult(
                MeasurementType.WAIT_TIME_VS_E2E,
                _routine.getWaittimevsE2E());
        } else {
            System.out.println("Executed an un-registered routine: " + _routine);
        }

        System.out.println("If necessary, this result can be further propagated to the SafeHome class");
    }

    public void sendMsgToConcurrencyController(Routine _routine)
    {//send MSG to lower layer
        EventBusSingleton.getInstance().getEventBus().post(new EventRtnMgrConCtrlMsg(true, _routine));
    }
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    @Subscribe
    public void EventSfHmRtnMgrMsg(EventSfHmRtnMgrMsg _eventSfHmRtnMgrMsg)
    {// Event from Upper layer
        if(_eventSfHmRtnMgrMsg.isFromSfHmToRtnMgr)
        { // incoming msg from SafeHomeManager
            this.receiveMsgFromSafeHomeManager(_eventSfHmRtnMgrMsg.routine);
            //TODO: create a producer consumer design pattern here..., push the routine in the BlockingQueue. Current implementation is OK for the first-prototype
        }
    }

    @Subscribe
    public synchronized void EventRtnMgrConCtrlMsg(EventRtnMgrConCtrlMsg _eventRtnMgrConCtrlMsg)
    {// Event from Lower layer
        if(!_eventRtnMgrConCtrlMsg.isFromRtnMgrToConCtl)
        {
            this.receiveMsgFromConcurrencyController(_eventRtnMgrConCtrlMsg.routine);
        }
    }
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void Dispose()
    {
        this.isDisposed = true;
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus

        for(RoutineHandler routineHandler : this.routineIDroutineMgrMap.values())
        {
            routineHandler.Dispose();
        }

        this.routineIDroutineMgrMap.clear();
        this.routineIDroutineMgrMap = null;

    }
}
