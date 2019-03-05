package EventBusManager;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author Shegufta Ahsan
 * @project SafeHomePrototype
 * @date 3/2/2019
 * @time 9:06 PM
 */
public class EventBusSingleton
{
    private static EventBusSingleton singleton;
    private Boolean isDisposed;
    private EventBus eventBus;
    private ExecutorService threadPool;

    private EventBusSingleton()
    {

        this.isDisposed = false;
        //this.eventBus = new EventBus(); // use it for synchronized communication
        this.threadPool = Executors.newCachedThreadPool();// use it for asynchronous communication
        this.eventBus = new AsyncEventBus(this.threadPool); // use it for asynchronous communication
        //this.eventBus = new AsyncEventBus(Executors.newCachedThreadPool()); // use it for asynchronous communication. Store the threadPool in a variable so that you can call shutdown letter.
    }

    public EventBus getEventBus()
    {
        return this.eventBus;
    }


    public static synchronized EventBusSingleton getInstance()
    {
        if(null == EventBusSingleton.singleton)
        {
            EventBusSingleton.singleton = new EventBusSingleton();
        }

        if(EventBusSingleton.singleton.isDisposed)
            return null;


        return EventBusSingleton.singleton;
    }


    public void Dispose()
    {
        this.isDisposed = true;
        this.threadPool.shutdownNow();
    }
}

/**
 * SBA: https://stackoverflow.com/questions/24740581/eventbus-google-guava-shutdown-hook
 *
 * The standard Guava EventBus is synchron, it has no internal Thread
 * or something like that. If the thread that calls post(Object event)
 * dies, the EventBus stops delivering events because it uses the caller's Thread).
 *
 * The AsyncEventBus on the other hand takes an Executor (from the
 * java.util.concurrent package) during construction which is used
 * to dispatch events. In this case it depends what executor
 * implementation you use. For example a ThreadPoolExecutor would
 * need a shutdownNow() call to stop delivering messages.
 */
