package DAGexecutionExperiment;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Shegufta Ahsan
 * @project DagExperiment
 * @date 3/11/2019
 * @time 1:45 AM
 */
public class Node implements Runnable
{
    //public Boolean result;
    public Boolean isTraversed;
    public Boolean isSuccessfullyExecuted;
    public String name;
    public List<Node> parentList;
    public List<Node> childrenList;
    public Thread executionThread;
    public Thread rollbackThread;
    public final Integer EXECUTION_TIME_MS = 1500;
    public Boolean isRollback;

    public Node(String _name)
    {
        this.name = _name;

        this.executionThread = new Thread(this, this.name + " : Execution Thread");
        this.rollbackThread = new Thread(this, this.name + " : Rollback Thread");

        this.parentList = new ArrayList<Node>();
        this.childrenList = new ArrayList<Node>();

        //this.result = false;
        this.isSuccessfullyExecuted = false;
        this.isTraversed = false;
        this.isRollback = false;
    }

    public void addParent(Node _parent)
    {
        this.parentList.add(_parent);
    }

    public void addChild(Node _child)
    {
        this.childrenList.add(_child);
    }

    private Thread startExecutionThread()
    {
        synchronized (this.executionThread)
        {
            if (!this.isTraversed)
            {
                this.isRollback = false;
                this.isTraversed = true;
                this.executionThread.start();

            }

            return this.executionThread;
        }
    }

    private Thread startRollbackThread()
    {
        synchronized (this.rollbackThread)
        {
            if (!this.isRollback)
            {
                this.isRollback = true;
                this.rollbackThread.start();
            }

            return this.rollbackThread;
        }
    }

    private void rollback() throws InterruptedException
    {
        synchronized (this.rollbackThread)
        {
            this.isRollback = true;

            try
            {// Rollback
                if(this.isSuccessfullyExecuted)
                {
                    this.isSuccessfullyExecuted = false;
                    //System.out.println(this.name + " sleeping");
                    Thread.sleep(EXECUTION_TIME_MS);
                    System.out.println("< ROLLBACK " + this.name + " >");
                }
            }
            catch (Exception ex)
            {

            }

            if (!parentList.isEmpty())
            {
                List<Thread> rollBackThreadList = new ArrayList<>();

                for (Node parentToRollback : this.parentList)
                {
                    rollBackThreadList.add(parentToRollback.startRollbackThread());
                }

                for (Thread thread : rollBackThreadList)
                { //wait till all rollback complete
                    thread.join();
                }
            }

        }
    }

    public Boolean traverse() throws InterruptedException
    {
        synchronized (this.executionThread)
        {
            this.isTraversed = true; // do not remove it from here.

            Boolean parentsExecutionResult = true;

            if (!parentList.isEmpty())
            {
                List<Thread> threadList = new ArrayList<>();

                for (Node parent : this.parentList)
                {
                    threadList.add(parent.startExecutionThread());
                }

                for (Thread thread : threadList)
                {
                    thread.join();
                }

                for (Node parent : this.parentList)
                {
                    parentsExecutionResult &= parent.isSuccessfullyExecuted;
                }
            }// the else part: if no parent, then parent result is by default true

            Boolean isRollBack = !parentsExecutionResult;

            if (!isRollBack)
            {
                try
                {
                    //System.out.println(this.name + " sleeping");


                    if(0 == this.name.compareTo("a"))
                    {
                        Thread.sleep(EXECUTION_TIME_MS);
                        System.out.println("< Fail " + this.name + " >");
                        this.isSuccessfullyExecuted = false;
                    }
                    else
                    {
                        Thread.sleep(EXECUTION_TIME_MS);
                        System.out.println("< Execute " + this.name + " >");
                        this.isSuccessfullyExecuted = true;
                    }

                    isRollBack = !this.isSuccessfullyExecuted;
                }
                catch (Exception ex)
                {

                }
            }

            if(isRollBack)
            {
                // go upwards again, to roll-back
                this.startRollbackThread().join(); // wait till all rollback completes
                //this.isSuccessfullyExecuted = false;
            }
        }

        return this.isSuccessfullyExecuted; // if execution successful
    }

    public void run()
    {
        try
        {
            if(this.isRollback)
            {
                this.rollback();
            }
            else
            {
                this.traverse();
            }

        }
        catch (Exception ex)
        {

        }
    }

    public static void main(String[] args) throws InterruptedException
    {
        Node a = new Node("a");
        Node b = new Node("b");
        Node c = new Node("c");
        Node d = new Node("d");
        Node e = new Node("e");
        Node f = new Node("f");
        Node g = new Node("g");
        Node h = new Node("h");

/*
        a.addParent(b);
        b.addParent(c);
        b.addParent(d);

        a.traverse();
*/

        a.addParent(b);
        a.addParent(c);

        b.addParent(d);
        c.addParent(d);
        c.addParent(e);

        e.addParent(f);


        a.traverse();

    }
}
