package AllAlgorithm;

import java.text.DecimalFormat;
import java.util.Random;

public class RunAlgorithm
{
	public static void main(String[] args)
	{
		
//		String testData="data\\cloudlets.txt";
//		DecimalFormat dft = new DecimalFormat("###.##");
//		//createTestData(testData);
//		int taskNum=60;
//		long startGA=System.currentTimeMillis();
//		String finishTmGA=Ga.Runtest(testData,taskNum);
//		long finishGA=System.currentTimeMillis();
//		long trainTmGA=finishGA-startGA;
//		long startACA=System.currentTimeMillis();
//		String finishTmACA=ACA.Runtest(testData,taskNum);
//		long finishACA=System.currentTimeMillis();
//		long trainTmACA=finishACA-startACA;
//		printSpendTime(finishTmGA,trainTmGA,"GA");
//		printSpendTime(finishTmACA,trainTmACA,"ACA");
	}
	public static void printSpendTime(String finishTm,long trainTm,String algorithm) {
		System.out.println("This schedule plan takes "+finishTm+" ms to finish execution, takes "+trainTm+"ms to train in "+algorithm);
	}
	public static void createTestData(String filePath)
	{
		//create 50,000 data as cloudlet length for subsequent testing.
		int taskNum=50000;
		int[]taskLength=new int[taskNum];
		for(int i=0;i<taskNum;i++)
		{
			taskLength[i]=(new Random().nextInt(200)+1)*50+new Random().nextInt(500);
		}
		
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<taskNum;i++)
		{
			sb.append(taskLength[i]).append("\t");
			if(i%20==19)//20 data each line.
			{
				Ga.writeTxtAppend(filePath, sb.toString());
				sb=null;
				sb=new StringBuilder();
			}
		}
	}

}
