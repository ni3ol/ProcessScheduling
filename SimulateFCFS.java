import java.text.DecimalFormat;
import java.util.Scanner;
import simulator.Config;
import simulator.KernelFCFS;
import simulator.TRACE;

public class SimulateFCFS {
	
	public static void main (String[]args) {
		
		Scanner sc = new Scanner(System.in);
		
		System.out.println("*** FCFS Simulator ***");
		System.out.print("Enter configuration file name: ");
		String fileName = sc.next();
		//String fileName = "configTEST.cfg";
		System.out.print("Enter cost of system call: ");
		int sysCallCost = Integer.parseInt(sc.next());
		//int sysCallCost = 1;
		System.out.print("Enter cost of context switch: ");
		int contextSwitchCost = Integer.parseInt(sc.next());
		//int contextSwitchCost = 3;
		System.out.print("Enter trace level: ");
		int trace = Integer.parseInt(sc.next());
		//int trace = 256;
		KernelFCFS kernel = new KernelFCFS();
		TRACE.SET_TRACE_LEVEL(trace);
		Config.init(kernel, contextSwitchCost, sysCallCost);
		Config.buildConfiguration(fileName);

		Config.run();
		
	
		//System.out.println("*** Results ***");
		System.out.println(Config.getSystemTimer());
	    System.out.println("Context switches: " + Config.getCPU().getContextSwitches());
	    float util = (float)Config.getSystemTimer().getUserTime() / Config.getSystemTimer().getSystemTime();
	    DecimalFormat df = new DecimalFormat("###.##");
	    System.out.println("CPU utilization: " + df.format(util*100));
	    
	    sc.close();
	}
	
}
