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
import java.util.Map;
import java.util.Iterator;
import java.util.Properties;
import java.util.Vector;
import java.util.Iterator;
import org.godhuli.rhipe.REXPProtos.REXP;
import org.godhuli.rhipe.REXPProtos.REXP.RClass;
import org.apache.hadoop.io.*;

import org.apache.hadoop.conf.Configuration;

public class TunnelBase {
    public static int CMDSENDCFG = 0;
    public void addJobConfToEnvironment(Configuration conf, Properties env) {
	// Iterator it = conf.iterator();
	// while (it.hasNext()) {
	//     Map.Entry en = (Map.Entry) it.next();
	//     String name = (String) en.getKey();
	//     String value = conf.get(name); 
	//     env.put(name, value);
	// }
	env.put( "TMPDIR",System.getProperty("java.io.tmpdir"));
    }
    public void send_config_to_r(Configuration cfg,DataOutput dob)  throws RHIPERuntimeException{
	try{
	    REXP r = mapredopts(cfg);
	    byte[] b= r.toByteArray();
	    WritableUtils.writeVInt(dob,TunnelBase.CMDSENDCFG);
	    WritableUtils.writeVInt(dob,b.length);
	    dob.write(b);
	}catch(IOException e){
	    throw new RHIPERuntimeException(e.toString());
	}
    }

    public  REXP mapredopts(Configuration cfg) throws RHIPERuntimeException{
	try{
	Iterator<Map.Entry<String,String>> iter = cfg.iterator();
	REXP.Builder mrvals   = REXP.newBuilder();
	REXP.Builder mrnames   = REXP.newBuilder();

	mrvals.setRclass(REXP.RClass.STRING);
	mrnames.setRclass(REXP.RClass.STRING);
	REXPProtos.STRING.Builder content;
	while(iter.hasNext()){
	    Map.Entry<String,String> c = iter.next();
	    String key = c.getKey();
	    String value = c.getValue();

	    content =REXPProtos.STRING.newBuilder();
	    content.setStrval(c.getValue());
	    mrvals.addStringValue(content.build());

	    content=REXPProtos.STRING.newBuilder();
	    content.setStrval(c.getKey());
	    mrnames.addStringValue(content.build());
	}
	mrvals.addAttrName("names");
	mrvals.addAttrValue(mrnames.build());
	return(mrvals.build());
	}catch(Exception e){
	    throw new RHIPERuntimeException(e.toString());
	}

    }
}