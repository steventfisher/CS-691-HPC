/* Utilized from https://dzenanhamzic.com/2016/09/21/java-mapreduce-for-top-n-twitter-hashtags/
  Code for Homework 1, Question 5.
  To run file type the following:
  hadoop jar HashCount.jar HashCount  <INPUT FILE> <OUTPUT FILE>

  Example: hadoop jar HashCount.jar HashCount /user/hduser/users/twitter/tweets.txt /user/hduser/users/twitter/HashOut
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
import java.util.StringTokenizer;
 
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
 
public class HashCount {
 
    public static class TokenizerMapper extends
            Mapper<Object, Text, Text, IntWritable> {
 
        private final static IntWritable one = new IntWritable(1);
        private Text word = new Text();
        
        public void map(Object key, Text value, Context context)
                throws IOException, InterruptedException {
 
                String line = value.toString();
                StringTokenizer itr = new StringTokenizer(line);
 
                while (itr.hasMoreTokens()) {
 
                    String token = itr.nextToken();
                    //token = token.toLowerCase();
                    if (token.startsWith("#")) {
                        word.set(token);
                        context.write(word, one);
                    }
                }
 
        }
    }
 
    /**
     * Reducer
     * it sums values for every hashtag and puts them to HashMap
     */
    public static class IntSumReducer extends
            Reducer<Text, IntWritable, Text, IntWritable> {
 
        private Map<Text, IntWritable> countMap = new HashMap<>();
 
        @Override
        public void reduce(Text key, Iterable<IntWritable> values,
                Context context) throws IOException, InterruptedException {
 
            // computes the number of occurrences of a single word
            int sum = 0;
            for (IntWritable val : values) {
                sum += val.get();
            }
 
            // puts the number of occurrences of this word into the map.
            // We need to create another Text object because the Text instance
            // we receive is the same for all the words
            countMap.put(new Text(key), new IntWritable(sum));
        }
 
        /**
         * this method is run after the reducer has seen all the values.
         * it is used to output top 15 hashtags in file.
         */
        @Override
        protected void cleanup(Context context) throws IOException,
                InterruptedException {
	    
            Map<Text, IntWritable> sortedMap = sortByValues(countMap);
 
            int counter = 0;
            for (Text key : sortedMap.keySet()) {
                if (counter++ == 100) {
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
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(HashCount.class);
        job.setMapperClass(TokenizerMapper.class);
        // job.setCombinerClass(IntSumReducer.class);
        job.setReducerClass(IntSumReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
