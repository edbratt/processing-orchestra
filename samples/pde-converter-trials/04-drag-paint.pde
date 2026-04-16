float brushX = 260;
float brushY = 180;
float brushHue = 20;
float brushSize = 36;

void setup() {
  size(520, 360);
  colorMode(HSB, 360, 100, 100, 100);
  background(220, 20, 10);
  noStroke();
}

void draw() {
  fadeBackground();
  drawBrush();
  drawLabel();
}

void mouseDragged() {
  brushX = mouseX;
  brushY = mouseY;
  brushHue = (brushHue + 12) % 360;
}

void fadeBackground() {
  fill(220, 20, 10, 10);
  rect(0, 0, width, height);
}

void drawBrush() {
  fill(brushHue, 80, 100, 90);
  ellipse(brushX, brushY, brushSize, brushSize);
}

void drawLabel() {
  fill(0, 0, 100, 85);
  textAlign(CENTER, CENTER);
  text("Drag to paint", width / 2, height - 24);
}
