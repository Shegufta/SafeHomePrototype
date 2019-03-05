package ConcurrencyManager.ConcurrencyControllerFactory;

import Utility.ConcurrencyControllerType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 12:24 PM
 */
public class ConcurrencyControllerFactory
{
    public static ConcurrencyController createConcurrencyController(ConcurrencyControllerType _controllerType)
    {
        switch(_controllerType)
        {
            case BASIC:
            {
                return new ConcurrencyControllerBasic(_controllerType);
            }
            default:
            {
                assert (false);
                System.out.println("Inside ConcurrencyControllerFactory, Unknown Device Type: _controllerType = " + _controllerType.name());
                System.exit(1);
            }
        }

        return null;
    }
}
