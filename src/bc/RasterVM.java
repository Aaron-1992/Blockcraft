package bc;

import java.util.*;

/** General integer processor and small graphics coprocessor. No game rules live in the interpreter.
 * Four words per instruction: opcode,a,b,c. All addresses/branches are bounds checked. */
final class RasterVM {
   static final String[] OPS={"HALT","LI","MOV","ADD","SUB","MUL","DIV","MOD","AND","OR","XOR","SHL","SHR","LD","ST","JMP","JZ","JNZ","JLT","CALL","RET","IN","OUT","WAIT","RAND","CLEAR","PIXEL","SPRITE","HIT","STAMP","BLIT","PRESENT","DIGIT"};
   static final class Cpu {
      int pc,wait,sp,rng=0x1234567;
      final int[] r=new int[32],stack=new int[32],out=new int[8];
      final byte[] frame=new byte[64*64];
      String fault="";int width=16,height=28;
      void reset(){pc=wait=sp=0;Arrays.fill(r,0);Arrays.fill(out,0);Arrays.fill(frame,(byte)0);fault="";}
   }
   interface IO {int input(int port);void present(Cpu cpu);default void output(int port,int value){}}
   static int reg(int i){if(i<0||i>=32)throw new IllegalArgumentException("Register outside 0..31");return i;}
   static int addr(int i,int[] mem){if(i<0||i>=mem.length)throw new IllegalArgumentException("Memory address "+i);return i;}
   static void pixel(Cpu c,int x,int y,int v){if(x>=0&&x<c.width&&y>=0&&y<c.height)c.frame[y*64+x]=(byte)(v&15);}
   static void step(Cpu c,int[] code,int[] mem,IO io){
      if(c.wait>0){c.wait--;return;}if(!c.fault.isEmpty())return;
      try {for(int budget=0;budget<12000;budget++){
         if(c.pc<0||c.pc>=code.length/4)throw new IllegalArgumentException("PC outside ROM");
         int q=c.pc++*4,op=code[q],a=code[q+1],b=code[q+2],d=code[q+3];int[] r=c.r;
         switch(op){
            case 0 -> {c.fault="HALT";return;}
            case 1 -> r[reg(a)]=b;
            case 2 -> r[reg(a)]=r[reg(b)];
            case 3 -> r[reg(a)]+=r[reg(b)];case 4 -> r[reg(a)]-=r[reg(b)];case 5 -> r[reg(a)]*=r[reg(b)];
            case 6 -> r[reg(a)]/=r[reg(b)];case 7 -> r[reg(a)]%=r[reg(b)];case 8 -> r[reg(a)]&=r[reg(b)];
            case 9 -> r[reg(a)]|=r[reg(b)];case 10 -> r[reg(a)]^=r[reg(b)];case 11 -> r[reg(a)]<<=r[reg(b)];case 12 -> r[reg(a)]>>>=r[reg(b)];
            case 13 -> r[reg(a)]=mem[addr(r[reg(b)],mem)];case 14 -> mem[addr(r[reg(a)],mem)]=r[reg(b)];
            case 15 -> c.pc=a;case 16 -> {if(r[reg(a)]==0)c.pc=b;}case 17 -> {if(r[reg(a)]!=0)c.pc=b;}case 18 -> {if(r[reg(a)]<r[reg(b)])c.pc=d;}
            case 19 -> {if(c.sp>=32)throw new IllegalArgumentException("Call stack full");c.stack[c.sp++]=c.pc;c.pc=a;}
            case 20 -> {if(c.sp==0)throw new IllegalArgumentException("Call stack empty");c.pc=c.stack[--c.sp];}
            case 21 -> r[reg(a)]=io.input(b);
            case 22 -> {if(a<0||a>=8)throw new IllegalArgumentException("Output port");int value=r[reg(b)];if(c.out[a]!=value){c.out[a]=value;io.output(a,value);}}
            case 23 -> {c.wait=Math.max(0,Math.min(1200,a)-1);return;}
            case 24 -> {c.rng^=c.rng<<13;c.rng^=c.rng>>>17;c.rng^=c.rng<<5;r[reg(a)]=Math.floorMod(c.rng,Math.max(1,r[reg(b)]));}
            case 25 -> Arrays.fill(c.frame,(byte)(r[reg(a)]&15));
            case 26 -> pixel(c,r[reg(a)],r[reg(b)],r[reg(d)]);
            // Graphics convention: r0=4x4 mask, r1=x,r2=y,r3=color,r4=RAM base,r5=width,r6=height,r8=collision result.
            case 27,28,29 -> {
               r[8]=0;if(op!=27&&(r[5]<1||r[5]>64||r[6]<1||r[6]>64||r[4]<0||(long)r[4]+r[5]*r[6]>mem.length))throw new IllegalArgumentException("Grid bounds");
               for(int yy=0;yy<4;yy++)for(int xx=0;xx<4;xx++)if((r[0]&(1<<(yy*4+xx)))!=0){
                  int x=r[1]+xx,y=r[2]+yy;
                  if(op==27)pixel(c,x,y,r[3]);
                  else if(x<0||y<0||x>=r[5]||y>=r[6])r[8]=1;
                  else {int p=r[4]+y*r[5]+x;if(op==28){if(mem[p]!=0)r[8]=1;}else mem[p]=r[3];}
               }
            }
            case 30 -> {if(r[5]<0||r[5]>64||r[6]<0||r[6]>64||r[4]<0||(long)r[4]+r[5]*r[6]>mem.length)throw new IllegalArgumentException("Blit bounds");for(int y=0;y<r[6];y++)for(int x=0;x<r[5];x++)pixel(c,r[1]+x,r[2]+y,mem[r[4]+y*r[5]+x]);}
            case 31 -> io.present(c);
            case 32 -> {int[] digits={0x7b6f,0x2492,0x73e7,0x73cf,0x5bc9,0x79cf,0x79ef,0x7249,0x7bef,0x7bcf,0x2bed,0x6bae,0x7927,0x6b6e,0x79e7,0x79e4};int bits=digits[r[0]&15];for(int y=0;y<5;y++)for(int x=0;x<3;x++)if((bits&(1<<(14-y*3-x)))!=0)pixel(c,r[1]+x,r[2]+y,r[3]);}
            default -> throw new IllegalArgumentException("Unknown opcode "+op);
         }
      }}catch(RuntimeException e){c.fault=e.getMessage()==null?e.getClass().getSimpleName():e.getMessage();}
   }
   static final class Assembler {
      final List<String> lines=new ArrayList<>();
      void label(String name){lines.add(name+":");}
      void op(String op,Object... args){String s=op;for(Object a:args)s+=" "+a;lines.add(s);}
      String text(){return String.join("\n",lines)+"\n";}
      int[] build(){return assemble(text());}
   }
   static int[] assemble(String text){
      if(text.length()>262144)throw new IllegalArgumentException("ROM source too large");
      Map<String,Integer> labels=new HashMap<>();List<String[]> list=new ArrayList<>();
      for(String line:text.split("\\R")){line=line.split(";",2)[0].trim();if(line.isEmpty())continue;if(line.endsWith(":")){String l=line.substring(0,line.length()-1);if(labels.put(l,list.size())!=null)throw new IllegalArgumentException("Duplicate label "+l);}else list.add(line.split("\\s+"));}
      if(list.size()>4096)throw new IllegalArgumentException("ROM is limited to 4096 instructions");int[] out=new int[list.size()*4];
      for(int i=0;i<list.size();i++){String[] words=list.get(i);int op=Arrays.asList(OPS).indexOf(words[0].toUpperCase(Locale.ROOT));if(op<0||words.length>4)throw new IllegalArgumentException("Bad instruction "+i);out[i*4]=op;for(int j=1;j<words.length;j++){String w=words[j];out[i*4+j]=labels.containsKey(w)?labels.get(w):Integer.decode(w);}}
      return out;
   }
}
