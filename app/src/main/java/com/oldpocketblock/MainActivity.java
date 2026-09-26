package com.oldpocketblock;

import android.app.*;
import android.os.*;
import android.opengl.*;
import android.view.*;
import android.content.*;
import java.nio.*;
import java.util.*;

public class MainActivity extends Activity {
 public void onCreate(Bundle b){super.onCreate(b);getWindow().setFlags(1024,1024);setContentView(new Game(this));}
 static class Game extends GLSurfaceView {
  Renderer r; float lx,ly;
  Game(Context c){super(c);setEGLContextClientVersion(2);r=new Renderer();setRenderer(r);setRenderMode(RENDERMODE_CONTINUOUSLY);}
  public boolean onTouchEvent(MotionEvent e){
   float x=e.getX(),y=e.getY();
   if(e.getAction()==0){lx=x;ly=y;r.touchStart(x,y);return true;}
   if(e.getAction()==2){r.yaw+=(x-lx)*.35f;r.pitch+=(y-ly)*.28f;r.pitch=Math.max(-75,Math.min(75,r.pitch));lx=x;ly=y;return true;}
   if(e.getAction()==1){r.touchEnd(x,y);return true;} return true;
  }
 }
 static class Renderer implements GLSurfaceView.Renderer {
  FloatBuffer vb; int program; float yaw=0,pitch=10;
  float px=0,py=2.2f,pz=5,vy=0; boolean grounded=false;
  boolean forward,back,left,right; int selected=1;
  int[][][] w=new int[24][12][24];
  float[] proj=new float[16],view=new float[16],model=new float[16];
  final float[] cube={
   -.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,-.5f,
   -.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f,
   -.5f,-.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,.5f,-.5f,-.5f,.5f,
   .5f,-.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,
   -.5f,.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,-.5f,.5f,.5f,
   -.5f,-.5f,-.5f,.5f,-.5f,-.5f,.5f,-.5f,.5f,-.5f,-.5f,.5f};
  final short[] idx={0,1,2,0,2,3,4,5,6,4,6,7,8,9,10,8,10,11,12,13,14,12,14,15,16,17,18,16,18,19,20,21,22,20,22,23};
  Renderer(){
   vb=ByteBuffer.allocateDirect(cube.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();vb.put(cube).position(0);
   Random r=new Random(17);
   for(int x=0;x<24;x++)for(int z=0;z<24;z++){
    int h=1+(int)(2+Math.sin(x*.45)*1.2+Math.cos(z*.38)*1.1+r.nextDouble()*1.2);
    h=Math.max(1,Math.min(5,h));
    for(int y=0;y<h;y++)w[x][y][z]=(y==h-1?1:(y>h-3?2:3));
   }
   // small tree
   for(int y=3;y<6;y++)w[14][y][10]=4;
   for(int x=12;x<=16;x++)for(int z=8;z<=12;z++)w[x][6][z]=5;
  }
  public void onSurfaceCreated(GL10 g,EGLConfig c){
   GLES20.glClearColor(.52f,.76f,.96f,1);GLES20.glEnable(GLES20.GL_DEPTH_TEST);
   String vs="attribute vec3 a;uniform mat4 mvp;void main(){gl_Position=mvp*vec4(a,1.0);}";
   String fs="precision mediump float;uniform vec4 col;void main(){gl_FragColor=col;}";
   int a=GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);GLES20.glShaderSource(a,vs);GLES20.glCompileShader(a);
   int b=GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);GLES20.glShaderSource(b,fs);GLES20.glCompileShader(b);
   program=GLES20.glCreateProgram();GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program);
  }
  public void onSurfaceChanged(GL10 g,int ww,int hh){GLES20.glViewport(0,0,ww,hh);float ar=(float)ww/hh;Matrix.frustumM(proj,0,-ar,ar,-1,1,1,100);}
  boolean solid(float x,float y,float z){int X=(int)Math.floor(x),Y=(int)Math.floor(y),Z=(int)Math.floor(z);return X>=0&&X<24&&Y>=0&&Y<12&&Z>=0&&Z<24&&w[X][Y][Z]!=0;}
  void physics(){
   float a=(float)Math.toRadians(yaw),sx=(float)Math.sin(a),cz=(float)Math.cos(a);
   float dx=0,dz=0;if(forward){dx+=sx;dz-=cz;}if(back){dx-=sx;dz+=cz;}if(left){dx-=cz;dz-=sx;}if(right){dx+=cz;dz+=sx;}
   float len=(float)Math.sqrt(dx*dx+dz*dz);if(len>0){dx/=len;dz/=len;dx*=.10;dz*=.10;}
   if(!solid(px+dx,py,pz)&&!solid(px+dx,py+1,pz))px+=dx;
   if(!solid(px,py,pz+dz)&&!solid(px,py+1,pz+dz))pz+=dz;
   vy-=.012f;float ny=py+vy;
   if(vy<=0 && (solid(px,ny-.15f,pz)||solid(px+.25f,ny-.15f,pz)||solid(px-.25f,ny-.15f,pz))){py=(float)Math.floor(ny-.15f)+1.15f;vy=0;grounded=true;}else{py=ny;grounded=false;}
   if(py<1)py=2;
  }
  void block(float x,float y,float z,float[] c){
   Matrix.setIdentityM(model,0);Matrix.translateM(model,0,x,y,z);float[] t=new float[16],m=new float[16];Matrix.multiplyMM(t,0,view,0,model,0);Matrix.multiplyMM(m,0,proj,0,t,0);
   GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program,"mvp"),1,false,m,0);GLES20.glUniform4fv(GLES20.glGetUniformLocation(program,"col"),1,c,0);
   GLES20.glVertexAttribPointer(0,3,GLES20.GL_FLOAT,false,0,vb);GLES20.glEnableVertexAttribArray(0);
   ShortBuffer ib=ByteBuffer.allocateDirect(idx.length*2).order(ByteOrder.nativeOrder()).asShortBuffer();ib.put(idx).position(0);GLES20.glDrawElements(GLES20.GL_TRIANGLES,idx.length,GLES20.GL_UNSIGNED_SHORT,ib);
  }
  public void onDrawFrame(GL10 g){
   physics();GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT|GLES20.GL_DEPTH_BUFFER_BIT);
   float rad=(float)Math.toRadians(yaw),pr=(float)Math.toRadians(pitch);
   float dx=(float)Math.sin(rad)*Math.cos(pr),dy=(float)Math.sin(pr),dz=-(float)Math.cos(rad)*Math.cos(pr);
   Matrix.setLookAtM(view,0,px,py+1.55f,pz,px+dx,py+1.55f+dy,pz+dz,0,1,0);
   GLES20.glUseProgram(program);
   for(int x=0;x<24;x++)for(int y=0;y<12;y++)for(int z=0;z<24;z++)if(w[x][y][z]!=0){
    int b=w[x][y][z];float[] c=b==1?new float[]{.36f,.67f,.22f,1}:b==2?new float[]{.48f,.32f,.18f,1}:b==3?new float[]{.42f,.42f,.42f,1}:b==4?new float[]{.38f,.25f,.13f,1}:new float[]{.22f,.50f,.20f,1};
    block(x,y,z,c);
   }
  }
  void touchStart(float x,float y){}
  void touchEnd(float x,float y){
   // left third: movement forward/back, right third: action
   // short tap near right edge places/breaks the block along view ray
   if(x>getWidth()/2 && Math.abs(y-getHeight()/2)<getHeight()*.28) editBlock();
  }
  void editBlock(){
   float a=(float)Math.toRadians(yaw),p=(float)Math.toRadians(pitch);
   float dx=(float)Math.sin(a)*Math.cos(p),dy=(float)Math.sin(p),dz=-(float)Math.cos(a)*Math.cos(p);
   int lastX=-1,lastY=-1,lastZ=-1;
   for(float t=.5f;t<5;t+=.08f){
    int x=(int)Math.floor(px+dx*t),y=(int)Math.floor(py+1.5f+dy*t),z=(int)Math.floor(pz+dz*t);
    if(x<0||x>=24||y<0||y>=12||z<0||z>=24)continue;
    if(w[x][y][z]!=0){
     if(lastX>=0)w[lastX][lastY][lastZ]=selected; else w[x][y][z]=0; return;
    }
    lastX=x;lastY=y;lastZ=z;
   }
  }
 }
}