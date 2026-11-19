package ohheck.table;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Where the computer players do their thinking. In play it's a background thread, so a Hard
 * player weighing hundreds of imagined deals never holds up a frame; the table waits for
 * the answer (and its usual pause) before playing it. Tests and the screenshot run think at
 * once, so their games repeat exactly. Either way a bot sees only its own snapshot of the
 * table and its own random numbers, so it decides the same thing.
 */
interface Thinker {

	<T> Future<T> think(Callable<T> decision);

	/** At once, on the calling thread. */
	Thinker AT_ONCE = new Thinker() {
		@Override
		public <T> Future<T> think(Callable<T> decision) {
			FutureTask<T> f = new FutureTask<T>(decision);
			f.run();
			return f;
		}
	};

	/** On one shared background thread. */
	static Thinker background() {
		return Background.THINKER;
	}

	/** The background thread, made the first time it's wanted. */
	final class Background {
		private static final ExecutorService THREAD = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "computer players");
			t.setDaemon(true); // never keeps the app from closing
			return t;
		});
		static final Thinker THINKER = new Thinker() {
			@Override
			public <T> Future<T> think(Callable<T> decision) {
				return THREAD.submit(decision);
			}
		};

		private Background() {
		}
	}
}
