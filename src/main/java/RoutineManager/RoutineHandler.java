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

        /*
        try {
            // Guava event bus cannot maintain event ordering. Say, if I push routines in this order R1, R2, R3
            // it might happen that it will be received in the order R1, R3, R2 (actually the exact same thing happened,
            // and it took a thorough debug to figure out Guava's out-of-ordering issue)
            // To force ordering, before sending the next event we are sleeping for sleepTimeMillisecond.
            int sleepTimeMillisecond = 10;
            Thread.sleep(sleepTimeMillisecond);
        } catch (Exception ex)
        {
            System.out.println(ex.toString());
        }
        */
        RoutineManagerSingleton.getInstance().sendMsgToConcurrencyController(this.routine);
    }

    public synchronized void executionResult(Routine _routine)
    {
        System.out.println("\n\n-----------------INSIDE_ROUTINE_HANDLER----------------");
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
