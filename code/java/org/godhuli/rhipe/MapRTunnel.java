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
import org.apache.hadoop.conf.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public static class MapRTunnel extends TunnelBase{
    protected static final Log LOG = LogFactory.getLog(MapRTunnel.class.getName());
    private static MapRTunnel singleton = null;
    private static boolean have_started;
    private static volatile Context ctx;
    private static InReader datareader;
    public static Process tunnel;
    public static DataOutputStream tunOut,funOut;
    public static DataInputStream funIn,funMsg;
    public static RHBytesWritable key,value;
    public static int CMDFORK=-1, CMDSETUP=-2,CMDMAP=-3, CMDCLEANUP=-4;
    static {
	have_started = false;
	singleton = new MapRTunnel();
    }
    public static class InReader extends Thread {
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
		}catch(IOException e){}
		return;
	    }
	}
    }
    public static class MsgReader extends Thread {
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
		}catch(IOException e){}
		return;
	    }
	}
    }
    public static class InReader extends Thread {
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
		}catch(IOException e){}
		return;
	    }
	}
    }
    public static MapRTunnel getInstance() {
	return singleton;
    }
    public void start_tunnel(Context ctx,Configuration cfg) throws RHIPERuntimeException{
	try{
	    if(!have_started){
		ProcessBuilder builder = new ProcessBuilder(cfg.get("rhipe_map_engine").split(" "));
		builder.environment().putAll(addJobConfToEnvironment(new Environment()).toMap());
		tunnel = builder.start();
		tunOut =  tunnel.getOutputStream();
		tunIn  = new DataInputStream(new BufferedInputStream(tunnel.getInputStream(),10*1024));
		tunOut = new DataOutputstream(new BufferedOutputStream(tunnel.getOutputStream(),10*1024));
		tunMsg = new DataOutputstream(new BufferedOutputStream(tunnel.getErrorStream(),10*1024));
		have_started = true;
	    }
	    this.ctx = ctx;
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }
    public void invoke_setup() throws RHIPERuntimeException{
	try{
	    WritableUtils.writeVInt(MapRTunnel.CMDSETUP);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }
    public void invoke_map() throws RHIPERuntimeException{
	try{
	    WritableUtils.writeVInt(MapRTunnel.CMDMAP);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }
    public void invoke_cleanup() throws RHIPERuntimeException{
	try{
	    WritableUtils.writeVInt(MapRTunnel.CMDCLEANUP);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }

    public void send_key_value(RHBytesWritable key, RHBytesWritable value) 
	throws RHIPERuntimeException {
	try{
	    key.write(tunOut);
	    value.write(tunOut);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }

    private void start_tunIn_reader(){
	datareader = new InReader();
	datareader.start();
    }

    private void send_fork_request(String uuid) throws RHIPERuntimeException {
	try{
	    // msgreader = new InReader();
	    // msgreader.start();
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }

}