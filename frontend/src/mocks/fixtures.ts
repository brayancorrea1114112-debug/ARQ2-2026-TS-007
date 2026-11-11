import type {
  HomeTitle,
  HomeView,
  ProfileSummary,
} from "@/shared/api/contracts";

export const MOCK_PASSWORD = "S3cret-pass";

export interface MockAccount {
  accountId: string;
  email: string;
  profiles: ProfileSummary[];
}

export const MOCK_ACCOUNTS: MockAccount[] = [
  {
    accountId: "acc_ana",
    email: "ana@example.com",
    profiles: [
      { profile_id: "prof_9921", name: "Ana", type: "ADULT" },
      { profile_id: "prof_9922", name: "Sam", type: "KIDS" },
    ],
  },
  {
    accountId: "acc_luis",
    email: "luis@example.com",
    profiles: [
      { profile_id: "prof_9931", name: "Luis", type: "ADULT" },
    ],
  },
];

const TITLES: Record<string, HomeTitle> = {
  mov_8832: {
    content_id: "mov_8832",
    name: "La última función",
    synopsis:
      "Un proyeccionista de un cine antiguo descubre que la película que exhibe cada noche cambia según quién la mira.",
    duration_seconds: 7080,
    age_rating: "PG-13",
    genres: ["Drama", "Misterio"],
  },
  mov_77: {
    content_id: "mov_77",
    name: "Ruta nocturna",
    synopsis:
      "Una conductora de turno nocturno recibe a un pasajero que asegura venir de su futuro.",
    duration_seconds: 6240,
    age_rating: "R",
    genres: ["Suspenso"],
  },
  mov_91: {
    content_id: "mov_91",
    name: "El mar en silencio",
    synopsis:
      "Dos hermanas vuelven a la casa del faro para vaciar lo que quedó de su padre.",
    duration_seconds: 5760,
    age_rating: "PG-13",
    genres: ["Drama"],
  },
  mov_42: {
    content_id: "mov_42",
    name: "Los días de agosto",
    synopsis:
      "Un verano de pueblo, una radio pirata y tres amigos que prometen no olvidarse.",
    duration_seconds: 6600,
    age_rating: "PG",
    genres: ["Romance", "Comedia"],
  },
  mov_105: {
    content_id: "mov_105",
    name: "Verano eléctrico",
    synopsis:
      "Una banda improvisada intenta tocar en la fiesta del barrio antes de que se vaya la energía.",
    duration_seconds: 5520,
    age_rating: "PG",
    genres: ["Comedia", "Música"],
  },
  mov_64: {
    content_id: "mov_64",
    name: "Frecuencia",
    synopsis:
      "Una radioaficionada capta una señal que repite el mismo minuto de su vida.",
    duration_seconds: 7680,
    age_rating: "PG-13",
    genres: ["Ciencia ficción"],
  },
  mov_1: {
    content_id: "mov_1",
    name: "Ciudad de cristal",
    synopsis:
      "Un arquitecto reconstruye de memoria un edificio que ya no existe.",
    duration_seconds: 6420,
    age_rating: "R",
    genres: ["Misterio"],
  },
  mov_2: {
    content_id: "mov_2",
    name: "Niebla",
    synopsis:
      "La niebla cubre el pueblo y con ella llegan los recuerdos que nadie quería.",
    duration_seconds: 6060,
    age_rating: "PG-13",
    genres: ["Suspenso"],
  },
  mov_12: {
    content_id: "mov_12",
    name: "Cinta negra",
    synopsis:
      "Detrás de un archivo de noticiarios olvidados, una familia descubre su propia historia.",
    duration_seconds: 5280,
    age_rating: "PG",
    genres: ["Documental"],
  },
  mov_58: {
    content_id: "mov_58",
    name: "Atlas",
    synopsis:
      "Un cartógrafo del siglo XIX dibuja una isla que todos juran haber visto.",
    duration_seconds: 8040,
    age_rating: "PG-13",
    genres: ["Aventura"],
  },
  mov_31: {
    content_id: "mov_31",
    name: "La casa del faro",
    synopsis:
      "Una familia hereda un faro y, con él, la costumbre de encenderlo cada noche.",
    duration_seconds: 5940,
    age_rating: "PG",
    genres: ["Drama", "Familia"],
  },
  mov_7: {
    content_id: "mov_7",
    name: "Animales de la noche",
    synopsis:
      "Un ilustrador insomne empieza a dibujar las vidas que se cruzan bajo su ventana.",
    duration_seconds: 7260,
    age_rating: "R",
    genres: ["Drama"],
  },
};

interface HomeLayout {
  continueWatching: Array<{
    content_id: string;
    episode_id?: string;
    progress_seconds: number;
  }>;
  recommended: string[];
  categories: Array<{ name: string; ids: string[] }>;
}

const ADULT_LAYOUT: HomeLayout = {
  continueWatching: [
    { content_id: "mov_8832", progress_seconds: 1420 },
    { content_id: "mov_77", episode_id: "ep_204", progress_seconds: 3200 },
  ],
  recommended: ["mov_8832", "mov_77", "mov_91", "mov_42", "mov_105", "mov_64"],
  categories: [
    {
      name: "Tendencias esta noche",
      ids: ["mov_1", "mov_2", "mov_12", "mov_58", "mov_64"],
    },
    {
      name: "Estrenos",
      ids: ["mov_42", "mov_105", "mov_64", "mov_7"],
    },
    {
      name: "Drama para después de la cena",
      ids: ["mov_91", "mov_31", "mov_8832", "mov_7"],
    },
    {
      name: "Para ver en familia",
      ids: ["mov_2", "mov_31", "mov_105", "mov_58"],
    },
  ],
};

const KIDS_LAYOUT: HomeLayout = {
  continueWatching: [{ content_id: "mov_105", progress_seconds: 900 }],
  recommended: ["mov_105", "mov_31", "mov_2", "mov_58"],
  categories: [
    {
      name: "Para ver en familia",
      ids: ["mov_105", "mov_31", "mov_2", "mov_58"],
    },
    {
      name: "Aventuras",
      ids: ["mov_58", "mov_105", "mov_31"],
    },
    {
      name: "Historias para dormir",
      ids: ["mov_31", "mov_2", "mov_105"],
    },
  ],
};

const LUIS_LAYOUT: HomeLayout = {
  continueWatching: [{ content_id: "mov_1", progress_seconds: 600 }],
  recommended: ["mov_1", "mov_58", "mov_64", "mov_12", "mov_7"],
  categories: [
    { name: "Para ti", ids: ["mov_1", "mov_58", "mov_64", "mov_12"] },
    { name: "Misterio", ids: ["mov_1", "mov_2", "mov_8832", "mov_7"] },
    { name: "Documentales", ids: ["mov_12", "mov_91", "mov_42"] },
  ],
};

function layoutFor(profileId: string): HomeLayout {
  if (profileId === "prof_9922") {
    return KIDS_LAYOUT;
  }
  if (profileId === "prof_9931") {
    return LUIS_LAYOUT;
  }
  return ADULT_LAYOUT;
}

function titlesFor(ids: string[]): HomeTitle[] {
  return ids
    .map((id) => TITLES[id])
    .filter((title): title is HomeTitle => title !== undefined);
}

export function buildHome(profileId: string): HomeView {
  const layout = layoutFor(profileId);
  return {
    profile_id: profileId,
    generated_at: new Date().toISOString(),
    source: "cache",
    categories: layout.categories.map((category) => ({
      name: category.name,
      titles: titlesFor(category.ids),
    })),
    continue_watching: layout.continueWatching,
    recommended: titlesFor(layout.recommended),
  };
}
