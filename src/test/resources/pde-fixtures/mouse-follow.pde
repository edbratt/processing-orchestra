float circleSize = 60;

void setup() {
  size(400, 400);
  noStroke();
}

void draw() {
  background(20);
  ellipse(mouseX, mouseY, circleSize, circleSize);
}

void mousePressed() {
  circleSize += 10;
}
