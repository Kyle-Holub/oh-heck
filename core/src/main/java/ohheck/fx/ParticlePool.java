package ohheck.fx;

/**
 * A fixed pool of small particles (position, velocity, life and size), shared
 * by the table's effects: {@link Sparks} and {@link Motes}. Subclasses decide
 * how particles are born, move and look; nothing is allocated after creation.
 */
abstract class ParticlePool {

	final int max;
	final float[] x, y, vx, vy, life, maxLife, size;
	/** Seconds of game time, for twinkles and sway. */
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

	/** A free slot, or -1 if all are in use. */
	int free() {
		for (int i = 0; i < max; i++) {
			if (life[i] <= 0) {
				return i;
			}
		}
		return -1;
	}

	/** Starts particle i with a lifetime of the given seconds. */
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

	/** 0 when born, rising to 1 as it dies. */
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

	/** Ages every live particle and moves it by its velocity, after {@link #accelerate}. */
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

	/** Changes particle i's velocity for this step (gravity, drag, sway); the default does nothing. */
	void accelerate(int i, float dt) {
	}
}
