package com.chamchamcham.api.weather;


public record KmaGrid(int nx, int ny) {
    public int getNx() { return nx; }
    public int getNy() { return ny; }
}
