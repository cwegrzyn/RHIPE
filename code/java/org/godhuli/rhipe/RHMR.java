/**
 * Copyright 2009 Saptarshi Guha
 *   
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.godhuli.rhipe;
import java.util.Collection;
import java.util.Vector;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutput;
import java.io.DataInput;
import java.io.EOFException;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.Calendar;
import java.net.URI;

import java.text.SimpleDateFormat;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.FileSystem;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.hadoop.filecache.DistributedCache;
import org.apache.hadoop.io.WritableUtils;
import org.apache.hadoop.mapreduce.Job;


import org.godhuli.rhipe.REXPProtos.REXP;
import com.google.protobuf.Descriptors.Descriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;

public class RHMR  implements Tool {
    protected String[] argv_;
    protected Configuration config_;
    protected Hashtable<String,String> rhoptions_;
    protected Job job_;
    protected boolean debug_;
    public static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
    final static Log LOG = LogFactory.getLog(RHMR.class);

    public static void main(String[] args)  {
	int res;
	try{
	    RHMR r = new RHMR();
	    r.setConf(new Configuration());
	    res = ToolRunner.run(r.getConf(), r, args);
	}catch(Exception ex){
	    ex.printStackTrace();
	    res=-2;
	}
	System.exit(res);
    }
    public Configuration getConf(){
	return config_;
    }
    public void setConf(Configuration c){
	config_ = c;
    }
    public int run(String[] args) throws Exception {
	this.argv_ = args;
	init();
	submitAndMonitorJob(argv_[0]);
	return 1;
    }
    protected void init()  {
	try {
	    debug_=false;
	    readParametersFromR(argv_[0]);
	    System.exit(0);
	    fillInConf();
	    job_ = new Job(config_);
	    setJob();
	} catch (Exception io) {
	    io.printStackTrace();
	    throw new RuntimeException(io);
	}
    }
    
    
    public void readParametersFromR(String configfile) throws
	IOException
    {
	rhoptions_ = new Hashtable<String,String>();
	FileInputStream in = new FileInputStream (configfile);
	REXP lines=REXP.parseFrom(in);
	REXP names = lines.getAttrValue(0);
	for(int i=0;i < lines.getStringValueCount();i++){
	    rhoptions_.put(names.getStringValue(i).getStrval(),lines.getStringValue(i).getStrval());
	}
	in.close();
    }

    public void fillInConf() throws IOException,URISyntaxException
    {
	Enumeration keys = rhoptions_.keys();
	while( keys.hasMoreElements() ) {
	    String key = (String) keys.nextElement();
	    String value = (String) rhoptions_.get(key);
	    config_.set(key,value);
	}
	REXPHelper.setFieldSep( config_.get("mapred.field.separator"," "));
	config_.set("rhipe_job_uuid",java.util.UUID.randomUUID().toString());
	String[] shared = config_.get("rhipe_shared").split(",");
	if(shared!=null){
	    for(String p : shared)
		if(p.length()>1) DistributedCache.addCacheFile(new URI(p),config_);
	}
	DistributedCache.createSymlink(config_);
    }

    public void setJob() throws ClassNotFoundException,IOException,
				    URISyntaxException {
	Calendar cal = Calendar.getInstance();
	SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	String jname = rhoptions_.get("rhipe_jobname");
	if(jname.equals(""))
	    job_.setJobName(sdf.format(cal.getTime()));
	else
	    job_.setJobName(jname);
	job_.setJarByClass(RHMR.class);

	Class<?> clz=config_.getClassByName(rhoptions_.get("rhipe_outputformat_class"));
	Class<? extends OutputFormat> ofc = clz.asSubclass(OutputFormat.class);
	job_.setOutputFormatClass(ofc);

	Class<?> clz2=config_.getClassByName(  rhoptions_.get("rhipe_inputformat_class"));
	Class<? extends InputFormat> ifc = clz2.asSubclass(InputFormat.class);
	job_.setInputFormatClass(ifc);
	
	if(!rhoptions_.get("rhipe_input_folder").equals(""))
	    FileInputFormat.setInputPaths(job_,rhoptions_.get("rhipe_input_folder"));

	String output_folder = rhoptions_.get("rhipe_output_folder");
	if(! output_folder.equals("")){
		Path ofp = new Path(output_folder);
		FileSystem srcFs = FileSystem.get(job_.getConfiguration());
		srcFs.delete(ofp, true);
		FileOutputFormat.setOutputPath(job_,ofp);
		job_.setOutputKeyClass(Class.forName(rhoptions_
						     .get("rhipe_outputformat_keyclass")));
		job_.setOutputValueClass(Class.forName(rhoptions_
						       .get("rhipe_outputformat_valueclass")));
		job_.setMapOutputKeyClass(RHBytesWritable.class);
		job_.setMapOutputValueClass(RHBytesWritable.class);
	    } else{
		job_.setOutputFormatClass(org.apache.hadoop.mapreduce.lib.output.NullOutputFormat.class);
		job_.setOutputKeyClass(org.apache.hadoop.io.NullWritable.class);
		job_.setOutputValueClass(org.apache.hadoop.io.NullWritable.class);
		job_.setMapOutputKeyClass(org.apache.hadoop.io.NullWritable.class);
		job_.setMapOutputValueClass(org.apache.hadoop.io.NullWritable.class);
	    }

	job_.setMapperClass(RHMRMapper.class);
	job_.setReducerClass(RHMRReducer.class);
	if(rhoptions_.get("rhipe_combiner").equals("TRUE"))
	    job_.setCombinerClass(RHMRReducer.class);	
    }

    public int submitAndMonitorJob(String configfile) throws Exception {
	int k =0;
	job_.submit();
	LOG.info("Tracking URL ----> "+ job_.getTrackingURL());
	boolean verb = rhoptions_.get("rhipe_job_verbose").equals("TRUE")
	    ? true: false;
	long now = System.currentTimeMillis();
	boolean result = job_.waitForCompletion( verb );
	double tt = (System.currentTimeMillis() - now)/1000.0;
	FileOutputStream out = new FileOutputStream (configfile);
	DataOutputStream fout = new DataOutputStream(out);
	try{
	    if(!result){
		k=-1;
	    }else{
		k=0;
		org.apache.hadoop.mapreduce.Counters counter = job_.getCounters();
		REXP r = buildListFromCounters(counter,tt);
		RHBytesWritable rb = new RHBytesWritable(r.toByteArray());
		rb.writeAsInt(fout);
	    }
	}catch(Exception e){
	    k=-1;
	}finally{
	    fout.close();
	    out.close();
	}
	return k;
    }
    
    public REXP buildListFromCounters(org.apache.hadoop.mapreduce.Counters counters,double tt){
	String[] groupnames = counters.getGroupNames().toArray(new String[]{});
	String[] groupdispname = new String[groupnames.length+1];
	Vector<REXP> cn = new Vector<REXP>();
	for(int i=0;i < groupnames.length; i++){
	    org.apache.hadoop.mapreduce.CounterGroup
		cgroup = counters.getGroup( groupnames[i]);
	    groupdispname[i] = cgroup.getDisplayName();
	    REXP.Builder cvalues = REXP.newBuilder();
	    Vector<String> cnames = new Vector<String>();
	    cvalues.setRclass(REXP.RClass.REAL);
	    for (org.apache.hadoop.mapreduce.Counter counter: cgroup){
		cvalues.addRealValue((double)counter.getValue());
		cnames.add(counter.getDisplayName());
	    }
	    cvalues.addAttrName("names");
	    cvalues.addAttrValue(RObjects.makeStringVector(cnames.toArray(new String[]{})));
	    cn.add( cvalues.build());
	}
	groupdispname[groupnames.length]="job_time";
	REXP.Builder cvalues = REXP.newBuilder();
	cvalues.setRclass(REXP.RClass.REAL);
	cvalues.addRealValue(tt);
	cn.add(cvalues.build());
	return(RObjects.makeList(groupdispname,cn));
    }

}