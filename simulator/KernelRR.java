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

public class KernelRR implements Kernel 
{
	    private Deque<ProcessControlBlock> readyQueue;
	    private int timeslice;
	        
	    public KernelRR(int timeslice) 
	    {
			// Set up the ready queue.
	    	readyQueue = new ArrayDeque<ProcessControlBlock>();
	    	this.timeslice = timeslice; 
	    }
	    
	    private ProcessControlBlock dispatch() 
	    {	
	    	//Get current process on CPU
	    	ProcessControlBlock pcb = Config.getCPU().getCurrentProcess();
	    	ProcessControlBlock switching = null; 
	    	
	    	//If a process on the CPU exists
	    	if (pcb != null)
	    	{
	    		if (pcb.getState() == State.RUNNING) 
	    		{
	    			if (readyQueue.peekFirst() != null) 
	    			{
		    			Config.getSimulationClock().scheduleInterrupt(timeslice,this,readyQueue.peekFirst().getPID());
		    			
		    			switching = Config.getCPU().contextSwitch(readyQueue.peekFirst());
		    			readyQueue.poll().setState(State.RUNNING);
		    			
	    			}
	    			else if(readyQueue.peekFirst() == null)
	    			{
	    				Config.getSimulationClock().scheduleInterrupt(timeslice,this,pcb.getPID());
		    			switching =  Config.getCPU().contextSwitch(pcb); 	    			
	    			}
	    		}
	    	
	    		else if (pcb.getState() == State.READY) 
	    		{
	    			readyQueue.addLast(pcb);
	    			Config.getSimulationClock().scheduleInterrupt(timeslice,this,readyQueue.peekFirst().getPID());
	    			
	    			switching = Config.getCPU().contextSwitch(readyQueue.peekFirst()); 
	    			readyQueue.poll().setState(State.RUNNING);	    	    			
	   			}
	    		
	   			else if (pcb.getState() == State.WAITING || pcb.getState() == State.TERMINATED)
	   			{
	   				if (readyQueue.peek() == null) 
	    			{
	    				switching = Config.getCPU().contextSwitch(readyQueue.poll());    
	    			}
	    			else if (readyQueue.peek() != null) 
	   				{
	   					Config.getSimulationClock().scheduleInterrupt(timeslice,this,readyQueue.peekFirst().getPID());
	   					
	   	    			switching = Config.getCPU().contextSwitch(readyQueue.peekFirst()); 
	   	    			readyQueue.poll().setState(State.RUNNING);	   	    		
	   				}
	    		}
	    	}
	    	else 
	    	{
	    		if (readyQueue.peekFirst() != null)
	    		{
	    			Config.getSimulationClock().scheduleInterrupt(timeslice,this,readyQueue.peekFirst().getPID());
	    			
   	    			switching = Config.getCPU().contextSwitch(readyQueue.peekFirst()); 
  	    			readyQueue.poll().setState(State.RUNNING);
	    		}
	    		else
	    		{
	    			switching = null;
	    		}
	    	}	    	
	    	return switching;
	    }
	               
	    public int syscall(int number, Object... varargs) 
	    {
	        int result = 0;
	        switch (number) {
	             case MAKE_DEVICE:
	                {
	                    IODevice device = new IODevice((Integer)varargs[0], (String)varargs[1]);
	                    Config.addDevice(device);
	                }
	                break;
	             case EXECVE: 
	                {
	                    ProcessControlBlock pcb = KernelRR.loadProgram((String)varargs[0]);
	                    pcb.setPriority((Integer)(varargs[1]));
	                    if (pcb!=null) 
	                    {
	                        // Loaded successfully.
							// Now add to end of ready queue.
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
	                	Config.getSystemTimer().cancelInterrupt(pcb.getPID());
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
	                    Config.getSystemTimer().cancelInterrupt(pcb.getPID());
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
	            {
	            	ProcessControlBlock pcb = Config.getCPU().getCurrentProcess();
	            	if (readyQueue.peek() == null)
	            	{
	            		if (pcb != null)
	            		{
	            			pcb.setState(State.RUNNING);
	            			dispatch();
	            		}
	            	}
	            	else
	            	{
	            		pcb.setState(State.READY);
	            		dispatch();
	            	}
	            	break;
	            }
	                
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
	                throw new IllegalArgumentException("KernelRR:interrupt("+interruptType+"...): unknown type.");
	        }
	    }
	       
	    private static ProcessControlBlock loadProgram(String filename) 
	    {
	        try 
	        {
	        	PCB pcb = new PCB(filename, 0);
	        	final BufferedReader reader = new BufferedReader(new FileReader(filename));
	        	String line = reader.readLine();
	        	String[] l = line.split(" ");
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
