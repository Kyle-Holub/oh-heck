package ohheck.fx;

/** A fixed pool of particles shared by Sparks and Motes; nothing is allocated after creation. */
abstract class ParticlePool {

	final int max;
	final float[] x, y, vx, vy, life, maxLife, size;
	float clock;

	ParticlePool(int max) {
		this.max = max;
		x = new float[max];
		y = new float[max];
		vx = new float[max];
		vy = new float[max];
		life = new float[max];
		maxLife = new float[max];
		size = new float[max];
	}

	int free() {
		for (int i = 0; i < max; i++) {
			if (life[i] <= 0) {
				return i;
			}
		}
		return -1;
	}

	void born(int i, float px, float py, float pvx, float pvy, float lifetime, float sz) {
		x[i] = px;
		y[i] = py;
		vx[i] = pvx;
		vy[i] = pvy;
		maxLife[i] = life[i] = lifetime;
		size[i] = sz;
	}

	boolean alive(int i) {
		return life[i] > 0;
	}

	float age(int i) {
		return 1 - life[i] / maxLife[i];
	}

	public boolean active() {
		for (int i = 0; i < max; i++) {
			if (life[i] > 0) {
				return true;
			}
		}
		return false;
	}

	public void step(float dt) {
		clock += dt;
		for (int i = 0; i < max; i++) {
			if (life[i] <= 0) {
				continue;
			}
			life[i] -= dt;
			accelerate(i, dt);
			x[i] += vx[i] * dt;
			y[i] += vy[i] * dt;
		}
	}

	void accelerate(int i, float dt) {
	}
}
