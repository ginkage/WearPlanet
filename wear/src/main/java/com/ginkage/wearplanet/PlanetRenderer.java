package com.ginkage.wearplanet;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class PlanetRenderer implements Renderer {
	private static final String TAG = "WearPlanetActivity";

	private int quadProgram;
	private int qvPosition;
	private int quRatio;
	private int quTexture0;
	private int quTexture1;
	private int quTexture2;
	private int quTexture3;
	private int quRotate;

	float ratioX, ratioY;

	private int quadVB;

	private int planetTex;
	private int offsetTex1;
	private int offsetTex2;
	private int normalTex;

	private final int[] genbuf = new int[1];

	public float fps = 0;
	private long start_frame;
	private long frames_drawn;

	private long prevTime = -1;
	public float rotateAngle = 0;
	public float tiltAngle = 0;
	public int screenWidth = 0;
	public int screenHeight = 0;

	public float scaleFactor = 1;
	public double rotateSpeed = -0.125f;
	public double tiltSpeed = 0;

	private final Context mContext;

	public PlanetRenderer(Context context)
	{
		super();
		mContext = context;
	}

	@Override
	public void onDrawFrame(GL10 arg0)
	{
		long curTime = SystemClock.uptimeMillis();

		if (curTime > start_frame + 1000) {
			fps = frames_drawn * 1000.0f / (curTime - start_frame);
			start_frame = curTime;
			frames_drawn = 0;
		}

		if (prevTime < 0) prevTime = curTime;
		double delta = (curTime - prevTime) / 1000.0f;
		prevTime = curTime;

		rotateAngle += delta * rotateSpeed;
		rotateAngle -= Math.floor(rotateAngle);

		tiltAngle += delta * tiltSpeed;
		while (tiltAngle > 2) tiltAngle -= 2;
		while (tiltAngle < 0) tiltAngle += 2;

		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, planetTex);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offsetTex1);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, offsetTex2);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, normalTex);

		GLES20.glUseProgram(quadProgram);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);

		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, quadVB);
		GLES20.glEnableVertexAttribArray(qvPosition);
		GLES20.glVertexAttribPointer(qvPosition, 2, GLES20.GL_FLOAT, false, 8, 0);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		GLES20.glUniform1i(quTexture0, 0);
		GLES20.glUniform1i(quTexture1, 1);
		GLES20.glUniform1i(quTexture2, 2);
		GLES20.glUniform1i(quTexture3, 3);

		double ta = tiltAngle * Math.PI;
		GLES20.glUniform3f(quRotate, rotateAngle, (float) Math.sin(ta), (float) Math.cos(ta));

		float minScale = 0.5f, maxScale = 2.0f / (ratioX < ratioY ? ratioX : ratioY);
		if (scaleFactor < minScale) scaleFactor = minScale;
		if (scaleFactor > maxScale) scaleFactor = maxScale;

		GLES20.glUniform4f(quRatio, ratioX * scaleFactor, ratioY * scaleFactor, 1, 1);

		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

		GLES20.glDisableVertexAttribArray(qvPosition);
		GLES20.glDisable(GLES20.GL_BLEND);

		frames_drawn++;
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height)
	{
		GLES20.glViewport(0, 0, width, height);

		screenWidth = width;
		screenHeight = height;

		if (width < height) {
			ratioX = 1;
			ratioY = width / (float)height;
		}
		else {
			ratioX = height / (float)width;
			ratioY = 1;
		}

		initPlanet();

		start_frame = SystemClock.uptimeMillis();
		frames_drawn = 0;
		fps = 0;
	}

	private String readRawTextFile(int resId)
	{
		InputStream inputStream = mContext.getResources().openRawResource(resId);
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				sb.append(line).append("\n");
			}
			reader.close();
			return sb.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private int loadGLShader(int type, int resId)
	{
		String code = readRawTextFile(resId);
		int shader = GLES20.glCreateShader(type);
		GLES20.glShaderSource(shader, code);
		GLES20.glCompileShader(shader);

		// Get the compilation status.
		final int[] compileStatus = new int[1];
		GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

		// If the compilation failed, delete the shader.
		if (compileStatus[0] == 0) {
			Log.e(TAG, "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
			GLES20.glDeleteShader(shader);
			shader = 0;
		}

		if (shader == 0) {
			throw new RuntimeException("Error creating shader.");
		}

		return shader;
	}

	private int Compile(int vsId, int fsId)
	{
		int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, vsId);
		int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, fsId);

		int prog = GLES20.glCreateProgram();			 // create empty OpenGL Program
		GLES20.glAttachShader(prog, vertexShader);   // add the vertex shader to program
		GLES20.glAttachShader(prog, fragmentShader); // add the fragment shader to program
		GLES20.glLinkProgram(prog);				  // creates OpenGL program executables

		return prog;
	}

	int loadTexture(final Context context, final int resourceId)
	{
		GLES20.glGenTextures(1, genbuf, 0);
		int tex = genbuf[0];

		if (tex != 0) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inScaled = false;
			final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);
			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			bitmap.recycle();
		}

		return tex;
	}

	int arrayTexture(int texSize, int[] pixels)
	{
		GLES20.glGenTextures(1, genbuf, 0);
		int tex = genbuf[0];
		if (tex != 0) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, texSize, texSize, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, IntBuffer.wrap(pixels));
		}
		return tex;
	}

	private void initPlanet()
	{
		int texSize = 1024;
		double r = texSize * 0.5;
		int[] pixels = new int[texSize * texSize];

		for (int row = 0, idx = 0; row < texSize; row++) {
			double y = (r - row) / r;
			double sin_theta = Math.sqrt(1 - y*y);
			double theta = Math.acos(y);
			long v = Math.round(16383 * theta / Math.PI);

			for (int col = 0; col < texSize; col++) {
				double x = (r - col) / r;
				long u = 0;

				if (x >= -sin_theta && x <= sin_theta) {
					double z = Math.sqrt(1 - y*y - x*x);
					double phi = Math.atan2(z, x);
					u = Math.round(16383 * phi / (2 * Math.PI));
				}

				pixels[idx++] = (int) ((v << 16) + u);
			}
		}

		offsetTex1 = arrayTexture(texSize, pixels);

		for (int row = 0, idx = 0; row < texSize; row++) {
			double z = (row - r) / r;
			double x_limit = Math.sqrt(1 - z*z);

			for (int col = 0; col < texSize; col++) {
				double x = (r - col) / r;
				long u = 0, v = 0;

				if (x >= -x_limit && x <= x_limit) {
					double y = Math.sqrt(1 - z*z - x*x);
					double phi = Math.atan2(z, x);
					double theta = Math.acos(y);

					if (phi < 0) phi += (2 * Math.PI);
					u = Math.round(16383 * phi / (2 * Math.PI));
					v = Math.round(16383 * theta / Math.PI);
				}

				pixels[idx++] = (int) ((v << 16) + u);
			}
		}

		offsetTex2 = arrayTexture(texSize, pixels);

		planetTex = loadTexture(mContext, R.drawable.planet);
		normalTex = loadTexture(mContext, R.drawable.normal);
	}

	private int createBuffer(float[] buffer)
	{
		FloatBuffer floatBuf = ByteBuffer.allocateDirect(buffer.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		floatBuf.put(buffer);
		floatBuf.position(0);

		GLES20.glGenBuffers(1, genbuf, 0);
		int glBuf = genbuf[0];
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, glBuf);
		GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, buffer.length * 4, floatBuf, GLES20.GL_STATIC_DRAW);
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

		return glBuf;
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config)
	{
		GLES20.glClearColor(0, 0, 0, 1);

		quadProgram = Compile(R.raw.quad_vertex, R.raw.quad_fragment);
		qvPosition = GLES20.glGetAttribLocation(quadProgram, "vPosition");
		quRatio = GLES20.glGetUniformLocation(quadProgram, "uRatio");
		quTexture0 = GLES20.glGetUniformLocation(quadProgram, "uTexture0");
		quTexture1 = GLES20.glGetUniformLocation(quadProgram, "uTexture1");
		quTexture2 = GLES20.glGetUniformLocation(quadProgram, "uTexture2");
		quTexture3 = GLES20.glGetUniformLocation(quadProgram, "uTexture3");
		quRotate = GLES20.glGetUniformLocation(quadProgram, "uRotate");

		final float quad[] = {
			-1,  1,
			-1, -1,
			 1,  1,
			 1, -1,
		};

		quadVB = createBuffer(quad);
	}
}
