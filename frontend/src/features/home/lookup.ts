import type {
  ContinueWatchingItem,
  HomeTitle,
  HomeView,
} from "@/shared/api/contracts";
import { humanizeId } from "@/shared/lib/format";

export function findTitle(
  home: HomeView | undefined,
  contentId: string,
): HomeTitle | null {
  if (!home) {
    return null;
  }
  const recommended = home.recommended.find(
    (title) => title.content_id === contentId,
  );
  if (recommended) {
    return recommended;
  }
  for (const category of home.categories) {
    const match = category.titles.find(
      (title) => title.content_id === contentId,
    );
    if (match) {
      return match;
    }
  }
  return null;
}

export function titleOrStub(
  home: HomeView | undefined,
  contentId: string,
): HomeTitle {
  return (
    findTitle(home, contentId) ?? {
      content_id: contentId,
      name: humanizeId(contentId),
    }
  );
}

export function continueWatchingFor(
  home: HomeView | undefined,
  contentId: string,
): ContinueWatchingItem | null {
  if (!home) {
    return null;
  }
  return (
    home.continue_watching.find((item) => item.content_id === contentId) ?? null
  );
}

export function featuredSelection(
  home: HomeView,
): { title: HomeTitle; progress: ContinueWatchingItem | null } | null {
  const resume = home.continue_watching[0] ?? null;
  if (resume) {
    return {
      title: titleOrStub(home, resume.content_id),
      progress: resume,
    };
  }

  const title = home.recommended[0] ?? home.categories[0]?.titles[0] ?? null;
  if (!title) {
    return null;
  }
  return { title, progress: null };
}
