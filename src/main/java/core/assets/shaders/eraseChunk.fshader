  #version 440
  out vec4 gl_FragColor;
  in vec4 gl_FragCoord;  /* Going from 0 to image size */
  varying vec2 v_texCoords;

  void main(void){
    gl_FragColor.r = 0; gl_FragColor.g = 0; gl_FragColor.b = 0; gl_FragColor.a = 1;
  }