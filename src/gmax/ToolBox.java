package gmax;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.provisioners.*;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Random;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;

public class ToolBox
{
	private static List<Cloudlet> cloudletList = new ArrayList<Cloudlet>(); ;
	private static List<Vm> vmList= new ArrayList<Vm>();;

	//1.初始化
	//2.runSimulation_PSO()
	public static void Runtest(String dataFilePath,int taskNum)
	{
		Log.printLine("Starting to run simulations...");

		try
		{
			int num_user = 1; // number of cloud users
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;
		
			CloudSim.init(num_user, calendar, trace_flag);

			@SuppressWarnings("unused")
			Datacenter datacenter0 = createDatacenter("Datacenter_0");
			// #3 step: Create Broker
			DatacenterBroker broker = createBroker();
			int brokerId = broker.getId();
			// #4 step: Create 5 virtual machines
			// VM description
			long size = 10000; // image size (MB)
			int ram = 512; // vm memory (MB)
			long bw = 1000;
			int pesNumber = 1; // number of cpus
			String vmm = "Xen"; // VMM name

			Vm vm1 = new Vm(0, brokerId, 5000, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());
			Vm vm2 = new Vm(1, brokerId, 2500, pesNumber, ram, bw, size,
					vmm,new CloudletSchedulerTimeShared());
			Vm vm3 = new Vm(2, brokerId, 2500, pesNumber, ram, bw, size,
					vmm,new CloudletSchedulerTimeShared());
			Vm vm4 = new Vm(3, brokerId, 1500, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());
			Vm vm5 = new Vm(4, brokerId, 1000, pesNumber, ram, bw, size,
					vmm, new CloudletSchedulerSpaceShared());

			// add the VMs to the vmList
			vmList.add(vm1);
			vmList.add(vm2);
			vmList.add(vm3);
			vmList.add(vm4);
			vmList.add(vm5);

			// submit vm list to the broker
			broker.submitVmList(vmList);
			//create cloudlets and submit them.
			createTasks(brokerId,dataFilePath,taskNum);
			broker.submitCloudletList(cloudletList);
			
			runSimulation_PSO(broker);
			
			Log.printLine("\nThe simulation is finished!");
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.printLine("The simulation has been terminated due to an unexpected error");
		}
	}
	
	public static void runSimulation_PSO(DatacenterBroker broker)
	{
		int gmax1=50;
		int gmax2=10;
		
		//GA调度算法的参数
		int popsize=200;
		double crossoverProb=0.8;
		double mutationRate=0.01;
		
		//PSO调度算法的参数
		//c1,c2参数没有用到，多余了
		double w=0.9;
		double c1=2.0;
		double c2=2.0;
		//执行PSO调度方案
		//applyPSOscheduling(w,c1,c2,gmax);
		
		//计算时间
		long startTimeGA=System.currentTimeMillis();
		//执行GA调度方案
		applyGAscheduling(popsize,gmax1,crossoverProb,mutationRate);
		long endTimeGA=System.currentTimeMillis();
		
		//普通调度
		CloudSim.startSimulation();
		// Final step: Print results when simulation is over
		List<Cloudlet> newList = broker.getCloudletReceivedList();
		CloudSim.stopSimulation();
		for(Vm vm:vmList)
		{
			Log.printLine(String.format("vm id= %s ,mips = %s ",vm.getId(),vm.getMips()));
		}
		String finishTm=printCloudletList(newList);
		System.out.println("This (GA) schedule plan takes "+finishTm+" ms to finish execution.(gmax="+gmax1+")");
		System.out.println("GA算法用时"+(endTimeGA-startTimeGA)+ "ms");
		
		//由于每次执行PSO调度算法的调度结果都不同(由于PSO过程中加入了随机性,甚至可能比RR还差),以下3行代码是取n次调度方案来计算PSO结果的平均执行时间.
		int n=1;
		//计算时间
		long startTimePSO=System.currentTimeMillis();
		double avgRuntime=getAvgRuntimeByPSOscheduling(n,w,c1,c2,gmax2);
		long endTimePSO=System.currentTimeMillis();
		
		//System.out.println(String.format("==============Printing the average runningtime PSO schedule plans ===================\nAvg runtime of (n=%d) PSO schedule plans:%.2f ms.",n,avgRuntime+0.1));//模拟中从0.1开始运行，所以+0.1
		System.out.println(String.format("\nThis (PSO) schedule plan takes %.2f ms to finish execution.(gmax=%d)",avgRuntime+0.1,gmax2));
		System.out.println("PSO算法用时"+(endTimePSO-startTimePSO)+ "ms");
		
		
//		double[] avgRuntimes = new double[51];
//		for(int i=1;i<=50;i++)
//		{
//			gmax=i;
//			avgRuntimes[i]=getAvgRuntimeByPSOscheduling(n,w,c1,c2,gmax);
		
//		}
//		System.out.println("不同迭代次数的结果：");
//		for(int i=1;i<=50;i++)
//		{
//			System.out.print(i+"\t");
//		}
//		System.out.println();
//		for(int i=1;i<=50;i++)
//		{
//			System.out.print(String.format("%.2f ms\t",avgRuntimes[i]+0.1));
//		}
		
	}
	
	public static double getAvgRuntimeByPSOscheduling(int times,double w,double c1,double c2,int gmax)
	{
		double sum=0;
		for(int i=0;i<times;i++)
		{
			int[] schedule=getScheduleByPSO(w,c1,c2,gmax);
			//这里可以改成模拟运行时间
			sum+=getFitness(schedule);
		}
		return sum/times;
	}
	
	public static void applyPSOscheduling(double w,double c1,double c2,int gmax)
	{
		int []schedule=getScheduleByPSO(w,c1,c2,gmax);
		assignResourcesWithSchedule(schedule);
	}
	
	public static void assignResourcesWithSchedule(int []schedule)
	{
		for(int i=0;i<schedule.length;i++)
		{
			getCloudletById(i).setVmId(schedule[i]);//Specifies that a given cloudlet must run in a specific virtual machine
		}
	}


	
	private static int[] getScheduleByPSO(double w,double c1,double c2,int gmax)
	{
		int[] schedule=initPopsRandomly(cloudletList.size(),vmList.size());
		schedule=PSO(schedule,w,c1,c2,gmax);
		return schedule;
	}
	
	private static int[] initPopsRandomly(int taskNum,int vmNum)
	{
		//data structure for saving a schedule：array,index of array are cloudlet id,content of array are vm id.
		int[] schedule=new int[taskNum];
		for(int j=0;j<taskNum;j++)
		{
			schedule[j]=new Random().nextInt(vmNum);//随机分配到0~vmNum号VM
		}
		
		return schedule;
	}
	
	private static double getFitness(int[] schedule)
	{
		double fitness=0;

		//vmTasks<vm号，在该vm上运行的任务号>
		HashMap<Integer,ArrayList<Integer>> vmTasks=new HashMap<Integer,ArrayList<Integer>>();
		int size=cloudletList.size();
		
		//将任务号加到对应vm号的ArrayList
		for(int i=0;i<size;i++)
		{
			if(!vmTasks.keySet().contains(schedule[i]))
			{
				ArrayList<Integer> taskList=new ArrayList<Integer>();
				taskList.add(i);
				vmTasks.put(schedule[i],taskList);
			}
			else
			{
				vmTasks.get(schedule[i]).add(i);
			}
		}

		for(Entry<Integer, ArrayList<Integer>> vmtask:vmTasks.entrySet())
		{
			int length=0;
			for(Integer taskid:vmtask.getValue())
			{
				length+=getCloudletById(taskid).getCloudletLength();
			}
			
			double runtime=length/getVmById(vmtask.getKey()).getMips();
			if (fitness<runtime)
			{
				fitness=runtime;
			}
		}
		
		return fitness;
	}

	private static int[] PSO(int[] schedule,double w,double c1,double c2,int gmax)
	{
		
		int[] newschedule=schedule;
		
		//计算pbest
		int[] pbest=getpbest(schedule);
		
		//计算gbest
		int[] gbest=getgbest(schedule);
		
		
		//更新位置
		newschedule=updateposition(pbest,gbest,w,c1,c2);
		
		//更新w
		w=w-0.014*(50-gmax);
		
		gmax--;
		return gmax > 0 ? PSO(newschedule,w,c1,c2,gmax): newschedule;
	}

	
	private static int[] getpbest(int[] schedule)
	{
		int[] pbest=schedule;
		for(int i=0;i<schedule.length;i++)
		{
			int[] temp=schedule;
			int index=temp[i];
			
			
			//vmTasks<vm号，在该vm上运行的任务号>
			HashMap<Integer,ArrayList<Integer>> vmTasks=new HashMap<Integer,ArrayList<Integer>>();
			int size=cloudletList.size();
			
			//将任务号加到对应vm号的ArrayList
			for(int j=0;j<size;j++)
			{
				//不加入当前任务
				if(j==i)
				{
					continue;
				}
				
				if(!vmTasks.keySet().contains(schedule[j]))
				{
					ArrayList<Integer> taskList=new ArrayList<Integer>();
					taskList.add(j);
					vmTasks.put(schedule[j],taskList);
				}
				else
				{
					vmTasks.get(schedule[j]).add(j);
				}
			}

			double fitness=1000000;
			for(Entry<Integer, ArrayList<Integer>> vmtask:vmTasks.entrySet())
			{
				int length=0;
				for(Integer taskid:vmtask.getValue())
				{
					length+=getCloudletById(taskid).getCloudletLength();
				}
				
				double runtime=length/getVmById(vmtask.getKey()).getMips();
				if (fitness>runtime)
				{
					fitness=runtime;
					index=vmtask.getKey();
				}
			}
			
			
			pbest[i]=index;
		}
		
		//打印pbest
//		System.out.println();
//		for(int i=0;i<pbest.length;i++)
//		{
//			System.out.print(pbest[i]);
//		}
//		System.out.println();
//		System.out.println(String.format("%.2f ms_pbest", getFitness(pbest)));
		
		return pbest;
	}
	
	private static int[] getgbest(int[] schedule)
	{
		int[] gbest=schedule;
		for(int i=0;i<schedule.length;i++)
		{
			int[] temp=schedule;
			int index=temp[i];
			double max=1000000;
			double fitness;
			for(int j=0;j<5;j++)
			{
				temp[i]=j;
				fitness=getFitness(temp);
				if(fitness<max)
				{
					index=j;
					max=fitness;
				}
			}
			gbest[i]=index;
		}
		
		//打印gbest
		//System.out.println();
//		for(int i=0;i<gbest.length;i++)
//		{
//			System.out.print(gbest[i]);
//		}
//		System.out.println();
//		System.out.println(String.format("%.2f ms_gbest", getFitness(gbest)));
//		//System.out.println(getFitness(gbest)+"ms_gbest");
		
		return gbest;
	}

	private static int[] updateposition(int[] pbest,int[] gbest,double w,double c1,double c2)
	{
		
		//v[i] = w * v[i] + c1 * rand() * (pbest[i] - present[i]) + c2 * rand() * (gbest - present[i])    
		//present[i] = present[i] + v[i] 
		//以上公式不能直接用
		
		//根据w，按一定几率变为pbest或gbest
		//c1,c2参数没有用到
		
		int[] newschedule=gbest;
		
		Random rand = new Random();
		for(int i=0;i<pbest.length;i++)
		{
			
			double random=rand.nextDouble();
			
			if(random<w)
			{
				newschedule[i]=pbest[i];
			}
			else
			{
				newschedule[i]=gbest[i];
			}
			
		}
		
		return newschedule;
	}

	
	
	//GA
	public static void applyGAscheduling(int popSize,int gmax,double crossoverProb,double mutationRate)
	{
		int []schedule=getScheduleByGA(popSize,gmax,crossoverProb,mutationRate);
		assignResourcesWithSchedule(schedule);
	}
	
	private static int[] getScheduleByGA(int popSize,int gmax,double crossoverProb,double mutationRate)
	{
		ArrayList<int[]> pop=initPopsRandomlyGA(cloudletList.size(),vmList.size(),popSize);
		pop=GA(pop,gmax,crossoverProb,mutationRate);
		return findBestSchedule(pop);
	}
	
	private static int[] findBestSchedule(ArrayList<int[]> pop)
	{
		double bestFitness=1000000000;
		int bestIndex=0;
		for(int i=0;i<pop.size();i++)
		{
			int []schedule=pop.get(i);
			double fitness=getFitness(schedule);
			if(bestFitness>fitness)
			{
				bestFitness=fitness;
				bestIndex=i;
			}
		}
		return pop.get(bestIndex);
	}	
	
	private static ArrayList<int[]> initPopsRandomlyGA(int taskNum,int vmNum,int popsize)
	{
		ArrayList<int[]> schedules=new ArrayList<int[]>();
//这里popsize有问题		
		for(int i=0;i<popsize;i++)
		{
			//data structure for saving a schedule：array,index of array are cloudlet id,content of array are vm id.
			int[] schedule=new int[taskNum];
			for(int j=0;j<taskNum;j++)
			{
				schedule[j]=new Random().nextInt(vmNum);
			}
			schedules.add(schedule);
		}
		return schedules;
	}
	
	private static ArrayList<int[]> GA(ArrayList<int[]> pop,int gmax,double crossoverProb,double mutationRate)
	{
		HashMap<Integer,double[]> segmentForEach=calcSelectionProbs(pop);
		ArrayList<int[]> children=new ArrayList<int[]>();
		ArrayList<int[]> tempParents=new ArrayList<int[]>();
		while(children.size()<pop.size())
		{	
			//selection phase:select two parents each time.
			for(int i=0;i<2;i++)
			{
				double prob = new Random().nextDouble();
				for (int j = 0; j < pop.size(); j++)
				{
					if (isBetween(prob, segmentForEach.get(j)))
					{
						tempParents.add(pop.get(j));
						break;
					}
				}
			}
			//cross-over phase.
			int[] p1,p2,p1temp,p2temp;
			p1= tempParents.get(tempParents.size() - 2).clone();
			p1temp= tempParents.get(tempParents.size() - 2).clone();
			p2 = tempParents.get(tempParents.size() -1).clone();
			p2temp = tempParents.get(tempParents.size() -1).clone();
			if(new Random().nextDouble()<crossoverProb)
			{
				int crossPosition = new Random().nextInt(cloudletList.size() - 1);
				//cross-over operation
				for (int i = crossPosition + 1; i < cloudletList.size(); i++)
				{
					int temp = p1temp[i];
					p1temp[i] = p2temp[i];
					p2temp[i] = temp;
				}
			}
			//choose the children if they are better,else keep parents in next iteration.
			children.add(getFitness(p1temp) < getFitness(p1) ? p1temp : p1);
			children.add(getFitness(p2temp) < getFitness(p2) ? p2temp : p2);	
			// mutation phase.
			if (new Random().nextDouble() < mutationRate)
			{
				// mutation operations bellow.
				int maxIndex = children.size() - 1;

				for (int i = maxIndex - 1; i <= maxIndex; i++)
				{
					operateMutation(children.get(i));
				}
			}
		}
		
		gmax--;
		return gmax > 0 ? GA(children, gmax, crossoverProb, mutationRate): children;
	}
	
	public static void operateMutation(int []child)
	{
		int mutationIndex = new Random().nextInt(cloudletList.size());
		int newVmId = new Random().nextInt(vmList.size());
		while (child[mutationIndex] == newVmId)
		{
			newVmId = new Random().nextInt(vmList.size());
		}

		child[mutationIndex] = newVmId;
	}
	
	private static boolean isBetween(double prob,double[]segment)
	{
		if(segment[0]<=prob&&prob<=segment[1])
			return true;
		return false;	
	}
	
	private static HashMap<Integer,double[]> calcSelectionProbs(ArrayList<int[]> parents)
	{
		int size=parents.size();
		double totalFitness=0;	
		ArrayList<Double> fits=new ArrayList<Double>();
		HashMap<Integer,Double> probs=new HashMap<Integer,Double>();
		
		for(int i=0;i<size;i++)
		{
			double fitness=getFitness(parents.get(i));
			fits.add(fitness);
			totalFitness+=fitness;
		}
		for(int i=0;i<size;i++)
		{
			probs.put(i,fits.get(i)/totalFitness );
		}
		
		return getSegments(probs);
	}
	
	private static HashMap<Integer,double[]> getSegments(HashMap<Integer,Double> probs)
	{
		HashMap<Integer,double[]> probSegments=new HashMap<Integer,double[]>();
		//probSegments保存每个个体的选择概率的起点、终点，以便选择作为交配元素。
		int size=probs.size();
		double start=0;
		double end=0;
		for(int i=0;i<size;i++)
		{
			end=start+probs.get(i);
			double[]segment=new double[2];
			segment[0]=start;
			segment[1]=end;
			probSegments.put(i, segment);
			start=end;
		}
		
		return probSegments;
	}
	
	
	
	
	
	
	//基础	
	private static void createTasks(int brokerId,String filePath, int taskNum)
	{
		try
		{
			@SuppressWarnings("resource")
			BufferedReader br= new BufferedReader(new InputStreamReader(new FileInputStream(filePath)));
			String data = null;
			int index = 0;
			
			//cloudlet properties.
			int pesNumber = 1;
			long fileSize = 1000;
			long outputSize = 1000;
			UtilizationModel utilizationModel = new UtilizationModelFull();

			while ((data = br.readLine()) != null)
			{
				System.out.println(data);
				String[] taskLength=data.split("\t");
				for(int j=0;j<20;j++){
					Cloudlet task=new Cloudlet(index+j, (long) Double.parseDouble(taskLength[j]), pesNumber, fileSize,
							outputSize, utilizationModel, utilizationModel,
							utilizationModel);
					task.setUserId(brokerId);
					cloudletList.add(task);
					if(cloudletList.size()==taskNum)
					{	
						br.close();
						return;
					}
				}
				//20 cloudlets each line in the file cloudlets.txt.
				index+=20;
			}
			
		} 
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	private static Datacenter createDatacenter(String name)
	{
		List<Host> hostList = new ArrayList<Host>();
		List<Pe> peList = new ArrayList<Pe>();
		
		int mips = 5000;
		peList.add(new Pe(0, new PeProvisionerSimple(mips))); // need to store MIPS Rating
		
		mips = 2500;
		peList.add(new Pe(1, new PeProvisionerSimple(mips))); 
		
		mips = 2500;
		peList.add(new Pe(2, new PeProvisionerSimple(mips))); 
		
		mips = 1500;
		peList.add(new Pe(3, new PeProvisionerSimple(mips)));
			
		mips = 1000;
		peList.add(new Pe(4, new PeProvisionerSimple(mips))); 
													
		int hostId = 0;
		int ram = 4096; // host memory (MB)
		long storage = 10000000; // host storage
		int bw = 10000;

		hostList.add(new Host(hostId, new RamProvisionerSimple(ram),
				new BwProvisionerSimple(bw), storage, peList,
				new VmSchedulerTimeShared(peList)));
		String arch = "x86"; // system architecture
		String os = "Linux"; // operating system
		String vmm = "Xen";
		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this
										// resource
		double costPerBw = 0.001; // the cost of using bw in this resource
		
		//we are not adding SAN devices by now
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
				arch, os, vmm, hostList, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		// 6. Finally, we need to create a PowerDatacenter object.
		Datacenter datacenter = null;
		try
		{
			datacenter = new Datacenter(name, characteristics,
					new VmAllocationPolicySimple(hostList), storageList, 0);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		return datacenter;
	}

	private static DatacenterBroker createBroker()
	{

		DatacenterBroker broker = null;
		try
		{
			broker = new DatacenterBroker("Broker");
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		return broker;
	}

	private static String printCloudletList(List<Cloudlet> list)
	{
		int size = list.size();
		Cloudlet cloudlet;

		String indent = "    ";
		Log.printLine();
		Log.printLine("================ Execution Result ==================");
		Log.printLine("No."+indent +"Cloudlet ID" + indent + "STATUS" + indent
				+ "Data center ID" + indent + "VM ID" + indent+"VM mips"+ indent +"CloudletLength"+indent+ "Time"
				+ indent + "Start Time" + indent + "Finish Time");

		DecimalFormat dft = new DecimalFormat("###.##");
		for (int i = 0; i < size; i++)
		{
			cloudlet = list.get(i);
			Log.print(i+1+indent+indent + cloudlet.getCloudletId() + indent + indent);

			if (cloudlet.getStatus()== Cloudlet.SUCCESS)
			{
				Log.print("SUCCESS");

				Log.printLine(indent +indent + indent + cloudlet.getResourceId()
						+ indent + indent + indent + cloudlet.getVmId()
						+ indent + indent + getVmById(cloudlet.getVmId()).getMips()
						+ indent + indent + cloudlet.getCloudletLength()
						+ indent + indent+ indent + indent
						+ dft.format(cloudlet.getActualCPUTime()) + indent
						+ indent + dft.format(cloudlet.getExecStartTime())
						+ indent + indent
						+ dft.format(cloudlet.getFinishTime()));
			}
		}
		Log.printLine("================ Execution Result Ends here ==================");
		//最后完成的任务的完成时刻就是调度方案的总执行时间
		return dft.format(list.get(size-1).getFinishTime());
	}

	public static Vm getVmById(int vmId)
	{
		for(Vm v:vmList)
		{
			if(v.getId()==vmId)
				return v;
		}
		return null;
	}
	
	public static Cloudlet getCloudletById(int id)
	{
		for(Cloudlet c:cloudletList)
		{
			if(c.getCloudletId()==id)
				return c;
		}
		return null;
	}
	
	public static void writeTxtAppend(String file, String conent)
	{
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true)));
			out.write(conent + "\r\n");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try
			{
				out.close();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
}

