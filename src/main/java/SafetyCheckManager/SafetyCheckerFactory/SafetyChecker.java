package SafetyCheckManager.SafetyCheckerFactory;

import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventSftyCkrDevMngrMsg;
import EventBusManager.Events.EventDeviceStatusChange;
import Utility.DeviceStatus;
import Utility.Routine;
import Utility.SafetyCheckerType;
import com.google.common.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 11:17 PM
 */
public abstract class SafetyChecker
{
    public SafetyCheckerType safetyCheckerType;
    public Map<String, DeviceStatus> devNameStatusMap;
    public Boolean isDisposed;

    public SafetyChecker(SafetyCheckerType _checkerType)
    {
        this.safetyCheckerType = _checkerType;
        this.devNameStatusMap = new HashMap<>();
        this.isDisposed = false;

        EventBusSingleton.getInstance().getEventBus().register(this); //Register to EventBus
    }

    public abstract void doAwesomeSafetyChecking(Routine _routineToCheck);
    public abstract Routine prepareRollbackFormula(Routine _checkedRoutine);

    public abstract void eventHandler_DeviceStatusChange(Map<String, DeviceStatus> devNameStatusMap);

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////

    public abstract void receiveMsgFromConcurrencyController(Routine _routine);
    public void sendMsgToConcurrencyController(Routine _routine)
    {//send MSG to upper layer
        assert(_routine.executionResult != Routine.RoutineExecutionStatus.NOT_ASSIGNED_YET); // By this time routine should have a status!
        EventBusSingleton.getInstance().getEventBus().post(new EventConCtrlSftyCkrMsg(false, _routine)); // post to EventBus
    }

    public abstract void receiveMsgFromDeviceManager(EventSftyCkrDevMngrMsg _eventSftyCkrDevMngrMsg);
    public void sendMsgToDeviceManager(Routine _checkedRoutine, Routine _rollBackFormula)
    {//send MSG to lower layer
        assert(_checkedRoutine.uniqueRoutineID == _rollBackFormula.uniqueRoutineID);

        EventSftyCkrDevMngrMsg rollBackRecipe = new EventSftyCkrDevMngrMsg(
                true,
                _checkedRoutine,
                _rollBackFormula);

        EventBusSingleton.getInstance().getEventBus().post(rollBackRecipe); // post it to the DeviceConnectionManager
    }
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////

    @Subscribe
    public synchronized void EventConCtlrSftyCkrMsg(EventConCtrlSftyCkrMsg _eventConCtlrSftyCkrMsg)
    {// Event from Upper layer
        if(_eventConCtlrSftyCkrMsg.isFromConCtlToSftyCkr)
        {
            this.receiveMsgFromConcurrencyController(_eventConCtlrSftyCkrMsg.routine);
        }
    }

    @Subscribe
    public synchronized void EventCheckerDeviceMsg(EventSftyCkrDevMngrMsg _eventSftyCkrDevMngrMsg)
    {//Event from lower layer
        if(!_eventSftyCkrDevMngrMsg.isFromCheckerToDevice)
        {
            this.receiveMsgFromDeviceManager(_eventSftyCkrDevMngrMsg);
        }
    }
    @Subscribe
    public synchronized void EventDeviceStatusChange(EventDeviceStatusChange eventDeviceStatusChange)
    {
        this.eventHandler_DeviceStatusChange(eventDeviceStatusChange.devNameStatusMap);
    }

    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String toString()
    {
        String str = "";

        str += "Current Device Status = ";
        for (Map.Entry<String, DeviceStatus> nameDevStatus : this.devNameStatusMap.entrySet())
        {
            str += "[ " + nameDevStatus.getKey() + " : " + nameDevStatus.getValue().name() +" ] ";
        }

        return str;
    }

    public abstract void Dispose();

    protected synchronized void unregisterEventBus()
    {// call it from lower class's Dispose
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus
    }
}
