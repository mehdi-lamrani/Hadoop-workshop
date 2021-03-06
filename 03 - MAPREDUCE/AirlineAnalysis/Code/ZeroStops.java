import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class ZeroStops {

	public static class AirlineMap extends
			Mapper<LongWritable, Text, Text, Text> {

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] arr = line.split(",");
			// arr[0] = Airline ID
			// arr[1] = Name
			if(!arr[0].equals("\\N")){
				context.write(new Text(arr[0]), new Text("Name\t" + arr[1]));				
			}
		}
	}

	public static class RouteMap extends Mapper<LongWritable, Text, Text, Text> {

		public void map(LongWritable key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] arr = line.split(",");
			// arr[1] = Airline ID
			// arr[7] = Stops
			if(!arr[1].equals("\\N")){				
				context.write(new Text(arr[1]), new Text("Stops\t" + arr[7]));
			}

		}
	}

	public static class ZeroStopsJoinReduce extends
			Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> value, Context context)
				throws IOException, InterruptedException {

			// Sequence: AirlineID Name <Name> Stops <Stops>
			String name = "";
			String stops = "";
			
			for (Text x : value) {
				String arr[] = x.toString().split("\t");
				
				if (arr[0].equals("Name")){
					name = arr[1];
				}
				else if (arr[0].equals("Stops") && arr.length>1){
					stops = arr[1]; 
					if (stops.equals("0") && !name.equals("")){
						context.write(key, new Text(name));
						break;
					}
				}
			}
		}
	}
	
	public static void main(String[] args) throws Exception{
		
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf);
		
		job.setJarByClass(ZeroStops.class);
		
		job.setReducerClass(ZeroStopsJoinReduce.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		
		MultipleInputs.addInputPath(job, new Path(args[0]), TextInputFormat.class, AirlineMap.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, RouteMap.class);
		
		job.setOutputFormatClass(TextOutputFormat.class);
		
		Path outputPath = new Path(args[2]);
		
		FileOutputFormat.setOutputPath(job, new Path(args[2]));
		
		outputPath.getFileSystem(conf).delete(outputPath, true);
		
		System.exit(job.waitForCompletion(true)? 0 : 1);
		
	}
	
}
