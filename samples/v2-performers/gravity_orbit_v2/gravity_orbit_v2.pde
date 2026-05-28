// gravity_orbit_v2
//
// Stand-alone Processing performer for processing-server v2 OSC mode.
//
// Required Processing libraries:
// - oscP5
//
// Listens for the current v2 walker-trigger contract:
// - /walker/signal/x   float [-1..1]
// - /walker/signal/y   float [-1..1]
// - /test/ping         string "ping", int count
// - /event/ping        int count
//
// Also listens for the general v2 orchestra-input-v1 contract:
// - /input/position    float x, float y
// - /input/trigger     string kind, int count
//
// Preferred for this sketch: orchestra-session-v1
// - /session/join      string sessionId, string name, string instrumentId
// - /session/leave     string sessionId
// - /session/position  string sessionId, float x, float y
// - /session/trigger   string sessionId, string kind, int count
// - /session/pitch     string sessionId, string note, int midi, float frequency, float level
//
// Run processing-server with:
//   .\run.ps1 -Properties "-Doutput.mode=osc -Dosc.debug.logging=true"

import oscP5.*;
import java.util.ArrayList;
import java.util.HashMap;

final int LISTEN_PORT = 12002;
final float BASE_ATTRACTION = 0.0015;
final float BASE_ORBIT = 0.00075;
final float DAMPING = 0.93;
final float MAX_SPEED = 0.026;

OscP5 osc;

float walkerX = 0;
float walkerY = 0;
boolean haveWalkerX = false;
boolean haveWalkerY = false;

float anchorX = 0.5;
float anchorY = 0.5;
float pulse = 0;
int pingCount = 0;
int nextBodyIndex = 0;

HashMap<String, Orbiter> bodiesBySession = new HashMap<String, Orbiter>();

void setup() {
  size(1000, 720);
  colorMode(HSB, 360, 100, 100, 100);
  smooth(4);
  frameRate(60);
  surface.setTitle("gravity_orbit_v2 | OSC port " + LISTEN_PORT);

  osc = new OscP5(this, LISTEN_PORT);
  println("gravity_orbit_v2 listening on OSC port " + LISTEN_PORT);
}

void draw() {
  background(0, 0, 4);

  updateSharedAnchor();

  pulse = lerp(pulse, 0, 0.08);

  drawField();
  updateBodies();
  drawConnections();
  drawAnchor();
  drawHud();
}

void updateBodies() {
  ArrayList<Orbiter> bodies = activeBodies();

  for (int i = 0; i < bodies.size(); i++) {
    Orbiter body = bodies.get(i);

    float dx = body.targetX - body.x;
    float dy = body.targetY - body.y;
    float distSq = max(0.0004, dx * dx + dy * dy);
    float dist = sqrt(distSq);
    float nx = dx / dist;
    float ny = dy / dist;

    float attraction = BASE_ATTRACTION * (0.75 + pulse * 1.4) / distSq;
    float orbitDirection = body.orbitDirection;
    float orbit = BASE_ORBIT * (1.0 + body.phaseOffset * 0.12);

    body.vx += nx * attraction + (-ny * orbit * orbitDirection);
    body.vy += ny * attraction + (nx * orbit * orbitDirection);

    for (int j = i + 1; j < bodies.size(); j++) {
      pushApart(body, bodies.get(j));
    }
  }

  for (Orbiter body : bodies) {
    body.vx *= DAMPING;
    body.vy *= DAMPING;
    limitVelocity(body);
    body.x += body.vx;
    body.y += body.vy;
    bounce(body);
    body.energy = lerp(body.energy, pulse, 0.08);
    body.draw();
  }
}

ArrayList<Orbiter> activeBodies() {
  return new ArrayList<Orbiter>(bodiesBySession.values());
}

void updateSharedAnchor() {
  ArrayList<Orbiter> bodies = activeBodies();
  if (!bodies.isEmpty()) {
    float sx = 0;
    float sy = 0;
    for (Orbiter body : bodies) {
      sx += body.targetX;
      sy += body.targetY;
    }
    anchorX = lerp(anchorX, sx / bodies.size(), 0.15);
    anchorY = lerp(anchorY, sy / bodies.size(), 0.15);
    return;
  }

  if (haveWalkerX && haveWalkerY) {
    float targetX = map(walkerX, -1, 1, 0.08, 0.92);
    float targetY = map(walkerY, -1, 1, 0.92, 0.08);
    anchorX = lerp(anchorX, targetX, 0.22);
    anchorY = lerp(anchorY, targetY, 0.22);
  }
}

void pushApart(Orbiter a, Orbiter b) {
  float dx = b.x - a.x;
  float dy = b.y - a.y;
  float distSq = dx * dx + dy * dy;
  if (distSq < 0.000001) {
    dx = random(-0.01, 0.01);
    dy = random(-0.01, 0.01);
    distSq = dx * dx + dy * dy;
  }

  float minDist = a.radiusNormalized() + b.radiusNormalized();
  float dist = sqrt(distSq);
  if (dist >= minDist) {
    return;
  }

  float nx = dx / dist;
  float ny = dy / dist;
  float overlap = (minDist - dist) * 0.45;
  a.x -= nx * overlap;
  a.y -= ny * overlap;
  b.x += nx * overlap;
  b.y += ny * overlap;
}

void limitVelocity(Orbiter body) {
  float speed = sqrt(body.vx * body.vx + body.vy * body.vy);
  if (speed > MAX_SPEED) {
    float scale = MAX_SPEED / speed;
    body.vx *= scale;
    body.vy *= scale;
  }
}

void bounce(Orbiter body) {
  float radius = body.radiusNormalized();
  if (body.x < radius) {
    body.x = radius;
    body.vx *= -0.45;
  } else if (body.x > 1 - radius) {
    body.x = 1 - radius;
    body.vx *= -0.45;
  }

  if (body.y < radius) {
    body.y = radius;
    body.vy *= -0.45;
  } else if (body.y > 1 - radius) {
    body.y = 1 - radius;
    body.vy *= -0.45;
  }
}

void drawField() {
  noFill();
  strokeWeight(1);
  for (int i = 0; i < 5; i++) {
    float r = (90 + i * 70) * (1 + pulse * 0.45);
    stroke(205, 55, 70, 24 - i * 3);
    ellipse(anchorX * width, anchorY * height, r, r);
  }
}

void drawConnections() {
  ArrayList<Orbiter> bodies = activeBodies();
  strokeWeight(1.1);
  for (int i = 0; i < bodies.size(); i++) {
    for (int j = i + 1; j < bodies.size(); j++) {
      Orbiter a = bodies.get(i);
      Orbiter b = bodies.get(j);
      float dx = b.x - a.x;
      float dy = b.y - a.y;
      float dist = sqrt(dx * dx + dy * dy);
      if (dist < 0.34) {
        float alpha = map(dist, 0.04, 0.34, 48, 4);
        stroke(190, 30, 80, alpha);
        line(a.x * width, a.y * height, b.x * width, b.y * height);
      }
    }
  }
}

void drawAnchor() {
  float x = anchorX * width;
  float y = anchorY * height;
  noStroke();
  fill(48, 90, 100, 14 + pulse * 35);
  ellipse(x, y, 90 + pulse * 80, 90 + pulse * 80);
  fill(48, 80, 100, 90);
  ellipse(x, y, 14 + pulse * 18, 14 + pulse * 18);
}

void drawHud() {
  fill(0, 0, 92, 72);
  textSize(13);
  textAlign(LEFT, TOP);
  text("gravity_orbit_v2  |  port " + LISTEN_PORT
    + "  |  collaborators=" + bodiesBySession.size()
    + "  |  pings=" + pingCount,
    18, 16);
}

void oscEvent(OscMessage msg) {
  if (msg.checkAddrPattern("/session/join") && msg.checkTypetag("sss")) {
    String sessionId = msg.get(0).stringValue();
    String name = msg.get(1).stringValue();
    String instrumentId = msg.get(2).stringValue();
    getOrCreateBody(sessionId, name, instrumentId);
    return;
  }

  if (msg.checkAddrPattern("/session/leave") && msg.checkTypetag("s")) {
    String sessionId = msg.get(0).stringValue();
    bodiesBySession.remove(sessionId);
    return;
  }

  if (msg.checkAddrPattern("/session/position") && msg.checkTypetag("sff")) {
    String sessionId = msg.get(0).stringValue();
    float x = constrain(msg.get(1).floatValue(), -1, 1);
    float y = constrain(msg.get(2).floatValue(), -1, 1);
    receiveSessionPosition(sessionId, x, y);
    return;
  }

  if (msg.checkAddrPattern("/session/trigger") && msg.checkTypetag("ssi")) {
    String sessionId = msg.get(0).stringValue();
    String kind = msg.get(1).stringValue();
    receiveSessionTrigger(sessionId, kind);
    return;
  }

  if (msg.checkAddrPattern("/session/pitch") && msg.checkTypetag("ssiff")) {
    String sessionId = msg.get(0).stringValue();
    String note = msg.get(1).stringValue();
    int midi = msg.get(2).intValue();
    float frequency = msg.get(3).floatValue();
    float level = msg.get(4).floatValue();
    receiveSessionPitch(sessionId, note, midi, frequency, level);
    return;
  }

  if (msg.checkAddrPattern("/input/position") && msg.checkTypetag("ff")) {
    walkerX = constrain(msg.get(0).floatValue(), -1, 1);
    walkerY = constrain(msg.get(1).floatValue(), -1, 1);
    haveWalkerX = true;
    haveWalkerY = true;
    receiveSessionPosition("aggregate", walkerX, walkerY);
    return;
  }

  if (msg.checkAddrPattern("/input/trigger")) {
    receiveSessionTrigger("aggregate", "trigger");
    return;
  }

  if (msg.checkAddrPattern("/input/pitch") && msg.checkTypetag("siff")) {
    String note = msg.get(0).stringValue();
    int midi = msg.get(1).intValue();
    float frequency = msg.get(2).floatValue();
    float level = msg.get(3).floatValue();
    receiveSessionPitch("aggregate", note, midi, frequency, level);
    return;
  }

  if (msg.checkAddrPattern("/walker/signal/x") && msg.checkTypetag("f")) {
    walkerX = constrain(msg.get(0).floatValue(), -1, 1);
    haveWalkerX = true;
    return;
  }

  if (msg.checkAddrPattern("/walker/signal/y") && msg.checkTypetag("f")) {
    walkerY = constrain(msg.get(0).floatValue(), -1, 1);
    haveWalkerY = true;
    return;
  }

  if (msg.checkAddrPattern("/test/ping") || msg.checkAddrPattern("/event/ping")) {
    receiveSessionTrigger("legacy", "ping");
  }
}

void receivePing() {
  pingCount++;
  pulse = 1.0;
  for (Orbiter body : activeBodies()) {
    float dx = body.x - anchorX;
    float dy = body.y - anchorY;
    float mag = max(0.001, sqrt(dx * dx + dy * dy));
    body.vx += (dx / mag) * random(0.004, 0.014);
    body.vy += (dy / mag) * random(0.004, 0.014);
    body.energy = 1.0;
    body.hueVelocity += random(-9, 9);
  }
}

void receiveSessionPosition(String sessionId, float oscX, float oscY) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  body.targetX = map(oscX, -1, 1, 0.08, 0.92);
  body.targetY = map(oscY, -1, 1, 0.92, 0.08);
  walkerX = oscX;
  walkerY = oscY;
  haveWalkerX = true;
  haveWalkerY = true;
}

void receiveSessionTrigger(String sessionId, String kind) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  pingCount++;
  pulse = 1.0;
  body.energy = 1.0;
  body.hueVelocity += random(-12, 12);

  float dx = body.x - body.targetX;
  float dy = body.y - body.targetY;
  float mag = max(0.001, sqrt(dx * dx + dy * dy));
  float strength = "shake".equals(kind) ? 0.02 : 0.012;
  body.vx += (dx / mag) * random(strength * 0.45, strength);
  body.vy += (dy / mag) * random(strength * 0.45, strength);
}

void receiveSessionPitch(String sessionId, String note, int midi, float frequency, float level) {
  Orbiter body = getOrCreateBody(sessionId, "", "");
  body.lastNote = note;
  body.lastFrequency = frequency;
  body.hueBase = (midi % 12) * 30;
  body.energy = max(body.energy, constrain(level * 8, 0.1, 1.0));
}

Orbiter getOrCreateBody(String sessionId, String name, String instrumentId) {
  if (sessionId == null || sessionId.length() == 0) {
    sessionId = "unknown";
  }
  Orbiter body = bodiesBySession.get(sessionId);
  if (body != null) {
    if (name != null && name.trim().length() > 0) {
      body.label = name.trim();
    }
    if (instrumentId != null && instrumentId.trim().length() > 0) {
      body.instrumentId = instrumentId.trim();
    }
    return body;
  }

  int index = nextBodyIndex++;
  float angle = TWO_PI * index / max(1, nextBodyIndex);
  float radius = 0.16 + 0.035 * (index % 4);
  String label = (name == null || name.trim().length() == 0) ? shortSessionId(sessionId) : name.trim();
  body = new Orbiter(
    sessionId,
    label,
    instrumentId == null ? "" : instrumentId,
    0.5 + cos(angle) * radius,
    0.5 + sin(angle) * radius,
    28 + index * 47 % 360,
    index
  );
  bodiesBySession.put(sessionId, body);
  return body;
}

String shortSessionId(String sessionId) {
  return sessionId.length() <= 8 ? sessionId : sessionId.substring(0, 8);
}

void mousePressed() {
  walkerX = map(mouseX, 0, width, -1, 1);
  walkerY = map(mouseY, height, 0, -1, 1);
  haveWalkerX = true;
  haveWalkerY = true;
  receiveSessionPosition("local", walkerX, walkerY);
  receiveSessionTrigger("local", "mouse");
}

class Orbiter {
  String sessionId;
  String label;
  String instrumentId;
  String lastNote = "";
  float lastFrequency = 0;
  float x;
  float y;
  float targetX;
  float targetY;
  float vx;
  float vy;
  float hueBase;
  float hueCurrent;
  float hueVelocity;
  float energy;
  float phaseOffset;
  float orbitDirection;

  Orbiter(String sessionId, String label, String instrumentId, float x, float y, float hueBase, int index) {
    this.sessionId = sessionId;
    this.label = label;
    this.instrumentId = instrumentId;
    this.x = x;
    this.y = y;
    this.targetX = x;
    this.targetY = y;
    this.hueBase = hueBase;
    this.hueCurrent = hueBase;
    this.phaseOffset = index % 5;
    this.orbitDirection = (index % 2 == 0) ? 1 : -1;
  }

  float radiusPixels() {
    return 28 + phaseOffset * 6 + energy * 26;
  }

  float radiusNormalized() {
    return (radiusPixels() * 0.5 + 5) / min(width, height);
  }

  void draw() {
    hueVelocity = lerp(hueVelocity, 0, 0.07);
    hueCurrent = (hueBase + hueVelocity + 360) % 360;
    float speed = constrain(sqrt(vx * vx + vy * vy) / MAX_SPEED, 0, 1);
    float r = radiusPixels();
    float px = x * width;
    float py = y * height;

    noStroke();
    fill(hueCurrent, 65 + speed * 25, 85 + energy * 15, 18);
    ellipse(px, py, r * 2.0, r * 2.0);
    fill(hueCurrent, 72 + speed * 20, 92 + energy * 8, 88);
    ellipse(px, py, r, r);
    fill((hueCurrent + 35) % 360, 35, 100, 72);
    ellipse(px - r * 0.16, py - r * 0.18, r * 0.32, r * 0.32);

    stroke(hueCurrent, 45, 95, 44);
    strokeWeight(1);
    line(px, py, targetX * width, targetY * height);
    fill(0, 0, 95, 72);
    textAlign(CENTER, TOP);
    textSize(12);
    String displayLabel = lastNote.length() == 0 ? label : label + "  " + lastNote;
    text(displayLabel, px, py + r * 0.62);
  }
}
