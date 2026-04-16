float circleSize = 70;
float circleHue = 200;

void setup() {
  size(500, 350);
  colorMode(HSB, 360, 100, 100);
  noStroke();
}

void draw() {
  background(230, 50, 15);

  float pulse = 1.0 + sin(frameCount * 0.08) * 0.12;

  fill(circleHue, 80, 100);
  ellipse(mouseX, mouseY, circleSize * pulse, circleSize * pulse);

  fill(0, 0, 100);
  textAlign(CENTER, CENTER);
  text("Move the mouse", width / 2, height - 24);
}
