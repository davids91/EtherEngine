#version 440
out vec4 gl_FragColor;
in vec4 gl_FragCoord;  /* Going from 0 to image size */
varying vec2 v_texCoords;

uniform float chunkSize;
layout(binding=1)uniform sampler2D inputs1; /* proposed changes */
layout(binding=2)uniform sampler2D inputs2; /* elements */
layout(binding=3)uniform sampler2D inputs3; /* forces */
layout(binding=4)uniform sampler2D inputs4; /* scalars */

<COORDINATES_LIBRARY>
<MATERIAL_LIBRARY>
<WORLD_LIBRARY>
<ETH_LIBRARY>
<ELM_LIBRARY>

void main(void){
  gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;

  float toApply = elm_getToApply(currentPosition.xy, inputs1);
  int offsetCodeOfC = coords_getOffsetCode(currentPosition.xy, inputs1);
  int targetX = coords_getIntXFromOffsetCode(int(gl_FragCoord.x), offsetCodeOfC);
  int targetY = coords_getIntYFromOffsetCode(int(gl_FragCoord.y), offsetCodeOfC);
  vec2 target = vec2( ((targetX + 0.5) / chunkSize), ((targetY + 0.5) / chunkSize) );
  vec2 force = elm_getForce(currentPosition.xy, inputs3);
  float weight = elm_getWeight(currentPosition.xy, inputs2, inputs4);
  vec2 positionOfT = vec2((targetX+0.5)/chunkSize, (targetY+0.5)/chunkSize);
  if( /* Update the forces on a cell.. */
    coords_insideInnerBounds(currentPosition.xy)
    &&(0 < coords_getToApply(currentPosition.xy, inputs1))&&(0 != coords_getOffsetCode(currentPosition.xy, inputs1))
    &&(coords_insideEdges(target))
  ){
    if( elm_ACanMoveB( currentPosition.xy, target, inputs2, inputs4 ) ){ /* The cells swap, decreasing forces on both *//* TODO: Also decrease the force based on the targets weight */
      force.x += -force.x * 0.7f * (abs(weight) / max(0.00001, max(abs(weight), abs(force.x))));
      force.y += -force.y * 0.7f * (abs(weight) / max(0.00001, max(abs(weight), abs(force.y))));
      force += world_gravity * weight;
    }else if( elm_ACanMoveB( target, currentPosition.xy, inputs2, inputs4 ) ){
      vec2 u1 = normalize(force);
      float m2 = elm_getWeight(positionOfT, inputs2, inputs4);
      vec2 u2 = normalize(elm_getForce(positionOfT, inputs3));
      vec2 result_speed = vec2( /*!Note: https://en.wikipedia.org/wiki/Elastic_collision#One-dimensional_Newtonian */
       ((weight - m2)/(weight+m2)*u1.x) + (2*m2/(weight+m2))*u2.x,
       ((weight - m2)/(weight+m2)*u1.y) + (2*m2/(weight+m2))*u2.y
      );
      /* F = m*a --> `a` is the delta v, which is the change in the velocity */
      force.x = (weight * (result_speed.x - u1.x));
      force.y = (weight * (result_speed.y - u1.y));
      force += world_gravity * weight;
    }
  }

  elm_setForce(force);
}