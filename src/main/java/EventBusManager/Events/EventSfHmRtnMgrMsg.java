package EventBusManager.Events;

import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/4/2019
 * @time 7:28 PM
 */
public class EventSfHmRtnMgrMsg
{
    public Boolean isFromSfHmToRtnMgr;
    public Routine routine;

    public EventSfHmRtnMgrMsg(Boolean _isFromSfHmToRtnMgr, Routine _routine)
    {

        this.isFromSfHmToRtnMgr = _isFromSfHmToRtnMgr;
        this.routine = _routine;
    }
}
