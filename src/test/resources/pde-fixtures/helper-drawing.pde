float radius = 25;

void setup() {
  size(320, 240);
  noStroke();
}

void draw() {
  background(10);
  drawDot(width / 2, height / 2);
}

void drawDot(float x, float y) {
  fill(80, 180, 255);
  ellipse(x, y, radius * 2, radius * 2);
}
