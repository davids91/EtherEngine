package com.crystalline.aether.services;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;
import com.crystalline.aether.models.Config;
import com.crystalline.aether.models.InputService;
import com.crystalline.aether.models.Materials;
import com.crystalline.aether.models.Spells;


/**TODO:
 * - Set addition as a togglable direction: - -> o -> + (forwards / backwards in mouse )
 * - Make spell amount based on scrollwheel
 * - target different elements with the spell
 */
public class UserInputCapsule extends InputService {
    private final Config conf;
    private final WorldCapsule worldCapsule;
    private final WorldDisplay displayCapsule;
    private final Vector3 mouseInCam3D = new Vector3();

    private Spells.SpellEtherTendency userSpellTendency = Spells.SpellEtherTendency.Take;
    private final float max_spell_amount;
    private float spell_amount = 5.0f;

    public UserInputCapsule(Scene parent, WorldCapsule worldCapsule_, WorldDisplay displayCapsule_, float max_spell_amount_, Config conf_){
        super(parent);
        conf = conf_;
        worldCapsule = worldCapsule_;
        displayCapsule = displayCapsule_;
        max_spell_amount = max_spell_amount_;
    }

    @Override
    public void handle_input() {
        mouseInCam3D.x = Gdx.input.getX();
        mouseInCam3D.y = Gdx.graphics.getHeight() - Gdx.input.getY();
        mouseInCam3D.z = 0;
        ((OrthographicCamera)displayCapsule.get_object("camera")).unproject(mouseInCam3D);
        worldCapsule.accept_input("mouseInWorld2D", mouseInCam3D.x,mouseInCam3D.y);

        if(
            Gdx.input.isButtonPressed(Input.Buttons.LEFT)
            || Gdx.input.isButtonPressed(Input.Buttons.RIGHT)
        ){
            float add_this = spell_amount;
            if(userSpellTendency == Spells.SpellEtherTendency.Equalize) {
                signal("equalize", add_this);
            }else{
                if(userSpellTendency == Spells.SpellEtherTendency.Take){
                    add_this *= -1.0f;
                }else if(userSpellTendency == Spells.SpellEtherTendency.Give) { }

                if(Gdx.input.isButtonPressed(Input.Buttons.LEFT)){
                    signal("add_nether", add_this);
                    if(Gdx.input.isKeyPressed(Input.Keys.C))
                        signal("add_aether", (add_this / Materials.nether_ratios[Materials.Names.Fire.ordinal()]));
                }
                if(Gdx.input.isButtonPressed(Input.Buttons.RIGHT)){
                    signal("add_aether", add_this);
                    if(Gdx.input.isKeyPressed(Input.Keys.C))
                        signal("add_nether", (add_this * Materials.nether_ratios[Materials.Names.Fire.ordinal()]));
                }
            }
        }
    }

    @Override
    public void dispose() {

    }

    @Override
    public boolean keyDown(int i) {
        if(Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)){
            signal("initialize");
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ENTER)){
            signal("play_pause");
        }
        return false;
    }

    @Override
    public boolean keyUp(int i) {
        return false;
    }

    @Override
    public boolean keyTyped(char c) {
        return false;
    }

    @Override
    public boolean touchDown(int x, int y, int pointer, int button) {
        if(Input.Buttons.BACK == button){
            if(0 < userSpellTendency.ordinal())
                userSpellTendency = userSpellTendency.previous();
            signal(userSpellTendency.name());
        }else if(Input.Buttons.FORWARD == button){
            if(Spells.SpellEtherTendency.values().length-1 > userSpellTendency.ordinal())
                userSpellTendency = userSpellTendency.next();
            signal(userSpellTendency.name());
        }
        return false;
    }

    @Override
    public boolean touchUp(int x, int y, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        float modif = Math.max(0.1f,Math.min(0.3f, (spell_amount / max_spell_amount)));
        if(0 < -amountY) {
            spell_amount = Math.min(Math.max(0.1f,spell_amount * (1+modif)),max_spell_amount);
        } else {
            spell_amount = Math.min(Math.max(0.1f,spell_amount * (1-modif)),max_spell_amount);
        }
        signal("spell_amount", spell_amount);
        return false;
    }
}
