package com.crystalline.aether.services.computation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.crystalline.aether.services.utils.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class GPUBackend extends CalculationBackend<String, FloatBuffer>{
    private final SpriteBatch batch; /* libgdx rendering */
    private final TextureRegion placeHolder;
    private final int chunkSize;
    private ArrayList<ShaderProgram> shaders;
    private ArrayList<Integer> textureHandles;
    private final int framebufferHandle; /* TODO: Try out Renderbuffers */
    private final int outputTextureHandle;

    private static final String defaultVertexSource = StringUtils.readFileAsString(
        Gdx.files.internal("shaders/default.vshader")
    );

    public GPUBackend(int chunkSize_){
        super();
        batch = new SpriteBatch();
        chunkSize = chunkSize_;
        shaders = new ArrayList<>();
        textureHandles = new ArrayList<>();
        framebufferHandle = Gdx.gl.glGenFramebuffer();
        outputTextureHandle = createTexture();
        placeHolder  = new TextureRegion(new Texture(Gdx.files.internal("ether.png")), chunkSize, chunkSize);
    }

    private int createTexture(){
        int newHandle = Gdx.gl.glGenTexture();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, newHandle);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST);
        Gdx.gl.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST);
        return newHandle;
    }

    @Override
    public int addPhase(String phase, int outputSize) throws Exception {
        int phaseIndex = outputs.size();
        outputs.add(ByteBuffer.allocateDirect(Float.BYTES * outputSize).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer());
        ShaderProgram newPhase = new ShaderProgram(defaultVertexSource, phase);
        if(!newPhase.isCompiled()){
            throw new Exception("Unable to compile Phase GPU Kernel! \n Log: " + newPhase.getLog());
        }
        phases.add(phase);
        shaders.add(newPhase);
        return phaseIndex;
    }

    @Override
    public void runPhase(int index) {
        /* Generate the textures for the inputs if needed */
        while(inputs.length > textureHandles.size()){
            textureHandles.add(createTexture());
        }
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
        batch.setShader(shaders.get(index));
        batch.begin();
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        for(int t = 0; t < inputs.length; ++t){
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1 + t);
            /*!Note: Let's pray nobody will want more, than 30  input textures ?! */
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, textureHandles.get(t));
            inputs[t].position(0);
            Gdx.gl.glTexImage2D( /* Upload the inputs into the textures */
                GL20.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F,
                chunkSize, chunkSize, 0, GL20.GL_RGBA, GL20.GL_FLOAT, inputs[t]
            );
            batch.getShader().setUniformi("inputs"+(1+t), (1+t));
        }
        /* Attach the output texture to the Framebuffer */
        Gdx.gl.glFramebufferTexture2D(
            GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0,
            GL20.GL_TEXTURE_2D, outputTextureHandle, 0
        );

        /* Render the output to the texture */
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0); /* bind texture 0 again, so it would be bound for the placeholder */
        batch.draw(placeHolder, 0,0, chunkSize, chunkSize);
        batch.end();
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER,0); /* on IOS default FBO is under glGetIntegerv(GL_FRAMEBUFFER_BINDING_OES, &defaultFBO); */

        /* Load the texture into the buffer */
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, outputTextureHandle);
        outputs.get(index).position(0);
        Gdx.gl30.glReadPixels(0,0,chunkSize, chunkSize, GL20.GL_RGBA, GL20.GL_FLOAT, outputs.get(index));
    }

}

