precision mediump float;

attribute vec4 vPosition;

uniform vec4 uRatio;

varying vec4 Position;

void main() {
	gl_Position = vPosition * uRatio;
	Position = vPosition;
}