package EventBusManager.Events;

import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/3/2019
 * @time 10:16 PM
 */
public class EventConCtrlSftyCkrMsg
{
    public Boolean isFromConCtlToSftyCkr;
    public Routine routine;

    public EventConCtrlSftyCkrMsg(Boolean _isFromConCtlToSftyCkr, Routine _routine)
    {
        this.isFromConCtlToSftyCkr = _isFromConCtlToSftyCkr;
        this.routine = _routine;
    }
}
