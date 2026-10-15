#!/usr/bin/env python3
"""Pack your own PNG sequence + PCM16 WAV into a safe Blockcraft BCV1 cartridge.
Usage: python Make-media.py frames/ soundtrack.wav output.bcv --width 64 --height 48
Requires Pillow. Frames are sorted numerically, resized and thresholded to monochrome.
20 fps, maximum 120 seconds. Audio is downmixed/resampled to mono 44100, trimmed/padded.
Only use media you have permission to use. No video downloader or executable codec.
"""
import argparse, array, gzip, re, struct, sys, wave
from pathlib import Path
from PIL import Image

def pack(folder, sound, destination, width=64, height=48):
    if not 1<=width<=64 or not 1<=height<=64:raise ValueError('Dimensions must be 1..64')
    frames=sorted(Path(folder).glob('*.png'),key=lambda p:[int(x) if x.isdigit() else x.lower() for x in re.split(r'(\d+)',p.name)])
    if not 1<=len(frames)<=2400:raise ValueError('Need 1..2400 PNGs (20 frames/second)')
    bits=bytearray()
    for path in frames:
        with Image.open(path) as image:
            rgba=image.convert('RGBA');background=Image.new('RGBA',rgba.size,(0,0,0,255));background.alpha_composite(rgba)
            pixels=list(background.convert('L').resize((width,height),Image.Resampling.LANCZOS).getdata())
            frame=bytearray((width*height+7)//8)
            for i,v in enumerate(pixels):
                if v>=128:frame[i>>3]|=128>>(i&7)
            bits.extend(frame)
    if Path(sound).stat().st_size>64*1024*1024:raise ValueError('WAV must be under 64 MiB')
    with wave.open(str(sound),'rb') as wav:
        channels,rate=wav.getnchannels(),wav.getframerate()
        if wav.getsampwidth()!=2 or channels not in (1,2) or not 8000<=rate<=96000 or wav.getcomptype()!='NONE':raise ValueError('Use uncompressed PCM16 mono/stereo WAV, 8000..96000 Hz')
        source=array.array('h',wav.readframes(min(wav.getnframes(),len(frames)*rate//20+2)))
    if sys.byteorder!='little':source.byteswap()
    mono=[sum(source[i:i+channels])/channels for i in range(0,len(source)-channels+1,channels)]
    pcm=array.array('h');count=len(frames)*2205
    for i in range(count):
        p=i*rate/44100;j=int(p);f=p-j
        sample=mono[j]*(1-f)+(mono[j+1] if j+1<len(mono) else 0)*f if j<len(mono) else 0
        pcm.append(max(-32768,min(32767,round(sample))))
    if sys.byteorder=='little':pcm.byteswap()
    raw=struct.pack('>7i',0x42435631,width,height,20,len(frames),44100,count)+bits+pcm.tobytes()
    out=Path(destination);out.parent.mkdir(parents=True,exist_ok=True)
    if out.exists():raise FileExistsError('Refusing to overwrite '+str(out))
    out.write_bytes(gzip.compress(raw,compresslevel=9,mtime=0));print(out,len(frames),'frames,',count,'audio samples,',out.stat().st_size,'bytes')
if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('frames');ap.add_argument('wav');ap.add_argument('output');ap.add_argument('--width',type=int,default=64);ap.add_argument('--height',type=int,default=48);a=ap.parse_args();pack(a.frames,a.wav,a.output,a.width,a.height)
