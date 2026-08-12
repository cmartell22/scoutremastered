package io.github.cmartell22.scoutremastered.client;

/** Read-only layout bridge implemented by the ADR-012 AbstractContainerScreen Mixin. */
public interface IntegratedScreenLayoutAccess {
	int scoutremastered$leftPos();

	int scoutremastered$topPos();

	void scoutremastered$setTopPos(int topPos);

	int scoutremastered$imageWidth();

	int scoutremastered$imageHeight();
}
