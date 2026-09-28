-- =============================================================================
-- V2 - Remove o que sobrou da entidade Cultura (28/09/2026)
--
-- O app abandonou Cultura como entidade em 14/09/2026 (a cultura virou texto
-- livre em talhao.cultura). No backend o vinculo foi tirado de Safra (Fase 2 do
-- sync) e de MetaFinanceira (Fase 3), mas o ddl-auto=update nunca remove nada,
-- entao ficaram para tras:
--   - safra.cultura_id           (coluna orfa + FK para cultura)
--   - meta_financeira.cultura_id (coluna orfa + FK para cultura)
--   - tabela cultura             (sem entidade/endpoint desde este commit)
--
-- Snapshot manual do RDS tirado antes desta migration:
-- safrapay-db-pre-flyway-2026-09-28 (us-east-2).
-- =============================================================================

ALTER TABLE public.safra DROP CONSTRAINT fkfb87rup5paj2opi3r8yno1059;
ALTER TABLE public.safra DROP COLUMN cultura_id;

ALTER TABLE public.meta_financeira DROP CONSTRAINT fk8i50g0i2kjs9yrb8o5k9d65bv;
ALTER TABLE public.meta_financeira DROP COLUMN cultura_id;

DROP TABLE public.cultura;
