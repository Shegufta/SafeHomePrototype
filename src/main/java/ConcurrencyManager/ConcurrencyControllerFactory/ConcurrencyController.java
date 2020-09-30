package ConcurrencyManager.ConcurrencyControllerFactory;

import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlDevMngrMsg;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventRtnMgrConCtrlMsg;
import Utility.CONSISTENCY_TYPE;
import Utility.ConcurrencyControllerType;
import Utility.LockTable;
import Utility.Routine;
import com.google.common.eventbus.Subscribe;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 12:25 PM
 */
public abstract class ConcurrencyController
{
    public ConcurrencyControllerType concurrencyControllerType;
    public Boolean isDisposed;
    protected LockTable lockTable;

    public ConcurrencyController(ConcurrencyControllerType _concurrencyControllerType,
                                 List<String> _devIDlist,
                                 CONSISTENCY_TYPE _consistencyType,
                                 long _safeHomeStartTime)
    {
        this.concurrencyControllerType = _concurrencyControllerType;
        this.isDisposed = false;
        this.lockTable = new LockTable(_devIDlist, _consistencyType, _safeHomeStartTime);
        EventBusSingleton.getInstance().getEventBus().register(this); //Register to EventBus
    }



    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////

    public abstract void receiveMsgFromRoutingManager(Routine _routine);
    public void sendMsgToRoutingManager(Routine _routine)
    {//send MSG to upper layer
        EventBusSingleton.getInstance().getEventBus().post(new EventRtnMgrConCtrlMsg(false, _routine));
    }


    public abstract void receiveMsgFromDevMngr(Routine _routine);
    public void sendMsgToDevMngr(Routine _routine)
    {//send MSG to lower layer
        EventBusSingleton.getInstance().getEventBus().post(new EventConCtrlDevMngrMsg(true, _routine));
    }

    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    @Subscribe
    public synchronized void EventRtnMgrConCtrlMsg(EventRtnMgrConCtrlMsg _eventRtnMgrConCtrlMsg)
    {// Event from Upper layer
        if(_eventRtnMgrConCtrlMsg.isFromRtnMgrToConCtl)
        {
            this.receiveMsgFromRoutingManager(_eventRtnMgrConCtrlMsg.routine);
        }
    }

    @Subscribe
    public synchronized void EventConCtlrSftyCkrMsg(EventConCtrlDevMngrMsg _eventConCtrlDevMngrMsg)
    {//Event from lower layer
        if(!_eventConCtrlDevMngrMsg.isFromConCtlToDevMngr)
        {
            this.receiveMsgFromDevMngr(_eventConCtrlDevMngrMsg.routine);
        }
    }
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public LockTable getLockTable() {
        return this.lockTable;
    }

    public void clearLockTable() { this.lockTable.clear(); }

    public abstract void Dispose();

    protected void unregisterEventBus()
    { // call it from lower class's Dispose
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus
    }
}
