float circleX = 200;
float speed = 2;

void setup() {
  size(400, 400);
}

void draw() {
  background(0);
  circleX += speed;
  ellipse(circleX, 200, 80, 80);
}
