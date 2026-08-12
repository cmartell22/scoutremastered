package com.example.scout26.client;

/** Read-only layout bridge implemented by the ADR-012 AbstractContainerScreen Mixin. */
public interface IntegratedScreenLayoutAccess {
	int scout26$leftPos();

	int scout26$topPos();

	void scout26$setTopPos(int topPos);

	int scout26$imageWidth();

	int scout26$imageHeight();
}
