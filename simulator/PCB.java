package simulator;
import java.util.LinkedList;

public class PCB implements ProcessControlBlock 
{

	int pID;
	static int counter = 1;
	String programName;
	int priority;
	Instruction instruction;
	State state;
	LinkedList<Instruction> list;
	
	public PCB(String name, int priority) 
	{
		this.programName = name;
		this.priority = priority;
		pID = counter++;
		state = State.READY;
		instruction = null; 
		list  = new LinkedList<Instruction>();
	}
	
	public int getPID() 
	{
		return pID;
	}

	public String getProgramName() 
	{
		
		return programName;
	}

	public int getPriority() 
	{
		return priority;
	}

	public int setPriority(int value) 
	{
		int p = priority;   //old value
		priority = value;
		return p;
	}

	public Instruction getInstruction() 
	{
		return instruction;
	}

	public boolean hasNextInstruction() 
	{
		return (!list.isEmpty());
	}

	public void nextInstruction() 
	{
		instruction = list.poll();
	}
	
	public void addInstruction(Instruction i) 
	{
		if (instruction == null) 
		{
			instruction = i; 
		}
		else 
		{
			list.add(i);
		}
	}

	public State getState() 
	{
		return state;
	}

	public void setState(State state) 
	{
		this.state = state;
	}
	
	public String toString() 
	{
		return "process(pid=" + pID + ", state=" + state + ", name=" + '"' + programName + '"' + ")";
	}
}
