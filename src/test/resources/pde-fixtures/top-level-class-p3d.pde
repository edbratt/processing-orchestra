float spin = 0;
Blob blob;

void setup() {
  size(500, 300, P3D);
  blob = new Blob(40);
}

void draw() {
  background(0);
  translate(width / 2, height / 2, 0);
  rotateY(spin);
  blob.display();
  spin += 0.01;
}

class Blob {
  float radius;

  Blob(float radius) {
    this.radius = radius;
  }

  void display() {
    noFill();
    stroke(255);
    sphere(radius);
  }
}
