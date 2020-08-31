package ConcurrencyManager.ConcurrencyControllerFactory;

import Utility.CONSISTENCY_TYPE;
import Utility.ConcurrencyControllerType;

import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 12:24 PM
 */
public class ConcurrencyControllerFactory
{
    public static ConcurrencyController createConcurrencyController(ConcurrencyControllerType _controllerType,
                                                                    List<String> _devIDlist,
                                                                    CONSISTENCY_TYPE _consistencyType,
                                                                    long _safeHomeStartTime)
    {
        switch(_controllerType)
        {
            case BASIC:
            {
                return new ConcurrencyControllerBasic(_controllerType,
                        _devIDlist,
                        _consistencyType,
                        _safeHomeStartTime);
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
