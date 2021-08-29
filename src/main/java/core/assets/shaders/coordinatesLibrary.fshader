/* =================== COORDINATES_LIBRARY =================== */

int coords_getOffsetCode(vec2 position, sampler2D proposedChanges){
  return int(texture(proposedChanges, position).r);
}

void coords_setOffsetCode(inout vec4 previous_color, float value){
  previous_color.r = value;
}

float coords_getToApply(vec2 position, sampler2D proposedChanges){
  return(texture(proposedChanges, position).g);
}

/*!Note: floating point values mean sampler indices */
float coords_getXFromOffsetCode(float x, int code){
    const float unitCoordinate = 1/chunkSize;
    switch(code){
        case 1: case 8: case 7: return (x-(1/chunkSize));
        case 2: case 0: case 6: return (x);
        case 3: case 4: case 5: return (x+(1/chunkSize));
    }
    return x;
}

float coords_getYFromOffsetCode(float y, int code){
    const float unitCoordinate = 1/chunkSize;
    switch(code){
        case 7: case 6: case 5: return (y+unitCoordinate);
        case 8: case 0: case 4: return (y);
        case 1: case 2: case 3: return (y-unitCoordinate);
    }
    return y;
}

float coords_getTargetX(vec2 position, sampler2D proposedChanges, float chunkSize){
  int offsetCode = coords_getOffsetCode(position, proposedChanges);
  return coords_getXFromOffsetCode(position.x, offsetCode);
}

float coords_getTargetY(vec2 position, sampler2D proposedChanges, float chunkSize){
  int offsetCode = coords_getOffsetCode(position, proposedChanges);
  return coords_getYFromOffsetCode(position.y, offsetCode);
}

bool coords_insideInnerBounds(vec2 position, float chunkSize){
  float unitCoordinate = 1/chunkSize;
  return (
    (unitCoordinate <= position.x)&&(1-unitCoordinate > position.x)
    &&(unitCoordinate <= position.y)&&(1-unitCoordinate > position.y)
  );
}

bool coords_insideEdges(vec2 position, float chunkSize){
  return ( (0 <= position.x)&&(1 > position.x)&&(0 <= position.y)&&(1 > position.y) );
}