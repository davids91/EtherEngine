package com.crystalline.aether;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.services.World;

/** TODO:
 * - Display heat and cold
 * - Heat and cold to be counted in para-effect plane( ? )
 * - Create Ether crystals: it's not a target ratio, only when the ratio is already at that.
 * - "running" indicator
 * - debug panel to show pixel in focus
 * - Debug NANs
 * - push a to b  ( material merges )
 * - typechange conflicts --> especially handle water to disappear when lava is near
 * - "Move together" ?
 */

public class GameClass extends ApplicationAdapter {
	SpriteBatch batch;
	Texture img_aether;
	Texture img_nether;

	OrthographicCamera camera;
	ShapeRenderer shapeRenderer;
	MeshBuilder meshbuilder;
	Mesh debug_arrows;
	BitmapFont font;

	Config conf = new Config();
	World world;
	float addition = 5.0f;
	boolean play = true;

	private enum Debug_state {
		OFF,TEXT,NORMAL_ARROWS,ARROWS;
		private static Debug_state[] vals = values();
		public Debug_state next(){ return vals[(this.ordinal() + 1) % vals.length]; }
		public Debug_state previous(){
			if(0 == this.ordinal())
				return vals[vals.length-1];
			else return vals[this.ordinal() -1];
		}
	}
	private Debug_state debug_state = Debug_state.OFF;

	@Override
	public void create () {
		Gdx.gl.glClearColor(0.9f, 0.5f, 0.8f, 1);
		camera = new OrthographicCamera();
		camera.setToOrtho(false,conf.world_size[0], conf.world_size[1]);
		camera.update();

		batch = new SpriteBatch();
		shapeRenderer = new ShapeRenderer();
		meshbuilder = new MeshBuilder();

		img_aether = new Texture("aether.png");
		img_nether = new Texture("nether.png");
		font = new BitmapFont();

		world = new World(conf);
//		world.pond_with_grill();
	}

	@Override
	public void render() {
		my_game_loop();

		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.setProjectionMatrix(camera.combined);
		batch.begin();
		TextureRegion lofaszbazdmeg = new TextureRegion(new Texture(world.getWorldImage(mouseInWorld2D,(addition/10.0f), world.get_eth_plane())));
		lofaszbazdmeg.flip(false,true);
		batch.draw(lofaszbazdmeg,0,0,conf.world_size[0],conf.world_size[1]);
		if(Debug_state.TEXT == debug_state)
		for(int x = (int)Math.max(0.0f,(mouseInWorld2D.x - 20)); x < Math.min((mouseInWorld2D.x + 20),conf.world_block_number[0]); ++x){
			for(int y = (int)Math.max(0.0f,(mouseInWorld2D.y - 20)); y < Math.min((mouseInWorld2D.y + 20),conf.world_block_number[1]); ++y){
				drawblock(x,y, (world.get_eth_plane().aether_value_at(x,y)/Math.max(world.get_eth_plane().aether_value_at(x,y),world.get_eth_plane().nether_value_at(x,y))), img_aether);
				drawblock(x,y, (world.get_eth_plane().nether_value_at(x,y)/Math.max(world.get_eth_plane().aether_value_at(x,y),world.get_eth_plane().nether_value_at(x,y))), img_nether);
//		        draw_velo(x,y);
			}
		}
		batch.end();
//		drawGrid(1.0f, world_block_size);

		/* draw velocity arrrays */
		if((Debug_state.NORMAL_ARROWS == debug_state) || (Debug_state.ARROWS == debug_state)){
			meshbuilder.clear();
			meshbuilder.begin(VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, GL20.GL_TRIANGLES);
			for(int x = (int)Math.max(0.0f,(mouseInWorld2D.x - 10)); x < Math.min((mouseInWorld2D.x + 10),conf.world_block_number[0]); ++x){
				for(int y = (int)Math.max(0.0f,(mouseInWorld2D.y - 10)); y < Math.min((mouseInWorld2D.y + 10),conf.world_block_number[1]); ++y){
					if(
						(
							(Materials.Names.Nothing == debug_focus)
							||(world.get_elm_plane().element_at(x,y) == debug_focus)
						)&&(0 < world.get_elm_plane().get_force(x,y).len())
					){
						if (Debug_state.NORMAL_ARROWS == debug_state) {
							ArrowShapeBuilder.build(
							meshbuilder,
							x * conf.world_block_size + conf.block_radius, y * conf.world_block_size + conf.block_radius, 0,
							x * conf.world_block_size + conf.block_radius + conf.world_block_size * world.get_elm_plane().get_force(x,y).cpy().nor().scl(conf.block_radius).x,
							y * conf.world_block_size + conf.block_radius + conf.world_block_size * world.get_elm_plane().get_force(x,y).cpy().nor().scl(conf.block_radius).y, 0,
							0.1f,0.5f,4
							);
						}else{
							ArrowShapeBuilder.build(
							meshbuilder,
							x * conf.world_block_size + conf.block_radius,
							y * conf.world_block_size + conf.block_radius, 0,
							x * conf.world_block_size + conf.block_radius
									+ conf.world_block_size * Math.max(-conf.world_size[0], Math.min(conf.world_size[0],world.get_elm_plane().get_force(x,y).x)),
							y * conf.world_block_size + conf.block_radius
									+ conf.world_block_size * Math.max(-conf.world_size[1], Math.min(conf.world_size[1],world.get_elm_plane().get_force(x,y).y)), 0,
							0.1f,0.5f,4
							);
						}
					}
				}
			}
			debug_arrows = meshbuilder.end();
			batch.begin();
			debug_arrows.render(batch.getShader(), GL20.GL_TRIANGLES);
			batch.end();
		}
	}

	private void drawblock(int x, int y, float scale_, Texture tex){
		float scale = Math.max( -1.0f, Math.min( 1.0f , scale_) );
		batch.draw(
			tex,
			x * conf.world_block_size + (conf.block_radius) - (conf.block_radius) * Math.abs(scale),
			y * conf.world_block_size + (conf.block_radius) - (conf.block_radius) * Math.abs(scale),
			(Math.abs(scale) * conf.world_block_size),
			(Math.abs(scale) * conf.world_block_size)
		);
		font.draw(
			batch,
			String.format( "U: %.2f", world.unit_at(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (4.0f * conf.world_block_size/5.0f)
		);
		font.draw(
			batch,
			String.format( "Ae: %.2f", world.get_eth_plane().aether_value_at(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (3.0f * conf.world_block_size/5.0f)
		);
		font.draw(
			batch,
			String.format( "Ne: %.2f", world.get_eth_plane().nether_value_at(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (conf.block_radius)
		);
		font.draw(
			batch,
			String.format( "R: %.2f", world.get_eth_plane().get_ratio(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (conf.world_block_size/5.0f)
		);
	}

	private void draw_velo(int x, int y){
		font.draw(
			batch,
			String.format( "U: %.2f", world.unit_at(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (4.0f * conf.world_block_size/5.0f)
		);
		font.draw(
			batch,
				String.format( "%.2f, %.2f",
					world.get_elm_plane().get_force(x,y).x,
					world.get_elm_plane().get_force(x,y).y
				),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (conf.block_radius)
		);
		font.draw(
			batch,
			String.format( "m: %.2f", world.get_weight(x,y) ),
			x * conf.world_block_size + (conf.world_block_size/4.0f),
			y * conf.world_block_size + (conf.world_block_size/5.0f)
		);
	}

	private void drawGrid(float lineWidth, float cellSize) {
		shapeRenderer.setProjectionMatrix(camera.combined);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		shapeRenderer.setColor(Color.LIME);
		for(float x = cellSize;x<conf.world_size[0];x+=cellSize){
			shapeRenderer.rect(x,0,lineWidth,conf.world_size[1]);
		}
		for(float y = cellSize;y<conf.world_size[0];y+=cellSize){
			shapeRenderer.rect(0,y,conf.world_size[0],lineWidth);
		}
		shapeRenderer.end();
	}

	private final Vector2 mouseInWorld2D = new Vector2();
	private final Vector3 mouseInCam3D = new Vector3();

	public void my_game_loop() {
		mouseInCam3D.x = Gdx.input.getX();
		mouseInCam3D.y = Gdx.input.getY();
		mouseInCam3D.z = 0;
		camera.unproject(mouseInCam3D);
		mouseInWorld2D.x = (
				(mouseInCam3D.x - (conf.block_radius) + (conf.world_block_size / 4.0f))
						/ conf.world_block_size);
		mouseInWorld2D.y = (
				(mouseInCam3D.y - (conf.block_radius) + (conf.world_block_size / 4.0f))
						/ conf.world_block_size);

		if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
			debug_state = debug_state.previous();
		}
		if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
			debug_state = debug_state.next();
		}

		if (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
			addition *= 1.1f;
		} else if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
			addition *= 0.9f;
		}

		if(Gdx.input.isKeyPressed(Input.Keys.ALT_LEFT)){
			if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
				float add_this = addition;
				if(!Gdx.input.isKeyPressed(Input.Keys.SPACE))add_this *= -1.0f;
				world.try_to_equalize((int)mouseInWorld2D.x,(int)mouseInWorld2D.y, add_this);
			}
		}else{
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
		}

		if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
			world.pond_with_grill();
		}

		/* Game logic */
		if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){ play = !play; }
		if(play){
			world.main_loop(0.01f);
		}else
		if(Gdx.input.isKeyJustPressed(Input.Keys.TAB)){
			world.main_loop(0.01f);
		}
		if(Gdx.input.isKeyJustPressed(Input.Keys.SPACE)){
			Vector2 tmp_force = world.get_elm_plane().get_force((int)mouseInWorld2D.x,(int)mouseInWorld2D.y);
			System.out.println("forces at ("+(int)mouseInWorld2D.x+","+(int)mouseInWorld2D.y+"):"
					+ "(" + tmp_force.x + "," + tmp_force.y + ") + "
					+ "("
					+ (-tmp_force.x * (world.get_weight((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) / Math.max(0.001f, tmp_force.x))) + ","
					+ (-tmp_force.y * (world.get_weight((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) / Math.max(0.001f, tmp_force.y)))
					+ ")"
					+ "("
					+ -tmp_force.x + " * " + (world.get_weight((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) + "/" + Math.max(0.001f, tmp_force.x)) + ","
					+ -tmp_force.y + " * " + (world.get_weight((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) + "/" + Math.max(0.001f, tmp_force.y))
					+ ")"
			);

			if(Materials.Names.Nothing == debug_focus){
				debug_focus = world.get_elm_plane().element_at((int)mouseInWorld2D.x,(int)mouseInWorld2D.y);
			}else{
				debug_focus = Materials.Names.Nothing;
			}

//			if(conf.world_block_number[0] == first_point.get_i_x()){
//				System.out.print("force at ("+(int)mouseInWorld2D.x+","+(int)mouseInWorld2D.y+"):" + world.get_elm_plane().get_force((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) + "[");
//				first_point.set(mouseInWorld2D);
//			}else{
//				System.out.print("nx,x==>" + (int)mouseInWorld2D.x + "," + first_point.get_i_x() + ";");
//				System.out.print("w diff: " + -(world.get_weight((int)mouseInWorld2D.x,(int)mouseInWorld2D.y) - world.get_weight(first_point)) + ";");
//				System.out.println("nx-x diff: " + ((int)mouseInWorld2D.x - first_point.get_i_x()) + "]");
////				float tmp = 0;
////				for (int nx = (first_point.get_i_x() - 1); nx < (first_point.get_i_x() + 2); ++nx) {
////					for (int ny = (first_point.get_i_y() - 1); ny < (first_point.get_i_y() + 2); ++ny) {
////						float weight_difference = Math.max(-2.5f, Math.min(2.5f,(world.get_weight(first_point) - world.get_weight(nx,ny))));
////						System.out.print("("+nx+","+ny+")"+(nx-first_point.get_i_x()) * weight_difference+";");
////						tmp += (nx-first_point.get_i_x()) * weight_difference;
////					}
////				}
////				System.out.println("Final force delta x : " + tmp);
//				first_point.x = conf.world_block_number[0];
//			}
		}
	}

//	Util.MyCell first_point = new Util.MyCell(1,1,conf.world_block_number[0]);
	Materials.Names debug_focus = Materials.Names.Nothing;

	@Override
	public void dispose () {
		batch.dispose();
	}
}
