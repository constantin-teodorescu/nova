package com.dotc.nova.filesystem;

import java.io.*;
import java.util.concurrent.*;

import com.dotc.nova.ProcessingLoop;

public class Filesystem {
	private final ProcessingLoop eventDispatcher;
	private final ExecutorService fileReadExecutor;

	public Filesystem(ProcessingLoop eventDispatcher) {
		this.eventDispatcher = eventDispatcher;
		ThreadFactory threadFactory = new ThreadFactory() {

			@Override
			public Thread newThread(Runnable arg0) {
				Thread t = new Thread(arg0, "FileSystemReader-" + System.currentTimeMillis());
				t.setDaemon(true);
				return t;
			}
		};
		fileReadExecutor = Executors.newCachedThreadPool(threadFactory);
	}

	public void readFile(String filePath, final FileReadHandler handler) {
		fileReadExecutor.execute(new AsyncFileReadCommand(filePath, handler));
	}

	public String readFileSync(String filePath) throws IOException {
		File file = new File(filePath);
		if (!file.exists()) {
			throw new FileNotFoundException();
		}

		BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		try {
			StringBuffer buf = new StringBuffer();
			while (reader.ready()) {
				buf.append(reader.readLine());
				if (reader.ready()) {
					buf.append('\n');
				}
			}
			return buf.toString();
		} finally {
			reader.close();
		}
	}

	private class AsyncFileReadCommand implements Runnable {
		private final FileReadHandler handler;
		private final String filePath;

		public AsyncFileReadCommand(String filePath, FileReadHandler handler) {
			this.handler = handler;
			this.filePath = filePath;
		}

		@Override
		public void run() {
			try {
				final String contents = readFileSync(filePath);
				eventDispatcher.dispatch(new Runnable() {

					@Override
					public void run() {
						handler.fileRead(contents);
					}
				});
			} catch (final IOException e) {
				eventDispatcher.dispatch(new Runnable() {

					@Override
					public void run() {
						handler.errorOccurred(e);
					}
				});
			}

		}

	}

}
