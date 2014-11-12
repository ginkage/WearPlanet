precision mediump float;

uniform sampler2D uTexture0;
uniform sampler2D uTexture1;
uniform sampler2D uTexture2;
uniform sampler2D uTexture3;
uniform vec3 uRotate;

varying vec4 Position;

void main() {
	float sx = Position.x * 1.1;
	float sy = -Position.y * 1.1;
	float z2 = 1.0 - sx * sx - sy * sy;

	if (z2 > 0.0) {
		float sz = sqrt(z2);
		float tx = (1.0 + sx) * 0.5;
		float y = (sz * uRotate.y - sy * uRotate.z);
		float z = (sy * uRotate.y + sz * uRotate.z);
		vec2 vCoord;

		if (abs(z) > abs(y)) {
			vec4 vTex = texture2D(uTexture1, vec2(tx, (1.0 - y) * 0.5));
			vec4 vOff = floor(vTex * 255.0 + 0.5);
			vCoord = vec2(
				(vOff.y * 256.0 + vOff.x) / 16383.0,
				(vOff.w * 256.0 + vOff.z) / 16383.0);
			if (z < 0.0) { vCoord.x = 1.0 - vCoord.x; }
		}
		else {
			vec4 vTex = texture2D(uTexture2, vec2(tx, (1.0 + z) * 0.5));
			vec4 vOff = floor(vTex * 255.0 + 0.5);
			vCoord = vec2(
				(vOff.y * 256.0 + vOff.x) / 16383.0,
				(vOff.w * 256.0 + vOff.z) / 16383.0);
			if (y < 0.0) { vCoord.y = 1.0 - vCoord.y; }
		}

		vCoord.x += uRotate.x;

		vec3 vCol = texture2D(uTexture0, vCoord).rgb;
		vec3 vNorm = normalize(texture2D(uTexture3, vCoord).rgb - 0.5);

		float sin_theta = sy;
		float cos_theta = sqrt(1.0 - sy * sy);
		float sin_phi = sx / cos_theta;
		float cos_phi = sz / cos_theta;
		float light = (vNorm.z * cos_theta - vNorm.y * sin_theta) * cos_phi  - vNorm.x * sin_phi;

		gl_FragColor = vec4(vCol * light, 1.0);
	} else {
		gl_FragColor = vec4(0.25, 0.5, 1.0, (z2 + 0.21) * 1.5);
	}
}
