package com.crystalline.aether;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.services.World;

/**TODO:
 * - Display heat and cold
 * - Heat and cold to be counted in para-effect plane( ?)
 * - Create Ether crystals: it's not a target ratio, only when the ratio is already at that.
 * - "running" indicator
 * - debug panel to show pixel in focus
 * - Debug NANs
 * - push a to b
 * - typechange conflicts --> especially handle water to disappear when lava is near
 * - water to go sideways to the direction of less unit
 */

public class GameClass extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img_aether;
	Texture img_nether;

	OrthographicCamera camera;
	ShapeRenderer shapeRenderer;
	BitmapFont font;

	World world;
	float addition = 5.0f;

	final int[] world_block_number = {150,150};
	final float world_block_size = 100.0f;
	final float[] world_size = {world_block_number[0] * world_block_size, world_block_number[1] * world_block_size};

	private boolean debug_panel_shown = false;

	@Override
	public void create () {
		Gdx.gl.glClearColor(0, 0.1f, 0.1f, 1);
		camera = new OrthographicCamera();
		camera.setToOrtho(false,world_block_number[0] * world_block_size, world_block_number[1] * world_block_size);
		camera.update();

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		img_aether = new Texture("aether.png");
		img_nether = new Texture("nether.png");
		font = new BitmapFont();

		world = new World(world_block_number[0], world_block_number[1]);
		world.pond_with_grill();
	}

	private void drawblock(int x, int y, float scale_, Texture tex){
		float scale = Math.max( -1.0f, Math.min( 1.0f , scale_) );
		batch.draw(
			tex,
			x * world_block_size + (world_block_size/2.0f) - (world_block_size/2.0f) * Math.abs(scale),
			y * world_block_size + (world_block_size/2.0f) - (world_block_size/2.0f) * Math.abs(scale),
			(Math.abs(scale) * world_block_size),
			(Math.abs(scale) * world_block_size)
		);
		font.draw(
			batch,
			String.format( "U: %.2f", world.unit_at(x,y) ),
			x * world_block_size + (world_block_size/4.0f),
			y * world_block_size + (4.0f * world_block_size/5.0f)
		);
		font.draw(
			batch,
			String.format( "Ae: %.2f", world.get_eth_plane().aether_value_at(x,y) ),
			x * world_block_size + (world_block_size/4.0f),
			y * world_block_size + (3.0f * world_block_size/5.0f)
		);
		font.draw(
			batch,
			String.format( "Ne: %.2f", world.get_eth_plane().nether_value_at(x,y) ),
			x * world_block_size + (world_block_size/4.0f),
			y * world_block_size + (world_block_size/2.0f)
		);
		font.draw(
			batch,
			String.format( "R: %.2f", world.get_eth_plane().get_ratio(x,y) ),
			x * world_block_size + (world_block_size/4.0f),
			y * world_block_size + (world_block_size/5.0f)
		);
	}

	private void drawGrid(float lineWidth, float cellSize) {
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.LIME);
		for(float x = cellSize;x<world_size[0];x+=cellSize){
			shapeRenderer.rect(x,0,lineWidth,world_size[1]);
		}
		for(float y = cellSize;y<world_size[0];y+=cellSize){
			shapeRenderer.rect(0,y,world_size[0],lineWidth);
		}
		shapeRenderer.end();
	}

	private final Vector2 mouseInWorld2D = new Vector2();
	private final Vector3 mouseInCam3D = new Vector3();

	public void my_game_loop(){
		mouseInCam3D.x = Gdx.input.getX();
		mouseInCam3D.y = Gdx.input.getY();
		mouseInCam3D.z = 0;
		camera.unproject(mouseInCam3D);
		mouseInWorld2D.x = (
				(mouseInCam3D.x - (world_block_size/2.0f) + (world_block_size/4.0f))
						/ world_block_size);
		mouseInWorld2D.y = (
				(mouseInCam3D.y - (world_block_size/2.0f) + (world_block_size/4.0f))
						/ world_block_size);

		if(Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)){
			addition *= 1.1f;
		}else if(Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)){
			addition *= 0.9f;
		}
		if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
			float add_this = addition;
			if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
			world.add_nether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, add_this);
			if(Gdx.input.isKeyPressed(Input.Keys.C))
				world.add_aether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, (add_this/ Materials.nether_ratios[Materials.Names.Fire.ordinal()]));
		}
		if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)){
			float add_this = addition;
			if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
			world.add_aether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, add_this);
			if(Gdx.input.isKeyPressed(Input.Keys.C))
				world.add_nether_to((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, (add_this * Materials.nether_ratios[Materials.Names.Earth.ordinal()]));
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
			world.pond_with_grill();
		}

		/* Game logic */
		if(!Gdx.input.isKeyPressed(Input.Keys.ENTER)){
			world.main_loop(0.01f);
		}else
		if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
			world.main_loop(0.01f);
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
			float pressure = world.unit_at((int)mouseInWorld2D.x,world_block_number[1]-1);
			for(int i = world_block_number[1]-2; i >= mouseInWorld2D.y; --i ){
				pressure += world.unit_at((int)mouseInWorld2D.x, i);
			}
			System.out.println("Pressure at point at("+mouseInWorld2D.x+","+mouseInWorld2D.y+"):" + pressure);
		}
	}

	@Override
	public void render() {
		my_game_loop();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.begin();
		batch.setProjectionMatrix(camera.combined);
		TextureRegion lofaszbazdmeg = new TextureRegion(new Texture(world.getWorldImage(mouseInWorld2D,(addition/10.0f), world.get_eth_plane())));
		lofaszbazdmeg.flip(false,true);
		batch.draw(lofaszbazdmeg,0,0,world_size[0],world_size[1]);
//		for(int x = 0; x < world_block_number[0]; ++x){
//			for(int y = 0; y < world_block_number[1]; ++y){
//				drawblock(x,y, (world.get_eth_plane().aether_value_at(x,y)/Math.max(world.get_eth_plane().aether_value_at(x,y),world.get_eth_plane().nether_value_at(x,y))), img_aether);
//				drawblock(x,y, (world.get_eth_plane().nether_value_at(x,y)/Math.max(world.get_eth_plane().aether_value_at(x,y),world.get_eth_plane().nether_value_at(x,y))), img_nether);
//			}
//		}
		batch.end();
		shapeRenderer.setProjectionMatrix(camera.combined);
		//drawGrid(1.0f, world_block_size);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
	}
}
