package com.jetbrains.actionscript.profiler.sampler;

public class CreateObjectSample extends Sample {
  public final int id;
  public final String className;
  public final int size;

  public CreateObjectSample(long duration, FrameInfo[] frames, int id, String className, int size) {
    super(duration, frames);
    this.id = id;
    this.className = className;
    this.size = size;
  }
}
