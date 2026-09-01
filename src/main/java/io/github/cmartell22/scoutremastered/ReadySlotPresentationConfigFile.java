package io.github.cmartell22.scoutremastered;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** External-file operations for the client presentation config. */
public final class ReadySlotPresentationConfigFile {
	private ReadySlotPresentationConfigFile() {
	}

	public static LoadResult load(Path path, ReadySlotPresentationConfig fallback) {
		if (!Files.exists(path)) {
			return new LoadResult(fallback, Status.MISSING, null);
		}
		if (!Files.isRegularFile(path)) {
			return new LoadResult(fallback, Status.NOT_REGULAR, null);
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return new LoadResult(ReadySlotPresentationConfig.parse(reader), Status.EXTERNAL, null);
		} catch (IOException | RuntimeException exception) {
			return new LoadResult(fallback, Status.INVALID, exception);
		}
	}

	/** Creates the tested baseline without ever replacing a file that appeared concurrently. */
	public static void createDefault(Path path, String json) throws IOException {
		Files.createDirectories(path.getParent());
		Files.writeString(
			path,
			json,
			StandardCharsets.UTF_8,
			StandardOpenOption.CREATE_NEW,
			StandardOpenOption.WRITE
		);
	}

	/** Atomically replaces the client-only presentation file whenever the file system supports it. */
	public static void save(Path path, ReadySlotPresentationConfig config) throws IOException {
		Path absolute = path.toAbsolutePath();
		Path parent = absolute.getParent();
		if (parent == null) {
			throw new IOException("Ready Slots config path has no parent: " + path);
		}
		Files.createDirectories(parent);
		Path temporary = Files.createTempFile(parent, absolute.getFileName().toString() + ".", ".tmp");
		try {
			Files.writeString(
				temporary,
				config.toJson(),
				StandardCharsets.UTF_8,
				StandardOpenOption.TRUNCATE_EXISTING,
				StandardOpenOption.WRITE
			);
			try {
				Files.move(
					temporary,
					absolute,
					StandardCopyOption.ATOMIC_MOVE,
					StandardCopyOption.REPLACE_EXISTING
				);
			} catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	public enum Status {
		EXTERNAL,
		MISSING,
		NOT_REGULAR,
		INVALID
	}

	public record LoadResult(
		ReadySlotPresentationConfig config,
		Status status,
		Exception failure
	) {
	}
}
