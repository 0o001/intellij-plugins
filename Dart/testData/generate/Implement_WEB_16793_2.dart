class Animal {
  void speak(int times, {String say: 'woof'});
}

class Dog implements Animal {
  String name;
  Dog(this.name);
  <caret>
}