package EventBusManager.Events;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 2:21 PM
 */

import Utility.DeviceStatus;
import Utility.Routine;

/**
 * This class contains the message transfer from and to
 * the class SafetyCheckerSingleton and DeviceConnectionManagerSingleton
 * The message direction is marked by the flag isFromCheckerToDevice
 */
public class EventSftyCkrDevMngrMsg
{
    /**
     * The routine will contain a list of command C_0,C_1,C_2,C_3,C_4... Some of these are MUST, some are BEST_EFFORT
     * The DeviceManager will try to deploy the command sequentially, starting from c0
     * If any of the MUST command failed (say C_n), then 'n' is the executionFailedIndex for this routine.
     */

    public Routine routine;
    public Routine rollBackFormula;
    public Boolean isFromCheckerToDevice;

    public EventSftyCkrDevMngrMsg(Boolean _isFromCheckerToDevice, Routine _routine, Routine _rollBackFormula)
    {
        this.routine = _routine;
        this.rollBackFormula = _rollBackFormula;
        this.isFromCheckerToDevice = _isFromCheckerToDevice;
    }

    public Boolean isFailed()
    {
        // if rollback, atleast the first entry of the rollback formula will be executed!
        return (this.rollBackFormula.commandList.get(0).afterExecutionStatus != DeviceStatus.COMMAND_NOT_EXECUTED_YET);
    }

}
