package ConcurrencyManager.ConcurrencyControllerFactory;


import EventBusManager.EventBusSingleton;
import EventBusManager.Events.EventConCtrlSftyCkrMsg;
import EventBusManager.Events.EventRtnMgrConCtrlMsg;
import Utility.ConcurrencyControllerType;
import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 12:32 PM
 */
public class ConcurrencyControllerBasic extends ConcurrencyController
{
    private Boolean isDisposed;

    public ConcurrencyControllerBasic(ConcurrencyControllerType _controllerType)
    {
        super(_controllerType);
        assert(ConcurrencyControllerType.BASIC == _controllerType);
        this.isDisposed = false;
    }

    public void doAwesomeConcurrencyControlling(Routine _routine)
    {
        // TO RUI: this at this stage you need to resolve the concurrency controller
        // Feel free to use your own series of functions
        // this one is just a dummy function!
        this.sendMsgToSafetyChecker(_routine);
    }


    @Override
    public void receiveMsgFromRoutingManager(Routine _routine)
    {//MSG from upper layer
        this.doAwesomeConcurrencyControlling(_routine);
    }

    @Override
    public void receiveMsgFromSafetyChecker(Routine _routine)
    {//MSG from lower layer
        this.sendMsgToRoutingManager(_routine);
    }

    @Override
    public void Dispose()
    {
        this.isDisposed = true;
        this.unregisterEventBus();
    }
}
