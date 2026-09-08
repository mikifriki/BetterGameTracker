"use strict";
const $ = (id) => document.getElementById(id);
const schemas = {
  game: [
    ["gameTitle", "Title"],
    ["description", "Description", "textarea", false],
    ["releasePlatform", "Platform", "text", false],
    ["releaseDate", "Release date", "text", false],
    ["developer", "Developer", "text", false],
    ["metaRating", "Critic rating", "text", false],
    ["userRating", "Your rating", "text", false],
    ["physicalCopy", "Physical copy", "text", false],
  ],
  play: [
    ["playthroughRating", "Rating"],
    ["completionDate", "Completion date"],
    ["platformPlayedOn", "Platform"],
    ["timeToBeat", "Total play time", "text", false],
    ["completionRate", "Completion"],
    ["location", "Location"],
    ["coop", "Co-op", "text", false],
  ],
  review: [
    ["reviewTitle", "Title", "text", false],
    ["reviewDate", "Date", "text", false],
    ["review", "Review", "textarea", false],
    ["rating", "Rating", "text", false],
  ],
  time: [
    ["date", "Date", "date"],
    ["durationMinutes", "Minutes", "number"],
    ["notes", "Notes", "textarea", false],
  ],
};
let session, selectedGame, selectedPlay, saveEditor;
function node(tag, text, className) {
  const n = document.createElement(tag);
  if (text != null) n.textContent = text;
  if (className) n.className = className;
  return n;
}
function action(text, run, className = "secondary") {
  const b = node("button", text, className);
  b.type = "button";
  b.addEventListener("click", () =>
    Promise.resolve().then(run).catch(showError),
  );
  return b;
}
function showError(error) {
  $("message").textContent =
    error.message || "Something went wrong. Please try again.";
}
async function api(path, method = "GET", body) {
  const headers = {};
  if (body !== undefined && !(body instanceof FormData)) {
    headers["Content-Type"] = "application/json";
    body = JSON.stringify(body);
  }
  if (session?.csrfToken) headers[session.csrfHeader] = session.csrfToken;
  const response = await fetch(path, {
    method,
    headers,
    body,
    credentials: "same-origin",
  });
  if (!response.ok) {
    let problem;
    try {
      problem = await response.json();
    } catch {}
    if (response.status === 401) {
      $("login").hidden = false;
      $("library").hidden = true;
    }
    throw new Error(problem?.detail || `Request failed (${response.status}).`);
  }
  return response.status === 204 ? null : response.json();
}
function edit(kind, title, path, value = {}) {
  $("editor-title").textContent = title;
  $("fields").replaceChildren();
  $("form-error").textContent = "";
  for (const [key, label, type = "text", required = true] of schemas[kind]) {
    const labelNode = node("label", label + (required ? "" : " (optional)"));
    labelNode.htmlFor = "field-" + key;
    const input = node(type === "textarea" ? "textarea" : "input");
    input.id = "field-" + key;
    input.name = key;
    if (type !== "textarea") input.type = type;
    input.required = required;
    input.value = value[key] ?? "";
    if (type === "number") {
      input.min = "1";
      input.max = "2147483647";
      input.step = "1";
    }
    $("fields").append(labelNode, input);
  }
  saveEditor = async () => {
    const body = {};
    for (const [key, , type] of schemas[kind]) {
      const v = $("field-" + key).value;
      body[key] = type === "number" ? Number(v) : v || null;
    }
    await api(path, value.id ? "PUT" : "POST", body);
    $("editor").close();
    await refresh();
  };
  $("editor").showModal();
}
$("edit-form").addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = event.submitter;
  button.disabled = true;
  try {
    await saveEditor();
  } catch (error) {
    $("form-error").textContent = error.message;
  } finally {
    button.disabled = false;
  }
});
$("close-editor").addEventListener("click", () => $("editor").close());
$("add-game").addEventListener("click", () =>
  edit("game", "Add game", "/api/v1/games"),
);
$("logout").addEventListener("click", async () => {
  try {
    const headers = {};
    headers[session.csrfHeader] = session.csrfToken;
    await fetch("/logout", { method: "POST", headers });
    location.reload();
  } catch (error) {
    showError(error);
  }
});
async function remove(path, label, after) {
  if (
    !confirm(
      `Delete ${label}? This also deletes its related records and cannot be undone.`,
    )
  )
    return;
  await api(path, "DELETE");
  if (after) after();
  await refresh();
}
async function refresh() {
  $("message").textContent = "";
  const games = await api("/api/v1/games");
  $("games").replaceChildren();
  $("count").textContent = `${games.length} games`;
  if (!games.length)
    $("games").append(
      node("p", "Your library is empty. Add your first game.", "muted"),
    );
  for (const game of games) {
    const card = node("article", null, "card");
    const image = node("img");
    image.className = "cover";
    image.alt = "";
    image.loading = "lazy";
    image.src = `/api/v1/games/${game.id}/cover`;
    image.addEventListener("error", () => (image.hidden = true));
    card.append(
      image,
      node("h2", game.gameTitle),
      node("p", game.releasePlatform, "muted"),
      action("Open game", async () => {
        selectedGame = game.id;
        selectedPlay = null;
        await detail();
        $("detail").scrollIntoView({ behavior: "smooth" });
      }),
    );
    $("games").append(card);
  }
  if (selectedGame) await detail();
}
async function detail() {
  const path = `/api/v1/games/${selectedGame}`;
  const [game, plays] = await Promise.all([api(path), api(path + "/plays")]);
  const area = $("detail");
  area.hidden = false;
  area.className = "detail";
  area.replaceChildren();
  const heading = node("div", null, "section-heading");
  heading.append(node("h1", game.gameTitle));
  const actions = node("div", null, "actions");
  actions.append(
    action("Edit game", () => edit("game", "Edit game", path, game)),
    action(
      "Delete game",
      () =>
        remove(path, game.gameTitle, () => {
          selectedGame = null;
          selectedPlay = null;
          area.hidden = true;
        }),
      "danger",
    ),
  );
  heading.append(actions);
  area.append(heading, node("p", game.description));
  const metadata = node(
    "p",
    `${game.releasePlatform} · ${game.releaseDate} · ${game.developer} · Critic: ${game.metaRating} · Your rating: ${game.userRating} · Physical copy: ${game.physicalCopy}`,
    "muted",
  );
  area.append(metadata);
  const coverLabel = node("label", "Cover image (PNG or JPEG, up to 5 MB)");
  const upload = node("input");
  upload.type = "file";
  upload.accept = "image/png,image/jpeg";
  coverLabel.append(upload);
  upload.addEventListener("change", async () => {
    if (!upload.files[0]) return;
    try {
      const data = new FormData();
      data.append("file", upload.files[0]);
      await api(path + "/cover", "PUT", data);
      await refresh();
    } catch (error) {
      showError(error);
    }
  });
  area.append(
    coverLabel,
    action("Remove cover", async () => {
      await api(path + "/cover", "DELETE");
      await refresh();
    }),
  );
  const title = node("div", null, "section-heading");
  title.append(
    node("h2", "Playthroughs"),
    action("Add playthrough", () =>
      edit("play", "Add playthrough", path + "/plays"),
    ),
  );
  area.append(title);
  if (!plays.length) area.append(node("p", "No playthroughs yet.", "muted"));
  for (const play of plays) {
    const row = node("article", null, "entry");
    row.append(
      node("h3", `${play.platformPlayedOn} · ${play.completionDate}`),
      node(
        "p",
        `${play.completionRate} · ${play.timeToBeat} · Rating: ${play.playthroughRating} · ${play.location}${play.coop ? " · Co-op: " + play.coop : ""}`,
      ),
    );
    const buttons = node("div", null, "actions");
    const playPath = path + "/plays/" + play.id;
    buttons.append(
      action("Sessions & reviews", async () => {
        selectedPlay = play.id;
        await detail();
      }),
      action("Edit", () => edit("play", "Edit playthrough", playPath, play)),
      action(
        "Delete",
        () => remove(playPath, "this playthrough", () => (selectedPlay = null)),
        "danger",
      ),
    );
    row.append(buttons);
    area.append(row);
    if (selectedPlay === play.id) {
      const [times, reviews] = await Promise.all([
        api(playPath + "/time-entries"),
        api(playPath + "/reviews"),
      ]);
      const columns = node("div", null, "columns");
      for (const [kind, items, suffix, label] of [
        ["time", times, "time-entries", "Play sessions"],
        ["review", reviews, "reviews", "Reviews"],
      ]) {
        const section = node("section");
        section.append(
          node("h3", label),
          action(kind === "time" ? "Log time" : "Add review", () =>
            edit(
              kind,
              kind === "time" ? "Log play time" : "Add review",
              playPath + "/" + suffix,
            ),
          ),
        );
        if (!items.length)
          section.append(node("p", "No entries yet.", "muted"));
        for (const item of items) {
          const entry = node("div", null, "entry");
          entry.append(
            node(
              "p",
              kind === "time"
                ? `${item.date} · ${item.durationMinutes} minutes
${item.notes || ""}`
                : `${item.reviewTitle || "Untitled review"} · ${item.reviewDate || ""}
${item.review || ""}
Rating: ${item.rating || "—"}`,
            ),
          );
          const itemPath = playPath + "/" + suffix + "/" + item.id;
          const controls = node("div", null, "actions");
          controls.append(
            action("Edit", () => edit(kind, "Edit entry", itemPath, item)),
            action("Delete", () => remove(itemPath, "this entry"), "danger"),
          );
          entry.append(controls);
          section.append(entry);
        }
        columns.append(section);
      }
      area.append(columns);
    }
  }
}
(async () => {
  try {
    session = await api("/api/v1/session");
    const locked = session.hosted && !session.authenticated;
    $("login").hidden = !locked;
    $("library").hidden = locked;
    $("add-game").hidden = locked;
    $("logout").hidden = !session.authenticated;
    if (!locked) await refresh();
  } catch (error) {
    showError(error);
  }
})();
