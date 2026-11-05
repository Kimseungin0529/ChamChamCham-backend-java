package com.chamchamcham.application.pesticide.sync;


public record PesticideSyncResult(
    int fetchedRowCount,
    int createdApplicationCount,
    int pageCount
) {
    public int getFetchedRowCount() { return fetchedRowCount; }
    public int getCreatedApplicationCount() { return createdApplicationCount; }
    public int getPageCount() { return pageCount; }
}
