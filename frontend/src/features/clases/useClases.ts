"use client";

import { useQuery } from "@tanstack/react-query";
import { fetchClases } from "@/features/clases/api";

export function useClases() {
  return useQuery({
    queryKey: ["clases"],
    queryFn: fetchClases,
    staleTime: 60_000,
    retry: false,
  });
}
