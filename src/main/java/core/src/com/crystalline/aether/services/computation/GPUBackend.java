package com.crystalline.aether.services.computation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.services.utils.BufferUtils;
import com.crystalline.aether.services.utils.StringUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;

public class GPUBackend extends CalculationBackend<String, FloatBuffer>{
    private final SpriteBatch batch; /* libgdx rendering */
    private final TextureRegion placeHolder;
    private final int chunkSize;
    private final ArrayList<ShaderProgram> shaders;
    private final ArrayList<Integer> textureHandles;
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
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
        outputTextureHandle = createTexture();
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, outputTextureHandle);
        Gdx.gl.glTexImage2D(
            GL20.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F,
            chunkSize, chunkSize, 0, GL20.GL_RGBA, GL20.GL_FLOAT, null
        );
        Gdx.gl.glFramebufferTexture2D( /* Attach the output texture to the FrameBuffer */
            GL20.GL_FRAMEBUFFER, GL20.GL_COLOR_ATTACHMENT0,
            GL20.GL_TEXTURE_2D, outputTextureHandle, 0
        );
        bindBackDefaultFBO();
        placeHolder  = new TextureRegion(new Texture(Gdx.files.internal("fire.png")), chunkSize, chunkSize);
        ShaderProgram.pedantic = false;
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
        newPhase.setUniformi("chunkSize", chunkSize);
        phases.add(phase);
        shaders.add(newPhase);
        return phaseIndex;
    }

    @Override
    public void runPhase(int index) {
        while(inputs.length > textureHandles.size()){ /* Generate the textures for the inputs if needed */
            textureHandles.add(createTexture());
        }
        batch.setShader(shaders.get(index));
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER, framebufferHandle);
        batch.begin();
        for(int t = 0; t < inputs.length; ++t){ /* Set the inputs */
            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE1 + t); /*!Note: Let's pray nobody will want more, than 30  input textures ?! */
            Gdx.gl.glBindTexture(GL20.GL_TEXTURE_2D, textureHandles.get(t));
            inputs[t].position(0);
            Gdx.gl.glTexImage2D( /* Upload the inputs into the textures */
                GL20.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F,
                chunkSize, chunkSize, 0, GL20.GL_RGBA, GL20.GL_FLOAT, inputs[t]
            );
            batch.getShader().setUniformi("inputs"+(1+t), (1+t));
        }
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        /* Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT); Don't know if this is necessary... */
        batch.draw(placeHolder, 0,0, chunkSize, chunkSize);
        System.out.println("(draw)error: " + String.format("0x%08X", Gdx.gl.glGetError()));
        System.out.println("(draw)fbo state: " + String.format("0x%08X", Gdx.gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)));
        batch.end();

        /* Load the texture into the buffer */
        outputs.get(index).position(0);
        Gdx.gl.glReadPixels(0,0,chunkSize, chunkSize, GL20.GL_RGBA, GL20.GL_FLOAT, outputs.get(index));
        /* Bind back the default FrameBuffer */
        System.out.println("(run)error: " + String.format("0x%08X", Gdx.gl.glGetError()));
        System.out.println("(run)fbo state: " + String.format("0x%08X", Gdx.gl.glCheckFramebufferStatus(GL20.GL_FRAMEBUFFER)));
        bindBackDefaultFBO();
    }
    private static void bindBackDefaultFBO(){
        Gdx.gl.glBindFramebuffer(GL20.GL_FRAMEBUFFER,0); /* on IOS default FBO is under glGetIntegerv(GL_FRAMEBUFFER_BINDING_OES, &defaultFBO); */
    }
}

