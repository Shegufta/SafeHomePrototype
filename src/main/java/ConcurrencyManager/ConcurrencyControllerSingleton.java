package ConcurrencyManager;

import ConcurrencyManager.ConcurrencyControllerFactory.ConcurrencyController;
import ConcurrencyManager.ConcurrencyControllerFactory.ConcurrencyControllerFactory;
import EventBusManager.EventBusSingleton;
import Utility.ConcurrencyControllerType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 12:19 PM
 */
public class ConcurrencyControllerSingleton
{
    private static ConcurrencyControllerSingleton singleton;
    private Boolean isDisposed;
    private ConcurrencyController concurrencyController;

    private ConcurrencyControllerSingleton(ConcurrencyControllerType _concurrencyControllerType)
    {
        this.isDisposed = false;
        this.concurrencyController = ConcurrencyControllerFactory.createConcurrencyController(_concurrencyControllerType);
        EventBusSingleton.getInstance().getEventBus().register(this); // register to event bus
    }

    public static synchronized ConcurrencyControllerSingleton getInstance(ConcurrencyControllerType _controllerType)
    {
        if(null == ConcurrencyControllerSingleton.singleton)
        {
            ConcurrencyControllerSingleton.singleton = new ConcurrencyControllerSingleton(_controllerType);
        }

        if(ConcurrencyControllerSingleton.singleton.isDisposed)
            return null;

        return ConcurrencyControllerSingleton.singleton;
    }

    public void Dispose()
    {
        this.isDisposed = true;
        this.concurrencyController.Dispose();
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus
    }
}
