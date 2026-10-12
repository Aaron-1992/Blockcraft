package com.blockcraft.pe;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Low-latency Android AudioTrack SFX & Ambient Music Engine for Blockcraft PE
 * on Samsung Galaxy A55 5G (API 26..34).
 */
public final class PeAudio {
   private static final int SAMPLE_RATE = 22050;
   private final Map<String, short[]> sfxCache = new HashMap<String, short[]>();
   private final Random rng = new Random(551480L);
   private boolean enabled = true;

   public PeAudio() {
      generateCoreSfx();
   }

   private void generateCoreSfx() {
      this.sfxCache.put("click", synthClick(0.045F, 920.0F));
      this.sfxCache.put("pop", synthPop(0.075F, 520.0F, 980.0F));
      this.sfxCache.put("dig", synthNoiseBurst(0.11F, 0.55F, 480.0F));
      this.sfxCache.put("step", synthNoiseBurst(0.065F, 0.30F, 340.0F));
      this.sfxCache.put("swing", synthWhoosh(0.12F));
      this.sfxCache.put("hit", synthHit(0.14F));
      this.sfxCache.put("gun", synthGunshot(0.18F));
      this.sfxCache.put("explode", synthExplosion(0.55F));
      this.sfxCache.put("levelup", synthChime(0.38F));
   }

   public void playSound(String key, float volume, float pitch) {
      if (key == null) return;
      String mapped = "click";
      if (key.startsWith("bow") || key.startsWith("gun")) mapped = "gun";
      else if (key.contains("explode")) mapped = "explode";
      else if (key.contains("ignite") || key.contains("click") || key.contains("pop") || key.contains("eat")) mapped = "pop";
      else if (key.contains("dig")) mapped = "dig";
      else if (key.contains("step")) mapped = "step";
      else if (key.contains("swing")) mapped = "swing";
      else if (key.contains("hit") || key.contains("hurt")) mapped = "hit";
      else if (key.contains("slime") || key.contains("level")) mapped = "levelup";
      playSfx(mapped, volume, pitch);
   }

   public void playSfx(final String name, final float volume, final float pitch) {
      if (!this.enabled) return;
      final short[] pcm = this.sfxCache.get(name);
      if (pcm == null || pcm.length == 0) return;

      new Thread(new Runnable() {
         @Override
         public void run() {
            try {
               int rate = Math.max(8000, Math.min(48000, (int)(SAMPLE_RATE * pitch)));
               int byteLen = pcm.length * 2;
               AudioTrack track;
               if (Build.VERSION.SDK_INT >= 26) {
                  track = new AudioTrack.Builder()
                     .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                     .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                     .setBufferSizeInBytes(byteLen)
                     .setTransferMode(AudioTrack.MODE_STATIC)
                     .build();
               } else {
                  track = new AudioTrack(
                     AudioManager.STREAM_MUSIC, rate,
                     AudioFormat.CHANNEL_OUT_MONO,
                     AudioFormat.ENCODING_PCM_16BIT,
                     byteLen, AudioTrack.MODE_STATIC
                  );
               }
               track.write(pcm, 0, pcm.length);
               float v = Math.max(0.0F, Math.min(1.0F, volume));
               track.setVolume(v);
               track.play();
               Thread.sleep(Math.max(40L, (pcm.length * 1000L) / rate + 30L));
               track.release();
            } catch (Throwable ignored) {
            }
         }
      }, "pe-sfx").start();
   }

   private short[] synthClick(float durSec, float freq) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      for (int i = 0; i < n; i++) {
         float t = i / (float)SAMPLE_RATE;
         float env = (float)Math.exp(-t * 65.0F);
         float wave = (float)Math.sin(2.0 * Math.PI * freq * t);
         out[i] = (short)(wave * env * 14000.0F);
      }
      return out;
   }

   private short[] synthPop(float durSec, float f0, float f1) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float t = i / (float)SAMPLE_RATE;
         float f = f0 + (f1 - f0) * u;
         float env = (1.0F - u) * (1.0F - u);
         out[i] = (short)(Math.sin(2.0 * Math.PI * f * t) * env * 16000.0F);
      }
      return out;
   }

   private short[] synthNoiseBurst(float durSec, float amp, float toneHz) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      float lp = 0.0F;
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float env = (float)Math.pow(1.0F - u, 1.8);
         float white = (this.rng.nextFloat() * 2.0F - 1.0F);
         lp = lp * 0.72F + white * 0.28F;
         float tone = (float)Math.sin(2.0 * Math.PI * toneHz * (i / (float)SAMPLE_RATE)) * 0.35F;
         out[i] = (short)((lp + tone) * env * amp * 18000.0F);
      }
      return out;
   }

   private short[] synthWhoosh(float durSec) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      float lp = 0.0F;
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float env = (float)Math.sin(u * Math.PI);
         float white = (this.rng.nextFloat() * 2.0F - 1.0F);
         lp = lp * 0.86F + white * 0.14F;
         out[i] = (short)(lp * env * 14000.0F);
      }
      return out;
   }

   private short[] synthHit(float durSec) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float t = i / (float)SAMPLE_RATE;
         float env = (float)Math.exp(-u * 6.5F);
         float punch = (float)Math.sin(2.0 * Math.PI * (180.0F - 110.0F * u) * t);
         float noise = (this.rng.nextFloat() * 2.0F - 1.0F) * 0.45F;
         out[i] = (short)((punch + noise) * env * 19000.0F);
      }
      return out;
   }

   private short[] synthGunshot(float durSec) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float env = (float)Math.exp(-u * 9.0F);
         float crack = (this.rng.nextFloat() * 2.0F - 1.0F);
         float boom = (float)Math.sin(2.0 * Math.PI * 145.0F * (i / (float)SAMPLE_RATE));
         out[i] = (short)((crack * 0.7F + boom * 0.5F) * env * 22000.0F);
      }
      return out;
   }

   private short[] synthExplosion(float durSec) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      float lp = 0.0F;
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float env = (float)Math.pow(1.0F - u, 1.5);
         float white = (this.rng.nextFloat() * 2.0F - 1.0F);
         lp = lp * 0.91F + white * 0.09F;
         float rumble = (float)Math.sin(2.0 * Math.PI * 62.0F * (i / (float)SAMPLE_RATE)) * 0.5F;
         out[i] = (short)Math.max(-32000, Math.min(32000, (int)((lp * 1.4F + rumble) * env * 24000.0F)));
      }
      return out;
   }

   private short[] synthChime(float durSec) {
      int n = (int)(SAMPLE_RATE * durSec);
      short[] out = new short[n];
      float[] notes = new float[]{523.25F, 659.25F, 783.99F, 1046.50F};
      for (int i = 0; i < n; i++) {
         float u = i / (float)n;
         float t = i / (float)SAMPLE_RATE;
         int idx = Math.min(notes.length - 1, (int)(u * notes.length));
         float f = notes[idx];
         float env = (1.0F - u);
         out[i] = (short)(Math.sin(2.0 * Math.PI * f * t) * env * 13000.0F);
      }
      return out;
   }
}
