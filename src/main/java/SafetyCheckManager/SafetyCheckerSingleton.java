package SafetyCheckManager;

import EventBusManager.EventBusSingleton;
import SafetyCheckManager.SafetyCheckerFactory.SafetyChecker;
import SafetyCheckManager.SafetyCheckerFactory.SafetyCheckerFactory;
import Utility.SafetyCheckerType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 11:12 PM
 */
public class SafetyCheckerSingleton
{
    private static SafetyCheckerSingleton singleton;
    private Boolean isDisposed;
    private SafetyChecker safetyChecker;

    private SafetyCheckerSingleton(SafetyCheckerType _safetyCheckerType)
    {
        this.isDisposed = false;
        this.safetyChecker = SafetyCheckerFactory.createSafetyChecker(_safetyCheckerType);
        EventBusSingleton.getInstance().getEventBus().register(this); // register to event bus
    }

    public static synchronized SafetyCheckerSingleton getInstance(SafetyCheckerType _safetyCheckerType)
    {
        if(null == SafetyCheckerSingleton.singleton)
        {
            SafetyCheckerSingleton.singleton = new SafetyCheckerSingleton(_safetyCheckerType);
        }

        if(SafetyCheckerSingleton.singleton.isDisposed)
            return null;

        return SafetyCheckerSingleton.singleton;
    }
f
    public void Dispose()
    {
        this.isDisposed = true;
        this.safetyChecker.Dispose();
        EventBusSingleton.getInstance().getEventBus().unregister(this); //Unregister from EventBus
    }
}
