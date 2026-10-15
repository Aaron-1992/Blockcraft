package bc;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/** Portable, bounded monochrome media cartridge. Immutable data, no executable media codecs. */
final class RasterClip {
   static final int RATE=44100,FPS=20,SAMPLES_PER_FRAME=2205,MAX_BYTES=16*1024*1024;
   final int width,height,frames;final byte[] bitmap,encoded;final short[] pcm;final String hash;
   private RasterClip(int w,int h,int n,byte[] bits,short[] sound,byte[] encoded){width=w;height=h;frames=n;bitmap=bits;pcm=sound;this.encoded=encoded;try{hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(encoded));}catch(NoSuchAlgorithmException e){throw new AssertionError(e);}}
   static RasterClip read(byte[] encoded)throws IOException{
      if(encoded.length>MAX_BYTES)throw new IOException("Media file exceeds 16 MiB");
      try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(encoded)))){
         if(in.readInt()!=0x42435631)throw new IOException("Not a BCV1 cartridge");
         int w=in.readInt(),h=in.readInt(),fps=in.readInt(),frames=in.readInt(),rate=in.readInt(),samples=in.readInt();
         if(w<1||w>64||h<1||h>64||fps!=FPS||frames<1||frames>2400||rate!=RATE||samples!=frames*SAMPLES_PER_FRAME)throw new IOException("Invalid media dimensions/timing (max 64x64, 120s, 20fps, PCM44100)");
         byte[] bits=new byte[((w*h+7)/8)*frames];in.readFully(bits);short[] pcm=new short[samples];for(int i=0;i<samples;i++)pcm[i]=in.readShort();if(in.read()!=-1)throw new IOException("Trailing cartridge data");
         return new RasterClip(w,h,frames,bits,pcm,encoded.clone());
      }
   }
   int pixel(int frame,int x,int y){if(frame<0||frame>=frames||x<0||y<0||x>=width||y>=height)return 0;int p=y*width+x;return ((bitmap[frame*((width*height+7)/8)+(p>>3)]>>(7-(p&7)))&1)==0?0:15;}
   void draw(RasterVM.Cpu cpu,int frame){if(frame<0||frame>=frames)throw new IllegalArgumentException("Video frame outside cartridge");Arrays.fill(cpu.frame,(byte)0);for(int y=0;y<Math.min(height,cpu.height);y++)for(int x=0;x<Math.min(width,cpu.width);x++)cpu.frame[y*64+x]=(byte)pixel(frame,x,y);}
}
