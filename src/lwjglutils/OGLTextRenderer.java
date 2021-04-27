package lwjglutils;

import org.lwjgl.BufferUtils;
import transforms.Mat4Scale;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.ARBClearTexture.glClearTexImage;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL33.glPopAttrib;
import static org.lwjgl.opengl.GL33.glPushAttrib;

public class OGLTextRenderer {
	private int width;
	private int height;
	private Color color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
	private Font font;
	int textureID;
	BufferedImage img;
	private Viewer viewer;
	private ByteBuffer clearTexBuffer = BufferUtils.createByteBuffer(4);
	
	
	private class Viewer {
		protected final int shaderProgram;
		protected final OGLBuffers buffers;
		protected final int locMat;
		
		private  final String[] SHADER_VERT_SRC = {
				"#version 330\n",
				"in vec2 inPosition;", 
				"in vec2 inTexCoord;", 
				"uniform mat4 matTrans;",
				"out vec2 texCoords;", 
				"void main() {",
				"	gl_Position = vec4(inPosition , 0.0f, 1.0f);",
				"   texCoords = inTexCoord;",
				"}"
			};
		
		private  final String[] SHADER_FRAG_SRC = { 
				"#version 330\n",
				"in vec2 texCoords;", 
				"out vec4 fragColor;", 
				"uniform sampler2D drawTexture;",
				"void main() {",
				" 	fragColor = texture(drawTexture, texCoords);", 
				" 	if (length(fragColor.rgb) == 0)", 
				" 		fragColor.a = 0.0;", 
				"}" 
			};

		private OGLBuffers createBuffers() {
			float[] vertexBufferData = { 
					-1, -1, 0, 1, 
					1, -1, 1, 1, 
					-1, 1, 0, 0,
					1, 1, 1, 0 };
			int[] indexBufferData = { 0, 1, 2, 3 };

			OGLBuffers.Attrib[] attributes = { 
					new OGLBuffers.Attrib("inPosition", 2),
					new OGLBuffers.Attrib("inTexCoord", 2) };

			return new OGLBuffers(vertexBufferData, attributes, indexBufferData);
		}

		private Viewer() {
			buffers = createBuffers();
			this.shaderProgram = ShaderUtils.loadProgram(SHADER_VERT_SRC, SHADER_FRAG_SRC, null, null, null, null); 
			locMat = glGetUniformLocation(shaderProgram, "matTrans");
		}

		private void view(int textureID, double aspectXY) {
			if (glIsProgram(shaderProgram)) {
				glPushAttrib(GL_DEPTH_BUFFER_BIT|GL_ENABLE_BIT);
				int sp[] = {'0'};
				glGetIntegerv(GL_CURRENT_PROGRAM, sp);
				glUseProgram(shaderProgram);
				glActiveTexture(GL_TEXTURE0);
				glEnable(GL_TEXTURE_2D);
				glEnable(GL_BLEND);
				glDisable(GL_DEPTH_TEST);
				glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
				glUniformMatrix4fv(locMat, false, ToFloatArray
						.convert(new Mat4Scale(1, 1/aspectXY, 1)));
				glBindTexture(GL_TEXTURE_2D, textureID);
				glUniform1i(glGetUniformLocation(shaderProgram, "drawTexture"), 0);
				buffers.draw(GL_TRIANGLE_STRIP, shaderProgram);
				glDisable(GL_TEXTURE_2D);
				glDisable(GL_BLEND);
				glUseProgram(sp[0]);
				glPopAttrib();

			}
		}
		
		@Override
		public void finalize() throws Throwable {
			super.finalize();
			//if (glIsProgram(shaderProgram))
			//	glDeleteProgram(shaderProgram);
		}
	}
	
	/**
	 * Create TextRenderer object
	 * 
	 * @param width
	 *            width of output rendering frame
	 * @param height
	 *            height of output rendering frame
	 * @param font
	 * 			  font
	 */
	public OGLTextRenderer(int width, int height, Font font) {
		this.font = font;
		resize(width, height);
		viewer = new Viewer();
	}

	/**
	 * Create TextRenderer object
	 * 
	 * @param width
	 *            width of output rendering frame
	 * @param height
	 *            height of output rendering frame
	 */
	public OGLTextRenderer(int width, int height) {
		this(width, height, new Font("SansSerif", Font.PLAIN, 12));
	}

	/**
	 * Update size of output rendering frame
	 * 
	 * @param width
	 *            updated width of output rendering frame
	 * @param height
	 *            updated height of output rendering frame
	 */
	public void resize(int width, int height) {
		this.width = width;
		this.height = height;
		if (glIsTexture(textureID))
			glDeleteTextures(textureID);
		textureID = glGenTextures();
		glBindTexture(GL_TEXTURE_2D, textureID);
		glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
	}

	/**
	 * Changes the current color. The default color is opaque white.
	 * 
	 * @param color
	 *            the new color to use for rendering text
	 */
	public void setColor(Color color) {
		this.color = color;
	}
	 
	/**
	 * Draw string on 2D coordinates of the raster frame
	 * 
	 * @param gl
	 * @param x
	 *            x position of string in range <0, width-1> of raster frame
	 * @param y
	 *            y position of string in range <0, height-1> of raster frame
	 * @param s
	 *            string to draw
	 */
	public void addStr2D(int x, int y, String s) {
		if (s != null){
			Graphics gr = img.getGraphics();
			gr.setColor(color);
			gr.setFont(font);
			gr.drawString(s, x, y);
			Rectangle2D textBox = gr.getFontMetrics().getStringBounds(s, 0,
					s.length(), gr);
			glBindTexture(GL_TEXTURE_2D, textureID);
			
			int x1 = Math.max(0, x + (int)textBox.getMinX());
			x1 = Math.min(width, x1);
			int x2 = Math.max(0, x+(int)textBox.getMaxX());
			x2 = Math.min(width, x2);
			int y1 = Math.max(0, y+ (int)textBox.getMinY());
			y1 = Math.min(height, y1 );
			int y2 = Math.max(0, y+(int)textBox.getMaxY());
			y2 = Math.min(height, y2);
			int w = x2-x1;
			int h = y2-y1;
			
			int[] array = new int[w * h];
			img.getRGB(x1, y1, w, h, array, 0, w);
			glTexSubImage2D(GL_TEXTURE_2D, 0, x1, y1, w, h, GL_BGRA, GL_UNSIGNED_INT_8_8_8_8_REV, array);
	
		}
	}

	public void clear() {
		//clear texture on GPU
		glClearTexImage (textureID, 0, GL_RGBA, GL_UNSIGNED_BYTE, clearTexBuffer);
		Graphics gr = img.getGraphics();
		gr.clearRect(0, 0, width, height);
	}

	public void draw(){
		glViewport(0, 0, width, height);
		glBindTexture(GL_TEXTURE_2D, textureID);
		viewer.view(textureID, (float)height/width);
	}
	
	@Override
	public void finalize() throws Throwable{
		super.finalize();
		viewer.finalize();
		//if (glIsTexture(textureID))
		//	glDeleteTextures(textureID);

	}
}
