/* Utilized from https://dzenanhamzic.com/2016/09/21/java-mapreduce-for-top-n-twitter-hashtags/
  Code for Homework 1, Question 6.
  To run file type the following:
  hadoop jar MostTweetedCities.jar MostTweetedCities <USERS INPUT FILE> <TWEETS INPUT FILE> <TEMP OUTPUT> <OUTPUT FILE> 

  Example: hadoop jar CityCount.jar CityCount /user/hduser/users/twitter/users.txt /user/hduser/users/twitter/tweets.txt /user/hduser/users/twitter/CityTmp1 /user/hduser/users/twitter/CityOut1

*/

import java.io.IOException;
import org.apache.log4j.Logger;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Arrays;
 
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
 
public class CityCount {
	public static class CityMapper extends
	Mapper<Object, Text, Text, Text> {
	public void map(Object key, Text value, Context context)
		throws IOException, InterruptedException {
	String record = value.toString();
	String users[] = record.split("\t");
	String cities[] = users[1].split(",");
	context.write(new Text(users[0]), new Text("city\t" + cities[0]));
}
}

public static class TweetsMapper extends
	Mapper<Object, Text, Text, Text> {
public void map(Object key, Text value, Context context)
		throws IOException, InterruptedException {
	String record = value.toString();
	String tweets[] = record.split("\t");
	if(tweets.length >= 2){
	    context.write(new Text(tweets[0]), new Text("tweet\t" + "1"));
	    }
}
}
     /*
     * Reducer
     * it sums values for every hashtag and puts them to HashMap
     */
    public static class ReduceJoinReducer extends
            Reducer<Text, Text, Text, Text> {
 
        //private Map<Text, IntWritable> countMap = new HashMap<>();
 
        //@Override
        public void reduce(Text key, Iterable<Text> values,
                Context context) throws IOException, InterruptedException {
 	    String name = "";
            // computes the number of occurrences of a single word
            int sum = 0;
            for (Text t : values) {
		String usertocity[] = t.toString().split("\t");
		if(usertocity[0].equals("tweet")){
		    //sum++;
			sum += Integer.parseInt(usertocity[1]);
		}else if(usertocity[0].equals("city")){
			name = usertocity[1];
		}

            }
	    String total = String.format("%d", sum);
	    context.write(new Text(name), new Text(total));
	    //context.write(new Text(name), new IntWritable(Integer.parseInt(sum)));
	}
    }
    
    public static class TokenizeCombined extends
            Mapper<Object, Text, Text, IntWritable> {
 
        //private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {

                String line = value.toString();
		String[] itr = line.split("\t");
                if(itr[0].compareTo("") != 0){
		    word.set(itr[0]);
		    context.write(word, new IntWritable(Integer.parseInt(itr[1])));
		}
 
        }
    }
    
    public static class SortReduce extends
            Reducer<Text, IntWritable, Text, IntWritable> {
 
        //private Map<Text, Text> countMap = new HashMap<>();
	private Map<Text, IntWritable> countMap = new HashMap<>();
        //@Override
        public void reduce(Text key, Iterable<IntWritable> values,
                Context context) throws IOException, InterruptedException {
 
            // computes the number of occurrences of a single word
            int sum = 0;
            for (IntWritable val : values) {
		//String tmp[] = val.toString().split("\t");
		//String tmp = val.toString();
		//if(tmp.length >= 1){
		//sum += Integer.parseInt(tmp[0]);
		//sum += Integer.parseInt(tmp);
		sum += val.get();
		    //sum++;
		    //}
            }
 
            // puts the number of occurrences of this word into the map.
            // We need to create another Text object because the Text instance
            // we receive is the same for all the words
	    String total = String.format("%d", sum);
	    countMap.put(new Text(key), new IntWritable(sum));
            //countMap.put(new Text(key), new Text(total));
	    //context.write(new Text(key), new Text(total));
        }
 
        /**
         * this method is run after the reducer has seen all the values.
         * it is used to output top 15 hashtags in file.
         */
        @Override
        protected void cleanup(Context context) throws IOException,
                InterruptedException {
	    
            Map<Text, IntWritable> sortedMap = sortByValues(countMap);
	    //Map<Text, Text> sortedMap = sortByValues(countMap);
            int counter = 0;
            for (Text key : sortedMap.keySet()) {
                if (counter++ == 10) {
                    break;
                }
                context.write(key, sortedMap.get(key));
            }
	    }
    }
    
    /**
       Since, the original mapreducer sorts the hashtags by key and not value 
       We need a method that will our results by value. That way we
       will obtain the top number of values.
       Utilized from https://dzenanhamzic.com/2016/09/21/java-mapreduce-for-top-n-twitter-hashtags/
     * This method sorts Map by values
     * @param map
     * @return sortedMap Map<K,V>;
     */
    private static <K extends Comparable, V extends Comparable> Map<K, V> sortByValues(
            Map<K, V> map) {
        List<Map.Entry<K, V>> entries = new LinkedList<Map.Entry<K, V>>(
                map.entrySet());
 
        Collections.sort(entries, new Comparator<Map.Entry<K, V>>() {
 
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
 
        // LinkedHashMap will keep the keys in the order they are inserted
        // which is currently sorted on natural ordering
        Map<K, V> sortedMap = new LinkedHashMap<K, V>();
 
        for (Map.Entry<K, V> entry : entries) {
            sortedMap.put(entry.getKey(), entry.getValue());
        }
 
        return sortedMap;
    }
    
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "city count");
        job.setJarByClass(CityCount.class);
        //job.setMapperClass(TokenizerMapper.class);
        // job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(ReduceJoinReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

	MultipleInputs.addInputPath(job, new Path(args[0]),TextInputFormat.class, CityMapper.class);
	MultipleInputs.addInputPath(job, new Path(args[1]),TextInputFormat.class, TweetsMapper.class);
        FileOutputFormat.setOutputPath(job, new Path(args[2]));
	job.waitForCompletion(true);

	Configuration conf2 = new Configuration();
	Job job2 = Job.getInstance(conf2, "sort");
        job2.setJarByClass(CityCount.class);
	job2.setMapperClass(TokenizeCombined.class);
	job2.setCombinerClass(SortReduce.class);
	job2.setReducerClass(SortReduce.class);
	job2.setOutputKeyClass(Text.class);
	//job2.setOutputValueClass(Text.class);
	job2.setOutputValueClass(IntWritable.class);

	FileInputFormat.addInputPath(job2, new Path(args[2]));
        FileOutputFormat.setOutputPath(job2, new Path(args[3]));
        System.exit(job2.waitForCompletion(true) ? 0 : 1);
    }
}

