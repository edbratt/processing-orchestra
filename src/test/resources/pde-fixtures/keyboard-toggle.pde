boolean inverted = false;

void setup() {
  size(300, 300);
}

void draw() {
  if (inverted) {
    background(255);
  } else {
    background(0);
  }
}

void keyPressed() {
  if (key == ' ') {
    inverted = !inverted;
  }
}
