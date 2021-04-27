import lwjglutils.OGLTexture2D;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWScrollCallback;
import transforms.Point3D;
import transforms.Vec3D;
import utils.AbstractRenderer;
import utils.GLCamera;
import utils.GluUtils;

import java.io.IOException;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;


public class Renderer extends AbstractRenderer {
    private float dx, dy, ox, oy;
    private float zenit, azimut;

    private float trans, deltaTrans = 0;

    private float translateX = 0, translateY = 0, translateZ = 0;
    private float rotateX = 0, rotateY = 0;

    private boolean mouseButton1 = false;
    private boolean per = true, wire = false, textures = false;

    private GLCamera camera;

    private int vaoId, vboId, iboId;

    private OGLTexture2D texture;

    private final ArrayList<Integer> indices = new ArrayList<>();
    private final ArrayList<Float> vertices = new ArrayList<>();
    private int numPoints = 0;

    private int length = 2;
    private int depth = 1;
    private Point3D origPoint;

    public Renderer() {
        super();

        // Key call backs
        glfwKeyCallback = new GLFWKeyCallback() {
            @Override
            public void invoke(long window, int key, int scancode, int action, int mods) {
                if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE)
                    glfwSetWindowShouldClose(window, true);
                if (action == GLFW_RELEASE) {
                    trans = 0;
                    deltaTrans = 0;
                }

                // 1 akce po stisku
                if (action == GLFW_PRESS) {
                    switch (key) {
                        case GLFW_KEY_C:
                            per = !per;
                            break;
                        case GLFW_KEY_X:
                            wire = !wire;
                            break;
                        case GLFW_KEY_M:
                            depth++;
                            reCreate();
                            break;
                        case GLFW_KEY_N:
                            if (depth > 0){
                                depth--;
                                reCreate();
                            }
                            break;
                        case GLFW_KEY_V:
                            if (length > 1){
                                length--;
                                calcOriginPoint();
                                reCreate();
                            }
                            break;
                        case GLFW_KEY_B:
                            length++;
                            calcOriginPoint();
                            reCreate();
                            break;
                        case GLFW_KEY_Z:
                            textures = !textures;
                            break;
                        case GLFW_KEY_W:
                        case GLFW_KEY_S:
                        case GLFW_KEY_A:
                        case GLFW_KEY_D:
                            deltaTrans = 0.001f;
                            break;
                    }
                }

                // opakujici se akce po stisku
                switch (key) {
                    case GLFW_KEY_W:
                        camera.forward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_S:
                        camera.backward(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_A:
                        camera.left(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_D:
                        camera.right(trans);
                        if (deltaTrans < 0.001f)
                            deltaTrans = 0.001f;
                        else
                            deltaTrans *= 1.02;
                        break;

                    case GLFW_KEY_T:
                        translateZ -= 0.01;
                        break;
                    case GLFW_KEY_F:
                        translateX -= 0.01;
                        break;
                    case GLFW_KEY_G:
                        translateZ += 0.01;
                        break;
                    case GLFW_KEY_H:
                        translateX += 0.01;
                        break;
                    case GLFW_KEY_R:
                        translateY += 0.01;
                        break;
                    case GLFW_KEY_Y:
                        translateY -= 0.01;
                        break;
                    case GLFW_KEY_I:
                        rotateY++;
                        break;
                    case GLFW_KEY_J:
                        rotateX++;
                        break;
                    case GLFW_KEY_K:
                        rotateY--;
                        break;
                    case GLFW_KEY_L:
                        rotateX--;
                        break;
                }
            }
        };

        glfwMouseButtonCallback = new GLFWMouseButtonCallback() {


            @Override
            public void invoke(long window, int button, int action, int mods) {
                DoubleBuffer xBuffer = BufferUtils.createDoubleBuffer(1);
                DoubleBuffer yBuffer = BufferUtils.createDoubleBuffer(1);
                glfwGetCursorPos(window, xBuffer, yBuffer);
                double x = xBuffer.get(0);
                double y = yBuffer.get(0);

                mouseButton1 = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_1) == GLFW_PRESS;

                if (button == GLFW_MOUSE_BUTTON_1 && action == GLFW_PRESS) {
                    ox = (float) x;
                    oy = (float) y;
                }
            }

        };

        // Ovladani kamery po stisky leveho tlacitka
        glfwCursorPosCallback = new GLFWCursorPosCallback() {
            @Override
            public void invoke(long window, double x, double y) {
                if (mouseButton1) {
                    dx = (float) x - ox;
                    dy = (float) y - oy;
                    ox = (float) x;
                    oy = (float) y;
                    zenit -= dy / width * 180;
                    if (zenit > 90)
                        zenit = 90;
                    if (zenit <= -90)
                        zenit = -90;
                    azimut += dx / height * 180;
                    azimut = azimut % 360;
                    camera.setAzimuth(Math.toRadians(azimut));
                    camera.setZenith(Math.toRadians(zenit));
                    dx = 0;
                    dy = 0;
                }
            }
        };

        glfwScrollCallback = new GLFWScrollCallback() {
            @Override
            public void invoke(long window, double dx, double dy) {
            }
        };
    }

    @Override
    public void init() {
        super.init();

        // Zakladni barva platna CERNA
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // Zapnuti Z testu
        glEnable(GL_DEPTH_TEST);
        // Zpusob vykresleni predni a zadni strany
        glPolygonMode(GL_FRONT, GL_FILL);
        glPolygonMode(GL_BACK, GL_FILL);

        // Antialiasing
        glEnable(GL_LINE_SMOOTH);
        glHint(GL_LINE_SMOOTH_HINT, GL_FASTEST);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        // Nacteni textury
        try {
            texture = new OGLTexture2D("textures/bricks.jpg"); // vzhledem k adresari res v projektu
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Inicializace kamery
        camera = new GLCamera();
        camera.setPosition(new Vec3D(0));
        camera.setFirstPerson(false);
        camera.setRadius(5);

        // Inicializace Siepinski pyramidu
        calcOriginPoint();
        calcSierpinskiPyramid(origPoint, length, depth);

        // Vytvoreni vertex a index bufferu
        createBuffers();
    }

    // Vertex a index buffer
    private void createBuffers() {
        // Konverze pomocnych listu do spravnych poli
        int[] indicesBuff = indices.stream().mapToInt(i -> i).toArray();
        float[] buffer = new float[vertices.size()];
        for(int i=0; i<vertices.size(); i++){
            buffer[i] = vertices.get(i);
        }

        // Transforamce poli na buffery
        FloatBuffer vertexBufferDataBuffer = (FloatBuffer) BufferUtils
                .createFloatBuffer(buffer.length)
                .put(buffer)
                .rewind();

        IntBuffer indexBufferDataBuffer = (IntBuffer) BufferUtils
                .createIntBuffer(indicesBuff.length)
                .put(indicesBuff)
                .rewind();

        // Nastaveni poli pro definice geometrie
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBufferDataBuffer, GL_STATIC_DRAW);

        // Nastaveni prelozeni hodnot
        if(textures){
            glVertexPointer(3, GL_FLOAT, 5 * 4, 0);
            glTexCoordPointer(2, GL_FLOAT,5 * 4, 3 * 4);
        } else {
            glVertexPointer(3, GL_FLOAT, 6 * 4, 0);
            glColorPointer(3, GL_FLOAT, 6 * 4, 3 * 4);
        }

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        iboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBufferDataBuffer, GL_STATIC_DRAW);
    }

    @Override
    public void display() {
        // Natazeni projekce do okna
        glViewport(0, 0, width, height);
        // Vymazani obrazovky a Z bufferu
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);

        // Inicializace textur
        int paramTex = GL_REPEAT;
        int paramTexApp = GL_REPLACE;
        if(textures) {
            glEnable(GL_TEXTURE_2D);
            glMatrixMode(GL_TEXTURE);
            glLoadIdentity();
        }

        trans += deltaTrans;

        // Projekce
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        if (per)
            GluUtils.gluPerspective(45, width / (float) height, 0.1f, 500.0f);
        else
            glOrtho(-20 * width / (float) height,
                    20 * width / (float) height,
                    -20, 20, 0.1f, 20.0f);

        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();

        camera.setMatrix();

        // Rotace a posunuti
        glRotatef(rotateX, 0, 1, 0);
        glRotatef(rotateY, 1, 0, 0);
        glTranslatef(translateX, translateY, translateZ);

        // Vyplneny nebo drateny model
        if (!wire)
            glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        else
            glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Pripraveni bufferu
        glBindVertexArray(vaoId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, iboId);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_COLOR_ARRAY);
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glEnableClientState(GL_INDEX_ARRAY);

        // Bindovani textury
        if(textures) {
            texture.bind();
            glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, paramTexApp);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, paramTex);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, paramTex);
        }

        // Vykresleni trojuhelniku z bufferu
        glDrawElements(GL_TRIANGLES, indices.size(), GL_UNSIGNED_INT, 0);

        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_INDEX_ARRAY);
        glBindVertexArray(0);

        glDisable(GL_VERTEX_ARRAY);
        glDisable(GL_COLOR_ARRAY);
        glDisable(GL_TEXTURE_COORD_ARRAY);
        glDisableClientState(GL_COLOR_ARRAY);
        glDisableClientState(GL_VERTEX_ARRAY);

        glDisable(GL_TEXTURE_2D);

        // Vykresleni textu
        textRenderer.clear();
        textRenderer.addStr2D(3, 20, "Sierpiński Pyramid - Uzivani: [lmb] rozhled kamerou, [wsad] pohyb kamerou, [tfghry] posun pyramidu, [ijkl] rotace pyramidu");
        textRenderer.addStr2D(3, 40, "[vb] zmena velikosti pyramidu: " + length + " [nm] hloubka rekurze: " + depth + " [x] dratovy/vyplneny model [c] zmena projekce [z] de/activuj textury");
        textRenderer.addStr2D(width - 230, height - 3, " Pouzity knihovny a priklady (c) PGRF UHK");
        textRenderer.draw();
    }

    // Spocita bod pocatku tak aby stred pyramidu byl v 0, 0, 0
    private void calcOriginPoint() {
        float x = length / 2f;

        float h = (float) (Math.sqrt(3) / 2 * length);
        float r = (1f / 3f * h);

        float bigR = (float) (Math.sqrt(6) / 4 * length);
        float y = (float) (Math.sqrt((bigR * bigR) - (r * r)) / 2);

        origPoint = new Point3D(-x, -y, r);
    }

    // Vycisti pomocne indexy
    private void clearTempIndex() {
        indices.clear();
        vertices.clear();
        numPoints = 0;
    }

    // Prepocita cely pyramid a znovu vytvori buffery
    private void reCreate() {
        clearTempIndex();
        calcSierpinskiPyramid(origPoint, length, depth);
        createBuffers();
    }

    // Vypocita Sierpinsky pyramid podle zadaneho pocatecniho bodu, delky a hloubky rekurze
    private void calcSierpinskiPyramid(Point3D firstPoint, float length, float depth) {
        if (depth > 0){
            float half = (length / 2f);
            float quater = (half / 2f);
            float h = (float) (Math.sqrt(3) / 2 * half);
            float r = (1f / 3f * h);
            float h2 = (float) ((half / 3) * Math.sqrt(6));

            //Rekurze pro pruchod 4 nove vytvorenych pyramidu
            calcSierpinskiPyramid(firstPoint, half, (depth - 1));
            calcSierpinskiPyramid(new Point3D((firstPoint.getX() + half), firstPoint.getY(), firstPoint.getZ()), half, (depth - 1));
            calcSierpinskiPyramid(new Point3D((firstPoint.getX() + quater), firstPoint.getY(), firstPoint.getZ() - h), half, (depth - 1));
            calcSierpinskiPyramid(new Point3D((firstPoint.getX() + quater), firstPoint.getY() + h2, firstPoint.getZ() - r), half, (depth - 1));
        } else {
            int startVert = numPoints;
            float h = (float) (Math.sqrt(3) / 2 * length);
            float r = (1f / 3f * h);
            float h2 = (float) ((length / 3) * Math.sqrt(6));

            //Prida do pomocneho bufferu vertex hodnoty x,y,z,r,g,b nebo x,y,z,xTexture, yTexture
            vertices.add((float) firstPoint.getX());
            vertices.add((float) firstPoint.getY());
            vertices.add((float) firstPoint.getZ());
            if(textures) {
                vertices.add(0f);
                vertices.add(0f);
            } else {
                vertices.add(1f);
                vertices.add(1f);
                vertices.add(1f);
            }
            vertices.add((float) firstPoint.getX() + length);
            vertices.add((float) firstPoint.getY());
            vertices.add((float) firstPoint.getZ());
            if(textures) {
                vertices.add(0f);
                vertices.add(0f);
            } else {
                vertices.add(0f);
                vertices.add(0f);
                vertices.add(1f);
            }
            vertices.add((float) firstPoint.getX() + (length / 2f));
            vertices.add((float) firstPoint.getY());
            vertices.add((float) firstPoint.getZ() - h);
            if(textures) {
                vertices.add(0f);
                vertices.add(0f);
            } else {
                vertices.add(0f);
                vertices.add(1f);
                vertices.add(1f);
            }
            vertices.add((float) firstPoint.getX() + (length / 2f));
            vertices.add((float) firstPoint.getY() + h2);
            vertices.add((float) firstPoint.getZ() - r);
            if(textures) {
                vertices.add(0f);
                vertices.add(0f);
            } else {
                vertices.add(1f);
                vertices.add(1f);
                vertices.add(0f);
            }

            // Prida do pomocneho bufferu spojove hodnoty
            indices.add(startVert);
            indices.add(startVert + 1);
            indices.add(startVert + 2);
            indices.add(startVert);
            indices.add(startVert + 1);
            indices.add(startVert + 3);
            indices.add(startVert + 1);
            indices.add(startVert + 2);
            indices.add(startVert + 3);
            indices.add(startVert);
            indices.add(startVert + 2);
            indices.add(startVert + 3);

            // Pripocita k pomocne hodnote pocet novych bodu
            numPoints += 4;
        }
    }

}
