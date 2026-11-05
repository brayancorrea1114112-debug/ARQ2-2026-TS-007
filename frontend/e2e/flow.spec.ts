import { expect, test, type Page } from "@playwright/test";

async function enterAsAna(page: Page) {
  await page.goto("/login");
  await page.getByLabel("Correo electrónico").fill("ana@example.com");
  await page.getByLabel("Contraseña").fill("S3cret-pass");
  await page.getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/profiles$/);
  await page.getByRole("button", { name: "Entrar como Ana" }).click();
  await expect(page).toHaveURL(/\/home$/);
}

test("login, profile selection and home with mocks", async ({ page }) => {
  await page.goto("/login");
  await expect(page.getByRole("heading", { name: "Entrar" })).toBeVisible({
    timeout: 20_000,
  });
  await page.screenshot({ path: "test-results/login.png", fullPage: true });

  await page.getByLabel("Correo electrónico").fill("ana@example.com");
  await page.getByLabel("Contraseña").fill("S3cret-pass");
  await page.getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/profiles$/);
  await page.getByRole("button", { name: "Entrar como Ana" }).click();

  await expect(page).toHaveURL(/\/home$/);
  await expect(
    page.getByRole("heading", { name: "Seguir viendo" }),
  ).toBeVisible();
  await expect(
    page.getByRole("heading", { name: "Recomendado para ti" }),
  ).toBeVisible();

  await page.screenshot({ path: "test-results/home.png", fullPage: true });
});

test("playback reports a ping and a rating is accepted", async ({ page }) => {
  await enterAsAna(page);

  await page.getByRole("link", { name: "Ver detalles" }).click();
  await expect(page).toHaveURL(/\/title\/mov_8832$/);

  await page.getByRole("link", { name: "Reanudar" }).click();
  await expect(page).toHaveURL(/\/watch\/mov_8832/, { timeout: 20_000 });

  await page.getByRole("button", { name: "Reproducir" }).click();
  await page.waitForTimeout(1200);
  await page.getByRole("button", { name: "Pausar" }).click();

  await expect(page.getByText("Progreso guardado.")).toBeVisible({
    timeout: 15_000,
  });

  await page.getByRole("button", { name: "Me gusta", exact: true }).click();
  await expect(page.getByText(/Recibido\./)).toBeVisible({ timeout: 15_000 });

  await page.screenshot({ path: "test-results/watch.png", fullPage: true });
});
