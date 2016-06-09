import java.text.DecimalFormat;
import java.util.Scanner;
import simulator.Config;
import simulator.KernelRR;
import simulator.TRACE;

public class SimulateRR {
		
		public static void main (String[]args) {
			
			Scanner sc = new Scanner(System.in);
			
			System.out.println("*** RR Simulator ***");
			System.out.print("Enter configuration file name: ");
			String fileName = sc.next();
			//String fileName = "Test2C/config.cfg";
			System.out.print("Enter slice time: ");			
			int timeslice =  Integer.parseInt(sc.next());
			System.out.print("Enter cost of system call: ");
			int sysCallCost = Integer.parseInt(sc.next());
			//int sysCallCost = 1;
			System.out.print("Enter cost of context switch: ");
			int contextSwitchCost = Integer.parseInt(sc.next());
			//int contextSwitchCost = 3;
			System.out.print("Enter trace level: ");
			int trace = Integer.parseInt(sc.next());
			//int trace = 31;
			KernelRR kernel = new KernelRR(timeslice + contextSwitchCost);
			TRACE.SET_TRACE_LEVEL(trace);
			Config.init(kernel, contextSwitchCost, sysCallCost);
			Config.buildConfiguration(fileName);
			Config.run();
			
			//if trace > 0, print trace
			//System.out.println("*** Trace ***");
			//System.out.println();
			
			//System.out.println("*** Results ***");
			System.out.println(Config.getSystemTimer());
		    System.out.println("Context switches: " + Config.getCPU().getContextSwitches());
		    float util = (float)Config.getSystemTimer().getUserTime() * 100 / Config.getSystemTimer().getSystemTime();
		    System.out.println("CPU utilization: " + String.format("%.2f", util));
		}
}
