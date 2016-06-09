package simulator;

import simulator.Config;
import simulator.IODevice;
import simulator.Kernel;
import simulator.ProcessControlBlock;
import simulator.ProcessControlBlock.State;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;



/**
 * Concrete Kernel type
 * 
 * @author Stephan Jamieson
 * @version 8/3/15
 */
public class KernelFCFS implements Kernel 
{
    
    private Deque<ProcessControlBlock> readyQueue;
        
    public KernelFCFS() 
    {
		// Set up the ready queue.
    	readyQueue = new ArrayDeque<ProcessControlBlock>();
    }
    
    private ProcessControlBlock dispatch() 
    {
		// Perform context switch, swapping process
		// currently on CPU with one at front of ready queue.
		// If ready queue empty then CPU goes idle ( holds a null value).
    	// Returns process removed from CPU.
    	
    	ProcessControlBlock ready = readyQueue.poll();
    	ProcessControlBlock switched = null; 
    	
    	if (ready == null)
    	{
    		switched = Config.getCPU().contextSwitch(null);
    	}
    	if (ready != null) 
    	{
    		if (ready.getState() == State.READY)
    		{
    			switched = Config.getCPU().contextSwitch(ready);
    			ready.setState(State.RUNNING);
    		}
    	}
    	return switched;
    }
    	
   
    public int syscall(int number, Object... varargs) 
    {
        int result = 0;
        switch (number) 
        {
             case MAKE_DEVICE:
                {
                    IODevice device = new IODevice((Integer)varargs[0], (String)varargs[1]);
                    Config.addDevice(device);
                
                break;
                }
             
             case EXECVE: 
                {
                    ProcessControlBlock pcb = KernelFCFS.loadProgram((String)varargs[0]);
                    pcb.setPriority((Integer)(varargs[1]));
                    if (pcb!=null) 
                    {
                        // Loaded successfully and add to end of ready queue.
                    	readyQueue.addLast(pcb);
						// If CPU idle then call dispatch.
                    	if (Config.getCPU().isIdle() == true) 
                    		{    
                    			dispatch();
                    			pcb.setState(State.RUNNING);
                    		}                
                    	else 
                    	{
                    		result = -1;
                    	}
                    }
                }
                break;
             
             case IO_REQUEST: 
                {
					// IO request has come from process currently on the CPU.
					// Get PCB from CPU.
                	ProcessControlBlock pcb =Config.getCPU().getCurrentProcess();
					// Find IODevice with given ID: Config.getDevice((Integer)varargs[0]);
                	IODevice device = Config.getDevice((Integer)varargs[0]);
					// Make IO request on device providing burst time (varages[1]),
					// the PCB of the requesting process, and a reference to this kernel 
                	device.requestIO((int)varargs[1], pcb, this);
                	//(so // that the IODevice can call interrupt() when the request is completed.
					// Set the PCB state of the requesting process to WAITING.
                	pcb.setState(State.WAITING);
					// Call dispatch().
                	dispatch();
                }
                break;
             
             case TERMINATE_PROCESS:
                {
					// Process on the CPU has terminated.
					// Get PCB from CPU.            
                	ProcessControlBlock pcb = Config.getCPU().getCurrentProcess();
                	// Set status to TERMINATED.
                    pcb.setState(State.TERMINATED);
                    // Call dispatch().
                	dispatch();
                }
                break;
             default:
                result = -1;
        }
        return result;
    }
   
    
    public void interrupt(int interruptType, Object... varargs){
        switch (interruptType) {
            case TIME_OUT:
                throw new IllegalArgumentException("KernelFCFS:interrupt("+interruptType+"...): this kernel does not suppor timeouts.");
            case WAKE_UP:
				// IODevice has finished an IO request for a process.
				// Retrieve the PCB of the process (varargs[1]), set its state
				// to READY, put it on the end of the ready queue.
            	ProcessControlBlock pcb = (ProcessControlBlock)varargs[1];
            	pcb.setState(State.READY);
            	readyQueue.add(pcb);
				// If CPU is idle then dispatch().
            	if (Config.getCPU().isIdle() == true) 
            	{
            		dispatch(); 
            	}
                break;
            default:
                throw new IllegalArgumentException("KernelFCFS:interrupt("+interruptType+"...): unknown type.");
        }
    }
       
    private static ProcessControlBlock loadProgram(String filename) 
    {
        try 
        {
        	PCB pcb = new PCB(filename, 0);
        	final BufferedReader reader = new BufferedReader(new FileReader(filename));
        	//read line from file
        	String line = reader.readLine();
        	//split line into separate works
        	String[] l = line.split(" ");
        	//loop through lines as line as lines exist
            while (line!=null) 
            {
            	if (l[0] == "#") 
            	{
            		//Ignore, is a comment line 
            	}
            	else if (l[0].equals("CPU")) 
                {		
            		int duration = Integer.parseInt(l[1]);
            		CPUInstruction cpu = new CPUInstruction(duration);
            		if (cpu != null) 
            		{
            		pcb.addInstruction(cpu);
            		}
                }
                else if (l[0].equals("IO")) 
                {
                   int duration = Integer.parseInt(l[1]);
                   int id = Integer.parseInt(l[2]);
                   IOInstruction io = new IOInstruction(duration, id);
                   if (io != null)
                   {              	  
                   pcb.addInstruction(io);
                   }
                }
            	line = reader.readLine();
            	if (line != null) 
            	{
            		l = line.split(" ");
            	}
            }
            reader.close();
            return pcb;
        }
        catch (FileNotFoundException fileExp) 
        {
            return null;
        }
        catch (IOException ioExp) 
        {
            return null;
        }
    }
}
