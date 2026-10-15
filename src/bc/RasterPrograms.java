package bc;

/** Example programs assembled for the generic processor; the VM itself has no falling-block rules. */
final class RasterPrograms {
   static final int[] SHAPES={0x00f0,0x2222,0x00f0,0x2222,0x0066,0x0066,0x0066,0x0066,0x0072,0x0262,0x0270,0x0232,0x0071,0x0226,0x0470,0x0322,0x0074,0x0622,0x0170,0x0223,0x0036,0x0462,0x0036,0x0462,0x0063,0x0264,0x0063,0x0264};
   static RasterVM.Assembler a;static boolean audio;
   static void tone(int note,int ms,int voice){if(audio){o("LI",0,note);o("LI",1,ms);o("LI",2,voice);o("TONE",0,1,2);}}
   static void o(String op,Object...args){a.op(op,args);}static void l(String s){a.label(s);}
   static void imm(String op,int r,int v){o("LI",31,v);o(op,r,31);}
   static void call(String s){o("CALL",s);}static void key(int mask,String skip){o("MOV",27,21);imm("AND",27,mask);o("JZ",27,skip);}
   static void graphics(){o("MOV",0,26);o("MOV",1,10);o("MOV",2,11);o("MOV",3,15);o("LI",4,0);o("LI",5,10);o("LI",6,20);}
   static String fallingSource(){return fallingSource(true);}
   static synchronized String fallingSource(boolean withAudio){
      audio=withAudio;
      a=new RasterVM.Assembler();o("LI",0,0x4244);o("OUT",7,0);
      for(int i=0;i<SHAPES.length;i++){o("LI",0,256+i);o("LI",1,SHAPES[i]);o("ST",0,1);}
      o("LI",19,1);
      l("restart");o("LI",29,0);o("LI",0,0);l("erase");o("ST",29,0);imm("ADD",29,1);o("LI",31,200);o("JLT",29,31,"erase");
      for(int r:new int[]{16,17,18,20})o("LI",r,0);
      o("LI",31,7);o("RAND",14,31);call("spawn");
      l("loop");o("IN",21,0);key(64,"pausekey");tone(67,140,0x901);o("LI",19,0);o("JMP","restart");
      l("pausekey");key(32,"active");imm("XOR",19,1);tone(76,80,0x601);
      l("active");o("JNZ",20,"draw");o("JNZ",19,"draw");
      if(audio){key(3,"nomovetone");tone(60,35,0x501);l("nomovetone");}
      key(1,"right");o("MOV",23,10);imm("SUB",10,1);call("test");o("JZ",8,"right");o("MOV",10,23);
      l("right");key(2,"rotate");o("MOV",23,10);imm("ADD",10,1);call("test");o("JZ",8,"rotate");o("MOV",10,23);
      l("rotate");key(4,"drop");tone(72,60,0x701);o("MOV",25,12);o("MOV",23,10);imm("ADD",12,1);imm("MOD",12,4);call("mask");call("test");o("JZ",8,"drop");
      imm("SUB",10,1);call("test");o("JZ",8,"drop");imm("ADD",10,2);call("test");o("JZ",8,"drop");o("MOV",10,23);o("MOV",12,25);call("mask");
      l("drop");key(16,"gravity");l("harddrop");imm("ADD",11,1);call("test");o("JNZ",8,"land");imm("ADD",17,2);o("JMP","harddrop");
      l("gravity");o("IN",27,1);imm("AND",27,8);o("JNZ",27,"fall");imm("ADD",18,1);
      o("MOV",22,16);imm("DIV",22,10);imm("MUL",22,2);o("LI",27,20);o("SUB",27,22);o("MOV",22,27);o("LI",31,4);o("JLT",31,22,"speedready");o("LI",22,4);
      l("speedready");o("JLT",18,22,"draw");l("fall");o("LI",18,0);imm("ADD",11,1);call("test");o("JZ",8,"draw");
      l("land");imm("SUB",11,1);call("lock");o("JMP","draw");
      l("mask");o("MOV",4,13);imm("MUL",4,4);o("ADD",4,12);imm("ADD",4,256);o("LD",26,4);o("RET");
      l("test");graphics();o("HIT");o("RET");
      l("spawn");o("MOV",13,14);o("LI",31,7);o("RAND",14,31);o("LI",10,3);o("LI",11,0);o("LI",12,0);o("LI",18,0);o("MOV",15,13);imm("ADD",15,1);call("mask");call("test");o("JZ",8,"spawnok");o("LI",20,1);tone(36,650,0xa02);l("spawnok");o("RET");
      l("lock");tone(43,100,0xa02);graphics();o("STAMP");o("LI",29,19);
      l("row");o("LI",28,0);
      l("cell");o("MOV",30,29);imm("MUL",30,10);o("ADD",30,28);o("LD",0,30);o("JZ",0,"nextrow");imm("ADD",28,1);o("LI",31,10);o("JLT",28,31,"cell");
      imm("ADD",16,1);imm("ADD",17,100);tone(84,180,0xb01);o("MOV",28,29);
      l("shiftrow");o("JZ",28,"cleartop");o("LI",27,0);
      l("shiftcell");o("MOV",30,28);imm("SUB",30,1);imm("MUL",30,10);o("ADD",30,27);o("LD",0,30);imm("ADD",30,10);o("ST",30,0);imm("ADD",27,1);o("LI",31,10);o("JLT",27,31,"shiftcell");imm("SUB",28,1);o("JMP","shiftrow");
      l("cleartop");o("LI",27,0);o("LI",0,0);l("zero");o("ST",27,0);imm("ADD",27,1);o("LI",31,10);o("JLT",27,31,"zero");o("JMP","row");
      l("nextrow");imm("SUB",29,1);o("LI",31,0);o("JLT",29,31,"cleared");o("JMP","row");l("cleared");call("spawn");o("RET");
      l("draw");o("LI",0,0);o("CLEAR",0);o("LI",4,0);o("LI",5,10);o("LI",6,20);o("LI",1,1);o("LI",2,7);o("BLIT");
      o("MOV",0,26);o("MOV",1,10);imm("ADD",1,1);o("MOV",2,11);imm("ADD",2,7);o("MOV",3,15);o("SPRITE");
      o("MOV",4,14);imm("MUL",4,4);imm("ADD",4,256);o("LD",0,4);o("LI",1,12);o("LI",2,8);o("MOV",3,14);imm("ADD",3,1);o("SPRITE");
      o("LI",3,8);o("JZ",19,"overcolor");o("LI",3,14);l("overcolor");o("JZ",20,"border");o("LI",3,9);
      l("border");o("LI",2,6);l("vertical");o("LI",1,0);o("PIXEL",1,2,3);o("LI",1,11);o("PIXEL",1,2,3);imm("ADD",2,1);o("LI",31,28);o("JLT",2,31,"vertical");
      o("LI",1,0);l("horizontal");o("LI",2,6);o("PIXEL",1,2,3);o("LI",2,27);o("PIXEL",1,2,3);imm("ADD",1,1);o("LI",31,12);o("JLT",1,31,"horizontal");
      o("LI",3,8);o("MOV",0,16);imm("SHR",0,4);o("LI",1,1);o("LI",2,0);o("DIGIT");o("MOV",0,16);o("LI",1,5);o("DIGIT");
      o("MOV",27,16);imm("DIV",27,10);imm("ADD",27,1);o("MOV",0,27);o("LI",1,12);o("DIGIT");
      o("OUT",0,17);o("OUT",1,16);o("OUT",2,27);o("OUT",3,19);o("OUT",4,20);o("OUT",5,14);o("PRESENT");o("WAIT",1);o("JMP","loop");
      return a.text();
   }
   static int[] falling(){return RasterVM.assemble(fallingSource());}
   static int[] testPattern(){return RasterVM.assemble("LI 0 1\nCLEAR 0\nPRESENT\nWAIT 20\nLI 0 2\nCLEAR 0\nPRESENT\nWAIT 20\nJMP 0\n");}
}
