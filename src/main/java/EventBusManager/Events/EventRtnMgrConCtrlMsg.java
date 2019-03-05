package EventBusManager.Events;

import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/5/2019
 * @time 12:55 AM
 */
public class EventRtnMgrConCtrlMsg
{
    public Boolean isFromRtnMgrToConCtl;
    public Routine routine;

    public EventRtnMgrConCtrlMsg(Boolean _isFromRtnMgrToConCtl, Routine _routine)
    {
        this.isFromRtnMgrToConCtl = _isFromRtnMgrToConCtl;
        this.routine = _routine;
    }
}
