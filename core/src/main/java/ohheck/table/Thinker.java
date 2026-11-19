package ohheck.table;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

/**
 * Where the bots think: on a background thread in play, at once in tests and the screenshot
 * run.
 */
interface Thinker {

	<T> Future<T> think(Callable<T> decision);

	Thinker AT_ONCE = new Thinker() {
		@Override
		public <T> Future<T> think(Callable<T> decision) {
			FutureTask<T> f = new FutureTask<T>(decision);
			f.run();
			return f;
		}
	};

	static Thinker background() {
		return Background.THINKER;
	}

	final class Background {
		private static final ExecutorService THREAD = Executors.newSingleThreadExecutor(r -> {
			Thread t = new Thread(r, "computer players");
			t.setDaemon(true);
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
