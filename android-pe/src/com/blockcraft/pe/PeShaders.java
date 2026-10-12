package com.blockcraft.pe;

import android.opengl.GLES20;

/**
 * OpenGL ES 2.0 / 3.0 GLSL Shader Pipeline for Blockcraft PE (Samsung Galaxy A55 5G Xclipse 530 GPU).
 */
public final class PeShaders {
   private static final String VS_3D =
      "uniform mat4 uMVP;\n" +
      "attribute vec3 aPos;\n" +
      "attribute vec2 aUV;\n" +
      "attribute vec4 aColor;\n" +
      "varying vec2 vUV;\n" +
      "varying vec4 vColor;\n" +
      "varying float vDist;\n" +
      "void main() {\n" +
      "  vec4 clip = uMVP * vec4(aPos, 1.0);\n" +
      "  gl_Position = clip;\n" +
      "  vUV = aUV;\n" +
      "  vColor = aColor;\n" +
      "  vDist = length(clip.xyz);\n" +
      "}\n";

   private static final String FS_3D =
      "precision mediump float;\n" +
      "uniform sampler2D uTex;\n" +
      "uniform vec3 uFogColor;\n" +
      "uniform vec2 uFogRange;\n" +
      "uniform vec4 uTint;\n" +
      "varying vec2 vUV;\n" +
      "varying vec4 vColor;\n" +
      "varying float vDist;\n" +
      "void main() {\n" +
      "  vec4 tex = texture2D(uTex, vUV) * vColor * uTint;\n" +
      "  if (tex.a < 0.12) discard;\n" +
      "  float fog = clamp((vDist - uFogRange.x) / max(1.0, uFogRange.y - uFogRange.x), 0.0, 1.0);\n" +
      "  vec3 rgb = mix(tex.rgb, uFogColor, fog * fog);\n" +
      "  gl_FragColor = vec4(rgb, tex.a);\n" +
      "}\n";

   private static final String VS_2D =
      "uniform mat4 uOrtho;\n" +
      "attribute vec2 aPos;\n" +
      "attribute vec2 aUV;\n" +
      "attribute vec4 aColor;\n" +
      "varying vec2 vUV;\n" +
      "varying vec4 vColor;\n" +
      "void main() {\n" +
      "  gl_Position = uOrtho * vec4(aPos, 0.0, 1.0);\n" +
      "  vUV = aUV;\n" +
      "  vColor = aColor;\n" +
      "}\n";

   private static final String FS_2D =
      "precision mediump float;\n" +
      "uniform sampler2D uTex;\n" +
      "varying vec2 vUV;\n" +
      "varying vec4 vColor;\n" +
      "void main() {\n" +
      "  vec4 tex = texture2D(uTex, vUV) * vColor;\n" +
      "  if (tex.a < 0.04) discard;\n" +
      "  gl_FragColor = tex;\n" +
      "}\n";

   public int prog3D;
   public int uMVP;
   public int uTex3D;
   public int uFogColor;
   public int uFogRange;
   public int uTint;
   public int aPos3D;
   public int aUV3D;
   public int aColor3D;

   public int prog2D;
   public int uOrtho;
   public int uTex2D;
   public int aPos2D;
   public int aUV2D;
   public int aColor2D;

   public void initGL() {
      this.prog3D = linkProgram(VS_3D, FS_3D);
      this.uMVP = GLES20.glGetUniformLocation(this.prog3D, "uMVP");
      this.uTex3D = GLES20.glGetUniformLocation(this.prog3D, "uTex");
      this.uFogColor = GLES20.glGetUniformLocation(this.prog3D, "uFogColor");
      this.uFogRange = GLES20.glGetUniformLocation(this.prog3D, "uFogRange");
      this.uTint = GLES20.glGetUniformLocation(this.prog3D, "uTint");
      this.aPos3D = GLES20.glGetAttribLocation(this.prog3D, "aPos");
      this.aUV3D = GLES20.glGetAttribLocation(this.prog3D, "aUV");
      this.aColor3D = GLES20.glGetAttribLocation(this.prog3D, "aColor");

      this.prog2D = linkProgram(VS_2D, FS_2D);
      this.uOrtho = GLES20.glGetUniformLocation(this.prog2D, "uOrtho");
      this.uTex2D = GLES20.glGetUniformLocation(this.prog2D, "uTex");
      this.aPos2D = GLES20.glGetAttribLocation(this.prog2D, "aPos");
      this.aUV2D = GLES20.glGetAttribLocation(this.prog2D, "aUV");
      this.aColor2D = GLES20.glGetAttribLocation(this.prog2D, "aColor");
   }

   private static int compileShader(int type, String src) {
      int sh = GLES20.glCreateShader(type);
      GLES20.glShaderSource(sh, src);
      GLES20.glCompileShader(sh);
      return sh;
   }

   private static int linkProgram(String vsSrc, String fsSrc) {
      int vs = compileShader(GLES20.GL_VERTEX_SHADER, vsSrc);
      int fs = compileShader(GLES20.GL_FRAGMENT_SHADER, fsSrc);
      int prog = GLES20.glCreateProgram();
      GLES20.glAttachShader(prog, vs);
      GLES20.glAttachShader(prog, fs);
      GLES20.glLinkProgram(prog);
      return prog;
   }
}
