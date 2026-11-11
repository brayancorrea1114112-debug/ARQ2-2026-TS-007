import { describe, expect, it } from "vitest";
import { normalizeHome } from "@/shared/api/contracts";

describe("normalizeHome", () => {
  it("keeps the shape defined by the spec", () => {
    const home = normalizeHome({
      profile_id: "prof_9921",
      generated_at: "2026-10-06T19:58:00Z",
      source: "cache",
      categories: [
        {
          name: "Trending",
          titles: [
            {
              content_id: "mov_1",
              name: "Title One",
              poster: "https://cdn.example.com/p/mov_1.jpg",
            },
          ],
        },
      ],
      continue_watching: [
        { content_id: "mov_8832", episode_id: "ep_102", progress_seconds: 1420 },
      ],
      recommended: [
        { content_id: "mov_77", name: "Title 77", poster: null },
      ],
    });

    expect(home.source).toBe("cache");
    expect(home.categories[0].titles[0]).toMatchObject({
      content_id: "mov_1",
      name: "Title One",
    });
    expect(home.continue_watching[0]).toEqual({
      content_id: "mov_8832",
      episode_id: "ep_102",
      progress_seconds: 1420,
    });
    expect(home.recommended[0].name).toBe("Title 77");
  });

  it("tolerates the raw Redis projection with ids and title_ids", () => {
    const home = normalizeHome({
      profile_id: "prof_9921",
      generated_at: "2026-10-06T19:58:00Z",
      categories: [{ name: "Trending", title_ids: ["mov_2"] }],
      continue_watching: [{ content_id: "mov_8832", progress_seconds: 42 }],
      recommended: ["mov_1"],
    });

    expect(home.source).toBe("fallback");
    expect(home.categories[0].titles[0].name).toBe("Título 2");
    expect(home.recommended[0]).toMatchObject({
      content_id: "mov_1",
      name: "Título 1",
    });
  });

  it("returns safe defaults for an invalid payload", () => {
    const home = normalizeHome(null);

    expect(home.profile_id).toBe("");
    expect(home.source).toBe("fallback");
    expect(home.categories).toEqual([]);
    expect(home.recommended).toEqual([]);
    expect(home.continue_watching).toEqual([]);
  });
});
