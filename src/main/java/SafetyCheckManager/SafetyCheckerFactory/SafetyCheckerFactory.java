package SafetyCheckManager.SafetyCheckerFactory;

import Utility.SafetyCheckerType;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 11:16 PM
 */
public class SafetyCheckerFactory
{

    public static SafetyChecker createSafetyChecker(SafetyCheckerType _checkerType)
    {

        switch(_checkerType)
        {
            case PER_ROUTINE:
            {
                return new SafetyCheckerPerRoutine(_checkerType);
            }
            default:
            {
                assert (false);
                System.out.println("Inside SafetyCheckerFactory, Unknown Device Type: _checkerType = " + _checkerType.name());
                System.exit(1);
            }
        }

        return null;
    }
}
