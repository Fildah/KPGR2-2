package lwjglutils;

public interface OGLTexture {
	interface Viewer {
		default void view(OGLTexture texture) { view(texture.getTextureId()); }
		default void view(OGLTexture texture, double x, double y){ view(texture.getTextureId(), x, y); }
		default void view(OGLTexture texture, double x, double y, double scale){ view(texture.getTextureId(), x, y, scale); }
		default void view(OGLTexture texture, double x, double y, double scale, double aspectXY){ view(texture.getTextureId(), x, y, scale, aspectXY); }
		default void view(OGLTexture texture, double x, double y, double scale, double aspectXY, int level){ view(texture.getTextureId(), x, y, scale, aspectXY, level); }
		void view(int textureID);
		void view(int textureID, double x, double y);
		void view(int textureID, double x, double y, double scale);
		void view(int textureID, double x, double y, double scale, double aspectXY);
		void view(int textureID, double x, double y, double scale, double aspectXY, int level);
	}
	void bind(int shaderProgram, String name, int slot);
	void bind(int shaderProgram, String name);
	int getTextureId();
}
