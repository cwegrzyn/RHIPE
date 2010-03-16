#include "ream.h"
#include <netinet/in.h>
#include <unistd.h>

const int i___ = 1;
#define is_bigendian() ( (*(char*)&i___) == 0 )

/*************************************
 ** Variable Length Encoding
 ************************************/

uint32_t nlz( int64_t x) {
  if (x == 0) return(64); 
  uint32_t n = 0; 
  if (x <=  0x00000000FFFFFFFFLL) {n = n +32; x = x <<32;} 
  if (x <=  0x0000FFFFFFFFFFFFLL) {n = n +16; x = x <<16;}
  if (x <=  0x00FFFFFFFFFFFFFFLL) {n = n + 8; x = x << 8;}
  if (x <=  0x0FFFFFFFFFFFFFFFLL) {n = n + 4; x = x << 4;}
  if (x <=  0x3FFFFFFFFFFFFFFFLL) {n = n + 2; x = x << 2;}
  if (x <=  0x7FFFFFFFFFFFFFFFLL) {n = n + 1;} 
  return n; 
}


uint32_t getVIntSize( int64_t i) {
  if (i >= -112 && i <= 127) {
    return 1;
  }
  if (i < 0) {
    i = ~i;
  }
  int dataBits = sizeof(int64_t)*8 - nlz(i);
  return (dataBits + 7) / 8 + 1;
}
uint32_t isNegativeVInt(const int8_t value) {
  return (uint32_t)(value < -120 || (value >= -112 && value < 0));
}
uint32_t decodeVIntSize(const int8_t value) {
  if (value >= -112) {
    return 1;
  } else if (value < -120) {
    return -119 - value;
  }
  return -111 - value;
}

void writeVInt64ToFileDescriptor( int64_t  i , FILE* fd) {
  char x ;
  if (i >= -112 && i <= 127) {
    x=(char)i;
    fwrite(&x,sizeof(x),1,fd);
    return;
  }
  int32_t len = -112;
  if (i < 0) {
    i ^= -1L; // take one's complement'
    len = -120;
  }
  int64_t tmp = i;
  while (tmp != 0) {
    tmp = tmp >> 8;
    len--;
  }
  x=(char)len;
  // mmessage("B=%x %d",x,x);
  fwrite(&x,sizeof(x),1,fd);
  len = (len < -120) ? -(len + 120) : -(len + 112);
  int32_t idx;
  for (idx = len; idx != 0; idx--) {
    int32_t shiftbits = (idx - 1) * 8;
    int64_t mask = 0xFFLL << shiftbits;
    x = (char)((i & mask) >> shiftbits);
    // mmessage("B=%x",x);
    fwrite(&x,sizeof(x),1,fd);
  }
  

  // int32_t x = (int32_t) i, tonetwork;
  // tonetwork = reverseUInt(x);
  // fwrite(&tonetwork,sizeof(int32_t),1,fd);
  
}


int64_t readVInt64FromFileDescriptor(FILE* fd){
  uint8_t  firstByte = 0 ;
  if(fread(&firstByte,sizeof(uint8_t),1,fd)<=0) 
    return(0);

  int len = decodeVIntSize((int8_t)firstByte);
  if (len == 1) {
    return (int8_t)firstByte;
  }
  int64_t  i = 0;
  int32_t idx;
  for (idx = 0; idx < len-1; idx++) {
    int8_t b;
    int32_t x;
    // Readn(fd,&x,sizeof(b));
    fread(&x,sizeof(b),1,fd);
    b=(int8_t)x;
    i = i << 8;
    i = i | (b & 0xFF);
  }
  return  (isNegativeVInt(firstByte) ? (i ^ -1L) : i);
  // int32_t r,fromnetwork;
  // fread(&r,sizeof(uint32_t),1,fd);
  // fromnetwork = reverseUInt(r);
  // return(fromnetwork);
}

uint32_t reverseUInt (uint32_t i) {
    uint8_t c1, c2, c3, c4;

    if (is_bigendian()) {
        return i;
    } else {
        c1 = i & 255;
        c2 = (i >> 8) & 255;
        c3 = (i >> 16) & 255;
        c4 = (i >> 24) & 255;

        return ((uint32_t)c1 << 24) + ((uint32_t)c2 << 16) + ((uint32_t)c3 << 8) + c4;
    }
}



