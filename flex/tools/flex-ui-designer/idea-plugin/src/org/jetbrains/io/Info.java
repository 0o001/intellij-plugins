package org.jetbrains.io;

import org.jetbrains.annotations.NotNull;

public class Info<E> implements Identifiable {
  int id = -1;
  protected final E element;

  public Info(@NotNull E element) {
    this.element = element;
  }

  @Override
  public int getId() {
    return id;
  }

  public E getElement() {
    return element;
  }
}