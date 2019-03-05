package ConcurrencyManager.ConcurrencyControllerFactory;

import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventRtnMgrConCtrlMsg;
import Utility.ConcurrencyControllerType;
import Utility.Routine;
import com.google.common.eventbus.Subscribe;

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

    public ConcurrencyController(ConcurrencyControllerType _concurrencyControllerType)
    {
        this.concurrencyControllerType = _concurrencyControllerType;
        this.isDisposed = false;
        EventBusSingleton.getInstance().getEventBus().register(this); //Register to EventBus
    }



    /////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////_SEND/RECEIVE_MSG_///////////////////////////////////////////

    public abstract void receiveMsgFromRoutingManager(Routine _routine);
    public void sendMsgToRoutingManager(Routine _routine)
    {//send MSG to upper layer
        EventBusSingleton.getInstance().getEventBus().post(new EventRtnMgrConCtrlMsg(false, _routine));
    }


    public abstract void receiveMsgFromSafetyChecker(Routine _routine);
    public void sendMsgToSafetyChecker(Routine _routine)
    {//send MSG to lower layer
        EventBusSingleton.getInstance().getEventBus().post(new EventConCtrlSftyCkrMsg(true, _routine));
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
    public synchronized void EventConCtlrSftyCkrMsg(EventConCtrlSftyCkrMsg _eventConCtlrSftyCkrMsg)
    {//Event from lower layer
        if(!_eventConCtlrSftyCkrMsg.isFromConCtlToSftyCkr)
        {
            this.receiveMsgFromSafetyChecker(_eventConCtlrSftyCkrMsg.routine);
        }
    }
    ////////////////////////////////_INCOMING_EVENTS_////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////

    public abstract void Dispose();

    protected void unregisterEventBus()
    { // call it from lower class's Dispose
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus
    }
}
