#!/usr/bin/env python3
"""Original silhouette animation and original synthesized score. No external media inputs.
Requires Pillow. Output: portable BCV1 cartridge (64x48, 20 fps, 32 s, mono 44.1 kHz).
BCV1 is gzip of seven big-endian int32s (magic,w,h,fps,frames,rate,samples),
then row-major 1-bit frames (MSB first), followed by signed big-endian PCM16.
"""
import array, gzip, math, random, struct, sys, wave
from pathlib import Path
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[2]
W,H,FPS,N,RATE=64,48,20,640,44100
S=4

def frame(i):
    t=i/FPS; scene=int(t//8); u=t%8
    bg=255 if scene in (1,3) else 0; fg=255-bg
    im=Image.new('L',(W*S,H*S),bg);d=ImageDraw.Draw(im)
    def poly(p,c=fg):d.polygon([(round(x*S),round(y*S)) for x,y in p],fill=c)
    def circ(x,y,r,c=fg):d.ellipse((round((x-r)*S),round((y-r)*S),round((x+r)*S),round((y+r)*S)),fill=c)
    def line(p,width=1,c=fg):
        d.line([(round(x*S),round(y*S)) for x,y in p],fill=c,width=max(1,round(width*S)),joint='curve')
        for x,y in (p[0],p[-1]):circ(x,y,width/2,c)
    def dancer(cx,cy,phase,scale=1):
        def pt(x,y):return cx+x*scale,cy+y*scale
        # An original scarf-wearing figure, not a traced character or borrowed choreography.
        sway=math.sin(phase)*1.6;head=pt(sway,-13)
        circ(*head,3.3*scale);line([pt(sway,-10),pt(-sway*.3,0)],4*scale)
        poly([pt(sway-2,-8),pt(sway+2,-8),pt(5+2*math.sin(phase),6),pt(-5+math.sin(phase),6)])
        left=pt(-10+2*math.cos(phase),-8-7*math.sin(phase*.7));right=pt(10,-7-8*math.cos(phase*.7))
        line([pt(sway,-8),pt(-5,-6),left],2.2*scale);line([pt(sway,-8),pt(5,-5),right],2.2*scale)
        line([pt(-1,4),pt(-4-2*math.sin(phase),12),pt(-7,13)],2.4*scale)
        line([pt(1,4),pt(4+2*math.sin(phase),12),pt(7,13)],2.4*scale)
        poly([pt(sway,-10),pt(sway+2,-8),pt(13+3*math.sin(phase),-9),pt(5,-12)])
    def bird(x,y,size,phase):
        flap=math.sin(phase)*5
        poly([(x-size,y-5-flap),(x-3,y-2),(x,y),(x+3,y-2),(x+size,y-5-flap),(x+size*.55,y+2),(x,y+3),(x-size*.55,y+2)])
    if scene==0:
        # A crescent, falling seeds and a moving figure, visible even on the paused first frame.
        circ(50,10,7);circ(53,8,6,bg)
        for k in range(8):
            x=(k*17+u*2)%64;y=(k*11+u*3)%43
            circ(x,y,.6+(.3 if k%2 else 0))
        dancer(29+2*math.sin(u*.5),29, u*1.8)
        line([(5,44),(59,44)],.7)
    elif scene==1:
        # Daylight flight: a bird passes an orbital ring, petals become a second dancer.
        d.ellipse((7*S,4*S,57*S,44*S),outline=fg,width=S)
        if u<4:
            bird(12+u*10,24+5*math.sin(u),10,u*4)
            for k in range(5):bird((k*19+u*7)%76-6,8+k%3*12,3,u*4+k)
        else:
            dancer(32,28,(u-4)*2,1.05)
            for k in range(7):
                a=k*math.tau/7+u*.7;x=32+22*math.cos(a);y=24+18*math.sin(a)
                poly([(x-2,y),(x,y-3),(x+2,y),(x,y+3)])
    elif scene==2:
        # Skyline, a travelling figure, then a rising flock. Same original geometry.
        for k in range(12):
            x=k*6;h=5+(k*7)%13;d.rectangle((x*S,(48-h)*S,(x+4)*S,48*S),fill=fg)
            for yy in range(49-h,46,4):d.rectangle(((x+1)*S,yy*S,(x+2)*S,(yy+1)*S),fill=bg)
        circ(49,10,6)
        if u<5:dancer(18+u*4,26,u*2.5,.8)
        else:
            for k in range(6):bird((k*13+u*6)%70-3,31-(u-5)*7+k%3*4,4,u*5+k)
    else:
        # A fan of leaves closes into a quiet title card. No rapid full-screen strobe.
        if u<4:
            for k in range(8):
                a=k*math.tau/8+u*.6;r=10+u*3
                x,y=32+math.cos(a)*r,24+math.sin(a)*r
                poly([(32,24),(x-3*math.sin(a),y+3*math.cos(a)),(x*1.1-3.2,y*1.1-2.4),(x+3*math.sin(a),y-3*math.cos(a))])
            circ(32,24,3,bg)
        else:
            # Pillow's built-in bitmap font is used only for this tiny original title.
            small=Image.new('L',(W,H),bg);sd=ImageDraw.Draw(small)
            for text,y in [('SHADOW',12),('SIGNAL',25)]:
                box=sd.textbbox((0,0),text);sd.text(((64-(box[2]-box[0]))//2,y),text,fill=fg)
            im=small.resize((W*S,H*S),Image.Resampling.NEAREST)
    return im.resize((W,H),Image.Resampling.LANCZOS).point(lambda p:255 if p>=128 else 0).convert('1')

def soundtrack():
    rng=random.Random(6100610);out=array.array('h');last_noise=0
    chords=((57,60,64),(53,57,60),(55,59,62),(52,55,59))
    motifs=((69,72,76,74,72,67,69,64),(65,69,72,76,74,72,69,65),(67,71,74,79,76,74,71,67),(64,67,71,74,72,71,67,64))
    def freq(note):return 440*2**((note-69)/12)
    for j in range(N*RATE//FPS):
        t=j/RATE;bar=int(t/2);chord=chords[bar%4];eighth=int(t/.25);et=t% .25;beat=int(t/.5);bt=t% .5
        note=motifs[bar%4][eighth%8]+(12 if 8<=bar<12 else 0);ph=freq(note)*t
        triangle=2/math.pi*math.asin(math.sin(math.tau*ph));lead=.22*triangle*min(1,et/.008)*math.exp(-et*10)
        bass=.2*math.sin(math.tau*freq(chord[0]-24)*t)*min(1,bt/.008)*math.exp(-bt*5)
        pad=sum(math.sin(math.tau*freq(n)*t) for n in chord)*.035*(.7+.3*math.sin(math.pi*(t%2)/2))
        kick=.28*math.sin(math.tau*(48*bt+2*(1-math.exp(-bt*35))))*math.exp(-bt*28)
        noise=rng.uniform(-1,1);hat=(noise-last_noise)*.035*math.exp(-et*85);last_noise=noise
        snare=noise*.09*math.exp(-bt*38) if beat%2 else 0
        envelope=min(1,t/.08,max(0,(32-t)/1.5))
        out.append(round(26000*math.tanh((lead+bass+pad+kick+hat+snare)*envelope)))
    return out

def main():
    frames=[frame(i) for i in range(N)];pcm=soundtrack();data=b''.join(f.tobytes() for f in frames)
    big=array.array('h',pcm)
    if sys.byteorder=='little':big.byteswap()
    header=struct.pack('>7i',0x42435631,W,H,FPS,N,RATE,len(pcm))
    path=ROOT/'res/media/shadow-signal.bcv';path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(gzip.compress(header+data+big.tobytes(),compresslevel=9,mtime=0))
    preview=ROOT/'build/media-preview';preview.mkdir(parents=True,exist_ok=True)
    for i in (0,60,120,200,280,360,440,520,600):frames[i].resize((512,384),Image.Resampling.NEAREST).save(preview/f'frame-{i:03}.png')
    with wave.open(str(preview/'Shadow-Signal-original.wav'),'wb') as w:
        w.setnchannels(1);w.setsampwidth(2);w.setframerate(RATE);little=array.array('h',pcm)
        if sys.byteorder!='little':little.byteswap()
        w.writeframes(little.tobytes())
    print(path, path.stat().st_size, 'bytes; 640 frames; 32 seconds; peak',max(abs(x) for x in pcm))
if __name__=='__main__':main()
