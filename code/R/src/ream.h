#ifndef	__ream_h
#define	__ream_h
#include <jni.h>

#include <iostream>
#include <google/protobuf/stubs/common.h>

#include <rexp.pb.h>
#include <stdint.h>
#include <sys/types.h>	
#include <sys/time.h>	
#include <time.h>	
#include <errno.h>
#include <fcntl.h>	
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/wait.h>

#define R_NO_REMAP
#define R_INTERFACE_PTRS 1
#define CSTACK_DEFNS 1 

#include <Rversion.h>
#include <R.h>
#include <Rdefines.h>
/* #include <Rinternals.h> */
#include <Rinterface.h>
#include <Rembedded.h>
#include <R_ext/Boolean.h>
#include <R_ext/Parse.h>
#include <R_ext/Rdynload.h>
#include "org_godhuli_rhipe_RMapAndReduceGateway.h"

#define DLEVEL -9

void rexp2message(REXP *, const SEXP);
void fill_rexp(REXP *, const SEXP );
SEXP message2rexp(const REXP&);
void jstring2REXP(JNIEnv* ,jbyteArray ,REXP*);

/*********
 * Utility
 *********/
uint32_t nlz(const int64_t);
uint32_t getVIntSize(const int64_t) ;
uint32_t isNegativeVInt(const int8_t);
uint32_t decodeVIntSize(const int8_t);
uint32_t reverseUInt (uint32_t );
void writeVInt64ToFileDescriptor( int64_t , FILE* );
int64_t readVInt64FromFileDescriptor(FILE* );
int32_t readJavaInt(FILE* );

/*****************
 ** Displays
 *****************/

/******************
 ** Counter/Collect
 *****************/
SEXP counter(SEXP );
SEXP status(SEXP );
SEXP collect(SEXP ,SEXP );
extern  R_CallMethodDef callMethods[];


/* Java Helper */
extern "C" {
  char *GetStringNativeChars(jstring ) ;
  int embedR(char *,int ,char **,char*);
  void exitR(void);
  int voidevalR(const char* );
  extern jobject engineObj;
  extern jclass engineClass;
  extern JNIEnv *jenv;
  void Re_ShowMessage(const char*);
  void Re_WriteConsoleEx(const char *, int , int );

}
#endif
