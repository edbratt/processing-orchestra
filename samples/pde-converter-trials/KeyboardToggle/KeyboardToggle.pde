float hueValue = 20;
float circleSize = 120;
boolean ringVisible = true;

void setup() {
  size(500, 350);
  colorMode(HSB, 360, 100, 100);
  strokeWeight(4);
}

void draw() {
  background(225, 20, 12);

  fill(hueValue, 80, 100);
  noStroke();
  ellipse(width / 2, height / 2, circleSize, circleSize);

  if (ringVisible) {
    noFill();
    stroke((hueValue + 180) % 360, 60, 100);
    ellipse(width / 2, height / 2, circleSize + 40, circleSize + 40);
  }

  fill(0, 0, 100);
  textAlign(CENTER, CENTER);
  text("Space toggles ring, arrows change color", width / 2, height - 24);
}

void keyPressed() {
  if (key == ' ') {
    ringVisible = !ringVisible;
  }

  if (keyCode == LEFT) {
    hueValue = (hueValue + 345) % 360;
  } else if (keyCode == RIGHT) {
    hueValue = (hueValue + 15) % 360;
  }
}
