#version 440
out vec4 gl_FragColor;
in vec4 gl_FragCoord;  /* Going from 0 to image size */
varying vec2 v_texCoords;

uniform float chunkSize;
layout(binding=1)uniform sampler2D inputs1; /* elements */
layout(binding=2)uniform sampler2D inputs2; /* forces */
layout(binding=3)uniform sampler2D inputs3; /* scalars */
layout(binding=4)uniform sampler2D inputs4; /* ethereal */

<COORDINATES_LIBRARY>
<MATERIAL_LIBRARY>
<WORLD_LIBRARY>
<ETH_LIBRARY>
<ELM_LIBRARY>

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  vec2 force = vec2(0,0);
  float currentElement = elm_getElement(currentPosition.xy, inputs1);
  float currentUnit = world_getUnit(currentPosition.xy, inputs3);
  float currentState = world_getState(currentElement, currentUnit);
  float normalizedRandomValue = elm_getPriority(currentPosition.xy, inputs1) / 1000000.0f;

  if(!world_isCellDiscardable(currentElement, currentUnit)){
    force = elm_getForce(currentPosition.xy, inputs2);
  }

  if(world_indexEther == currentElement){
    for(float nx = currentPosition.x - (2*unitCoordinate); nx <= currentPosition.x + (2*unitCoordinate); nx += unitCoordinate){
      for(float ny = currentPosition.y - (2*unitCoordinate); ny <= currentPosition.y + (2*unitCoordinate); ny += unitCoordinate){
        vec2 n = vec2(nx, ny);
        if(
          coords_insideEdges(n)
          &&(unitCoordinate < (currentPosition - n).length())
          &&(world_indexEther == elm_getElement(n, inputs1))
          &&(currentUnit < world_getUnit(n, inputs3))
        ){
          float aetherDiff = max(-10.5, min(10.5,
            eth_getAether(currentPosition.xy, inputs4) - eth_getAether(n, inputs4)
          ));
          force = force + vec2(
            ((n.x - currentPosition.x) * aetherDiff),
            ((n.y - currentPosition.y) * aetherDiff)
          );
        }
      }
    }
  }

  if(world_stateFluid == currentState){
    if(
      coords_insideInnerBounds(currentPosition.xy)
      && world_isSameMat(
        currentElement, currentUnit,
        elm_getElement(vec2(currentPosition.x,currentPosition.y-unitCoordinate), inputs1),
        world_getUnit(vec2(currentPosition.x,currentPosition.y-unitCoordinate), inputs3)
      )
    ){
      if((0 < force.x)&&(6 > force.x)){
        force.x *= 1.2;
      }else{
        force.x = (normalizedRandomValue * 6) - 3.0;
        force.y = 1.001; /* TODO: when 1.01; elements disappear.... */
      }
    }
  }else if(world_statePlasma == currentState){
    force.x += (normalizedRandomValue * 4) - 2;
  }
  elm_setForce(force);
  gl_FragColor.b = normalizedRandomValue;
}