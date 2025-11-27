package logging;

import java.util.ArrayList;
import java.util.List;

public class Logger {

	LogLevel minLevel;
	List<LogEntry> data;

	public Logger(LogLevel minLevel) {
		this.data = new ArrayList<>();
		this.minLevel = minLevel;
	}

	public void log(LogLevel level, String s) {
		if (level.compareTo(this.minLevel) >= 0) {
			this.data.add(new LogEntry(level, s));
		}
	}

	// returns the highest log level encountered within the logs.
	// If there are no logs, DEBUG is returned.
	public LogLevel dump() {
		LogLevel maxLevel = LogLevel.DEBUG;
		for (LogEntry entry : this.data) {
			if (entry.level.compareTo(maxLevel) > 0) {
				maxLevel = entry.level;
			}
			System.out.println("[" + entry.level.name() + "] " + entry.data);
		}

		return maxLevel;
	}

	/////////////////////////

	private static class LogEntry {
		LogLevel level;
		String data;

		public LogEntry(LogLevel level, String data) {
			this.level = level;
			this.data = data;
		}
	}
}
