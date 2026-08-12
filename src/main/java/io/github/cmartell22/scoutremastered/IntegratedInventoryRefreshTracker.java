package io.github.cmartell22.scoutremastered;

import java.util.Objects;

/** Debounces client requests to revalidate an open server-authoritative integrated session. */
public final class IntegratedInventoryRefreshTracker {
	private boolean sessionOpen;
	private boolean requestPending;
	private boolean mismatchHandled;

	public void beginSession() {
		this.sessionOpen = true;
		this.requestPending = true;
		this.mismatchHandled = true;
	}

	public void completeRequest(boolean consistent) {
		if (this.sessionOpen) {
			this.requestPending = false;
			this.mismatchHandled = !consistent;
		}
	}

	public void endSession() {
		this.sessionOpen = false;
		this.requestPending = false;
		this.mismatchHandled = false;
	}

	public boolean shouldRequest(
		IntegratedInventoryData expected,
		IntegratedInventoryData active,
		IntegratedInventoryData equipped
	) {
		Objects.requireNonNull(expected, "expected");
		Objects.requireNonNull(active, "active");
		Objects.requireNonNull(equipped, "equipped");
		if (!this.sessionOpen || this.requestPending) {
			return false;
		}
		boolean consistent = expected.equals(active) && expected.equals(equipped);
		if (consistent) {
			this.mismatchHandled = false;
			return false;
		}
		if (this.mismatchHandled) {
			return false;
		}
		this.mismatchHandled = true;
		this.requestPending = true;
		return true;
	}
}
