/* =================== ETH_LIBRARY =================== */
float eth_getAether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).b;
}

float eth_getNether(vec2 position, sampler2D etherValues){
  return texture(etherValues, position).r;
}

void eth_SetAether(float value){
  gl_FragColor.b = value;
}

void eth_SetNether(float value){
  gl_FragColor.r = value;
}

const float aetherWeightInUnits = 4;
float eth_getUnit(vec2 position, sampler2D etherValues){
  float aeVal = eth_getAether(position, etherValues);
  float neVal = eth_getNether(position, etherValues);
  return ( ((aeVal * aetherWeightInUnits) + neVal) / (aetherWeightInUnits + 1) );
}

float eth_getRatio(vec2 position, sampler2D etherValues){
  float aeVal = eth_getAether(position, etherValues);
  float neVal = eth_getNether(position, etherValues);
  if(0 < aeVal) return (neVal / aeVal);
    else return 0;
}