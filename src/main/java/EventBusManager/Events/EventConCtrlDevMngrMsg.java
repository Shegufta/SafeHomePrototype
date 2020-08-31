package EventBusManager.Events;

import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 30/8/2020
 * @time 02:34 PM
 */
public class EventConCtrlDevMngrMsg
{
    public Boolean isFromConCtlToDevMngr;
    public Routine routine;

    public EventConCtrlDevMngrMsg(Boolean _isFromConCtlToDevMngr, Routine _routine)
    {
        this.isFromConCtlToDevMngr = _isFromConCtlToDevMngr;
        this.routine = _routine;
    }
}
