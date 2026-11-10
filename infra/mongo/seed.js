// Development catalog for the local stack. Idempotent: run it as many times as needed.
// Shape follows spec.md 2.3. Only `name`, `images.poster` and `trending_rank` are read
// by catalog-service; the other fields are here for future title-detail endpoints.
const streamhub = db.getSiblingDB("streamhub");

const titles = [
  {
    _id: "mov_8832",
    type: "SERIES",
    name: "La última función",
    genres: ["Drama", "Misterio"],
    age_rating: "PG-13",
    duration_seconds: 7080,
    trending_rank: 1,
    images: { poster: null },
  },
  {
    _id: "mov_77",
    type: "MOVIE",
    name: "Ruta nocturna",
    genres: ["Suspenso"],
    age_rating: "R",
    duration_seconds: 6240,
    trending_rank: 2,
    images: { poster: null },
  },
  {
    _id: "mov_91",
    type: "MOVIE",
    name: "El mar en silencio",
    genres: ["Drama"],
    age_rating: "PG-13",
    duration_seconds: 5760,
    trending_rank: 3,
    images: { poster: null },
  },
  {
    _id: "mov_42",
    type: "MOVIE",
    name: "Los días de agosto",
    genres: ["Romance", "Comedia"],
    age_rating: "PG",
    duration_seconds: 6600,
    trending_rank: 4,
    images: { poster: null },
  },
  {
    _id: "mov_105",
    type: "MOVIE",
    name: "Verano eléctrico",
    genres: ["Comedia", "Música"],
    age_rating: "PG",
    duration_seconds: 5520,
    trending_rank: 5,
    images: { poster: null },
  },
  {
    _id: "mov_64",
    type: "MOVIE",
    name: "Frecuencia",
    genres: ["Ciencia ficción"],
    age_rating: "PG-13",
    duration_seconds: 7680,
    trending_rank: 6,
    images: { poster: null },
  },
  {
    _id: "mov_1",
    type: "MOVIE",
    name: "Ciudad de cristal",
    genres: ["Misterio"],
    age_rating: "R",
    duration_seconds: 6420,
    trending_rank: 7,
    images: { poster: null },
  },
  {
    _id: "mov_2",
    type: "MOVIE",
    name: "Niebla",
    genres: ["Suspenso"],
    age_rating: "PG-13",
    duration_seconds: 6060,
    trending_rank: 8,
    images: { poster: null },
  },
  {
    _id: "mov_12",
    type: "MOVIE",
    name: "Cinta negra",
    genres: ["Documental"],
    age_rating: "PG",
    duration_seconds: 5280,
    trending_rank: 9,
    images: { poster: null },
  },
  {
    _id: "mov_58",
    type: "MOVIE",
    name: "Atlas",
    genres: ["Aventura"],
    age_rating: "PG-13",
    duration_seconds: 8040,
    trending_rank: 10,
    images: { poster: null },
  },
  {
    _id: "mov_31",
    type: "MOVIE",
    name: "La casa del faro",
    genres: ["Drama", "Familia"],
    age_rating: "PG",
    duration_seconds: 5940,
    trending_rank: 11,
    images: { poster: null },
  },
  {
    _id: "mov_7",
    type: "MOVIE",
    name: "Animales de la noche",
    genres: ["Drama"],
    age_rating: "R",
    duration_seconds: 7260,
    trending_rank: 12,
    images: { poster: null },
  },
];

for (const title of titles) {
  const { _id, ...fields } = title;
  streamhub.titles.updateOne({ _id }, { $set: fields }, { upsert: true });
}

print(`Seeded ${titles.length} titles in streamhub.titles`);
