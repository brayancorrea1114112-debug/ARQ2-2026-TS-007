# StreamHub — Frontend design notes

## Brief

StreamHub is a VOD catalog and recommendation platform for families. The Home
screen is the product: it has to help someone choose something to watch in a few
seconds, at 8 PM, with the whole family around the TV. The interface is a client
of the microservices behind Kong, not a second backend.

Audience: adults choosing a film or series and kids using a separate profile.
Primary job: resume what was in progress, then browse a short, confident set of
recommendations.

## Visual direction

The interface borrows from an evening programme guide and a cinema marquee: a
dark projector room, warm paper text, brass light and hairline rules. It
deliberately avoids the generic streaming clone (pure black plus red) and the
SaaS card kit (identical rounded cards, soft shadows, gradient CTAs).

### Color

| Token | Value | Use |
|---|---|---|
| ink-950 | `#0B0F14` | Page background, player stage |
| ink-900 | `#111821` | Panels, profile cards |
| ink-800 | `#1A2430` | Inputs, meters, skeletons |
| line | `#2B3746` | Hairline borders and rules |
| paper | `#F3EDE3` | Primary text |
| paper-muted | `#A7B0BA` | Secondary text and metadata |
| brass | `#E8B04B` | Primary action, progress, focus ring |
| brass-strong | `#F4C979` | Hover on primary actions |
| signal | `#E2574C` | Errors and destructive states |
| mint | `#86C7B7` | Kids profile accent |

### Type

- **Fraunces** for display: the wordmark, page titles and section headings.
  It carries a printed-programme personality without looking like a default
  editorial template.
- **Instrument Sans** for UI and body: labels, forms, metadata and actions.
- Sizes follow a clear scale (display 5xl–6xl, section 2xl, body base, metadata
  xs). Numbers that change over time use tabular defaults via the UI font.

### Layout

```
Home
┌──────────────────────────────────────────────┐
│ wordmark             profile chip    salir   │
├──────────────────────────────────────────────┤
│ hero: eyebrow / big serif title / synopsis   │
│       metadata / actions / progress   poster │
├──────────────────────────────────────────────┤
│ Seguir viendo        ─────────────────────── │
│ [poster + ribbon] [poster + ribbon] ...      │
├──────────────────────────────────────────────┤
│ Recomendado para ti  ─────────────────────── │
│ [poster] [poster] [poster] ...               │
└──────────────────────────────────────────────┘
```

Content is left aligned and capped at `max-w-6xl`. The hero is the single bold
moment; rails stay quiet with hairline headings and no card shadows. Posters are
2:3 with a thin border; continue-watching posters carry a brass progress ribbon.

## Principles

- One memorable element per screen. On Home it is the hero; everywhere else it is
  the typography, not decoration.
- Structure encodes information: rules separate sections, badges identify kids
  profiles, ribbons show real progress.
- Motion is restrained: one rise-in on the hero and the profile heading. Hover
  changes color, not scale, and reduced-motion is respected.
- Copy is direct and in the interface's voice: "Reanudar", "Ver detalles",
  "¿Quién está viendo?", "El progreso se guarda mientras reproduces."
- Quality floor: keyboard focus visible, labels on every field, 44px touch
  targets, errors with a correlation id, accessible progress bars.

## Deliberate exclusions

- No autoplay: the player preview starts paused and only sends telemetry while
  playing.
- No fake avatars or stock images: posters use deterministic gradients derived
  from the content id until the real catalog provides `poster` URLs.
- No second backend: the mock layer (MSW) speaks the same HTTP contracts as the
  services. Turning `NEXT_PUBLIC_USE_MOCKS` off is the only integration step.
