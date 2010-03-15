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
import org.godhuli.rhipe.REXPProtos.REXP;
import org.godhuli.rhipe.REXPProtos.REXP.RClass;
import org.apache.hadoop.io.*;

public class RHMRMapper extends Mapper<RHBytesWritable,
				RHBytesWritable,RHBytesWritable,RHBytesWritable>{
    protected static final Log LOG = LogFactory.getLog(RHMRMapper.class.getName());
    protected static boolean will_copy_file;
    protected static final Environment env =null;
    protected static boolean has_r_started;
    protected static TunnelBase tbase;
    protected volatile Context ctx;
    static Process tunnel;
    static DataOutputStream tunOut;
    static DataInputStream tunIn,tunMsg;
    static RHBytesWritable key,value;
    static final int  CMDSETUP=-2,CMDMAP=-3, CMDCLEANUP=-4;
    private static InReader datareader;

    public void run(Context context) throws IOException,InterruptedException {
	Configuration cfg = context.getConfiguration();
	try{
	    String mif = ((FileSplit) context.getInputSplit()).getPath().toString();
	    cfg.set("mapred.input.file",mif);
	}catch(java.lang.ClassCastException e){}
	will_copy_file=cfg.get("rhipe_copy_file").equals("TRUE")? true: false;
	
	start_tunnel( context, cfg );
	
	setup(context);
	
	invoke(CMDMAP);
	while (context.nextKeyValue()) {
	    map(context.getCurrentKey(), context.getCurrentValue(), context);
	}
	cleanup(context);

	// if(will_copy_file) rtunnel.copyFiles(System.getProperty("java.io.tmpdir"));
    }								  
								  
    public void setup(Context context)  throws RHIPERuntimeException{
	invoke(CMDSETUP);
    }
    public void map(RHBytesWritable key, RHBytesWritable value, Context ctx) 
	throws IOException,InterruptedException {
	key.write(tunOut);
	value.write(tunOut);
    }

    public void cleanup(Context ctx)  throws RHIPERuntimeException {
	invoke(CMDCLEANUP);
    }
    public void start_tunnel(Context ctx,Configuration cfg) throws RHIPERuntimeException{
	try{
	    if(!has_r_started){
		tbase = new TunnelBase();
		ProcessBuilder builder = new ProcessBuilder(cfg.get("rhipe_map_engine").split(" "));
		Environment childEnv = (Environment)(new Environment()).clone();
		// addJobConfToEnvironment(cfg, childEnv);
		// builder.environment().putAll(childEnv.toMap());
		tunnel = builder.start();
		tunIn  = new DataInputStream(new BufferedInputStream(tunnel.getInputStream(),10*1024));
		tunOut = new DataOutputStream(new BufferedOutputStream(tunnel.getOutputStream(),10*1024));
		tunMsg = new DataInputStream(new BufferedInputStream(tunnel.getErrorStream(),10*1024));
		has_r_started = true;
		tbase.send_config_to_r(cfg,tunOut);
	    }
	    this.ctx = ctx;
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }
    public void invoke(int c) throws RHIPERuntimeException {
	try{
	    WritableUtils.writeVInt(tunOut,c);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }

    private void start_tunIn_reader(){
	datareader = new InReader();
	datareader.start();
    }

    class InReader extends Thread {
        boolean stop;
        public InReader(){
            stop = false;
        }
        public void run(){
            try{
                key.readFields(tunIn);
                value.readFields(tunIn);
                ctx.write(key,value);
            }catch(IOException e){
                try{
                    tunIn.close();
                }catch(IOException f){}
                return;
            }catch(InterruptedException e){
                try{
                    tunIn.close();
                }catch(IOException f){}
                return;
            }
        }
    }
   class MsgReader extends Thread {
        boolean stop;
        REXP rx;
        public MsgReader(){
            stop = false;
        }
        public void run(){
            try{
                rx = REXP.parseDelimitedFrom(tunMsg);
                System.out.println(rx);
            }catch(IOException e){
                try{
                    tunMsg.close();
                }catch(IOException f){}
                return;
            }
        }
    }

}

	