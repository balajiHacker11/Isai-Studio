const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: denied all reads and writes", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").set({
      userId: ALICE_UID,
      name: "Vibes",
      description: "",
      trackIds: [1],
      coverKey: "artist_mic_studio",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner create, partial update, cross-user & timestamp guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Listener",
      email: "alice@example.com",
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice Studio",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Immutable createdAt cannot be mutated
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 200),
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Future timestamp rejected
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      displayName: "Bob",
      email: "bob@example.com",
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Cross-user PII read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());

  // Blanket user listing rejected
  await assertFails(aliceDb.collection("users").get());
});

test("Liked tracks: create, list, delete, reject invalid ID or extra @DocumentId field", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("track_1").set({
      userId: ALICE_UID,
      trackNumber: 1,
      title: "Katchi Sera",
      album: "iSaiAbhyankkar (The Indie Isai)",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get()
  );

  // Reject ghost `id` field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("track_2").set({
      id: "track_2",
      userId: ALICE_UID,
      trackNumber: 2,
      title: "Aasa Kooda",
      album: "iSaiAbhyankkar (The Indie Isai)",
      createdAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read & delete blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("track_1").delete());

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("track_1").delete()
  );
});

test("Playlists, play history, lyrics, and preferences: full CRUD & schema bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Playlist create + update
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_indie").set({
      userId: ALICE_UID,
      name: "Late Night Think Indie",
      description: "Top Sai Abhyankkar hits",
      trackIds: [1, 2, 3, 4],
      coverKey: "artist_mic_studio",
      createdAt: past,
      updatedAt: past,
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_indie").update({
      trackIds: [1, 2, 3, 4, 18],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Play history
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("play_history").doc("hist_1").set({
      userId: ALICE_UID,
      trackNumber: 18,
      title: "Oorum Blood",
      album: "Dude (Original Motion Picture Soundtrack)",
      playedAt: new Date(Date.now() - 1000),
    })
  );

  // Synced LRC lyrics
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lyrics").doc("track_1").set({
      userId: ALICE_UID,
      trackNumber: 1,
      lrcContent: "[00:05.00]Katchi Sera...",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("lyrics").doc("track_1").update({
      lrcContent: "[00:05.00]Katchi Sera updated",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Preferences
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("preferences").doc("settings").set({
      userId: ALICE_UID,
      darkThemeEnabled: true,
      dynamicTintEnabled: true,
      accentKey: "coral",
      crossfadeSeconds: 4,
      gaplessEnabled: true,
      normalizeVolumeEnabled: false,
      eqPreset: "Studio",
      eqBands: [200, 100, 0, 150, 300],
      privateSessionEnabled: false,
      swipeMiniPlayerEnabled: true,
      swipeArtworkEnabled: true,
      swipeRowQueueEnabled: true,
      swipeRowLikeEnabled: true,
      doubleTapSeekEnabled: true,
      doubleTapLikeEnabled: true,
      hapticFeedbackEnabled: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid accentKey enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("preferences").doc("settings").update({
      accentKey: "neon_green",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});
