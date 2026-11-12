# StreamHub — Frontend design notes

## Brief

StreamHub is a VOD catalog and recommendation platform for families, and also
the place where the Software Architecture II class recordings are watched. The
Home screen is the product: it has to help someone choose what to watch in a few
seconds. The interface is a client of the microservices behind Kong, not a
second backend.

## Visual direction: engineering notebook

Cool grid paper, navy ink, ballpoint blue for actions and one orange marker that
only ever means "position / progress". Video screens switch to a dark slate
("pizarra"). It avoids black + red streaming clones, cream + terracotta, and the
identical-rounded-cards SaaS kit.

### Tokens (`src/app/globals.css`)

| Token | Value | Use |
|---|---|---|
| hoja | `#F2F4F8` | Page background (with the `.papel` grid) |
| hoja-2 / hoja-3 | `#FFFFFF` / `#E7EBF3` | Cards and inputs / skeletons |
| regla | `rgba(16,24,72,.14)` | Hairline borders and rules |
| tinta / tinta-suave / tinta-tenue | `#101848` / `#4A5280` / `#666D94` | Primary, secondary, tertiary text |
| azul | `#2433E8` | Primary action, links, focus |
| marcador | `#FF5B2E` | Playhead, progress, active nav (fills only) |
| marcador-texto | `#C33A12` | Small orange text (contrast-safe) |
| pizarra / tiza | `#0B1030` / `#F1F3FF` | Dark video surfaces and their text |
| menta | `#0C8F6A` | Kids profile accent |

### Type

- **Archivo** (variable width axis) at 72% width, weight 800 for titles
  (`.titular`): reads like a timetable header.
- **Geist** for UI and body text.

### Signature

The orange **playhead**: the tick in the wordmark, the segment on every section
rule, the marker on progress bars and the active nav underline. The class
library adds the **semester ruler**: each recording is a tick on its real date.

### Depth

Borders only (hairline rules), no card shadows. Hover lifts 2px and the border
turns blue. Radius scale: 3px tags, 6px (`rounded-hoja`) surfaces, full for
avatars and play buttons.

## Principles

- One memorable element per screen: the hero stage on Home, the date on class
  cards, the titles everywhere else.
- Structure encodes information: rules separate sections, badges identify kids
  profiles, ticks show real dates and progress.
- Motion is restrained: one rise-in on page headings, 150ms hover/press
  feedback, reduced motion respected.
- Quality floor: visible keyboard focus, labels on every field, 44px touch
  targets, accessible progress bars, responsive to 375px.

## Deliberate exclusions

- No autoplay: the player starts paused and only sends telemetry while playing.
- No second backend: the mock layer (MSW) speaks the same HTTP contracts as the
  services.
