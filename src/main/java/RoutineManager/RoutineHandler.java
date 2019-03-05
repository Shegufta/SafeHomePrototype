package RoutineManager;

import Utility.Routine;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 2/28/2019
 * @time 1:30 AM
 */
public class RoutineHandler
{

    public Routine routine;
    public Boolean isDisposed;

    public RoutineHandler(Routine _routine)
    {
        this.isDisposed = false;
        this.routine = _routine;
        this.sendToConcurrencyController();
    }

    public void sendToConcurrencyController()
    {
        assert(0 <= this.routine.uniqueRoutineID);

        RoutineManagerSingleton.getInstance().sendMsgToConcurrencyController(this.routine);
    }

    public synchronized void executionResult(Routine _routine)
    {
        System.out.println("\n\n-----------------INSIDE_ROUTINE_HANDLER----------------");
        System.out.println(" Routine Execution Result : " + routine.executionResult);
        System.out.println(" TODO: Take further Decision......[hints: if failed, retry? if successful, count it as long-running?]");
        System.out.println(_routine);
        System.out.println("-------------------------------------------------------\n");
    }

    public synchronized void Dispose()
    {
        this.isDisposed = true;
        this.routine.Dispose();
    }
}
