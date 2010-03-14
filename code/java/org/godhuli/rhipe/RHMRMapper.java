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

import java.io.*;
import java.net.URLDecoder;
import org.apache.hadoop.mapreduce.MapContext;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.conf.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;

public class RHMRMapper extends Mapper<RHBytesWritable,
				RHBytesWritable,RHBytesWritable,RHBytesWritable>{
    protected static final Log LOG = LogFactory.getLog(RHMRMapper.class.getName());
    protected static boolean will_copy_file;
    protected static MapRTunnel rtunnel   = MapRTunnel.getInstance();
 
    public void run(Context context) throws IOException,InterruptedException {
	Configuration cfg = context.getConfiguration();
	try{
	    String mif = ((FileSplit) context.getInputSplit()).getPath().toString();
	    cfg.set("mapred.input.file",mif);
	}catch(java.lang.ClassCastException e){}
	will_copy_file=cfg.get("rhipe_copy_file").equals("TRUE")? true: false;

	rtunnel.start_tunnel( cfg );
	
	setup(context);
	
	rtunnel.invoke_map();
	while (context.nextKeyValue()) {
		map(context.getCurrentKey(), context.getCurrentValue(), context);
	}
	cleanup(context);

	if(will_copy_file) rtunnel.copyFiles(System.getProperty("java.io.tmpdir"));
    }								  
								  
								  
    public void setup(Context context)  {
	try{
	    rtunnel.invoke_setup();
	}catch(RHIPERuntimeException r){
	    throw new RuntimeException(r.toString());
	}
    }


    public void map(RHBytesWritable key, RHBytesWritable value, Context ctx) 
	throws IOException,InterruptedException {
	rtunnel.send_key_value(key,value)
	    }
    
    public void cleanup(Context ctx) {
	try{
	    rtunnel.invoke_cleanup();
	}catch(RHIPERuntimeException e){
	    throw new RuntimeException(r.toString());	
	}
    }
}
	