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
    public Boolean result;
    public Boolean isTraversed;
    public String name;
    public List<Node> parentList;
    public List<Node> childrenList;
    public Thread thread;
    public final Integer EXECUTION_TIME_MS = 1500;
    private Thread threadStart()
    {
        synchronized (this.thread)
        {
            if (!this.isTraversed)
            {
                this.thread.start();
                this.isTraversed = true;
            }

            return this.thread;
        }
    }

    public Node(String _name)
    {
        this.name = _name;
        this.thread = new Thread(this);
        this.thread.setName(this.name);
        this.parentList = new ArrayList<Node>();
        this.childrenList = new ArrayList<Node>();
        this.result = false;
        this.isTraversed = false;
    }

    public void addParent(Node _parent)
    {
        this.parentList.add(_parent);
    }

    public void addChild(Node _child)
    {
        this.childrenList.add(_child);
    }

    public Boolean traverse() throws InterruptedException
    {
        synchronized (this.thread)
        {
            this.isTraversed = true;

            //if (parentList.size() == 1)
            if (!parentList.isEmpty())
            {
//                this.result = parentList.get(0).traverse();
//            }
//            else if (1 < parentList.size())
//            {
                List<Thread> threadList = new ArrayList<>();

                for (Node parent : this.parentList)
                {
                    threadList.add(parent.threadStart());
                }

                for (Thread thread : threadList)
                {
                    thread.join();
                }

                this.result = true;
                for (Node parent : this.parentList)
                {
                    this.result &= parent.result;
                }
            }
            else
            {// this is the top most node
                this.result = true;
            }

            if (this.result)
            {
                try
                {
                    //System.out.println(this.name + " sleeping");
                    Thread.sleep(EXECUTION_TIME_MS);
                    System.out.println("< " + this.name + " >");
                } catch (Exception ex)
                {

                }
            }
        }

        return this.result; // if execution successful
    }

    public void run()
    {
        try
        {
            this.traverse();
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

        a.addParent(b);
        a.addParent(c);

        b.addParent(d);
        c.addParent(d);
        c.addParent(e);

        e.addParent(f);


        a.traverse();
    }
}
